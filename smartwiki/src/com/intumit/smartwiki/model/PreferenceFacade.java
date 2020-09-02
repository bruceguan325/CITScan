package com.intumit.smartwiki.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class PreferenceFacade {
	static final PreferenceFacade instance = new PreferenceFacade();

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static PreferenceFacade getInstance() {
		return instance;
	}

	private PreferenceFacade() {
	}

	public synchronized void saveOrUpdate(Preference Preference) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(Preference);
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
			ses.createQuery("delete from " + Preference.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public synchronized List<Preference> listAll() {
		return list();
	}
	public synchronized Preference get(int id) {
		try {
			return (Preference)HibernateUtil.getSession().get(Preference.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
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
			Criteria ct = ses.createCriteria(Preference.class).addOrder(Order.asc("id"));
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
