package com.intumit.viewRecord;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class KeywordToIdRecord {
	@Id @GeneratedValue
	private int id;

	@Index(name="idRecord")
	private String idRecord;

	@Index(name="viewTime")
	int viewTime;
	
	@ManyToOne
	private ViewRecordEntity viewRecordEntity;

	@Index(name="modifyTime")
	private Date modifyTime;
	
	public KeywordToIdRecord() {
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIdRecord() {
		return idRecord;
	}

	public void setIdRecord(String idRecord) {
		this.idRecord = idRecord;
	}

	public int getViewTime() {
		return viewTime;
	}

	public void setViewTime(int viewTime) {
		this.viewTime = viewTime;
	}
	
	public ViewRecordEntity getViewRecordEntity() {
		return viewRecordEntity;
	}

	public void setViewRecordEntity(ViewRecordEntity viewRecordEntity) {
		this.viewRecordEntity = viewRecordEntity;
	}
	
	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}
	
	public void save() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(this);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
}
