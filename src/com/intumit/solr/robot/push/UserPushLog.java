package com.intumit.solr.robot.push;

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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
/**
 * 存放要推播給使用者訊息的地方
 * 1. 有條件（條件類型目前暫訂支援「時間」、「SQL」跟「Script」）
 * 2. PUSH 的內容可以有「直接文字」跟「指定QA（Kid）」
 * 3. 可以指定頻道（單一、多選、全部或者 by 用戶設定）
 * 4. 可指定單一用戶
 * 4. 可指定通知自刪條件（通知幾次後自刪 或 幾月幾號之後自刪）
 * 5. 有此 Queue 是否已完成的狀態紀錄
 * 
 * @author herb
 */
public class UserPushLog implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	public enum Status { success, failed, unknown, }

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Index(name = "tenantId")
	private Integer tenantId;
	
	@Index(name = "pushDataId")
	private Integer pushDataId;
	
	@Index(name = "userClueId")
	private Long userClueId;
	
	@Index(name = "channel")
	@Column(columnDefinition="varchar(64)")
	private String channel;
	
	//@Index(name = "createdTime")
	private Date createdTime;
	
	private String additionalInfo;
	
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getPushDataId() {
		return pushDataId;
	}

	public void setPushDataId(Integer pushDataId) {
		this.pushDataId = pushDataId;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public static synchronized UserPushLog get(long id) {
		try {
			return (UserPushLog)HibernateUtil.getSession().get(UserPushLog.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static UserPushLog getByCookieUserId(int tenantId, String cookieUserId) {
		UserPushLog db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserPushLog.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("cookieUserId", cookieUserId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
						
			db = (UserPushLog) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static boolean saveOrUpdate(UserPushLog clue) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			
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
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

}
