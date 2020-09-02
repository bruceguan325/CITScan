package com.intumit.solr.robot.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.common.lang3.StringUtils;

import com.google.common.base.Stopwatch;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;
import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;
import com.intumit.android.search.util.TaiwanAddressNormalizeUtil.AddrPart;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.WiVoUtil;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.time.nlp.TimeNormalizer;
import com.time.nlp.TimeUnit;
import com.time.util.DateUtil;

/**
 * 簡易的自訂辭典斷詞工具
 * 快速、高效
 * 
 * @author herb
 */
public class QAEntityDictionary extends BaseChineseDictionary implements Serializable 
{
	static Map<Integer, AhoCorasickDoubleArrayTrie<Set<QAEntity>>> trieMap = 
			new HashMap<Integer, AhoCorasickDoubleArrayTrie<Set<QAEntity>>>();
	static Map<String, AhoCorasickDoubleArrayTrie<Set<QAEntity>>> trieCategoryMap = 
			new HashMap<String, AhoCorasickDoubleArrayTrie<Set<QAEntity>>>();
	static Map<Integer, Map<Pattern, QAEntity>> regexMap = new HashMap<Integer, Map<Pattern, QAEntity>>();
	static Map<Integer, Set<QAEntity>> locationMap = new HashMap<Integer, Set<QAEntity>>();
	static Map<Integer, Set<QAEntity>> datetimeMap = new HashMap<Integer, Set<QAEntity>>();

    public static void clear(Integer tenantId) {
    	trieMap.remove(tenantId);
    	regexMap.remove(tenantId);
    	locationMap.remove(tenantId);
    	datetimeMap.remove(tenantId);
    	WiVoUtil.clear(tenantId);
    }
    
    public static AhoCorasickDoubleArrayTrie<Set<QAEntity>> loadIfNull(Integer tenantId, String category) {
    	if (!trieMap.containsKey(tenantId)) {
    		List<QAEntity> ddList = QAEntity.listByTenantId(tenantId);
    		if (ddList.size() == 0) return null;
    		AhoCorasickDoubleArrayTrie<Set<QAEntity>> trie = new AhoCorasickDoubleArrayTrie<Set<QAEntity>>();
    		
	        TreeMap<String, Set<QAEntity>> map = new TreeMap<String, Set<QAEntity>>();
	        Map<Pattern, QAEntity> rmap = new HashMap<Pattern, QAEntity>();
	        Set<QAEntity> lmap = new HashSet<QAEntity>();
	        Set<QAEntity> dtmap = new HashSet<QAEntity>();
	        
	        for (QAEntity dd: ddList) {
	        	if (dd.isEnabled()) {
	        		if (StringUtils.isNotEmpty(dd.getName())) {
		        		if (map.containsKey(dd.getName())) map.get(dd.getName()).add(dd);
						else {
							Set<QAEntity> set = new HashSet<QAEntity>();
							set.add(dd);
							map.put(dd.getName(), set);
						}
	        		}
	        		
	        		/*if (dd.getEntityType() == QAEntityType.STRING) {
		        		String[] keywords = StringUtils.split(dd.getEntityValues(), "|");
		        		
		        		for (String keyword: keywords) {
	    					if (map.containsKey(keyword)) map.get(keyword).add(dd);
							else {
								Set<QAEntity> set = new HashSet<QAEntity>();
								set.add(dd);
								map.put(keyword, set);
							}
				        	
			        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, keyword, false);
			        		
			        		if (syns != null && syns.size() > 0) {
			        			for (String syn: syns) {
			        				if (!StringUtils.equalsIgnoreCase(syn, keyword)) {
			        					if (map.containsKey(syn)) map.get(syn).add(dd);
			    						else {
			    							Set<QAEntity> set = new HashSet<QAEntity>();
			    							set.add(dd);
			    							map.put(syn, set);
			    						}
			        				}
			        			}
			        		}
		        		}
	        		}
	        		else */
	        		if (dd.getEntityType() == QAEntityType.REGEXP) {
		        		String[] regexes = StringUtils.split(dd.getEntityValues(), "\n");
		        		
		        		for (String regex: regexes) {
		        			Pattern p = Pattern.compile(regex);
		        			rmap.put(p, dd);
		        		}
	        		}
	        		else if (dd.getEntityType() == QAEntityType.LOCATION) {
	        			lmap.add(dd);
	        		}
	        		else if (dd.getEntityType() == QAEntityType.DATETIME) {
	        			dtmap.add(dd);
	        		}
	        	}
	        }

		    if (map.size() == 0 && rmap.size() == 0) return null;
		    
	        trie.build(map);
	        
	        trieMap.put(tenantId, trie);
	        regexMap.put(tenantId, rmap);
	        locationMap.put(tenantId, lmap);
	        datetimeMap.put(tenantId, dtmap);
    	}
    	if (category == null) {
    		return trieMap.get(tenantId);
    	}
    	else {
    		//新增trieCategoryMap，for租戶-分類的知識點
    		if (!trieCategoryMap.containsKey(tenantId + "_" + category)) {
    			List<QAEntity> ddList = QAEntity.listByTenantIdAndCategory(tenantId, category);
				if (ddList.size() == 0) {
					trieCategoryMap.put(tenantId + "_" + category, trieMap.get(tenantId));
					return trieCategoryMap.get(tenantId + "_" + category);
				}
        		AhoCorasickDoubleArrayTrie<Set<QAEntity>> trie = new AhoCorasickDoubleArrayTrie<Set<QAEntity>>();
        		TreeMap<String, Set<QAEntity>> map = new TreeMap<String, Set<QAEntity>>();
        		
    		    for (QAEntity dd: ddList) {
    		     	if (dd.getCategory().equals(category)) {
    	        		String[] keywords = StringUtils.split(dd.getEntityValues(), ",");
    	        		
    	        		for (String keyword: keywords) {
	    	        		List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, keyword, false);
	    	        		
	    	        		if (syns != null && syns.size() > 0) {
	    	        			for (String syn: syns) {
	    	        				if (!StringUtils.equalsIgnoreCase(syn, keyword)) {
	    	        					if (map.containsKey(syn)) map.get(syn).add(dd);
	    	    						else {
	    	    							Set<QAEntity> set = new HashSet<QAEntity>();
	    	    							set.add(dd);
	    	    							map.put(syn, set);
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

    public static Set<QAEntity>[] search(Integer tenantId, String text, String qaCategory) {
    	final char[] charArray = text.toCharArray();
    	final List<Set<QAEntity>> results = new ArrayList<Set<QAEntity>>();
    	AhoCorasickDoubleArrayTrie<Set<QAEntity>> trie = loadIfNull(tenantId, qaCategory);
    	AhoCorasickDoubleArrayTrie<Set<CustomData>> cdTrie = CustomDataDictionary.loadIfNull(tenantId);
    	if (trie == null && cdTrie == null) return (Set<QAEntity>[])results.toArray(new Set<?>[0]);
    	
        final Object[] wordNet = new Object[charArray.length];
        final int[] lengthNet = new int[charArray.length];
        
        cdTrie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<CustomData>>() {
			@Override
			public void hit(int begin, int end, Set<CustomData> value) {
				int length = end - begin;
				
				if (length > charArray.length || begin >= charArray.length) return;
				
				boolean allAscii = true;
				for (int i=begin; i < end; i++) {
					if (!CustomDataDictionary.isLetterOrDigit(charArray[i])) {
						//System.out.println("[" + charArray[i] + "] is not isLetterOrDigit");
						allAscii = false;
						break;
					}
				}
				
				if (allAscii) {
					
					if (begin > 0 && CustomDataDictionary.isLetterOrDigit(charArray[begin-1])) {
						return;
					}
					if (end < charArray.length && CustomDataDictionary.isLetterOrDigit(charArray[end])) {
						return;
					}
				}

				if (length > lengthNet[begin]) {
					wordNet[begin] = value;
					lengthNet[begin] = length;
				}
			}
		});
        
        /*trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Set<QAEntity>>()
        {
            @Override
            public void hit(int begin, int end, Set<QAEntity> value)
            {
                int length = end - begin;
                if (length > lengthNet[begin])
                {
                    wordNet[begin] = value;
                    lengthNet[begin] = length;
                }
            }
        });*/
        
        Map<Pattern, QAEntity> rmap = regexMap.get(tenantId);
        Set<QAEntity> lmap = locationMap.get(tenantId);
        Set<QAEntity> dtmap = datetimeMap.get(tenantId);
        
        if (rmap != null && rmap.size() > 0) {
        	for (Map.Entry<Pattern, QAEntity> e: rmap.entrySet()) {
        		Pattern p = e.getKey();
        		Matcher m = p.matcher(text);
        		
        		while (m.find()) {
            		QAEntity ed = e.getValue().getCopy();
        			int begin = m.start();
        			int end = m.end();
        			String t = m.group();
        			
        			ed.setEntityValues(t);
        			Set<QAEntity> set = new HashSet<QAEntity>();
        			set.add(ed);
        			int length = end - begin;
        			
                    if (length > lengthNet[begin])
                    {
                        wordNet[begin] = set;
                        lengthNet[begin] = length;
                    }
        		}
        	}
        }
        
        if (lmap != null && lmap.size() > 0) {
        	AhoCorasickDoubleArrayTrie<AddrPart[]> addrPartTrie = TaiwanAddressNormalizeUtil.getAddrTrie();
            
            addrPartTrie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<AddrPart[]>()
            {
                @Override
                public void hit(int begin, int end, AddrPart[] value)
                {
                    int length = end - begin;
                    if (length > lengthNet[begin])
                    {
                        wordNet[begin] = value;
                        lengthNet[begin] = length;
                    }
                }
            });
        }
        
        for (int offset = 0; offset < wordNet.length; )
        {
            if (wordNet[offset] == null) {
                ++offset;
                continue;
            }
            else {
            	Object obj = wordNet[offset];
            	
            	if (obj instanceof AddrPart[]) {
            		AddrPart[] addrParts = (AddrPart[])obj;

                	for (AddrPart ap: addrParts) {
                		Set<QAEntity> clonedSet = new HashSet<QAEntity>(lmap.size());
                		
						for (QAEntity ed : lmap) {
							QAEntityMatched newEd = (QAEntityMatched) ed.getCopy(new QAEntityMatched());
							newEd.setEntityValues(ap.text); // 未來也許這邊不再這樣使用
							newEd.setMatchedPosition(offset);

							List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenantId, ap.text);

							if (syns.size() > 0) {
								newEd.setMatchedValue(syns.get(0).getKeyword());
							}
							else {
								newEd.setMatchedValue(ap.text);
							}
							clonedSet.add(newEd);
						}
    	            	results.add(clonedSet);
    	            	offset += lengthNet[offset];
                	}
            	}
            	else {
	            	Set<Object> set = (Set<Object>)obj;
	            	Set<QAEntity> clonedSet = new HashSet<QAEntity>();
	            	
	            	for (Object ed: set) {
	            		QAEntityMatched newOne = null;
	            		String name = null;
	            		
	            		if (ed instanceof QAEntity) {
	            			QAEntity qae = (QAEntity)ed;
	            			name = qae.getName();
	            			newOne = (QAEntityMatched)(qae.getCopy(new QAEntityMatched()));
	            		}
	            		else if (ed instanceof CustomData) {
	            			CustomData cd = (CustomData)ed;
	            			if (cd.getEntityRef() != null) {
	            				name = cd.getEntityRef().getName();
	            				newOne = (QAEntityMatched)cd.getEntityRef().getCopy(new QAEntityMatched());
	            			}
	            		}
	            		if (newOne != null) {
    	            		//newOne.setId((long)(Math.random() * 10000L));
    	            		String val = new String(charArray, offset, lengthNet[offset]);
    	            		
    	            		newOne.setMatchedPosition(offset);
    	            		
    	            		List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenantId, val);
    	            		
    	            		if (syns.size() > 0) {
    	            			newOne.setMatchedValue(syns.get(0).getKeyword());
    	            		}
    	            		else {
    	            			newOne.setMatchedValue(val);
    	            		}
    	            		
    	            		if (!val.equalsIgnoreCase(name))
    	            			newOne.setEntityValues(val);
    	            		else 
    	            			newOne.setEntityValues(null);
    	            		
    	            		clonedSet.add(newOne);
	            		}
	            	}
	            	results.add(clonedSet);
	            	offset += lengthNet[offset];
            	}
            }
        }

        if (dtmap != null && dtmap.size() > 0) {
        	String path = TimeNormalizer.class.getResource("/TimeExp.m").getPath();
    		TimeNormalizer normalizer = new TimeNormalizer(path);
    		
    		String cn = WiSeUtils.tw2cn(text);
    		normalizer.parse(cn);// 抽取时间
    		TimeUnit[] unit = normalizer.getTimeUnit();
    		
    		if (unit != null && unit.length > 0) {
    			Set<QAEntity> set = (Set<QAEntity>)dtmap;
            	Set<QAEntity> clonedSet = new HashSet<QAEntity>();

        		for (TimeUnit u: unit) {
                	for (Object ed: set) {
                		QAEntityMatched newOne = null;
                		
                		if (ed instanceof QAEntity) {
                			QAEntity qae = (QAEntity)ed;
                			newOne = (QAEntityMatched)(qae.getCopy(new QAEntityMatched()));
                		}
                		
                		if (newOne != null) {
    	            		newOne.setMatchedPosition(StringUtils.indexOf(cn, u.Time_Expression));
                			newOne.setEntityValues(DateUtil.formatDateDefault(u.getTime()));
    	            		clonedSet.add(newOne);
                		}
            		}
            	}
            	
            	results.add(clonedSet);
    		}
        }
        
        return (Set<QAEntity>[])results.toArray(new Set<?>[0]);
    }
    
    public static void main(String[] args) {
    	HibernateUtil.init();
    	Set<QAEntity>[] results = QAEntityDictionary.search(7, "我要開卡，你這個混蛋", null);
    	
    	for (Set<QAEntity> resultSet: results) {
        	for (QAEntity result: resultSet) {
        		System.out.println("(" + result.getId() + "). " + result.getEntityValues() + ":" + result.getCode() + "(" + result.getSubEntities() + ")"); 
        	}
    	}
    	
    	String path = TimeNormalizer.class.getResource("/TimeExp.m").getPath();
		TimeNormalizer normalizer = new TimeNormalizer(path);
		
		String[] testCases = {
			"今天下午兩點",
			"今天下午2點",
			"明天下午2點",
			"明天下午兩點",
			"大後天凌晨5點",
			"下週二上午九點",
		};
		
		for (int i=0; i < testCases.length; i++) {
			String tc = testCases[i];
    		String cn = WiSeUtils.tw2cn(tc);
    		normalizer.parse(cn);
    		TimeUnit[] unit = normalizer.getTimeUnit();
    		
    		System.out.println(
    				String.format("%s -> %s", 
    						tc,
    						DateUtil.formatDateDefault(unit[0].getTime())
    						));
		}
    }
}
