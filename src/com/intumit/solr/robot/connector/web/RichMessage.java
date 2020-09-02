package com.intumit.solr.robot.connector.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.RobotImageFilePath;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.systemconfig.RobotImageFileConfig;

@Entity(name = "RichMessageWeb")
@Table(name = "RichMessageWeb")
/**
 * 存放 WEB 特殊訊息格式的地方（理論上只要不是純文字，就這邊處理）
 * 
 * @author herb
 */
public class RichMessage implements Serializable {
	
	public static final String imgPath = new StringBuilder().append("img").append(File.separator).append("webRM").toString();

	private static final long serialVersionUID = 5231594886007731728L;

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
	
	public static List<RichMessage> list(int tenantId, Boolean expired){
		return list(tenantId, expired, null);
	}
	
	@SuppressWarnings("unchecked")
	public static List<RichMessage> list(int tenantId, Boolean expired, String type) {
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
			
			if (type != null) {
				ct.add(Restrictions.eq("msgType", type));
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
	
	public static synchronized RichMessage save(Integer tenantId, String mkey, String msgName,
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
			return richMessage;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
		return null;
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
		String tmpl = msgTemplate;
		
		if (msgType.equals("quickReplies")) {
			tmpl = handleQuickReplies(ctx, tmpl);
		}
		
		tmpl = handleTemplateReplace(ctx, tmpl);
		
		return tmpl;
	}
	
	public String handleTemplateReplace(QAContext ctx, String tmpl) {
		tmpl = FunctionUtil.collectExecAndReplace(tmpl, ctx);
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

	public String handleQuickReplies(QAContext ctx, String tmpl) {
		JSONObject message = new JSONObject();
		try {
			message = new JSONObject(tmpl);
			// 先處理Actions
			JSONArray items = message.getJSONObject("quickReply").getJSONArray("items");
			int itemCount = items.size();
			for (int i = 0; i < itemCount; i++) {
				String item = items.getJSONObject(i).toString();
				items.getJSONObject(i).put("action", new JSONObject(item));
				items.getJSONObject(i).put("type", "action");
				
				items.getJSONObject(i).remove("label");
				items.getJSONObject(i).remove("text");
				items.getJSONObject(i).remove("displayText");
				items.getJSONObject(i).remove("data");
			}
			
			// 後處理type
			String type = message.getString("type");
			if ("text".equals(type)) {
				message.remove("template");
			} else if ("template".equals(type)) {
				message.remove("text");
				String tmplMkey = message.getString("template");
				RichMessage rm = RichMessage.getByMKey(ctx.getTenant().getId(), tmplMkey);
				if (rm != null) {
					JSONObject msgTemplate = new JSONObject(rm.getMsgTemplate());
					message.put("altText", msgTemplate.getString("altText"));
					if (rm.getMsgType().equals("imagemap")) {
						message.remove("template");
						message.put("type", "imagemap");
						message.put("baseUrl", msgTemplate.getString("baseUrl"));
						message.put("baseSize", msgTemplate.getJSONObject("baseSize"));
						message.put("actions", msgTemplate.getJSONArray("actions"));
					} else {
						message.put("template", msgTemplate.getJSONObject("template"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message.toString();
	}
	
	public static void createFolder(String sizeFolder) {
		Path pathDir = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
				.append(RichMessage.imgPath).append(File.separator).append(sizeFolder).toString());
		if (!Files.exists(pathDir)) {
			try {
				Files.createDirectories(Paths.get(pathDir.toFile().getCanonicalPath()));
			} catch (IOException e) {
				System.out.println("Error to create ../" + RobotImageFileConfig.getImageFileConfig()[0] + "/img/webRM/");
			}
		}
	}
	
}
