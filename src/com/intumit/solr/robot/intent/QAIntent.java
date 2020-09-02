package com.intumit.solr.robot.intent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.wivo.WiVoEntry;

@Entity
@Table(name = "IntentDatabase")
public class QAIntent implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Index(name="category")
	private String category;

	@Index(name="tag")
	private String tag;

	@Index(name="keyword")
	@Lob
	private String keywords;

	@Index(name="enabled")
	private boolean enabled;
	
	@Index(name="altTemplates")
	@Lob
	private String altTemplates;


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	public String getKeywords() {
		return keywords;
	}
	public void setKeywords(String keyword) {
		this.keywords = StringUtils.trim(keyword);
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = StringUtils.trim(category);
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = StringUtils.trim(tag);
	}
	public String getAltTemplates() {
		return altTemplates;
	}
	public void setAltTemplates(String altTemplates) {
		this.altTemplates = altTemplates;
	}	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	public boolean isSystemBuiltIn() {
		return StringUtils.startsWith(tag, "_");
	}
	@Override
	public String toString() {
		return "Intent [id=" + id + ", keywords=" + keywords
				+ ", enabled=" + enabled + ", category=" + category + ", tag=" + tag + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QAIntent other = (QAIntent) obj;
		if (enabled != other.enabled) return false;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (keywords == null) {
			if (other.keywords != null) return false;
		}
		else if (!keywords.equals(other.keywords)) return false;
		if (category == null) {
			if (other.category != null) return false;
		}
		else if (!category.equals(other.category)) return false;
		return true;
	}
	public QAIntent getCopy() {
		QAIntent newOne = new QAIntent();
		newOne.id = id;
		newOne.category = category;
		newOne.keywords = keywords;
		newOne.tag = tag;
		newOne.enabled = enabled;
		return newOne;
	}
	
	public static synchronized QAIntent get(long id) {
		try {
			return (QAIntent)HibernateUtil.getSession().get(QAIntent.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

	public static synchronized void save(Integer tenantId, String sentence, boolean enabled, String category, String tag) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAIntent word = new QAIntent();
			word.setTenantId(tenantId);
			word.setTag(tag);
			word.setKeywords(sentence.toLowerCase());
			word.setEnabled(enabled);
			word.setCategory(category);
			ses.saveOrUpdate(word);
			tx.commit();

			EventCenter.fireEvent(QAIntent.class.getName(), tenantId, "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void save(Integer tenantId, String keywords, boolean enabled, String category, String tag, String altTemplates) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAIntent word = new QAIntent();
			word.setTenantId(tenantId);
			word.setTag(tag);
			word.setKeywords(keywords.toLowerCase());
			word.setEnabled(enabled);
			word.setCategory(category);
			word.setAltTemplates(altTemplates);
			ses.save(word);
			tx.commit();

			EventCenter.fireEvent(QAIntent.class.getName(), tenantId, "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}
	
	public static synchronized void update(Integer tenantId, String id, String sentence,
			boolean enabled, String category, String tag) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			QAIntent oldWord = get(lid);
			QAIntent newWord = get(lid);

			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(lid);
				newWord.setTag(tag);
				newWord.setKeywords(sentence.toLowerCase());
				newWord.setEnabled(enabled);
				newWord.setCategory(category);

				ses.update(newWord);
				tx.commit();
				
				EventCenter.fireEvent(QAIntent.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void saveOrUpdate(QAIntent p) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
			
			EventCenter.fireEvent(QAIntent.class.getName(), p.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(Integer tenantId, String id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			long lid = Long.parseLong(id);
			QAIntent word = get(lid);

			if (word != null) {
				ses.delete(word);
				tx.commit();
				
				EventCenter.fireEvent(QAIntent.class.getName(), tenantId, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static synchronized QAIntent get(Integer tenantId, String tag, String category) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(QAIntent.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("tag"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (tag != null) {
				ct.add(Restrictions.eq("tag", tag));
			}

			if (category != null) {
				ct.add(Restrictions.eq("category", category));
			}

			return (QAIntent)ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static synchronized QAIntent get(Integer tenantId, String sentence) {
		List<QAIntent> l = list(tenantId, sentence, true, null);

		if (l.size() > 0) {
			return l.get(0);
		}

		return null;
	}

	public static synchronized List<QAIntent> listAll() {
		return list(null, null, false, null);
	}

	public static List<QAIntent> listByTenantId(Integer tenantId) {
		return list(tenantId, null, false, null);
	}

	public static List<QAIntent> listByTenantIdQaCategory(Integer tenantId, String qaCategory) {
		return listByQaCategory(tenantId, qaCategory, false, true);
	}

	public static synchronized List<QAIntent> search(Integer tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, null);
	}

	private static List listByQaCategory(Integer tenantId, String qaCategory, boolean fullMatch, Boolean enabled) {
		String searchCategory = StringUtils.lowerCase(qaCategory);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAIntent.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("tag"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (qaCategory != null) {
				ct.add(fullMatch
								? Restrictions.eq("category", searchCategory)
								: Restrictions.like("category", searchCategory, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("category", searchCategory)
									: Restrictions.like("category", searchCategory, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
			}
			else {
			}

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

	private static List list(Integer tenantId, String query, boolean fullMatch, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(QAIntent.class)
					.addOrder(Order.asc("category"))
					.addOrder(Order.asc("tag"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(fullMatch
								? Restrictions.eq("keywords", searchKeyword)
								: Restrictions.like("keywords", searchKeyword, MatchMode.ANYWHERE)
							);
			}

			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch
									? Restrictions.eq("keywords", searchKeyword)
									: Restrictions.like("keywords", searchKeyword, MatchMode.ANYWHERE)
							, Restrictions.eq("enabled", enabled.booleanValue())
							));
			}
			else {
			}

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
	
	public static Map<String, QAIntent> collToMap(Collection<QAIntent> l) {
		Map<String, QAIntent> m = new HashMap<>();
		
		if (l != null)
		for (QAIntent i: l) {
			m.put(i.getTag(), i);
		}
		
		return m;
	}
}
