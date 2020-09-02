<%@page language="java" contentType="application/json"

import="org.apache.wink.json4j.*"
import="java.util.HashMap"
import="java.util.Map"
import="java.util.ArrayList"
import="java.util.List"
import="java.io.*"
import="com.intumit.solr.robot.QAChannel"
import="com.intumit.solr.robot.QAUserType"
import="org.apache.commons.lang.StringUtils"
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
String tenantIdStr = request.getParameter("tenantId");
if(StringUtils.isBlank(tenantIdStr)) return;
int tenantId = Integer.valueOf(tenantIdStr);

Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
Map<String, Object> resultData = new HashMap<String, Object>();
JSONArray channelArray = new JSONArray(QAChannel.getArrayData(tenantId));
resultData.put("channel", channelArray);

JSONArray userTypeArray = new JSONArray(QAUserType.getArrayData(tenantId, locale));
resultData.put("userType", userTypeArray);

JSONObject json = new JSONObject();
json.putAll( resultData );
%>
<%= json.toString(4) %>