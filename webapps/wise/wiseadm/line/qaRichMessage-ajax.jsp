<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.connector.line.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.qadialog.DialogLogEntry"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null) { 
	%>{"needLogin":true}<%
	return;
}

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) { 
	%>{"permissionDenied":true}<%
	return;
}
%><%! 

Object getRealFieldValue(SolrInputDocument doc, String name) {
	Object val = doc.containsKey(name) ? doc.get(name).getValue() : null;

	if (val instanceof Map) {
		Map m = (Map)val;

		if (m.containsKey("set")) {
			return m.get("set");
		}
		else if (m.containsKey("add")) {
			return m.get("add");
		}
		else {
			throw new RuntimeException("Unknown SolrInputDocument Field:" + m);
		}
	}
	else {
		return val;
	}
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
QAUtil qautil = QAUtil.getInstance(t);

response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");
JSONObject resp = new JSONObject();
String action = request.getParameter("action");
String msgType = request.getParameter("msgType");
String msgStr = request.getParameter("msg");
String lineMessages = request.getParameter("lineMessages");

if ("sendToMe".equals(action)) {
	String ac = LINEBotApi.getAccessToken(t.getId());
	UserClue uc = UserClue.getByAdminUserId(t.getId(), user.getId());
	
	if (uc != null && uc.getLineUserId() != null) {
		JSONArray messages = new JSONArray();
		String qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), "line"), uc.getLineUserId());
		QAContext ctx = QAContextManager.lookup(qaId);
		if (ctx == null) {
			ctx = QAContextManager.create(qaId);
		}
		ctx.setTenant(t);
		ctx.setUserClue(uc);
		
		if (lineMessages == null) {
			JSONObject jMsg = new JSONObject(msgStr);
			jMsg = new RichMessageServlet().pruneInvalidData(t, msgType, jMsg);
			RichMessage rm = new RichMessage();
			rm.setMsgTemplate(jMsg.toString());
			JSONObject msg = new JSONObject(rm.getMsgTemplate(ctx));
			messages.put(msg);
		} else {
			messages = new JSONArray(lineMessages);
		}
		System.out.println(new JSONArray(messages.toString()).toString(2));
		JSONObject result = LINEBotApi.push(ac, uc.getLineUserId(), messages);
		resp = result;
	}
	else {
		resp.put("message", "User not bind to any line uid");
		resp.put("StatusCode", 400);
	}
}%><%= resp.toString(2) %>