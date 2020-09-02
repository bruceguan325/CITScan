package com.intumit.solr.robot.push;

import java.util.ArrayList;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.elasticsearch.common.lang3.StringUtils;

import com.ibm.icu.util.Calendar;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.LineConfig;
import com.intumit.solr.robot.NaverLineAnswerTransformer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAChannelType;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.ServiceLogEntity;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.connector.line.LINEBotApi;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.push.PushData.ContentType;
import com.intumit.solr.robot.push.PushData.TargetChannelsType;
import com.intumit.solr.robot.qarule.AttentionKeywordRule;
import com.intumit.solr.robot.qarule.ForwardToCrmRule;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.tenant.Tenant;

public class PushService {

	Tenant t = null;

	public PushService(Tenant t) {
		super();
		this.t = t;
	}
	
	public boolean syncPush(PushData data, UserClue u) {
		Tenant t = Tenant.get(data.getTenantId());
		
		if (t == null) {
			System.out.println("Push failed, tenant not exist [" + data.getTenantId() + "]");
			return false;
		}
		if (t.getId() != u.getTenantId()) {
			System.out.println("Wired push data, the tenant id not equal! [pushData.getTenantId() = " + t.getId() + " / userClue.getTenantId() = " + u.getTenantId() + "]");
			return false;
		}
		
		List<QAChannel> targetChannels = new ArrayList<QAChannel>();
		
		switch (data.getTargetChannelsType()) {
			case all:
				for (QAChannel c: QAChannel.list(t.getId())) {
					if (c.getType() == QAChannelType.FACEBOOK_MESSENGER) {
						targetChannels.add(c);
					}
					else if (c.getType() == QAChannelType.LINE) {
						targetChannels.add(c);
					}
					else if (c.getType() == QAChannelType.SKYPE) {
						targetChannels.add(c);
					}
					else if (c.getType() == QAChannelType.SLACK) {
						targetChannels.add(c);
					}
					else if (c.getType() == QAChannelType.MICROSOFT_TEAMS) {
						targetChannels.add(c);
					}
				}
				break;
			case byConfig:
				String[] chStrs = StringUtils.split(data.getTargetChannels(), ",");
				for (String chStr: chStrs) {
					QAChannel c = QAChannel.get(t.getId(), chStr);
					
					if (c != null) {
						targetChannels.add(c);
					}
				}
				break;
			case byUserSubscription:
				// #TODO : Not implemented (未來有提供使用者設定介面的時候就可以讀取該設定，可能想太多）
				break;
			default:
				
		}
		
		try {
			JSONObject botCfg = new JSONObject(t.getLineBotConfigJson());
			for (QAChannel c: targetChannels) {
				switch (c.getType()) {
					case LINE:
						JSONObject lineCfgJson = botCfg.optJSONObject(c.getCode());
						if (lineCfgJson == null) {
							lineCfgJson = botCfg.optJSONObject("line");
						}
						if (lineCfgJson == null) {
							System.out.println("Cannot find any config in tenant for LINE channel:" + c.getCode());
							continue;
						}
						
						LineConfig lineCfg = new LineConfig(c.getCode(), lineCfgJson.toString());
						ContentType ct = data.getContentType();
						JSONArray msgs = new JSONArray();
						JSONObject resp = null;
						
						switch (ct) {
							case TEXT:
			        			msgs.add(0, LINEBotApi.buildReplyTextMessage(data.getPushContent()));
								msgs.forEach( (m)->NaverLineAnswerTransformer.addEventSource((JSONObject)m, "_PUSHDATA_" + data.getId(), QAContext.EVENT_SOURCE_TYPE_BUTTON) );
								break;
							case JSON_OBJECT:
								JSONObject msg = new JSONObject(data.getPushContent());
								msgs.add(msg);
								msgs.forEach( (m)->NaverLineAnswerTransformer.addEventSource((JSONObject)m, "_PUSHDATA_" + data.getId(), QAContext.EVENT_SOURCE_TYPE_BUTTON) );
								break;
							case JSON_ARRAY:
								msgs = new JSONArray(data.getPushContent());
								msgs.forEach( (m)->NaverLineAnswerTransformer.addEventSource((JSONObject)m, "_PUSHDATA_" + data.getId(), QAContext.EVENT_SOURCE_TYPE_BUTTON) );
								break;
							case QA:
								Long kid = new Long(data.getPushContent());
								QAUtil qautil = QAUtil.getInstance(t);
								final QAContext tmpCtx = new QAContext();
								tmpCtx.setTenant(t);
								tmpCtx.setUserClue(u);
								
								QA currentQa = new QA(qautil.getMainQASolrDocument(kid));
        					    resp = qautil.getStandardA(kid, c, tmpCtx);
        
					        	// line.messages 拿來全用
					        	if (resp.has("line")) {
					        		JSONObject lo = resp.optJSONObject("line", new JSONObject());
					        		
					        		if (lo.has("messages")) {
					        			msgs = lo.optJSONArray("messages", new JSONArray());
					        		}

									msgs.forEach( (m)->NaverLineAnswerTransformer.addEventSource((JSONObject)m, currentQa.getId(), QAContext.EVENT_SOURCE_TYPE_BUTTON) );
					        	}
					        	else if (resp.has("messages")) {
					        		// 理論上應該不會來到這裡
					        		msgs.addAll(resp.optJSONArray("messages", new JSONArray()));
        
						        	String forLine = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
					        		if (StringUtils.trimToNull(forLine) != null) {
					        			msgs.add(0, LINEBotApi.buildReplyTextMessage(forLine));
					        		}
					        	}
								break;
							case HTML:
							case JPEG:
							case PNG:
							case XML:
							default:
								// #TODO: to be implement, 其他 format 都不管？！
						}
						
						if (msgs.size() > 0 && resp != null) {
							// 為了 Push 也能有紀錄 (START)
							resp.put("eventTypeCode", "push");
							resp.put("eventTypeChannel", c.getCode());
							resp.put("TriggeredByEventType_Code", "push");
							resp.put("TriggeredByEventType_Channel", c.getCode());
							resp.put("eventSourceType", "schedule");
							resp.put("eventSource", "_PUSHDATA_" + data.getId());
							resp.put("eservice", c.getCode());
							
							String qaId = QAContextManager.generateQaId(t, c, u.getLineUserId());
							QAContext ctx = QAContextManager.lookup(qaId);
							if (ctx == null) {
								ctx = QAContextManager.create(qaId);
								ctx.setTenant(t);
								if (c != null) ctx.setQaChannel(c.getCode());
							}

							ServiceLogEntity log = ServiceLogEntity.getFromSession(t, qaId);
							if (log == null) {
								log = ServiceLogEntity.log(ctx.getTenant(), null, "robot:cathay:chat", "" + System.currentTimeMillis(),
											ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
								log.setTsCreated(Calendar.getInstance().getTime());
								ServiceLogEntity.setIntoSession(qaId, log);
							}
							JSONObject conversations = ctx.getConversationsJson();
							
							conversations.getJSONArray("messages").put(resp);
							ctx.setConversations(conversations.toString());
							QAContextManager.put(qaId, ctx);
							
							log.setConversations(conversations.toString(2));
							log.setChannel(ctx.getQaChannel());
							log.setLastMessage(ctx.getCurrentQuestion());
							
							// 有限制業務類別就存業務類別，沒有就看看知識主題有沒有業務類別，有的話存第一個
							if (ctx.getRestrictToQaCategory() != null) {
								log.setLastQaCategory(ctx.getRestrictToQaCategory());
							}
							else if (ctx.getCurrentQA() != null && ctx.getCurrentQA().getQaCategory() != null) {
								log.setLastQaCategory(ctx.getCurrentQA().getQaCategory());
							}
							else if (ctx.getCurrentKPs() != null && ctx.getCurrentKPs().length > 0) {
								for (DictionaryDatabase dd: ctx.getCurrentKPs()) {
									if (StringUtils.isNotEmpty(dd.getCategory())) {
										log.setLastQaCategory(dd.getCategory());
										break;
									}
								}
							}
							else if (ctx.getLastKPs() != null && ctx.getLastKPs().length > 0) {
								for (DictionaryDatabase dd: ctx.getLastKPs()) {
									if (StringUtils.isNotEmpty(dd.getCategory())) {
										log.setLastQaCategory(dd.getCategory());
										break;
									}
								}
							}
							log.setStatMsgCountFromUser(log.getStatMsgCountFromUser() + 1);
							log.setStatMsgCountFromRobot(log.getStatMsgCountFromRobot() + 1);
							log.setStatMsgCountTotal(log.getStatMsgCountTotal() + 2);
							
							if (ctx.getAnswerType() == QAContext.ANSWER_TYPE.FORWARD) {
								log.setStatForward(log.getStatForward() | 1);
							}
							
							if (AttentionKeywordRule.hasNegativeKeywords(ctx)) {
								log.setStatForward(log.getStatForward() | 2);
							}
							
							if (ctx.getAnswerType() == QAContext.ANSWER_TYPE.NO_ANSWER) {
								log.setStatMsgCountNoAnswer(log.getStatMsgCountNoAnswer() + 1);
							}
							else {
								log.setStatMsgCountHasAnswer(log.getStatMsgCountHasAnswer() + 1);
							}

							ServiceLogEntity.save(log);
							// 為了 Push 也能有紀錄 (END)
							
							LINEBotApi.push(lineCfg.getAccessToken(), u.getLineUserId(), msgs);
						}
						else {
							System.out.println("There is no content for push, push data id [" + data.getId() + "] userCludId [" + u.getId() + "]");
						}
						break;
					case FACEBOOK_MESSENGER:
						break;
					case SLACK:
					case SKYPE:
					case MICROSOFT_TEAMS:
						break;
					default:
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static void main(String[] args) {
		HibernateUtil.init();
		MessageUtil.initialize();
		
		/*
		 * You should set following system property to get this test work correctly
		 * -Dtest.line.id=""
		 * -Dtest.line.accessToken=""
		 * -Dtest.line.channelId=""
		 * -Dtest.line.channelSecret=""
		 * -Dtest.line.version=""
		 */
		String lineId = System.getProperty("test.line.id");	// The tester who want to get test message
		String channelId = System.getProperty("test.line.channelId");
		String channelSecret = System.getProperty("test.line.channelSecret");
		String version = System.getProperty("test.line.version");
		String accessToken = System.getProperty("test.line.accessToken");
		String lineCfgStr = "{\"line\": { \"channelId\": \"" + channelId 
				+ "\", \"channelSecret\": \"" + channelSecret 
				+ "\", \"version\": \"" + version 
				+ "\", \"accessToken\": \"" + accessToken + "\" } }";
		
		Tenant tmpTenant = new Tenant();
		QAChannel ch = new QAChannel();
		UserClue c = null;  boolean deleteUserClue = false;
		PushData data = new PushData();
		try {
        		String ts = "" + System.currentTimeMillis();
        		// 準備暫時用 Tenant 以及 QAChannel（最後面 finally 區會刪掉）
        		tmpTenant.setName("Tmp" + ts);
        		tmpTenant.setNotes("TmpTN" + ts); 
        		tmpTenant.setLineBotConfigJson( lineCfgStr );
        		Tenant.saveOrUpdate(tmpTenant);
        		
        		ch.setType(QAChannelType.LINE);
        		ch.setCode("line");
        		ch.setName("LINE");
        		ch.setTenantId(tmpTenant.getId());
        		QAChannel.saveOrUpdate(ch);
        		
        		// 開始準備 PushData
        		data.setTenantId(tmpTenant.getId());
        		data.setContentType(ContentType.TEXT);
        		data.setPushContent("你好，帥哥");
        		data.setTargetChannels("line");
        		data.setTargetChannelsType(TargetChannelsType.byConfig);
        		PushData.saveOrUpdate(data); // 目前不需要 save 就能傳
		
        		// UserClue for receiving the message
        		c = UserClue.getByLineUserId(tmpTenant.getId(), lineId);
        		if (c == null) {
        			deleteUserClue = true;
        			c = new UserClue();
        			c.setLineUserId(lineId);
        			c.setTenantId(tmpTenant.getId());
        			UserClue.saveOrUpdate(c);
        		}
        		
        		PushService ps = new PushService(tmpTenant);
        		ps.syncPush(data, c);
        		
        		data.setContentType(ContentType.JSON_ARRAY);
        		data.setPushContent("[\n" + 
        				"    {\n" + 
        				"      \"type\": \"template\",\n" + 
        				"      \"template\": {\n" + 
        				"        \"type\": \"buttons\",\n" + 
        				"        \"actions\": [\n" + 
        				"          {\n" + 
        				"            \"type\": \"postback\",\n" + 
        				"            \"label\": \"甲溝炎相關症狀\",\n" + 
        				"            \"data\": \"action=_message&message=甲溝炎\"\n" + 
        				"          },\n" + 
        				"          {\n" + 
        				"            \"type\": \"postback\",\n" + 
        				"            \"label\": \"如何管理您的副作用\",\n" + 
        				"            \"data\": \"action=_message&message=管理我的副作用\"\n" + 
        				"          }\n" + 
        				"        ],\n" + 
        				"        \"text\": \"您可以點選下列按鈕進一步瞭解\",\n" + 
        				"        \"title\": \"相關資訊\"\n" + 
        				"      },\n" + 
        				"      \"altText\": \"請至手機觀看訊息內容\"\n" + 
        				"    }\n" + 
        				"]\n" + 
        				"");
        		
        		ps.syncPush(data, c);
        		
		}
		finally {
			try {
				// clean test data
				if (ch.getId() != 0) {
					QAChannel.delete(ch);
				}
				if (tmpTenant.getId() != 0) {
					Tenant.delete(tmpTenant.getId());
				}
				if (data.getId() != null) {
					//PushData.delete(data.getId());
				}
				if (c != null && c.getId() != null && deleteUserClue) {
					UserClue.delete(c);
				}
				
				HibernateUtil.shutdown();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.exit(0);
	}
}
