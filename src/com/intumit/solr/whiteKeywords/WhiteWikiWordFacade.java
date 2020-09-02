package com.intumit.solr.whiteKeywords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.smartwiki.WikiWord;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;

public class WhiteWikiWordFacade {
	public static final String DEFAULT_FUNC = "default";
	static final WhiteWikiWordFacade instance = new WhiteWikiWordFacade();
	public static final int MAX_KEYWORD_LENGTH = 128;
	

	/**
	 * ?��? Singleton
	 * 
	 * @return
	 */
	public static WhiteWikiWordFacade getInstance() {
		return instance;
	}

	private WhiteWikiWordFacade() {
	}

	public synchronized WikiWord get(int id) {
		try {
			return (WikiWord)HibernateUtil.getSession().get(WikiWord.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	
		return null;
	}
	
	public synchronized void save(String keyword, String nature) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WikiWord word = new WikiWord();
			word.setPageTitle(keyword);
			word.setNature(nature);
			ses.saveOrUpdate(word);

			for (Tenant t: Tenant.list()) {
				try {
					QAUtil.getInstance(t.getId()).getAmbiguityForest().addBranch(word.getPageTitle(), new String[] {word.getPageTitle(), word.getNature()});
				}catch (Exception ignore) {}
			}
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	
	public synchronized void update(int id, String keyword, String nature) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WikiWord word = get(id);

			for (Tenant t: Tenant.list()) {
				try {
					Library.removeWord(QAUtil.getInstance(t.getId()).getAmbiguityForest(), word.getPageTitle());
				}catch (Exception ignore) {}
			}
			
			word.setPageTitle(keyword);
			word.setNature(nature);
			ses.update(word);

			for (Tenant t: Tenant.list()) {
				try {
					QAUtil.getInstance(t.getId()).getAmbiguityForest().addBranch(word.getPageTitle(), new String[] {word.getPageTitle(), word.getNature()});
				}catch (Exception ignore) {}
			}
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	
	public synchronized void delete(int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			
			WikiWord word = get(id);
			ses.createQuery("delete from " + WikiWord.class.getName() + " Where id=" + id)
					.executeUpdate();
			
			for (Tenant t: Tenant.list()) {
				try {
					Library.removeWord(QAUtil.getInstance(t.getId()).getAmbiguityForest(), word.getPageTitle());
				}catch (Exception ignore) {}
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	
	public synchronized List<String> whiteList() {
		List<String> sl = new ArrayList<String>();
		for (WikiWord ww: listSynonymKeywords()) {
			sl.add(ww.getPageTitle());
		}
		
		return sl;
	}
	public synchronized Set<String> whiteSet() {
		Set<String> sl = new HashSet<String>();
		for (WikiWord ww: listSynonymKeywords()) {
			sl.add(ww.getPageTitle());
		}
		
		return sl;
	}
	
	public synchronized List<WikiWord> listSynonymKeywords() {
		return list();
	}
	
	public synchronized List<WikiWord> listSynonymKeywords(String searchKeyword) {
		return list(searchKeyword);
	}
	
	@SuppressWarnings("unchecked")
	private List list() {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WikiWord.class);
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
	
	private List list(String searchKeyword) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WikiWord.class);
			Criterion c1 = (Restrictions.like("pagetitle","%" + searchKeyword + "%"));
			LogicalExpression orExp = Restrictions.or(c1,null);
			ct.add(orExp);
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
