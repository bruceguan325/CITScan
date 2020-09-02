package com.intumit.solr.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.tenant.Tenant;

@Entity
@Table(name = "FrontEndUser")
public class User {

	public static final String SESSION_KEY = User.class.getName();

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	private String name;
	private String email;

	@Index(name="apikeyIdx")
	private String apikey;

	@Index(name="passwordIdx")
	private String password;

	@Column(length = 128)
	private String tenantIds;

	public User() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public void setTenantIds(String tenantIds) {
		this.tenantIds = tenantIds;
	}

	public String getTenantIds() {
		return tenantIds;
	}

	public Set<Integer> getTenantIdSet() {
		Set<Integer> set = new HashSet<Integer>();

		if (StringUtils.trimToNull(tenantIds) != null) {
			for (String idStr: StringUtils.split(tenantIds, ",")) {
				set.add(new Integer(idStr));
			}
		}
		return set;
	}

	public static List<User> list(){
		Session sess = null;
		Transaction tx = null;
		try{
			sess = HibernateUtil.getSession();
			tx = sess.beginTransaction();
			@SuppressWarnings("unchecked")
			List<User> list = sess.createCriteria(User.class)
				.addOrder(Order.asc("name"))
				.list();
			return list;
		}finally{
			if(tx != null){
				tx.commit();
			}
			if(sess != null){
				sess.close();
			}
		}
	}

	public static int save(String name){
		Session sess = null;
		Transaction tx = null;
		try{
			sess = HibernateUtil.getSession();
			tx = sess.beginTransaction();
			User u = new User();
			u.setName(name);
			sess.save(u);
			return u.getId();
		}finally{
			if(tx != null){
				tx.commit();
			}
			if(sess != null){
				sess.close();
			}
		}
	}

	public static int saveOrUpdate(User u){
		Session sess = null;
		Transaction tx = null;
		try{
			sess = HibernateUtil.getSession();
			tx = sess.beginTransaction();
			sess.saveOrUpdate(u);
			return u.getId();
		}finally{
			if(tx != null){
				tx.commit();
			}
			if(sess != null){
				sess.close();
			}
		}
	}

	public static User get(int id){
		Session sess = null;
		Transaction tx = null;
		try{
			sess = HibernateUtil.getSession();
			tx = sess.beginTransaction();
			return (User) sess.get(User.class, id);
		}finally{
			if(tx != null){
				tx.commit();
			}
			if(sess != null){
				sess.close();
			}
		}
	}

	public static User getByApiKey(String apikey) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(User.class);
			ct.add( Restrictions.eq("apikey", apikey ) );

			User phts = (User)ct.uniqueResult();
			return phts;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	public static void delete(int id){
		Session sess = null;
		Transaction tx = null;
		try{
			sess = HibernateUtil.getSession();
			tx = sess.beginTransaction();
			User u = new User();
			u.setId(id);
			sess.delete(u);
		}finally{
			if(tx != null){
				tx.commit();
			}
			if(sess != null){
				sess.close();
			}
		}
	}

	public static User login(String email, String password) {
		List<User> result = new ArrayList<User>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(User.class).addOrder(Order.asc("id"));
			ct.add( Restrictions.eq("email", email ) );
			ct.add( Restrictions.eq("password", password ) );
			@SuppressWarnings("unchecked")
			List<User> list = ct.list();
			result = list;
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		if (result.size() > 0) {
			return result.get(0);
		}
		return null;
	}

	public static User getFromSession(HttpSession sess){
		return (User) sess.getAttribute(SESSION_KEY);
	}

	public static void setToSession(HttpSession sess, User user){
		sess.setAttribute(SESSION_KEY, user);
	}


	public static String genApikey() {
		int length = 16;
		String hs = "";

		while (length > 0) {
			hs += StringUtils.leftPad(Integer.toHexString((int)(Math.random() * 65536)), 4, '0');
			length -= 4;
		}

		System.out.println("hs:" + hs + ":" + hs.length());

		return getVerifyCode(hs) + hs;
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

	public static class GuestUser extends User {

		public Set<Integer> getTenantIdSet() {
			Set<Integer> set = new HashSet<Integer>();

			List<Tenant> tlist = Tenant.list();
			for (Tenant t: tlist) {
				if (t.getEnableTenant())
					set.add(t.getId());
			}
			return set;
		}
	}
	public static boolean isLogin(String email,String password){
		Session ses = null;
		Transaction tx = null;
		List list=new ArrayList();
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria criteria = ses.createCriteria(User.class);
			criteria.add(Restrictions.or(Restrictions.eq("email", email), Restrictions.eq("name", email)));
			criteria.add(Restrictions.eq("password", password));
			list = criteria.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		if(list.size()!=0){
			return true;
		}
		return false;
	}
}
