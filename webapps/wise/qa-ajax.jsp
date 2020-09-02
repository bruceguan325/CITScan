<%@page import="com.intumit.citi.MisLogReport"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.*"
	import="java.io.*"
	import="java.util.*"
	import="java.lang.*"
	import="org.apache.commons.lang.*"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
	import="com.microsoft.ttshttpoxford.sample.*"
	import="com.intumit.systemconfig.*"
	import="org.apache.solr.common.*"
	import="com.intumit.solr.robot.QAUtil.FormalAnswerReplacer"
	import="com.intumit.solr.robot.dictionary.KnowledgePointDictionary"
	import="com.intumit.solr.robot.connector.line.LINEBotApi"
	import="com.intumit.mas.MASEnvironment"
%><%!
%><%
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

MASEnvironment.isSkipCodeInChooseQAOptionRuleWhenMatchCurrentQuestion = true;
MASEnvironment.currentQuestionInChooseQAOptionRuleNeedSkipCode = new String[]{"1元"};

String qaId = request.getParameter("sessionid");

ArrayList<String> whiteList = new ArrayList();
ArrayList<String> whiteIp = WiSeUtils.whiteList(whiteList);
String remoteGuest = WiSeUtils.stGuest(request.getRemoteAddr());

if(StringUtils.isBlank(qaId)){
	qaId = request.getParameter("id");
}

if (true){
	boolean testMode = false;
	if (request.getParameter("testMode") != null) {
		if (Boolean.parseBoolean(request.getParameter("testMode")))
		{
			testMode = true;
		}
	}

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
	}

	try {
		QAUtil.parseRequest(qaCtx, request, testMode, true);
	}
	catch (RobotException rbtEx) {
		out.println(rbtEx.getError().toString(2));
		return;
	}

	String fromUser = request.getParameter("id");

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
	String eventType = StringUtils.defaultString(request.getParameter("et"), "message");
	JSONObject resp = null;

	/*
	// eventType 目前從各公司別內的「系統管理」當中有「事件管理」，可以維護可接受的事件代號
	// 而問答的「其他」頁籤當中可以選擇對應的事件，因此以後特定事件可以直接從管理介面維護對應的答案
	if ("inbound".equals(eventType)) {
		JSONObject wo = new JSONObject();
		wo.put("answerType", "WELCOME");
		wo.put("questionType", "WELCOME");
		
		String welcomeMsg = null;
		try {
			welcomeMsg = TemplateUtil.process("{{F:INBOUBDMSG}}", qaCtx.getQAUtil().getFormalAnswerReplacer(qaCtx));
		}
		catch (Exception ignore) {}
		wo.put("output", StringUtils.defaultString(welcomeMsg, "您好，很高興能為您服務!"));
		wo.put("datetime", QAUtil.formatDatetime(Calendar.getInstance().getTime()));;

		out.println(wo.toString(2));
		System.out.println("inbound - ok");
		return;
	} else if ("hangup".equals(eventType)) {
		JSONObject wo = new JSONObject();
		wo.put("answerType", "HANGUP");
		wo.put("questionType", "HANGUP");
		
		String hangupMsg = null;
		try {
			hangupMsg = TemplateUtil.process("{{F:HANGUPMSG}}", qaCtx.getQAUtil().getFormalAnswerReplacer(qaCtx));
		}
		catch (Exception ignore) {}
		wo.put("output", StringUtils.defaultString(hangupMsg, "希望能再次為您服務!"));
		wo.put("datetime", QAUtil.formatDatetime(Calendar.getInstance().getTime()));;

		out.println(wo.toString(2));
		System.out.println("hangup - ok");
		return;
	} else if ("message".equals(eventType)) {
	*/ 
	
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
		resp.append("bundle", ctxAttr.get("_bundle"));
	    JSONObject jsonobj = (JSONObject)ctxAttr.get("_bundle"); 
	    
	    String isAction = "";
	    if (jsonobj.has("action")) {
	    	isAction = jsonobj.get("action").toString();
	    }

	    if (isAction.equals("auto") && jsonobj.has("upLevel")) {
	    	resp.put("confidence", "高");
	    }
	    
		QAContextManager.put(qaId, qaCtx);
		
	    JSONObject misJson = qaCtx.genResponseJSON();
	    String countStr = request.getParameter("count");
	    int count = 0;
	    if (countStr != null && countStr.equals("1")) {
	    	count += 1;
	    }
		String userId = (jsonobj.has("UserID")? jsonobj.get("UserID").toString() : "");
	    String sessionId = (jsonobj.has("sessionid")? jsonobj.get("sessionid").toString() : "");
	    String currentCategory = (misJson.has("currentCategory")? misJson.get("currentCategory").toString() : "");
	    String kid = (misJson.has("kid")? misJson.get("kid").toString() : "");
	    String father = request.getParameter("father");
	    String upLevel = request.getParameter("upLevel");
	    String action = request.getParameter("action");
	    resp.put("action", action);
	    boolean isMenu = false;
	    String menu = request.getParameter("menu");
	    if (menu != null && menu.equals("true")) {
	    	isMenu = true;
	    }
	    String answerType = qaCtx.getAnswerType() + "";
	    int noAnswerCount = 0;
	    if (answerType.equals("NO_ANSWER")) {
	    	noAnswerCount += 1;
	    }
	    String nowDate = qaCtx.getOriginalQuestionTime();
		Date createTime = sdf.parse(nowDate);

		// DO LOG
		ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId, sessionId);
		MisLogReport misLog = new MisLogReport();
		
		if (testMode) {
			log = (ServiceLogEntity) session.getAttribute("TestModeServiceLogEntity");
			session.setAttribute("TestModeServiceLogEntity", log);
		}
		else {
			if (log == null) {
				log = ServiceLogEntity.log(qaCtx, request, "robot:cathay:chat", "" + System.currentTimeMillis(),
				ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null, qaId, sessionId, userId);
				ServiceLogEntity.setIntoSession(qaId, log);
			}
			
			log.setConversations(conversation.toString(2));
			log.setQaId(qaId);
			log.setUserId(userId);
			log.setSessionId(sessionId);
			log.setChannel(qaCtx.getQaChannel());
			log.setLastMessage(qaCtx.getOriginalQuestion());
			
			// MisReport
			misLog.setAction(action);
			misLog.setChannel(qaCtx.getQaChannel());
			misLog.setCount(count);
			misLog.setCreateTime(createTime);
			misLog.setCurrentCategory(currentCategory);
			misLog.setFather(father);
			misLog.setUpLevel(upLevel);
			misLog.setKid(kid);
			misLog.setMenu(isMenu);
			misLog.setNoAnswerCount(noAnswerCount);
			misLog.setSessionId(sessionId);
			misLog.setQuestion(MisLogReport.getStandQ(kid, t));
			
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
		        String[] ttsSetting = TTSService.getMicrosoftTtsSetting(qaCtx.getTenant().getLocale());
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
			MisLogReport.save(misLog);
			
		}
	}
	//}

	if (resp.has("currentQuestion")) resp.remove("currentQuestion");
	%><%= resp.toString(4) %>
<%
}else{
    response.sendRedirect(WiseSystemConfig.get().getContextPath() + "/ErrorPage404.jsp");
}
%>



