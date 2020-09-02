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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null) { 
	%>{"needLogin":true}<%
	return;
}

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	%>{"permissionDenied":true}<%
	return;
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

String idStr = request.getParameter("id");
String ltrStr = request.getParameter("ltr");
String lastRobotAnswerIdStr = request.getParameter("lastRobotAnswerId");
EvaluationLogEntity log = EvaluationLogEntity.get(new Integer(idStr));
if (log.getTenantId() != t.getId())
	return;

int ltr = new Integer(ltrStr);
Long actualKid = StringUtils.isNotEmpty(lastRobotAnswerIdStr) ? new Long(lastRobotAnswerIdStr) : null;

//com.intumit.syslog.SyslogEntity.log(request, "qa:robot", "UpdateEvaluationLogReviewStatus", "id=" + idStr + "&reviewStatus=" + statusStr, "");
log.setLastTestResult(ltr);
log.setLastRobotAnswerId(actualKid);
log.setLastTestResultTimestamp(Calendar.getInstance().getTime());
EvaluationLogEntity.update(log);

%>{"logId":<%= log.getId() %>}