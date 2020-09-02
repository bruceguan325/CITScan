package com.intumit.solr.robot;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
/**
 * 存放使用者資料的地方
 * LINE / Facebook 這類頻道都有機會取得使用者的資訊，若要應用或者統計，必需要有地方儲存
 * 若為 Cookie，則應該有 expire time，確保資訊會正常過期
 * 
 * @author herb
 */
public class UserClue implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	public enum Status { normal, deleted, blockedByUser, blockedBySystem }

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name = "tenantId")
	private Integer tenantId;
	
	@Column(columnDefinition="varchar(200)")
	@Index(name = "iosDeviceToken")
	private String iosDeviceToken;
	
	@Column(columnDefinition="varchar(200)")
	@Index(name = "androidDeviceToken")
	private String androidDeviceToken;
	
	@Column(length=64)
	@Index(name = "cookieUserId")
	private String cookieUserId;
	
	@Column(length=64)
	@Index(name = "lineUserId")
	private String lineUserId;
	
	@Column(length=128)
	@Index(name = "fbPsId")
	private String fbPsId;
	
	@Index(name = "adminUserId")
	private Integer adminUserId;

	@Index(name = "updateTime")
	private Date updateTime;

	@Index(name = "createdTime")
	private Date createdTime;
	
	@Index(name = "expireTime")
	private Date expireTime;
	
	@Enumerated(EnumType.STRING)
	private Status status = Status.normal;

	@Column(columnDefinition="varchar(200)")
	private String nickname;

	@Column(columnDefinition="varchar(200)")
	private String email;
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public static synchronized UserClue get(long id) {
		try {
			return (UserClue)HibernateUtil.getSession().get(UserClue.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static UserClue getByAndroidDeviceToken(int tenantId, String token) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("androidDeviceToken", token))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static UserClue getByIosDeviceToken(int tenantId, String token) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("iosDeviceToken", token))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static UserClue getByCookieUserId(int tenantId, String cookieUserId) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("cookieUserId", cookieUserId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
						
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static UserClue getByLineUserId(int tenantId, String lineUserId) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("lineUserId", lineUserId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static UserClue getByFbPsId(int tenantId, String fbPsId) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("fbPsId", fbPsId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static UserClue getByAdminUserId(int tenantId, int adminUserId) {
		UserClue db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClue.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("adminUserId", adminUserId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
			db = (UserClue) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static boolean saveOrUpdate(UserClue clue) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			clue.updateTime = now;
			
			if (clue.id == null || clue.createdTime == null) {
				clue.createdTime = now;
			}
			ses.saveOrUpdate(clue);
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
	
	public static void delete(UserClue uc) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(uc);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIosDeviceToken() {
		return iosDeviceToken;
	}

	public void setIosDeviceToken(String deviceToken) {
		this.iosDeviceToken = deviceToken;
	}
	
	public String getAndroidDeviceToken() {
		return androidDeviceToken;
	}

	public void setAndroidDeviceToken(String androidDeviceToken) {
		this.androidDeviceToken = androidDeviceToken;
	}

	public String getLineUserId() {
		return lineUserId;
	}

	public void setLineUserId(String lineUserId) {
		this.lineUserId = lineUserId;
	}

	public String getCookieUserId() {
		return cookieUserId;
	}

	public void setCookieUserId(String cookieUserId) {
		this.cookieUserId = cookieUserId;
	}

	public String getFbPsId() {
		return fbPsId;
	}

	public void setFbPsId(String fbPsId) {
		this.fbPsId = fbPsId;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public Integer getAdminUserId() {
		return adminUserId;
	}

	public void setAdminUserId(Integer adminUserId) {
		this.adminUserId = adminUserId;
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

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
	
	public boolean isExpired() {
		if (expireTime == null)
			return false;
		
		return expireTime.before(Calendar.getInstance().getTime());
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "UserClue [" + (id != null ? "id=" + id + ", " : "") + (tenantId != null ? "tenantId=" + tenantId + ", " : "")
				+ (iosDeviceToken != null ? "iosDeviceToken=" + iosDeviceToken + ", " : "")
				+ (androidDeviceToken != null ? "androidDeviceToken=" + androidDeviceToken + ", " : "")
				+ (cookieUserId != null ? "cookieUserId=" + cookieUserId + ", " : "") + (lineUserId != null ? "lineUserId=" + lineUserId + ", " : "")
				+ (adminUserId != null ? "adminUserId=" + adminUserId + ", " : "") + (updateTime != null ? "updateTime=" + updateTime + ", " : "")
				+ (expireTime != null ? "expireTime=" + expireTime + ", " : "") + (status != null ? "status=" + status + ", " : "")
				+ (nickname != null ? "nickname=" + nickname + ", " : "") + (email != null ? "email=" + email : "") + "]";
	}

}
