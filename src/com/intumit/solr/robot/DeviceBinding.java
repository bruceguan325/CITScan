package com.intumit.solr.robot;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class DeviceBinding {
	
	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	public enum Status { normal, deleted }
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private long id;
	
	@Column(length = 300, unique=true)
	@Index(name = "deviceTokenIdx")
	private String deviceToken;
	
	@Column(length = 32, unique=true)
	@Index(name = "nidIdx")
	private String nid;

	private Date updateTime;
	
	@Enumerated(EnumType.STRING)
	private Status status = Status.normal;
	
	public static DeviceBinding getByDeviceToken(String token) {
		DeviceBinding db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DeviceBinding.class)
				.add(Restrictions.eq("deviceToken", token));
			db = (DeviceBinding) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static DeviceBinding getByNid(String nid) {
		DeviceBinding db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(DeviceBinding.class)
				.add(Restrictions.eq("nid", nid));
			db = (DeviceBinding) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static boolean setStatusToDeleted(String nid) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DeviceBinding db = getByNid(nid);
			if(db != null){
				db.status = Status.deleted;
				db.updateTime = new Date();
				ses.update(db);
			}
			tx.commit();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return success;
	}
	
	public static boolean setStatusToNormal(String nid, String token) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			deleteByDeviceToken(token);
			DeviceBinding db = getByNid(nid);
			if(db == null){
				db = new DeviceBinding();
			}
			db.nid = nid;
			db.deviceToken = token;
			db.status = Status.normal;
			db.updateTime = new Date();
			ses.saveOrUpdate(db);
			tx.commit();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return success;
	}
	
	public static void deleteByDeviceToken(String token) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			DeviceBinding db = getByDeviceToken(token);
			if(db != null){
				ses.delete(db);
			}
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

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public String getNid() {
		return nid;
	}

	public void setNid(String nid) {
		this.nid = nid;
	}

	public Date getUpdateTime() {
		return updateTime;
	}
	
	public String getFormattedUpdateTime() {
		return new SimpleDateFormat(DATETIME_FORMAT).format(updateTime);
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

}
