<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="com.intumit.solr.user.*"
	import="com.intumit.syslog.*"
	import="java.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.syslog.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    
    <jsp:include page="../header-opinion.jsp" />
	<script src='<%= request.getContextPath() %>/script/d3.v3.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/script/nv.d3.min.js' type='text/javascript'></script>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/styles/nv.d3.min.css" type="text/css" media="all"/>
    <style>
    label {display: inline;}
    </style>
</head>
<body class='contrast-fb'>
<%
List<SyslogEntity> logs = SyslogEntity.listByNS("frontend:user", 50);
%>
<h4>
<table class="table table-striped table-bordered">
<% for (SyslogEntity log: logs) { %>
    <tr>
    <td class="span1">
    <i class='icon-user text-success'></i>
    <%= log.getIdentity() %> 
    </td>
    <td class="span1">
    <%= log.getEvent() %>
    </td>
    <td class="span1">
    <%= log.getStatusMessage() %>
    </td>
    <td class="span2">
    <small class='timeago muted' title='<%= log.getTimestamp() %>'><%= log.getTimestamp() %></small>
    </td>
    <td class="span1">
    <small class='text-error'>(<%= log.getClientIp() %>)</small>
    </td>
    <td class="span6">
    <%= log.getParameters() %>
    </td>
    </tr>
<% } %>
</table>
</h4>
<div class='divider'></div>
</body>
</html>