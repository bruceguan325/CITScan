<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.*"
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
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<title><bean:message key="top.question.method"/><bean:message key="global.list"/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">

<h3><bean:message key='alt.template.batch.import'/><br></h3> 

<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/qaAltTemplateFileUpload">
    <input type="file" name="File1" size="20" maxlength="20"> <br><br>
    <input type="submit"value="<bean:message key='global.import.csv'/>">
</form>

<table class='table table-strip'>
<thead>
	<tr>
	<th>No</th>
	<th><bean:message key="global.mkey"/></th>
	<th><bean:message key='template.name'/></th>
	<th><bean:message key='template.description'/></th>
	<th><bean:message key='priority.order'/></th>
	<th><bean:message key='proposed.logic'/></th>
	<th><bean:message key='operation'/></th>
	</tr>
</thead>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<QAAltTemplate> patterns = QAAltTemplate.list(t.getId());
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String without = MessageUtil.getMessage(locale, "global.without");
int no = 1;

for (QAAltTemplate p: patterns) {
%>
<tr>
	<td><%= no++ %></td>
	<td><%= p.getMkey() %></td>
	<td><%= p.getName() %></td>
	<td><%= WiSeUtils.nl2br(StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getDescription()))) %></td>
	<td><%= p.getPriority() %></td>
	<td><%= WiSeUtils.nl2br(StringEscapeUtils.escapeHtml(StringUtils.defaultString(StringUtils.substringBefore(p.getSuggestPatterns(), "\r"), without))) %></td>
	<td>
	<a href='qaAltTemplateEdit.jsp?id=<%= p.getId() %>' class='btn btn-success'><bean:message key='modify'/></a>
	<a href='qaAltTemplateSave.jsp?action=delete&id=<%= p.getId() %>' class='btn btn-danger'><bean:message key='delete'/></a>
	</td>
</tr>
<%
}
%>
</table>
	<a href='qaAltTemplateEdit.jsp' class='btn btn-success'><bean:message key='global.add'/></a>
</div>
</body>
</html>