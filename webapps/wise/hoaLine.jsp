<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"

import="com.intumit.systemconfig.WiseSystemConfig"
import="com.intumit.solr.robot.*"
import="java.util.*"
import="com.intumit.solr.tenant.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.util.WiSeUtils"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.qarule.*"
import="org.apache.wink.json4j.*"
import="org.apache.http.impl.client.CloseableHttpClient"
import="org.apache.http.impl.client.HttpClientBuilder"
import="org.apache.http.client.methods.HttpPost"
import="org.apache.http.entity.StringEntity"
import="org.apache.http.HttpResponse"
import="org.apache.http.client.methods.HttpGet"

import="org.apache.commons.io.IOUtils"
import="java.io.*"


import="com.intumit.hithot.HitHotLocale"

import="com.intumit.solr.util.RequestRateLimiter"
%>
<%!
JSONObject buildReplyTextMessage(String msg) {
	JSONObject msgObj = new JSONObject();
	try {
		msgObj.put("type", "text");
		msgObj.put("text", msg);
	}
	catch (Exception e) {
		e.printStackTrace();
	}
	return msgObj;
}
JSONArray buildReplyTextMessagePush(String msg) {
	JSONArray pushMessage = new JSONArray();
	try {
		JSONObject msgObj = new JSONObject();
		msgObj.put("type", "text");
		msgObj.put("text", msg);
		pushMessage.add(msgObj);
	}
	catch (Exception e) {
		e.printStackTrace();
	}
	return pushMessage;
}

JSONObject getUserProfile(String accessToken, String uid) {
 	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
 	
	try {
		HttpGet request = new HttpGet("https://api.line.me/v2/bot/profile/" + uid);
        request.addHeader("content-type", "application/json");
        request.addHeader("Authorization", "Bearer " + accessToken);
        HttpResponse response = httpClient.execute(request);
         
        if (response != null) {
         	System.out.println( "StatusCode : "+response.getStatusLine().getStatusCode());
         	
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(response.getEntity().getContent()));
             String inputLine;
             StringBuffer resultData = new StringBuffer();

             while ((inputLine = in.readLine()) != null) {
             	resultData.append(inputLine);
             }
             in.close();
             
             System.out.println("Line User Profile:" + resultData.toString());
             
             return new JSONObject(resultData.toString());
        }
	} catch (Exception ex) {
     	System.out.println(ex.toString());
	} finally {
         httpClient.getConnectionManager().shutdown();
	}
	
	return null;
}

Boolean getBooleanParameter(HttpServletRequest request, String paramName, Boolean defaultVal) {
	Boolean val = null;
	if (request.getParameter(paramName) != null) {
		if (Boolean.parseBoolean(request.getParameter(paramName))) {
			val = true;
		}
		else {
			val = false;
		}
	}
	else {
		val = defaultVal;
	}

	return val;
}

String getStringParameterDefaultIfEmpty(HttpServletRequest request, String paramName, String defaultVal) {
	String val = request.getParameter(paramName);
	if (StringUtils.isEmpty(val)) {
		val = defaultVal;
	}

	return val;
}

void parseRequest(QAContext qaCtx, HttpServletRequest request, boolean testMode, boolean doShift, Tenant t, String ch) {
	Locale locale = (Locale) request.getSession().getAttribute("org.apache.struts.action.LOCALE");

	if (request.getParameter("locale") != null) {
		String localeStr = request.getParameter("locale");
		HitHotLocale hhl = HitHotLocale.valueOf(localeStr);
		locale = HitHotLocale.guessJavaLocale(hhl);

		qaCtx.setLocale(hhl);
	}
	else {
		qaCtx.setLocale(com.intumit.hithot.HitHotLocale.guessHitHotLocaleByJavaLocale(locale));
	}

	String baseUrl = StringUtils.trimToEmpty(WiseSystemConfig.get().getHostname()) + request.getContextPath();
	qaCtx.setCtxAttr("baseUrl", baseUrl);

	boolean noRecommand = getBooleanParameter(request, "noRecommand", false);
	String qaChannel = getStringParameterDefaultIfEmpty(request, "ch", ch);
	String userType = getStringParameterDefaultIfEmpty(request, "userType", "unknown");
	String etStr = getStringParameterDefaultIfEmpty(request, "et", "message");
	String eventSource = getStringParameterDefaultIfEmpty(request, "es", null);
	String eventSourceType = getStringParameterDefaultIfEmpty(request, "est", null);
	Boolean forceToggleContextThisTime = getBooleanParameter(request, "ftc", null);
	boolean htmlFormat = getBooleanParameter(request, "html", true);
	String icspid = getStringParameterDefaultIfEmpty(request, "ICSPID", null);
	String qaCategorySelect = getStringParameterDefaultIfEmpty(request, "category", null);
	String qaTagFilter = getStringParameterDefaultIfEmpty(request, "qaTagFilter", null);
	String replaceName = getStringParameterDefaultIfEmpty(request, "replaceName", null);

	if (t != null) {
		qaCtx.setTenant(t);
	}
	
	// 如果還是沒有 tenant，則失敗
	if (t == null) {
		JSONObject errorOutput = new JSONObject();
		try {
			errorOutput.put("errorCode", 400);
			errorOutput.put("errorMessage", "Cann't determine skill type.");
		}
		catch (JSONException ignore) {
		}

		throw new RobotException(errorOutput);
	}
	else if(!t.getEnableTenant()) {
		JSONObject errorOutput = new JSONObject();
		try {
			errorOutput.put("errorCode", 400);
			errorOutput.put("errorMessage", "Tenant disable.");
		} catch (JSONException ignore) {
		}
		
		throw new RobotException(errorOutput);
	}
	// 測試模式不計算 request 量，不然目前排程測試或者相關的自動測試可能都會卡住
	else if (!testMode) {
		if (!qaCtx.acquireRequestPermit()
				|| !RequestRateLimiter.acquireRequestPermition(
						com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request),
						qaCtx.getTenant(),
						"/qa-ajax.jsp")) {
			JSONObject errorOutput = new JSONObject();
			try {
				errorOutput.put("errorCode", 600);
				errorOutput.put("errorMessage",
						"Cannot acquire request permission, you should not flooding API request.");
			}
			catch (JSONException ignore) {
			}

			throw new RobotException(errorOutput);
		}
	}

	if (doShift) qaCtx.shift(); // 清除 request scope attributes

	String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(request.getParameter("q")));
	qaCtx.setOriginalQuestion(tmpQ);
	qaCtx.setOriginalQuestionTime(QAUtil.formatDatetime(Calendar.getInstance().getTime()));
	qaCtx.setCurrentQuestion(tmpQ);
	qaCtx.setQaChannel(qaChannel);
	qaCtx.setUserType(userType);
	qaCtx.setClientSupportHtml(htmlFormat);
	qaCtx.setForceToggleContextThisTime(forceToggleContextThisTime);
	qaCtx.setIcspid(icspid);
	if (qaCtx.getLastQaCategory() == null && qaCategorySelect != null) {
		qaCtx.setLastQaCategory(qaCategorySelect);
	}

	qaCtx.setEservice(qaChannel);

	EventType et = EventType.matchBestEventType(t.getId(), qaChannel, etStr);
	if (et == null) {
		System.out.println("Unknown EventType [" + etStr + "]...");
	}
	qaCtx.setEventType(et);
	
	// 是 message 的 default eventSource 是 InputBox，其餘是 Unknown
	if (et != null && "message".equals(et.getCode())) {
		if (eventSource == null) {
			eventSource = QAContext.EVENT_SOURCE_INPUTBOX;
		}
		if (eventSourceType == null) {
			eventSourceType = QAContext.EVENT_SOURCE_TYPE_KEYIN;
		}
	}
	else {
		if (eventSource == null) {
			eventSource = QAContext.EVENT_SOURCE_UNKNOWN;
		}
		if (eventSourceType == null) {
			eventSourceType = QAContext.EVENT_SOURCE_TYPE_UNKNOWN;
		}
	}
	qaCtx.setEventSource(eventSource);
	qaCtx.setEventSourceType(eventSourceType);

	qaCtx.setRequestAttribute("noRecommand", noRecommand);
	qaCtx.setRequestAttribute("qaCategorySelect", qaCategorySelect);
	if (qaTagFilter != null) {
		qaCtx.setRequestAttribute("qaTagFilter", qaTagFilter);
	}
	if (replaceName != null) {
		qaCtx.setRequestAttribute("replaceName", replaceName);
	}

	qaCtx.setCtxAttr("_bundle", new JSONObject(WiSeUtils.getParameterMap(request)));
}
%>
<%
JSONObject success = null;
if (request.getParameter("success") != null) {
	try{
		success = new JSONObject(new String(org.apache.commons.codec.binary.Base64.decodeBase64(StringUtils.trimToEmpty(request.getParameter("success"))), "UTF-8"));
	} catch (Exception ex) {
		JSONObject resp = new JSONObject();
		resp.put("isSuccess", "false");
		resp.put("Error in success, msg: ", ex);
		System.out.println(resp);
		out.println(resp);
		return;
	}
	
}

String ROCID = null;
String name = null;
String account = null;
String Birthday = null;
String ID = null;
String ch = null;

//選填
ROCID = success.has("ROCID") && !success.isNull("ROCID") ? success.getString("ROCID") : "";
Birthday = success.has("Birthday") && !success.isNull("Birthday") ? success.getString("Birthday") : "";

//必填
try {
	name = success.getString("name") != null ? success.getString("name") : "";
	account = success.getString("account") != null ? success.getString("account") : "";
	ID = success.getString("ID") != null ? success.getString("ID") : "";
	ch = success.getString("ch") != null ? success.getString("ch") : "";
} catch (Exception ex) {
	JSONObject resp = new JSONObject();
	resp.put("isSuccess", "false");
	resp.put("msg", ex);
	System.out.println(resp);
	out.println(resp);
	return;
}


QAContext qaCtx = QAContextManager.lookup(ID);
if (qaCtx == null) {
	JSONObject resp = new JSONObject();
	resp.put("isSuccess", "false");
	resp.put("msg", "No suce qaId");
	System.out.println(resp);
	out.println(resp);
	return;
}
qaCtx.setAnswerText(null);
qaCtx.setCtxAttr("userID", ROCID);
qaCtx.setCtxAttr("birthday", Birthday);
qaCtx.setCtxAttr("name", name);
qaCtx.setCtxAttr("account", account);

System.out.println("userID: " + ROCID + ", birthday: " + Birthday + ", name" + name + ", account" + account);

try {
	parseRequest(qaCtx, request, false, true, qaCtx.getTenant(), qaCtx.getQaChannel());
}
catch (RobotException rbtEx) {
	out.println(rbtEx.getError().toString(2));
	return;
}

boolean explainQA = qaCtx.getTenant().getEnableQAExplain();

Tenant t = qaCtx.getTenant();
JSONObject botCfg = new JSONObject(StringUtils.isNotEmpty(t.getLineBotConfigJson()) ? t.getLineBotConfigJson() : "{}");
JSONObject lineBotCfg = botCfg.has("line") ? botCfg.getJSONObject("line") : botCfg; // Hardcode for old json format, should be removed someday.
String accessToken = lineBotCfg.optString("accessToken");
String channelSecret = lineBotCfg.optString("channelSecret");
String theQ = null;
JSONObject resp = null;
JSONArray messages = new JSONArray();
Map<String, Object> eventData = null;
String tmpQ = null;
String pageToken = FacebookBotApi.getPageToken(t.getId());

theQ = qaCtx.getCtxAttr("lastQ").toString();


UserClue clue = qaCtx.getUserClue();
if (clue == null) {
	clue = UserClue.getByLineUserId(t.getId(), qaCtx.getContextId());
	
	if (clue == null) {
		clue = new UserClue();
		clue.setTenantId(t.getId());
		clue.setLineUserId(qaCtx.getContextId());
		UserClue.saveOrUpdate(clue);
	}
	else {
		UserClue.saveOrUpdate(clue);	// 單純刷新 updateTime
	}
}

if (qaCtx.getCtxAttr("NICKNAME") == null || clue.getNickname() == null) {
	JSONObject profile = null;
	if (true) {
		profile = getUserProfile(accessToken, qaCtx.getContextId().toString());
	}
	
	if (profile != null) {
		String displayName = profile.optString("displayName");
		
		if (displayName != null) {
			qaCtx.setCtxAttr("NICKNAME", displayName);
			
			if (!StringUtils.equals(displayName, clue.getNickname())) {
				clue.setTenantId(t.getId());		// 解決版本差異
				clue.setNickname(displayName);
				UserClue.saveOrUpdate(clue);
			}
		}
	}
}


DemoFlow df = DemoFlow.getInstance(qaCtx.getTenant().getId());

qaCtx.setCurrentQuestion(theQ);
qaCtx.setOriginalQuestion(theQ);
qaCtx.setQaChannel(qaCtx.getQaChannel());
qaCtx.setEservice(qaCtx.getQaChannel());
qaCtx.setClientSupportHtml(false);

String etCode = "message";
EventType et = EventType.matchBestEventType(t.getId(), qaCtx.getQaChannel(), etCode);
if (et == null) {
	System.out.println("Unknown EventType [" + etCode + "]...");
}
qaCtx.setEventType(et);


	//有 Q 就跑答案
if ( theQ != null ) {
    tmpQ = theQ;

	if (explainQA) {
		qaCtx.appendExplain("Enter QA-AJAX", "Got Question from LINE [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (qaCtx.getCtxAttr("LINEID") != null ? "FROM (" + qaCtx.getCtxAttr("LINEID") + ")" : ""));
	}
    
    String suggestQ = qaCtx.getTenant().getEnablePhoneticHomonym() 
    	? KnowledgePointDictionary.suggestFullQuestion(qaCtx.getTenant().getId(), tmpQ)
    	: null;

    qaCtx.setOriginalQuestion(tmpQ);
    qaCtx.setCurrentQuestion(suggestQ != null ? suggestQ : tmpQ);
    
    if (t.getEnableDebug() && df.isEnable() && df.isOverwriteMode()) {
        if (df.getCurrentOffset() != -1 && df.getCurrentOffset() < df.getQuestions().size()) {
        	String dfQ = df.getQuestions().get(df.getCurrentOffset());
        		
        	if (df.isAutopilotMode()) {
        		df.setCurrentOffset(df.getCurrentOffset() + 1);
        	}
        		
        	if (df.isSelfRepair()) { // 目前暫時拿來作為覆寫模式每次覆寫完就停止覆寫的啟用或關閉
        		df.setOverwriteMode(false);
        	}
        	qaCtx.setOriginalQuestion(dfQ);
        	qaCtx.setCurrentQuestion(dfQ);
        }
    }

	if (qaCtx.getCurrentQuestion() != null) {
        QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
        c.check(qaCtx);
        resp = qaCtx.genResponseJSON();

        // line.messages 拿來全用
        if (resp.containsKey("line")) {
        	JSONObject lo = resp.optJSONObject("line", new JSONObject());
        	if (lo.has("messages")) {
				messages.addAll(lo.optJSONArray("messages", new JSONArray()));
        	}
        }
        else if (resp.has("messages")) {
        	// 理論上應該不會來到這裡
        	if ("line".equals(qaCtx.getQaChannel())){
				messages.addAll(resp.optJSONArray("messages", new JSONArray()));
	
		        String forLine = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
	        	if (StringUtils.trimToNull(forLine) != null) {
	        		messages.add(0, buildReplyTextMessage(forLine));
	        	}
        	}
        }
	}
}

if (resp != null) {
	System.out.println(resp);
	JSONObject conversation = qaCtx.getConversationsJson();
    conversation.getJSONArray("messages").put(resp);
    qaCtx.setConversations(conversation.toString());
    	
    //System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
    QAContextManager.put(ID, qaCtx);
    	
    // DO LOG
    ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), ID);
    	
    
   	if (log == null) {
	    log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", qaCtx.getCtxAttr("LINEID").toString(),
	    ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
	    ServiceLogEntity.setIntoSession(ID, log);
   	}

   	log.setConversations(conversation.toString(2));
   	log.setChannel(qaCtx.getQaChannel());
   	log.setLastMessage(qaCtx.getCurrentQuestion());
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
	   		
  	if (qaCtx.getTenant() != null) {
//     	EvaluationLogEntity evaLog = EvaluationLogEntity.log(request, QAUtil.CURRENT_EVALUATION_LOG_NS, resp.toString(), qaCtx);
   		
//     	JSONArray actions = new JSONArray();
//     	JSONObject action = null;
//     	action = new JSONObject().put("type", "postback").put("label", qaCtx.getMessage("question.feedback.good")).put("data", "EVALUATION_"+evaLog.getId()+"_2");
//     	actions.add(action);
//     	action = new JSONObject().put("type", "uri").put("label", qaCtx.getMessage("question.feedback.bad")).put("uri", WiseSystemConfig.get().getHostname() + "/wise/qa-eval-log-line.jsp?from=line&logId="+evaLog.getId()+"&flag=5&qaId="+qaCtx.getContextId());
//     	actions.add(action);
	   	if ("line".equals(qaCtx.getQaChannel())){
	   		NaverLineAnswerTransformer.push(accessToken, qaCtx.getCtxAttr("LINEID").toString(), messages);
	  	}
	   	if ("facebook".equals(qaCtx.getQaChannel())){
	   		String forMessenger = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
	   		if (StringUtils.trimToNull(forMessenger) != null) {
        		FacebookBotApi.push(pageToken, qaCtx.getCtxAttr("MESSENGERID").toString(), FacebookBotApi.buildTextMessage(forMessenger));
			}
	        	
	        if (resp.containsKey("messages")) {
				messages.addAll(resp.optJSONArray("messages", new JSONArray()));
	        			
        		for (int i = 0; i < messages.size(); i++) {
        			FacebookBotApi.push(pageToken, qaCtx.getCtxAttr("MESSENGERID").toString(), messages.getJSONObject(i));
        		}
	        }
	   	}
	}
}



JSONObject resps = new JSONObject();
resps.put("isSuccess", "true");
out.println(resps);
%>