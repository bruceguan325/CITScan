package com.intumit.solr.tenant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class Apikey implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Index(name="tenantIdIdx")
	private Integer tenantId;
	
	private String name;
	
	@Index(name="apikeyIdx")
	@Column(length = 32)
	private String apikey;

	private Boolean enableRateLimitByIP = null;
	private Boolean enableRateLimitByQAContext = null;
	private Integer rateLimitByIpPerSec;
	private Integer rateLimitByQAContextPerSec;
	
	@Index(name="publishDateIdx")
	private Date publishDate;
	
	@Index(name="expireDateIdx")
	private Date expireDate;
	
	private Long callCounter;
	private Boolean enableApikey;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}
	
	public Tenant getTenant() {
		return Tenant.get(tenantId);
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}
	
	public Long getCallCounter() {
		return callCounter;
	}

	public void setCallCounter(Long callCounter) {
		this.callCounter = callCounter;
	}

	public Boolean getEnableRateLimitByIP() {
		return enableRateLimitByIP != null ? enableRateLimitByIP : Tenant.DEFAULT_ENABLE_RATE_LIMIT_BY_IP_ADDR;
	}

	public void setEnableRateLimitByIP(Boolean enableRateLimitByIP) {
		this.enableRateLimitByIP = enableRateLimitByIP;
	}

	public Boolean getEnableRateLimitByQAContext() {
		return enableRateLimitByQAContext != null ? enableRateLimitByQAContext : Tenant.DEFAULT_ENABLE_RATE_LIMIT_BY_QA_CONTEXT;
	}

	public void setEnableRateLimitByQAContext(Boolean enableRateLimitByQAContext) {
		this.enableRateLimitByQAContext = enableRateLimitByQAContext;
	}

	public Integer getRateLimitByIpPerSec() {
		if (rateLimitByIpPerSec == null) {
			if (tenantId != null) {
				return Tenant.get(tenantId).getRateLimitByIpPerSec();
			}
			else {
				return Tenant.DEFAULT_RATE_LIMIT_BY_IP_PER_SECOND;
			}
		}
		return rateLimitByIpPerSec;
	}

	public void setRateLimitByIpPerSec(Integer rateLimitByIpPerSec) {
		this.rateLimitByIpPerSec = rateLimitByIpPerSec;
	}

	public Integer getRateLimitByQAContextPerSec() {
		if (rateLimitByQAContextPerSec == null) {
			if (tenantId != null) {
				return Tenant.get(tenantId).getRateLimitByQAContextPerSec();
			}
			else {
				return Tenant.DEFAULT_RATE_LIMIT_BY_QA_CONTEXT_PER_SECOND;
			}
		}
		return rateLimitByQAContextPerSec;
	}

	public void setRateLimitByQAContextPerSec(Integer rateLimitByQAContextPerSec) {
		this.rateLimitByQAContextPerSec = rateLimitByQAContextPerSec;
	}

	public Date getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	public Date getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}

	public Boolean getEnableApikey() {
		return enableApikey != null ? enableApikey : false;
	}

	public void setEnableApikey(Boolean enable) {
		this.enableApikey = enable;
	}
	
	public boolean isValid() {
		if (!this.getEnableApikey())
			return false;
		
		Date now = Calendar.getInstance().getTime();
		
		if (getPublishDate() != null && now.before(getPublishDate()))
			return false;
		
		if (getExpireDate() != null && now.after(getExpireDate()))
			return false;
		
		return true;
	}

	public static synchronized Long saveOrUpdate(Apikey apikey) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(apikey);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return apikey.getId();
	}

	public static synchronized Apikey get(Long id) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (Apikey)ses.get(Apikey.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}


	public static synchronized void delete(Long id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Apikey phts = (Apikey)ses.get(Apikey.class, id);
			ses.delete(phts);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}


	@SuppressWarnings("unchecked")
	public static List<Apikey> list() {
		List<Apikey> result = new ArrayList<Apikey>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(Apikey.class).addOrder(Order.asc("id"));
			ct.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
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
	public static synchronized List<Apikey> list(Integer tenantId) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(Apikey.class)
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

	public static Apikey getByApiKey(String apikey) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(Apikey.class);
			ct.add( Restrictions.eq("apikey", apikey ) );

			Apikey phts = (Apikey)ct.uniqueResult();
			return phts;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static String genApikey() {
		while (true) {
			int length = 16;
			String hs = "";
	
			while (length > 0) {
				hs += StringUtils.leftPad(Integer.toHexString((int)(Math.random() * 65536)), 4, '0');
				length -= 4;
			}
	
			System.out.println("hs:" + hs + ":" + hs.length());
			String newKey = getVerifyCode(hs) + hs;

			Apikey dup = getByApiKey(newKey);
			
			if (dup == null)
				return newKey;
		}
	}

	static String getVerifyCode(String apikeyBase) {
		long verifyCode = 0;

		for (char c: apikeyBase.toCharArray()) {
			if (verifyCode % 2 != 0) {
				verifyCode *= c;
			}
			else {
				verifyCode += c;
			}
		}

		return Long.toHexString(verifyCode).substring(0, 4);
	}

	public static boolean isValidApikey(String apikey) {
		if (apikey == null || apikey.length() != 20) {
			return false;
		}

		String apikeyLowered = apikey.toLowerCase();
		String verifyCode = apikeyLowered.substring(0, 4);
		String apikeyBase = apikeyLowered.substring(4);

		return verifyCode.equals(getVerifyCode(apikeyBase));
	}

	public long incCallCounter() {
		if (callCounter == null)
			callCounter = 0L;
		
		callCounter++;
		
		return callCounter;
	}
	
}
