<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="application/json"
	import="java.io.*"
	import="java.util.*"
	import="org.apache.commons.lang.*"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.tenant.*"
%><%
String qaId = request.getParameter("id");
if (StringUtils.isBlank(qaId)) {
	qaId = java.util.UUID.randomUUID().toString();
}

boolean testMode = false;
if (request.getParameter("testMode") != null) {
	if (Boolean.parseBoolean(request.getParameter("testMode")))
	{
		testMode = true;
	}
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}

qaCtx.setClientSupportHtml(true);
String apikey = request.getParameter("apikey");
Tenant t = null;
if (apikey != null) {
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;

	if (t != null) {
		qaCtx.setTenant(t);
		qaCtx.setClientSupportHtml(false);
	}
}
else if (testMode && request.getParameter("tid") != null) {
	t = Tenant.get(Integer.parseInt(request.getParameter("tid")));
	if (t != null) {
		qaCtx.setTenant(t);
	}
}

if (qaCtx.getTenant() == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");

	out.println(errorOutput.toString(2));

	return;
}

List<ServiceLogEntity> logEntitys = ServiceLogEntity.getFromQaId(t.getId(), qaId, t.getSessionExpirationSecond(), null);
JSONObject resp = new JSONObject();

// ServiceLogEntity.getFromQaId 改成用 timestamp 降冪 (新到舊) 排序
for (int i=logEntitys.size() - 1; i >= 0; i--) {
	ServiceLogEntity logEntity = logEntitys.get(i);
	JSONObject conversation = new JSONObject(logEntity.getConversations());
	
	if (conversation.has("messages")) {
        JSONArray messages = new JSONArray();
        try {
            messages = conversation.getJSONArray("messages");
        }
        catch (Exception ex) {}
            
        for (int j=0; j < messages.length(); j++) {
        	Object obj = messages.get(j);
        	if (obj instanceof JSONObject) {
        		JSONObject msg = (JSONObject)obj;
        		resp.append("msgs", msg);
        	}
        }
    }
}
%><%= resp.toString(4) %>
