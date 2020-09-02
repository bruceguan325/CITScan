package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.tenant.Tenant;

import flexjson.JSONSerializer;

@Entity
public class QAAltTemplate {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Index(name="tenantIdIdx")
	Integer tenantId;

	@Column(length = 250)
	private String name;
	
	@Column(length = 8)
	@Index(name="mkeyIdIdx")
	String mkey;
	
	@Column(length = 512)
	private String description;
	
	@Lob
	private String template;
	
	@Lob
	private String suggestPatterns;
	
	private int priority;

	public QAAltTemplate() {
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
	public void setName(String column) {
		this.name = column;
	}
	public String getName() {
		return name;
	}
	public String getMkey() {
		return mkey;
	}
	public void setMkey(String mkey) {
		this.mkey = mkey;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	public List<String> splitTemplate() {
		return Arrays.asList(StringUtils.trimToEmpty(template).split("\r?\n"));
	}
	public List<String> splitAppliedTemplate(String[] keywords) {
		String str = StringUtils.trimToEmpty(template);
		
		for (int i=0; i < keywords.length; i++) {
			if (i == 0) {
				str = StringUtils.replace(str, "{{KEYWORD}}", keywords[i]);
			}
			str = StringUtils.replace(str, "{{KEYWORD" + (i+1) + "}}", keywords[i]);
		}
		return Arrays.asList(StringUtils.trimToEmpty(str).split("\r?\n"));
	}
	public String getSuggestPatterns() {
		return suggestPatterns;
	}
	public void setSuggestPatterns(String suggestPatterns) {
		this.suggestPatterns = suggestPatterns;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}


	public static synchronized List<QAAltTemplate> list(Integer tenantId) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAAltTemplate.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.addOrder(Order.asc("id"));
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
	public static synchronized QAAltTemplate get(int id) {
		try {
			return (QAAltTemplate)HibernateUtil.getSession().get(QAAltTemplate.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
    public static synchronized void save(Integer tenantId, String mkey, String name,
        String description, String template, String suggestPatterns, Integer priority) {
        Session ses = null;
        Transaction tx = null;
        try {
            ses = HibernateUtil.getSession();
            tx = ses.beginTransaction();
            QAAltTemplate altTemplate = new QAAltTemplate();
            altTemplate.setTenantId(tenantId);
            altTemplate.setName(name);
            altTemplate.setDescription(description);
            altTemplate.setMkey(mkey);
            altTemplate.setTemplate(template);
            altTemplate.setSuggestPatterns(suggestPatterns);
            altTemplate.setPriority(priority);
            ses.saveOrUpdate(altTemplate);
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
        } finally {
            ses.close();
        }
    }
	
	public static synchronized void delete(Integer tenantId, int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			QAAltTemplate p = get(id);
			if (p.getTenantId() != tenantId) return;
			
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.createQuery("delete from " + QAAltTemplate.class.getName() + " where id=" + id)
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	public static QAAltTemplate getByKey(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (QAAltTemplate)ses.createCriteria(QAAltTemplate.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("mkey", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	public static QAAltTemplate getByName(Integer tenantId, String name){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (QAAltTemplate)ses.createCriteria(QAAltTemplate.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("name", name)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	public static void deleteByName(Integer tenantId, String name) {
		Session ses = null;
		Transaction tx = null;
		QAAltTemplate cn = getByName(tenantId, name);
		if(cn==null) return;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	public static synchronized void saveOrUpdate(QAAltTemplate p) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}
	
	public static synchronized long updateEffectedQA(Tenant t, QAAltTemplate changed, String userName) {
		QAUtil qautil = QAUtil.getInstance(t);

		int start = 0;
		int rows = 100;
		long totalEffect = 0;
		
		do {
			SolrDocumentList docs = qautil.lowLevelSearchCommonSense("InheritantAltTemplateMkeys_ms", changed.getMkey(), start, rows);
			totalEffect = docs.getNumFound();
			if (start == 0) {
				System.out.println("QAAltTemplate saved, effected [" + totalEffect + "] docs, now start scheduling for recaluldate alts.");
			}
			
			for (SolrDocument doc: docs) {
				QA qa = new QA(doc);
				QAAltBuildQueue.add(t.getId(), qa.getId(), qa.getKid(), qa.getQuestionAltTemplates(), userName);
			}
			
			start += rows;
		}
		while (totalEffect > start);
		
		return totalEffect;
	}
}
