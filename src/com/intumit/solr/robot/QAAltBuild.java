package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class QAAltBuild implements Serializable {
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private long id;
	
	@Index(name = "tenantIdIdx")
	private Integer tenantId;
	
	@Column(length = 64)
	@Index(name = "docIdIdx")
	private String docId;
	
	private long kid;
	
	@Lob
	private String qAltTpls;
	
	@Transient
	private String derivedAltTpls;

	@Index(name = "createdTimeIdx")
	private Date createdTime;
	
	@Transient
	private boolean expandSynonyms = true;
	
	private String userName;
	
	public static List<QAAltBuild> list() {
		List<QAAltBuild> builds = new ArrayList<QAAltBuild>();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAAltBuild.class)
				.addOrder(Order.asc("createdTime"));
			@SuppressWarnings("unchecked")
			List<QAAltBuild> list = ct.list();
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
	
	public static List<QAAltBuild> listByTenant(Integer tenantId) {
		List<QAAltBuild> builds = new ArrayList<QAAltBuild>();
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAAltBuild.class).add(Restrictions.eq("tenantId", tenantId))
				.addOrder(Order.asc("createdTime"));
			@SuppressWarnings("unchecked")
			List<QAAltBuild> list = ct.list();
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
			String hql = "delete " + QAAltBuild.class.getName() + " where id = :id";
			ses.createQuery(hql)
		    	.setLong("id", id)
	    		.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public void deleteByDocIdAndSave(){
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			deleteByTenantcAndDocId(tenantId, docId);
			save();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	static void deleteByTenantcAndDocId(Integer tenantId, String docId) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			String hql = "delete " + QAAltBuild.class.getName() + " where tenantId = :tenantId AND docId = :docId";
			ses.createQuery(hql)
	    		.setInteger("tenantId", tenantId)
		    	.setString("docId", docId)
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

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public long getKid() {
		return kid;
	}

	public void setKid(long kid) {
		this.kid = kid;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public String getQAltTpls() {
		return qAltTpls;
	}

	public void setQAltTpls(String qAltTpls) {
		this.qAltTpls = qAltTpls;
	}

	public String getDerivedAltTpls() {
		return (StringUtils.trimToNull(derivedAltTpls) != null) ? derivedAltTpls : qAltTpls;
	}

	public void setDerivedAltTpls(String derivedAltTpls) {
		this.derivedAltTpls = derivedAltTpls;
	}

	public boolean isExpandSynonyms() {
		return expandSynonyms;
	}

	public void setExpandSynonyms(boolean fastProcess) {
		this.expandSynonyms = fastProcess;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String getUserName() {
		return userName;
	}
}
