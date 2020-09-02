package com.intumit.solr.qparser;

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.util.WordSeg;

public class SynAndSegQueryUtils {
	
	public static final boolean DEBUG = false;
	
	static Set<UnicodeBlock> chineseUnicodeBlocks = new HashSet<UnicodeBlock>() {
		{
	    add(UnicodeBlock.CJK_COMPATIBILITY);
	    add(UnicodeBlock.CJK_COMPATIBILITY_FORMS);
	    add(UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
	    add(UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
	    add(UnicodeBlock.CJK_RADICALS_SUPPLEMENT);
	    add(UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
	    add(UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
	    add(UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
	    add(UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
	    add(UnicodeBlock.KANGXI_RADICALS);
	    add(UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS);
		}
	};
	
	public static boolean isChinese(char c) {
		return chineseUnicodeBlocks.contains(UnicodeBlock.of(c));
	}
	
	public static boolean isAllChinese(String term) {
		for (int i = 0; i < term.length(); i++) {
			if (term.charAt(i) != '*' && !chineseUnicodeBlocks.contains(UnicodeBlock.of(term.charAt(i))))
				return false;
		}
		
		return true;
	}

    public static Query doSyn(Integer tenantId, QueryParser qp, String field, String queryText, boolean isPhrase) {
    	try {
    		Query baseQuery = qp.parse(getFieldQueryString(field, queryText, isPhrase));
			List<String> syns = SynonymKeywordFacade.getInstance().getSynonyms(tenantId, queryText, false);
			
			if (syns != null)
				syns.remove(queryText);
			
			if (syns != null && syns.size() > 0) {
		    	BooleanQuery query = new BooleanQuery(false);
    			query.add(baseQuery, BooleanClause.Occur.SHOULD);
		    	
				for (String syn : syns) {
					Query subQuery = qp.parse(getFieldQueryString(field, syn, true));
	    			query.add(subQuery, BooleanClause.Occur.SHOULD);
				}
				
				return query;
			}
			return baseQuery;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }

	public static Query doSeg(Integer tenantId, QueryParser qp, String field, String queryText, boolean syn) {
    	try {
	    	WordSeg ws = WordSeg.getInstance(WordSeg.NAMESPACE_CUSTOMIZE);
			List<String> segments = ws.segWords(queryText, true, true);
			segments.remove(queryText.toLowerCase());
			if (DEBUG) System.out.println("seg:" + segments);
			
			if (segments != null && segments.size() > 0) {
		    	BooleanQuery query = new BooleanQuery(false);
		    	
		    	/*if (syn) {
		    		query.add(doSyn(term), BooleanClause.Occur.SHOULD);
		    	}
		    	else {
		    		if (StringUtils.startsWith(term, "\"") && StringUtils.endsWith(term, "\"")) {
		    			PhraseQuery pq = new PhraseQuery();
		    			pq.add(new Term("", term));
		    			query.add(pq, BooleanClause.Occur.SHOULD);
		    		}
		    		else {
		    			query.add(new TermQuery(new Term("", term)), BooleanClause.Occur.SHOULD);
		    		}
		    	}*/
		    	
				BooleanQuery allSegBQ = new BooleanQuery(false);
				for (String segment : segments) {
					if (syn) {
						allSegBQ.add(doSyn(tenantId, qp, field, segment, false), BooleanClause.Occur.MUST);
					}
					else {
						Query segQuery = qp.parse(getFieldQueryString(field, segment, false));
		    			allSegBQ.add(segQuery, BooleanClause.Occur.SHOULD);
					}
				}
				query.add(allSegBQ, BooleanClause.Occur.SHOULD);
				
				return query;
			}
			
			return syn ? doSyn(tenantId, qp, field, queryText, false) : qp
					.parse(getFieldQueryString(field, queryText, false));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
    }
    
	public static Query doSegAndSyn(Integer tenantId, QueryParser qp,
			BooleanQuery query, boolean seg, boolean syn) throws IOException {
		BooleanQuery newQ = new BooleanQuery();
		for (Object o : query.clauses()) {
			BooleanClause c = (BooleanClause) o;
			Query cq = c.getQuery();
			if (DEBUG)
				System.out.println(c.getOccur() + "/" + cq + "/"
						+ cq.getClass().getName());
			
			if (cq instanceof BooleanQuery) {
				newQ.add(
						doSegAndSyn(tenantId, qp, (BooleanQuery) cq, seg, syn),
						c.getOccur());
			} else if (cq instanceof TermQuery) {
				TermQuery tq = (TermQuery) cq;
				Term t = tq.getTerm();
				if (seg) {
					newQ.add(doSeg(tenantId, qp, t.field(), t.text(), syn), c.getOccur());
				} else { // must be syn=true
					newQ.add(
							(syn ? doSyn(tenantId, qp, t.field(), t.text(), false) 
									: cq), c.getOccur());
				}	
			} else if (cq instanceof PhraseQuery) {
				PhraseQuery pq = (PhraseQuery) cq;
				Term t = getPhraseText(pq);
				if (seg) {
					newQ.add(doSeg(tenantId, qp, t.field(), t.text(), syn), c.getOccur());
				} else { // must be syn=true
					newQ.add(
							(syn ? doSyn(tenantId, qp, t.field(), t.text(), true) 
									: cq), c.getOccur());
				}	
			} else {
				System.out.println("Unknown query part:[" + cq.getClass().getName() + "]:" + cq.toString());
				newQ.add(cq, c.getOccur());
			}
		}
		
		/*
		 * System.out.println("newQ:" + newQ); BooleanQuery t = new BooleanQuery();
		 * 
		 * flatten 被發現會造成奇怪的現象，例如 (+台積電) (+張忠謀) 原本因為外面的括弧，這兩個其實是 OR 的條件。但
		 * FLATTEN 之後，會變成 +台積電 +張忠謀 這樣就變成 AND。而會造成 (+台積電)
		 * 的原因是做斷詞以及同義詞的時候，有可能最後只斷出一個詞，本來有 + 號的原因是如果斷出多個，有可能某個是一定要有的。
		 * 
		 * SolrPluginUtils.flattenBooleanQuery(t, newQ);
		 */
		
		return newQ;
    }
    
	public static String doSegAndSyn(Integer tenantId, SolrCore core,
			String query, boolean seg, boolean syn) throws IOException {
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(
				core.getSolrConfig().luceneMatchVersion);
		AnalyzingQueryParser aqp = new AnalyzingQueryParser(
				core.getSolrConfig().luceneMatchVersion, "", analyzer);
		aqp.setLowercaseExpandedTerms(false);
		// StringBuffer buf = new StringBuffer();
		Query finalQ = null;
		
		try {
			if (!"*:*".equals(query)) {
				Query cq = aqp.parse(query);
				if (DEBUG)
					System.out.println(cq + "//" + cq.getClass().getName());
	
				if (cq instanceof BooleanQuery) {
					finalQ = doSegAndSyn(tenantId, aqp, (BooleanQuery) cq, seg,
							syn);
				} else if (cq instanceof TermQuery) {
					TermQuery tq = (TermQuery) cq;
					Term t = tq.getTerm();
					
					if (seg) {
						finalQ = doSeg(tenantId, aqp, t.field(), t.text(), syn);
					} else {
						finalQ = syn ? doSyn(tenantId, aqp, t.field(), t.text(), false) : cq;
					}
				} else {
					if (cq instanceof PrefixQuery) {
						PrefixQuery pq = (PrefixQuery) cq;
						String termText = pq.getPrefix().text();
						
						if (isAllChinese(termText)) {
							if (seg) {
								finalQ = doSeg(tenantId, aqp, pq.getField(), termText, syn);
							} else {
								finalQ = syn ? doSyn(tenantId, aqp, pq.getField(), termText, false) : pq;
							}
						}
					} else {
						System.out.println("Unknown query part:["
								+ cq.getClass().getName() + "]:"
								+ cq.toString());
					}
					
					if (finalQ == null)
						finalQ = cq;
				}
			} else {
				return query;
			}
		} catch (Exception e) {
			System.out.println("Cannot doSegAndSyn [" + query + "]:"
					+ e.getMessage());
			return query;
		}
		
		if (DEBUG)
			System.out.println("FinalQ:" + finalQ);
		return finalQ.toString();
    }

	private static String getFieldQueryString(String field, String queryText,
			boolean isPhrase) {
		return StringUtils.isNotEmpty(field) ? (field + ":(" + queryText + ")")
				: ("(" + queryText + ")");
	}
    
    private static Term getPhraseText(PhraseQuery pq) {
    	int[] positions = pq.getPositions();
    	Term[] terms = pq.getTerms();
    	String field = null;
    	
    	StringBuilder sb = new StringBuilder();
    	int lastPos = -1;
		boolean hasCJK = false;
    	boolean isLastTermCJK = true; // 一開始是 true 是個 hack 避免讓後面判斷邏輯不會造成第一個英文也先 append 空白
    	
		for (int i = 0; i < positions.length; i++) {
    		int pos = positions[i];
    		Term term = terms[i];
			if (field == null)
				field = term.field();
    		
    		boolean isCJK = isAllChinese(term.text());
			if (isCJK) hasCJK = true;
    		if ((!isCJK && !isLastTermCJK) || (pos > (lastPos + 1)))
    			sb.append(" ");
    		sb.append(term.text());
    		
    		lastPos = pos;
    		isLastTermCJK = isCJK;
    	}
		return new Term(field, hasCJK ? sb.toString() : WiSeUtils.dblQuote(sb.toString()));
    }

    public static void main(String[] args) {
    	HibernateUtil.init();
    	CoreContainer cc = new CoreContainer("."); 
    	cc.load();
    	SolrCore core0 = cc.getCore("core0");
    	
    	try {
			String q = doSegAndSyn(1, core0, "Name_t:保稅人員", true, true);
			System.out.println("***:" + q);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	cc.shutdown();
    	HibernateUtil.shutdown();
    	
    	System.exit(0);
    }
}
