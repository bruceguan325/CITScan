package com.intumit.solr.robot.dictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAAltBuild;
import com.intumit.solr.robot.QASaver;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.systemconfig.WiseSystemConfig;

/**
 * 簡易的自訂辭典斷詞工具
 * 快速、高效
 * 
 * @author herb
 */
public class EssentialKeywordDictionary extends BaseChineseDictionary
{
	static Map<Integer, AhoCorasickDoubleArrayTrie<String>> trieMap = 
			new HashMap<Integer, AhoCorasickDoubleArrayTrie<String>>();
	static Map<Integer, Set<String>> listCacheMap = new HashMap<Integer, Set<String>>();
	static Set<Integer> expiredCache = new HashSet<Integer>(); // 要清除 cache 前先塞入 expiredCache，下次要 reloadCache 前先比對是否有差異

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    	expiredCache.add(tenantId);
//    	listCacheMap.remove(tenantId);
    }
    
    public static Set<String> eksSet(Integer tenantId) {
    	try {
	    	Set<String> oldEks = null;
	    	
	    	if (expiredCache.contains(tenantId)) {
	    		expiredCache.remove(tenantId);
	    		oldEks = listCacheMap.get(tenantId);
	    		listCacheMap.remove(tenantId);
	    	}
	    	
	    	Set<String> eks = listCacheMap.get(tenantId);
	    	
	    	if (eks == null) {
	    		eks = new HashSet<>();
	    		eks.addAll(getFromIndex(Tenant.get(tenantId).getCoreServer4Write()));
	    		listCacheMap.put(tenantId, eks);
	    	}
	    	
	    	// 確認舊的 cache 跟新的 cache 是否有差異，若有差異則 rebuild 使用者辭典
	    	if (oldEks != null) {
	    		if (!org.apache.commons.collections.CollectionUtils.isEqualCollection(oldEks, eks)) {
	    			QAUtil.getInstance(tenantId).forceRebuildUserDefiniedDictionary();
	    		}
	    	}
	    	
	    	return eks;
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    	
    	return null;
    }
    
    public synchronized static AhoCorasickDoubleArrayTrie<String> loadIfNull(Integer tenantId) {
    	if (!trieMap.containsKey(tenantId)) {
    		Tenant t = Tenant.get(tenantId);
    		List<String> cdSet = getFromIndex(t.getCoreServer());
    		
    		if (cdSet.size() == 0) {
    			trieMap.put(tenantId, null);
    			return null;
    		}
    		
    		AhoCorasickDoubleArrayTrie<String> trie = new AhoCorasickDoubleArrayTrie<String>();
	        TreeMap<String, String> map = new TreeMap<String, String>();
	        for (String cd: cdSet) {
        		map.put(cd, cd);
	        }
		    if (map.size() == 0) {
    			trieMap.put(tenantId, null);
		    	return null;
		    }
		    
	        trie.build(map);
	        trieMap.put(tenantId, trie);
    	}
    	return trieMap.get(tenantId);
    }

    public static String[] search(Integer tenantId, char[] charArray) {
    	final Set<String> results = new TreeSet<String>();
    	AhoCorasickDoubleArrayTrie<String> trie = loadIfNull(tenantId);
    	if (trie == null) return (String[])results.toArray(new String[0]);
    	
        final String[] wordNet = new String[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<String>()
        {
            @Override
            public void hit(int begin, int end, String value)
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
        return (String[])results.toArray(new String[0]);
    }

    public static Set<String> expand(Integer tenantId, final char[] charArray) {
    	final Set<String> results = new HashSet<String>();
    	AhoCorasickDoubleArrayTrie<String> trie = loadIfNull(tenantId);
    	if (trie == null) return results;
    	
    	final Collection<String> keywords = new TreeSet<String>();
        final String[] wordNet = new String[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<String>()
        {
            @Override
            public void hit(int begin, int end, String value)
            {
                int length = end - begin;
                if (length > charArray.length || begin >= charArray.length)
                	return;
                
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });
        
        List<Integer> offsets = new ArrayList<Integer>();
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	keywords.add(wordNet[offset]);
            	offsets.add(offset);
            	offset += lengthNet[offset];
            }
        }
    	
        if (keywords.size() > 0) {
	    	QAAltBuild b = new QAAltBuild();
	    	b.setTenantId(tenantId);
			b.setExpandSynonyms(false);
			b.setQAltTpls("[" + StringUtils.join(keywords, "][") + "]");
			QASaver qs = new QASaver(b, new QA());
			
	        Collection<TreeSet<String>> leks = qs.expandEssentialKeywords();
	        
	        for (TreeSet<String> eks: leks) {
	        	results.add(StringUtils.join(eks.iterator(), ""));
	        }
        }
        
        return results;
    }

    public static Set<String> expand(Integer tenantId, final List<String> strList) {
    	final Set<String> results = new HashSet<String>();
    	
    	for (String str: strList) {
    		results.addAll(expand(tenantId, str.toCharArray()));
    	}
    	
    	return results;
    }
    
    public synchronized static List<String> getFromIndex(SolrServer server) {
		List<String> allPossibleTerms = QAUtil.getAllPossibleFacetTerms(server, QA.FN_ESSENTIAL_KEYWORDS , "dataType_s:(" + QAUtil.DATATYPE_COMMON_SENSE + " " + QAUtil.DATATYPE_CASUAL + ")");
		
		System.out.println("Essential Keywords size()=" + allPossibleTerms.size());
		return allPossibleTerms;
	}
    
    public static void main(String[] args) {
    	HibernateUtil.init();
    	Collection<String> results = EssentialKeywordDictionary.expand(WiseSystemConfig.get().getDefaultTenantId(), "新北市預售屋地址查詢".toCharArray());
    	
    	for (String result: results) {
    		System.out.println(result); 
    	}
    	
    	HibernateUtil.shutdown();
    }
}
