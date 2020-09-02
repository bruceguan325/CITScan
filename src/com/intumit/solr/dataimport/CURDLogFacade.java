package com.intumit.solr.dataimport;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class CURDLogFacade {
	static final CURDLogFacade instance = new CURDLogFacade();

	/**
	 * Singleton
	 * 
	 * @return
	 */
	public static CURDLogFacade getInstance() {
		return instance;
	}

	private CURDLogFacade() {
	}
	
	public synchronized void save(String coreName, String entityName, String pk, String action) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			CURDLog log = new CURDLog();
			log.setCoreName(coreName);
			log.setEntityName(entityName);
			log.setPrimaryKey(pk);
			log.setAction(action);
			log.setTimestamp(Calendar.getInstance().getTime());
			ses.saveOrUpdate(log);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public synchronized void saveOrUpdate(CURDLog log) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(log);
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
			ses.createQuery("delete from " + CURDLog.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public synchronized List<CURDLog> listAll() {
		return list();
	}
		
	public synchronized CURDLog get(int id) {
		try {
			return (CURDLog)HibernateUtil.getSession().get(CURDLog.class, id);
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
			Criteria ct = ses.createCriteria(CURDLog.class).addOrder(Order.asc("id"));
//			if (xdmpId != null)
//				ct.add( Restrictions.eq("xdmp", xdmpId ) );
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
