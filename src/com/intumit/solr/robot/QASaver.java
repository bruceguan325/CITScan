package com.intumit.solr.robot;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.dictionary.EssentialKeywordDictionary;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.mariten.kanatools.KanaConverter;

public class QASaver {
	QAAltBuild build;
	long begin = -1;
	private QA qa;
	private String[] additionalAlts;
	private BulkRequestBuilder bulkRequest = null;
	
	private volatile boolean proccessing = false;
	private volatile boolean cancel = false;
	
	private Map<String, List<String>> synonymsCache = new HashMap<String, List<String>>();

	/**
	 * SolrInputDocument should have id, kid, QUESTION_s, ANSWER_s
	 * @param build
	 * @param doc
	 * @param additionalAlts
	 */
	public QASaver(QAAltBuild build, QA qa, String... additionalAlts) {
		this.build = build;
		this.qa = qa;
		this.additionalAlts = additionalAlts;
	}

	public void cancel() {
		cancel = true;
	}
	
	public boolean isCanceled() {
		return (!proccessing && cancel);
	}

	public boolean isProccessing() {
		return proccessing;
	}

	public void save() {
		if (!proccessing) {
			try {
				proccessing = true;
				
				Collection<String> qaAltTpls = QAUtil.parseMultiValue(build.getQAltTpls());
				Collection<String> derivedAltTpls = QAUtil.parseMultiValue(build.getQAltTpls());
				//QAUtil.addOrUpdateField(doc, "QUESTION_ALT_TPL_ms", qaAtlTpls);
				begin = System.currentTimeMillis();
				Tenant tenant = Tenant.get(build.getTenantId());
				
				// 這裡取 QA.InheritantAltTemplate_ms 欄位，然後去把所有 QAAltTemplate 找出來，替換所有關鍵字後把問法加入 qaAltTpls
				List<String> ihAltTplSettings = (List)qa.getFieldValues(QA.FN_INHERITANT_ALT_TEMPLATES);
				if (ihAltTplSettings != null && ihAltTplSettings.size() > 0) {
					for (String ihAltTplSetting: ihAltTplSettings) {
						String mkey = StringUtils.substringBefore(ihAltTplSetting, ":");
						QAAltTemplate tpl = QAAltTemplate.getByKey(tenant.getId(), mkey);
						String str = StringUtils.substringAfter(ihAltTplSetting, ":");
						
						if (tpl != null && StringUtils.isNotEmpty(str)) {
							List<String> keywords = Arrays.asList(StringUtils.split(str, ","));
							String tplStr = tpl.getTemplate();
							int counter = 1;
							
							for (String kw: keywords) {
								if (counter == 1) {
									tplStr = StringUtils.replace(tplStr, "{{KEYWORD}}", kw);
								}
								
								tplStr = StringUtils.replace(tplStr, "{{KEYWORD" + counter + "}}", kw);
								
								counter ++;
							}
							
							System.out.println("Inheritant QA Alts:");
							System.out.println("*******************");
							System.out.println(tplStr);
							System.out.println("*******************");
							
							List<String> newAlts = QAUtil.parseMultiValue(tplStr);
							
							if (newAlts.size() > 0) {
								if (derivedAltTpls == null) {
									derivedAltTpls = newAlts;
								}
								else {
									derivedAltTpls.addAll(newAlts);
								}
							}
						}
					}
				}
				
				// 處理日文平假名的問法
    			if (tenant != null && tenant.getLocale() == HitHotLocale.ja_JP) {
    				KuromojiUtil ku = new KuromojiUtil();
    				List<String> aa = new ArrayList<>();
    				
					if (additionalAlts != null) {
						aa.addAll(Arrays.asList(additionalAlts));
					}
					
					aa.add(qa.getQuestion());
					// 標準問除了原始問句，也要放 Kata to Hira 的轉換結果
					aa.add(ku.convert(qa.getQuestion(), KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA));
					additionalAlts = aa.toArray(new String[0]);
    				
    				if (derivedAltTpls != null && derivedAltTpls.size() > 0) {
	        			List<String> newInnerList = new ArrayList<>();
        				
    					for (String alt: derivedAltTpls) {
	        				if (StringUtils.isEmpty(alt))
	        					continue ;
    	        				
	        				Matcher m = QAUtil.expandBlock.matcher(alt);
	        				StringBuilder sb = new StringBuilder();
	        				int pointer = 0;
        					boolean dirty = false;
    	        				
	        				while (m.find()) {
	        					int start = m.start();
	        					int end = m.end();
    	        					
	        					String before = StringUtils.substring(alt, pointer, start);
	        					String inners = m.group(1);
	        					pointer = end;
	        					
	        					sb.append(before);
	        					sb.append("(");
	        					//String after = StringUtils.substring(alt, end);
	        					String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
    	        					
	        					for (int i=0; i < innerArr.length; i++) {
	        						String inner = innerArr[i];
	        	        			String hira = ku.segAndToHiragana(inner);
	        	        			
        	        				if (i > 0) {
        	        					sb.append("|");
        	        				}
	        	        			
	        	        			if (StringUtils.equals(inner, hira)) {
	        	        				sb.append(inner);
	        	        			}
	        	        			else {
	        	        				sb.append(hira);
	        	        				dirty = true;
	        	        			}
	        					}
	        					
	        					sb.append(")");
	        				}
	        				
	        				sb.append(StringUtils.substring(alt,  pointer));
	        				
	        				System.out.println("New altTpl convert to hiragana: [" + alt + " == > " + sb.toString() + "]");
	        				if (dirty) newInnerList.add(sb.toString());
    					}

    					if (newInnerList.size() > 0) {
							derivedAltTpls.addAll(newInnerList);
    					}
    				}
    			}
				
				if (derivedAltTpls != null && derivedAltTpls.size() > 0) {
					build.setDerivedAltTpls(StringUtils.join(derivedAltTpls, "\n"));
				}

				// 移除所有排除句
				List<String> toBeExcluded = qa.getExcludeQuestionAltTemplates();
				if (toBeExcluded != null && toBeExcluded.size() > 0) {
					int size = derivedAltTpls.size();
					derivedAltTpls.removeAll(toBeExcluded);
					
					if (derivedAltTpls.size() != size) {
						build.setDerivedAltTpls(StringUtils.join(derivedAltTpls, "\n"));
					}
				}
				
				String esCoreName = QASaver.solrCoreNameToESCoreName(tenant);
				long estimateAltCount = derivedAltTpls == null ? 0 : QASaver.estimateAltCount(build.getTenantId(), QASaver.cleanCommentText(derivedAltTpls), build.isExpandSynonyms());
				System.out.println(
						String.format("Start %s [%d], estimated alt count: %d", 
								build.isExpandSynonyms() ? "processing" : "preprocessing", build.getKid(), estimateAltCount)
						);
				
				if(build.getDocId().contains(QAUtil.DATATYPE_COMMON_SENSE))
					qa.setDataType(QAUtil.DATATYPE_COMMON_SENSE);
				if(tenant.getEnableQaAudit() && build.getDocId().contains(QAUtil.DATATYPE_QA_AUDIT))
					qa.setDataType(QAUtil.DATATYPE_QA_AUDIT);
				
				if (estimateAltCount > QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER) {
					
					try {
						Client esCli = SearchManager.getESClient();
						PercolateResponse response = QASaver.esInitCore(esCli, esCoreName);
						bulkRequest = esCli.prepareBulk();
					}
					catch (ElasticsearchException e1) {
						e1.printStackTrace();
					}
					catch (IOException e1) {
						e1.printStackTrace();
					}
					
					qa.setQuestionAltTemplates(qaAltTpls == null ? new ArrayList<String>() : new ArrayList<String>(qaAltTpls));
					qa.setAltRebuildDate(Calendar.getInstance().getTime());
					
					TooMuchAltsIndexer tmai = new TooMuchAltsIndexer(tenant.getCoreServer4Write(), build.getDocId(), build.getKid(), qa);
					expandAlt(tmai);
					tmai.cleanRestOfParts();
					
					try {
						if (bulkRequest.numberOfActions() > 0) {
							BulkResponse bulkResponse = bulkRequest.execute().actionGet();
							if (bulkResponse.hasFailures()) {
							// process failures by iterating through each bulk response item
							}
						}
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				else {
					try {
						Client esCli = SearchManager.getESClient();
						PercolateResponse response = QASaver.esInitCore(esCli, esCoreName);
						QASaver.esDeletePercolatorsByKid(esCli, esCoreName, qa.getKid());
						response = QASaver.esInitCore(esCli, esCoreName);
						bulkRequest = esCli.prepareBulk();
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
					
					Collection<String> alts = derivedAltTpls == null ? new ArrayList<String>(0) : expandAlt(null);
					
					try {
						if (bulkRequest != null && bulkRequest.numberOfActions() > 0) {
							BulkResponse bulkResponse = bulkRequest.execute().actionGet();
							if (bulkResponse.hasFailures()) {
							// process failures by iterating through each bulk response item
							}
						}
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}

					try {
						if (alts.isEmpty())
							alts.addAll(Arrays.asList(additionalAlts));
						
						Collection<TreeSet<String>> eks = derivedAltTpls == null ? new ArrayList<TreeSet<String>>(0) : expandEssentialKeywords();
						TreeSet<String> uset = new TreeSet<String>();
						List<String> leks = new ArrayList<String>();
						
						if (eks.size() > 0) {
							for (TreeSet<String> ek: eks) {
								uset.addAll(ek);
								leks.add(StringUtils.join(ek.iterator(), ""));
							}
						}
						qa.setEssentialSentences(leks.size() > 0 ? leks : null);
						qa.setEssentialKeywords(uset.size() > 0 ? uset : null);
						
						int altCount = alts.size();
						qa.setQuestionAltTemplates(qaAltTpls == null ? new ArrayList<String>() : new ArrayList<String>(qaAltTpls));
						qa.setAltRebuildDate(Calendar.getInstance().getTime());
						qa.setAltRebuildTimeCost(System.currentTimeMillis() - begin);
						qa.setAltCount(altCount);
						
						EssentialKeywordDictionary.clear(tenant.getId());
						try {
							SolrServer server = tenant.getCoreServer4Write();
							server.deleteByQuery("+kid_l:" + build.getKid() + " +isPart_i:[2 TO *]");
							server.commit(true, true, false);
						} catch (SolrServerException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							indexQA(tenant.getCoreServer4Write(), build.getDocId(), build.getKid(), qa, alts);
						} catch (SolrServerException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
			catch (CanceledException canceled) {
				System.out.println("QASaver of [" + build.getKid() + "] canceled.");
				throw canceled;
			}
			finally {
				EventCenter.fireEvent(QASaver.class.getName(), build.getTenantId(), "processed", "" + build.getKid());
				proccessing = false;
			}
		}
		else {
			throw new RuntimeException("This QASaver is already running...");
		}
	}

	public static void setOrUpdateField(SolrInputDocument doc, String name, Object val){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("set", val);
		doc.setField(name, map);
	}

	public static void addOrUpdateField(SolrInputDocument doc, String name, Object val){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("set", val);
		doc.addField(name, map);
	}

	static Collection<String> expandQuestionMark(Collection<String> l) {
		Set<String> r = new HashSet<String>();
		boolean again = false;
		
		for (String str: l) {
			if (StringUtils.contains(str, '?')) {
				String sb = StringUtils.substringBefore(str, "?");
				String sa = StringUtils.substringAfter(str, "?");
				String n1 = sb + sa;
				String n2 = sb.substring(0, sb.length() - 1) + sa; 
				
				r.add(n1);
				r.add(n2);
				
				again = true;
			}
			else {
				r.add(str);
			}
		}
		
		return again ? expandQuestionMark(r) : r;
	}
	
	void addAndCheckTooMuch(String esCoreName, String alt, List<String> allSet, TooMuchAltsIndexer tmai, String firstLvAlt) {
		allSet.add(alt);
		
		if (StringUtils.indexOfIgnoreCase(alt, CustomData.FIXED_CUSTOM_QA_PARTIAL_PARAM) != -1) {
			System.out.println("**************** " + firstLvAlt + " >>> " + alt);
			
			String tmp = alt.replaceAll("(?i)" + CustomData.FIXED_CUSTOM_QA_PARTIAL_PARAM + "[a-z]+", " ");
			if (StringUtils.trimToNull(tmp) != null) {
				Client esCli = SearchManager.getESClient();
				
				String[] tokens = StringUtils.splitPreserveAllTokens(tmp, " ");
				BoolQueryBuilder qb = QueryBuilders.boolQuery();
				boolean enableFuzzy = false;
				String prefix = "";
				String postfix = "";
				
				for (int i = 0 ; i < tokens.length ; i++) {
					String token = tokens[i];
					if(i == 0) prefix = token;
					else if(i == tokens.length - 1) postfix = token;
					if(StringUtils.isBlank(token)) continue;
					enableFuzzy = true;
					qb.must(QueryBuilders.matchPhraseQuery("terms", token));
				}
				
				if(enableFuzzy) {
				try {
					IndexRequestBuilder indexReq = this.esAddPercolator(esCli, esCoreName, qa.getKid(), firstLvAlt, qb);
					if (bulkRequest != null) {
						bulkRequest.add(indexReq);
					}
					else {
						indexReq.execute().actionGet();
					}
				}
				catch (ElasticsearchException e) {
					e.printStackTrace();
				}
				System.out.println(qb);
			}
		}
		}
		
		if (allSet.size() % 1000 == 0) {
			System.out.println("[DEBUG] Current alt set size => " + allSet.size() + ": " + alt);
		}
		if (allSet.size() >= QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER) {
			// 允許 ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + 兩倍 ALT_LIMIT_PER_DOC 的空間...
			// 畢竟前面的 estimate 可能會有誤差
			if (tmai == null && allSet.size() >= (QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + QAUtil.ALT_LIMIT_PER_DOC * 2)) {
				System.out.println("Expanded alt ended, size exceed " + (QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + QAUtil.ALT_LIMIT_PER_DOC * 2) + " records...");
				throw new RuntimeException("Expanded alt ended, size exceed " + (QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + QAUtil.ALT_LIMIT_PER_DOC * 2) + " records");
			}
			else if (tmai != null) {
				tmai.continueSaveQA(allSet);
				allSet.clear();
			}
		}
	}

	void recursiveExpand(Tenant tenant, String alt, boolean expandSynonym, boolean reconstructByNLP, int level, List<String> allSet, BloomFilter<CharSequence> dupCheck, TooMuchAltsIndexer tmai, String firstLvAlt) 
	throws CanceledException {
		if (alt == null)
			return ;
		
		Matcher m = QAUtil.expandBlock.matcher(alt);
		String esCoreName = solrCoreNameToESCoreName(tenant);
		QAUtil qautil = QAUtil.getInstance(tenant);
		
		if (m.find()) {
			int start = m.start();
			int end = m.end();
			
			String before = StringUtils.substring(alt, 0, start);
			String after = StringUtils.substring(alt, end);
			String inners = m.group(1);
			
			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			List<String> newInnerList = new ArrayList<>();
			boolean dirty = false;
			
			for (String inner: innerArr) {
				if (StringUtils.startsWith(inner, "#")) {
					System.out.print("Try to find entity [" + inner + "]... ");
					dirty = true;
					QAEntity entity = QAEntity.get(tenant.getId(), inner.substring(1), null, null);
					
					if (entity != null) {
						System.out.println("Found entity, now expand innerArr...");
		        			String[] allValues = StringUtils.split(entity.getEntityValues(), "|");
		        			newInnerList.addAll(Arrays.asList(allValues));
					}
					else {
						System.out.println("Cannot found the entity [" + innerArr + "] skip it.");
					}
				}
				else if (StringUtils.startsWith(inner, "@")) {
					System.out.print("Try to find intent [" + inner + "]... ");
					dirty = true;
					QAIntent intent = QAIntent.get(tenant.getId(), inner.substring(1), null);
					
					if (intent != null) {
						System.out.println("Found intent, now expand innerArr...");
		        			String[] allValues = StringUtils.split(intent.getKeywords(), ",");
		        			newInnerList.addAll(Arrays.asList(allValues));
					}
					else {
						System.out.println("Cannot found the intent [" + innerArr + "] skip it.");
					}
				}
				else {
					newInnerList.add(inner);
				}
			}
			
			if (dirty) {
				innerArr = newInnerList.toArray(new String[0]);
			}
			
			for (String inner: innerArr) {
				{
					if (cancel) throw new CanceledException("Canceled in keyword expanding block");
					
					String newAlt = appendSpaceIfAppendLetterToLetter(before, inner, after);
					if (QAUtil.expandBlock.matcher(newAlt).find()) {
						//int sizeBefore = allSet.size();
						recursiveExpand(tenant, StringUtils.trimToEmpty(newAlt), expandSynonym, reconstructByNLP, level+1, allSet, dupCheck, tmai, firstLvAlt);
						//if (allSet.size() % 1000 == 0) {
						//	System.out.println("[DEBUG] Current alt set size:" + sizeBefore + "=>" + allSet.size() + " [" + level + "/Synonym(" + inner + ")]:" + newAlt);
						//}
					}
					else {
						if (dupCheck.put(newAlt)) {
							addAndCheckTooMuch(esCoreName, newAlt, allSet, tmai, firstLvAlt);
						}
						if (reconstructByNLP) {
							List<String> cleanedAll = QAUtil.reconstructQuestion(newAlt, ReconstructQuestionParameter.DEFAULT_QA_ALT_PARAM, null, qautil.getToAnalysis());//QAUtil.cleanUpAltString(newAlt);
							
							for (String cleaned: cleanedAll) {
								if (dupCheck.put(cleaned)) {
									addAndCheckTooMuch(esCoreName, cleaned, allSet, tmai, firstLvAlt);
								}
							}
						}
					}
				}
				
				if (expandSynonym) {
					List<String> synonyms = synonymsCache.get(inner);
						
					if (synonyms == null) {
						synonyms = SynonymKeywordFacade.getInstance().getSynonyms(build.getTenantId(), inner, true);
						synonymsCache.put(inner, synonyms);
					}
					synonyms.remove(inner);
					
					if (synonyms.size() > 0) {
						for (String syn: synonyms) {
							if (cancel) throw new CanceledException("Canceled in synonyms calculating block");
							
							String newAlt = appendSpaceIfAppendLetterToLetter(before, syn, after);
	
							if (QAUtil.expandBlock.matcher(newAlt).find()) {
								int sizeBefore = allSet.size();
								recursiveExpand(tenant, StringUtils.trimToEmpty(newAlt), false, reconstructByNLP, level+1, allSet, dupCheck, tmai, firstLvAlt);
								if ((allSet.size() - sizeBefore) > QAUtil.ALT_LIMIT_PER_DOC) {
									System.out.println("[DEBUG] Current alt set size:" + sizeBefore + "=>" + allSet.size() + " [" + level + "/Synonym(" + inner + ")]:" + newAlt);
								}
							}
							else {
								if (dupCheck.put(newAlt)) {
									addAndCheckTooMuch(esCoreName, newAlt, allSet, tmai, firstLvAlt);
								}
								
								if (reconstructByNLP) {
									List<String> cleanedAll = QAUtil.reconstructQuestion(newAlt, ReconstructQuestionParameter.DEFAULT_QA_ALT_PARAM, null, qautil.getToAnalysis());//QAUtil.cleanUpAltString(newAlt);
									
									for (String cleaned: cleanedAll) {
										if (dupCheck.put(cleaned)) {
											addAndCheckTooMuch(esCoreName, cleaned, allSet, tmai, firstLvAlt);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else {
			if (dupCheck.put(alt)) {
				addAndCheckTooMuch(esCoreName, alt, allSet, tmai, firstLvAlt);
			}
			if (reconstructByNLP) {
				List<String> cleanedAll = QAUtil.reconstructQuestion(alt, ReconstructQuestionParameter.DEFAULT_QA_ALT_PARAM, null, qautil.getToAnalysis());//QAUtil.cleanUpAltString(newAlt);
				
				for (String cleaned: cleanedAll) {
					if (dupCheck.put(cleaned)) {
						addAndCheckTooMuch(esCoreName, cleaned, allSet, tmai, firstLvAlt);
					}
				}
			}
		}
	}

	public Collection<String> expandAlt(TooMuchAltsIndexer tmai) throws CanceledException {
		System.out.println("****");
		Collection<String> qmExpaned = QASaver.expandQuestionMark(QASaver.cleanCommentText(QAUtil.parseMultiValue(build.getDerivedAltTpls())));
		if (additionalAlts != null) {
			qmExpaned.addAll(Arrays.asList(additionalAlts));
		}
		List<String> l = new ArrayList<String>();
		BloomFilter<CharSequence> dupCheck = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000, 0.00001);
		Tenant tenant = Tenant.get(build.getTenantId());
		
		for (String alt: qmExpaned) {
			String t = TemplateUtil.process(alt, TemplateUtil.CUSTOM_QA_REPLACER);
			if (StringUtils.indexOf(t, "[") == -1 && StringUtils.indexOf(t, "]") == -1) {
				recursiveExpand(tenant, t, build.isExpandSynonyms(), true, 1, l, dupCheck, tmai, alt);
			}
		}
		
		if (tmai != null && l.size() > 0) {
			tmai.continueSaveQA(l);
			l.clear();
		}
		System.out.println(l);
		System.out.println("****");
		
		return l;
	}

	public Collection<TreeSet<String>> expandEssentialKeywords() throws CanceledException {
		Collection<String> qmExpaned = QASaver.expandQuestionMark(QASaver.cleanCommentText(QAUtil.parseMultiValue(build.getDerivedAltTpls())));
		if (additionalAlts != null) {
			qmExpaned.addAll(Arrays.asList(additionalAlts));
		}
		List<String> l = new ArrayList<String>();
		BloomFilter<CharSequence> dupCheck = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000, 0.00001);
		Tenant tenant = Tenant.get(build.getTenantId());
		
		for (String alt: qmExpaned) {
			String t = TemplateUtil.process(alt, TemplateUtil.CUSTOM_QA_REPLACER);
			if (StringUtils.indexOf(t, "[") != -1 && StringUtils.indexOf(t, "]") != -1) {
				recursiveExpand(tenant, t, build.isExpandSynonyms(), false, 1, l, dupCheck, null, alt);
			}
		}
		
		if (tenant.getEnableDebug()) System.out.println("*** Expanded essential keywords:" + l);
		
		List<TreeSet<String>> lset = new ArrayList<TreeSet<String>>();
		for (String alt: l) {
			Matcher m = QAUtil.essentialBlock.matcher(alt);
			TreeSet<String> ts = new TreeSet<String>();
			
			while (m.find()) {
				String g = m.group(1);
				
				if (StringUtils.isNotEmpty(g))
					ts.add(g);
			}
			
			lset.add(ts);
		}
		
		if (tenant.getEnableDebug()) System.out.println("****** Expanded essential keywords:" + lset);
		
		return lset;
	}
	
	public static Collection<String> cleanCommentText(Collection<String> multivalue) {
		Set<String> r = new HashSet<String>();
		
		for (String str: multivalue) {
			r.add(QA.parseAndGetQAAltOnly(str));
		}
		
		return r;
	}
	
	public static long estimateAltCount(Integer tenantId, Collection<String> multivalue, boolean expandSynonym) {
		long sum = 0;
		for (String alt: QASaver.expandQuestionMark(QASaver.cleanCommentText(multivalue))) {
			sum += recursiveEstimateAltCount(tenantId, alt, expandSynonym, 1);
		}
		return sum;
	}

	static long recursiveEstimateAltCount(Integer tenantId, String alt, boolean expandSynonym, int level) {
		Matcher m = QAUtil.expandBlock.matcher(alt);
		
		long sum = 1;
		
		if (m.find()) {
			int start = m.start();
			int end = m.end();
			
			String before = StringUtils.substring(alt, 0, start);
			String after = StringUtils.substring(alt, end);
			String inners = m.group(1);
			
			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			List<String> newInnerList = new ArrayList<>();
			boolean dirty = false;
			
			for (String inner: innerArr) {
				if (StringUtils.startsWith(inner, "#")) {
					dirty = true;
					QAEntity entity = QAEntity.get(tenantId, inner.substring(1), null, null);
					
					if (entity != null) {
		        			String[] allValues = StringUtils.split(entity.getEntityValues(), "|");
		        			newInnerList.addAll(Arrays.asList(allValues));
					}
					else {
						System.out.println("Cannot found the entity [" + innerArr + "] skip it.");
					}
				}
				else if (StringUtils.startsWith(inner, "@")) {
					System.out.print("Try to find intent [" + inner + "]... ");
					dirty = true;
					QAIntent intent = QAIntent.get(tenantId, inner.substring(1), null);
					
					if (intent != null) {
		        			String[] allValues = StringUtils.split(intent.getKeywords(), ",");
		        			newInnerList.addAll(Arrays.asList(allValues));
					}
					else {
						System.out.println("Cannot found the intent [" + innerArr + "] skip it.");
					}
				}
				else {
					newInnerList.add(inner);
				}
			}
			
			if (dirty) {
				innerArr = newInnerList.toArray(new String[0]);
			}
			
			long innerCount = 0;
			
			if (expandSynonym) {
				for (String inner: innerArr) {
					List<String> synonyms = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, inner, true);
					
					innerCount += (int)Math.max(synonyms.size(), 1);
				}
			}
			else {
				innerCount += innerArr.length;
			}
			
			if (innerCount > 0) {
				String newAlt = before + innerArr[0] + after;
				sum *= innerCount * recursiveEstimateAltCount(tenantId, StringUtils.trimToEmpty(newAlt), expandSynonym, level+1);
			}
		}
		else {
			sum *= 2; // 預估最終文字去跑 NLP 後可能會產生兩種以上的組合
		}
		
		return sum;
	}

	public static int indexQA(SolrServer server, String docId, long kid, SolrInputDocument docBase, Collection<String> alts) throws SolrServerException, IOException {
		if (alts.size() > QAUtil.ALT_LIMIT_PER_QUESTION)
			throw new RuntimeException("Too many alts of question");
		
		int totalNumOfParts = indexQA(server, docId, kid, docBase, alts, 1);
		server.commit(true, true, false);
		
		try {
			// wait for softCommit
			Thread.sleep(1000);
		} catch (InterruptedException ignore) {
		} 
		
		return totalNumOfParts;
	}
	
	protected static int indexQA(SolrServer server, String docId, long kid, SolrInputDocument docBase, Collection<String> alts, int continueWithPartNumber) throws SolrServerException, IOException {
		int parts = (int) Math.max(Math.ceil( ((double)alts.size()) / QAUtil.ALT_LIMIT_PER_DOC), 1);
		if (continueWithPartNumber == 1) {
			System.out.println("Question[" + docId + "] split to (" + parts + ") parts => " + alts.size() + "/" + QAUtil.ALT_LIMIT_PER_DOC);
		}
		else {
			System.out.println("Question[" + docId + "] continue with part num [" + continueWithPartNumber + "], split to (" + parts + ") parts => " + alts.size() + "/" + QAUtil.ALT_LIMIT_PER_DOC);
		}
		
		for (int i=0; i < parts; i++) {
			Iterator<String> itr = alts.iterator();
			Set<String> newSet = new HashSet<String>();
			
			for (int j=0; j < alts.size() && itr.hasNext(); j++) {
				String alt = itr.next();
				
				if (j % parts == i) {
					newSet.add(alt);
				}
			}
			
			SolrInputDocument partDoc = docBase.deepCopy();
			partDoc.setField("isPart_i", (continueWithPartNumber+i));
			partDoc.setField("altPartCount_i", newSet.size());
			if(newSet.isEmpty()){
				newSet = null;
			}
			QASaver.setOrUpdateField(partDoc, "QUESTION_ALT_ms", newSet);
			QASaver.setOrUpdateField(partDoc, "QUESTION_ALT_mt", newSet);
			//QASaver.setOrUpdateField(partDoc, "QUESTION_ALT_BAK_ms", newSet);
			if ((continueWithPartNumber+i) > 1) {
				partDoc.setField("id", docId + "-" + (continueWithPartNumber+i));
			}
			else {
				partDoc.setField("id", docId);
			}
			if ((continueWithPartNumber+i) == 1) {
				partDoc.setField("hasPart_i", parts);
			}
			server.add(partDoc);
		}
		
		return parts;
	}
	
	public static String appendSpaceIfAppendLetterToLetter(String a, String b, String c) {
		String part1 = null;
		
		if (a.length() > 0 && a.substring(a.length() - 1, a.length()).matches("^\\w$") 
				&& b.length() > 0 && b.substring(0, 1).matches("^\\w$")) {
			part1 = a + " " + b;
		}
		else {
			part1 = a + b;
		}

		if (b.length() > 0 && b.substring(b.length() - 1, b.length()).matches("^\\w$") 
				&& c.length() > 0 && c.substring(0, 1).matches("^\\w$")) {
			return part1 + " " + c;
		}
		else {
			return part1 + c;
		}
	}

	public QAAltBuild getBuild() {
		return build;
	}
	
	static void quickRecursiveExpand(String alt, int level, List<String> allSet) {
		if (alt == null)
			return ;
		
		Matcher m = QAUtil.expandBlock.matcher(alt);
		
		if (m.find()) {
			int start = m.start();
			int end = m.end();
			
			String before = StringUtils.substring(alt, 0, start);
			String after = StringUtils.substring(alt, end);
			String inners = m.group(1);
			
			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			
			for (String inner: innerArr) {
				{
					String newAlt = appendSpaceIfAppendLetterToLetter(before, inner, after);
					if (QAUtil.expandBlock.matcher(newAlt).find()) {
						quickRecursiveExpand(StringUtils.trimToEmpty(newAlt), level+1, allSet);
					}
					else {
						allSet.add(newAlt);
					}
				}
			}
		}
		else {
			allSet.add(alt);
		}
	}

	public static Collection<String> quickExpand(String toBeExpand) throws CanceledException {
		List<String> l = new ArrayList<String>();
		
		String t = TemplateUtil.process(toBeExpand, TemplateUtil.CUSTOM_QA_REPLACER);
		quickRecursiveExpand(t, 1, l);
		
		return l;
	}
	
	
	// Elasticsearch Manipulation

	public static String solrCoreNameToESCoreName(Tenant t) {
		String coreName = t.getCoreName();
		String esCoreName = coreName;
		if (coreName.startsWith("http://") || coreName.startsWith("https://")) {
			esCoreName = StringUtils.substringAfterLast(coreName, "/");
		}
		
		return esCoreName;
	}

	public static IndexRequestBuilder esAddPercolator(Client esCli, String esCoreName, Long kid, String firstLvAlt, BoolQueryBuilder query) {
		try {
			IndexRequestBuilder indexReq = esCli.prepareIndex(esCoreName, ".percolator")
											    .setSource(jsonBuilder().startObject()
											    		.field("query", query) // Register the query
											    		.field("kid", kid)
											    		.field("originalAlt", firstLvAlt)
											        .endObject())
											    .setRefresh(true);
			return indexReq;
		}
		catch (ElasticsearchException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void esDeletePercolatorsByKid(Client esCli, String esCoreName, Long kid) {
	
		// 刪除 Percolators (此 KID)
		try {
			DeleteByQueryRequestBuilder deleteReqBuilder= new DeleteByQueryRequestBuilder(esCli);
			deleteReqBuilder
	        		.setIndices(esCoreName)
					.setTypes(".percolator")
					.setQuery(matchQuery("kid", kid)
			        );
			deleteReqBuilder.execute().actionGet();
		}
		catch (ElasticsearchException e1) {
			e1.printStackTrace();
		}
	}

	public static PercolateResponse esInitCore(Client esCli, String esCoreName) throws IOException {
		// Percolate a doc first to ensure Elasticsearch add field mapping automatically
		// Build a document to check against the percolator
		XContentBuilder docBuilder = jsonBuilder().startObject();
			docBuilder.field("doc").startObject(); //This is needed to designate the document
			docBuilder.field("terms", "This is amazing!");
			docBuilder.field("kid", 100);
			docBuilder.field("originalAlt", "IMPOSSIBLEINTUMITSPECIFICSTRING");
			docBuilder.endObject(); //End of the doc field
			docBuilder.endObject(); //End of the JSON root object
		//Percolate
		PercolateResponse response = esCli.preparePercolate()
		                        .setIndices(esCoreName)
		                        .setDocumentType("alts")
		                        .setSource(docBuilder).execute().actionGet();
		
		return response;
	}
}
