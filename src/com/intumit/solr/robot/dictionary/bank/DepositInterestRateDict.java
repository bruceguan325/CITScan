package com.intumit.solr.robot.dictionary.bank;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.fuzzy.MODE;
import com.intumit.android.search.fuzzy.PhoneticSimilarity;
import com.intumit.android.search.fuzzy.Piece;
import com.intumit.android.search.fuzzy.VROResult;
import com.intumit.android.search.fuzzy.VoiceRecognitionOptimizer;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

@Entity
public class DepositInterestRateDict implements Serializable {
	private static final String ALL = "ALL";
	
	public static enum Purpose {
		DEPOSIT_TYPE("存款類型"),
		CURRENCY_TYPE("幣別"),
		TIME_RANGE("期別"),
		RATE("利率"),
		OTHERS("其他相關輔助詞彙"),
		;

		String desc;
		Purpose(String desc) {
			this.desc = desc;
		}
		public String getDesc() {
			return desc;
		}
	}

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="keyword")
	private String keyword;

	@Index(name="purposes")
	private String purposes;

	@Index(name="enabled")
	private boolean enabled;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public String getPurposes() {
		return StringUtils.strip(purposes, ",");
	}
	public void setPurposes(String purposes) {
		this.purposes = purposes;
	}
	public Set<Purpose> getPurposeSet() {
		Set<Purpose> set = new HashSet<Purpose>();
		for (String pstr: StringUtils.split(purposes, ",")) {
			try {
				Purpose p = Purpose.valueOf(pstr);
				set.add(p);
			}
			catch (Exception ignored) {}
		}
		return set;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	@Override
	public String toString() {
		return "DepositInterestRateDict [id=" + id + ", keyword=" + keyword + ", purpose=" + purposes
				+ ", enabled=" + enabled + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DepositInterestRateDict other = (DepositInterestRateDict) obj;
		if (enabled != other.enabled) return false;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (keyword == null) {
			if (other.keyword != null) return false;
		}
		else if (!keyword.equals(other.keyword)) return false;
		return true;
	}
	
	public static synchronized DepositInterestRateDict get(long id) {
		try {
			return (DepositInterestRateDict)HibernateUtil.getSession().get(DepositInterestRateDict.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized void save(Integer tenantId, String sentence, String purposes,
			boolean enabled, String category) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DepositInterestRateDict word = new DepositInterestRateDict();
			word.setTenantId(tenantId);
			word.setKeyword(sentence.toLowerCase());
			word.setPurposes(purposes);
			word.setEnabled(enabled);
			ses.saveOrUpdate(word);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	public static synchronized void update(Integer tenantId, String id, String sentence,
			String purposes, boolean enabled, String category) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DepositInterestRateDict oldWord = get(lid);
			DepositInterestRateDict newWord = get(lid);

			if (newWord.getTenantId() == tenantId) {

				newWord.setId(lid);
				newWord.setKeyword(sentence.toLowerCase());
				newWord.setPurposes(purposes);
				newWord.setEnabled(enabled);

				ses.update(newWord);
				tx.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(Integer tenantId, String id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			long lid = Long.parseLong(id);
			DepositInterestRateDict word = get(lid);

			if (word != null) {
				ses.delete(word);
				tx.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized DepositInterestRateDict get(Integer tenantId, String sentence) {
		List<DepositInterestRateDict> l = list(tenantId, sentence, true, null);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<DepositInterestRateDict> listAll() {
		return list(null, null, false, null);
	}

	public static List<DepositInterestRateDict> listByTenantId(Integer tenantId) {
		return list(tenantId, null, false, null);
	}

	public static synchronized List<DepositInterestRateDict> search(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, null);
	}

	private static List list(Integer tenantId, String query, boolean fullMatch, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DepositInterestRateDict.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(fullMatch
								? Restrictions.eq("keyword", searchKeyword)
								: Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("keyword", searchKeyword)
									: Restrictions.like("keyword", searchKeyword, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
			}
			else {
			}

			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static Set<String> getDictionary(Integer tenantId, Purpose p) {
		return getFromCache(tenantId, p);
	}

	static Map<Integer, Map<Purpose, Set<String>>> cache = new HashMap<Integer, Map<Purpose, Set<String>>>();
	static Map<Integer, Set<String>> cache4All = new HashMap<Integer, Set<String>>();

	protected static void clearCache(Integer tenantId) {
		synchronized (cache) {
			if (cache.containsKey(tenantId)) {
				cache.remove(tenantId);
			}
			
			cache4All.remove(tenantId);
		}
	}
	private static Set<String> getFromCache(Integer tenantId, Purpose p) {
		if (p == null) {
			if (!cache4All.containsKey(tenantId)) {
				initCache(tenantId);
			}
			return cache4All.get(tenantId);
		}
		else {
			if (!cache.containsKey(tenantId)) {
				initCache(tenantId);
			}
			return cache.get(tenantId).get(p);
		}
	}
	private synchronized static void initCache(Integer tenantId) {
		if (cache.containsKey(tenantId) && cache4All.containsKey(tenantId)) { return; }
		
		Set<String> all = new HashSet<String>();
		
		Map<Purpose, Set<String>> ccc = new HashMap<Purpose, Set<String>>();
		for (Purpose p: Purpose.values()) {
			ccc.put(p, new HashSet<String>());
		}

		List<DepositInterestRateDict> l = listByTenantId(tenantId);
		for (DepositInterestRateDict dd: l) {
			if (dd.isEnabled()) {
				for (Purpose p: dd.getPurposeSet()) {
					ccc.get(p).add(dd.getKeyword());
					all.add(dd.getKeyword());
				}
			}
		}

		cache.put(tenantId, ccc);
		cache4All.put(tenantId, all);
	}
	
	
	
	static Map<Integer, AhoCorasickDoubleArrayTrie<DepositInterestRateDict>> trieMap = new HashMap<Integer, AhoCorasickDoubleArrayTrie<DepositInterestRateDict>>();
	static Map<Integer, VoiceRecognitionOptimizer> fuzzyTrieMap = new HashMap<Integer, VoiceRecognitionOptimizer>();
	static Map<Integer, Set<String>> allSynKwsMap = new HashMap<Integer, Set<String>>();

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    	fuzzyTrieMap.remove(tenantId);
    	allSynKwsMap.remove(tenantId);
    }
    
    public static AhoCorasickDoubleArrayTrie<DepositInterestRateDict> loadIfNull(Integer tenantId) {
    	if (!trieMap.containsKey(tenantId)) {
    		Tenant t = Tenant.get(tenantId);
    		List<DepositInterestRateDict> ddList = DepositInterestRateDict.listByTenantId(tenantId);
			if (ddList.size() == 0)
				return null;
    		
    		AhoCorasickDoubleArrayTrie<DepositInterestRateDict> trie = new AhoCorasickDoubleArrayTrie<DepositInterestRateDict>();

    		// 建立 WiVo 知識點 VRO
			VoiceRecognitionOptimizer fuzzy = new VoiceRecognitionOptimizer(
					new File(WiSeEnv.getHomePath() + "/dict/fuzzydata_bank/t" + tenantId));
    		Set<String> fuzzyKwSet = new HashSet<String>();
    		
	        TreeMap<String, DepositInterestRateDict> map = new TreeMap<String, DepositInterestRateDict>();
			for (DepositInterestRateDict dd : ddList) {
	        	if (dd.isEnabled()) {
	        		map.put(dd.getKeyword(), dd);
	        		
	        		if (!dd.getPurposeSet().contains(Purpose.OTHERS))
	        			fuzzyKwSet.add(dd.getKeyword());
	        		
	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, dd.getKeyword(), true);
	        		
	        		if (syns != null && syns.size() > 0) {
						for (String syn : syns) {
	        				if (!StringUtils.equalsIgnoreCase(syn, dd.getKeyword())) {
	        					map.put(syn, dd);
	        					
	        	        		if (!dd.getPurposeSet().contains(Purpose.OTHERS))
	        	        			fuzzyKwSet.add(syn);
	        				}
	        			}
	        		}
	        	}
	        }
			
			// 如果要連同義詞一起 WiVo 才跑下面這段
			/*for (SynonymKeyword sk: SynonymKeywordFacade.getInstance().listSynonymKeywords(tenantId)) {
				fuzzyKwSet.addAll(sk.getKeywordAndSynonymList());
			}*/

			if (map.size() == 0)
				return null;
		    
	        trie.build(map);
	        
			// 預設只比對完全同音，採用 MODE.MPS2 是包含幾聲的聲調都要相同。若要不管聲調則可採用 MODE.MPS2_NO_TONE
	        fuzzy.createDictionary(fuzzyKwSet, "dir", MODE.MPS2);
	        fuzzy.setFuzzyLevel(FuzzyLevel.DEFAULT);
	        fuzzy.setPhoneticSimilarity(PhoneticSimilarity.FLEXIBLE);
	        fuzzyTrieMap.put(tenantId, fuzzy);
	        trieMap.put(tenantId, trie);
	    	return trie;
    	}
    	return trieMap.get(tenantId);
    }

    public static DepositInterestRateDict[] search(QAContext ctx, Integer tenantId, char[] charArray) {
    	final List<DepositInterestRateDict> results = new ArrayList<DepositInterestRateDict>();
    	AhoCorasickDoubleArrayTrie<DepositInterestRateDict> trie = loadIfNull(tenantId);
		if (trie == null)
			return results.toArray(new DepositInterestRateDict[0]);
    	
        final DepositInterestRateDict[] wordNet = new DepositInterestRateDict[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
		trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<DepositInterestRateDict>() {
            @Override
			public void hit(int begin, int end, DepositInterestRateDict value) {
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
		
		DepositInterestRateDict timeRange = findTimeRange(new String(charArray));
		if (timeRange != null) {
			Iterator<DepositInterestRateDict> itr = results.iterator();
			
			while (itr.hasNext()) {
				DepositInterestRateDict i = itr.next();
				
				if (StringUtils.contains(timeRange.getKeyword(), i.getKeyword())) {
					itr.remove();
				}
			}
			
			results.add(timeRange); 
		}

		DepositInterestRateDict rate = findInterestRate(new String(charArray));
		if (rate != null) {
			ctx.setRequestAttribute("INTEREST_RATE", rate.getKeyword());
			results.add(0, rate); 
		}
		
        return results.toArray(new DepositInterestRateDict[0]);
    }
    
    public static DepositInterestRateDict findInterestRate(String currentQuestion) {
		Matcher m = Pattern.compile("[0-9]*\\.?[0-9]+%?").matcher(currentQuestion);
		while (m.find()) {
			String ir = m.group();
			
			if (ir.indexOf("%") == -1 && ir.indexOf(".") == -1)
				continue;
			
			DepositInterestRateDict c = new DepositInterestRateDict();
			c.setKeyword(ir);
			c.setPurposes(Purpose.RATE.name());
			c.setEnabled(true);
			
			return c;
		}
		return null;
    }

	static Pattern p = Pattern.compile("\\d+(年|個月|週|天|季|旬|周|月|禮拜|個禮拜)");
	public static DepositInterestRateDict findTimeRange(String currentQuestion) {
		Matcher m = p.matcher(currentQuestion);
		if (m.find()) {
			DepositInterestRateDict c = new DepositInterestRateDict();
			c.setKeyword(m.group(0));
			c.setPurposes(Purpose.TIME_RANGE.name());
			c.setEnabled(true);
			
			return c;
		}
		return null;
	}

    /**
     * 回傳句子當中是否有跟知識點同音的區段
     * 
     * @param tenantId
     * @param question
     * @return 回傳是有找到的知識點的關鍵字（給知識點，而非原關鍵詞）
     */
    public static List<String> suggest(Integer tenantId, String question) {
    	loadIfNull(tenantId);
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
    	loadIfNull(tenantId);
    	VoiceRecognitionOptimizer vo = fuzzyTrieMap.get(tenantId);
    	
		if (vo == null)
			return null;

    	Set<String> kpSet = getDictionary(tenantId, null);
    	
    	/* 
    	 * 若需要同義詞 WiVo 才需要這段
    	*/
    	Set<String> synSet = allSynKwsMap.get(tenantId);
    	
    	if (synSet == null) {
    		synchronized (allSynKwsMap) {
    			if (!allSynKwsMap.containsKey(tenantId)) {
		    		synSet = new HashSet<String>();
		    		
		    		for (SynonymKeyword sk: SynonymKeywordFacade.getInstance().listAll(tenantId)) {
		    			synSet.addAll(sk.getKeywordAndSynonymList());
		    		}
		    		allSynKwsMap.put(tenantId, synSet);
    			}
    			else {
    		    	synSet = allSynKwsMap.get(tenantId);
    			}
    		}
    	}
    			
    	VROResult vor = vo.suggest(question);
    	List<String> suggests = vo.suggest(question).getSuggests();
    	Set<String> alreadySub = new HashSet<String>();
    	
		for (String suggest : suggests) {
        	List<Piece> pieces = vor.getMatchedPieces(suggest);
        	
			for (Piece piece : pieces) {
				if (KnowledgePointDictionary.isEatingWord(piece)) 
					continue;
				
				if (alreadySub.contains(piece.getText()))
					continue;
				
				if (synSet.contains(piece.getText()))
					continue;
				
				if (KnowledgePointDictionary.DONT_TOUCH_ME.contains(piece.getText())) 
					continue;
				
        		if (!kpSet.contains(piece.getText()) && !StringUtils.contains(piece.getText(), suggest)) {// 知識主題或者同義詞需要避免已經是現有知識主題或同義詞還被替換   && !synSet.contains(piece.getText())) {
        			question = question.replaceAll(piece.getText(), suggest);
        			alreadySub.add(piece.getText());
				} else {
        			System.out.println("A matched piece by original text is knowledge point:" + piece);
        		}
        	}
    	}
    	return question;
    }
    
}
