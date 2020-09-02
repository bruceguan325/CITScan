package com.intumit.solr.robot;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RegexpQueryBuilder;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.dictionary.CategorizedKeyValuePair;
import com.intumit.solr.tenant.Tenant;

public class CasualQASaver {
	long begin = -1;
	private QA qa;
	
	private volatile boolean proccessing = false;
	private volatile boolean cancel = false;
	private boolean regexp = false;
	private Tenant t;
	
	
	static {
		Timer dirtyCheck = new Timer();
		dirtyCheck.schedule(new DirtyCheckTask(), 20000, 10000);
	}
	
	static Map<Integer, AtomicInteger> dirtyCountMap = new HashMap<Integer, AtomicInteger>();
	
	static public class DirtyCheckTask extends TimerTask {

		@Override
		public void run() {
			for (Integer tid: dirtyCountMap.keySet()) {
				AtomicInteger dirtyCount = dirtyCountMap.get(tid);
				
				if (dirtyCount.get() > 0) {
					Tenant t = Tenant.get(tid);
					System.out.println("CasualQASaver found tenant[" + t + "] has uncommit index, now doing commit." );
					SolrServer server = QAUtil.getCasualServer(t, true);
					try {
						server.commit(true, true, false);   // 標注起來只是暫時給大量匯入 Casual 用
						dirtyCount.set(0);
						// wait for new searcher
						Thread.sleep(1000);
						CasualKeywordDictionary.clearCache();
					} catch (Exception ignore) {
					}
				}
			}
		}
	}
	
	static void incDirtyCount(Tenant t) {
		if (!dirtyCountMap.containsKey(t.getId())) {
			dirtyCountMap.put(t.getId(), new AtomicInteger(1));
		}
		else {
			dirtyCountMap.get(t.getId()).getAndIncrement();
		}
	}
	

	/**
	 * SolrInputDocument should have id, kid, QUESTION_s, ANSWER_s
	 * @param build
	 * @param doc
	 * @param additionalAlts
	 */
	public CasualQASaver(QA qa, Tenant t) {
		this.qa = qa;
		this.t = t;
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

	public boolean isRegexp() {
		return regexp;
	}

	public void setRegexp(boolean regexp) {
		this.regexp = regexp;
	}

	public void save() {
		if (!proccessing) {
			try {
				proccessing = true;
				
				List<String> qaAtlTpls = qa.getQuestionAltTemplates();
				if (qaAtlTpls == null) {
					qaAtlTpls = new ArrayList<String>();
				}
				else {
				}
				//QAUtil.addOrUpdateField(doc, "QUESTION_ALT_TPL_ms", qaAtlTpls);
				begin = System.currentTimeMillis();
				long estimateAltCount = qaAtlTpls == null 
						? 0 
						: isRegexp() ? qaAtlTpls.size() : CasualQASaver.estimateAltCount(qaAtlTpls, true);
				System.out.println(
						String.format("Start %s [%s], estimated alt count: %d", 
								true ? "processing" : "preprocessing", qa.getQuestion(), estimateAltCount)
						);
				
				if (isRegexp()) {
					// 如果生活問答是正規表示法的，就有另外的處理方式（用 Elasticsearch 的 percolator）
					try {
						Client esCli = SearchManager.getESClient();
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
						                        .setIndices(t.getCasualCoreUrl())
						                        .setDocumentType("alts")
						                        .setSource(docBuilder).execute().actionGet();
					}
					catch (ElasticsearchException e1) {
						e1.printStackTrace();
					}
					catch (IOException e1) {
						e1.printStackTrace();
					}
					
					for (String regexAlt: qaAtlTpls) {
						addRegexPercolator(t.getCasualCoreUrl(), qa, regexAlt);
					}
				}
				
				if (estimateAltCount > QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER) {
					throw new RuntimeException("Too Much Alts on Casual Data:[" + qa.getQuestion() + "]");
				}
				else {
					// 如果試正規表示法的就不用產生各種問法了
					Collection<String> alts = (isRegexp() || qaAtlTpls == null) ? new ArrayList<String>(0) : expandAlt(null);
					
					int altCount = alts.size();
					qa.setDataType(QAUtil.DATATYPE_CASUAL);
					qa.setAltRebuildDate(Calendar.getInstance().getTime());
					qa.setAltRebuildTimeCost(System.currentTimeMillis() - begin);
					qa.setAltCount(altCount);
					
					try {
						indexQA(QAUtil.getCasualServer(t, true), qa.getKid(), qa, alts);
						incDirtyCount(t);
					} catch (SolrServerException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			catch (CanceledException canceled) {
				System.out.println("CasualQASaver of [" + qa.getKid() + "] canceled.");
				throw canceled;
			}
			finally {
				proccessing = false;
			}
		}
		else {
			throw new RuntimeException("This CasualQASaver is already running...");
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
	
	void addAndCheckTooMuch(String alt, List<String> allSet, TooMuchAltsIndexer tmai) {
		allSet.add(alt);
		if (allSet.size() % 1000 == 0) {
			System.out.println("[DEBUG] Current alt set size => " + allSet.size() + ": " + alt);
		}
		if (allSet.size() >= QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER) {
			if (tmai == null) {
				System.out.println("Expanded alt ended, size exceed " + QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + " records...");
				throw new RuntimeException("Expanded alt ended, size exceed " + QAUtil.ALT_LIMIT_USING_TOO_MUCH_ALTS_INDEXER + " records");
			}
			else {
				tmai.continueSaveQA(allSet);
				allSet.clear();
			}
		}
	}

	void recursiveExpand(String alt, boolean expandSynonym, int level, List<String> allSet, BloomFilter<CharSequence> dupCheck, TooMuchAltsIndexer tmai) 
	throws CanceledException {
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
					if (cancel) throw new CanceledException("Canceled in keyword expanding block");
					
					String newAlt = before + inner + after;
					if (QAUtil.expandBlock.matcher(newAlt).find()) {
						//int sizeBefore = allSet.size();
						recursiveExpand(StringUtils.trimToEmpty(newAlt), expandSynonym, level+1, allSet, dupCheck, tmai);
						//if (allSet.size() % 1000 == 0) {
						//	System.out.println("[DEBUG] Current alt set size:" + sizeBefore + "=>" + allSet.size() + " [" + level + "/Synonym(" + inner + ")]:" + newAlt);
						//}
					}
					else {
						List<String> cleanedAll = QAUtil.reconstructForCasual(null, newAlt);//QAUtil.cleanUpAltString(newAlt);
						
						for (String cleaned: cleanedAll) {
							if (dupCheck.put(cleaned)) {
								addAndCheckTooMuch(cleaned, allSet, tmai);
							}
						}
					}
				}
				
				if (expandSynonym) {
					List<String> synonyms = new ArrayList<String>(0);//SynonymKeywordFacade.getInstance().getSynonyms(inner, true);
					synonyms.remove(inner);
					
					if (synonyms.size() > 0) {
						for (String syn: synonyms) {
							if (cancel) throw new CanceledException("Canceled in synonyms calculating block");
							
							String newAlt = before + syn + after;
	
							if (QAUtil.expandBlock.matcher(newAlt).find()) {
								int sizeBefore = allSet.size();
								recursiveExpand(StringUtils.trimToEmpty(newAlt), false, level+1, allSet, dupCheck, tmai);
								if ((allSet.size() - sizeBefore) > QAUtil.ALT_LIMIT_PER_DOC) {
									System.out.println("[DEBUG] Current alt set size:" + sizeBefore + "=>" + allSet.size() + " [" + level + "/Synonym(" + inner + ")]:" + newAlt);
								}
							}
							else {
								List<String> cleanedAll = QAUtil.reconstructForCasual(null, newAlt);//QAUtil.cleanUpAltString(newAlt);
								
								for (String cleaned: cleanedAll) {
									if (dupCheck.put(cleaned)) {
										addAndCheckTooMuch(cleaned, allSet, tmai);
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
				addAndCheckTooMuch(alt, allSet, tmai);
			}
			
			List<String> cleanedAll = QAUtil.reconstructForCasual(null, alt);//QAUtil.cleanUpAltString(newAlt);
			
			for (String cleaned: cleanedAll) {
				if (dupCheck.put(cleaned)) {
					addAndCheckTooMuch(cleaned, allSet, tmai);
				}
			}
		}
	}

	public Collection<String> expandAlt(TooMuchAltsIndexer tmai) throws CanceledException {
		List<String> qaAtlTpls = qa.getQuestionAltTemplates();
		if (qaAtlTpls == null) {
			qaAtlTpls = new ArrayList<String>();
		}
		else {
		}
		Collection<String> qmExpaned = CasualQASaver.expandQuestionMark(qaAtlTpls);
		System.out.println(qmExpaned);
		List<String> l = new ArrayList<String>();
		BloomFilter<CharSequence> dupCheck = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 10000000, 0.00001);
		RobotNameReplacer r = new RobotNameReplacer();
		
		for (String alt: qmExpaned) {
			String t = TemplateUtil.processByTagName(alt, "ROBOT_NAME", r);
			recursiveExpand(t, true, 1, l, dupCheck, tmai);
		}
		
		if (tmai != null && l.size() > 0) {
			tmai.continueSaveQA(l);
			l.clear();
		}
		return l;
	}
	
	public static long estimateAltCount(Collection<String> multivalue, boolean expandSynonym) {
		long sum = 0;
		for (String alt: CasualQASaver.expandQuestionMark(multivalue)) {
			if (StringUtils.trimToNull(alt) == null)
				continue;
			
			sum += recursiveEstimateAltCount(alt, expandSynonym, 1);
		}
		return sum;
	}

	static long recursiveEstimateAltCount(String alt, boolean expandSynonym, int level) {
		Matcher m = QAUtil.expandBlock.matcher(alt);
		
		long sum = 1;
		
		if (m.find()) {
			int start = m.start();
			int end = m.end();
			
			String before = StringUtils.substring(alt, 0, start);
			String after = StringUtils.substring(alt, end);
			String inners = m.group(1);
			
			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			long innerCount = 0;
			
			if (expandSynonym) {
				for (String inner: innerArr) {
					List<String> synonyms = new ArrayList<String>(0); // @TODO: SynonymKeywordFacade.getInstance().getSynonyms(inner, true);
					innerCount += (int)Math.max(synonyms.size(), 1);
				}
			}
			else {
				innerCount += innerArr.length;
			}
			
			if (innerCount > 0) {
				String newAlt = before + innerArr[0] + after;
				sum *= innerCount * recursiveEstimateAltCount(StringUtils.trimToEmpty(newAlt), expandSynonym, level+1);
			}
		}
		else {
			sum *= 2; // 預估最終文字去跑 NLP 後可能會產生兩種以上的組合
		}
		
		return sum;
	}

	public static int indexQA(SolrServer server, long kid, SolrInputDocument docBase, Collection<String> alts) throws SolrServerException, IOException {
		if (alts.size() > QAUtil.ALT_LIMIT_PER_QUESTION)
			throw new RuntimeException("Too many alts of question");
		
		int totalNumOfParts = indexQA(server, kid, docBase, alts, 1);
		server.deleteByQuery("+kid_l:" + kid + " +isPart_i:[" + (totalNumOfParts+1) + " TO *]");
		
		return totalNumOfParts;
	}
	
	protected static int indexQA(SolrServer server, long kid, SolrInputDocument docBase, Collection<String> alts, int continueWithPartNumber) throws SolrServerException, IOException {
		int parts = (int) Math.max(Math.ceil( ((double)alts.size()) / QAUtil.ALT_LIMIT_PER_DOC), 1);
		if (continueWithPartNumber == 1) {
			System.out.println("Question[" + kid + "] split to (" + parts + ") parts => " + alts.size() + "/" + QAUtil.ALT_LIMIT_PER_DOC);
		}
		else {
			System.out.println("Question[" + kid + "] continue with part num [" + continueWithPartNumber + "], split to (" + parts + ") parts => " + alts.size() + "/" + QAUtil.ALT_LIMIT_PER_DOC);
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
			CasualQASaver.setOrUpdateField(partDoc, "QUESTION_ALT_ms", newSet);
			CasualQASaver.setOrUpdateField(partDoc, "QUESTION_ALT_mt", newSet);
			if ((continueWithPartNumber+i) > 1) {
				partDoc.setField("id", QAUtil.DATATYPE_CASUAL + "-" + kid + "-" + (continueWithPartNumber+i));
			}
			else {
				partDoc.setField("id", QAUtil.DATATYPE_CASUAL + "-" + kid);
			}
			if ((continueWithPartNumber+i) == 1) {
				partDoc.setField("hasPart_i", parts);
			}
			server.add(partDoc);
		}
		
		return parts;
	}
	
	public static void addRegexPercolator(String coreName, QA qa, String regexAlt) {
		if (StringUtils.trimToNull(regexAlt) != null) {
			Client esCli = SearchManager.getESClient();
			
			RegexpQueryBuilder qb = QueryBuilders.regexpQuery("regexp", regexAlt);
			
			try {
				IndexRequestBuilder indexReq = esCli.prepareIndex(coreName, ".percolator")
												    .setSource(jsonBuilder().startObject()
												    		.field("query", qb) // Register the query
												    		.field("kid", qa.getKid())
												    		.field("originalAlt", regexAlt)
												    		.field("answers", qa.getFieldValue(QA.FN_ANSWER))
												        .endObject())
												    .setRefresh(true);
				indexReq.execute().actionGet();
			}
			catch (ElasticsearchException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("CasualQASaver regexp percolator saved: " + qb);
		}
	}
	
	public static class RobotNameReplacer implements Replacer {
		public RobotNameReplacer() {
		}

		@Override
		public String call(String name, String val) {
			if (StringUtils.equals(name, "ROBOT_NAME")) {
				return StringUtils.trimToNull(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.ROBOTNAME));
			}
			
			return null;
		}
		
		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
		}
	}
}
