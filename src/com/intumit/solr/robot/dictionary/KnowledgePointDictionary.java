package com.intumit.solr.robot.dictionary;

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
import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.EvaluationLogEntity;
import com.intumit.solr.robot.KuromojiUtil;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.WiVoUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

/**
 * 簡易的自訂辭典斷詞工具 快速、高效
 *
 * @author herb
 */
public class KnowledgePointDictionary extends BaseChineseDictionary {
	public static int DEFAULT_SEG_SCORE = 160000;
	static Map<Integer, AhoCorasickDoubleArrayTrie<DictionaryDatabase>> trieMap = new HashMap<Integer, AhoCorasickDoubleArrayTrie<DictionaryDatabase>>();
	static Map<String, AhoCorasickDoubleArrayTrie<DictionaryDatabase>> trieCategoryMap = new HashMap<String, AhoCorasickDoubleArrayTrie<DictionaryDatabase>>();

	static Map<Integer, VoiceRecognitionOptimizer> fuzzyTrieMap = new HashMap<Integer, VoiceRecognitionOptimizer>();
	static Map<Integer, Set<String>> allSynKwsMap = new HashMap<Integer, Set<String>>();

    public static Set<String> DONT_TOUCH_ME = new HashSet<String>(Arrays.asList(new String[] {"台幣", "新台幣"}));

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    	fuzzyTrieMap.remove(tenantId);
    	allSynKwsMap.remove(tenantId);

    	WiVoUtil.clear(tenantId);

		Set<String> tcKeySet = new HashSet<String>(trieCategoryMap.keySet());

    	for (String tcKey: tcKeySet) {
			if (tcKey.startsWith("" + tenantId + "_")) {
				trieCategoryMap.remove(tcKey);
			}
		}
    }

    public static AhoCorasickDoubleArrayTrie<DictionaryDatabase> loadIfNull(Integer tenantId, String qaCategory) {
    	if (!trieMap.containsKey(tenantId)) {
    		Tenant t = Tenant.get(tenantId);
    		List<DictionaryDatabase> ddList = DictionaryDatabase.listByTenantId(tenantId);
    		
			if (ddList.size() == 0)
				return null;

    		AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = new AhoCorasickDoubleArrayTrie<DictionaryDatabase>();

    		// 建立 WiVo 知識點 VRO
			VoiceRecognitionOptimizer fuzzy = new VoiceRecognitionOptimizer(
					new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/t" + tenantId));
    		Set<String> fuzzyKwSet = new HashSet<String>();

	        TreeMap<String, DictionaryDatabase> map = new TreeMap<String, DictionaryDatabase>();
			for (DictionaryDatabase dd : ddList) {
	        	if (dd.isEnabled() && dd.getPurposeSet().contains(DictionaryDatabase.Purpose.KNOWLEDGE_POINT)) {
	        		map.put(dd.getKeyword(), dd);
	        		fuzzyKwSet.add(dd.getKeyword());

        			if (t != null && t.getLocale() == HitHotLocale.ja_JP) {  // 日文會加入平假名
            			String hira = new KuromojiUtil().segAndToHiragana(dd.getKeyword());
            			
            			if (!StringUtils.equalsIgnoreCase(dd.getKeyword(), hira) && !map.containsKey(hira)) {
            				System.out.println("Load KP [" + dd.getKeyword() + "] => Hiragana [" + hira + "].");
            				map.put(hira, dd);
            			}
        			}
	        		
	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, dd.getKeyword(), true);

	        		if (syns != null && syns.size() > 0) {
						for (String syn : syns) {
	        				if (!StringUtils.equalsIgnoreCase(syn, dd.getKeyword())) {
	        					map.put(syn, dd);
	        					fuzzyKwSet.add(syn);
	        				}
	        			}
	        		}
	        	}
	        }

			// 如果要連同義詞一起 WiVo 才跑下面這段
			/*
			for (SynonymKeyword sk: SynonymKeywordFacade.getInstance().listSynonymKeywords(tenantId)) {
				fuzzyKwSet.addAll(sk.getKeywordAndSynonymList());
			}
			*/

			if (map.size() == 0)
				return null;

	        trie.build(map);

			// 預設只比對完全同音，採用 MODE.MPS2 是包含幾聲的聲調都要相同。若要不管聲調則可採用 MODE.MPS2_NO_TONE
	        fuzzy.createDictionary(fuzzyKwSet, "kp", MODE.MPS2);
	        fuzzy.setFuzzyLevel(FuzzyLevel.LOW);
	        fuzzy.setPhoneticSimilarity(PhoneticSimilarity.NORMAL);
	        fuzzyTrieMap.put(tenantId, fuzzy);
	        trieMap.put(tenantId, trie);
    	}

		if (qaCategory == null) {
    		return trieMap.get(tenantId);
		} else {
			// 新增trieCategoryMap，for租戶-分類的知識點
    		if (!trieCategoryMap.containsKey(tenantId + "_" + qaCategory)) {
        		Tenant t = Tenant.get(tenantId);
    			List<DictionaryDatabase> ddList = DictionaryDatabase.listByTenantIdQaCategory(tenantId, qaCategory);
    			
				if (ddList.size() == 0) {
					trieCategoryMap.put(tenantId + "_" + qaCategory, trieMap.get(tenantId));
					return trieCategoryMap.get(tenantId + "_" + qaCategory);
				}
        		AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = new AhoCorasickDoubleArrayTrie<DictionaryDatabase>();
        		TreeMap<String, DictionaryDatabase> map = new TreeMap<String, DictionaryDatabase>();
				for (DictionaryDatabase dd : ddList) {
					if (dd.getPurposeSet().contains(DictionaryDatabase.Purpose.KNOWLEDGE_POINT)
							&& dd.getCategory().equals(qaCategory)) {
    		        	map.put(dd.getKeyword(), dd);

            			if (t != null && t.getLocale() == HitHotLocale.ja_JP) {
                			String hira = new KuromojiUtil().segAndToHiragana(dd.getKeyword());
                			
                			if (!StringUtils.equalsIgnoreCase(dd.getKeyword(), hira) && !map.containsKey(hira)) {
                				map.put(hira, dd);
                			}
            			}

    	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, dd.getKeyword(), false);

    	        		if (syns != null && syns.size() > 0) {
							for (String syn : syns) {
    	        				if (!StringUtils.equalsIgnoreCase(syn, dd.getKeyword()))
    	        					map.put(syn, dd);
    	        			}
    	        		}
    		        }
    		    }

				if (map.size() == 0)
					return null;

    	        trie.build(map);
    			trieCategoryMap.put(tenantId + "_" + qaCategory, trie);
    		}
    		return trieCategoryMap.get(tenantId + "_" + qaCategory);
    	}
    }

    public static DictionaryDatabase[] search(Integer tenantId, char[] charArray, String qaCategory) {
    	final List<DictionaryDatabase> results = new ArrayList<DictionaryDatabase>();
    	AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = loadIfNull(tenantId, qaCategory);
		if (trie == null)
			return results.toArray(new DictionaryDatabase[0]);

        final DictionaryDatabase[] wordNet = new DictionaryDatabase[charArray.length];
        final int[] lengthNet = new int[charArray.length];

		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<DictionaryDatabase>() {
            @Override
			public void hit(int begin, int end, DictionaryDatabase value) {
                int length = end - begin;
				if (length > lengthNet[begin]) {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });

		for (int offset = 0; offset < wordNet.length;) {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
			} else {
            	results.add(wordNet[offset]);
            	offset += lengthNet[offset];
            }
        }
        return results.toArray(new DictionaryDatabase[0]);
    }

    /**
     * 回傳句子當中是否有跟知識點同音的區段
     *
     * @param tenantId
     * @param question
     * @return 回傳是有找到的知識點的關鍵字（給知識點，而非原關鍵詞）
     */
    public static List<String> suggest(Integer tenantId, String question) {
    	loadIfNull(tenantId, null);
    	VoiceRecognitionOptimizer vo = fuzzyTrieMap.get(tenantId);
		if (vo == null)
			return new ArrayList<String>();

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
    	loadIfNull(tenantId, null);
    	VoiceRecognitionOptimizer vo = fuzzyTrieMap.get(tenantId);

		if (vo == null)
			return null;

    	Set<String> kpSet = DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT);

    	/*
    	 * 若需要同義詞 WiVo 才需要這段
    	Set<String> synSet = allSynKwsMap.get(tenantId);

    	if (synSet == null) {
    		synchronized (allSynKwsMap) {
    			if (!allSynKwsMap.containsKey(tenantId)) {
		    		synSet = new HashSet<String>();

		    		for (SynonymKeyword sk: SynonymKeywordFacade.getInstance().listSynonymKeywords(tenantId)) {
		    			synSet.addAll(sk.getKeywordAndSynonymList());
		    		}
		    		allSynKwsMap.put(tenantId, synSet);
    			}
    			else {
    		    	synSet = allSynKwsMap.get(tenantId);
    			}
    		}
    	}
    	*/

    	VROResult vor = vo.suggest(question);
    	List<String> suggests = vo.suggest(question).getSuggests();

		for (String suggest : suggests) {
        	List<Piece> pieces = vor.getMatchedPieces(suggest);
        	System.out.println("*******");
        	System.out.println(pieces);
        	System.out.println("*******");

			for (Piece piece : pieces) {
				if (isEatingWord(piece))
					continue;

				if (DONT_TOUCH_ME.contains(piece.getText()))
					continue;

        		if (!kpSet.contains(piece.getText())) {// 知識主題或者同義詞需要避免已經是現有知識主題或同義詞還被替換   && !synSet.contains(piece.getText())) {
        			question = question.replaceAll(piece.getText(), suggest);
				} else {
        			System.out.println("A matched piece by original text is knowledge point:" + piece);
        		}
        	}
    	}
    	return question;
    }

    public static void main(String[] args) {
    	HibernateUtil.init();
		MessageUtil.initialize();

		/* 批次把 EvaluationLogEntity 的問題全部拿出來斷詞放在文字檔裡頭（方便比較）*/
		int tenantId = 5;
		Tenant t = Tenant.get(tenantId);
		QAUtil qautil = QAUtil.getInstance(tenantId);

		int total = EvaluationLogEntity.countByNS(tenantId, QAUtil.CURRENT_EVALUATION_LOG_NS).intValue();
		int rows = 500;
		File out = new File("output-修正前_斷1.csv");

		for (int i=0; i < total; i+=rows) {
			List<EvaluationLogEntity> logs = EvaluationLogEntity.listByNS(tenantId, QAUtil.CURRENT_EVALUATION_LOG_NS, i, rows);

			for (EvaluationLogEntity log: logs) {
				String question = StringUtils.lowerCase(log.getQuestion());
				try {
					String nlpRes = nlp(t, qautil, question, false);
					String line = question + "\t" + nlpRes;
					System.out.println(line);
					FileUtils.writeStringToFile(out, line + "\n", true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		/*
		*/

		Forest userDefinedForest = new Forest();
		Forest ambiguityForest = new Forest();
		/*

		try {
			String ambiguityLibrary = MyStaticValue.ambiguityLibrary;
			if (StringUtils.isNotBlank(ambiguityLibrary)) {
				File file = new File(ambiguityLibrary);
				if (file.isFile() && file.canRead()) {
					try {
						ambiguityForest = Library.makeForest(ambiguityLibrary);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("init ambiguity  error :" + new File(ambiguityLibrary).getAbsolutePath() + " because : not find that file or can not to read !");
						e.printStackTrace();
					}
					System.out.println("init ambiguityLibrary ok!");
				} else {
					System.out.println("init ambiguity  warning :" + new File(ambiguityLibrary).getAbsolutePath() + " because : file not found or failed to read !");
				}
			}

			// 加载用户自定义词典
			String userLibrary = MyStaticValue.userLibrary;
			UserDefineLibrary.loadLibrary(userDefinedForest, userLibrary);


			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			List<WikiWord> wikis = WhiteWikiWordFacade.getInstance().listSynonymKeywords();

			for (WikiWord wiki: wikis) {
				//UserDefineLibrary.insertWord(syn.getPageTitle(), syn.getNature(), 2000);
				Library.insertWord(ambiguityForest, new Value(wiki.getPageTitle(), wiki.getPageTitle(), wiki.getNature()));
			}

			Set<String> kps = DictionaryDatabase.getDictionary(t.getId(), Purpose.KNOWLEDGE_POINT);

			for (String kp: kps) {
				//UserDefineLibrary.insertWord(syn.getPageTitle(), syn.getNature(), 2000);
				//Library.insertWord(ambiguityForest, new Value(kp, kp, "#UserDefined_KP"));
				Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue(kp, "#UserDefined_KP", 300001));
			}
			whiteSet.addAll(kps);

			List<AmbiguityDatabase> adbList = AmbiguityDatabase.listAll(t.getId());
			for (AmbiguityDatabase adb: adbList) {
				if (adb.isEnabled() && !whiteSet.contains(adb.getSentence()))
					Library.insertWord(ambiguityForest, new Value(adb.getSentence(), adb.getDisambiuationParts()));
			}

			List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listSynonymKeywords(t.getId());

			for (SynonymKeyword syn: syns) {
				if (!whiteSet.contains(syn.getKeyword())) {
					whiteSet.add(syn.getKeyword());
					Value value = qautil.guessNature(syn.getKeyword());
					System.out.println("Load Synonym Keyword:" + syn.getKeyword() + "/" + value);

					if (value == null) {
						Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue(syn.getKeyword(), "#UserDefined_SSK"));
					}
					else {
						try {
							if (StringUtils.startsWith(value.getParamers()[0], "#")) {
								value = QAUtil.toUserDefinedValue(syn.getKeyword(), value.getParamers()[0]);
							}
							else {
								value = QAUtil.toUserDefinedValue(syn.getKeyword(), "#UserDefined_SSK");
							}
							//else if (Integer.parseInt(value.getParamers()[1]) < 1000) value.getParamers()[1] = "1000";
						}
						catch (Exception ignore) {}
						Library.insertWord(userDefinedForest, value);
					}
				}

				for (String synStr: syn.getSynonymList()) {
					if (!whiteSet.contains(synStr)) {
						whiteSet.add(synStr);
						System.out.println("Load UserDefined:" + synStr);
						Value value = qautil.guessNature(synStr);
						System.out.println("Load Synonym:" + synStr + "/" + value);

						if (value == null) {
							Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue(synStr, "#UserDefined_SS"));
						}
						else {
							try {
								if (StringUtils.startsWith(value.getParamers()[0], "#")) {
									value = QAUtil.toUserDefinedValue(synStr, value.getParamers()[0]);
								}
								else {
									value = QAUtil.toUserDefinedValue(synStr, "#UserDefined_SS");
								}
								//else if (Integer.parseInt(value.getParamers()[1]) < 1000) value.getParamers()[1] = "1000";
							}
							catch (Exception ignore) {}
							Library.insertWord(userDefinedForest, value);
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		*/
		//System.out.println(nlp(t, qautil, "汽車貸款如何辦理塗銷", false));

		//Analysis a = new UserDefineAnalysis(userDefinedForest);
		//a.setAmbiguityForest(ambiguityForest);
		//System.out.println(a.doParse("汽車貸款如何辦理塗銷").toString());

		/*
		String userLibrary = MyStaticValue.userLibrary;
		UserDefineLibrary.loadLibrary(userDefinedForest, userLibrary);

		Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue("請", "nz#SYN"));
		Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue("客服中心", "nz#SYN"));
		Library.insertWord(userDefinedForest, QAUtil.toUserDefinedValue("客服", "nz#SYN"));
		Assert.assertEquals("[請/r, 客服中心/nz#SYN, 匯/v, 外幣/n, 到/v, 大陸/ns, 費用/n, 是/vshi, 多少/ry]",
				UserDefineAnalysis.parse("請客服中心匯外幣到大陸費用是多少", userDefinedForest).toString());

		System.out.println(nlp(t, qautil, "客服中心匯外幣到大陸費用是多少", false));
		System.out.println(nlp(t, qautil, "請客服中心匯外幣到大陸費用是多少", false));
		DictionaryDatabase[] results = KnowledgePointDictionary.search(WiseSystemConfig.get().getDefaultTenantId(),
				"我今天早上用了信用卡搭車到新店市公所站旁邊的銀行換存摺並且處理外匯".toCharArray(), null);

		for (DictionaryDatabase result : results) {
    		System.out.println("(" + result.getId() + "). " + result.getKeyword() + ":" + result.getPurposes());
    	}

		List<String> suggests = KnowledgePointDictionary.suggest(WiseSystemConfig.get().getDefaultTenantId(),
				"我今天早上用了信用卡搭車到新店市公所站旁邊的銀行喚純蟄並且處理外匯");
		for (String suggest : suggests) {
    		System.out.println("Suggest:" + suggest);
    	}
		String suggestQ = KnowledgePointDictionary.suggestFullQuestion(WiseSystemConfig.get().getDefaultTenantId(),
				"我今天早上用了信用卡搭車到新店市公所站旁邊的銀行喚純蟄並且處理外匯");
		System.out.println("Suggest Q:" + suggestQ);*/
	}

	static String nlp(Tenant tenant, QAUtil qautil, String question, boolean replaceSyn) {
		StringBuilder notNM = new StringBuilder();

		List<org.ansj.domain.Term> parse = QAUtil.USE_NLPANALYSIS
				? org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + question + " ")).getTerms()
				: qautil.getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + question + " ")).getTerms();
		//parse = org.ansj.util.FilterModifWord.modifResult(parse) ;
		boolean allEnglish = true;
		boolean inEnMode = false;
		String englishPhraseStack = "";
		List<String> queue = new ArrayList<String>();

		for (int i=0; i < parse.size(); i++) {
			org.ansj.domain.Term t = parse.get(i);

			if ((i == 0 || i+1 == parse.size()) && StringUtils.isBlank(t.getName()))
				continue;

			String nature = t.getNatureStr();

			if (StringUtils.startsWith(nature, "mt")) {
				// 避免被下一個 elseif 到
			}
			else if (StringUtils.startsWith(nature, "m") && StringUtils.endsWithAny(t.getRealName(), QAUtil.TEMPORAL_STR_ENDING)) {
				t.setNature(new org.ansj.domain.Nature("t"));
				nature = "t";
			}

			if (StringUtils.startsWith(nature, "m") && !QAUtil.NOT_A_NUMBER_STR_SET.contains(t.getRealName())) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER), t.getRealName(), CategorizedKeyValuePair.Type.NUMBER);

				String convertable = CategorizedValueParser.toNumberRangeQuery(kv);

				if (convertable == null) {
					nature = "m?";
				}
			}
			else if (StringUtils.startsWith(nature, "t")) {
				CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL), t.getRealName(), CategorizedKeyValuePair.Type.TEMPORAL);
				String convertable = CategorizedValueParser.toDateRangeQuery(kv);

				if (convertable == null) {
					nature = "t?";
				}
			}

			if (StringUtils.equalsIgnoreCase(nature, "en")) {
				if (inEnMode) {
					englishPhraseStack += " " + t.getRealName();
				}
				else {
					inEnMode = true;
					englishPhraseStack = t.getRealName();
				}
				if (i < parse.size() - 1) continue;
			}
			else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"mt"})) {
				t.setRealName("{{$NUM}}|{{$DATE}}|");
			}
			else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"m"})) {
				t.setRealName("{{$NUM}}|");
			}
			else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"t"})) {
				t.setRealName("{{$DATE}}|");
			}
			else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
				// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
				if (i < parse.size() - 1) continue;
			}
			else {
				allEnglish = false;

				if (inEnMode) {
					queue.add(englishPhraseStack);
					inEnMode = false;
				}
			}

			if (!inEnMode && (nature != null && !"null".equals(nature) && StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
				String kw = t.getRealName();

				try {
					if (tenant.isForceIgnoreCharacter(kw)) {
						kw += "**";
					}
				}
				catch (Exception ignore) {}

				queue.add(kw);
			}
			else {
				String kw = t.getRealName();
				queue.add(kw + "**");
			}
			if (inEnMode && !(i < parse.size() - 1)) {
				queue.add(englishPhraseStack);
			}

			while (queue.size() > 0) {
				String kw = queue.remove(0);
				if ("nw".equals(nature)) {
					continue;
				}

				boolean replacedBySyn = false;
				if (replaceSyn) {
					if (kw.indexOf("**") != -1) {
						continue;
					}

					List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenant.getId(), kw);

					if (synonymsAll.size() > 0) {
						replacedBySyn = true;
						String newKw = "";

						for (SynonymKeyword syn: synonymsAll) {
							if (newKw.length() > 0) {
								newKw += "|";
							}

							newKw += syn.getKeyword();
						}

						kw = newKw;
					}
				}

				if (notNM.length() > 0) {
					//notNM.append(" / ");
				}

				if (kw.length() > 0) {
					if (replaceSyn) {
						if (kw.length() > 1 || replacedBySyn || StringUtils.startsWith(nature, "#")) {
							notNM.append("(");
							notNM.append(kw);
							notNM.append(")");
						}
						else {
							notNM.append(kw);
						}
					}
					else {
						notNM.append("(");
						notNM.append(kw);// + ":[" + nature + "]");
						notNM.append(")");
					}
				}
			}
		}

		return notNM.toString();
    }

	static final char[] PHONETIC_MAIN = "ㄅㄆㄇㄈㄉㄊㄋㄌㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".toCharArray();

	public static boolean isEatingWord(Piece p) {
		String mb = p.getMatchedBy();
		String pi = p.getPinyin();

		if (pi.endsWith(mb) || pi.startsWith(mb)) {
			String remain = pi.replaceAll(mb, "");

			if (StringUtils.containsAny(remain, PHONETIC_MAIN))
				return true;
		}

		return false;
	}
}
