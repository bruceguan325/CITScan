package com.intumit.solr.searchKeywords;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TextField;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.recommendKeywords.RecommendKeyword;
import com.thoughtworks.xstream.XStream;

public class SearchKeywordLogFacade {
	public static final String DEFAULT_FUNC = "default";
	public static final String NO_RESULT_FUNC = "no_result";
	static final SearchKeywordLogFacade instance = new SearchKeywordLogFacade();
	public static final int MAX_KEYWORD_LENGTH = 128;
	private static String defaultDateFrom = "NOW+8HOUR/DAY-8HOUR-7DAY";
	private static String defaultDateTo = "NOW+8HOUR/DAY-8HOUR+1DAY";

	public static void setDefaultDateFrom(String defaultDateFrom) {
		SearchKeywordLogFacade.defaultDateFrom = defaultDateFrom;
	}

	public static void setDefaultDateTo(String defaultDateTo) {
		SearchKeywordLogFacade.defaultDateTo = defaultDateTo;
	}

	/*
	 * static BoundedOrderdCache<KeywordEntry> cache = new
	 * BoundedOrderdCache<KeywordEntry>(1024, new Comparator() {
	 * 
	 * @Override public int compare(Object o1, Object o2) { KeywordEntry k1 =
	 * (KeywordEntry)o1; KeywordEntry k2 = (KeywordEntry)o2;
	 * 
	 * if (k1.getSearchCount() == k2.getSearchCount()) return
	 * k2.getTimestamp().compareTo(k1.getTimestamp());
	 * 
	 * return k2.getSearchCount() - k1.getSearchCount(); }});
	 */

	/**
	 * 改成 Singleton
	 * 
	 * @return
	 */
	public static SearchKeywordLogFacade getInstance() {
		return instance;
	}

	private String mode = "db";

	private SearchKeywordLogFacade() {
	}

	public synchronized void log(Collection<Term> terms, IndexSchema schema) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			for (Term t : terms) {
				// 只記text和string field
				if (!(schema.getFieldType(t.field()) instanceof TextField))
					continue;
				SearchKeywordLog newLog = new SearchKeywordLog();
				if (t.text().length() > MAX_KEYWORD_LENGTH)
					newLog.setName(t.text().substring(0, MAX_KEYWORD_LENGTH));
				else
					newLog.setName(t.text());
				newLog.setFunc(DEFAULT_FUNC);
				newLog.setLogtime(new Timestamp(System.currentTimeMillis()));
				ses.saveOrUpdate(newLog);
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized void log(String word) {
		log(word, DEFAULT_FUNC);
	}

	/**
	 * 提供管道可以記錄不同的搜尋行為 例如不同的搜尋功能（快速搜尋，進階搜尋），供後續分析使用
	 * 
	 * @param word
	 * @param func
	 */
	public synchronized void log(String word, String func) {
		if(StringUtils.isNotBlank(func) && StringUtils.isNotBlank(word) && !"*:*".equals(word)){
			String w = StringUtils.lowerCase(word);
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				SearchKeywordLog newLog = new SearchKeywordLog();
				if (w.length() > MAX_KEYWORD_LENGTH)
					w = w.substring(0, MAX_KEYWORD_LENGTH);
				newLog.setName(w);
				newLog.setFunc(func);
				newLog.setLogtime(new Timestamp(System.currentTimeMillis()));
				ses.saveOrUpdate(newLog);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
	    }
	}

	public synchronized void deleteAll() throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + SearchKeywordLog.class.getName())
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized void deleteLogtimeBefore(Date before) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Query query = ses.createQuery("delete from " + SearchKeywordLog.class.getName() + " where logtime < :before");
			query.setDate("before", before);
			
			query.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized void calculateStatisicHourly(String from, String to) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery(
					"INSERT INTO " + SearchKeywordLogStatisticsHourly.class.getName()
+ " (name,func,logtime,logYear,logMonth,logDay,logHour,frequency) "
+ "    SELECT    name,func,MIN(logtime) as logtime, "
+ "       YEAR(logtime) as logYear,MONTH(logtime) as logMonth,DAY(logtime) as logDay,HOUR(logtime) as logHour, count(*) as frequency"
+ "	   FROM " + SearchKeywordLog.class.getName()
+ "	   WHERE logtime BETWEEN '" + from + "' AND '" + to + "' "
+ "	   GROUP BY name,func,YEAR(logtime),MONTH(logtime),DAY(logtime),HOUR(logtime)")
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	@SuppressWarnings("unchecked")
	private List<SearchKeywordLog> list(Order order, int count) {
		List<SearchKeywordLog> result = new ArrayList<SearchKeywordLog>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();

			Criteria ct = ses.createCriteria(SearchKeywordLog.class);

			ProjectionList p = Projections.projectionList();
			p.add(Projections.groupProperty("name"));
			p.add(Projections.count("name"), "frequency");
			p.add(Projections.max("logtime"), "logtime");
			ct.setProjection(p);

			ct.addOrder(order);
			ct.setMaxResults(count);

			List<Object[]> r = ct.list();
			for (Object[] row : r) {
				SearchKeywordLog log = new SearchKeywordLog();
				log.setName((String) row[0]);
				log.setFrequency((Integer) row[1]);
				log.setLogtime((Timestamp) row[2]);
				result.add(log);
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private List<String> columnList(String col, Order order, int count) {
		List<String> result = new ArrayList<String>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();

			Criteria ct = ses.createCriteria(SearchKeywordLog.class);
			ProjectionList p = Projections.projectionList();
			p.add(Projections.groupProperty("name"));
			p.add(Projections.count("name"), "frequency");
			p.add(Projections.max("logtime"), "logtime");
			ct.setProjection(p);
			ct.addOrder(order);
			ct.setMaxResults(count);

			List<Object[]> r = ct.list();
			for (Object[] row : r) {
				result.add((String) row[0]);
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	List<String> getMostPopularFromSearch(String from, String to, String func,
			int count) {
		List<String> kwList = new ArrayList<String>();

		SolrServer server = SearchManager.getServer("core-keyword");
		SolrQuery query = new SolrQuery();
		query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
		query.setFacet(true);
		query.addFacetField("Name_s");
		query.setFacetLimit(count);
		query.setFacetMinCount(1);
		query.setQueryType("standard");
		if (func != null && !DEFAULT_FUNC.equalsIgnoreCase(func)) 
			query.addFilterQuery("func_s:" + func);
		else 
			query.addFilterQuery("-func_s:" + NO_RESULT_FUNC);
		query.setQuery("*:*");
		try {
			QueryResponse rsp = server.query(query);
			FacetField kwFF = rsp.getFacetField("Name_s");

			if (kwFF != null && kwFF.getValues() != null) {
				for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
						.iterator(); iterator2.hasNext();) {
					FacetField.Count facetCount = iterator2.next();
					String word = facetCount.getName();
					int ccc = (int) facetCount.getCount();

					if (ccc > 0) {
						kwList.add(word);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return kwList;
	}

	List<SearchKeywordLog> getMostPopularFromSearchBeans(String from,
			String to, String func, int count) {
		List<SearchKeywordLog> kwList = new ArrayList<SearchKeywordLog>();

		SolrServer server = SearchManager.getServer("core-keyword");
		SolrQuery query = new SolrQuery();
		query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
		if (func != null && !DEFAULT_FUNC.equalsIgnoreCase(func)) 
			query.addFilterQuery("func_s:" + func);
		else 
			query.addFilterQuery("-func_s:" + NO_RESULT_FUNC);
		query.setFacet(true);
		query.addFacetField("Name_s");
		query.setFacetLimit(count);
		query.setFacetMinCount(1);
		query.setQueryType("standard");
		query.setQuery("*:*");
		try {
			QueryResponse rsp = server.query(query);
			FacetField kwFF = rsp.getFacetField("Name_s");

			if (kwFF != null && kwFF.getValues() != null) {
				for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
						.iterator(); iterator2.hasNext();) {
					FacetField.Count facetCount = iterator2.next();
					String word = facetCount.getName();
					int ccc = (int) facetCount.getCount();
					SearchKeywordLog log = new SearchKeywordLog();

					if (ccc > 0) {
						log.setName(word);
						log.setFrequency(ccc);
						kwList.add(log);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return kwList;
	}

	List<SearchKeywordLog> getLatestFromSearchBeans(String from, String to,
			String func, int count) {
		List<SearchKeywordLog> kwList = new ArrayList<SearchKeywordLog>();

		SolrServer server = SearchManager.getServer("core-keyword");
		SolrQuery query = new SolrQuery();
		query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
		if (func != null && !DEFAULT_FUNC.equalsIgnoreCase(func)) 
			query.addFilterQuery("func_s:" + func);
		else 
			query.addFilterQuery("-func_s:" + NO_RESULT_FUNC);
		query.setQueryType("standard");
		query.setSortField("Date_dt", SolrQuery.ORDER.desc);
		query.setQuery("*:*");
		query.setRows(count);

		try {
			QueryResponse rsp = server.query(query);
			SolrDocumentList result = rsp.getResults();

			if (result != null) {
				for (Iterator<SolrDocument> iterator2 = result.iterator(); iterator2
						.hasNext();) {
					SolrDocument doc = iterator2.next();

					SearchKeywordLog log = new SearchKeywordLog();
					log.setName((String) doc.getFieldValue("Name_s"));
					log.setFunc((String) doc.getFieldValue("func_s"));
					// log.setLogtime((Date)doc.getFieldValue("Name"));
					kwList.add(log);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return kwList;
	}

	List<String> getLatestFromSearch(String from, String to, String func,
			int count) {
		List<String> kwList = new ArrayList<String>();

		SolrServer server = SearchManager.getServer("core-keyword");
		SolrQuery query = new SolrQuery();
		query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
		if (func != null && !DEFAULT_FUNC.equalsIgnoreCase(func)) 
			query.addFilterQuery("func_s:" + func);
		else 
			query.addFilterQuery("-func_s:" + NO_RESULT_FUNC);
		query.setQueryType("standard");
		query.setSortField("Date_dt", SolrQuery.ORDER.desc);
		query.setQuery("*:*");
		query.setRows(count);

		try {
			QueryResponse rsp = server.query(query);
			SolrDocumentList result = rsp.getResults();

			if (result != null) {
				for (Iterator<SolrDocument> iterator2 = result.iterator(); iterator2
						.hasNext();) {
					SolrDocument doc = iterator2.next();
					String word = (String) doc.getFieldValue("Name_s");
					kwList.add(word);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return kwList;
	}

	public List<String> listMostPopular(int count) {
		return listMostPopular(null, count);
	}

	public List<String> listMostPopular(String func, int count) {
		if ("index".equalsIgnoreCase(mode)) {
			return getMostPopularFromSearch(defaultDateFrom, defaultDateTo,
					func, count);
		}
		return columnList("name", Order.desc("frequency"), count);
	}

	public synchronized List<SearchKeywordLog> listMostPopularBeans(
			String func, int count) {
		if ("index".equalsIgnoreCase(mode)) {
			return getMostPopularFromSearchBeans(defaultDateFrom,
					defaultDateTo, func, count);
		}
		return list(Order.desc("frequency"), count);
	}

	public synchronized String listMostPopularXml(String func, int count) {
		return out().toXML(listMostPopularBeans(func, count));
	}

	private XStream out() {
		XStream out = new XStream();
		out.alias("keyword", SearchKeywordLog.class);
		out.alias("keyword", RecommendKeyword.class);
		return out;
	}

	public void setMode(String string) {
		System.out
				.println("************* SET SearchKeywordLog MODE TO ["
						+ string
						+ "]**********");
		this.mode = string;
	}

	public List<RecommendKeyword> getHotKeywords(String dsId, Integer rows, Integer dayInteger) {
		int day = dayInteger.intValue();

		List<RecommendKeyword> result = new ArrayList<RecommendKeyword>();
		List<RecommendKeyword> adWordList = getAdWords(rows, dsId);

		RecommendKeyword[] temp = new RecommendKeyword[rows];		
		List<String> rcmdStringList = new ArrayList<String>();

		// 先將指定關鍵字塞到排序位置
		for (RecommendKeyword adWord: adWordList) {
			if (StringUtils.isNotBlank(adWord.getSort().toString())) {
				int targetIndex = Integer.parseInt(adWord.getSort().toString()) - 1;
				
				if (targetIndex >= 0 && targetIndex < rows) {
					temp[targetIndex] = adWord;
					rcmdStringList.add(adWord.getKeyword());
				}
			}
		}

		String dateFrom = "NOW+8HOUR/DAY-8HOUR" + day * (-1) + "DAY";
		String dateTo = "NOW+8HOUR/DAY-8HOUR+1DAY";
		List<String>realMostPopularStringList = 
			getMostPopularFromSearch(dateFrom, dateTo, dsId, rows);
		// 真正的熱門關鍵字去掉手動設定的關鍵字（去掉重複）
		realMostPopularStringList.removeAll(rcmdStringList);
		
		// 剩下的拿來填補空白
		for (int i=0; i < rows && realMostPopularStringList.size() > 0; i++) {
			if (temp[i] == null) {
				String str = realMostPopularStringList.remove(0);
				RecommendKeyword word = new RecommendKeyword();
				word.setKeyword(str);
				temp[i] = word;
			}
		}

		for (int k = 0; k < temp.length; k++)
			CollectionUtils.addIgnoreNull(result, temp[k]);
		return result;
	}

	private List<RecommendKeyword> getAdWords(int adWordNum,
			String adWordDataSetId) {
		List<RecommendKeyword> result = new ArrayList<RecommendKeyword>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();

			Criteria ct = ses.createCriteria(RecommendKeyword.class);
			ProjectionList p = Projections.projectionList();
			p.add(Projections.property("keyword"), "keyword");
			p.add(Projections.property("sort"), "sort");
			p.add(Projections.property("url"), "url");
			p.add(Projections.property("os"), "os");
			p.add(Projections.property("target"), "target");
			ct.setProjection(p);
			Order order1 = Order.asc("sort");
			Order order2 = Order.desc("saveTime");
			ct.addOrder(order1);
			ct.addOrder(order2);
			ct.setMaxResults(adWordNum);
			if (StringUtils.isNotBlank(adWordDataSetId)) {
				ct.add(Restrictions.or(Restrictions.sqlRestriction(
						"{alias}.os = (?)", "-1", Hibernate.STRING),
						Restrictions.sqlRestriction("{alias}.os = (?)",
								adWordDataSetId, Hibernate.STRING)));
			}
			ct.add(Restrictions.sqlRestriction("{alias}.sort <> (?)", "-1",
					Hibernate.STRING));
			List<Object[]> r = ct.list();
			for (Object[] row : r) {
				RecommendKeyword word = new RecommendKeyword();
				word.setKeyword((String) row[0]);
				word.setSort((Integer)row[1]);
				word.setUrl((String) row[2]);
				word.setOs((String) row[3]);
				word.setTarget((String) row[4]);
				result.add(word);
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

}
