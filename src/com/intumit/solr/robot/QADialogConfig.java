package com.intumit.solr.robot;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

/**
 * 情境對話的 entity，所有欄位除了 id & tenantId 之外，在 dialogConfig 當中應該都要有
 * 放 entity 只是為了資料管理（多台同步）之間的方便
 * 
 * @author herb
 *
 */
@Entity
public class QADialogConfig implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="tenantIdIdx")
	Integer tenantId;
	
	@Column(length = 32)
	@Index(name="mkeyIdIdx")
	String mkey;
	
	Boolean oldMode;

	@Index(name="tsIdx")
	Date dialogTimestamp;
	
	@Column(length = 64)
	String dialogName;
	
	@Lob
	String dialogDesc;
	
	@Index(name="deleteMarkIdx")
	Boolean deleteMark;

	
	/**
	 * 此欄位在有了 QADialogConfigVersion 之後變成暫存版本用的欄位
	 */
	@Lob
	String dialogConfig;
	
	Integer publishedVersionNumber;
	
	@Transient
	QADialogConfigVersion publishedVersion = null;
	
	public QADialogConfig() {
		super();
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	public String getMkey() {
		return mkey;
	}
	public void setMkey(String mkey) {
		this.mkey = mkey;
		//setJSONProperty("mkey", mkey);
	}
	public Boolean getOldMode() {
		return oldMode != null ? oldMode : false;
	}
	public void setOldMode(Boolean oldMode) {
		this.oldMode = oldMode;
	}
	public Boolean getDeleteMark() {
		return deleteMark != null ? deleteMark : false;
	}
	public void setDeleteMark(Boolean deleteMark) {
		this.deleteMark = deleteMark;
	}

	public String getDialogName() {
		return dialogName;
	}
	public void setDialogName(String dialogName) {
		this.dialogName = dialogName;
		//setJSONProperty("dialogName", dialogName);
	}
	public String getDialogDesc() {
		return dialogDesc;
	}
	public void setDialogDesc(String dialogDesc) {
		this.dialogDesc = dialogDesc;
		//setJSONProperty("dialogDesc", dialogDesc);
	}
	public String getDialogConfig() {
		return dialogConfig;
	}
	public QADialogConfigVersion getPublishedVersion() {
		if (publishedVersion == null) {
			publishedVersion = QADialogConfigVersion.getByKeyAndVersionNumber(tenantId, mkey, getPublishedVersionNumber());
		}
		return publishedVersion;
	}

	public Integer getPublishedVersionNumber() {
		return publishedVersionNumber != null ? publishedVersionNumber : 0;
	}

	public void setPublishedVersionNumber(Integer publishedVersionNumber) {
		this.publishedVersionNumber = publishedVersionNumber;
	}

	void setJSONProperty(String propertyName, Object value) {
		try {
			JSONObject o = getDialogConfigObject();
			o.put(propertyName, value);
			setDialogConfig(o.toString());
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	public boolean hasDraft() {
		if (StringUtils.trimToNull(dialogConfig) == null) {
			return false;
		}
		if (StringUtils.equals(dialogConfig, "{}")) {
			return false;
		}
		
		return true;
	}
	public JSONObject getDraftDialogConfigObject() {
		try {
			return new JSONObject(StringUtils.defaultString(dialogConfig, "{}"));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	public void clearDraftConfig() {
		dialogConfig = null;
	}
	/**
	 * 取得 published 版本，若 published 版本為 null，取 draft
	 * @return
	 */
	public JSONObject getDialogConfigObject() {
		try {
			QADialogConfigVersion cfgVer = getPublishedVersion();
			if (cfgVer != null) {
				return cfgVer.getDialogConfigObject();
			}
			return new JSONObject(StringUtils.defaultString(dialogConfig, "{}"));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	public void setDraftDialogConfig(String dialogConfig) {
		this.dialogConfig = dialogConfig;
		//JSONObject o = getDialogConfigObject();
		//mkey = o.optString("mkey");
		//dialogName = o.optString("dialogName");
		//dialogDesc = o.optString("dialogDesc");
		//dialogTimestamp = parseDate(o.optString("dialogTimestamp"));
	}
	public void setDialogConfig(String dialogConfig) {
		this.dialogConfig = dialogConfig;
	}
	/*private Date parseDate(String str) {
		if (str != null) {
			try {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:SS").parse(str);
			}
			catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Calendar.getInstance().getTime();
	}
	private String formatDate(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
		return sdf.format(d);
	}*/

	public Date getDialogTimestamp() {
		return dialogTimestamp;
	}
	public void setDialogTimestamp(Date dialogTimestamp) {
		this.dialogTimestamp = dialogTimestamp;
		//setJSONProperty("dialogTimestamp", formatDate(dialogTimestamp)); // 這裡應該有 JSON 存放 date 格式的問題
	}

	public static synchronized void saveOrUpdate(QADialogConfig p) {
		Session ses = null;
		Transaction tx = null;
		try {
			QADialogConfig oldOne = getByKey(p.getTenantId(), p.getMkey());
			String newMkey = p.getMkey();
			int retryNum = 2;
			
			while (oldOne != null) {
				if (!oldOne.getId().equals(p.getId())) {
					p.setMkey( newMkey + "(" + (retryNum++) + ")");
					oldOne = getByKey(p.getTenantId(), p.getMkey());
				}
				else {
					break;
				}
			}
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
			
			EventCenter.fireEvent(QADialogConfig.class.getName(), p.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(Integer tenantId, int id) throws Exception {
		try {
			QADialogConfig p = get(id);
			if (p.getTenantId() != tenantId) return;
			
			p.setDeleteMark(true);
			saveOrUpdate(p);
			
			EventCenter.fireEvent(QADialogConfig.class.getName(), p.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized List<QADialogConfig> list(Integer tenantId) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QADialogConfig.class)
					.add(Restrictions.or(Restrictions.eq("deleteMark", Boolean.FALSE), Restrictions.isNull("deleteMark")))
					.add(Restrictions.eq("tenantId", tenantId)).addOrder(Order.asc("id"));
			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result;
	}
	public static synchronized QADialogConfig get(int id) {
		try {
			return (QADialogConfig)HibernateUtil.getSession().get(QADialogConfig.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	public static QADialogConfig getByKey(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (QADialogConfig)ses.createCriteria(QADialogConfig.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("mkey", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
}
