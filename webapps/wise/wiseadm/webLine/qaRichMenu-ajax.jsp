<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.connector.webline.*"
import="com.intumit.solr.util.XssHttpServletRequestWrapper"
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");
JSONObject resp = new JSONObject();
String action = request.getParameter("action");
String type = request.getParameter("type");
String msgType = request.getParameter("msgType");
String msgStr = xssReq.getFakeParameter("msg");
String lineMessages = request.getParameter("lineMessages");
if ("update".equalsIgnoreCase(action) 
		&& "setDefaultMenu".equalsIgnoreCase(type)) {
	JSONObject msgs = new JSONObject(msgStr);
	String mkey = msgs.optString("mkey", "");
	RichMenu richMenu = RichMenu.getByMKey(t.getId(), mkey);
	if (richMenu != null) {
		richMenu.setBasicRichMenu(true);
		RichMenu.saveOrUpdate(richMenu);
		resp.put("message", "Enable Menu:" + mkey);
		resp.put("StatusCode", 200);
	}
} else if ("update".equalsIgnoreCase(action) 
		&& "cancelDefaultMenu".equalsIgnoreCase(type)) {
	JSONObject msgs = new JSONObject(msgStr);
	String mkey = msgs.optString("mkey", "");
	RichMenu richMenu = RichMenu.getByMKey(t.getId(), mkey);
	if (richMenu != null) {
		richMenu.setBasicRichMenu(false);
		RichMenu.saveOrUpdate(richMenu);
		resp.put("message", "Disable Menu:" + mkey);
		resp.put("StatusCode", 200);
	}
	
}
%><%= resp.toString(2) %>