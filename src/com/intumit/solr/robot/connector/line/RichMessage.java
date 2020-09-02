package com.intumit.solr.robot.connector.line;

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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.NaverLineAnswerTransformer;
import com.intumit.solr.robot.NaverLineAnswerTransformer.TagLGQReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.util.WiSeUtils;

@Entity
/**
 * 存放 LINE 特殊訊息格式的地方（理論上只要不是純文字，就這邊處理）
 * 
 * @author herb
 */
public class RichMessage implements Serializable {
	private static final long serialVersionUID = 5663340321763274061L;

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(length = 32)
	@Index(name="mkeyIdIdx")
	String mkey;

	@Index(name = "tenantId")
	private Integer tenantId;
	
	@Index(name = "updateTime")
	private Date updateTime;

	@Index(name = "createdTime")
	private Date createdTime;
	
	@Index(name = "expireTime")
	private Date expireTime;

	@Column(columnDefinition="varchar(200)")
	private String msgName;

	private String msgDesc;

	@Column(columnDefinition="varchar(32)")
	private String msgType;
	
	@Lob
	private String msgTemplate;
	
	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public static synchronized RichMessage get(long id) {
		try {
			return (RichMessage)HibernateUtil.getSession().get(RichMessage.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		return null;
	}
	
	public static RichMessage getByMKey(Integer tenantId, String mkey) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RichMessage.class).add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("mkey", mkey));
			return (RichMessage) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}
	
	public static List<RichMessage> list(int tenantId, Boolean expired) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RichMessage.class)
				.add(Restrictions.eq("tenantId", tenantId));
			
			if (expired != null) {
				if (expired) {
					ct.add(Restrictions.lt("expireTime", Calendar.getInstance().getTime()));
				}
				else {
					ct.add(Restrictions.or(
							Restrictions.isNull("expireTime"),
							Restrictions.ge("expireTime", Calendar.getInstance().getTime())
						));
				}
			}
			
			return (List<RichMessage>) ct.list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static boolean saveOrUpdate(RichMessage clue) {
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
	
	public static synchronized void save(Integer tenantId, String mkey, String msgName,
		String msgDesc, String msgType, String msgTemplate) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			RichMessage richMessage = new RichMessage();
			richMessage.setTenantId(tenantId);
			richMessage.setMkey(mkey);
			richMessage.setMsgName(msgName);
			richMessage.setMsgDesc(msgDesc);
			richMessage.setMsgType(msgType);
			richMessage.setMsgTemplate(msgTemplate);
			richMessage.setCreatedTime(new Date());
			richMessage.setUpdateTime(new Date());
			ses.saveOrUpdate(richMessage);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
    public static void delete(RichMessage msg) {
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

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
	
	public boolean isExpired() {
		if (expireTime == null)
			return false;
		
		return expireTime.before(Calendar.getInstance().getTime());
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

	/**
	 * 套用制式文案及 inline code
	 * @param ctx
	 * @return
	 */
	public String getMsgTemplate(QAContext ctx) {
		String tmpl =  msgTemplate;
		tmpl = FunctionUtil.collectExecAndReplace(tmpl, ctx);

		TagLGQReplacer tagLGQ = new NaverLineAnswerTransformer().new TagLGQReplacer(ctx);
		tmpl = TemplateUtil.process(ctx, tmpl, tagLGQ);
		
		FormalAnswerReplacer far = ctx.getQAUtil().getFormalAnswerReplacer(ctx);
		tmpl = TemplateUtil.processTwice(ctx, tmpl, far);
		tmpl = TemplateUtil.process(ctx, tmpl, tagLGQ);
		
		// 因為內容可能要換行(\n)，但存入DB會因為被轉成unicode而多了一個「\」
		tmpl = tmpl.replace("\\\\n", "\\n");
		return tmpl;
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

}
