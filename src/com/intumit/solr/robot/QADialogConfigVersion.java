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
import com.intumit.solr.admin.AdminUser;

/**
 * 情境對話的 entity 版本控制
 * 存在 QADialogConfigVersion 的都是儲存版本過的
 * 
 * @author herb
 *
 */
@Entity
public class QADialogConfigVersion implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="tenantIdIdx")
	Integer tenantId;
	
	@Column(length = 32)
	@Index(name="mkeyIdIdx")
	String mkey;

	@Index(name="tsIdx")
	Date dialogTimestamp;
	
	@Lob
	String dialogConfig;

	@Index(name="verNumIdx")
	Integer versionNumber;
	
	@Lob
	String versionComment;
	
	Integer contributor;
	
	public QADialogConfigVersion() {
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
	public Integer getVersionNumber() {
		return versionNumber != null ? versionNumber : 0;
	}

	public void setVersionNumber(Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

	public String getVersionComment() {
		return versionComment;
	}

	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}

	public Integer getContributor() {
		return contributor;
	}

	public void setContributor(Integer contributor) {
		this.contributor = contributor;
	}

	public String getDialogConfig() {
		return dialogConfig;
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
	public JSONObject getDialogConfigObject() {
		try {
			return new JSONObject(StringUtils.defaultString(dialogConfig, "{}"));
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	public void setDialogConfig(String dialogConfig) {
		this.dialogConfig = dialogConfig;
		//JSONObject o = getDialogConfigObject();
		//mkey = o.optString("mkey");
		//dialogTimestamp = parseDate(o.optString("dialogTimestamp"));
	}
	private Date parseDate(String str) {
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
	}

	public Date getDialogTimestamp() {
		return dialogTimestamp;
	}
	public void setDialogTimestamp(Date dialogTimestamp) {
		this.dialogTimestamp = dialogTimestamp;
		setJSONProperty("dialogTimestamp", formatDate(dialogTimestamp)); // 這裡應該有 JSON 存放 date 格式的問題
	}

	public static synchronized void saveOrUpdate(QADialogConfigVersion p) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
			
			EventCenter.fireEvent(QADialogConfigVersion.class.getName(), p.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized int saveAndCommitNewVersion(QADialogConfigVersion p, String comment, AdminUser admUser) {
		Session ses = null;
		Transaction tx = null;
		try {
			QADialogConfigVersion head = getHeadVersionByKey(p.getTenantId(), p.getMkey());
			if (head != null) {
				p.setVersionNumber(head.getVersionNumber() + 1);
			}
			else {
				p.setVersionNumber(1);
			}
			p.setContributor(admUser.getId());
			p.setVersionComment(comment);
			
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
			
			EventCenter.fireEvent(QADialogConfigVersion.class.getName(), p.getTenantId(), "reload", null);
			
			return p.getVersionNumber();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		
		return 0;
	}

	public static synchronized void delete(Integer tenantId, int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			QADialogConfigVersion p = get(id);
			if (p.getTenantId() != tenantId) return;
			
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + QADialogConfigVersion.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
			
			EventCenter.fireEvent(QADialogConfigVersion.class.getName(), p.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized List<QADialogConfigVersion> list(Integer tenantId) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QADialogConfigVersion.class)
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
	public static synchronized QADialogConfigVersion get(int id) {
		try {
			return (QADialogConfigVersion)HibernateUtil.getSession().get(QADialogConfigVersion.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	public static QADialogConfigVersion getHeadVersionByKey(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			
			
			List<QADialogConfigVersion> list = (List<QADialogConfigVersion>)ses.createCriteria(QADialogConfigVersion.class)
					.addOrder(Order.desc("versionNumber"))
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("mkey", key)).list();
			
			if (list != null && list.size() > 0) {
				return list.get(0);
			}
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	public static List<QADialogConfigVersion> listByKey(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			
			
			return (List<QADialogConfigVersion>)ses.createCriteria(QADialogConfigVersion.class)
					.addOrder(Order.desc("versionNumber"))
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("mkey", key)).list();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	public static QADialogConfigVersion getByKeyAndVersionNumber(Integer tenantId, String key, Integer verNum){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (QADialogConfigVersion)ses.createCriteria(QADialogConfigVersion.class)
					.addOrder(Order.desc("versionNumber"))
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("versionNumber", verNum))
					.add(Restrictions.eq("mkey", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
}
