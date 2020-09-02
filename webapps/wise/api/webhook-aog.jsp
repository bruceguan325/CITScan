<%@ include file="/commons/taglib.jsp"%><%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.servlet.FakeHttpRequest"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.*"
	import="java.io.*"
	import="java.util.*"
	import="java.lang.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.io.*"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
	import="com.microsoft.ttshttpoxford.sample.*"
	import="com.intumit.systemconfig.*"
	import="org.apache.solr.common.*"
	import="com.intumit.solr.robot.QAUtil.FormalAnswerReplacer"
	import="com.intumit.solr.robot.dictionary.KnowledgePointDictionary"
	import="com.intumit.solr.robot.connector.line.LINEBotApi"
%><%!
%><%

boolean testMode = false;
if (request.getParameter("testMode") != null) {
	if (Boolean.parseBoolean(request.getParameter("testMode")))
	{
		testMode = true;
	}
}

String jsonStr = IOUtils.toString(request.getInputStream(), "UTF-8");
JSONObject json = new JSONObject(jsonStr);
System.out.println(json.toString(2));

String qaId = null;
FakeHttpRequest fakeReq = new FakeHttpRequest(request);
boolean mayBeInbound = false;

if (json.has("conversation")) {
	JSONObject conv = json.optJSONObject("conversation", new JSONObject());
	String convType = conv.optString("type", "ACTIVE");
	qaId = conv.optString("conversationId");
	
	if (StringUtils.equalsIgnoreCase(convType, "NEW")) {
		mayBeInbound = true;
	}
}
if (qaId == null) {
	qaId = json.optJSONObject("user", new JSONObject()).optString("userId");
}
fakeReq.setFakeParameter("ch", "aog");
fakeReq.setFakeParameter("q", json.getJSONArray("inputs").getJSONObject(0).getJSONArray("rawInputs").getJSONObject(0).getString("query"));


//for LINE Channel Check
String LINE_CTX_PREFIX = "LINE:";
String ch = request.getParameter("ch");
QAChannel channel = null;
boolean isLINEChannel = false;
JSONArray messages = null;
Tenant t = Tenant.getFromRequest(request, testMode);
//======================= 

if (t != null) {
	channel = QAChannel.get(t.getId(), ch);
	if (channel != null && channel.getType() == QAChannelType.LINE) {
		qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), ch), request.getParameter("id")); // qa-ajax.jsp 模式下，id 目前被拿來當作 line id 傳遞，因此有此邏輯
		isLINEChannel = true;
	}
}

if (StringUtils.isBlank(qaId)) {
	qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), ch), null); //LINE_CTX_PREFIX + t.getId() + ":" + fromUser;
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
	
	if (mayBeInbound)
		fakeReq.setFakeParameter("et", "inbound");
}

try {
	QAUtil.parseRequest(qaCtx, fakeReq, testMode, true);
}
catch (RobotException rbtEx) {
	out.println(rbtEx.getError().toString(2));
	return;
}

String fromUser = qaId;

{	// Just for print out log
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	String tmpQ = request.getParameter("q");
		
	// for line
	if (isLINEChannel) {
		qaCtx.setCtxAttr("LINEID", fromUser); 
		qaCtx.setQaChannel(channel.getCode()); 
		qaCtx.setEservice(channel.getCode()); 
		qaCtx.setClientSupportHtml(false); 
		qaCtx.setOriginalQuestion(tmpQ); 
		qaCtx.setCurrentQuestion(tmpQ);
		
		messages = new JSONArray(); 
	}
	////////

	boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
	String logStr = 
		String.format("[T:%s(%d)] Got Question [%s -> %s] IP:[%s] CH:[%s] ADM:[%s] IS-HTML:[%b]", 
				qaCtx.getTenant().getName(), qaCtx.getTenant().getId(),
				qaCtx.getOriginalQuestion(), qaCtx.getCurrentQuestion(),
				com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request),
				qaCtx.getQaChannel(),
				user != null ? user.getLoginName() : "N/A",
				qaCtx.isClientSupportHtml()
				);
	System.out.println("*******************************************************************");
	System.out.println("*******************************************************************");
	System.out.println(logStr);

	if (explainQA) {
		qaCtx.appendExplain("Enter QA-AJAX", logStr);
	}
}

//檢查 et (Event Type)
String eventType = StringUtils.defaultString(fakeReq.getParameter("et"), "message");
JSONObject resp = null;

// 這裡開始 check DemoFlow, 只有在 debug 模式下才運行
if (qaCtx.getTenant().getEnableDebug()) {
	DemoFlow df = DemoFlow.getInstance(qaCtx.getTenant().getId());
			
	if (df.isEnable() && df.isOverwriteMode()) {
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
}

if ((qaCtx.getEventType() != null && !"message".equalsIgnoreCase(qaCtx.getEventType().getCode())) ||
	 qaCtx.getCurrentQuestion() != null) {
	JSONObject conversation = qaCtx.getConversationsJson();
	QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
	c.check(qaCtx);

	resp = qaCtx.genResponseJSON();
	// line.messages 拿來全用
	if (isLINEChannel) {
		if (resp.containsKey("line")) {
			JSONObject lo = resp.optJSONObject("line", new JSONObject());
			if (lo.has("messages")) {
				messages.addAll(lo.optJSONArray("messages", new JSONArray()));
			}
		}
		else if (resp.has("messages")) {
			// 理論上應該不會來到這裡
			messages.addAll(resp.optJSONArray("messages", new JSONArray()));
			
			String forLine = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
			if (StringUtils.trimToNull(forLine) != null) {
				messages.add(0, LINEBotApi.buildTextMessage(forLine));
			}
		}
	
		JSONObject lineResult = new JSONObject();
		lineResult.put("to", fromUser);
		lineResult.put("messages", messages);
		resp.put("line", lineResult);
	}
	////////
	conversation.getJSONArray("messages").put(resp);
	qaCtx.setConversations(conversation.toString());

	Map<String, Object> ctxAttr = qaCtx.getCtxAttr();
	//resp.append("bundle", ctxAttr.get("_bundle"));

	//System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
	QAContextManager.put(qaId, qaCtx);

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
			log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(),
			ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
			ServiceLogEntity.setIntoSession(qaId, log);
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
	}
}

System.out.println(resp.toString(2));
JSONObject aogResp = new JSONObject();
aogResp.put("expectUserResponse", true);

JSONObject simpleResponse = new JSONObject();
simpleResponse.put("simpleResponse", new JSONObject().put("textToSpeech", resp.optString("output", "")));

if (resp.has("aog")) {
	aogResp.put("expectedInputs", 
			new JSONObject().put("inputPrompt", 
					new JSONObject().put("richInitialPrompt", 
							new JSONObject().put("items", new JSONArray().put(simpleResponse))
							)
					).put("possibleIntents", resp.getJSONObject("aog").getJSONArray("messages").put(0, new JSONObject().put("intent", "actions.intent.TEXT"))
			));
	// aogResp.put("fulfillmentText", resp.optString("output", ""));
	// aogResp.put("fulfillmentMessages", resp.getJSONObject("aog").get("messages"));
	
	/*
	JSONObject google = new JSONObject();
	google.put("expectUserResponse", true);
	google.put("expectedInputs", 
			new JSONObject().put("inputPrompt", 
					new JSONObject().put("richInitialPrompt", 
							new JSONObject().put("items", resp.getJSONObject("aog").get("messages"))
							)
					).put("possibleIntents", new JSONObject().put("intent", "actions.intent.TEXT"))
			);
	
	aogResp.put("payload", new JSONObject().put("google", google));
	*/
}
else {
	aogResp.put("expectedInputs", 
			new JSONObject().put("inputPrompt", 
					new JSONObject().put("richInitialPrompt", 
							new JSONObject().put("items", new JSONArray().put(simpleResponse))
							)
					).put("possibleIntents",
							new JSONArray().put(
									new JSONObject().put("intent", "actions.intent.TEXT")
								)
						)
			
	);
}
	
//}
%><%= aogResp.toString(2) %>