<%@page import="com.runawaybits.html2markdown.HTML2Md"%>
<%@page import="flexjson.JSONSerializer"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.solr.robot.TemplateUtil.Replacer"%>
<%@page import="com.intumit.solr.robot.QAUtil.CrmRedirectTag"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugin"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="com.intumit.systemconfig.WiseSystemConfigFacade"%>
<%@page import="com.intumit.systemconfig.WiseSystemConfig"%>
<%@page import="com.intumit.solr.robot.qarule.*"%>
<%@page import="com.intumit.solr.robot.qarule.ForwardToCrmRule.Forward" %>
<%@page import="com.intumit.solr.robot.qarule.ForwardToCrmRule.WaitingCmd" %>
<%@page import="com.intumit.solr.robot.qarule.ForwardToCrmRule.OffHourCmd" %>
<%@page import="com.intumit.solr.robot.qarule.ForwardToCrmRule.Cmd" %>
<%@page language="java" contentType="application/json; charset=utf-8" pageEncoding="UTF-8" 
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.tenant.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="com.intumit.hithot.HitHotLocale"
import="com.microsoft.botframework.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.codec.binary.Base64"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"

import="org.apache.http.client.HttpClient"
import="org.apache.http.client.methods.HttpPost"
import="org.apache.http.entity.StringEntity"
import="org.apache.http.HttpResponse"

import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
%><%!

String preConstructClean(String r) {
	String tmp = r;
	tmp = StringUtils.replaceChars( StringUtils.lowerCase(tmp), ",.?$&|-()[]{}\\/\"'，。－＝＋？＄＆／＼｜（）「」『』", "");
	if(tmp.equals(""))
		tmp = " ";
	return tmp;
}

String transformUrlIfFromApp(QAContext qaCtx, String text){
	String out = text;
	boolean isApp = StringUtils.isNotEmpty(qaCtx.getIcspid());
	if(isApp){
		out = UrlTransformer.base64UrlEncodeAndPrepend(text,
			WiseSystemConfigFacade.getInstance().get().getAppUrlTransformPrefix());
	}
	return out;
}

String getPayload(HttpServletRequest request) {
	StringBuilder stringBuilder = new StringBuilder();
	BufferedReader bufferedReader = null;
	try {
	  InputStream inputStream = request.getInputStream();
	  if (inputStream != null) {
	    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	    char[] charBuffer = new char[128];
	    int bytesRead = -1;
	    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	      stringBuilder.append(charBuffer, 0, bytesRead);
	    }
	  } else {
	    stringBuilder.append("");
	  }
	} catch (IOException ex) {
	    ex.printStackTrace();
	} finally {
	  if (bufferedReader != null) {
	    try {
	      bufferedReader.close();
	    } catch (IOException ex) {
	      ex.printStackTrace();
	    }
	  }
	}
	return stringBuilder.toString();
}

/**
 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
 */
void push(String serviceUrl, String accessToken, JSONObject from, String channelId, JSONObject channelData, String conversationId, String replyToken, String text, JSONArray attachments, JSONObject suggestedActions) {
 	
 	HttpClient httpClient = com.intumit.solr.util.ApacheHttpClientUtil.getNewHttpClient();
	try {
		JSONObject json = new JSONObject();
     	json.put( "from", from );
     	json.put( "type", "message" );
     	
     	if (text != null)
	     	json.put( "text", text );
     	
     	if (suggestedActions != null) 
     		json.put( "suggestedActions", suggestedActions );
     	
     	if (attachments != null) {
     		//System.out.println("****** GOT ATTACHMENTS" + attachments.toString(2));
     		json.put( "attachments", attachments );
     	}
     	
     	//json.put("suggestedActions", new JSONObject("{ \"actions\": [ { \"type\": \"imBack\", \"title\": \"你是誰\", \"value\": \"你是誰\" }, { \"type\": \"imBack\", \"title\": \"使用說明\", \"value\": \"使用說明\" }, { \"type\": \"imBack\", \"title\": \"講笑話\", \"value\": \"講笑話\" } ] }"));
     	//json.put("attachments", new JSONArray("[ { \"contentType\": \"application/vnd.microsoft.card.hero\", \"content\": { \"title\": \"title goes here\", \"subtitle\": \"subtitle goes here\", \"text\": \"descriptive text goes here\", \"images\": [ { \"url\": \"http://aka.ms/Fo983c\", \"alt\": \"picture of a duck\", \"tap\": { \"type\": \"playAudio\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" } } ], \"buttons\": [ { \"type\": \"playAudio\", \"title\": \"Duck Call\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" }, { \"type\": \"openUrl\", \"title\": \"Watch Video\", \"image\": \"http://aka.ms/Fo983c\", \"value\": \"http://www.intumit.com/\" } ] } }, { \"contentType\": \"application/vnd.microsoft.card.hero\", \"content\": { \"title\": \"title goes here\", \"subtitle\": \"subtitle goes here\", \"text\": \"descriptive text goes here\", \"images\": [ { \"url\": \"http://aka.ms/Fo983c\", \"alt\": \"picture of a duck\", \"tap\": { \"type\": \"playAudio\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" } } ], \"buttons\": [ { \"type\": \"playAudio\", \"title\": \"Duck Call\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" }, { \"type\": \"openUrl\", \"title\": \"Watch Video\", \"image\": \"http://aka.ms/Fo983c\", \"value\": \"http://www.intumit.com/\" } ] } }, { \"contentType\": \"application/vnd.microsoft.card.hero\", \"content\": { \"title\": \"title goes here\", \"subtitle\": \"subtitle goes here\", \"text\": \"descriptive text goes here\", \"images\": [ { \"url\": \"http://aka.ms/Fo983c\", \"alt\": \"picture of a duck\", \"tap\": { \"type\": \"playAudio\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" } } ], \"buttons\": [ { \"type\": \"playAudio\", \"title\": \"Duck Call\", \"value\": \"http://bot.hithot.cc/wise/commons/EA6CD1A359D7CC3F4E6B448790C5DBDA7EE4E443A28B9C8E8B6C5105D3985961-Female.wav\" }, { \"type\": \"openUrl\", \"title\": \"Watch Video\", \"image\": \"http://aka.ms/Fo983c\", \"value\": \"http://www.intumit.com/\" } ] } } ]" )); 
     	
     	if (channelData != null)
	     	json.put( "channelData", channelData );//new JSONObject("{\"quick_replies\": [ { \"content_type\": \"text\", \"title\": \"讚\", \"payload\": \"讚\"}, { \"content_type\": \"text\", \"title\": \"不讚\", \"payload\": \"不讚\"} ] }"));
	     	//json.put( "channelData", new JSONObject("{\"attachment\": { \"type\":\"template\", \"payload\":{ \"template_type\":\"generic\", \"elements\":[ { \"title\":\"Welcome to Peters Hats\", \"image_url\":\"https://bot.hithot.cc:8443/wise/img/icon_App.png\", \"subtitle\":\"Weve got the right hat for everyone.\", \"buttons\":[ { \"type\":\"postback\", \"title\":\"View Website\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" },{ \"type\":\"postback\", \"title\":\"Start Chatting\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" }              ]      }, { \"title\":\"Welcome to Facebook Messenger\", \"image_url\":\"https://bot.hithot.cc:8443/wise/img/icon_FacebookMessenger.png\", \"subtitle\":\"Weve got the right hat for everyone.\", \"buttons\":[ { \"type\":\"postback\", \"title\":\"View Website\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" },{ \"type\":\"postback\", \"title\":\"Start Chatting\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" }              ]      }, { \"title\":\"Welcome to LINE\", \"image_url\":\"https://bot.hithot.cc:8443/wise/img/icon_LINE.png\", \"subtitle\":\"Weve got the right hat for everyone.\", \"buttons\":[ { \"type\":\"postback\", \"title\":\"View Website\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" },{ \"type\":\"postback\", \"title\":\"Start Chatting\", \"payload\":\"DEVELOPER_DEFINED_PAYLOAD\" }              ]      } ] } } }"));

	    //System.out.println("Endpoint:" + "https://" + channelId + ".botframework.com/v3/conversations/" + conversationId + "/activities");
	    //System.out.println("Access Token:" + accessToken);
	    //System.out.println("Payload:" + json.toString(2));
		HttpPost request = new HttpPost(StringUtils.stripEnd(serviceUrl, "/") + "/v3/conversations/" + conversationId + "/activities");
        StringEntity params = new StringEntity(json.toString());
        request.addHeader("content-type", "application/json; charset=utf-8");
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.setEntity(params);
        
        //System.out.println("Sample Curl: curl -v -d '" + json.toString() + "' -H 'Content-Type: application/json; charset=utf-8' -H 'Authorization: Bearer " + accessToken + "' -X POST " + StringUtils.stripEnd(serviceUrl, "/") + "/v3/conversations/" + conversationId + "/activities");
        HttpResponse response = httpClient.execute(request);
         
        if (response != null) {
         	System.out.println( "StatusCode : " + response.getStatusLine().getStatusCode() );
         	
            BufferedReader in = new BufferedReader(
                     new InputStreamReader(response.getEntity().getContent()));
            String inputLine;
            StringBuffer resultData = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
             	resultData.append(inputLine);
            }
            in.close();
             
            System.out.println(resultData.toString());
        }
	} catch (Exception ex) {
         // handle exception here
     	 System.out.println(ex.toString());
         ex.printStackTrace();
	} finally {
	}
}
%><%
JSONObject req = new JSONObject(getPayload(request));
String botId = request.getParameter("botId");
System.out.println("Got Botframework request:" + req);

String reqType = req.getString("type");

if ("typing".equals(reqType)) {
	// Typing 目前當作沒看到
	return;
}
else if ("conversationUpdate".equals(reqType)) {
	// Typing 目前當作沒看到
	return;
}
JSONObject recipient = req.getJSONObject("recipient");
JSONObject from = req.getJSONObject("from");
String serviceUrl = req.getString("serviceUrl");
String channelId = req.getString("channelId");
String fromUserId = from.getString("id");
String conversationId = req.has("conversation") ? req.getJSONObject("conversation").getString("id") : null;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

String qaId = "MSBF:" + channelId + ":" + conversationId;
if (StringUtils.isBlank(qaId)) {
	qaId = "MSBF:" + channelId + ":" + java.util.UUID.randomUUID().toString();
}

boolean testMode = false;
if (req.optString("testMode") != null) {
	if (Boolean.parseBoolean(req.optString("testMode"))) {
		testMode = true;
	}
}

Tenant t = Tenant.getFromRequest(request, testMode);

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}

try {
	QAUtil.parseRequest(qaCtx, request, testMode, true);
}
catch (RobotException rbtEx) {
	out.println(rbtEx.getError().toString(2));
	return;
}
qaCtx.setClientSupportHtml(true);
qaCtx.setQaChannel(channelId);
qaCtx.setEservice(channelId);

boolean noRecommand = false;
if (req.optString("noRecommand") != null) {
	if (Boolean.parseBoolean(req.optString("noRecommand"))) {
		noRecommand = true;
	}
}

String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(req.optString("text")));
if (StringUtils.contains(tmpQ, '<')) {
	tmpQ = tmpQ.replaceAll("(?is)<[^>]+>", " ");
}
String suggestQ = qaCtx.getTenant().getEnablePhoneticHomonym() 
		? KnowledgePointDictionary.suggestFullQuestion(qaCtx.getTenant().getId(), tmpQ)
		: null;

qaCtx.setOriginalQuestion(tmpQ);
qaCtx.setCurrentQuestion(suggestQ != null ? suggestQ : tmpQ);

boolean explainQA = qaCtx.getTenant().getEnableQAExplain();

if (explainQA) {
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	qaCtx.appendExplain("Enter QA-AJAX", "Got Question from Botframework [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
	+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (user != null ? "FROM (" + user.getLoginName() + ")" : ""));
}

JSONObject conversation = qaCtx.getConversationsJson();
JSONObject resp = null;

if (qaCtx.getCurrentQuestion() != null) {
	QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
	c.check(qaCtx);

	resp = qaCtx.genResponseJSON();
	conversation.getJSONArray("messages").put(resp);
	qaCtx.setConversations(conversation.toString());

	//System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
	QAContextManager.put(qaId, qaCtx);

	//System.out.println(qaCtx.explainPath());

	// DO LOG
	ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);

	if (testMode) {
		log = (ServiceLogEntity) session.getAttribute("TestModeServiceLogEntity");
		session.setAttribute("TestModeServiceLogEntity", log);
		//System.out.println(qaCtx.getExplain());
		//System.out.println("TestModeServiceLogEntity: " + log.getConversations());
	}
	else {
		if (log == null) {
	log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", fromUserId,
	ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
	ServiceLogEntity.setIntoSession(qaId, log);
		}

		log.setConversations(conversation.toString(2));
		log.setChannel(qaCtx.getQaChannel());
		log.setLastMessage(qaCtx.getOriginalQuestion());
		// 有限制業務類別就存業務類別，沒有就看看知識主題有沒有業務類別，有的話存第一個
		if (qaCtx.getRestrictToQaCategory() != null) {
			log.setLastQaCategory(qaCtx.getRestrictToQaCategory());
		}
		else if (qaCtx.getCurrentQA() != null && qaCtx.getCurrentQA().getQaCategory() != null) {
			log.setLastQaCategory(qaCtx.getCurrentQA().getQaCategory());
		}
		else if (qaCtx.getCurrentKPs() != null && qaCtx.getCurrentKPs().length > 0) {
			for (DictionaryDatabase dd: qaCtx.getCurrentKPs()) {
				if (StringUtils.isNotEmpty(dd.getCategory())) {
					log.setLastQaCategory(dd.getCategory());
					break;
				}
			}
		}
		else if (qaCtx.getLastKPs() != null && qaCtx.getLastKPs().length > 0) {
			for (DictionaryDatabase dd: qaCtx.getLastKPs()) {
				if (StringUtils.isNotEmpty(dd.getCategory())) {
					log.setLastQaCategory(dd.getCategory());
					break;
				}
			}
		}
		log.setStatMsgCountFromUser(log.getStatMsgCountFromUser() + 1);
		log.setStatMsgCountFromRobot(log.getStatMsgCountFromRobot() + 1);
		log.setStatMsgCountTotal(log.getStatMsgCountTotal() + 2);
		
		if (qaCtx.getAnswerType() == QAContext.ANSWER_TYPE.FORWARD) {
	log.setStatForward(log.getStatForward() | 1);
		}
		
		if (AttentionKeywordRule.hasNegativeKeywords(qaCtx)) {
	log.setStatForward(log.getStatForward() | 2);
		}
		
		if (qaCtx.getAnswerType() == QAContext.ANSWER_TYPE.NO_ANSWER) {
	log.setStatMsgCountNoAnswer(log.getStatMsgCountNoAnswer() + 1);
		}
		else {
	log.setStatMsgCountHasAnswer(log.getStatMsgCountHasAnswer() + 1);
		}

		ServiceLogEntity.save(log);
	}
}

if (resp.has("output")) {
	String scopeDataKey = StringUtils.isNotEmpty(botId) ? ("botframeworkAuth::" + botId) : "botframeworkAuth";
	BotConnectorAuthentication bca = (BotConnectorAuthentication)TenantScopeDataMap.optValue(qaCtx.getTenant().getId(), scopeDataKey, null);
	if (bca == null || bca.GetAccessToken() == null) {
		JSONObject bfCfg = new JSONObject(qaCtx.getTenant().getLineBotConfigJson()).getJSONObject("botframework");
		
		if (StringUtils.isNotEmpty(botId) && bfCfg.has(botId)) {
			bfCfg = bfCfg.getJSONObject(botId);
		}
		
		bca = new BotConnectorAuthentication(bfCfg.getString("clientId"), bfCfg.getString("clientSecret"));
		TenantScopeDataMap.setValue(qaCtx.getTenant().getId(), scopeDataKey, bca);
	}
	
	// Currently the BotFramework message format is using Markdown, so we convert output html to markdown 
	String output = resp.getString("output");
    output = WiSeUtils.html2md(output);
    System.out.println("Botframework output:" + output); 
	resp.put("text", output);
	push(serviceUrl, bca.GetAccessToken().access_token, recipient, channelId, (JSONObject)qaCtx.getResponseAttribute("bfc_channelData"), conversationId, null, output, (JSONArray)qaCtx.getResponseAttribute("bfc_attachments"), (JSONObject)qaCtx.getResponseAttribute("bfc_suggestedActions"));
	
	if (qaCtx.getResponseAttribute("bfc_additional_pushs") != null) {
		JSONArray additional = (JSONArray)qaCtx.getResponseAttribute("bfc_additional_pushs");
		
		for (int i=0; i < additional.length(); i++) {
			JSONObject message = additional.getJSONObject(i);
			push(serviceUrl, bca.GetAccessToken().access_token, recipient, channelId, message.optJSONObject("channelData"), conversationId, null, message.optString("text"), (JSONArray)message.optJSONArray("attachments"), (JSONObject)qaCtx.getResponseAttribute("bfc_suggestedActions"));
		}
	}
	//System.out.println(resp);
}
%><%= "{}" %>
