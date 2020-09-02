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
import javax.persistence.Lob;

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
 * 4. 可指定通知自刪條件（通知幾次後自刪 或 幾月幾號之後自刪）
 * 5. 有此 Queue 是否已完成的狀態紀錄
 * 
 * @author herb
 */
public class PushData implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	/**
	 * 為了不要再生出一個 ContentType 的物件（太複雜）
	 * 就把所有可能的 ContentType + MetaType 混在一起
	 * 
	 * @author herb
	 */
	public enum ContentType { 
		QA(null), 
		TEXT("text/plain"), 
		JSON_OBJECT("application/json"), 
		JSON_ARRAY("application/json"), 
		JPEG("image/jpg"), 
		PNG("image/png"), 
		HTML("text/html"), 
		XML("text/xml"), 
		; 
		
		String contentType;

		private ContentType(String contentType) {
			this.contentType = contentType;
		}

		public String getContentType() {
			return contentType;
		}
	}
	public enum TargetChannelsType { all, byConfig, byUserSubscription }

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Index(name = "tenantId")
	private Integer tenantId;

	@Enumerated(EnumType.STRING)
	@Index(name = "targetChannelsType")
	private TargetChannelsType targetChannelsType;
	private String targetChannels;

	@Enumerated(EnumType.STRING)
	@Index(name = "contentType")
	private ContentType contentType;
	@Lob
	private String pushContent;

	@Index(name = "traceCode")
	@Column(columnDefinition="varchar(128)")
	private String traceCode;
	
	@Index(name = "createdTime")
	private Date createdTime;
	
	@Index(name = "updateTime")
	private Date updateTime;
	
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public static synchronized PushData get(long id) {
		try {
			return (PushData)HibernateUtil.getSession().get(PushData.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static PushData getByCookieUserId(int tenantId, String cookieUserId) {
		PushData db = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(PushData.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("cookieUserId", cookieUserId))
				.add(Restrictions.or(
						Restrictions.isNull("expireTime"),
						Restrictions.ge("expireTime", Calendar.getInstance().getTime())
					));
						
			db = (PushData) ct.uniqueResult();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return db;
	}
	
	public static boolean saveOrUpdate(PushData clue) {
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

	public static synchronized void delete(Long id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			PushData phts = (PushData)ses.get(PushData.class, id);
			ses.delete(phts);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
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

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
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

	public TargetChannelsType getTargetChannelsType() {
		return targetChannelsType;
	}

	public void setTargetChannelsType(TargetChannelsType targetChannelsType) {
		this.targetChannelsType = targetChannelsType;
	}

	public String getTargetChannels() {
		return targetChannels;
	}

	public void setTargetChannels(String targetChannels) {
		this.targetChannels = targetChannels;
	}

	public ContentType getContentType() {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	public String getPushContent() {
		return pushContent;
	}

	public void setPushContent(String pushContent) {
		this.pushContent = pushContent;
	}

}
