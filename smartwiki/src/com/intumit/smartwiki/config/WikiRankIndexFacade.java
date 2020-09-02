package com.intumit.smartwiki.config;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class WikiRankIndexFacade {
	static final WikiRankIndexFacade instance = new WikiRankIndexFacade();

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static WikiRankIndexFacade getInstance() {
		return instance;
	}

	private WikiRankIndexFacade() {
	}


	public synchronized void save(String lang, String location) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WikiRankIndex WikiRankIndex = new WikiRankIndex();
			WikiRankIndex.setLang(lang);
			WikiRankIndex.setIndexLocation(location);
			ses.saveOrUpdate(WikiRankIndex);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized void saveOrUpdate(WikiRankIndex wikiRankIndex) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(wikiRankIndex);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized void delete(int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + WikiRankIndex.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized List<WikiRankIndex> listAll() {
		return list();
	}
	public synchronized WikiRankIndex get(int id) {
		try {
			return (WikiRankIndex)HibernateUtil.getSession().get(WikiRankIndex.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	public synchronized WikiRankIndex get(String lang) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();

			Criteria ct = ses.createCriteria(WikiRankIndex.class);
			ct.add( Restrictions.eq("lang", lang ) );
			List result = ct.list();
			tx.commit();
			
			if (result.size() > 0)
				return (WikiRankIndex)result.get(0);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private List list() {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WikiRankIndex.class).addOrder(Order.asc("id"));
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
}
