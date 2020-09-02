<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.ServletConfig"
	 import="javax.servlet.ServletException"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="org.json.*"
	 import="org.apache.commons.io.*"
	 import="org.apache.commons.httpclient.*"
	 import="org.apache.commons.httpclient.methods.*"
	 import="org.apache.commons.httpclient.params.HttpMethodParams"
	 import="org.apache.commons.lang.*"
	 import="org.apache.solr.core.*"
	 import="org.apache.solr.servlet.*"
	 import="org.apache.solr.client.solrj.*"
	 import="org.apache.solr.client.solrj.embedded.*"
	 import="org.apache.solr.client.solrj.response.*"
	 import="org.apache.solr.common.*"
	 import="com.intumit.solr.*"
	 import="com.intumit.solr.tenant.*"
	 import="com.intumit.solr.util.*"
	 import="com.intumit.solr.robot.qarule.*"
	 import="com.intumit.systemconfig.*"
	import="com.intumit.solr.robot.entity.*"
	import="com.intumit.solr.admin.*"
%><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String idStr = request.getParameter("id");
String category = request.getParameter("category");
String code = request.getParameter("code");
String action = StringUtils.defaultString(request.getParameter("action"), "add");

if ("add".equalsIgnoreCase(action)) {
	QAEntity.save(t.getId(), category, code, code, null, QAEntityType.STRING, "", true, true);
	com.intumit.solr.robot.NotificationServiceServlet.doNotification(t, NotificationEvent.NotificationType.SUCCESS, "QAEntity Added", category + " => " + code);
	JSONObject resp = new JSONObject();
	resp.put("status", "Done");
	out.println(resp.toString(2));
}
else if ("save".equalsIgnoreCase(action)) {

	JSONObject resp = new JSONObject();
	resp.put("status", "Done");
	out.println(resp.toString(2));
}
%>