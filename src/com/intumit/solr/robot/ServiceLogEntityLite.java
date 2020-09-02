package com.intumit.solr.robot;

import java.io.Serializable;
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
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.servlet.SolrDispatchFilter;

/**
 * Lite 版 ServiceLogEntity
 * Mapping 到同一個 table，只是 Entity 本身不含 conversation，減少大多 list 操作不必要的記憶體消耗
 * 
 * @author herb
 */
@Entity
@Table(name = "ServiceLogEntity")
public class ServiceLogEntityLite implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Column(length = 32)
	@Index(name = "namespaceIdx")
	private String namespace;

	@Column(length = 64)
	@Index(name = "statusIdx")
	private String status;

	@Column(length = 64)
	@Index(name = "clientIpIdx")
	private String clientIp;

	@Column(length = 64, name = "logIdentity")
	@Index(name = "identityIdx")
	private String identity;

	@Column(length = 512)
	private String parameters;
	
	@Lob
	private String channel;

	@Lob
	private String statusMessage;

	@Lob
	private String moreDetails;

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
	private Integer statMsgCountHasAnswer = 0;
	private Integer statMsgCountNoAnswer = 0;
	private Integer statForward; // 只要非繼續對話的都視同轉出
	
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

	public Date getTsCreated() {
		return tsCreated;
	}

	public void setTsCreated(Date tsCreated) {
		this.tsCreated = tsCreated;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
	
	public String getChannel() {
		return channel;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getMoreDetails() {
		return moreDetails;
	}

	public void setMoreDetails(String moreDetails) {
		this.moreDetails = moreDetails;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public ServiceLogEntityLite() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public ServiceLogEntity getEntity() {
		return id != null ? ServiceLogEntity.get(id) : null;
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

	public Integer getStatMsgCountHasAnswer() {
		return statMsgCountHasAnswer != null ? statMsgCountHasAnswer : 0;
	}

	public void setStatMsgCountHasAnswer(Integer statMsgCountHasAnswer) {
		this.statMsgCountHasAnswer = statMsgCountHasAnswer;
	}

	public Integer getStatMsgCountNoAnswer() {
		return statMsgCountNoAnswer != null ? statMsgCountNoAnswer : 0;
	}

	public void setStatMsgCountNoAnswer(Integer statMsgCountNoAnswer) {
		this.statMsgCountNoAnswer = statMsgCountNoAnswer;
	}

	public Integer getStatForward() {
		return statForward != null ? statForward : 0;
	}

	public void setStatForward(Integer statForward) {
		this.statForward = statForward;
	}

	public Integer getReviewedBy() {
		return reviewedBy;
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

	private void appendToMoreDetails(String more) {
		if (StringUtils.isEmpty(moreDetails)) {
			moreDetails = "";
		}
		moreDetails += more;
	}

	public static synchronized List<ServiceLogEntityLite> listByReviewStatus(
			Integer tenantId, String namespace, int reviewStatus, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			ct.add(Restrictions.eq("namespace", namespace));
			if(reviewStatus == 0){
				ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
			}else{
				ct.add(Restrictions.eq("reviewStatus", reviewStatus));
			}

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

	public static synchronized List<ServiceLogEntityLite> listLatestByStatus(
			Integer tenantId, String namespace, String status, Calendar after, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class)
					.addOrder(Order.desc("timestamp"))
					.addOrder(Order.desc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			ct.add(Restrictions.eq("namespace", namespace));
			ct.add(Restrictions.eq("status", status));
			if (after != null) {
				ct.add(Restrictions.ge("timestamp", after.getTime()));
			}

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

	public static synchronized void save(ServiceLogEntityLite log) {
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

	public static void setIdentitesAndSave(HttpServletRequest req, ServiceLogEntityLite log) {
		if (req != null) {
			String ip = SolrDispatchFilter.getClientIpAddr(req);
			log.setClientIp(ip);
			log.appendToMoreDetails(req.getHeader("User-Agent"));
		}
		else {
			log.setClientIp("No client");
		}

		save(log);
	}
	
	public static synchronized List<ServiceLogEntityLite> listBy(Integer tenantId, String namespace, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before, int start, int rows) {
		return listBy(tenantId, namespace, qaId, reviewStatus, lastQaCategory, after, before, null, start, rows);
	}
	
	/**
	 * 增加了一個沒有回答問題次數的查詢參數
	 * 
	 * @param tenantId
	 * @param namespace
	 * @param qaId
	 * @param reviewStatus
	 * @param lastQaCategory
	 * @param after
	 * @param before
	 * @param countNoAnswerGreaterThan
	 * @param start
	 * @param rows
	 * @return
	 */
	public static synchronized List<ServiceLogEntityLite> listBy(Integer tenantId, String namespace, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before, Integer countNoAnswerGreaterThan, int start, int rows) {
		List<ServiceLogEntityLite> result = new ArrayList<ServiceLogEntityLite>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class).addOrder(Order.desc("timestamp"));;

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
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

			if (countNoAnswerGreaterThan != null) {
				ct.add(Restrictions.gt("statMsgCountNoAnswer", countNoAnswerGreaterThan));
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
	
	public static synchronized List<ServiceLogEntityLite> listByDate(Integer tenantId, String namespace, Date after, Date before) {
		List<ServiceLogEntityLite> result = new ArrayList<ServiceLogEntityLite>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class).addOrder(Order.asc("timestamp"));;

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
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
	public static synchronized Number countBy(Integer tenantId, String namespace, String qaId, Integer[] statForward, Integer reviewStatus, String lastQaCategory, Date after, Date before) {
		return countBy(tenantId, namespace, qaId, statForward, reviewStatus, lastQaCategory, after, before, null);
	}

	public static synchronized Number countBy(Integer tenantId, String namespace, String qaId, Integer[] statForward, Integer reviewStatus, String lastQaCategory, Date after, Date before, Integer countNoAnswerGreaterThan) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
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
			if (statForward != null) {
				ct.add(Restrictions.in("statForward", statForward));
			}
			if (countNoAnswerGreaterThan != null) {
				ct.add(Restrictions.gt("statMsgCountNoAnswer", countNoAnswerGreaterThan));
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

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}
	
	/**
	 * Get by qaId, sort by timestamp DESC
	 * 
	 * @param tenantId
	 * @param qaId
	 * @param timeWindowInSecond NOT required, only get the log in specific time window.
	 * @param limit NOT required, only get the latest "limit" logs
	 * @return
	 */
	public static synchronized List<ServiceLogEntityLite> getFromQaId(Integer tenantId, String qaId, Integer timeWindowInSecond, Integer limit) {
		List<ServiceLogEntityLite> result = new ArrayList<ServiceLogEntityLite>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class)
					.addOrder(Order.desc("timestamp"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (qaId != null) {
				ct.add(Restrictions.eq("qaId", qaId));
			}
			if (timeWindowInSecond != null) {
				ct.add(Restrictions.gt("timestamp", new Date(
						System.currentTimeMillis() - (timeWindowInSecond * 1000))));	
			}
			
			if (limit != null) {
				ct.setMaxResults(limit);
			}

			result = ct.list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized List<ServiceLogEntityLite> listByIdentity(Integer tenantId, String identity, Integer reviewStatus, String lastQaCategory, Date after, Date before, int start, int rows) {
		List<ServiceLogEntityLite> result = new ArrayList<ServiceLogEntityLite>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (identity != null) {
				ct.add(Restrictions.eq("identity", identity));
			}
			if (reviewStatus != null) {
				if(reviewStatus == 0){
					ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
				}else{
					ct.add(Restrictions.eq("reviewStatus", reviewStatus));
				}
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
	
	public static synchronized Number countByIdentity(Integer tenantId, String identity, Integer[] statForward, Integer reviewStatus, String lastQaCategory, Date after, Date before) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntityLite.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (reviewStatus != null) {
				if(reviewStatus == 0){
					ct.add(Restrictions.or(Restrictions.eq("reviewStatus", reviewStatus), Restrictions.isNull("reviewStatus")));
				}else{
					ct.add(Restrictions.eq("reviewStatus", reviewStatus));
				}
			}
			if (identity != null) {
				ct.add(Restrictions.eq("identity", identity));
			}
			if (statForward != null) {
				ct.add(Restrictions.in("statForward", statForward));
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

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}
}
