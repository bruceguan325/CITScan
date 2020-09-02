<%@page import="org.apache.wink.json4j.JSONArray"%>
<%@page import="org.apache.wink.json4j.JSONObject"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
	import="java.util.*"
	import="com.intumit.solr.robot.ServiceLogEntity"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.systemconfig.WiseSystemConfig"
%>
<%

String apikey = request.getParameter("apikey");
String sessionid = request.getParameter("sessionid");
String feedback = request.getParameter("feedback");
String feedbackId = request.getParameter("feedbackId");

QAContext qaCtx = QAContextManager.lookup(sessionid);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(sessionid);
}

Tenant t = null;
if (apikey != null) {
	Apikey key = Apikey.getByApiKey(apikey);

	if (key != null && key.isValid()) {
	t = key.getTenant();

		if (t != null) {
			qaCtx.setTenant(t);
			qaCtx.setClientSupportHtml(false); // 透過 apikey 的預設 html=false，若有傳遞 html 後面會再覆蓋此值

			key.incCallCounter();
			Apikey.saveOrUpdate(key);
		}
	}
}

if (qaCtx.getTenant() == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");
	out.println(errorOutput.toString(2));
	return;
}
System.out.println("____________________________________");
System.out.println("tenant : " + qaCtx.getTenant().getId());
System.out.println("sessionid : " + sessionid);
System.out.println("feedback : " + feedback);
System.out.println("feedbackId : " + feedbackId);
System.out.println("____________________________________");
List<ServiceLogEntity> logEntitys = ServiceLogEntity.getFromQaId(qaCtx.getTenant().getId(), sessionid, t.getSessionExpirationSecond(), 100);
ServiceLogEntity log = null;
JSONObject currentMessage = null;
JSONObject result = new JSONObject();
if (logEntitys.isEmpty()) {
	result.put("status", "error");
	result.put("msg", "session time out or qaId is null");
    response.getWriter().append(result.toString());
	return;
}

try {
	boolean find = false;
	int index = 0;
	for (ServiceLogEntity logEntity : logEntitys) {
		JSONObject conversation = new JSONObject(logEntity.getConversations());
		if (conversation.has("messages")) {
	        JSONArray messages = new JSONArray();
	        try {
	            messages = conversation.getJSONArray("messages");
	        }
	        catch (Exception ex) {}
	        for (int i=0; i < messages.length(); i++) {
	        	Object obj = messages.get(i);
	        	if (obj instanceof JSONObject) {
	        		JSONObject msgs = (JSONObject)obj;
	        		if(msgs.has("feedbackId") && msgs.get("feedbackId").toString().equals(feedbackId)){
	        			log = logEntity;
	        			currentMessage = msgs;
	        			find = true;
	        			break;
	        		}
	        	}
	        	index++;
	        }
	        if (find)
	        	break;
	        index = 0;
	    }
	}
	if (log != null && currentMessage != null) {
		JSONObject conversations = new JSONObject(log.getConversations());
		JSONArray messages = conversations.getJSONArray("messages");
		currentMessage.put("feedback", (feedback != null || !feedback.equals(""))? feedback : "0");
		messages.remove(index);
		messages.add(index, currentMessage);
		log.setConversations(new JSONObject().put("messages", messages).toString());
		ServiceLogEntity.save(log);
		result.put("status", "success");
	}
	else {
		result.put("status", "MessageNotFound");
	}
} catch (Exception e) {
	e.printStackTrace();
}


%>
<%=result.toString(2)%>