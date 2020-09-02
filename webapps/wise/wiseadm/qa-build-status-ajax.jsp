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
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String docId = request.getParameter("docId");
QAAltBuildQueue.Status status = QAAltBuildQueue.getStatus(t.getId(), docId);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) {
	%>{"docId": "<%= docId %>", "status": "<%= status.name() %>"}<%
}
else {
	%>{"needLogin":true}<%
	return;
}
%>
