package com.intumit.quartz;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class ScheduleLogFacade {
	
	public static synchronized ScheduleLogEntity getLatestByEventSource(String eventSource) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ScheduleLogEntity.class)
						.addOrder(Order.desc("timestamp"));
			ct.add( Restrictions.eq("eventSource", eventSource ) );
			
			result = ct.setFetchSize(1).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result.size() > 0 ? (ScheduleLogEntity)result.get(0) : null;
	}
	
	public static synchronized List<ScheduleLogEntity> listByEventSource(String eventSource) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ScheduleLogEntity.class)
						.addOrder(Order.desc("timestamp"));
			ct.add( Restrictions.eq("eventSource", eventSource ) );
				
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


	public static synchronized void save(ScheduleLogEntity log) {
		Session ses = null;
		Transaction tx = null;
		try {
			if (log.getTimestamp() == null)
				log.setTimestamp(Calendar.getInstance().getTime());
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
}
