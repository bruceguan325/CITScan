package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;

@Entity
public class QAUserType {
	public static final String DEFAULT_USERTYPE_CODE = "unknown";

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	@Index(name = "tenantId")
	private int tenantId;
	@Column(length = 20 , nullable = false , unique = true , columnDefinition="nvarchar(255)")  
	private String name;

	@Column(length = 64)
	@Index(name = "code")
	private String code;
	
	public QAUserType(){
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getTenantId() {
		return tenantId;
	}
	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@SuppressWarnings("unchecked")
	public static List<QAUserType> list(int tenantId) {
		List<QAUserType> result = new ArrayList();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			result = ses
					.createCriteria(QAUserType.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.addOrder(Order.asc("id")).list();
			return result;
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}
	
	public static void checkData(int tenantId){
		if (list(tenantId).size() == 0) {
			QAUserType defaultUserType = new QAUserType();
			defaultUserType.setName("standard.answer");
			defaultUserType.setCode("unknown");
			defaultUserType.setTenantId(tenantId);
			saveOrUpdate(defaultUserType);
			
			QAUserType vipUserType = new QAUserType();
			vipUserType.setName("vip.answer");
			vipUserType.setCode("vip");
			vipUserType.setTenantId(tenantId);
			saveOrUpdate(vipUserType);
		}
	}
	
	public static void saveOrUpdate(QAUserType userType) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(userType);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static void delete(QAUserType userType) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(userType);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static QAUserType get(int id){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses
					.createCriteria(QAUserType.class)
					.add(Restrictions.eq("id", id));
			List<QAUserType> userTypes = ct.list();
			if (userTypes.size()<1)
				return null;
			return userTypes.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static QAUserType get(int tenantId, String name, String code){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses
					.createCriteria(QAUserType.class)
					.add(Restrictions.eq("tenantId", tenantId));
			if (name!=null) {
				//ct.add(Restrictions.ilike("name", name, MatchMode.EXACT));
				ct.add(Restrictions.eq("name", name));
			}
			if (code!=null) {
				//ct.add(Restrictions.ilike("code", code, MatchMode.EXACT));
				ct.add(Restrictions.eq("code", code));
			}
			List<QAUserType> userTypes = ct.list();
			if (userTypes.size()<1)
				return null;
			return userTypes.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static ArrayList<Map<String, Object>> getArrayData(int tenantId, Locale locale) {
		List<QAUserType> utList = list(tenantId);
		ArrayList<Map<String, Object>> userTypeList = new ArrayList<Map<String, Object>>();
		
		for (QAUserType ut: utList)  {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put( "id", ut.getId() );
			data.put( "name", MessageUtil.returnKeyIfNotExist(locale, ut.getName()));
			data.put( "code", ut.getCode() );
			userTypeList.add(data);
		}
		return userTypeList;
	}
}
