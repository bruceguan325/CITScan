package com.intumit.solr.recommendKeywords;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class RecommendKeywordFacade {
	public static final String DEFAULT_FUNC = "default";
	static final RecommendKeywordFacade instance = new RecommendKeywordFacade();
	public static final int MAX_KEYWORD_LENGTH = 128;

	/**
	 * 改成 Singleton
	 * 
	 * @return
	 */
	public static RecommendKeywordFacade getInstance() {
		return instance;
	}

	private RecommendKeywordFacade() {
	}

	public synchronized RecommendKeyword save(String keyword, String sort, String os,String url,String target) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			RecommendKeyword word = new RecommendKeyword();
			word.setKeyword(keyword);
			word.setSort(Integer.valueOf(sort));
			word.setUrl(url);
			word.setOs(os);
			word.setSaveTime(new Timestamp(System.currentTimeMillis()));
			word.setTarget(target);
			ses.saveOrUpdate(word);
			tx.commit();
			return word;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return null;
	}
	
	
	public synchronized RecommendKeyword update(String id, String keyword,  String sort, String os,String url,String target) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			RecommendKeyword word = new RecommendKeyword();
			word.setId(Long.parseLong(id));
			word.setKeyword(keyword);
			word.setSort(Integer.valueOf(sort));
			word.setUrl(url);
			word.setOs(os);
			word.setSaveTime(new Timestamp(System.currentTimeMillis()));
			word.setTarget(target);
			ses.update(word);
			tx.commit();
			
			return word;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return null;
	}

	
	public synchronized RecommendKeyword delete(String id) throws Exception {
		RecommendKeyword word = get(id);
		
		if (word != null) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.createQuery("delete from " + RecommendKeyword.class.getName() + " Where id=" + id)
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
			return word;
		}
		else {
			return null;
		}
	}
	

	
	public synchronized List<RecommendKeyword> listRecommendKeywords() {
		return list();
	}
	
	public synchronized List<RecommendKeyword> listRecommendKeywords(String searchKeyword) {
		return list(searchKeyword);
	}
	
	@SuppressWarnings("unchecked")
	private List list() {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(RecommendKeyword.class);
			Order order = Order.desc("saveTime");
			ct.addOrder(order);
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
	
	private List list(String searchKeyword) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(RecommendKeyword.class);
			Criterion c1 = (Restrictions.like("keyword","%" + searchKeyword + "%"));
			//Criterion c2 = (Restrictions.like("synonymKeyword","%" + searchKeyword + "%"));
			//LogicalExpression orExp = Restrictions.or(c1,c2);
			ct.add(c1);
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

	public synchronized RecommendKeyword get(String id) {
		try {
			return (RecommendKeyword)HibernateUtil.getSession().get(RecommendKeyword.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	


	


}
