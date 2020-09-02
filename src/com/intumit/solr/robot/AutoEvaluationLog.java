package com.intumit.solr.robot;

import java.math.BigDecimal;
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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
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
public class AutoEvaluationLog {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	@Index(name="scheduleTimestampIdx")
	private Long scheduleTimestamp;
	
	private Integer apiMark;

	@Column(length=255)
	private String scheduleNote;
	
	@Index(name="evaluationLogEntityIdIdx")
	private Integer evaluationLogEntityId;

	@Index(name = "robotAnswerIdIdx")
	private Long robotAnswerId;

	@Index(name = "correctAnswerIdIdx")
	private Long correctAnswerId;

	@Index(name = "resultStatusIdx")
	private Integer resultStatus;

	private Double score;

	@Index(name = "timestampIdx")
	private Date myTimestamp;
	
	private Integer qtime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getApiMark() {
		return apiMark != null ? apiMark : 0;
	}

	public void setApiMark(int apiMark) {
		this.apiMark = apiMark;
	}

	public Date getMyTimestamp() {
		return myTimestamp;
	}

	public void setMyTimestamp(Date myTimestamp) {
		this.myTimestamp = myTimestamp;
	}

	public void setQtime(Integer qtime) {
		this.qtime = qtime;
	}

	public Long getRobotAnswerId() {
		return robotAnswerId;
	}

	public void setRobotAnswerId(Long robotAnswerId) {
		this.robotAnswerId = robotAnswerId;
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
	
	public Integer getQtime() {
		return qtime;
	}

	public void setQtime(int qtime) {
		this.qtime = qtime;
	}

	public Long getScheduleTimestamp() {
		return scheduleTimestamp;
	}

	public void setScheduleTimestamp(Long scheduleTimestamp) {
		this.scheduleTimestamp = scheduleTimestamp;
	}

	public String getScheduleNote() {
		return scheduleNote;
	}

	public void setScheduleNote(String scheduleNote) {
		this.scheduleNote = scheduleNote;
	}

	public Integer getEvaluationLogEntityId() {
		return evaluationLogEntityId;
	}

	public void setEvaluationLogEntityId(Integer evaluationLogEntityId) {
		this.evaluationLogEntityId = evaluationLogEntityId;
	}

	public Integer getResultStatus() {
		return resultStatus;
	}

	public void setResultStatus(Integer resultStatus) {
		this.resultStatus = resultStatus;
	}

	public static synchronized AutoEvaluationLog get(int id) {
		try {
			return (AutoEvaluationLog)HibernateUtil.getSession().get(AutoEvaluationLog.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	/*public static synchronized Number countBy(Long scheduleTimestamp, Long kid, Integer resultStatus, Date after, Date before) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(AutoEvaluationLog.class);

			if (scheduleTimestamp != null) {
				ct.add(Restrictions.eq("scheduleTimestamp", scheduleTimestamp));
			}
			if (resultStatus != null) {
				ct.add(Restrictions.eq("resultStatus", resultStatus));
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

			return (Number)ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}*/

	public static synchronized void save(AutoEvaluationLog log) {
		Session ses = null;
		Transaction tx = null;
		try {
			Date now = Calendar.getInstance().getTime();
			log.setMyTimestamp(now);

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

	public static synchronized void update(AutoEvaluationLog log) {
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

	public static AutoEvaluationLog log(EvaluationLogEntity logEntity, Long scheduleTimestamp, int apiMark, String note, Integer resultStatus, JSONObject respJson, long qtime) throws JSONException {
		AutoEvaluationLog log = new AutoEvaluationLog();

		log.setScheduleTimestamp(scheduleTimestamp);
		log.setScheduleNote(note);
		log.setEvaluationLogEntityId(logEntity.getId());

		if (respJson.has("logId")) {
			log = get(respJson.getInt("logId"));
		}
		
		double score = respJson.has("score") ? respJson.getDouble("score") : 0;

		Long robotAnswerId = null;
		if (respJson.has("kid")) {
			robotAnswerId = respJson.getLong("kid");
		}

		Long correctAnswerId = null;
		if (respJson.has("correctAnswer")) {
			correctAnswerId = respJson.getLong("correctAnswer");
		}

		log.setMyTimestamp(Calendar.getInstance().getTime());
		log.setApiMark(apiMark);
		log.setResultStatus(resultStatus);
		log.setScore(score);
		log.setRobotAnswerId(robotAnswerId);
		log.setCorrectAnswerId(correctAnswerId);
		log.setQtime((int)qtime);
		
		save(log);

		return log;
	}

	private static class AutoEvaluationLogResultTransformer implements ResultTransformer {

		private static final long serialVersionUID = -5956388704055686980L;

		private final Class resultClass;
		private boolean isInitialized;
		private String[] aliases;
		private Setter[] setters;

		public AutoEvaluationLogResultTransformer(Class resultClass) {
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
