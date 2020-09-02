package com.intumit.solr.robot;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;

@Entity
/**
 * 縮網址存放處
 * 
 * @author herb
 */
public class UrlShortener {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name = "tenantId")
	private Integer tenantId;
	
	@Column(columnDefinition="varchar(64)")
	@Index(name = "mkey")
	private String mkey;
	
	private String fullUrl;

	@Column(columnDefinition="varchar(160)")
	@Index(name = "urlHashIdx")
	private String urlHash;
	
	@Column(columnDefinition="varchar(200)")
	@Index(name = "tag")
	private String tag;
	
	@Column(columnDefinition="varchar(200)")
	private String additionalInfo;
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	
	public static UrlShortener shorten(Tenant t, String url, String tag, String additionalInfo) {
		UrlShortener us = getByUrlHash(t.getId(), genUrlHash(url));
		
		if (us != null) {
			return us;
		}
		
		us = new UrlShortener();
		us.setFullUrl(url);
		us.setTenantId(t.getId());
		us.setTag(tag);
		us.setAdditionalInfo(additionalInfo);
		
		String mkey = Long.toHexString(System.currentTimeMillis());
		
		while (getByMkey(mkey) != null) {
			mkey = Long.toHexString(System.currentTimeMillis());
		}
		us.setMkey(mkey);
		
		saveOrUpdate(us);
		
		return us;
	}

	public static UrlShortener getByUrlHash(int tenantId, String urlHash) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(UrlShortener.class)
				.add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("urlHash", urlHash));
			return (UrlShortener) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}

	public static UrlShortener getByMkey(String key) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(UrlShortener.class)
				.add(Restrictions.eq("mkey", key));
			return (UrlShortener) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static boolean saveOrUpdate(UrlShortener ut) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(ut);
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

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getMkey() {
		return mkey;
	}

	public void setMkey(String key) {
		this.mkey = key;
	}

	public String getFullUrl() {
		return fullUrl;
	}

	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
		this.urlHash = genUrlHash(fullUrl);
	}

	public String getUrlHash() {
		return urlHash;
	}

	static String genUrlHash(String url) {
		return WiSeUtils.sha1(url);
	}
	
	public String toFullShortenUrl(String baseUrl) {
		return baseUrl + "/ss.jsp?uk=" + mkey;
	}
}
