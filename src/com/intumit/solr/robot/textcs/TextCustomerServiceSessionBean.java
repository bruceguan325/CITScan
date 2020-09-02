package com.intumit.solr.robot.textcs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class TextCustomerServiceSessionBean {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	Integer id;
	
	@Column(length = 32)
	String sessionId;
	
	@Column(length = 32)
	String customerId;
	
	@Column(length = 32)
	String customerNickName;
	
	@Lob
	String referer;
	
	@Column(length = 32)
	String ip;
	
	@Column(length = 32)
	String status;
	Date dateEnterWebsite;				// If we can trace cookie on the official website, we can calculate it.
	Date dateAskRobotService;			// The time
	Date dateRobotServiceBegin;			// The time
	Date dateAskCustomerService;		// The time
	
	@Column(length = 32)
	Integer customerServiceId;			// Null if user don't ask text customer service
	
	Date dateCustomerServiceBegin;		// Same as above
	
	Integer customerServiceDuration; 	// Only update when user leave (in seconds)

	@Lob
	String conversation;				// long json of user conversation

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getCustomerNickName() {
		return customerNickName;
	}

	public void setCustomerNickName(String customerNickName) {
		this.customerNickName = customerNickName;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getDateEnterWebsite() {
		return dateEnterWebsite;
	}

	public void setDateEnterWebsite(Date dateEnterWebsite) {
		this.dateEnterWebsite = dateEnterWebsite;
	}

	public Date getDateAskRobotService() {
		return dateAskRobotService;
	}

	public void setDateAskRobotService(Date dateAskRobotService) {
		this.dateAskRobotService = dateAskRobotService;
	}

	public Date getDateRobotServiceBegin() {
		return dateRobotServiceBegin;
	}

	public void setDateRobotServiceBegin(Date dateRobotServiceBegin) {
		this.dateRobotServiceBegin = dateRobotServiceBegin;
	}

	public Date getDateAskCustomerService() {
		return dateAskCustomerService;
	}

	public void setDateAskCustomerService(Date dateAskCustomerService) {
		this.dateAskCustomerService = dateAskCustomerService;
	}

	public Integer getCustomerServiceId() {
		return customerServiceId;
	}

	public void setCustomerServiceId(Integer customerServiceId) {
		this.customerServiceId = customerServiceId;
	}

	public Date getDateCustomerServiceBegin() {
		return dateCustomerServiceBegin;
	}

	public void setDateCustomerServiceBegin(Date dateCustomerServiceBegin) {
		this.dateCustomerServiceBegin = dateCustomerServiceBegin;
	}

	public Integer getCustomerServiceDuration() {
		return customerServiceDuration;
	}

	public void setCustomerServiceDuration(Integer customerServiceDuration) {
		this.customerServiceDuration = customerServiceDuration;
	}

	public String getConversation() {
		return conversation;
	}

	public void setConversation(String conversation) {
		this.conversation = conversation;
	}
	
	public synchronized void saveOrUpdate(TextCustomerServiceSessionBean bean) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(bean);
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
			ses.createQuery("delete from " + TextCustomerServiceSessionBean.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public synchronized List<TextCustomerServiceSessionBean> listAll() {
		return list(null);
	}
	
	public synchronized TextCustomerServiceSessionBean get(int id) {
		try {
			return (TextCustomerServiceSessionBean)HibernateUtil.getSession().get(TextCustomerServiceSessionBean.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	@SuppressWarnings("unchecked")
	private List list(String status) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TextCustomerServiceSessionBean.class).addOrder(Order.asc("dateAskCustomerService")).addOrder(Order.asc("id"));
			if (status != null)
				ct.add(Restrictions.eq("status", status));
			result = ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
}
