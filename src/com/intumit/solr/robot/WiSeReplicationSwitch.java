package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
@Table(name = "WiSeReplicationSwitch")
public class WiSeReplicationSwitch {
	
	public static final String MASTER = "master";
	
	public static final String SLAVE = "slave";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@Index(name = "host")
	String host;

	@Index(name = "port")
	String port;

	@Index(name = "replicationStauts")
	String replicationStauts;
	
	@Index(name = "groupId")
	Integer groupId;

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String getReplicationStauts() {
		return replicationStauts;
	}

	public void setReplicationStauts(String replicationStauts) {
		this.replicationStauts = replicationStauts;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public static List<WiSeReplicationSwitch> listNodes(String host, String status) {
		List<WiSeReplicationSwitch> result = new ArrayList<WiSeReplicationSwitch>();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WiSeReplicationSwitch.class).addOrder(Order.asc("groupId"));
			if (host != null) {
				ct.add(Restrictions.eq("host", host));
			}
			if (status != null) {
				ct.add(Restrictions.eq("replicationStauts", status));
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

	public static synchronized void saveOrUpdate(WiSeReplicationSwitch replication) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(replication);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}

	public static synchronized void delete(Integer id) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			WiSeReplicationSwitch replication = get(id);

			if (replication != null) {
				ses.delete(replication);
				tx.commit();

				EventCenter.fireEvent(WiSeReplicationSwitch.class.getName(), 0, "reload", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			throw new Exception(e);
		} finally {
			ses.close();
		}
	}

	public static synchronized WiSeReplicationSwitch get(int id) {
		try {
			return (WiSeReplicationSwitch) HibernateUtil.getSession().get(WiSeReplicationSwitch.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

}
