package com.intumit.solr.form;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class Form implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final Integer DRAFT = 0;
	
	public static final Integer NOTIFIED = 5;
	
	public static final Integer SUBMIT = 10;

	@Id 
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	private String uid;
	
	@Lob
	private String formContent;
	
	private Date submitDate;
	
	private Integer status;
	
	private Integer tenantId;
	
	public Form() {}
	
	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).build();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getFormContent() {
		return formContent;
	}

	public void setFormContent(String formContent) {
		this.formContent = formContent;
	}

	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	
	public static Form saveOrUpdate(Form form) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			form.setSubmitDate(new Date(System.currentTimeMillis()));
			ses.saveOrUpdate(form);
			tx.commit();
		} catch (HibernateException e) {
			tx.rollback();
			throw e;
		} finally {
			ses.close();
		}
		return form;
	}
	
	public static Form get(Integer tenantId, String uid) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(Form.class);
			ct.add(Restrictions.eq("uid", uid));
			ct.add(Restrictions.eq("tenantId", tenantId));
			ct.add(Restrictions.ne("status", SUBMIT));
			ct.addOrder(Order.desc("id"));
			List<Form> result = ct.list();
			if(!result.isEmpty()) {
				return result.get(0);
			}
		}
		catch(HibernateException ignore) {
			ignore.printStackTrace();
		}
		finally {
			ses.close();
		}
		return null;
	}
	
	public static void deleteAll() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + Form.class.getName()).executeUpdate();
			tx.commit();
		}
		catch(HibernateException ignore) {
			tx.rollback();
			ignore.printStackTrace();
		}
		finally {
			ses.close();
		}
	}
	
	public static void deleteById(Long id) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + Form.class.getName() + " where id = " + id).executeUpdate();
			tx.commit();
		}
		catch(HibernateException ignore) {
			tx.rollback();
			ignore.printStackTrace();
		}
		finally {
			ses.close();
		}
	}
	
	public static void deleteTenantForms(Integer tenantId) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + Form.class.getName() + " where tenantId = " + tenantId).executeUpdate();
			tx.commit();
		}
		catch(HibernateException ignore) {
			tx.rollback();
			ignore.printStackTrace();
		}
		finally {
			ses.close();
		}
	}
	
	public static void deleteByUid(Integer tenantId, String uid) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Query q = ses.createQuery("delete from " + Form.class.getName() + " where uid = :uid and tenantId = :tid " );
			q.setString("uid", uid);
			q.setInteger("tid", tenantId);
			q.executeUpdate();
			tx.commit();
		}
		catch(HibernateException ignore) {
			tx.rollback();
			ignore.printStackTrace();
		}
		finally {
			ses.close();
		}
	}
	
}
