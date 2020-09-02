package com.intumit.solr.searchKeywords;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.thoughtworks.xstream.XStream;

public class ClickLogFacade {
	public static final String DEFAULT_FUNC = "default";
	public static final String DEFAULT_IDENTITY = "unknown";
	static ClickLogFacade instance = null;
	public static final int MAX_TARGET_LENGTH = 255;
	
	/**
	 * 改成 Singleton
	 * @return
	 */
	public static ClickLogFacade getInstance() {
		if (instance == null) {
			instance = new ClickLogFacade();
		}
		return instance;
	}

	public void log(String target) {
		log(target, DEFAULT_FUNC, DEFAULT_IDENTITY);
	}

	/**
	 * 提供管道可以記錄不同的搜尋行為
	 * 例如不同的搜尋功能（快速搜尋，進階搜尋），供後續分析使用
	 * 
	 * @param word
	 * @param func
	 */
	public void log(String target, String func, String identity) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ClickLog newLog = new ClickLog();
			if (target.length() > MAX_TARGET_LENGTH)
				target = target.substring(0, MAX_TARGET_LENGTH);
			newLog.setTarget(target);
			newLog.setFunc(func);
			newLog.setIdentity(identity);
			newLog.setLogtime(new Timestamp(System.currentTimeMillis()));
			
			ses.saveOrUpdate(newLog);
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	public void log(String target, String title, String id, String func, String identity) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ClickLog newLog = new ClickLog();
			if (target.length() > MAX_TARGET_LENGTH)
				target = target.substring(0, MAX_TARGET_LENGTH);
			
			newLog.setTarget(target);
			newLog.setFunc(func);
			newLog.setIdentity(identity);
			newLog.setLogtime(new Timestamp(System.currentTimeMillis()));
			newLog.setClickId(id);
			newLog.setClickTitle(title);
			ses.saveOrUpdate(newLog);
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}

	public void deleteAll() throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + ClickLog.class.getName()).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> listMostPopular(int count) {
		List<String> result = new ArrayList<String>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();

			Criteria ct = ses.createCriteria(ClickLog.class);
			ProjectionList projList = Projections.projectionList();
			projList.add(Projections.groupProperty("target"));
			projList.add(Projections.count("target"), "ctn");
			ct.setProjection(projList);
			ct.addOrder(Order.desc("ctn"));
			ct.setMaxResults(count);

			List<Object[]> r = ct.list();
			for(Object[] row: r) {
				result.add((String)row[0]);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public List<String> listLatest(int count) {
		List<String> result = new ArrayList<String>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();

			Query query = ses.createQuery("select s.target from " + ClickLog.class.getName() + " s order by s.logtime desc");
			query.setMaxResults(count);
			result = query.list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<ClickLog> listMostPopularBeans(int count) {

		List<ClickLog> result = new ArrayList<ClickLog>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();

			Criteria ct = ses.createCriteria(ClickLog.class);
			ProjectionList projList = Projections.projectionList();
			projList.add(Projections.groupProperty("target"));
			projList.add(Projections.count("target"), "ctn");
			projList.add(Projections.max("logtime"), "ltime");
			ct.setProjection(projList);
			ct.addOrder(Order.desc("ctn"));
			ct.setMaxResults(count);

			List<Object[]> r = ct.list();
			for(Object[] row: r) {
				ClickLog log = new ClickLog();
				log.setTarget((String)row[0]);
				log.setLogtime((Timestamp)row[2]);
				result.add(log);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<ClickLog> listLatestBeans(int count) {
		List<ClickLog> result = new ArrayList<ClickLog>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Query query = ses.createQuery("from " + ClickLog.class.getName() + " s order by s.logtime desc");
			query.setMaxResults(count);
			result = query.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	public String listMostPopularXml(int count) {
		return out().toXML(listMostPopularBeans(count));
	}

	private XStream out() {
		XStream out = new XStream();
		out.alias("target", ClickLog.class);
		return out;
	}

	public String listLatestByXml(int count) {
		return out().toXML(listLatestBeans(count));
	}
	
	public List<ClickLog> getHotDocumentFromSearchBeans(String from, String to,
			String func, int count) {
		List<ClickLog> hotList = new ArrayList<ClickLog>();

		SolrServer server = SearchManager.getServer("core-click");
		SolrQuery query = new SolrQuery();
		query.addFilterQuery("LogTime:[" + from + " TO " + to + "]");
		if (func != null && !"default".equals(func))
			query.addFilterQuery("Func_s:" + func);
		query.setFacet(true);
		query.addFacetField("ClickTitle");
		query.setFacetLimit(count);
		query.setFacetMinCount(1);
		query.setQueryType("standard");
		query.setQuery("*:*");
		try {
			QueryResponse rsp = server.query(query);
			FacetField kwFF = rsp.getFacetField("ClickTitle");

			if (kwFF != null && kwFF.getValues() != null) {
				for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
						.iterator(); iterator2.hasNext();) {
					FacetField.Count facetCount = iterator2.next();
					String word = facetCount.getName();
					int ccc = (int) facetCount.getCount();
					ClickLog log = new ClickLog();

					if (ccc > 0) {
						log.setFrequency(ccc);
						log.setClickTitle(word);
						hotList.add(log);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return hotList;
	}
	
	
	public List<String> listHotDocuments(int count) {
		List<String> hotList = new ArrayList<String>();

		SolrServer server = SearchManager.getServer("core-click");
		SolrQuery query = new SolrQuery();
		query.setFacet(true);
		query.addFacetField("ClickTitle");
		query.setFacetLimit(count);
		query.setFacetMinCount(1);
		query.setQueryType("standard");
		query.setQuery("*:*");
		try {
			QueryResponse rsp = server.query(query);
			FacetField kwFF = rsp.getFacetField("ClickTitle");

			if (kwFF != null && kwFF.getValues() != null) {
				for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
						.iterator(); iterator2.hasNext();) {
					FacetField.Count facetCount = iterator2.next();
					String word = facetCount.getName();
					int ccc = (int) facetCount.getCount();
					ClickLog log = new ClickLog();

					if (ccc > 0) {
						hotList.add(word);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return hotList;
	}

}
