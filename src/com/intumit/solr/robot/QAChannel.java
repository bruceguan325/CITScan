package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.elasticsearch.common.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

@Entity
public class QAChannel {
	public static final String DEFAULT_CHANNEL_CODE = "web";

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	@Index(name = "tenantId")
	private int tenantId;
	private String name;

	@Column(length = 64)	
	@Index(name = "code")
	private String code;

	@Index(name = "defaultChannelCode")
	private String defaultChannelCode;		// 找不到 QA 時，找哪一個 channel 的答案
	
	private Boolean appendOptionToOutput;
	private Boolean returnOptionInJson;
	private Boolean useHtmlNewline;
	private Boolean supportMultiRichMessages;
	private Boolean useCustomMatchCtrlFlow; // 是否要使用自訂的 matchCtrlFlow
	
	@Enumerated(EnumType.STRING)
	private QAChannelType type;
	
	@Lob
	private String qaMatchCtrlFlow;
	
	public QAChannel(){
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
	
	public Boolean getAppendOptionToOutput() {
		return appendOptionToOutput != null ? appendOptionToOutput : Boolean.TRUE;
	}

	public void setAppendOptionToOutput(Boolean appendOptionToOutput) {
		this.appendOptionToOutput = appendOptionToOutput;
	}

	public Boolean getReturnOptionInJson() {
		return returnOptionInJson != null ? returnOptionInJson : Boolean.FALSE;
	}

	public void setReturnOptionInJson(Boolean returnOptionInJson) {
		this.returnOptionInJson = returnOptionInJson;
	}

	public Boolean getUseHtmlNewline() {
		return useHtmlNewline != null ? useHtmlNewline : Boolean.FALSE;
	}

	public void setUseHtmlNewline(Boolean useHtmlNewline) {
		this.useHtmlNewline = useHtmlNewline;
	}

	public Boolean getUseCustomMatchCtrlFlow() {
		return useCustomMatchCtrlFlow != null ? useCustomMatchCtrlFlow : Boolean.FALSE;
	}

	public void setUseCustomMatchCtrlFlow(Boolean useCustomMatchCtrlFlow) {
		this.useCustomMatchCtrlFlow = useCustomMatchCtrlFlow;
	}

	public QAChannelType getType() {
		return type != null ? type : QAChannelType.PLAIN_TEXT;  // 預設 Plain text 保持相容性
	}

	public void setType(QAChannelType type) {
		this.type = type;
	}

	public Boolean getSupportMultiRichMessages() {
		if (supportMultiRichMessages != null) return supportMultiRichMessages;
		
		if (getType() == QAChannelType.LINE || getType() == QAChannelType.FACEBOOK_MESSENGER) {
			return Boolean.TRUE;
		}
		
		return Boolean.FALSE;
	}

	public void setSupportMultiRichMessages(Boolean supportMultiRichMessages) {
		this.supportMultiRichMessages = supportMultiRichMessages;
	}

	public String getDefaultChannelCode() {
		return defaultChannelCode != null ? defaultChannelCode : QAChannel.DEFAULT_CHANNEL_CODE;
	}

	public void setDefaultChannelCode(String defaultChannelCode) {
		this.defaultChannelCode = defaultChannelCode;
	}

	public String getQaMatchCtrlFlow() {
		return qaMatchCtrlFlow;
	}

	public void setQaMatchCtrlFlow(String qaMatchCtrlFlow) {
		this.qaMatchCtrlFlow = qaMatchCtrlFlow;
	}

	@SuppressWarnings("unchecked")
	public static List<QAChannel> list(int tenantId) {
		List<QAChannel> result = new ArrayList();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			result = ses
					.createCriteria(QAChannel.class)
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
			QAChannel defaultChannel = new QAChannel();
			defaultChannel.setName("DEFAULT");
			defaultChannel.setCode("web");
			defaultChannel.setType(QAChannelType.PLAIN_TEXT);
			defaultChannel.setTenantId(tenantId);
			saveOrUpdate(defaultChannel);
			
			QAChannel appChannel = new QAChannel();
			appChannel.setName("APP");
			appChannel.setCode("app");
			defaultChannel.setType(QAChannelType.PLAIN_TEXT);
			appChannel.setTenantId(tenantId);
			saveOrUpdate(appChannel);
			
			QAChannel voiceChannel = new QAChannel();
			voiceChannel.setName("VOICE");
			voiceChannel.setCode("voice");
			defaultChannel.setType(QAChannelType.PLAIN_TEXT);
			voiceChannel.setTenantId(tenantId);
			saveOrUpdate(voiceChannel);
		}
	}
	
	public static void saveOrUpdate(QAChannel channel) {
		Session ses = null;
		Transaction tx = null;
		try {
			boolean initBuiltInEventTypes = channel.getId() <= 0;
			
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(channel);
			tx.commit();
			
			if (initBuiltInEventTypes) {
				EventType.initBuiltInTypes(channel.getTenantId(), channel);
			}

			EventCenter.fireEvent(QAChannel.class.getName(), channel.getTenantId(), "reload", null);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	public static void delete(QAChannel channel) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(channel);
			tx.commit();
			
			EventType.deleteAllByChannel(channel.getTenantId(), channel.getCode());
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static QAChannel get(int id){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses
					.createCriteria(QAChannel.class)
					.add(Restrictions.eq("id", id));
			List<QAChannel> channels = ct.list();
			if (channels.size()<1)
				return null;
			return channels.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static QAChannel get(int tenantId, String code){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses
					.createCriteria(QAChannel.class)
					.add(Restrictions.eq("tenantId", tenantId));
			if (code!=null) {
				//ct.add(Restrictions.ilike("code", code, MatchMode.EXACT));
				ct.add(Restrictions.eq("code", code));
			}
			List<QAChannel> channels = ct.list();
			if (channels.size()<1)
				return null;
			return channels.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static QAChannel get(int tenantId, String name, String code){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses
					.createCriteria(QAChannel.class)
					.add(Restrictions.eq("tenantId", tenantId));
			if (name!=null) {
				//ct.add(Restrictions.ilike("name", name, MatchMode.EXACT));
				ct.add(Restrictions.eq("name", name));
			}
			if (code!=null) {
				//ct.add(Restrictions.ilike("code", code, MatchMode.EXACT));
				ct.add(Restrictions.eq("code", code));
			}
			List<QAChannel> channels = ct.list();
			if (channels.size()<1)
				return null;
			return channels.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static ArrayList<Map<String, Object>> getArrayData(int tenantId) {
		List<QAChannel> chList = list(tenantId);
		ArrayList<Map<String, Object>> channelList = new ArrayList<Map<String, Object>>();
		
		for (QAChannel ch: chList)  {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put( "id", ch.getId() );
			data.put( "name", ch.getName() );
			data.put( "code", ch.getCode() );
			data.put( "defaultChannelCode", ch.getDefaultChannelCode() );
			data.put( "type", ch.getType().name() );
			data.put( "appendOptionToOutput", ch.getAppendOptionToOutput() );
			data.put( "returnOptionInJson", ch.getReturnOptionInJson() );
			data.put( "useHtmlNewline", ch.getUseHtmlNewline() );
			data.put( "supportMultiRichMessages", ch.getSupportMultiRichMessages() );
			data.put( "useCustomMatchCtrlFlow", ch.getUseCustomMatchCtrlFlow() );
			channelList.add(data);
		}
		return channelList;
	}

	public QAChannel getDefaultChannel() {
		String defChCode = getDefaultChannelCode();
		
		if (!StringUtils.equals(defChCode, code)) {
			return QAChannel.get(tenantId, defChCode);
		}
		
		return null;
	}
}
