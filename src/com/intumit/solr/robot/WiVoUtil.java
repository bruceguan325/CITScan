package com.intumit.solr.robot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

	// 暫時有個小地方可以解決特定詞一直被轉換的問題
	static Set<String> DONT_TOUCH_ME = new HashSet<String>(Arrays.asList(new String[] { "什麼" }));

	public static void clear(Integer tenantId) {
		fuzzyTrieMap.remove(tenantId);
		allKwsMap.remove(tenantId);
	}

	public static void init(Integer tenantId) {
		if (!fuzzyTrieMap.containsKey(tenantId)) {
			// 建立 WiVo 知識點 VRO
			VoiceRecognitionOptimizer fuzzy = new VoiceRecognitionOptimizer(
					new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/wv" + tenantId));
			Set<String> fuzzyKwSet = new HashSet<String>();
			fuzzyKwSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));
			List<QAEntity> l1 = QAEntity.listByTenantId(tenantId);
			for (QAEntity d : l1) {
				if (d.isEnabled()) fuzzyKwSet.addAll(Arrays.asList(StringUtils.split(d.getEntityValues(), ",")));
			}
			List<QAIntent> l2 = QAIntent.listByTenantId(tenantId);
			for (QAIntent d : l2) {
				if (d.isEnabled()) fuzzyKwSet.addAll(Arrays.asList(StringUtils.split(d.getKeywords(), ",")));
			}

			// 當一個詞是同義詞的頭的時候，就把所有同義詞都加進來
			Set<String> toBeAdd = new HashSet<>();
			for (String kw : fuzzyKwSet) {
				List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, kw, true);

				if (syns != null && syns.size() > 0) {
					for (String syn : syns) {
						if (!StringUtils.equalsIgnoreCase(syn, kw)) {
							toBeAdd.add(syn);
						}
					}
				}
			}
			fuzzyKwSet.addAll(toBeAdd);

			// 如果要連同義詞一起 WiVo 才跑下面這段
			/*
			 * for (SynonymKeyword sk:
			 * SynonymKeywordFacade.getInstance().listSynonymKeywords(tenantId))
			 * { fuzzyKwSet.addAll(sk.getKeywordAndSynonymList()); }
			 */

			// 預設只比對完全同音，採用 MODE.MPS2 是包含幾聲的聲調都要相同。若要不管聲調則可採用 MODE.MPS2_NO_TONE
			fuzzy.createDictionary(fuzzyKwSet, "kp", MODE.MPS2);
			fuzzy.setFuzzyLevel(FuzzyLevel.NOFUZZY);
			fuzzy.setPhoneticSimilarity(PhoneticSimilarity.FLEXIBLE);
			fuzzyTrieMap.put(tenantId, fuzzy);
			allKwsMap.put(tenantId, fuzzyKwSet);
		}
	}

	/**
	 * 回傳句子當中是否有跟知識點同音的區段
	 * 
	 * @param tenantId
	 * @param question
	 * @return 回傳是有找到的知識點的關鍵字（給知識點，而非原關鍵詞）
	 */
	public static List<String> suggest(Integer tenantId, String question) {
		init(tenantId);
		VoiceRecognitionOptimizer vo = fuzzyTrieMap.get(tenantId);
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
	public static String suggestFullQuestion(Integer tenantId, String question) {
		init(tenantId);
		VoiceRecognitionOptimizer vo = fuzzyTrieMap.get(tenantId);

		if (vo == null) return null;

		Set<String> kws = allKwsMap.get(tenantId);

		VROResult vor = vo.suggest(question);
		List<String> suggests = vo.suggest(question).getSuggests();
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
			List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenantId,
					suggest);

			pieceLoop: for (Piece piece : pieces) {
				if (isEatingWord(piece)) continue;

				if (piece.getText().length() != suggest.length()) continue;

				if (DONT_TOUCH_ME.contains(piece.getText())) continue;

				if (!kws.contains(piece.getText())) {// 知識主題或者同義詞需要避免已經是現有知識主題或同義詞還被替換
					for (int i = piece.getStartPos(); i < piece.getEndPos(); i++) {
						if (absoluteTerritory[i]) {
							System.out.println("A matched piece by original text is invade the ABSOLUTE TERRITORY:"
									+ piece);
							continue pieceLoop;
						}
					}

					String newSuggest = suggest;
					if (syns != null && syns.size() == 1) {
						newSuggest = syns.get(0).getKeyword();
					}
					question = question.replaceAll(piece.getText(), newSuggest);
				}
				else {
					System.out.println("A matched piece by original text is already a keyword:" + piece);
				}
			}
		}
		return question;
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
}
