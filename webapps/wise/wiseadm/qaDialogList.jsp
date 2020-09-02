<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='scenario.management'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<table class='table table-strip'>
<thead>
	<tr>
	<th><bean:message key='global.mkey'/></th>
	<th><bean:message key='global.name'/></th>
	<th><bean:message key='global.description'/></th>
	<th>Mode</th>
	<th>Published Version</th>
	<th>Timestamp</th>
	<th class='col-sm-3'><bean:message key='operation'/></th>
	</tr>
</thead>
<%
response.addHeader("Expires","-1");
response.addHeader("Pragma","no-cache");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<QADialogConfig> patterns = QADialogConfig.list(t.getId());
int no = 1;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
%>
<% for (QADialogConfig p: patterns) { %>
<tr>
	<td><%= p.getMkey() %></td>
	<td><strong><%= p.getDialogName() %></strong></td>
	<td style="word-wrap:break-word;"><%= p.getDialogDesc() %></td>
	<td>
		<% if (p.getOldMode()) { %>
		<span class='text text-danger'>OLD</span>
		<% } else { %>
		<span class='text text-success'>NEW</span>
		<% } %>
	</td>
	<td>
		<% if (p.getPublishedVersionNumber() > 0) { %>
		<%= p.getPublishedVersionNumber() %>
		<% } else { %>
		<bean:message key='global.draft'/>
		<% } %>
	</td>
	<td><%= p.getDialogTimestamp() %></td>
	<td>
		<% if (p.getOldMode()) { %>
			<a class='btn btn-danger btnDelete' data-id='<%= p.getId() %>' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-trash"></span></a>
		<% } else { %>
			<% if (p.hasDraft()) { %>
				<a href='qaDialogEditor.jsp?target=draft&id=<%= p.getId() %>' class='btn btn-warning' title="<bean:message key='modify'/><bean:message key='global.draft'/>"><span class="glyphicon glyphicon-hourglass"></span></a>
			<% } %>
			<a href='qaDialogEditor.jsp?id=<%= p.getId() %>' class='btn btn-success' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span></a>
			<a href='qaDialogEditor.jsp?action=copy&id=<%= p.getId() %>' class='btn btn-default' title="<bean:message key='copy'/>"><span class="glyphicon glyphicon-duplicate"></span></a>
			<a href='qaDialogVersionList.jsp?mkey=<%= p.getMkey() %>' class='btn btn-info' title="版本<bean:message key='global.list'/>"><span class="glyphicon glyphicon-time"></span></a>
			<a data-id='<%= p.getId() %>' class='btn btn-danger btnDelete' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-trash"></span></a>
		<% } %>
	</td>
</tr>
<% } %>
</table>
	<a href='qaDialogEditor.jsp' class='btn btn-success btnCreate'><bean:message key='global.add'/></a>
</div>
<script>
$('.btnDelete').click(function() {
	if (confirm('Are you sure?')) {
		$dataId = $(this).attr('data-id');
		
		$.ajax({
			url: "qaDialog-ajax.jsp",
			dataType: "JSON",
			data: {
				action: "delete",
				id: $dataId
			},
			success: function(result) {
				if (result.status == 'Done') {
					alert('Done');
				}
				
				location.reload();
			}
		});
	}
});
</script>
</body>
</html>