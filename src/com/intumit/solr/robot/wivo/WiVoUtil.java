package com.intumit.solr.robot.wivo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.DicAnalysis;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.lang3.StringUtils;
import org.nlpcn.commons.lang.tire.domain.Forest;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.fuzzy.MODE;
import com.intumit.android.search.fuzzy.PhoneticSimilarity;
import com.intumit.android.search.fuzzy.Piece;
import com.intumit.android.search.fuzzy.VROResult;
import com.intumit.android.search.fuzzy.VoiceRecognitionOptimizer;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.EvaluationLogEntity;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

/**
 * WiVO 工具，詞庫看 init() 怎麼 load
 * 這裡是直接以知識主題當作詞庫，當有了 WiVoEntry 時會採用 WiVOEntryDictionary 直接處理...
 * 
 * @author herb
 */
public class WiVoUtil {
	static Map<Integer, VoiceRecognitionOptimizer> fuzzyTrieMap = new HashMap<Integer, VoiceRecognitionOptimizer>();
	static Map<Integer, Set<String>> allKwsMap = new HashMap<Integer, Set<String>>();
	
	static Map<String, VoiceRecognitionOptimizer> fuzzyTrieMap4Channel = new HashMap<String, VoiceRecognitionOptimizer>();
	static Map<String, Set<String>> kwsMap4Channel = new HashMap<String, Set<String>>();

	// 暫時有個小地方可以解決特定詞一直被轉換的問題
	static Set<String> DONT_TOUCH_ME = new HashSet<String>(Arrays.asList(new String[] { "" }));

	public static void clear(Integer tenantId) {
		fuzzyTrieMap.remove(tenantId);
		allKwsMap.remove(tenantId);
		Iterator<String> itr = fuzzyTrieMap4Channel.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
		
			if (StringUtils.startsWith(key, "" + tenantId + "_")) {
				itr.remove();
			}
		}
		
		itr = kwsMap4Channel.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			if (StringUtils.startsWith(key, "" + tenantId + "_")) {
				itr.remove();
			}
		}
	}

	public void init(Integer tenantId, String channel, FuzzyLevel fuzzyLevel, PhoneticSimilarity phoneSimilarity, boolean enableNoTone) {

		if (channel == null && !fuzzyTrieMap.containsKey(tenantId)) {
			//Tenant t = Tenant.get(companyId);
			List<WiVoEntry> ddList = WiVoEntry.listByTenantId(tenantId);
			Set<String> fuzzyKwSet = new HashSet<String>();
			
			if (ddList.size() > 0) {
				for (WiVoEntry dd : ddList) {
					if (dd.isEnabled() 
							&& StringUtils.isEmpty(dd.getChannel()) // channel is empty 代表是「全部」
							) {
						fuzzyKwSet.add(dd.getKeyword());
					}
				}
			}

			// 預設只比對完全同音，採用 MODE.MPS2 是包含幾聲的聲調都要相同。若要不管聲調則可採用 MODE.MPS2_NO_TONE
			{
				System.out.println("VO fuzzyKwSet:" + fuzzyKwSet);
				VoiceRecognitionOptimizer fuzzy = new VoiceRecognitionOptimizer(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/wv" + tenantId));
				fuzzy.createDictionary(fuzzyKwSet, "kp", enableNoTone ? MODE.MPS2_NO_TONE : MODE.MPS2);
				fuzzy.setFuzzyLevel(fuzzyLevel);
				fuzzy.setPhoneticSimilarity(phoneSimilarity);
				fuzzyTrieMap.put(tenantId, fuzzy);
				allKwsMap.put(tenantId, fuzzyKwSet);
			}
		}
		else {
			// 新增trieCategoryMap，for租戶-分類的知識點
			String key = tenantId + "_" + channel;
			if (!fuzzyTrieMap4Channel.containsKey(key)) {
				List<WiVoEntry> ddList = WiVoEntry.listByTenantIdAndChannel(tenantId, channel, true);
				Set<String> subKwSet = new HashSet<String>();

				for (WiVoEntry dd : ddList) {
					if (StringUtils.isEmpty(dd.getChannel()) || dd.getChannel().equals(channel)) {
						subKwSet.add(dd.getKeyword());
					}
				}

				{
					VoiceRecognitionOptimizer fuzzy4ch = new VoiceRecognitionOptimizer(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/wv" + key));
					fuzzy4ch.createDictionary(subKwSet, "kp", enableNoTone ? MODE.MPS2_NO_TONE : MODE.MPS2);
					fuzzy4ch.setFuzzyLevel(fuzzyLevel);
					fuzzy4ch.setPhoneticSimilarity(phoneSimilarity);
					fuzzyTrieMap4Channel.put(key, fuzzy4ch);
					kwsMap4Channel.put(key, subKwSet);
				}
			}
		}
	}
	
	static Map<Integer, WiVoUtil> instances = new HashMap<>();
	
	public static WiVoUtil getInstance(Integer tenantId) {
		if (instances.containsKey(tenantId)) {
			return instances.get(tenantId);
		}
		
		return null;
	}
	
	public static WiVoUtil createNewInstance(Integer tenantId, FuzzyLevel fuzzyLevel, PhoneticSimilarity phoneticSimilarity) {
		return createNewInstance(tenantId, fuzzyLevel, phoneticSimilarity, false);
	}
	
	public static WiVoUtil createNewInstance(Integer tenantId, FuzzyLevel fuzzyLevel, PhoneticSimilarity phoneticSimilarity, boolean enableNoTone) {
		WiVoUtil wv = new WiVoUtil(tenantId, fuzzyLevel, phoneticSimilarity, enableNoTone);
		if (instances.containsKey(tenantId)) {
			WiVoUtil.clear(tenantId);
		}
		instances.put(tenantId, wv);
		
		return wv;
	}
	
	Integer tenantId;
	FuzzyLevel fuzzyLevel;
	PhoneticSimilarity phoneticSimilarity;
	boolean enableNoTone;
	
	WiVoUtil(Integer tenantId, FuzzyLevel fuzzyLevel, PhoneticSimilarity phoneticSimilarity, boolean enableNoTone) {
		this.tenantId = tenantId;
		this.fuzzyLevel = fuzzyLevel;
		this.phoneticSimilarity = phoneticSimilarity;
		this.enableNoTone = enableNoTone;
	}

	/**
	 * 回傳句子當中是否有跟 WiVoEntry 設定一樣的字串
	 * 注意此 method 並不會判定 exclude
	 * 
	 * @param tenantId
	 * @param channel 代號
	 * @param question
	 * @return 回傳是有找到的關鍵字（給 WiVoEntry keyword，而非原關鍵詞）
	 */
	public List<String> suggest(String channel, String question) {
		init(tenantId, channel, fuzzyLevel, phoneticSimilarity, enableNoTone);
		
		VoiceRecognitionOptimizer vo = null;
		Set<String> kws = null;
		
		if (channel == null) {
			vo = fuzzyTrieMap.get(tenantId);
			kws = allKwsMap.get(tenantId);
		}
		else {
			String key = "" + tenantId + "_" + channel;
			vo = fuzzyTrieMap4Channel.get(key);
			kws = kwsMap4Channel.get(key);
		}
		if (vo == null) return new ArrayList<String>();

		VROResult vor = vo.suggest(question);
		return vor.getSuggests();
	}

	/**
	 * 利用 WiVo 直接替換句子當中與知識點（及其同義詞）同音的區段
	 * 例如「我想辦釁用卡，請問有什麼幽會方案？」，假設「信用卡」跟「優惠活動」是知識點，而「優惠方案」是「優惠活動」的同義詞 透過本 method
	 * 後會轉換成「我想辦信用卡，請問有什麼優惠方案」
	 * 
	 * @param tenantId
	 * @param question
	 * @return
	 */
	public String suggestFullQuestion(String question, String channel) {
		init(tenantId, channel, fuzzyLevel, phoneticSimilarity, enableNoTone);
		
		Set<WiVoEntryMatched>[] matched = WiVoEntryDictionary.search(tenantId, question, channel);
		StringBuilder sb = new StringBuilder();
		
		int lastPos = 0;
		for (int i=0; i < matched.length; i++) {
			Set<WiVoEntryMatched> ms = matched[i];
			if (ms != null && ms.size() > 0) {
				WiVoEntryMatched m = ms.iterator().next();
				if (m.matchedPosition > lastPos) {
					sb.append(question.substring(lastPos, m.matchedPosition));
				}
				
				sb.append(m.getKeyword());
				lastPos = m.matchedPosition + StringUtils.length(m.matchedValue);
			}
		}
		
		if (lastPos < question.length())
			sb.append(question.substring(lastPos));
		
		String newQuestion = sb.toString();
		System.out.println("WivoEntryDictionary search & replace : [" + question + "] => [" + newQuestion + "]");

		VoiceRecognitionOptimizer vo = null;
		Set<String> kws = null;
		
		if (channel == null) {
			vo = fuzzyTrieMap.get(tenantId);
			kws = allKwsMap.get(tenantId);
		}
		else {
			String key = "" + tenantId + "_" + channel;
			vo = fuzzyTrieMap4Channel.get(key);
			kws = kwsMap4Channel.get(key);
		}

		if (vo == null) return null;

		VROResult vor = vo.suggest(newQuestion);
		List<String> suggests = vo.suggest(newQuestion).getSuggests();
		boolean[] absoluteTerritory = new boolean[question.length()];
		Arrays.fill(absoluteTerritory, false);

		for (String suggest : suggests) {
			List<Piece> pieces = vor.getMatchedPieces(suggest);

			for (Piece piece : pieces) {
				if (piece.getText().equals(suggest)) {
					Arrays.fill(absoluteTerritory, piece.getStartPos(), piece.getEndPos(), true);
				}
			}
		}

		for (String suggest : suggests) {
			List<Piece> pieces = vor.getMatchedPieces(suggest);
			WiVoEntry wv = WiVoEntry.get(tenantId, suggest);
			//List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenantId, suggest);

			pieceLoop: for (Piece piece : pieces) {
				if (isEatingWord(piece)) continue;

				if (piece.getText().length() != suggest.length()) continue;
				if (DONT_TOUCH_ME.contains(piece.getText())) continue;
				if (wv.getExcludeSet().contains(piece.getText())) continue;

				if (!kws.contains(piece.getText())) {// 避免已經是現有 WiVoEntry 還被替換
					for (int i = piece.getStartPos(); i < piece.getEndPos(); i++) {
						if (absoluteTerritory[i]) {
							System.out.println("A matched piece by original text is invade the ABSOLUTE TERRITORY:" + piece);
							continue pieceLoop;
						}
					}

					String newSuggest = suggest;
					/*if (syns != null && syns.size() == 1) {
						newSuggest = syns.get(0).getKeyword();
					}*/
					newQuestion = newQuestion.replaceAll(piece.getText(), newSuggest);
				}
				else {
					System.out.println("A matched piece by original text is already a keyword:" + piece);
				}
			}
		}
		return newQuestion;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public FuzzyLevel getFuzzyLevel() {
		return fuzzyLevel;
	}

	public PhoneticSimilarity getPhoneticSimilarity() {
		return phoneticSimilarity;
	}

	public boolean isEnableNoTone() {
		return enableNoTone;
	}

	static final char[] PHONETIC_MAIN = "ㄅㄆㄇㄈㄉㄊㄋㄌㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".toCharArray();

	public static boolean isEatingWord(Piece p) {
		String mb = p.getMatchedBy();
		String pi = p.getPinyin();

		if (pi.endsWith(mb) || pi.startsWith(mb)) {
			String remain = pi.replaceAll(mb, "");

			if (StringUtils.containsAny(remain, PHONETIC_MAIN)) return true;
		}

		return false;
	}

	public static void main(String[] args) {
		HibernateUtil.init();
		WiVoUtil wv = new WiVoUtil(7, FuzzyLevel.NOFUZZY, PhoneticSimilarity.FLEXIBLE, true);
		// 把「豐豐」設定為排除詞，主要關鍵字若為「風風」，應該要可以測出結果 => 我要開豐豐，開到你嫑嫑的，你這個風風的小子
		String r = wv.suggestFullQuestion("我要開豐豐，開到你嫑嫑的，你這個瘋瘋的小子", null);
		System.out.println(r);
		
		HibernateUtil.shutdown();
	}
}
