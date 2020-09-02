<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) {
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
<title><bean:message key='global.special.answer'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<table class='table table-striped table-hover '>
<thead>
	<tr>
	<th><bean:message key='global.mkey'/></th>
	<th><bean:message key='template.problem'/></th>
	<th><bean:message key='data.type'/></th>
	<th><bean:message key='max.number.pens'/></th>
	<th><bean:message key='special.restrictions'/></th>
	<th><bean:message key='global.standard.answer'/></th>
	<th><bean:message key='operation'/></th>
	</tr>
</thead>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<QAPattern> patterns = QAPattern.list(t.getId());
int no = 1;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
for (QAPattern p: patterns) {
%>
<tr>
	<td><%= p.getMkey() %></td>
	<td><%= p.getQuestionTemplate() %></td>
	<td><strong><%= p.getDataSource() %></strong><BR><%= p.getDataAggregator() %></td>
	<td><%= p.getMaxMatched() %></td>
	<td><%= StringUtils.defaultString(p.getSpecialRestriction(), MessageUtil.getMessage(locale, "global.without")) %></td>
	<td style="word-wrap:break-word;"><%= StringUtils.defaultString(p.getStaticAnswer(), MessageUtil.getMessage(locale, "global.without")) %></td>
	<td>
	<a href='qaPatternEdit.jsp?id=<%= p.getId() %>' class='btn btn-success'><bean:message key='modify'/></a>
	<a href='qaPatternSave.jsp?action=delete&id=<%= p.getId() %>' class='btn btn-danger'><bean:message key='delete'/></a>
	</td>
</tr>
<%
}
%>
</table>
	<a href='qaPatternEdit.jsp' class='btn btn-success'><bean:message key='global.add'/></a>
</div>
</body>
</html>