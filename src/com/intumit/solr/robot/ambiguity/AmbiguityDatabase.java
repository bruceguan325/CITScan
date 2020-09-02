package com.intumit.solr.robot.ambiguity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.StrUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

@Entity
public class AmbiguityDatabase implements Serializable {
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	@Index(name="tenantIdIdx")
	private Integer tenantId;
	
	@Index(name="sentence")
	private String sentence;
	
	@Index(name="synonymKeyword")
	private String disambiguation;
	
	@Index(name="enabled")
	private boolean enabled;
	
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
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String keyword) {
		this.sentence = keyword;
	}
	public String getDisambiguation() {
		return StringUtils.strip(disambiguation, ",");
	}
	public String[] getDisambiuationParts() {
		return StringUtils.splitPreserveAllTokens(getDisambiguation(), '\t');
	}
	public void setDisambiguation(String synonymKeyword) {
		this.disambiguation = synonymKeyword;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean reverse) {
		this.enabled = reverse;
	}
	@Override
	public String toString() {
		return "DictionaryDatabase [id=" + id + ", sentence=" + sentence + ", disambiguation=" + disambiguation
				+ ", enabled=" + enabled + "]";
	}

	public static synchronized AmbiguityDatabase get(long id) {
		try {
			return (AmbiguityDatabase)HibernateUtil.getSession().get(AmbiguityDatabase.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static synchronized void save(int tenantId, String sentence, String disambiguation,
			boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			AmbiguityDatabase word = new AmbiguityDatabase();
			word.setTenantId(tenantId);
			word.setSentence(sentence.toLowerCase());
			word.setDisambiguation(disambiguation.toLowerCase());
			word.setEnabled(enabled);
			ses.saveOrUpdate(word);
			
			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			if (enabled && !whiteSet.contains(word.getSentence())) {
				QAUtil.getInstance(tenantId).getAmbiguityForest().addBranch(word.getSentence(), word.getDisambiuationParts());
			}
			
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}

	public static synchronized void update(int tenantId, String id, String sentence,
			String disambiguation, boolean enabled) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			long lid = Long.parseLong(id);
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			AmbiguityDatabase word = get(lid);
			
			if (word.getTenantId() != tenantId) {
				return;
			}

			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			
			if (!whiteSet.contains(word.getSentence())) {
				Library.removeWord(QAUtil.getInstance(tenantId).getAmbiguityForest(), word.getSentence());
			}
			
			word.setId(lid);
			word.setSentence(sentence.toLowerCase());
			word.setDisambiguation(disambiguation.toLowerCase());
			word.setEnabled(enabled);
			ses.update(word);

			if (enabled && !whiteSet.contains(word.getSentence()))				
				QAUtil.getInstance(tenantId).getAmbiguityForest().addBranch(word.getSentence(), word.getDisambiuationParts());
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(int tenantId, String id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			long lid = Long.parseLong(id);
			AmbiguityDatabase word = get(lid);
			
			if (word.getTenantId() != tenantId) {
				return;
			}
			
			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			
			if (!whiteSet.contains(word.getSentence())) {
				Library.removeWord(QAUtil.getInstance(tenantId).getAmbiguityForest(), word.getSentence());
			}
			
			Query q = ses.createQuery("delete from " + AmbiguityDatabase.class.getName() + " Where id = :id");
			q.setParameter("id", word.getId());
			q.executeUpdate();
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}
	
	public static synchronized AmbiguityDatabase get(int tenantId, String sentence) {
		List<AmbiguityDatabase> l = list(tenantId, sentence, true, null);
		
		if (l.size() > 0) {
			return l.get(0);
		}
		
		return null;
	}

	public static synchronized List<AmbiguityDatabase> listAll(int tenantId) {
		return list(tenantId, null, false, null);
	}

	public static synchronized List<AmbiguityDatabase> search(int tenantId, String searchKeyword) {
		return list(tenantId, searchKeyword, false, null);
	}

	/**
	 * 
	 * @param searchKeyword
	 * @param enabled, null if match all
	 * @return
	 */
	public static synchronized List<AmbiguityDatabase> listSynonymKeywords(int tenantId, String searchKeyword, boolean enabled) {
		return list(tenantId, searchKeyword, false, enabled);
	}

	private static List list(Integer tenantId, String query, boolean fullMatch, Boolean enabled) {
		String searchKeyword = StringUtils.lowerCase(query);
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(AmbiguityDatabase.class);

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			
			if (query != null) {
				ct.add(fullMatch 
								? Restrictions.eq("sentence", searchKeyword)
								: Restrictions.like("sentence", "%" + searchKeyword + "%")
							);
			}
			
			if (enabled != null) {
				ct.add(Restrictions.and(
							fullMatch 
									? Restrictions.eq("sentence", searchKeyword)
									: Restrictions.like("sentence", "%" + searchKeyword + "%")
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
}
