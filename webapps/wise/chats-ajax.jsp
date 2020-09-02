<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
	import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.servlet.FakeHttpRequest"
	import="java.io.*"
	import="java.util.*"
	import="org.apache.commons.lang.*"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
	import="com.microsoft.ttshttpoxford.sample.*"
	import="com.intumit.systemconfig.*"
%><%!
%><%

boolean testMode = false;

String qaId = (String)session.getAttribute("qaId");
Tenant t = (Tenant)session.getAttribute("tenant");

if (StringUtils.isBlank(qaId) || t == null) {
	return ;
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}

try {
	FakeHttpRequest fakeReq = new FakeHttpRequest(request);
	Apikey apikey = Apikey.list(t.getId()).get(0);
	fakeReq.setFakeParameter("apikey", apikey.getApikey());
	QAUtil.parseRequest(qaCtx, fakeReq, testMode, true);
	
	if (request.getParameter("postback") != null) {
		JSONObject eventData = new JSONObject();
		// 未來應該每個 Tenant 甚至每個 session 有自己的 secret key
		eventData.put("data", WiSeUtils.aesDecrypt(Tenant.SIMPLE_AES_SECRET_KEY, Tenant.SIMPLE_AES_INIT_VECTOR, request.getParameter("postback")));
		qaCtx.setRequestAttribute("eventData", eventData);
	}
}
catch (RobotException rbtEx) {
	out.println(rbtEx.getError().toString(2));
	return;
}

{	// Just for print out log
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	String tmpQ = request.getParameter("q");

	boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
	System.out.println("*******************************************************************");
	System.out.println("*******************************************************************");
	System.out.println("Got Question [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (user != null ? "FROM (" + user.getLoginName() + ")" : "")
		+ "(Channel: " + qaCtx.getQaChannel() + ")"
		+ "(isClientSupportHTML: " + qaCtx.isClientSupportHtml() + ")"
		+ "(Tenant: " + qaCtx.getTenant().getName() + ")"
		);

	if (explainQA) {
		qaCtx.appendExplain("Enter QA-AJAX", "Got Question [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (user != null ? "FROM (" + user.getLoginName() + ")" : "")
		+ "(Channel: " + qaCtx.getQaChannel() + ")");
	}
}

JSONObject resp = null;

if (!"message".equalsIgnoreCase(qaCtx.getEventType().getCode()) || qaCtx.getCurrentQuestion() != null) {
	JSONObject conversation = qaCtx.getConversationsJson();
	QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
	c.check(qaCtx);

	resp = qaCtx.genResponseJSON();
	conversation.getJSONArray("messages").put(resp);
	qaCtx.setConversations(conversation.toString());

	Map<String, Object> ctxAttr = qaCtx.getCtxAttr();
	resp.append("bundle", ctxAttr.get("_bundle"));
	
	// Hacking for ckeditor wrap our answer with the annoying <p></p>，try to remove it
	String output = resp.getString("output");
	if (StringUtils.startsWith(output, "<p>")) {
		int pos = output.lastIndexOf("</p>");
		
		if (pos >= 0) {
			output = output.substring(3, pos) + output.substring(pos + 4);
		}
	}
	resp.put("output", output);

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
			log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(),
			ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
			ServiceLogEntity.setIntoSession(qaId, log);
		}

		log.setConversations(conversation.toString(2));
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
		
		if (Boolean.parseBoolean(request.getParameter("outputTTS"))) {
			String forTTS = com.intumit.solr.servlet.TextToSpeechServlet.ttsClean(resp.optString("output"), false, false);
			String[] ttsSetting = TTSService.getMicrosoftTtsSetting(t.getLocale());
			String outputFormat = AudioOutputFormat.Riff16Khz16BitMonoPcm;
		    String deviceLanguage = ttsSetting != null ? ttsSetting[0] : "zh-TW";
		    String genderName = ttsSetting != null ? ttsSetting[1] : Gender.Female;
		    String voiceName = ttsSetting != null ? ttsSetting[2] : "Microsoft Server Speech Text to Speech Voice (zh-TW, Yating, Apollo)";
			byte[] ttsBytes = TTSService.Synthesize(forTTS, outputFormat, deviceLanguage, genderName, voiceName);
			String FS = File.separator;
			String baseDir = WiSeEnv.getHomePath() + FS + ".." +  FS + "webapps" + FS + "wise" + FS + "commons" + FS;
			String key = TTSService.getCacheFileName(forTTS, deviceLanguage, genderName);
			WiseSystemConfig syscfg = WiseSystemConfig.get();
			
			resp.put("output", StringUtils.defaultString(syscfg.getHostname()) + syscfg.getContextPath() + "/" + "commons" + "/" + key + ".wav");
		}

		ServiceLogEntity.save(log);
	}
}

// 給前端的越簡單越好
JSONObject simplified = new JSONObject(resp, new String[] {"output", "datetime", "webRM", "feedbackId", "feedback", "inputType"});
//}
%><%= simplified.toString(4) %>