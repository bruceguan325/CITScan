package com.intumit.solr.robot.connector.web;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity(name = "RichMenuWeb")
@Table(name = "RichMenuWeb")
/**
 * 存放 Web RichMenu的地方
 * 
 * @author dudamel
 */
public class RichMenu implements Serializable {
	
	private static final long serialVersionUID = 5663340321763274061L;

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(length = 32)
	@Index(name="mkeyIdx")
	String mkey;

	@Index(name = "tenantIdx")
	private Integer tenantId;
	
	private Integer menuSeq;

	@Column(length = 32)
	@Index(name = "channelCodeIdx")
	String channelCode;

	@Index(name = "updateTime")
	private Date updateTime;

	@Index(name = "createdTime")
	private Date createdTime;

	@Column(columnDefinition="varchar(200)")
	private String msgName;

	private String msgDesc;

	@Column(columnDefinition="varchar(32)")
	@Index(name = "msgTypeIdx")
	private String msgType;
	
	@Transient
	private String fixHeight;
	
	private boolean basicRichMenu;
	
	@Lob
	private String msgTemplate;

	public static synchronized RichMenu get(long id) {
		try {
			return (RichMenu)HibernateUtil.getSession().get(RichMenu.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static RichMenu getByMKey(Integer tenantId, String mkey) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RichMenu.class).add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("mkey", mkey));
			return (RichMenu) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static List<RichMenu> getByChannel(Integer tenantId, String channel, boolean enable) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RichMenu.class).add(Restrictions.eq("tenantId", tenantId));
			if (enable) {
				ct.add(Restrictions.eq("basicRichMenu", enable));
			}
			ct.add(Restrictions.eq("channelCode", channel)).addOrder(Order.asc("menuSeq")).addOrder(Order.asc("id"));
			return (List<RichMenu>) ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static List<RichMenu> list(int tenantId) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RichMenu.class).add(Restrictions.eq("tenantId", tenantId))
					.addOrder(Order.asc("channelCode")).addOrder(Order.asc("menuSeq"));
			return (List<RichMenu>) ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static boolean saveOrUpdate(RichMenu clue) {
		boolean success = false;
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Date now = Calendar.getInstance().getTime();
			clue.updateTime = now;
			
			if (clue.id == null || clue.createdTime == null) {
				clue.createdTime = now;
			}
			ses.saveOrUpdate(clue);
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
	
	public static synchronized RichMenu save(Integer tenantId, String mkey, String msgName,
		String msgDesc, String msgType, String msgTemplate, String channelCode, String menuSeq) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			RichMenu richMenu = new RichMenu();
			richMenu.setTenantId(tenantId);
			richMenu.setMkey(mkey);
			richMenu.setMsgName(msgName);
			richMenu.setMsgDesc(msgDesc);
			richMenu.setMsgType(msgType);
			richMenu.setMsgTemplate(msgTemplate);
			richMenu.setCreatedTime(new Date());
			richMenu.setUpdateTime(new Date());
			richMenu.setChannelCode(channelCode);
			richMenu.setMenuSeq(menuSeq != null && !menuSeq.isEmpty() ? Integer.valueOf(menuSeq) : 1);
			
			ses.saveOrUpdate(richMenu);
			tx.commit();
			return richMenu;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return null;
	}
	
    public static void delete(RichMenu msg) {
        Session ses = null;
        Transaction tx = null;
        try {
            ses = HibernateUtil.getSession();
            tx = ses.beginTransaction();
            ses.delete(msg);
            tx.commit();
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

	public String getMkey() {
		return mkey;
	}

	public void setMkey(String mkey) {
		this.mkey = mkey;
	}

	public Date getUpdateTime() {
		return updateTime;
	}
	
	public String getFormattedUpdateTime() {
		return new SimpleDateFormat(DATETIME_FORMAT).format(updateTime);
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getMsgName() {
		return msgName;
	}

	public void setMsgName(String msgName) {
		this.msgName = msgName;
	}

	public String getMsgDesc() {
		return msgDesc;
	}

	public void setMsgDesc(String msgDesc) {
		this.msgDesc = msgDesc;
	}
	
	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getMsgTemplate() {
		return msgTemplate;
	}

	public void setMsgTemplate(String msgTemplate) {
		this.msgTemplate = msgTemplate;
	}

	public Date getCreatedTime() {
		return createdTime;
	}
	
	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}
	
	public boolean isBasicRichMenu() {
		return basicRichMenu;
	}

	public void setBasicRichMenu(boolean basicRichMenu) {
		this.basicRichMenu = basicRichMenu;
	}
	
	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public String getFixHeight() {
		return fixHeight;
	}

	public void setFixHeight(String fixHeight) {
		this.fixHeight = fixHeight;
	}
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	
	public Integer getMenuSeq() {
		return menuSeq;
	}

	public void setMenuSeq(Integer menuSeq) {
		this.menuSeq = menuSeq;
	}

}
