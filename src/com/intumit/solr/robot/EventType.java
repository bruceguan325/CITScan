package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.wivo.WiVoEntry;
import com.intumit.solr.tenant.Tenant;

@Entity
public class EventType implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	Integer id;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	@Column(length = 32)
	@Index(name = "codeIdx")
	String code;

	@Column(length = 32)
	@Index(name = "channelIdx")
	String channel;
	
	Boolean enabled;
	
	Boolean builtIn;

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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Boolean getEnabled() {
		return enabled != null ? enabled : Boolean.TRUE;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getBuiltIn() {
		return builtIn != null ? builtIn : Boolean.FALSE;
	}

	public void setBuiltIn(Boolean builtIn) {
		this.builtIn = builtIn;
	}
	

	public static synchronized EventType get(int id) {
		try {
			return (EventType)HibernateUtil.getSession().get(EventType.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}

    public static synchronized String save(int tenantId, String code, String channel, boolean builtIn, boolean enabled) {
		Session ses = null;
		Transaction tx = null;
        String result = "success";
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			EventType word = new EventType();
			word.setTenantId(tenantId);
			word.setChannel(channel);
			word.setCode(code);
			word.setEnabled(enabled);
			word.setBuiltIn(builtIn);
			ses.saveOrUpdate(word);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
            result = e.toString();
		} finally {
			ses.close();
		}
        return result;
	}
	public static synchronized String update(int tenantId, int id, String code, String channel, boolean builtIn, boolean enabled) {
		Session ses = null;
		Transaction tx = null;
		String result = "success";
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			EventType oldWord = get(id);
			EventType newWord = get(id);

			if (newWord.getTenantId().equals(tenantId)) {

				newWord.setId(id);
				newWord.setTenantId(tenantId);
				newWord.setChannel(channel);
				newWord.setCode(code);
				newWord.setEnabled(enabled);
				newWord.setBuiltIn(builtIn);

				ses.update(newWord);
				tx.commit();
			}
			else {
                result = "Intrusion detected: tenant[" + tenantId + "] trying to access EventType data of tenant[" + oldWord.getTenantId() + "]";
                System.out.println(result);
            }
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
            result = e.toString();
		} finally {
			ses.close();
		}
        return result;
	}

	public static synchronized void saveOrUpdate(EventType p) {
		Session ses = null;
		Transaction tx = null;
		try {
			if (p.getId() != null) {
				EventType oldWord = get(p.getId());

				if (!p.getTenantId().equals(oldWord.getTenantId())) {
					System.out.println("Intrusion detected: tenant[" + p.getTenantId() + "] trying to access EventType data of tenant[" + oldWord.getTenantId() + "]");
					return;
				}
			}
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(p);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

    public static synchronized String delete(int tenantId, int id) throws Exception {
		Session ses = null;
		Transaction tx = null;
        String result = "success";
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			EventType word = get(id);

			if (word != null && word.getTenantId().intValue() == tenantId) {
				ses.delete(word);
				tx.commit();
			}
			else {
                result = "Intrusion detected: tenant[" + tenantId + "] trying to access EventType data of tenant[" + word.getTenantId() + "]";
                System.out.println(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
            result = e.toString();
		} finally {
			ses.close();
		}
        return result;
	}

	public static void deleteAllByChannel(int tenantId, String channel) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			String hql = "DELETE from EventType where tenantId=:tenantId AND channel=:channel";
			ses.createQuery(hql).setInteger("tenantId", tenantId).setString("channel", channel).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static EventType matchBestEventType(int tenantId, String channel, String code) {
		EventType et = null;
		Session ses = null;
		try {
			List result = new ArrayList();
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(EventType.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("code"));

			ct.add(Restrictions.eq("tenantId", tenantId));
			ct.add(Restrictions.eq("code", code));

			if (channel != null) {
				ct.add(Restrictions.eq("channel", channel));
			}

			ct.add(Restrictions.eq("enabled", true));


			result = ct.list();
			if (result != null && result.size() > 0) {
				et = (EventType)result.get(0);
			}
			else if (channel != null) {
				et = matchBestEventType(tenantId, null, code);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return et;
	}
	
	public static synchronized List<EventType> listAll(Integer tenantId, Boolean builtIn) {
		return list(tenantId, null, null);
	}

	public static List<EventType> listByChannel(Integer tenantId, String channel, Boolean builtIn) {
		return listByChannel(tenantId, channel, builtIn, null);
	}

	private static List listByChannel(Integer tenantId, String channel, Boolean builtIn, Boolean enabled) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(EventType.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("code"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (channel != null) {
				ct.add(Restrictions.eq("channel", channel));
			}

			if (enabled != null) {
				ct.add(Restrictions.eq("enabled", enabled.booleanValue()));
			}

			if (builtIn != null) {
				ct.add(Restrictions.eq("builtIn", builtIn.booleanValue()));
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

	private static List list(Integer tenantId, Boolean builtIn, Boolean enabled) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(EventType.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("code"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (enabled != null) {
				ct.add(Restrictions.eq("enabled", enabled.booleanValue()));
			}

			if (builtIn != null) {
				ct.add(Restrictions.eq("builtIn", builtIn.booleanValue()));
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

	public static List<EventType> searchCodeAndChannel(Integer tenantId, String query, Boolean builtIn) {

		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(EventType.class)
					.addOrder(Order.asc("channel"))
					.addOrder(Order.asc("code"));

			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}

			if (query != null) {
				ct.add(
						Restrictions.or(
							Restrictions.like("code", query, MatchMode.ANYWHERE),
							Restrictions.like("channel", query, MatchMode.ANYWHERE)
						)
						);
			}

			if (builtIn != null) {
				ct.add(Restrictions.eq("builtIn", builtIn.booleanValue()));
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

	public static void initBuiltInTypes() {
		for (Tenant t: Tenant.list()) {
			List<EventType> builtIns = EventType.listAll(t.getId(), true);
			if (builtIns == null || builtIns.size() == 0) {
				initBuiltInTypes(t.getId(), null);
				
				for (QAChannel ch: QAChannel.list(t.getId())) {
					initBuiltInTypes(t.getId(), ch);
				}
			}
			else {
				// 內健全頻道的 postback 是新增的，需要檢查一下	
				boolean hasPostback = false;
				boolean hasPush = false;
				for (EventType et: builtIns) {
					if (StringUtils.isEmpty(et.getChannel()) && "postback".equals(et.getCode())) {
						hasPostback = true;
					}
					else if (StringUtils.isEmpty(et.getChannel()) && "push".equals(et.getCode())) {
						hasPush = true;
					}
				}
				
				if (!hasPostback) {
					EventType.save(t.getId(), "postback", "", true, true);
				}
				
				if (!hasPush) {
					EventType.save(t.getId(), "push", "", true, true);
				}
			}
		}
	}

	public static void initBuiltInTypes(int tenantId, QAChannel ch) {
		if (ch == null) {
			// null 代表建置通用 event
			EventType.save(tenantId, "inbound", "", true, true);
			EventType.save(tenantId, "hangup", "", true, true);
			EventType.save(tenantId, "message", "", true, true);
			EventType.save(tenantId, "push", "", true, true);
			EventType.save(tenantId, "postback", "", true, true);
		}
		else {
			switch (ch.getType()) {
				case LINE:
					EventType.save(tenantId, "follow", ch.getCode(), true, true);
					EventType.save(tenantId, "unfollow", ch.getCode(), true, true);
					EventType.save(tenantId, "postback", ch.getCode(), true, true);
					EventType.save(tenantId, "beacon", ch.getCode(), true, true);
					EventType.save(tenantId, "join", ch.getCode(), true, true);
					EventType.save(tenantId, "leave", ch.getCode(), true, true);
					
				break;
			}
		}
	}
}
