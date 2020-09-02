package com.intumit.solr.robot;

import java.io.Serializable;
import java.sql.Blob;
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
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAEvaluationLogQueue.Status;

@Entity
public class QAEvaluationLog implements Serializable {
	public static final int FLAG_STAR = 1;
	public static final int FLAG_THUMBUP = 2;
	public static final int FLAG_PENCIL = 3;
	public static final int FLAG_WRONG_BUT_ACCEPTABLE = 4;
	public static final int FLAG_THUMBDOWN = 5;
	public static final int FLAG_VERYBAD = 6;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Index(name = "tenantIdIdx")
	private Integer tenantId;

	private Date createdTime;
	
	@Column(columnDefinition = "bit default 1", nullable = false)
	private boolean showInList = true;

	private String charset;

	private String inputFileName;

	@Lob
	private Blob inputFile;

	@Lob
	private Blob detailFile;

	@Lob
	private Blob reportFile;

	private Status status;

	private double progress;

	private String statusMsg;

	public static synchronized QAEvaluationLog get(long id) {
		try {
			return (QAEvaluationLog) HibernateUtil.getSession().get(QAEvaluationLog.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static List<QAEvaluationLog> list(int tid, boolean showInList) {
		List<QAEvaluationLog> builds = new ArrayList<QAEvaluationLog>();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEvaluationLog.class).add(Restrictions.eq("tenantId", tid))
					.add(Restrictions.eq("showInList", showInList)).addOrder(Order.asc("status")).addOrder(Order.asc("id"));
			@SuppressWarnings("unchecked")
			List<QAEvaluationLog> list = ct.list();
			builds = list;
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return builds;
	}

	public static List<QAEvaluationLog> listByPenging() {
		List<QAEvaluationLog> builds = new ArrayList<QAEvaluationLog>();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEvaluationLog.class).add(Restrictions.eq("status", Status.PENDING))
					.addOrder(Order.asc("createdTime"));
			@SuppressWarnings("unchecked")
			List<QAEvaluationLog> list = ct.list();
			builds = list;
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return builds;
	}

	public static List<QAEvaluationLog> listByTenant(Integer tenantId) {
		List<QAEvaluationLog> builds = new ArrayList<QAEvaluationLog>();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAEvaluationLog.class).add(Restrictions.eq("tenantId", tenantId))
					.addOrder(Order.asc("createdTime"));
			@SuppressWarnings("unchecked")
			List<QAEvaluationLog> list = ct.list();
			builds = list;
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return builds;
	}

	public void delete() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			String hql = "delete " + QAEvaluationLog.class.getName() + " where id = :id";
			ses.createQuery(hql).setLong("id", id).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public void deleteByDocIdAndSave() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			deleteByTenantcAndDocId(tenantId, inputFileName);
			save();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	static void deleteByTenantcAndDocId(Integer tenantId, String inputFileName) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			String hql = "delete " + QAEvaluationLog.class.getName()
					+ " where tenantId = :tenantId AND inputFileName = :inputFileName";
			ses.createQuery(hql).setInteger("tenantId", tenantId).setString("inputFileName", inputFileName)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	void save() {
		createdTime = new Date();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.save(this);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public void update() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.update(this);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public QAEvaluationLogQueue.Status getStatus() {
		return status;
	}

	public void setStatus(QAEvaluationLogQueue.Status status) {
		this.status = status;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public Blob getInputFile() {
		return inputFile;
	}

	public void setInputFile(Blob inputFile) {
		this.inputFile = inputFile;
	}

	public Blob getReportFile() {
		return reportFile;
	}

	public void setReportFile(Blob reportFile) {
		this.reportFile = reportFile;
	}

	public Blob getDetailFile() {
		return detailFile;
	}

	public void setDetailFile(Blob detailFile) {
		this.detailFile = detailFile;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getStatusMsg() {
		return statusMsg;
	}

	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}
	
	public boolean isShowInList() {
		return showInList;
	}

	public void setShowInList(boolean showInList) {
		this.showInList = showInList;
	}
	
	/**
	 * 統一判斷 Last Test Result（1 為正確, 0 為錯誤, null 為 Unknown）的邏輯
	 * 
	 * @param loggedFlag
	 * @param expectKid
	 * @param loggedKid
	 * @param actualKid
	 * @return
	 */
	public static Integer judgeCorrectOrNot(int loggedFlag, Long expectKid, Long loggedKid, Long actualKid) {
		Integer ltr = null;

		switch (loggedFlag) {
			case FLAG_STAR:
			case FLAG_THUMBUP:
				if (expectKid == null && loggedKid != null) expectKid = loggedKid;	// flag 1 or 2，沒有 correctAnswerId 應該可以假設 robotAnswerId 是對的
				
				if (actualKid == null && expectKid == null
						|| (actualKid != null && actualKid.equals(expectKid))) {
					ltr = 1;
				}
				else {
					ltr = 0;
				}

				break;

			case FLAG_WRONG_BUT_ACCEPTABLE:
				// 非正確答案但可接受，因此 expect kid 為對，logged kid 亦為對
				if (actualKid == null && expectKid == null
						|| (actualKid != null && actualKid.equals(expectKid))) {
					ltr = 1;
				}
				else if (actualKid == null && loggedKid == null
						|| (actualKid != null && actualKid.equals(loggedKid))) {
					ltr = 1;
				}
				else {
					ltr = 0;
				}
				break;

			case FLAG_THUMBDOWN:
			case FLAG_VERYBAD:
				if (actualKid == null && expectKid == null
						|| (actualKid != null && actualKid.equals(expectKid))) {
					ltr = 1;
				}
				else {
					ltr = 0;
				}

				break;

			default:
				break;
		}

		return ltr;
	}

}
