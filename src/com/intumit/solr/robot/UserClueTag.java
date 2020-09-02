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
import com.intumit.solr.robot.entity.QAEntity;

@Entity
/**
 * 存放使用者被貼上的標籤，多對多
 * 
 * @author herb
 */
public class UserClueTag {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name = "tenantId")
	private Integer tenantId;
	
	@Index(name = "userClueId")
	private Long userClueId;
	
	@Column(columnDefinition="varchar(200)")
	@Index(name = "tag")
	private String tag;
	
	@Column(columnDefinition="varchar(200)")
	private String additionalInfo;

	@Index(name = "tagTime")
	private Date tagTime;
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public static List<UserClueTag> getByUserClueId(long userClueId) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(UserClueTag.class)
				.add(Restrictions.eq("userClueId", userClueId));
			return (List<UserClueTag>) ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static int clearTagOnUser(UserClue clue, String tagName) {
		List<UserClueTag> tags = findUserClueTags(clue, tagName);
		int dc = 0;
		
		if (tags != null) {
			for (UserClueTag tag: tags) {
				try {
					delete(clue.getTenantId(), tag.getId());
					dc++;
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return dc;
	}
	
	public static UserClueTag addTagToUser(UserClue clue, String tag) {
		return addTagToUser(clue, tag, null);
	}
	
	public static UserClueTag addTagToUser(UserClue clue, String tag, String additionalInfo) {
		UserClueTag ut = new UserClueTag();
		ut.setTenantId(clue.getTenantId());
		ut.setUserClueId(clue.getId());
		ut.setTag(tag);
		ut.setTagTime(Calendar.getInstance().getTime());
		ut.setAdditionalInfo(additionalInfo);
		UserClueTag.saveOrUpdate(ut);
		return ut;
	}
	
	public static List<UserClueTag> findUserClueTags(UserClue clue, String tag) {
		List<UserClueTag> list = null;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(UserClueTag.class)
				.add(Restrictions.eq("tenantId", clue.getTenantId()))
				.add(Restrictions.eq("userClueId", clue.getId()))
				.add(Restrictions.eq("tag", tag));
			list = (List<UserClueTag>)ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return list;
	}
	
	public static synchronized UserClueTag get(long id) {
		try {
			return (UserClueTag)HibernateUtil.getSession().get(UserClueTag.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static boolean saveOrUpdate(UserClueTag ut) {
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

	public static synchronized void delete(Integer tenantId, long id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			UserClueTag tag = get(id);

			if (tag != null) {
				ses.delete(tag);
				tx.commit();
				
				EventCenter.fireEvent(UserClueTag.class.getName(), tenantId, "reload", null);
			}
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

	public Long getUserClueId() {
		return userClueId;
	}

	public void setUserClueId(Long userClueId) {
		this.userClueId = userClueId;
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

	public Date getTagTime() {
		return tagTime;
	}

	public void setTagTime(Date tagTime) {
		this.tagTime = tagTime;
	}


}
