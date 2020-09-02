package com.intumit.solr.robot;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.tenant.Tenant;

@Entity
public class TextCrmServiceLogEntity implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;
	
	@Column(length = 450)
	private String tag;

	@Column(length = 64)
	@Index(name = "clientIpIdx")
	private String clientIp;

	@Column(length = 64, name = "logIdentity")
	@Index(name = "identityIdx")
	private String identity;

	@Lob
	private String conversations;

	@Lob
	private String firstMessage;

	@Lob
	private String lastMessage;
	
	@Column(length = 32)
	@Index(name = "lastQaCategoryIdx")
	private String lastQaCategory;

	@Index(name = "timestampIdx")
	private Date timestamp;

	@Index(name = "tsCreatedIdx")
	private Date tsCreated;

	@Column(length = 256)
	@Index(name = "qaIdIdx")
	private String qaId;
	
	/**
	 * 統計區，方便快速統計用
	 */
	private Integer statMsgCountFromUser = 0;
	private Integer statMsgCountFromRobot = 0;
	private Integer statMsgCountTotal = 0;
	
	/**
	 * 0: Not Reviewed
	 * 1: Reviewed
	 */
	@Index(name = "reviewStatusIdx")
	private Integer reviewStatus = 0;
	
	@Index(name = "reviewedByIdx")
	private Integer reviewedBy;

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Date getTsCreated() {
		return tsCreated;
	}

	public void setTsCreated(Date tsCreated) {
		this.tsCreated = tsCreated;
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

	public TextCrmServiceLogEntity() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getConversations() {
		return conversations;
	}

	public void setConversations(String conversations) {
		this.conversations = conversations;
	}

	public String getFirstMessage() {
		return firstMessage;
	}

	public void setFirstMessage(String firstMessage) {
		this.firstMessage = firstMessage;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

	public String getLastQaCategory() {
		return lastQaCategory;
	}

	public void setLastQaCategory(String lastQaCategory) {
		this.lastQaCategory = lastQaCategory;
	}

	public String getQaId() {
		return qaId;
	}

	public void setQaId(String qaId) {
		this.qaId = qaId;
	}

	public Integer getStatMsgCountFromUser() {
		return statMsgCountFromUser != null ? statMsgCountFromUser : 0;
	}

	public void setStatMsgCountFromUser(Integer statMsgCountFromUser) {
		this.statMsgCountFromUser = statMsgCountFromUser;
	}

	public Integer getStatMsgCountFromRobot() {
		return statMsgCountFromRobot != null ? statMsgCountFromRobot : 0;
	}

	public void setStatMsgCountFromRobot(Integer statMsgCountFromRobot) {
		this.statMsgCountFromRobot = statMsgCountFromRobot;
	}

	public Integer getStatMsgCountTotal() {
		return statMsgCountTotal != null ? statMsgCountTotal : 0;
	}

	public void setStatMsgCountTotal(Integer statMsgCountTotal) {
		this.statMsgCountTotal = statMsgCountTotal;
	}

	public Integer getReviewedBy() {
		return reviewedBy;
	}
	
	@Override
	public String toString() {
		return "TextCrmServiceLogEntity [id=" + id + ", tenantId=" + tenantId + ", tag=" + tag + ", clientIp=" + clientIp + ", identity=" + identity
				+ ", conversations=" + conversations + ", lastMessage=" + lastMessage + ", lastQaCategory=" + lastQaCategory + ", timestamp=" + timestamp
				+ ", tsCreated=" + tsCreated + ", qaId=" + qaId + ", statMsgCountFromUser=" + statMsgCountFromUser + ", statMsgCountFromRobot="
				+ statMsgCountFromRobot + ", statMsgCountTotal=" + statMsgCountTotal + ", reviewStatus=" + reviewStatus + ", reviewedBy=" + reviewedBy + "]";
	}

	public void setReviewedBy(Integer reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public Integer getReviewStatus() {
		return reviewStatus != null ? reviewStatus : 0;
	}

	public void setReviewStatus(Integer reviewStatus) {
		this.reviewStatus = reviewStatus;
	}

	public static synchronized TextCrmServiceLogEntity get(int id) {
		try {
			return (TextCrmServiceLogEntity)HibernateUtil.getSession().get(TextCrmServiceLogEntity.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized List<TextCrmServiceLogEntity> listLatestByUser(
			Integer tenantId, int userId, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
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

	public static synchronized List<TextCrmServiceLogEntity> listByReviewStatus(
			Integer tenantId, int reviewStatus, int start, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if(reviewStatus == 0){
				ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
			}else{
				ct.add(Restrictions.eq("reviewStatus", reviewStatus));
			}

			ct.setFirstResult(start);
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

	public static synchronized List<TextCrmServiceLogEntity> listByTag(
			Integer tenantId, String tag, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			ct.add(Restrictions.eq("tag", tag));

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

	public static synchronized void save(TextCrmServiceLogEntity log) {
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

	public static void setIdentitesAndSave(HttpServletRequest req, TextCrmServiceLogEntity log) {
		String ip = SolrDispatchFilter.getClientIpAddr(req);
		log.setClientIp(ip);

		save(log);
	}
	
	public static synchronized List<TextCrmServiceLogEntity> listBy(Integer tenantId, String tag, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before, int start, int rows) {
		List<TextCrmServiceLogEntity> result = new ArrayList<TextCrmServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (tag != null) {
				ct.add(Restrictions.eq("tag", tag));
			}
			if (reviewStatus != null) {
				if(reviewStatus == 0){
					ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
				}else{
					ct.add(Restrictions.eq("reviewStatus", reviewStatus));
				}
			}
			if (qaId != null) {
				ct.add(Restrictions.eq("qaId", qaId));
			}

			if (after != null && before != null) {
				ct.add(
						Restrictions.and(
								Restrictions.ge("timestamp", after),
								Restrictions.lt("timestamp", before)
								));
			}
			else if (after != null) {
				ct.add(Restrictions.ge("timestamp", after));
			}
			else if (before != null) {
				ct.add(Restrictions.le("timestamp", before));
			}

			if(StringUtils.isNotBlank(lastQaCategory)) {
				if (QAUtil.GENERAL_CATEGORIES.contains(lastQaCategory)) {
					
					ct.add(Restrictions.or(
							Restrictions.isNull("lastQaCategory"),
							Restrictions.eq("lastQaCategory", lastQaCategory)
							));
				}
				else {
					ct.add(Restrictions.eq("lastQaCategory", lastQaCategory));
				}
			}
			result = ct.setFirstResult(start).setMaxResults(rows).list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	public static boolean isBotName(String identity) {
		return (StringUtils.equalsIgnoreCase(identity, "bot")
				|| StringUtils.equalsIgnoreCase(identity, "chatbot"));
	}
	
	public static synchronized List<TextCrmServiceLogEntity> listByDate(Integer tenantId, String tag, Date after, Date before) {
		List<TextCrmServiceLogEntity> result = new ArrayList<TextCrmServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (tag != null) {
				ct.add(Restrictions.eq("tag", tag));
			}

			if (after != null && before != null) {
				ct.add(
						Restrictions.and(
								Restrictions.ge("timestamp", after),
								Restrictions.lt("timestamp", before)
								));
			}
			else if (after != null) {
				ct.add(Restrictions.ge("timestamp", after));
			}
			else if (before != null) {
				ct.add(Restrictions.le("timestamp", before));
			}

			result = ct.list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized Number countBy(Integer tenantId, String tag, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (tag != null) {
				ct.add(Restrictions.eq("tag", tag));
			}
			if (reviewStatus != null) {
				if(reviewStatus == 0){
					ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
				}else{
					ct.add(Restrictions.eq("reviewStatus", reviewStatus));
				}
			}
			if (qaId != null) {
				ct.add(Restrictions.eq("qaId", qaId));
			}

			if (after != null && before != null) {
				ct.add(
						Restrictions.and(
								Restrictions.ge("timestamp", after),
								Restrictions.lt("timestamp", before)
								));
			}
			else if (after != null) {
				ct.add(Restrictions.ge("timestamp", after));
			}
			else if (before != null) {
				ct.add(Restrictions.le("timestamp", before));
			}

			if (StringUtils.isNotBlank(lastQaCategory)) {
				if (QAUtil.GENERAL_CATEGORIES.contains(lastQaCategory)) {
					
					ct.add(Restrictions.or(
							Restrictions.isNull("lastQaCategory"),
							Restrictions.eq("lastQaCategory", lastQaCategory)
							));
				}
				else {
					ct.add(Restrictions.eq("lastQaCategory", lastQaCategory));
				}
			}

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}
	
	public static TextCrmServiceLogEntity log(Tenant t, HttpServletRequest req, String tag, String identity, String status, String param, String statusMessage) {
		TextCrmServiceLogEntity log = new TextCrmServiceLogEntity();
		log.setTenantId(t.getId());
		log.setTag(tag);
		log.setIdentity(identity);
		log.setTsCreated(Calendar.getInstance().getTime());
		log.setConversations(new JSONArray().toString());
		setIdentitesAndSave(req, log);
		
		return log;
	}

	public static TextCrmServiceLogEntity log(QAContext qaCtx, HttpServletRequest req, String tag, String identity, String status, String param, String statusMessage) {
		Tenant t = qaCtx.getTenant();

		TextCrmServiceLogEntity log = new TextCrmServiceLogEntity();
		log.setTenantId(t.getId());
		log.setTag(tag);
		log.setIdentity(identity);
		log.setTsCreated(Calendar.getInstance().getTime());
		log.setConversations(new JSONArray().toString());
		setIdentitesAndSave(req, log);

		return log;
	}

	public static TextCrmServiceLogEntity getFromSession(int tenantId, String qaId) {
		TextCrmServiceLogEntity log = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class)
					.addOrder(Order.desc("timestamp"));
			ct.add(Restrictions.eq("qaId", qaId));
			ct.add(Restrictions.eq("tenantId", tenantId));
			ct.add(Restrictions.gt("timestamp", new Date(
				System.currentTimeMillis() - (QAUtil.QA_SESSION_TIMEOUT_MINS * 60 * 1000))));

			log = (TextCrmServiceLogEntity) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return log;
	}
	
	public static synchronized List<TextCrmServiceLogEntity> getFromQaId(Integer tenantId, String qaId) {
		List<TextCrmServiceLogEntity> result = new ArrayList<TextCrmServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TextCrmServiceLogEntity.class)
					.addOrder(Order.asc("timestamp"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (qaId != null) {
				ct.add(Restrictions.eq("qaId", qaId));
			}

			result = ct.list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static void setIntoSession(String qaId, TextCrmServiceLogEntity log) {
		log.qaId = qaId;
		save(log);
	}

}
