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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.tenant.Tenant;

@Entity
public class ServiceLogEntity implements Serializable {

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
	@Basic(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	private String conversations;

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
	
	@Column(length = 128)
    @Index(name = "sessionIdIdx")
    private String sessionId;
    
	@Column(length = 256,name="userID")
	@Index(name = "userIdIdx")
	private String userId;
	
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

	public ServiceLogEntity() {
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


	public String getConversations() {
		return conversations;
	}

	public void setConversations(String conversations) {
		this.conversations = conversations;
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

	public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
	public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
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

	public static synchronized ServiceLogEntity get(int id) {
		try {
			return (ServiceLogEntity)HibernateUtil.getSession().get(ServiceLogEntity.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized List<ServiceLogEntity> listByReviewStatus(
			Integer tenantId, String namespace, int reviewStatus, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class)
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

	public static synchronized List<ServiceLogEntity> listLatestByStatus(
			Integer tenantId, String namespace, String status, Calendar after, int max) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class)
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

	public static synchronized void save(ServiceLogEntity log) {
		Session ses = null;
		Transaction tx = null;
		try {
			log.setTimestamp(Calendar.getInstance().getTime());
			log.setQaId(StringUtils.substring(log.getQaId(), 0, 256));
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
	
	public static synchronized void saveWithoutUpdateTimestamp(ServiceLogEntity log) {
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
	
	/**
	 * 更新 Review Status
	 * 不更新 timestamp
	 * 
	 * @param log
	 * @param status
	 * @param byUser
	 */
	public static void updateReviewStatus(ServiceLogEntity log, Integer status, AdminUser byUser) {
		Session ses = null;
		Transaction tx = null;
		try {
			log.setReviewStatus(status);
			log.setReviewedBy(byUser.getId());
			
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

	public static void setIdentitesAndSave(HttpServletRequest req, ServiceLogEntity log) {
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
	
	public static synchronized List<ServiceLogEntity> listBy(Integer tenantId, String namespace, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before, int start, int rows) {
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
	public static synchronized List<ServiceLogEntity> listBy(Integer tenantId, String namespace, String qaId, Integer reviewStatus, String lastQaCategory, Date after, Date before, Integer countNoAnswerGreaterThan, int start, int rows) {
		List<ServiceLogEntity> result = new ArrayList<ServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class).addOrder(Order.desc("timestamp"));;

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
	
	public static synchronized List<ServiceLogEntity> listByDate(Integer tenantId, String namespace, Date after, Date before) {
		List<ServiceLogEntity> result = new ArrayList<ServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class).addOrder(Order.asc("timestamp"));;

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
			Criteria ct = ses.createCriteria(ServiceLogEntity.class);

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
	
	@SuppressWarnings("deprecation")
	public static float[] hitrateStatistic(Date after, Date before, Integer tid)
			throws Exception, SQLException {
		Connection con = null;
		PreparedStatement pstmt = null;
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Session ses = null;
		float rate = 0;
		int statMsgCountNoAnswer = 0;
		int statMsgCountHasAnswer = 0;
		int statMsgCountFromUser = 0;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String mssql = "select (case when sum(statMsgCountFromUser) = 0 then 0 else ROUND((CAST(sum(statMsgCountHasAnswer) AS float)/CAST(sum(statMsgCountFromUser) AS float)),3) end) as rate, sum(statMsgCountNoAnswer) as statMsgCountNoAnswer, sum(statMsgCountHasAnswer) as statMsgCountHasAnswer, sum(statMsgCountFromUser) as statMsgCountFromUser from ServiceLogEntity sle "
					+ "where " + (tid != null ? "sle.tenantId = ? and " : "") + " sle.timestamp between ? and ?";
			String mysql = "select if(sum(statMsgCountFromUser) = 0, 0, sum(statMsgCountHasAnswer)/sum(statMsgCountFromUser)) as rate, sum(statMsgCountNoAnswer) as statMsgCountNoAnswer, sum(statMsgCountHasAnswer) as statMsgCountHasAnswer, sum(statMsgCountFromUser) as statMsgCountFromUser from ServiceLogEntity sle "
					+ "where " + (tid != null ? "sle.tenantId = ? and " : "") + " sle.timestamp between ? and ?";
			pstmt = null;
			switch (HibernateUtil.SQL_TYPE) {
				case MYSQL: pstmt = con.prepareStatement(mysql); break;
				case MSSQL: pstmt = con.prepareStatement(mssql); break;
			}
			
			int paramIdx = 1;
			if (tid != null) {
				pstmt.setString(paramIdx++, String.valueOf(tid));
			}
			pstmt.setString(paramIdx++, sdf1.format(after));
			pstmt.setString(paramIdx++, sdf1.format(before));
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				statMsgCountNoAnswer = rs.getInt("statMsgCountNoAnswer");
				statMsgCountHasAnswer = rs.getInt("statMsgCountHasAnswer");
				statMsgCountFromUser = rs.getInt("statMsgCountFromUser");
				rate = rs.getFloat("rate");
			}
			return new float[] { statMsgCountNoAnswer, statMsgCountHasAnswer, statMsgCountFromUser, rate };

		} catch (SQLException e) {
			if (con != null)
				con.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}

	}
	
	public static Object[][] statisticsByHour(Integer tenantId, Date dateStart, Date dateEnd) {
		String mssql = "select DATEPART(hh, sle.tsCreated) as tempHour, count(*) as tempSsions from ServiceLogEntity sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by DATEPART(hh, sle.tsCreated) "
		        + " order by DATEPART(hh, sle.tsCreated)";
		
		String mysql = "select hour(sle.tsCreated) as hour, count(sle) from " + ServiceLogEntity.class.getName() + " sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by hour(sle.tsCreated) "
		        + " order by hour(sle.tsCreated)";
		
        Query q = null;
        switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql); 
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql); 
				break;
        }
        if (tenantId != null) q.setParameter("tenantId", tenantId);
        if (dateStart != null) q.setParameter("dateStart", dateStart);
        if (dateEnd != null) q.setParameter("dateEnd", dateEnd);
        System.out.println("Q: " + q + ", tenantId: " + tenantId + ", dateStart: " + dateStart + ", dateEnd: " + dateEnd);
	    List<Object[]> objects = q.list();
	    
	    return objects.toArray(new Object[0][]);
	}
	
	public static Object[][] statisticsByDay(Integer tenantId, Date dateStart, Date dateEnd) {
		String mssql = "select CONVERT(varchar, sle.tsCreated, 105), count(*) from ServiceLogEntity sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by CONVERT(varchar, sle.tsCreated, 105)"
		        + " order by CONVERT(varchar, sle.tsCreated, 105)";
		
		String mysql = "select DATE_FORMAT(sle.tsCreated, '%Y-%m-%d') as day, count(sle) from " + ServiceLogEntity.class.getName() + " sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by DATE_FORMAT(sle.tsCreated, '%Y-%m-%d')"
		        + " order by DATE_FORMAT(sle.tsCreated, '%Y-%m-%d')";
		
        Query q = null;
        switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql); 
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql); 
				break;
        }
        if (tenantId != null) q.setParameter("tenantId", tenantId);
        if (dateStart != null) q.setParameter("dateStart", dateStart);
        if (dateEnd != null) q.setParameter("dateEnd", dateEnd);

	    List<Object[]> objects = q.list();
	    
	    return objects.toArray(new Object[0][]);
	}
	
	public static Double averageSessionTime(Integer tenantId, Date dateStart, Date dateEnd) {
		String mssql = "select avg(datediff(s,sle.tsCreated,sle.timestamp)) as average from ServiceLogEntity sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
				;
		
		String mysql = "select avg(sle.timestamp - sle.tsCreated) as average from " + ServiceLogEntity.class.getName() + " sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
				;

		Query q = null;
		switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql);
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql);
				break;
		}
		if (tenantId != null) q.setParameter("tenantId", tenantId);
	    if (dateStart != null) q.setParameter("dateStart", dateStart);
	    if (dateEnd != null) q.setParameter("dateEnd", dateEnd);
	    
	    Double average = null;
	    
	    switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				average = (Double)q.uniqueResult();
				break;
			case MSSQL: 
				Integer result = (Integer)q.uniqueResult();
				if (result != null) {
			    	average = result * 1.0;
			    }
				break;
    	}

//	    for (Object[] o : objects) {
//	        Object hour = o[0];
//	        Object count = o[1];
//	        System.out.println("Hour[" + hour + "]:" + count);
//	    }

	    if (average != null)
	    		return average;
	    return null;
	}
	
	public static Object[][] statisticsByChannel(Integer tenantId, Date dateStart, Date dateEnd) {
		String mssql = "select convert(varchar(255),sle.channel), CAST(count(*) AS varchar(255)) from ServiceLogEntity sle " +
		        "where 1=1 "
		        + (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by convert(varchar(255),sle.channel) "
		        + " order by convert(varchar(255),sle.channel) ";
		
		String mysql = "select channel, count(sle) from " + ServiceLogEntity.class.getName() + " sle " +
		        "where 1=1 "
				+ (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by channel "
		        + " order by channel ";

		Query q = null;
		switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql);
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql);
				break;
		}
		if (tenantId != null) q.setParameter("tenantId", tenantId);
        if (dateStart != null) q.setParameter("dateStart", dateStart);
        if (dateEnd != null) q.setParameter("dateEnd", dateEnd);
        
        List<Object[]> objects = q.list();

	    return objects.toArray(new Object[0][]);
	}
	
	public static Object[][] statisticsByLastQaCategory(Integer tenantId, Date dateStart, Date dateEnd) {
		String mssql = "select lastQaCategory, count(*) from ServiceLogEntity sle " +
		        "where 1=1 "
		        + (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by lastQaCategory "
		        + " order by lastQaCategory ";
				
		String mysql = "select lastQaCategory, count(sle) from " + ServiceLogEntity.class.getName() + " sle " +
		        "where 1=1 "
		        + (tenantId != null ? " AND tenantId = :tenantId" : "")
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by lastQaCategory "
		        + " order by lastQaCategory ";

		Query q = null;
		switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql);
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql);
				break;
		}
		if (tenantId != null) q.setParameter("tenantId", tenantId);
        if (dateStart != null) q.setParameter("dateStart", dateStart);
        if (dateEnd != null) q.setParameter("dateEnd", dateEnd);
        
        List<Object[]> objects = q.list();

	    return objects.toArray(new Object[0][]);
	}
	
	public static Object[][] statisticsByTenantId(Date dateStart, Date dateEnd) {
		String mssql = "select tenantId, count(*) from ServiceLogEntity sle " +
		        "where 1=1 "
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by tenantId "
		        + " order by count(*) DESC ";
		
		String mysql = "select tenantId, count(sle) from " + ServiceLogEntity.class.getName() + " sle " +
		        " WHERE 1=1 "
				+ (dateStart != null ? " AND sle.tsCreated >= :dateStart" : "")
				+ (dateEnd != null ? " AND sle.tsCreated < :dateEnd" : "")
		        + " group by tenantId "
		        + " order by count(sle) desc ";

		Query q = null;
		switch (HibernateUtil.SQL_TYPE) {
			case MYSQL: 
				q = HibernateUtil.getSession().createQuery(mysql);
				break;
			case MSSQL: 
				q = HibernateUtil.getSession().createSQLQuery(mssql);
				break;
		}
        if (dateStart != null) q.setParameter("dateStart", dateStart);
        if (dateEnd != null) q.setParameter("dateEnd", dateEnd);
        
        List<Object[]> objects = q.list();

	    return objects.toArray(new Object[0][]);
	}
	
	public static ServiceLogEntity log(Tenant t, HttpServletRequest req, String namespace, String identity, String status, String param, String statusMessage) {
		ServiceLogEntity log = new ServiceLogEntity();
		log.setTenantId(t.getId());
		log.setNamespace(namespace);
		log.setIdentity(identity);
		log.setTsCreated(Calendar.getInstance().getTime());
		log.setStatus(status);
		log.setParameters(param);
		log.setConversations(new JSONObject().toString());
		log.setStatusMessage(statusMessage);
		setIdentitesAndSave(req, log);
		
		return log;
	}

	public static ServiceLogEntity log(QAContext qaCtx, HttpServletRequest req, String namespace, String identity, String status, String param, String statusMessage, String qaId, String sessionId, String userId) {
		Tenant t = qaCtx.getTenant();
		UserClue uc = qaCtx.getUserClue();
		
		ServiceLogEntity log = new ServiceLogEntity();
		log.setTenantId(t.getId());
		log.setNamespace(namespace);
		log.setIdentity(uc != null ? uc.getId().toString() : identity);
		log.setTsCreated(Calendar.getInstance().getTime());
		log.setStatus(status);
		log.setParameters(param);
		log.setQaId(qaId);
		log.setSessionId(sessionId);
		log.setUserId(userId);
		log.setConversations(new JSONObject().toString());
		log.setStatusMessage(statusMessage);
		setIdentitesAndSave(req, log);
		
		return log;
	}

	public static ServiceLogEntity getFromSession(Tenant t, String qaId, String sessionId) {
        ServiceLogEntity log = null;
        Session ses = null;
        Transaction tx = null;
        try {
            ses = HibernateUtil.getSession();
            tx = ses.beginTransaction();
            Criteria ct = ses.createCriteria(ServiceLogEntity.class)
                    .addOrder(Order.desc("timestamp"));
            if(StringUtils.isNotBlank(sessionId))
                ct.add(Restrictions.eq("sessionId", sessionId));
            ct.add(Restrictions.eq("qaId", qaId));
            ct.add(Restrictions.eq("tenantId", t.getId()));
            ct.add(Restrictions.gt("timestamp", new Date(
                System.currentTimeMillis() - (t.getSessionExpirationSecond() * 1000))));

            ct.setMaxResults(1);
            ct.setFetchSize(1);
            List<ServiceLogEntity> l = ct.list();
            log = (l != null && l.size() > 0) ? l.get(0) : null;
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
        } finally {
            ses.close();
        }

        return log;
    }
	
	
	public static ServiceLogEntity getFromSession(Tenant t, String qaId) {
		ServiceLogEntity log = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class)
					.addOrder(Order.desc("timestamp"));
			ct.add(Restrictions.eq("qaId", qaId));
			ct.add(Restrictions.eq("tenantId", t.getId()));
			ct.add(Restrictions.gt("timestamp", new Date(
				System.currentTimeMillis() - (t.getSessionExpirationSecond() * 1000))));

			ct.setMaxResults(1);
			ct.setFetchSize(1);
			List<ServiceLogEntity> l = ct.list();
			log = (l != null && l.size() > 0) ? l.get(0) : null;
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return log;
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
	public static synchronized List<ServiceLogEntity> getFromQaId(Integer tenantId, String qaId, Integer timeWindowInSecond, Integer limit) {
		List<ServiceLogEntity> result = new ArrayList<ServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class)
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

	public static void setIntoSession(String qaId, ServiceLogEntity log) {
		log.qaId = qaId;
		save(log);
	}


	public static synchronized List<ServiceLogEntity> listByIdentity(Integer tenantId, String identity, Integer reviewStatus, String lastQaCategory, Date after, Date before, int start, int rows) {
		List<ServiceLogEntity> result = new ArrayList<ServiceLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(ServiceLogEntity.class);

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
			Criteria ct = ses.createCriteria(ServiceLogEntity.class);

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
