<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json; charset=utf-8" pageEncoding="UTF-8" 
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="java.util.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.io.IOUtils"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.servlet.SolrDispatchFilter"
	import="com.intumit.systemconfig.WiseSystemConfig"
	import="org.jsoup.Jsoup"
%>
<%

String fromGateway = request.getParameter("fromGateway");

String json = StringUtils.defaultIfEmpty(com.intumit.solr.util.WiSeUtils.getPayload(request), "{}"); 
System.out.println("got conn-messenger : " + new org.json.JSONObject(json).toString());

String mode = request.getParameter("hub.mode");
String verify = request.getParameter("hub.verify_token");

Tenant t = Tenant.getFromRequest(request, false);

// validate messenger webhook
if (fromGateway == null) {
	if (mode != null && verify != null) {
		System.out.println("webhook validation");
		if (request.getParameter("hub.verify_token").equals(FacebookBotApi.getVerifyToken(t.getId())) && 
			request.getParameter("hub.mode").equals("subscribe")) {
		    response.getWriter().append(request.getParameter("hub.challenge"));
		    return;
		}
	}
}

String pageToken = FacebookBotApi.getPageToken(t.getId());
String ch = request.getParameter("ch") != null ? request.getParameter("ch") : "messenger";

// 這裡開始 check DemoFlow
DemoFlow df = DemoFlow.getInstance(t.getId());

// for gateway
JSONObject messengerResp = new JSONObject();
JSONArray preMessages = new JSONArray();

JSONObject jsonData = new JSONObject(json);
JSONArray entrys = jsonData.getJSONArray("entry");
if (entrys == null || entrys.length() == 0) {
	System.out.println("'entry' json array from incoming event payload shouldn't be null.");
    return ;
}
	
for (int entryIdx=0; entryIdx < entrys.length(); entryIdx++) {
	JSONObject entry = ((JSONObject)entrys.get(entryIdx));
	JSONArray messagings = null;
	Map<String, Object> eventData = null;
	boolean isStandbyEvent = false;
	
	// 處理 Standby
	if (!entry.has("messaging") && entry.has("standby")) {
		isStandbyEvent = true;
		messagings = entry.getJSONArray("standby");	
	} 
	else {
		messagings = entry.getJSONArray("messaging");
	}

	String conversationId = entry.getString("id");
	JSONObject messaging = ((JSONObject)messagings.getJSONObject(0));
	JSONObject sender = messaging.getJSONObject("sender");
	JSONObject recipient = messaging.getJSONObject("recipient");
	String fromUser = sender.getString("id");
	String toUser = recipient.getString("id");
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	
    String qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), ch), fromUser); //MESSENGER_CTX_PREFIX + t.getId() + ":" + fromUser;
	
	boolean testMode = false;
	if (request.getParameter("testMode") != null && Boolean.parseBoolean(request.getParameter("testMode"))) {
		testMode = true;
	}
	boolean noRecommand = false;
	if (request.getParameter("noRecommand") != null && Boolean.parseBoolean(request.getParameter("noRecommand"))) {
		noRecommand = true;
	}
	
	QAContext qaCtx = QAContextManager.lookup(qaId);
	if (qaCtx == null) {
		qaCtx = QAContextManager.create(qaId);
	}
	try {
		QAUtil.parseRequest(qaCtx, request, testMode, true);
	} catch (RobotException rbtEx) {
		out.println(rbtEx.getError().toString(2));
		continue;
	}
	
	try {
		System.out.println("*****************, got webhook event:" + messaging);

		// 取得user傳送過來的資料類型
		String etCode = "message";
		String theQ = null;
		
		if (messaging.has("message")) {
			etCode = "message";
			eventData = (Map<String, Object>)messaging.get("message");
			
			if (eventData != null && eventData.containsKey("text")) {
				theQ = (String)eventData.get("text");
			}
		}
		else if (messaging.has("postback")) {
			etCode = "postback";
			eventData = (Map<String, Object>)messaging.get("postback");
		}
		
		if (eventData != null) {
			// eventData 有可能在 EventTypeRule 被使用。也會被放在 ServiceLogEntity 當中
			qaCtx.setRequestAttribute("eventData", eventData);
		}
		
		/* 暫時先 disable handover control 的所有判定
		if (messaging.has("referral")){
			JSONObject referral = messaging.getJSONObject("referral");
			if(referral.has("source") && referral.has("type")){
				if(referral.getString("source").equals("CUSTOMER_CHAT_PLUGIN") && 
						referral.getString("type").equals("OPEN_THREAD")){
					qStr = "小編機器人按鈕";
				}
			}
		}
		
		if (message.has("referral")){
			JSONObject referral = message.getJSONObject("referral");
			if(referral.has("source") && referral.has("type")){
				if(referral.getString("source").equals("CUSTOMER_CHAT_PLUGIN") && 
						referral.getString("type").equals("OPEN_THREAD")){
					qStr = "小編機器人按鈕";
				}
			}
		}
		if ((message == null || isStandbyEvent) && qStr == null) {
			
			if (messaging.has("pass_thread_control")) {
				Long newAppId = messaging.getJSONObject("pass_thread_control").getLong("new_owner_app_id");
				
				if (newAppId == 181381352427431L) { // 181381352427431L 是永豐證 Messenger Bot 的 App Id
					System.out.println("Got thread control passed from Page Inbox");
					//原本認知是 toUser or fromUser 其中一個會是「使用者」，一個是「Page Inbox」，但目前貌似 FB 有 bug
					//其中一個是「Page Inbox」，另一個是「按下 DONE 的小編」，因此無法判斷要轉給誰
					//String result = FacebookBotApi.passControlToPrimary(pageToken, fromUser).toString();
					//System.out.println(result);
				}
			}
			else if (isStandbyEvent) {
				System.out.println("In standby mode, got webhook event:" + messaging);
				if (messaging.has("postback")) {
			    	System.out.println("Ignore postback in standby event");
			    	continue;
			    }
				// 這裡是在小編狀態，但不用按鈕，直接打字的時候，會有 standby event 要處理
				else if (message != null && "芝麻開門".equals(messageText) && fromGateway == null) {
			    	JSONObject result = FacebookBotApi.passControlToPrimary(pageToken, fromUser);
				    if (!FacebookBotApi.checkSuccess(result)){
				    	continue;
				    }
				    System.out.println("芝麻開門（使用者直接打字）！切回機器人:" + result.toString());
				    qStr = messageText; 
		        }
			}
			else {
				System.out.println("Cannot deal with this webhook event:" + messaging);
				continue;
			}	
		}
		
		
		//交接通訊協定 (轉換小編 / 機器人)
		if (fromGateway == null) {
			try {
				if (messageText != null && messageText.equals("芝麻關門")){
					JSONObject result = FacebookBotApi.passControl(pageToken, fromUser);
					if (!FacebookBotApi.checkSuccess(result)){
						continue;
				    }
					System.out.println(result.toString());
					qStr = messageText; // 這個是轉小編之後，還是要先有一個問候語，因此拿 payload 來當作 Q 取得問候語
				} 
				else if (messageText != null && messageText.equals("芝麻開門")) {
			    		JSONObject result = FacebookBotApi.passControlToPrimary(pageToken, fromUser);
				    if (!FacebookBotApi.checkSuccess(result)){
				    	continue;
				    }
				    System.out.println("芝麻開門！切回機器人:" + result.toString());
				    qStr = messageText; // 這個是轉機器人之後，還是要先有一個問候語，因此拿 payload 來當作 Q 取得問候語
		        }
				// User傳送過來的訊息為附件(非文字，目前沒有特別處理)
				JSONArray attachments = message.optJSONArray("attachemnts");
				if (attachments != null) {
					etCode = ((JSONObject)attachments.get(0)).getString("type");
				}
			} catch (Exception e){
				System.out.println(e);
			}
		}
		*/
		
		qaCtx.setCtxAttr("MESSENGERID", fromUser);
		
		UserClue clue = qaCtx.getUserClue();
		if (clue == null) {
			clue = UserClue.getByFbPsId(t.getId(), fromUser);
			
			if (clue == null) {
				clue = new UserClue();
				clue.setTenantId(t.getId());
				clue.setFbPsId(fromUser);
				UserClue.saveOrUpdate(clue);
			}
			else {
				clue.setTenantId(t.getId());
				UserClue.saveOrUpdate(clue);	// 單純刷新 updateTime
			}
			qaCtx.setUserClue(clue);
		}
		
		qaCtx.setQaChannel(ch);
		qaCtx.setEservice(ch);
		qaCtx.setClientSupportHtml(false);
		EventType et = EventType.matchBestEventType(t.getId(), qaCtx.getQaChannel(), etCode);
		if (et == null) {
			System.out.println("Unknown EventType [" + etCode + "]...");
		}
		qaCtx.setEventType(et);
		
		boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
		
		JSONObject resp = null;
		JSONArray messages = new JSONArray();
		String tmpQ = null;
		
		if (!isStandbyEvent) {
			if (theQ != null) {
				theQ = StringUtils.lowerCase(StringUtils.trimToEmpty( theQ ));
			    
				// 有 Q 就跑答案
				if (StringUtils.isNotEmpty(theQ)) {
					if (explainQA) {
						qaCtx.appendExplain("Enter QA-AJAX", "Got Question from Messenger [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
							+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (fromUser != null ? "FROM (" + fromUser + ")" : ""));
					}
			
					qaCtx.setOriginalQuestion(theQ);
					qaCtx.setCurrentQuestion(theQ);
					
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
				        System.out.println("resp : " + resp.toString());
				        String forMessenger = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
				        System.out.println(forMessenger);
			        	
				        if (fromGateway == null) {
							if (StringUtils.trimToNull(forMessenger) != null) {
				        		FacebookBotApi.push(pageToken, fromUser, FacebookBotApi.buildTextMessage(forMessenger));
							}
					        	
					        if (resp.containsKey("messages")) {
								messages.addAll(resp.optJSONArray("messages", new JSONArray()));
					        			
				        		for (int i = 0; i < messages.size(); i++) {
				        			FacebookBotApi.push(pageToken, fromUser, messages.getJSONObject(i));
				        		}
					        }
					        	
					        // 附加參數
					        if (resp.containsKey("message")) {
					        	FacebookBotApi.push(pageToken, fromUser, resp.getJSONObject("message"));
					        }
				        } 
				        else {
				        	JSONObject sendUser = new JSONObject();
				        	sendUser.put("id", fromUser);
				        	
				        	if (StringUtils.trimToNull(forMessenger) != null) {
				        		JSONObject messenger = new JSONObject();
					        	messenger.put("recipient", sendUser);
					        	messenger.put("message", FacebookBotApi.buildTextMessage(forMessenger));
					        	JSONObject msg = new JSONObject();
					        	msg.put("messenger", messenger);
					        	preMessages.put(msg);
				        	}
				        	
					        if (resp.containsKey("messages")) {
								messages.addAll(resp.optJSONArray("messages", new JSONArray()));
				        		for (int i = 0; i < messages.size(); i++) {
				        			JSONObject messenger = new JSONObject();
						        	messenger.put("recipient", sendUser);
						        	messenger.put("message", messages.getJSONObject(i));
						        	JSONObject msg = new JSONObject();
						        	msg.put("messenger", messenger);
						        	preMessages.put(msg);
				        		}
					        }
					        	
					        // 附加參數
					        if (resp.containsKey("message")) {
					        	JSONObject messenger = new JSONObject();
					        	messenger.put("recipient", sendUser);
					        	messenger.put("message", resp.getJSONObject("message"));
					        	JSONObject msg = new JSONObject();
					        	msg.put("messenger", messenger);
					        	preMessages.put(msg);
					        }
				        }
					}
				}
			}
			else { // 沒有 theQ 一樣丟去問，因為現在有 EventTypeRule 的機制
		        QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
		        c.check(qaCtx);
		        resp = qaCtx.genResponseJSON();
		        System.out.println("resp : " + resp.toString());
		        String forMessenger = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
		        System.out.println(forMessenger);
	        	
		        if (fromGateway == null) {
					if (StringUtils.trimToNull(forMessenger) != null) {
		        		FacebookBotApi.push(pageToken, fromUser, FacebookBotApi.buildTextMessage(forMessenger));
					}
			        	
			        if (resp.containsKey("messages")) {
						messages.addAll(resp.optJSONArray("messages", new JSONArray()));
			        			
		        		for (int i = 0; i < messages.size(); i++) {
		        			FacebookBotApi.push(pageToken, fromUser, messages.getJSONObject(i));
		        		}
			        }
			        	
			        // 附加參數
			        if (resp.containsKey("message")) {
			        	FacebookBotApi.push(pageToken, fromUser, resp.getJSONObject("message"));
			        }
		        } 
		        else {
		        	JSONObject sendUser = new JSONObject();
		        	sendUser.put("id", fromUser);
		        	
		        	if (StringUtils.trimToNull(forMessenger) != null) {
		        		JSONObject messenger = new JSONObject();
			        	messenger.put("recipient", sendUser);
			        	messenger.put("message", FacebookBotApi.buildTextMessage(forMessenger));
			        	JSONObject msg = new JSONObject();
			        	msg.put("messenger", messenger);
			        	preMessages.put(msg);
		        	}
		        	
			        if (resp.containsKey("messages")) {
						messages.addAll(resp.optJSONArray("messages", new JSONArray()));
		        		for (int i = 0; i < messages.size(); i++) {
		        			JSONObject messenger = new JSONObject();
				        	messenger.put("recipient", sendUser);
				        	messenger.put("message", messages.getJSONObject(i));
				        	JSONObject msg = new JSONObject();
				        	msg.put("messenger", messenger);
				        	preMessages.put(msg);
		        		}
			        }
			        	
			        // 附加參數
			        if (resp.containsKey("message")) {
			        	JSONObject messenger = new JSONObject();
			        	messenger.put("recipient", sendUser);
			        	messenger.put("message", resp.getJSONObject("message"));
			        	JSONObject msg = new JSONObject();
			        	msg.put("messenger", messenger);
			        	preMessages.put(msg);
			        }
		        }
			}
			
			if (resp != null) {
				JSONObject conversation = qaCtx.getConversationsJson();
				conversation.getJSONArray("messages").put(resp);
				qaCtx.setConversations(conversation.toString());
				ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);
				
				if (testMode) {
					log = (ServiceLogEntity)session.getAttribute("TestModeServiceLogEntity");
					session.setAttribute("TestModeServiceLogEntity", log);
				} 
				else {
					if (log == null) {
					    log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", fromUser,
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
				
				    	ServiceLogEntity.save(log);
					
				}
			}
		}
	}
	catch (Exception e) {
		e.printStackTrace();
	}
	finally {
		QAContextManager.put(qaId, qaCtx);
	}
}

messengerResp.put("response", preMessages);

%><%= (fromGateway == null ? "{}" : messengerResp.toString(4)) %>
