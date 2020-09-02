package com.intumit.solr.tenant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.EventCenter;

@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = {"tenantId", "adminId", "groupId"})
	})
public class TenantAdminGroup implements Serializable {
	static final String SESSION_KEY = "_INTUMITTENANTADMGRP_";

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	Integer tenantId;
	Integer adminId;
	Integer groupId;
	
	public TenantAdminGroup() {
		super();
	}
	
	public TenantAdminGroup(Integer tenantId, Integer adminId, Integer groupId) {
		super();
		this.tenantId = tenantId;
		this.adminId = adminId;
		this.groupId = groupId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getAdminId() {
		return adminId;
	}

	public void setAdminId(Integer adminId) {
		this.adminId = adminId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	
	public AdminGroup getAdminGroup() {
		if (groupId != null) {
			return AdminGroupFacade.getInstance().get(groupId);
		}
		
		return null;
	}

	@Override
	public String toString() {
		return "TenantAdminGroup [id=" + id + ", " + (tenantId != null ? "tenantId=" + tenantId + ", " : "")
				+ (adminId != null ? "adminId=" + adminId + ", " : "") + (groupId != null ? "groupId=" + groupId : "") + "]";
	}

	public static synchronized int saveOrUpdate(TenantAdminGroup setting) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			TenantAdminGroup old = get(setting.getId());
			ses.saveOrUpdate(setting);
			tx.commit();
			
			if (old != null) {
				EventCenter.fireEvent(TenantAdminGroup.class.getName(), setting.getId(), "reload", null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return setting.getId();
	}

	public static synchronized TenantAdminGroup get(Integer id) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (TenantAdminGroup)ses.get(TenantAdminGroup.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}


	public static synchronized TenantAdminGroup getByTenantAndAdmin(Integer tenantId, Integer adminId) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TenantAdminGroup.class).addOrder(Order.asc("id"));
			ct.add( Restrictions.eq("tenantId", tenantId ) );
			ct.add( Restrictions.eq("adminId", adminId ) );
			return (TenantAdminGroup)ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static synchronized void delete(Integer id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			TenantAdminGroup phts = (TenantAdminGroup)ses.get(TenantAdminGroup.class, id);
			ses.delete(phts);
			tx.commit();
			
			EventCenter.fireEvent(TenantAdminGroup.class.getName(), phts.getId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}


	@SuppressWarnings("unchecked")
	public static List<TenantAdminGroup> listByTenant(int tenantId) {
		List<TenantAdminGroup> result = new ArrayList<TenantAdminGroup>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TenantAdminGroup.class).addOrder(Order.asc("id"));
			ct.add( Restrictions.eq("tenantId", tenantId ) );
			ct.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			result = ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static List<TenantAdminGroup> listByAdmin(int adminId) {
		List<TenantAdminGroup> result = new ArrayList<TenantAdminGroup>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(TenantAdminGroup.class).addOrder(Order.asc("id"));
			ct.add( Restrictions.eq("adminId", adminId ) );
			ct.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			result = ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static void checkTenantAndAdminThenSetSession(HttpSession sess) {
		AdminUser admin = AdminUserFacade.getInstance().getFromSession(sess);
		Tenant tenant = Tenant.getFromSession(sess);
		
		if (admin != null && tenant != null) {
			TenantAdminGroup tag = getByTenantAndAdmin(tenant.getId(), admin.getId());
			setSession(sess, tag);
		}
	}

	public static TenantAdminGroup getFromSession(HttpSession sess) {
		TenantAdminGroup tag = (TenantAdminGroup)sess.getAttribute(SESSION_KEY);
		return tag;
	}

	public static void setSession(HttpSession sess, TenantAdminGroup tag) {
		if (tag != null) {
			sess.setAttribute(SESSION_KEY, tag);
		}
		else if (sess.getAttribute(SESSION_KEY) != null) {
			sess.removeAttribute(SESSION_KEY);
		}
	}
}
