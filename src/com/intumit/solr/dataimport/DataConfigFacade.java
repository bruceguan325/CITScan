package com.intumit.solr.dataimport;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class DataConfigFacade {
	
	public static synchronized DataConfig get(String coreName) {
		DataConfig cfg = null;

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DataConfig.class).addOrder(Order.desc("id"));
			ct.add( Restrictions.eq("targetCoreName", coreName ) );
			
			cfg = (DataConfig)ct.uniqueResult();
			if (cfg != null) {
				cfg.getFields();
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return cfg;
	}
	
	public static synchronized List<DataConfig> list() {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DataConfig.class).addOrder(Order.desc("id"));
			//t.add( Restrictions.eq("eventSource", eventSource ) );
				
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


	public static synchronized void save(DataConfig cfg) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			
			List<DataField> fields = cfg.getFields();
			cfg.setFields(fields);
			ses.saveOrUpdate(cfg);
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
}
