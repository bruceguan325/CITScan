package com.intumit.syslog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.user.User;

@Entity
public class SyslogEntity {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;


	@Column(length = 32)
	@Index(name = "namespaceIdx")
	private String namespace;

	@Column(length = 64)
	@Index(name = "eventIdx")
	private String event;

	@Column(length = 64)
	@Index(name = "clientIpIdx")
	private String clientIp;

	@Column(length = 64, name = "logIdentity")
	@Index(name = "identityIdx")
	private String identity;

	@Column(length = 512)
	private String parameters;

	@Lob
	private String statusMessage;

	@Lob
	private String moreDetails;

	@Index(name = "timestampIdx")
	private Date timestamp;



	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getMoreDetails() {
		return moreDetails;
	}

	public void setMoreDetails(String moreDetails) {
		this.moreDetails = moreDetails;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public SyslogEntity() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}


	public static synchronized SyslogEntity getLatestByEvent(
			String namespace, String event) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SyslogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("namespace", namespace));
			ct.add(Restrictions.eq("event", event));

			result = ct.setMaxResults(1).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result.size() > 0 ? (SyslogEntity) result.get(0) : null;
	}


	public static synchronized List<SyslogEntity> listLatestByUser(
			int userId, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SyslogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("identity", "uid:" + userId));

			result = ct.setMaxResults(max).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized List<SyslogEntity> listByEvent(
			String namespace, String event, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SyslogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("namespace", namespace));
			ct.add(Restrictions.eq("event", event));

			result = ct.setMaxResults(max).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized List<SyslogEntity> listByNS(
			String namespace, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(SyslogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			ct.add(Restrictions.eq("namespace", namespace));

			result = ct.setMaxResults(max).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized void save(SyslogEntity log) {
		Session ses = null;
		Transaction tx = null;
		try {
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

	public static void setIdentitesAndSave(HttpServletRequest req, SyslogEntity log) {
		String ip = SolrDispatchFilter.getClientIpAddr(req);
		String identity = null;

		String reqUri = req.getRequestURI();
		String admPath = req.getContextPath() + "/wiseadm";

		if (StringUtils.startsWithIgnoreCase(reqUri, admPath)) {
			AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(req.getSession());
			if (user == null) {
				identity = null;
			}
			else {
				identity = "aid:" + user.getId();
			}
		}
		else {
			User user = User.getFromSession(req.getSession());
			if (user == null) {
				identity = null;
			}
			else {
				identity = "uid:" + user.getId();
			}
		}

		log.setClientIp(ip);
		if (identity != null) log.setIdentity(identity);
		log.appendToMoreDetails(req.getHeader("User-Agent"));

		save(log);
	}

	private void appendToMoreDetails(String more) {
		if (StringUtils.isEmpty(moreDetails)) {
			moreDetails = "";
		}
		moreDetails += more;
	}

	public static void log(HttpServletRequest req, String namespace, String event, String param, String statusMessage) {
		SyslogEntity log = new SyslogEntity();
		log.setNamespace(namespace);
		log.setEvent(event);
		log.setParameters(param);
		log.setStatusMessage(statusMessage);
		setIdentitesAndSave(req, log);
	}
}
