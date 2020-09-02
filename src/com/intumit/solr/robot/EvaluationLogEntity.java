package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;
import org.json.JSONException;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.tenant.Tenant;

/**
 * 記得增加欄位時要修改 listBy 的 Projection
 * @author herb
 */
@Entity
public class EvaluationLogEntity {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

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
	@Transient
	private String admName;
	@Column(columnDefinition="varchar(4000)")
	private String question;

	/**
	 * Original logged Kid
	 */
	@Index(name = "robotAnswerIdIdx")
	private Long robotAnswerId;

	/**
	 * Expect Kid
	 */
	@Index(name = "correctAnswerIdIdx")
	private Long correctAnswerId;

	/**
	 * Actual Kid
	 */
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

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

	/**
	 * Normally you should use setLastRobotAnswerId(), not this one.
	 * Set this only when you are absolutely sure what you're doing.
	 * 
	 * @param robotAnswerId
	 */
	public void setRobotAnswerId(Long robotAnswerId) {
		this.robotAnswerId = robotAnswerId;
	}

	public Long getAutoRobotAnswerId() {
		return autoRobotAnswerId;
	}

	public void setAutoRobotAnswerId(Long autoRobotAnswerId) {
		this.autoRobotAnswerId = autoRobotAnswerId;
	}

	/**
	 * 改成避免誤解的名稱
	 * @return
	 */
	public Long getLastRobotAnswerId() {
		return autoRobotAnswerId;
	}

	/**
	 * 改成避免誤解的名稱
	 * @return
	 */
	public void setLastRobotAnswerId(Long lastRobotAnswerId) {
		this.autoRobotAnswerId = lastRobotAnswerId;
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
	
	public String getAdmName() {
		if (this.admId != null && AdminUserFacade.getInstance().get(this.admId) != null)
			return AdminUserFacade.getInstance().get(this.admId).getLoginName();
		else
			return null;
	}
	public static synchronized EvaluationLogEntity get(int id) {
		try {
			return (EvaluationLogEntity)HibernateUtil.getSession().get(EvaluationLogEntity.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized List<EvaluationLogEntity> listByKid(Integer tenantId, String namespace, long kid, int start, int rows) {
		return listBy(tenantId, namespace, null, kid, null, null, null, null, null, false, null, null, null, null, start, rows);
	}
	public static synchronized List<EvaluationLogEntity> listByFlag(Integer tenantId, String namespace, int flag, int start, int rows) {
		return listBy(tenantId, namespace, null, null, flag, null, null, null, null, false, null, null, null, null, start, rows);
	}
	public static synchronized List<EvaluationLogEntity> listByNS(Integer tenantId, String namespace, int max) {
		return listByNS(tenantId, namespace, 0, max);
	}
	public static synchronized List<EvaluationLogEntity> listByNS(Integer tenantId, String namespace, int start, int rows) {
		return listBy(tenantId, namespace, null, null, null, null, null, null, null, false, null, null, null, null, start, rows);
	}
	public static synchronized List<EvaluationLogEntity> listByQaCategory(Integer tenantId, String namespace, String qaCategory, int start, int rows) {
		return listBy(tenantId, namespace, null, null, null, null, null, null, null, false, qaCategory, null, null, null, start, rows);
	}
	public static synchronized Number countByKid(Integer tenantId, String namespace, long kid) {
		return countBy(tenantId, namespace, null, kid, null, null, null, null, null, false, null, null, null, null);
	}
	public static synchronized Number countByFlag(Integer tenantId, String namespace, int flag) {
		return countBy(tenantId, namespace, null, null, flag, null, null, null, null, false, null, null, null, null);
	}
	public static synchronized Number countByNS(Integer tenantId, String namespace) {
		return countBy(tenantId, namespace, null, null, null, null, null, null, null, false, null, null, null, null);
	}
	public static synchronized Number countByQaCategory(Integer tenantId, String namespace, String qaCategory) {
		return countBy(tenantId, namespace, null, null, null, null, null, null, null, false, qaCategory, null, null, null);
	}

	public static synchronized List<EvaluationLogEntity> listBy(Integer tenantId, String namespace, Integer admId, Long kid, Integer flag, Integer lastTestResult, String logSource, String suggestQaTemplateMkey, String suggestQaTemplateKeywords, boolean distinctQuestion, String qaCategory, String bypassAdmIds, Date after, Date before, int start, int rows) {
		List<EvaluationLogEntity> result = new ArrayList<EvaluationLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			StringBuffer hql = new StringBuffer("from EvaluationLogEntity eval ");
			hql.append(" where (eval.reviewStatus is null or eval.reviewStatus <> -1) ");
			if (tenantId != null) {
				hql.append(" and eval.tenantId = :tenantId ");
			}
			if (namespace != null) {
				hql.append(" and eval.namespace = :namespace ");
			}
			if (distinctQuestion) {
				hql.append(" and eval.flag != 0 ");
			}
			if (admId != null) {
				hql.append(" and eval.admId = :admId ");
			}
			if (bypassAdmIds != null) {
				hql.append(" and eval.admId not in ( :bypassAdmIds ) ");
			}
			if (lastTestResult != null) {
				hql.append(" and eval.lastTestResult = :lastTestResult");
			}
			if (flag != null) {
				hql.append(" and eval.flag = :flag ");
			}
			if (kid != null) {
				hql.append(" and (eval.correctAnswerId = :kid or eval.robotAnswerId = :kid) ");
			}
			if (after != null && before != null) {
				hql.append(" and eval.timestamp between :startDate and :endDate ");
			}
			else if (after != null) {
				hql.append(" and eval.timestamp >= :startDate ");
			}
			else if (before != null) {
				hql.append(" and eval.timestamp <= :endDate ");
			}

			if (StringUtils.isNotBlank(qaCategory)) {
				if (QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {
					hql.append(" and (eval.qaCategory is null OR eval.qaCategory = :qaCategory) ");
				}
				else {
					hql.append(" and eval.qaCategory = :qaCategory ");
				}
			}

			if (logSource != null) {
				hql.append(" and eval.logSource = :logSource ");
			}
			if (suggestQaTemplateMkey != null) {
				hql.append(" and eval.suggestQaTemplateMkey = :suggestQaTemplateMkey");
			}
			if (suggestQaTemplateKeywords != null) {
				hql.append(" and eval.suggestQaTemplateKeywords = :suggestQaTemplateKeywords");
			}
			
			if (distinctQuestion) {
				hql.insert(0, "select * from EvaluationLogEntity log, ( select eval.question, max(eval.id) as id ");
				hql.append(" group by eval.question ) filtered where log.id = filtered.id ");
				hql.append(" order by log.timestamp desc, log.id desc");
			}
			else {
				hql.insert(0, "select * ");
				hql.append(" order by eval.timestamp desc, eval.id desc");
			}
			Query query = ses.createSQLQuery(hql.toString());
			if (namespace != null) {
				query.setString("namespace", namespace);
			}
			if (tenantId != null) {
				query.setInteger("tenantId", tenantId);
			}
			if (admId != null) {
				query.setInteger("admId", admId);
			}
			if (bypassAdmIds != null) {				
				List<Integer> ids = new ArrayList<Integer>();
				for (String idStr : StringUtils.split(bypassAdmIds, ",")) {
					ids.add(new Integer(idStr));
				}				
				query.setParameterList("bypassAdmIds", ids);
			}
			if (logSource != null) {
				query.setString("logSource", logSource);
			}
			if (suggestQaTemplateMkey != null) {
				query.setString("suggestQaTemplateMkey", suggestQaTemplateMkey);
			}
			if (suggestQaTemplateKeywords != null) {
				query.setString("suggestQaTemplateKeywords", suggestQaTemplateKeywords);
			}
			if (lastTestResult != null) {
				query.setInteger("lastTestResult", lastTestResult);
			}
			if (flag != null) {
				query.setInteger("flag", flag);
			}
			if (kid != null) {
				query.setLong("kid", kid);
			}
			if (after != null) {
				query.setTimestamp("startDate", after);
			}
			if (before != null) {
				query.setTimestamp("endDate", before);
			}

			if (StringUtils.isNotBlank(qaCategory)) {
				query.setString("qaCategory", qaCategory);
			}
			query.setResultTransformer(new EvaluationLogEntityResultTransformer(EvaluationLogEntity.class));
			result = query.setFirstResult(start).setMaxResults(rows).list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	/**
	 * 列出 log， group by qaAltTemplateMkey + qaAltTemplateMkeyKeywords 兩個欄位
	 * 而且前提是 qaAltTemplateMkey IS NOT NULL
	 * 
	 * 主要是給 qaEvaluationLogGroupView.jsp 用，用途分析真人文字客服的紀錄，標注範本跟關鍵字後需要有個地方可以 review
	 * 
	 * @param tenantId
	 * @param namespace
	 * @param admId
	 * @param kid
	 * @param flag
	 * @param lastTestResult
	 * @param logSource
	 * @param qaCategory
	 * @param bypassAdmIds
	 * @param after
	 * @param before
	 * @param start
	 * @param rows
	 * @return
	 */
	public static synchronized List<EvaluationLogEntity> listAndGroupByQaAltTemplateMkeyAndKeywords(Integer tenantId, String namespace, Integer admId, Long kid, Integer flag, Integer lastTestResult, String logSource, String qaCategory, String bypassAdmIds, Date after, Date before, int start, int rows) {
		List<EvaluationLogEntity> result = new ArrayList<EvaluationLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			StringBuffer hql = new StringBuffer("from EvaluationLogEntity eval ");
			hql.append(" where (eval.reviewStatus is null or eval.reviewStatus <> -1) ");
			hql.append(" and eval.flag != 0 ");
			hql.append(" and eval.suggestQaTemplateMkey IS NOT NULL ");

			if (tenantId != null) {
				hql.append(" and eval.tenantId = :tenantId ");
			}
			if (namespace != null) {
				hql.append(" and eval.namespace = :namespace ");
			}
			if (admId != null) {
				hql.append(" and eval.admId = :admId ");
			}
			if (bypassAdmIds != null) {
				hql.append(" and eval.admId not in ( :bypassAdmIds ) ");
			}
			if (lastTestResult != null) {
				hql.append(" and eval.lastTestResult = :lastTestResult");
			}
			if (flag != null) {
				hql.append(" and eval.flag = :flag ");
			}
			if (kid != null) {
				hql.append(" and (eval.correctAnswerId = :kid or eval.robotAnswerId = :kid) ");
			}
			if (after != null && before != null) {
				hql.append(" and eval.timestamp between :startDate and :endDate ");
			}
			else if (after != null) {
				hql.append(" and eval.timestamp >= :startDate ");
			}
			else if (before != null) {
				hql.append(" and eval.timestamp <= :endDate ");
			}
			
			if (logSource != null) {
				hql.append(" and eval.logSource = :logSource ");
			}

			if (StringUtils.isNotBlank(qaCategory)) {
				if (QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {
					hql.append(" and (eval.qaCategory is null OR eval.qaCategory = :qaCategory) ");
				}
				else {
					hql.append(" and eval.qaCategory = :qaCategory ");
				}
			}

			// Group by suggestQaTemplateMkey + suggestQaTemplateKeywords
			hql.insert(0, "select * from EvaluationLogEntity log, ( select eval.question, max(eval.id) as id ");
			hql.append(" group by eval.suggestQaTemplateMkey, eval.suggestQaTemplateKeywords ) filtered where log.id = filtered.id ");
			hql.append(" order by log.timestamp desc, log.id desc");
				
			Query query = ses.createSQLQuery(hql.toString());
			if (tenantId != null) {
				query.setInteger("tenantId", tenantId);
			}
			if (namespace != null) {
				query.setString("namespace", namespace);
			}
			if (admId != null) {
				query.setInteger("admId", admId);
			}
			if (bypassAdmIds != null) {
				query.setString("bypassAdmIds", bypassAdmIds);
			}
			if (lastTestResult != null) {
				query.setInteger("lastTestResult", lastTestResult);
			}
			if (flag != null) {
				query.setInteger("flag", flag);
			}
			if (kid != null) {
				query.setLong("kid", kid);
			}
			if (after != null) {
				query.setTimestamp("startDate", after);
			}
			if (before != null) {
				query.setTimestamp("endDate", before);
			}

			if (logSource != null) {
				query.setString("logSource", logSource);
			}
			if (StringUtils.isNotBlank(qaCategory)) {
				query.setString("qaCategory", qaCategory);
			}
			query.setResultTransformer(new EvaluationLogEntityResultTransformer(EvaluationLogEntity.class));
			result = query.setFirstResult(start).setMaxResults(rows).list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	public static synchronized List<EvaluationLogEntity> listBy(Integer tenantId, String namespace,
            Integer admId, Long kid, Integer flag, Integer lastTestResult, String logSource,
            String suggestQaTemplateMkey, String suggestQaTemplateKeywords,
            boolean distinctQuestion, String qaCategory, String bypassAdmIds, Date after,
            Date before) {
        List<EvaluationLogEntity> result = new ArrayList<EvaluationLogEntity>();
        Session ses = null;
        try {
            ses = HibernateUtil.getSession();
            StringBuffer hql = new StringBuffer("from EvaluationLogEntity eval ");
            hql.append(" where (eval.reviewStatus is null or eval.reviewStatus <> -1) ");
            if (tenantId != null) {
                hql.append(" and eval.tenantId = :tenantId ");
            }
            if (namespace != null) {
                hql.append(" and eval.namespace = :namespace ");
            }
            if (distinctQuestion) {
                hql.append(" and eval.flag != 0 ");
            }
            if (admId != null) {
                hql.append(" and eval.admId = :admId ");
            }
            if (bypassAdmIds != null) {
                hql.append(" and eval.admId not in ( :bypassAdmIds ) ");
            }
            if (lastTestResult != null) {
                hql.append(" and eval.lastTestResult = :lastTestResult");
            }
            if (flag != null) {
                hql.append(" and eval.flag = :flag ");
            }
            if (kid != null) {
                hql.append(" and (eval.correctAnswerId = :kid or eval.robotAnswerId = :kid) ");
            }
            if (after != null && before != null) {
                hql.append(" and eval.timestamp between :startDate and :endDate ");
            }
            else if (after != null) {
                hql.append(" and eval.timestamp >= :startDate ");
            }
            else if (before != null) {
                hql.append(" and eval.timestamp <= :endDate ");
            }

            if (StringUtils.isNotBlank(qaCategory)) {
                if (QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {
                    hql.append(" and (eval.qaCategory is null OR eval.qaCategory = :qaCategory) ");
                }
                else {
                    hql.append(" and eval.qaCategory = :qaCategory ");
                }
            }

            if (logSource != null) {
                hql.append(" and eval.logSource = :logSource ");
            }
            if (suggestQaTemplateMkey != null) {
                hql.append(" and eval.suggestQaTemplateMkey = :suggestQaTemplateMkey");
            }
            if (suggestQaTemplateKeywords != null) {
                hql.append(" and eval.suggestQaTemplateKeywords = :suggestQaTemplateKeywords");
            }

            if (distinctQuestion) {
                hql.insert(0,
                        "select * from EvaluationLogEntity log, ( select eval.question, max(eval.id) as id ");
                hql.append(" group by eval.question ) filtered where log.id = filtered.id ");
                hql.append(" order by log.timestamp desc, log.id desc");
            }
            else {
                hql.insert(0, "select * ");
                hql.append(" order by eval.timestamp desc, eval.id desc");
            }
            Query query = ses.createSQLQuery(hql.toString());
            if (namespace != null) {
                query.setString("namespace", namespace);
            }
            if (tenantId != null) {
                query.setInteger("tenantId", tenantId);
            }
            if (admId != null) {
                query.setInteger("admId", admId);
            }
            if (bypassAdmIds != null) {
				List<Integer> ids = new ArrayList<>();
				for (String idStr : StringUtils.split(bypassAdmIds, ",")) {
					ids.add(new Integer(idStr));
				}
                query.setParameterList("bypassAdmIds", ids);
            }
            if (logSource != null) {
                query.setString("logSource", logSource);
            }
            if (suggestQaTemplateMkey != null) {
                query.setString("suggestQaTemplateMkey", suggestQaTemplateMkey);
            }
            if (suggestQaTemplateKeywords != null) {
                query.setString("suggestQaTemplateKeywords", suggestQaTemplateKeywords);
            }
            if (lastTestResult != null) {
                query.setInteger("lastTestResult", lastTestResult);
            }
            if (flag != null) {
                query.setInteger("flag", flag);
            }
            if (kid != null) {
                query.setLong("kid", kid);
            }
            if (after != null) {
                query.setTimestamp("startDate", after);
            }
            if (before != null) {
                query.setTimestamp("endDate", before);
            }

            if (StringUtils.isNotBlank(qaCategory)) {
                query.setString("qaCategory", qaCategory);
            }
            query.setResultTransformer(
                    new EvaluationLogEntityResultTransformer(EvaluationLogEntity.class));
            result = query.list();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ses.close();
        }
        return result;
    }
	
	public static synchronized Number countBy(Integer tenantId, String namespace, Integer admId, Long kid, Integer flag, Integer lastTestResult, String logSource, String suggestQaTemplateMkey, String suggestQaTemplateKeywords, boolean distinctQuestion, String qaCategory, String bypassAdmIds, Date after, Date before) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(EvaluationLogEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
			}
			ct.add(Restrictions.or(
					Restrictions.isNull("reviewStatus"), Restrictions.ne("reviewStatus", -1)
					));

			if (admId != null) {
				ct.add(Restrictions.eq("admId", admId));
			}
			if (bypassAdmIds != null) {
				List<Integer> ids = new ArrayList<Integer>();
				for (String idStr : StringUtils.split(bypassAdmIds, ",")) {
					ids.add(new Integer(idStr));
				}
				ct.add(Restrictions.not(Restrictions.in("admId", ids)));
			}
			if (flag != null) {
				ct.add(Restrictions.eq("flag", flag));
			}
			if (lastTestResult != null) {
				ct.add(Restrictions.eq("lastTestResult", lastTestResult));
			}

			if (logSource != null) {
				ct.add(Restrictions.eq("logSource", logSource));
			}
			if (suggestQaTemplateMkey != null) {
				ct.add(Restrictions.eq("suggestQaTemplateMkey", suggestQaTemplateMkey));
			}
			if (suggestQaTemplateKeywords != null) {
				ct.add(Restrictions.eq("suggestQaTemplateKeywords", suggestQaTemplateKeywords));
			}
			if (kid != null) {
				ct.add(
						Restrictions.or(
								Restrictions.eq("correctAnswerId", kid),
								Restrictions.eq("robotAnswerId", kid)
								));
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

			if (StringUtils.isNotBlank(qaCategory)) {
				if (QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {

					ct.add(Restrictions.or(
							Restrictions.isNull("qaCategory"),
							Restrictions.eq("qaCategory", qaCategory)
							));
				}
				else {
					ct.add(Restrictions.eq("qaCategory", qaCategory));
				}
			}

			if (distinctQuestion) {
				return (Number)ct.setProjection(Projections.countDistinct("question")).uniqueResult();
			}

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}

	public static synchronized void save(EvaluationLogEntity log) {
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

	public static synchronized void update(EvaluationLogEntity log) {
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
	
	public static synchronized void updateCategoryByNumber(EvaluationLogEntity log, QAContext qaCtx) {
		Session ses = null;
		Transaction tx = null;
		try {
			SolrDocument oriDoc = QAUtil.getInstance(qaCtx.getTenant()).getMainQASolrDocument(log.getCorrectAnswerId());
			if(oriDoc != null && oriDoc.getFieldValue("QA_CATEGORY_s") != null){
				String qaCategory = (String)oriDoc.getFieldValue("QA_CATEGORY_s");
				log.setQaCategory(qaCategory);
			}
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

	public static void setIdentitesAndSave(HttpServletRequest req, QAContext qaCtx, EvaluationLogEntity log) {
		String ip = SolrDispatchFilter.getClientIpAddr(req);
		log.setClientIp(ip);

		if (qaCtx != null) {
			try {
					QAChannel ch = qaCtx.getQAChannelInstance();
					
					if (ch.getType() == QAChannelType.LINE) {
						log.setAdmId(qaCtx.getLineUserProfile().getInt("adminUserId"));
					}
			}
			catch (Exception ignore) {}
		}
		
		if (qaCtx == null || log.getAdmId() == null) {
			AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
			log.setAdmId(user.getId()); // user 不能是 null，不然不合理，一個紀錄沒有紀錄者
		}

		save(log);
	}

	public static List<EvaluationLogEntity> listByDate(Integer tenantId, Date after, Date before) {
		List<EvaluationLogEntity> result = new ArrayList<EvaluationLogEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(EvaluationLogEntity.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
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

	public static EvaluationLogEntity log(HttpServletRequest req, String namespace, String respJson, QAContext qaCtx) throws JSONException {
		JSONObject jobj = new JSONObject(respJson);
		EvaluationLogEntity log = new EvaluationLogEntity();
		
		Tenant t = null;
		if (qaCtx == null) {
			t = com.intumit.solr.tenant.Tenant.getFromSession(req.getSession());
			log.setTenantId(t.getId());
		}
		else {
			t = qaCtx.getTenant();
			log.setTenantId(qaCtx.getTenant().getId());
		}

		if (jobj.has("logId")) {
			log = get(jobj.getInt("logId"));
		}
		log.setNamespace(namespace);
		int flag = jobj.optInt("flag", 0);
		String question = jobj.getString("originalQuestion");
		double score = 0;
		String eservice = "";
		String userType = "";
		String orignalTime = null;

		try {
			score = new Double(jobj.optString("score", "0"));
			eservice = jobj.optString("eservice", "");
			userType = jobj.optString("userType", "");
			orignalTime = jobj.optString("originalQuestionTime", "");
		}
		catch (Exception ignore) {
		}

		Long robotAnswerId = null;
		if (jobj.has("kid")) {
			robotAnswerId = jobj.getLong("kid");
		}

		Long correctAnswerId = null;
		String correctAnswerCategory = null;
		String description = null;
		if (jobj.has("correctAnswer")) {
			correctAnswerId = jobj.getLong("correctAnswer");

			if (correctAnswerId != 0) {
				try {
					SolrDocument doc = QAUtil.getInstance(t).getMainQASolrDocument(correctAnswerId);
					if (doc.getFieldValue("QA_CATEGORY_s") != null) {
						correctAnswerCategory = (String) doc.getFieldValue("QA_CATEGORY_s");
					}
				}
				catch (Exception e) {
				}
			}
		}

		if (jobj.has("correctAnswerCategory") && correctAnswerCategory == null) {
			correctAnswerCategory = jobj.getString("correctAnswerCategory");
		}

		if (jobj.has("des")){
			description = jobj.getString("des");			
		}
		else if (jobj.has("description")){
			description = jobj.getString("description");			
		}
		
		log.setOriginalTime(orignalTime);
		log.setTimestamp(Calendar.getInstance().getTime());
		log.setFlag(flag);
		log.setConversations(respJson);
		log.setScore(score);
		log.setEservice(eservice);
		log.setUserType(userType);
		log.setQuestion(question);
		log.setRobotAnswerId(robotAnswerId);
		log.setCorrectAnswerId(correctAnswerId);
		log.setDescription(description);
		String qaCategory = t.getDefaultQaCategory();
		if (jobj.has("currentQaCategory")) {
			qaCategory = jobj.getString("currentQaCategory");
			// 為了舊資料
			if (QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {
				qaCategory = t.getDefaultQaCategory();
			}
		}
		if (correctAnswerCategory != null) {
			log.setQaCategory(correctAnswerCategory);
		}
		else {
			log.setQaCategory(qaCategory);
		}

		setIdentitesAndSave(req, qaCtx, log);

		return log;
	}

	public static EvaluationLogEntity createEvaluationLogAndSave(HttpServletRequest req, int flag, Long kid, String question, String answer, String qaCategory) {
		try {
			Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(req.getSession());
			String batchImportQaCategory = StringUtils.isNotBlank(qaCategory) ? qaCategory : t.getDefaultQaCategory();
			Long correctAnswerId = kid;
			Long robotAnswerId = kid;
			double score = 0;

			JSONObject jobj = new JSONObject();
			jobj.put("flag", flag);
			jobj.put("originalQuestion", question);
			jobj.put("score", score);
			jobj.put("kid", robotAnswerId);
			jobj.put("correctAnswer", correctAnswerId);
			jobj.put("qaCategory", batchImportQaCategory);
			jobj.put("output", answer);

			/*
			EvaluationLogEntity log = new EvaluationLogEntity();
			log.setTenantId(t.getId());
			log.setNamespace(QAUtil.CURRENT_EVALUATION_LOG_NS);
			log.setTimestamp(Calendar.getInstance().getTime());
			log.setFlag(flag);
			log.setScore(score);
			log.setQuestion(question);
			log.setRobotAnswerId(robotAnswerId);
			log.setCorrectAnswerId(correctAnswerId);
			log.setDescription("");
			log.setQaCategory(batchImportQaCategory);
			log.setConversations(jobj.toString());
			*/
			EvaluationLogEntity log = EvaluationLogEntity.log(req, QAUtil.CURRENT_EVALUATION_LOG_NS, jobj.toString(2), null);
			
			return log;
		}
		catch(Exception ignore) {
			ignore.printStackTrace();
		}
		
		return null;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private static long SESSION_TIMEOUT = 5L * 60 * 1000;

	private static class EvaluationLogEntityResultTransformer implements ResultTransformer {

		private static final long serialVersionUID = -5956388704055686980L;

		private final Class resultClass;
		private boolean isInitialized;
		private String[] aliases;
		private Setter[] setters;

		public EvaluationLogEntityResultTransformer(Class resultClass) {
			if (resultClass == null) {
				throw new IllegalArgumentException("resultClass cannot be null");
			}
			isInitialized = false;
			this.resultClass = resultClass;
		}

		@Override
		public Object transformTuple(Object[] tuple, String[] aliases) {
			Object result;
			try {
				if (!isInitialized) {
					initialize(aliases);
				}
				else {
				    check(aliases);
				}
				result = resultClass.newInstance();
				for (int i = 0; i < aliases.length; i++) {
					if (setters[i] != null) {
						Object val = tuple[i];
						if(StringUtils.equalsIgnoreCase(setters[i].getMethodName(), "setCorrectAnswerId") || StringUtils.equalsIgnoreCase(setters[i].getMethodName(), "setRobotAnswerId"))
						if(tuple[i] instanceof Number) {
							val = ((Number)tuple[i]).longValue();
						}
					    setters[i].set(result, val, null);
					}
				}
			}
			catch (InstantiationException e) {
			    throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
			}
			catch (IllegalAccessException e) {
			    throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
			}
			return result;
		}

		private void initialize(String[] aliases) {
			PropertyAccessor propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] { PropertyAccessorFactory.getPropertyAccessor(resultClass, null), PropertyAccessorFactory.getPropertyAccessor("field") });
			this.aliases = new String[aliases.length];
			setters = new Setter[aliases.length];
			for (int i = 0; i < aliases.length; i++) {
				String alias = aliases[i];
				if (alias != null) {
				    this.aliases[i] = alias;
				    setters[i] = propertyAccessor.getSetter(resultClass, alias);
				}
			}
			isInitialized = true;
        }

		private void check(String[] aliases) {
		     if (!Arrays.equals(aliases, this.aliases)) {
		       throw new IllegalStateException("aliases are different from what is cached; aliases=" + Arrays.asList(aliases) + " cached=" + Arrays.asList(this.aliases));
		     }
	    }

		@Override
        public List transformList(List collection){
			return collection;
	    }

	}

}
