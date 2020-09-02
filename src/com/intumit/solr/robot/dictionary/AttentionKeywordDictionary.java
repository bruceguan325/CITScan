package com.intumit.solr.robot.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.elasticsearch.common.lang3.StringUtils;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.systemconfig.WiseSystemConfig;

/**
 * 簡易的自訂辭典斷詞工具
 * 快速、高效
 * 
 * @author herb
 */
public class AttentionKeywordDictionary extends BaseChineseDictionary
{
	static Map<Integer, AhoCorasickDoubleArrayTrie<DictionaryDatabase>> trieMap = 
			new HashMap<Integer, AhoCorasickDoubleArrayTrie<DictionaryDatabase>>();
	static Map<String, AhoCorasickDoubleArrayTrie<DictionaryDatabase>> trieCategoryMap = 
			new HashMap<String, AhoCorasickDoubleArrayTrie<DictionaryDatabase>>();

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    }
    
    public static AhoCorasickDoubleArrayTrie<DictionaryDatabase> loadIfNull(Integer tenantId, String qaCategory) {
    	if (!trieMap.containsKey(tenantId)) {
    		Tenant t = Tenant.get(tenantId);
    		List<DictionaryDatabase> ddList = DictionaryDatabase.listByTenantId(tenantId);
    		if (ddList.size() == 0) return null;
    		AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = new AhoCorasickDoubleArrayTrie<DictionaryDatabase>();
    		
	        TreeMap<String, DictionaryDatabase> map = new TreeMap<String, DictionaryDatabase>();
	        for (DictionaryDatabase dd: ddList) {
	        	if (dd.isEnabled() && (dd.getPurposeSet().contains(DictionaryDatabase.Purpose.BLACKLIST) || dd.getPurposeSet().contains(DictionaryDatabase.Purpose.MARKETING))) {
	        		map.put(dd.getKeyword(), dd);
		        	
	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, dd.getKeyword(), false);
	        		
	        		if (syns != null && syns.size() > 0) {
	        			for (String syn: syns) {
	        				if (!StringUtils.equalsIgnoreCase(syn, dd.getKeyword()))
	        					map.put(syn, dd);
	        			}
	        		}
	        	}
	        }

		    if (map.size() == 0) return null;
		    
	        trie.build(map);
	        
	        trieMap.put(tenantId, trie);
    	}
    	if (qaCategory == null) {
    		return trieMap.get(tenantId);
    	}
    	else {
    		//新增trieCategoryMap，for租戶-分類的知識點
    		if (!trieCategoryMap.containsKey(tenantId + "_" + qaCategory)) {
    			List<DictionaryDatabase> ddList = DictionaryDatabase.listByTenantIdQaCategory(tenantId, qaCategory);
        		if (ddList.size() == 0) {
        			trieCategoryMap.put(tenantId + "_" + qaCategory, trieMap.get(tenantId));
        			return trieCategoryMap.get(tenantId + "_" + qaCategory);
        		}
        		AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = new AhoCorasickDoubleArrayTrie<DictionaryDatabase>();
        		TreeMap<String, DictionaryDatabase> map = new TreeMap<String, DictionaryDatabase>();
    		    for (DictionaryDatabase dd: ddList) {
    		     	if ((dd.getPurposeSet().contains(DictionaryDatabase.Purpose.BLACKLIST) || dd.getPurposeSet().contains(DictionaryDatabase.Purpose.MARKETING)) && dd.getCategory().equals(qaCategory)) {
    		        	map.put(dd.getKeyword(), dd);
    		        	
    	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, dd.getKeyword(), false);
    	        		
    	        		if (syns != null && syns.size() > 0) {
    	        			for (String syn: syns) {
    	        				if (!StringUtils.equalsIgnoreCase(syn, dd.getKeyword()))
    	        					map.put(syn, dd);
    	        			}
    	        		}
    		        }
    		    }

    		    if (map.size() == 0) return null;
    		    
    			trie.build(map);
    			
    			trieCategoryMap.put(tenantId + "_" + qaCategory, trie);
    		}
    		return trieCategoryMap.get(tenantId + "_" + qaCategory);
    	}
    }

    public static DictionaryDatabase[] search(Integer tenantId, char[] charArray, String qaCategory) {
    	final List<DictionaryDatabase> results = new ArrayList<DictionaryDatabase>();
    	AhoCorasickDoubleArrayTrie<DictionaryDatabase> trie = loadIfNull(tenantId, qaCategory);
    	if (trie == null) return results.toArray(new DictionaryDatabase[0]);
    	
        final DictionaryDatabase[] wordNet = new DictionaryDatabase[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<DictionaryDatabase>()
        {
            @Override
            public void hit(int begin, int end, DictionaryDatabase value)
            {
                int length = end - begin;
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	results.add(wordNet[offset]);
            	offset += lengthNet[offset];
            }
        }
        return results.toArray(new DictionaryDatabase[0]);
    }
    
    public static void main(String[] args) {
    	HibernateUtil.init();
    	DictionaryDatabase[] results = AttentionKeywordDictionary.search(7, "我要開卡，你這個混蛋".toCharArray(), null);
    	
    	for (DictionaryDatabase result: results) {
    		System.out.println("(" + result.getId() + "). " + result.getKeyword() + ":" + result.getPurposes()); 
    	}
    }
}
