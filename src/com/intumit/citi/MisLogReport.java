package com.intumit.citi;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.tenant.Tenant;

@Entity
public class MisLogReport implements Serializable  {
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(nullable = false)
	private int count;
	
	@Column(nullable = false)
	private int noAnswerCount;
	
	@Column(length = 32)
	@Index(name="kid")
	private String kid;
	
	//@Column(length = 64)
	//@Index(name="question")
	private String question;

	@Column(length = 32)
	@Index(name = "channel")
	private String channel;
	
	@Column(length = 32)
	@Index(name = "action")
	private String action;
	
	@Column(length = 32)
	@Index(name = "currentCategory")
	private String currentCategory;
	
	@Column(length = 32)
	@Index(name = "father")
	private String father;
	
	@Column(length = 32)
	@Index(name = "upLevel")
	private String upLevel;
	
	@Column(length = 128)
    @Index(name = "sessionIdIdx")
    private String sessionId;
	
	private boolean menu;
	
	@Column(nullable = false)
	private Date createTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getNoAnswerCount() {
		return noAnswerCount;
	}

	public void setNoAnswerCount(int noAnswerCount) {
		this.noAnswerCount = noAnswerCount;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getCurrentCategory() {
		return currentCategory;
	}

	public void setCurrentCategory(String currentCategory) {
		this.currentCategory = currentCategory;
	}

	public String getFather() {
		return father;
	}

	public void setFather(String father) {
		this.father = father;
	}

	public boolean isMenu() {
		return menu;
	}

	public void setMenu(boolean menu) {
		this.menu = menu;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getKid() {
		return kid;
	}

	public void setKid(String kid) {
		this.kid = kid;
	}
	
	public static synchronized void save(MisLogReport log) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(log);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static synchronized List<Object[]> getCountByKid(String channel, String branch, String after, String before, 
			String kid, String action, boolean menu) {
		List<Object[]> result = new ArrayList<Object[]>();
		Session ses = null;
		int menuInt = 0;
		String hql = "";
		try {
			// 主選單
			if (branch != null && branch.equals("FaF")) {
				menuInt = 1;
				ses = HibernateUtil.getSession();
				hql = "select createTime as date, count(*) as count, question as q, menu as m "  
							+ " from MisLogReport WHERE " 
							+ " channel = '" + channel + "'"
							+ " and kid = '" + kid + "'"
							+ " and action = '" + action + "'"
							+ " and menu = '" + menuInt + "'"
							+ " and createTime >= '" + after + "'"
							+ " and createTime <= '" + before + "'"
						    + " GROUP BY createTime, question, menu " 
							+ " order by date asc ";
			// 子選單
			} else if (branch != null && branch.equals("FaS")) {
				menuInt = 0;
				ses = HibernateUtil.getSession();
				hql = "select createTime as date, count(*) as count, question as q, father as f "  
							+ " from MisLogReport WHERE "
							+ " channel = '" + channel + "'"
							+ " and kid = '" + kid + "'"
							+ " and action = '" + action + "'"
							+ " and menu = '" + menuInt + "'"
							+ " and father != 0"
							+ " and createTime >= '" + after + "'"
							+ " and createTime <= '" + before + "'"
							+ " GROUP BY createTime, question, father " 
							+ " order by date asc ";
			// 純輸入
			} else if (branch != null && branch.equals("EbY")) {
				menuInt = 0;
				ses = HibernateUtil.getSession();
				hql = "select createTime as date, count(*) as count, question as q, currentCategory as c "  
						+ " from MisLogReport WHERE "
						+ " channel = '" + channel + "'"
						+ " and kid = '" + kid + "'"
						+ " and action = '" + action + "'"
						+ " and menu = '" + menuInt + "'"
						+ " and noAnswerCount != 1 "
						+ " and createTime >= '" + after + "'"
						+ " and createTime <= '" + before + "'"
						+ " GROUP BY createTime, question, currentCategory " 
						+ " order by date asc ";
			// 純點擊(現階段不會用到)
			} else if (branch != null && branch.equals("CkO")) {
				menuInt = 0;
				ses = HibernateUtil.getSession();
				hql = "select createTime as date, count(*) as count, question as q, currentCategory as c "  
						+ " from MisLogReport WHERE "
						+ " channel = '" + channel + "'"
						+ " and kid = '" + kid + "'"
						+ " and action = '" + action + "'"
						+ " and menu = '" + menuInt + "'"
						+ " and noAnswerCount != 1 "
						+ " and createTime >= '" + after + "'"
						+ " and createTime <= '" + before + "'"
						+ " GROUP BY createTime, question, currentCategory " 
						+ " order by date asc ";
			// No_Answer 次數
			} else if (branch != null && branch.equals("NoA")) {
				menuInt = 0;
				ses = HibernateUtil.getSession();
				hql = " select createTime as date, count(*) as count "
						+ " from MisLogReport WHERE "
						+ " channel = '" + channel + "'"
						+ " and action = '" + action + "'"
						+ " and menu = '" + menuInt + "'"
						+ " and currentCategory ='' "
						+ " and kid = '' "
						+ " and noAnswerCount = 1 "
						+ " and createTime >= '" + after + "'"
						+ " and createTime <= '" + before + "'"
						+ " GROUP BY createTime order by date asc ";  
			}		
			Query query = ses.createQuery(hql);
			result = query.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static String getStandQ(String kid, Tenant t) {
		String standQ = null;
		try {
			SolrDocument doc = null;
			String id = "COMMON_SENSE-" + kid;
			SolrQuery q = new SolrQuery();
			q.setQuery("id:\"" + URLEncoder.encode(id, "UTF-8") + "\"");
			SolrServer server = t.getCoreServer4Write();
			SolrDocumentList result = server.query(q).getResults();

			if (result.getNumFound() != 0) {
				doc = result.get(0);
				standQ = doc.getFieldValue("QUESTION_s") != null ? doc.getFieldValue("QUESTION_s").toString() : "";
			}
			return standQ;
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	public static synchronized List<MisLogReport> listByDate(String channel, Date after, Date before, String kid, String action) {
		List<MisLogReport> result = new ArrayList<MisLogReport>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(MisLogReport.class);
			
			ct.add(Restrictions.eq("channel", channel));
			ct.add(Restrictions.eq("kid", kid));
			ct.add(Restrictions.eq("action", action));
			
			if (after != null && before != null) {
				ct.add(
						Restrictions.and(
								Restrictions.ge("createTime", after),
								Restrictions.lt("createTime", before)
								));
			}

			result = ct.list();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	public static synchronized List<String> getKidList(String channel, String branch) {
		List<String> result = new ArrayList<String>();
		List<Integer> sortKid = new ArrayList<Integer>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(MisLogReport.class);		
			if (channel != null) {
				ct.add(Restrictions.eq("channel", channel));
			}
			ct.setProjection(Projections.distinct(Projections.property("kid")));		
			result = ct.list();
			if (!branch.equals("NoA")) {
				result.removeAll(Arrays.asList("", null));
				 for(String counter: result){
					 sortKid.add(Integer.parseInt(counter));
				 }
				Collections.sort(sortKid);
				result = Lists.transform(sortKid, Functions.toStringFunction()) ;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getUpLevel() {
		return upLevel;
	}

	public void setUpLevel(String upLevel) {
		this.upLevel = upLevel;
	}
}
