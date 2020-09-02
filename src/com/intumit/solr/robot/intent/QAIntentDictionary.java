package com.intumit.solr.robot.intent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.elasticsearch.common.lang3.StringUtils;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.WiVoUtil;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;

/**
 * 簡易的自訂辭典斷詞工具
 * 快速、高效
 * 
 * @author herb
 */
public class QAIntentDictionary extends BaseChineseDictionary implements Serializable, Cloneable
{
	static Map<Integer, AhoCorasickDoubleArrayTrie<Set<QAIntent>>> trieMap = 
			new HashMap<Integer, AhoCorasickDoubleArrayTrie<Set<QAIntent>>>();
	static Map<String, AhoCorasickDoubleArrayTrie<Set<QAIntent>>> trieCategoryMap = 
			new HashMap<String, AhoCorasickDoubleArrayTrie<Set<QAIntent>>>();

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    	WiVoUtil.clear(tenantId);
    }
    
    public static AhoCorasickDoubleArrayTrie<Set<QAIntent>> loadIfNull(Integer tenantId, String category) {
    	if (!trieMap.containsKey(tenantId)) {
    		Tenant t = Tenant.get(tenantId);
    		List<QAIntent> ddList = QAIntent.listByTenantId(tenantId);
    		if (ddList.size() == 0) return null;
    		AhoCorasickDoubleArrayTrie<Set<QAIntent>> trie = new AhoCorasickDoubleArrayTrie<Set<QAIntent>>();
    		
	        TreeMap<String, Set<QAIntent>> map = new TreeMap<String, Set<QAIntent>>();
	        for (QAIntent dd: ddList) {
	        	if (dd.isEnabled()) {
	        		String[] keywords = StringUtils.split(dd.getKeywords(), ",");
	        		
	        		for (String keyword: keywords) {
    					if (map.containsKey(keyword)) map.get(keyword).add(dd);
						else {
							Set<QAIntent> set = new HashSet<QAIntent>();
							set.add(dd);
							map.put(keyword, set);
						}
			        	
		        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, keyword, false);
		        		
		        		if (syns != null && syns.size() > 0) {
		        			for (String syn: syns) {
		        				if (!StringUtils.equalsIgnoreCase(syn, keyword)) {
		        					if (map.containsKey(keyword)) map.get(keyword).add(dd);
		    						else {
		    							Set<QAIntent> set = new HashSet<QAIntent>();
		    							set.add(dd);
		    							map.put(keyword, set);
		    						}
		        				}
		        			}
		        		}
	        		}
	        	}
	        }

		    if (map.size() == 0) return null;
		    
	        trie.build(map);
	        
	        trieMap.put(tenantId, trie);
    	}
    	if (category == null) {
    		return trieMap.get(tenantId);
    	}
    	else {
    		//新增trieCategoryMap，for租戶-分類的知識點
    		if (!trieCategoryMap.containsKey(tenantId + "_" + category)) {
    			List<QAIntent> ddList = QAIntent.listByTenantIdQaCategory(tenantId, category);
				if (ddList.size() == 0) {
					trieCategoryMap.put(tenantId + "_" + category, trieMap.get(tenantId));
					return trieCategoryMap.get(tenantId + "_" + category);
				}
        		AhoCorasickDoubleArrayTrie<Set<QAIntent>> trie = new AhoCorasickDoubleArrayTrie<Set<QAIntent>>();
        		TreeMap<String, Set<QAIntent>> map = new TreeMap<String, Set<QAIntent>>();
    		    for (QAIntent dd: ddList) {
    		     	if (dd.getCategory().equals(category)) {
    	        		String[] keywords = StringUtils.split(dd.getKeywords(), ",");
    	        		
    	        		for (String keyword: keywords) {
	    	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, keyword, false);
	    	        		
	    	        		if (syns != null && syns.size() > 0) {
	    	        			for (String syn: syns) {
	    	        				if (!StringUtils.equalsIgnoreCase(syn, keyword)) {
	    	        					if (map.containsKey(keyword)) map.get(keyword).add(dd);
	    	    						else {
	    	    							Set<QAIntent> set = new HashSet<QAIntent>();
	    	    							set.add(dd);
	    	    							map.put(keyword, set);
	    	    						}
	    	        				}
	    	        			}
	    	        		}
    	        		}
    		        }
    		    }

    		    if (map.size() == 0) return null;
    		    
    			trie.build(map);
    			
    			trieCategoryMap.put(tenantId + "_" + category, trie);
    		}
    		return trieCategoryMap.get(tenantId + "_" + category);
    	}
    }

    public static Set<QAIntent>[] search(Integer tenantId, char[] charArray, String qaCategory) {
    	final List<Set<QAIntent>> results = new ArrayList<Set<QAIntent>>();
    	AhoCorasickDoubleArrayTrie<Set<QAIntent>> trie = loadIfNull(tenantId, qaCategory);
    	if (trie == null) return (Set<QAIntent>[])results.toArray(new Set<?>[0]);
    	
        final Object[] wordNet = new Object[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<QAIntent>>()
        {
            @Override
            public void hit(int begin, int end, Set<QAIntent> value)
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
            	results.add((Set<QAIntent>)wordNet[offset]);
            	offset += lengthNet[offset];
            }
        }
        return (Set<QAIntent>[])results.toArray(new Set<?>[0]);
    }
    
    public static void main(String[] args) {
    	HibernateUtil.init();
    	Set<QAIntent>[] results = QAIntentDictionary.search(7, "我要開卡，你這個混蛋".toCharArray(), null);
    	
    	for (Set<QAIntent> resultSet: results) {
        	for (QAIntent result: resultSet) {
        		System.out.println("(" + result.getId() + "). " + result.getKeywords() + ":" + result.getTag()); 
        	}
    	}
    }
}
