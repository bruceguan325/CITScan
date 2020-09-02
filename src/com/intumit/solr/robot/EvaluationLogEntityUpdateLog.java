package com.intumit.solr.robot;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;

/**
 * 修改 EvaluationLogEntity 時，就要存一個 UpdateLog
 * 但目前 update EvaluationLogEntity.LastTestResult 時不會增加記錄 
 * @author herb
 */
@Entity
public class EvaluationLogEntityUpdateLog {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="evaluationLogEntityIdIdx")
	private int evaluationLogEntityId;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Column(length = 32)
	@Index(name = "namespaceIdx")
	private String namespace;

	@Index(name = "flagIdx")
	private int flag;

	@Column(length = 64)
	@Index(name = "clientIpIdx")
	private String clientIp;

	@Column(length = 512)
	@Index(name = "admIddx")
	private Integer admId;

	@Column(columnDefinition="varchar(4000)")
	private String question;

	@Index(name = "robotAnswerIdIdx")
	private Long robotAnswerId;

	@Index(name = "correctAnswerIdIdx")
	private Long correctAnswerId;

	@Index(name = "autoRobotAnswerIdIdx")
	private Long autoRobotAnswerId;

	private Double score;
	private String eservice;
	private String userType;

	@Lob
	private String conversations;

	@Index(name = "timestampIdx")
	private Date timestamp;

	@Index(name = "reviewStatusIdIdx")
	private Integer reviewStatus;

	@Index(name = "lastTestResultIdx")
	private Integer lastTestResult;

	@Index(name = "lastTestResultTimestampIdx")
	private Date lastTestResultTimestamp;

	@Column(columnDefinition="varchar(512)")
	private String qaCategory;

	private String description;
	
	/**
	 * logSource 標示來源，儘量只使用大寫英文跟底線，便於後續查詢，例如 "LINE"、"TEXT_CRM"
	 */
	@Column(columnDefinition="varchar(64)")
	@Index(name = "logSourceIdx")
	private String logSource;
	
	/**
	 * 來源本身的 reference，這個是方便要反向查出來源端對應的資料時，通常可以存來源那邊的 primary key 或者 unique key 之類的。
	 */
	@Column(columnDefinition="varchar(256)")
	private String sourceRef;
	/**
	 * 來源端建議可套用的範本
	 */
	@Column(columnDefinition="varchar(16)")
	@Index(name = "suggestQaTemplateMkeyIdx")
	private String suggestQaTemplateMkey;
	
	/**
	 * 來源端建議套用的範本時的關鍵字（or 知識主題）
	 */
	@Column(columnDefinition="varchar(512)")
	@Index(name = "suggestQaTemplateKeywordsIdx")
	private String suggestQaTemplateKeywords;

	private Integer qtime;
	
	private String originalTime;

	@Column(length = 512)
	@Index(name = "updateByAdmIddx")
	private Integer updateByAdmId;

	@Index(name = "logTimestamp")
	private Date logTimestamp;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public int getEvaluationLogEntityId() {
		return evaluationLogEntityId;
	}

	public void setEvaluationLogEntityId(int evaluationLogEntityId) {
		this.evaluationLogEntityId = evaluationLogEntityId;
	}

	public Integer getUpdateByAdmId() {
		return updateByAdmId;
	}

	public void setUpdateByAdmId(Integer updateByAdmId) {
		this.updateByAdmId = updateByAdmId;
	}

	public Date getLogTimestamp() {
		return logTimestamp;
	}

	public void setLogTimestamp(Date updateByTimestamp) {
		this.logTimestamp = updateByTimestamp;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public Integer getAdmId() {
		return admId;
	}

	public void setAdmId(Integer admId) {
		this.admId = admId;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public Long getRobotAnswerId() {
		return robotAnswerId;
	}

	public void setRobotAnswerId(Long robotAnswerId) {
		this.robotAnswerId = robotAnswerId;
	}

	public Long getAutoRobotAnswerId() {
		return autoRobotAnswerId;
	}

	public void setAutoRobotAnswerId(Long autoRobotAnswerId) {
		this.autoRobotAnswerId = autoRobotAnswerId;
	}

	public Long getCorrectAnswerId() {
		return correctAnswerId;
	}

	public void setCorrectAnswerId(Long corrrectAnswerId) {
		this.correctAnswerId = corrrectAnswerId;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getEservice() {
		return eservice;
	}
	
	public void setEservice(String eservice) {
		this.eservice = eservice;
	}
	
	public String getUserType() {
		return userType;
	}
	
	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getConversations() {
		return conversations;
	}

	public void setConversations(String conversations) {
		this.conversations = conversations;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getReviewStatus() {
		return reviewStatus == null ? 0 : reviewStatus;
	}

	public void setReviewStatus(Integer reviewStatus) {
		this.reviewStatus = reviewStatus;
	}

	public Integer getLastTestResult() {
		return lastTestResult;
	}

	public void setLastTestResult(Integer lastTestResult) {
		this.lastTestResult = lastTestResult;
	}

	public Date getLastTestResultTimestamp() {
		return lastTestResultTimestamp;
	}

	public void setLastTestResultTimestamp(Date lastTestResultTimestamp) {
		this.lastTestResultTimestamp = lastTestResultTimestamp;
	}

	public String getQaCategory() {
		return qaCategory == null ? QAUtil.GENERAL_CATEGORIES.iterator().next() : qaCategory;
	}

	public void setQaCategory(String qaCategory) {
		this.qaCategory = qaCategory;
	}

	public Integer getQtime() {
		return qtime;
	}

	public void setQtime(Integer qtime) {
		this.qtime = qtime;
	}
	
	public String getOriginalTime() {
		return originalTime;
	}
	
	public void setOriginalTime(String originalTime) {
		this.originalTime = originalTime;
	}

	public String getLogSource() {
		return logSource;
	}

	public void setLogSource(String logSource) {
		this.logSource = logSource;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public void setSourceRef(String sourceRef) {
		this.sourceRef = sourceRef;
	}

	public String getSuggestQaTemplateMkey() {
		return suggestQaTemplateMkey;
	}

	public void setSuggestQaTemplateMkey(String suggestQaTemplateMkey) {
		this.suggestQaTemplateMkey = suggestQaTemplateMkey;
	}

	public String getSuggestQaTemplateKeywords() {
		return suggestQaTemplateKeywords;
	}

	public void setSuggestQaTemplateKeywords(String suggestQaTemplateKeywords) {
		this.suggestQaTemplateKeywords = suggestQaTemplateKeywords;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static synchronized EvaluationLogEntityUpdateLog get(int id) {
		try {
			return (EvaluationLogEntityUpdateLog)HibernateUtil.getSession().get(EvaluationLogEntityUpdateLog.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized Number countByEvaluationLogEntityId(Integer tenantId, Integer evaluationLogEntityId) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(EvaluationLogEntityUpdateLog.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (evaluationLogEntityId != null) {
				ct.add(Restrictions.eq("evaluationLogEntityId", evaluationLogEntityId));
			}
			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}

	public static synchronized void save(EvaluationLogEntityUpdateLog log) {
		Session ses = null;
		Transaction tx = null;
		try {
			Date now = Calendar.getInstance().getTime();
			log.setTimestamp(now);
			log.setLastTestResultTimestamp(now);

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
	
	public static synchronized boolean checkAndCreateFirstLog(EvaluationLogEntity log) {
		Session ses = null;
		Transaction tx = null;
		try {
    		ses = HibernateUtil.getSession();
    		Number count = countByEvaluationLogEntityId(log.getTenantId(), log.getId());
    		
    		if (count == null || count.intValue() < 1) {
        		tx = ses.beginTransaction();
    			// 沒記錄先放舊記錄
    			EvaluationLogEntityUpdateLog firstLog = new EvaluationLogEntityUpdateLog();
    			PropertyUtils.copyProperties(firstLog, log);
    			firstLog.setId(null);
    			firstLog.setEvaluationLogEntityId(log.getId());
    			firstLog.setLogTimestamp(log.getTimestamp());
    			firstLog.setUpdateByAdmId(log.getAdmId());
    			ses.saveOrUpdate(firstLog);
        		tx.commit();
        		
        		return true;
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    		tx.rollback();
    	} finally {
    		ses.close();
    	}
		
		return false;
	}
	
	public static synchronized void addUpdateLog(EvaluationLogEntity log, AdminUser byUser) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			EvaluationLogEntityUpdateLog ulog = new EvaluationLogEntityUpdateLog();
			PropertyUtils.copyProperties(ulog, log);
			ulog.setId(null);
			ulog.setEvaluationLogEntityId(log.getId());
			ulog.setLogTimestamp(now);
			ulog.setUpdateByAdmId(byUser.getId());
			ses.saveOrUpdate(ulog);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

}
