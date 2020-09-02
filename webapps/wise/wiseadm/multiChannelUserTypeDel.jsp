<%@page language="java" pageEncoding="UTF-8" contentType="application/json"

import="org.apache.wink.json4j.*"
import="java.util.HashMap"
import="java.util.Map"
import="java.util.ArrayList"
import="java.util.List"
import="java.io.*"
import="com.intumit.solr.robot.QAChannel"
import="com.intumit.solr.robot.QAUserType"
import="com.intumit.solr.robot.MultiChannelAnswer"
import="java.util.Locale"
import="org.apache.struts.Globals"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<%
int tenantId = Integer.valueOf(request.getParameter("tenantId"));
String type = request.getParameter("type");
int id = Integer.valueOf(request.getParameter("id"));

System.out.println( "del attr tenantId:" + tenantId + " type:"+type + " id:" + id );

Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);

JSONObject json = new JSONObject();
Map<String, Object> resultData = new HashMap<String, Object>();
if (type.equals("channel")) {
	
	QAChannel delChannel = QAChannel.get(id);
	if (delChannel.getCode().equals("web")||delChannel.getCode().equals("app")) {
		resultData.put("fail", "Unable to delete channel");
		json.putAll( resultData );
		response.getWriter().println(json.toString(4));
		return;
	}
	
	QAChannel.delete(delChannel);
	MultiChannelAnswer.disableAnswer(delChannel.getCode(), null);
	
	JSONArray channelArray = new JSONArray(QAChannel.getArrayData(tenantId));
	resultData.put("channel", channelArray);
	
	json.putAll( resultData );
}

if (type.equals("userType")) {
	
	QAUserType delUserType = QAUserType.get(id);
	if (delUserType.getCode().equals("unknown")) {
		resultData.put("fail", "ä¸å¯åªé¤æ­¤userType");
		json.putAll( resultData );
		response.getWriter().println(json.toString(4));
		return;
	}
	
	QAUserType.delete(delUserType);
	MultiChannelAnswer.disableAnswer(null, delUserType.getCode());
	
	JSONArray userTypeArray = new JSONArray(QAUserType.getArrayData(tenantId, locale));
	resultData.put("userType", userTypeArray);
	
	json.putAll( resultData );
}
%>
<%= json.toString(4) %>
