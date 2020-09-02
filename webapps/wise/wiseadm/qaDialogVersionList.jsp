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
	<th>Version</th>
	<th>Comment</th>
	<th>Timestamp</th>
	<th>Modifier</th>
	<th><bean:message key='operation'/></th>
	</tr>
</thead>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String mkey = request.getParameter("mkey");
QADialogConfig cfg = QADialogConfig.getByKey(t.getId(), mkey);
List<QADialogConfigVersion> patterns = QADialogConfigVersion.listByKey(t.getId(), mkey);

if (request.getParameter("revertTo") != null) {
	try {
		int revertTo = Integer.parseInt(request.getParameter("revertTo"));
		cfg.setPublishedVersionNumber(revertTo);
		QADialogConfig.saveOrUpdate(cfg);
	}
	catch (Exception ignore) {}
}
int no = 1;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
for (QADialogConfigVersion p: patterns) {
%>
<tr>
	<td><%= p.getMkey() %> &nbsp;<span class='text-danger'><%= (cfg.getPublishedVersionNumber() == p.getVersionNumber()) ? "＊" : "" %></span></td>
	<td><strong><%= cfg.getDialogName() %></strong></td>
	<td><%= p.getVersionNumber() %></td>
	<td style="word-wrap:break-word;"><%= p.getVersionComment() %></td>
	<td><%= p.getDialogTimestamp() %></td>
	<td><%= AdminUser.getPrintableName(p.getContributor()) %></td>
	<td>
	<a href='qaDialogEditor.jsp?target=<%= p.getVersionNumber() %>&id=<%= cfg.getId() %>' class='btn btn-success' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span></a>
	<a href='qaDialogVersionList.jsp?revertTo=<%= p.getVersionNumber() %>&mkey=<%= mkey %>' class='btn btn-warning btnRevert' title="<bean:message key='global.revert'/>"><span class="glyphicon glyphicon-share-alt"></span></a>
	</td>
</tr>
<%
}
%>
</table>
</div>
<script>
$('.btnRevert').click(function() {
	if (confirm('Are you sure?')) {
		return true;
	}
	
	return false;
});
</script>
</body>
</html>