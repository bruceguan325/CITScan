package com.intumit.solr.dataimport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class DihLogFacade {

	public enum Status {
		IDLE, COMPLETED, ABORTED, RUNNING, FAILED
	}

	public static Status explainStatus(DihLogEntity log) {
		Status judge = Status.RUNNING;
		
		String statusMsg = log.getStatusMessage().toLowerCase();
		if (statusMsg.contains("completed")) {
			judge = Status.COMPLETED;
		}
		else if (statusMsg.contains("aborted")) {
			judge = Status.ABORTED;
		}
		else if (statusMsg.contains("failed")) {
			judge = Status.FAILED;
		}
		else {
			if (statusMsg.contains("time taken")) {
				judge = Status.FAILED;
			}
			else if (StringUtils.containsIgnoreCase(statusMsg, DihEndEventListener.class.getName())) {
				judge = Status.IDLE;
			}
		}
		
		return judge;
	}

	public static synchronized DihLogEntity getLatestByEventSource(
			String eventSource) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DihLogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("eventSource", eventSource));

			result = ct.setFetchSize(1).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result.size() > 0 ? (DihLogEntity) result.get(0) : null;
	}

	public static synchronized List<DihLogEntity> listByEventSource(
			String eventSource) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DihLogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("eventSource", eventSource));

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

	public static synchronized void save(DihLogEntity log) {
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
}
