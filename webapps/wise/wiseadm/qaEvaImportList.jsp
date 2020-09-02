<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.QAEvaluationLogQueue.Status"
import="com.intumit.solr.util.XssHttpServletRequestWrapper"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
response.addHeader("Expires","-1");
response.addHeader("Pragma","no-cache");
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
String display = xssReq.getParameter("display");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<QAEvaluationLog> qaEvaLogs = QAEvaluationLog.list(t.getId(), display == null ? true : Boolean.valueOf(display));
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='top.eva.import.qa'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<script type="text/javascript"></script>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div id="container" class="container">
	<table class='table table-strip'>
	<thead>
		<tr>
		<th class='col-sm-1'>TimeStamp</th>
		<th class='col-sm-2'><bean:message key='global.name'/></th>
		<th class='col-sm-2'><bean:message key='evaluation.import.progress'/></th>
		<th class='col-sm-1'><bean:message key='evaluation.import.status'/></th>
		<th class='col-sm-3'><bean:message key='global.description'/></th>
		<th class='col-sm-2'><bean:message key='operation'/></th>
		</tr>
	</thead>
	<% for (QAEvaluationLog qaEvaLog: qaEvaLogs) { %>
	<tr>
		<td><%= qaEvaLog.getCreatedTime() %></td>
		<td><%= qaEvaLog.getInputFileName() %></td>
		<td>
			<div class="progress">
			  <div class="progress-bar" role="progressbar" style="width: <%= qaEvaLog.getProgress() %>%" aria-valuenow="<%= qaEvaLog.getProgress() %>" aria-valuemin="0" aria-valuemax="100">
			  <%= qaEvaLog.getProgress() %>%
			  </div>
			</div>
		</td>
		<td><%=  MessageUtil.getMessage(locale, qaEvaLog.getStatus().text) %></td>
		<td><%= qaEvaLog.getStatusMsg() == null ? MessageUtil.getMessage(locale, "evaluation.import.waiting") : qaEvaLog.getStatusMsg()%></td>
		<td>
			<% if (qaEvaLog.getReportFile() != null) { %>
				<a href='<%= request.getContextPath() %>/wiseadm/eatExport?evaLogId=<%= qaEvaLog.getId() %>&evaLogType=report' class='btn btn-default' title="<bean:message key='evaluation.import.report'/>"><span class="glyphicon glyphicon-tasks"></span></a>
			<% } %>
			<% if (qaEvaLog.getDetailFile() != null) { %>
				<a href='<%= request.getContextPath() %>/wiseadm/eatExport?evaLogId=<%= qaEvaLog.getId() %>&evaLogType=detail' class='btn btn-primary' title="<bean:message key='evaluation.import.detail'/>"><span class="glyphicon glyphicon-list-alt"></span></a>
			<% } %>
			<% if (qaEvaLog.isShowInList()) { %>
			<a href='<%= request.getContextPath() %>/wiseadm/eatExport?evaLogId=<%= qaEvaLog.getId() %>&evaLogType=hidden' class='btn btn-warning' title="<bean:message key='global.hidden'/>"><span class="glyphicon glyphicon-minus"></span></a>
			<a href='<%= request.getContextPath() %>/wiseadm/eatExport?evaLogId=<%= qaEvaLog.getId() %>&evaLogType=delete' class='btn btn-danger' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-remove"></span></a>
			<% } %>
		</td>
	</tr>
	<% } %>
	</table>
</div>
<script>
$( document ).ready(function() {
	setInterval(function(){
		$( "#container" ).load(window.location.href + " #container", function (response, status, xhr) {
			if (xhr.status != 200 || response.indexOf('<title><bean:message key="top.eva.import.qa"/></title>') == -1) {
				window.location.replace('login.jsp');
			}
        });
	},5000);
});
</script>
</body>
</html>