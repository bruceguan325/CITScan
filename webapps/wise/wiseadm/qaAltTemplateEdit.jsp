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
<title><bean:message key="global.edit"/><bean:message key="top.question.method"/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<form id="thisForm" action="qaAltTemplateSave.jsp" method="post">
<input type="hidden" value="save" name="action" />
<table class='table table-bordered' style="width: 90%;">
<thead>
	<tr>
	</tr>
</thead>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String id = request.getParameter("id");
QAAltTemplate p = null;

if (id == null) {
	p = new QAAltTemplate();
	p.setTenantId(t.getId());
}
else {
	p = QAAltTemplate.get(new Integer(id));
	if (p.getTenantId() != t.getId())
		return;
}
%>
<tr>
	<th><bean:message key='global.id'/></th>
	<td>
		<% if (id != null) { %>
		<input type="hidden" name="id" value="<%= p.getId() %>">
		<% } %>
	</td>
</tr>
<tr>
	<th><bean:message key='template.name'/></th>
	<td>
	<input type='text' class='form-control' name="name"
		value='<%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getName())) %>'
		>
		<ul>
			<li><bean:message key='template.name.ex'/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key="global.mkey"/></th>
	<td>
	<input type='text' class='form-control' name="mkey"
		value='<%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getMkey())) %>'
		>
		<ul>
			<li><bean:message key="mkey.desc1"/></li>
			<li class='text-danger'><bean:message key="mkey.desc2"/></li>
			<li><bean:message key="mkey.desc3"/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key='template.description'/></th>
	<td>
	<textarea class='form-control' name="description"><%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getDescription())) %></textarea>
		<ul>
			<li><bean:message key='template.description.ex'/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key='top.question.method'/></th>
	<td>
	<textarea class='form-control' name="template" rows="10"><%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getTemplate())) %></textarea>
		<ul>
			<li>{{KEYWORD}}，若有多個依序取名 {{KEYWORD2}} {{KEYWORD3}} ...</li>
			<li><bean:message key='top.question.method.ex'/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key='proposed.logic'/></th>
	<td>
	<textarea class='form-control' name="suggestPatterns" rows="10"><%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(p.getSuggestPatterns())) %></textarea>
		<ul>
			<li><bean:message key='proposed.logic.ex'/></li>
			<li><bean:message key='proposed.logic.ex1'/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key='priority.order'/></th>
	<td>
	<input type='text' class='form-control' name="priority"
		value='<%= p.getPriority() %>'
		>
		<ul>
			<li><bean:message key='priority.order.ex'/></li>
		</ul>
	</td>
</tr>
<tr>
	<th><bean:message key='operation'/></th>
	<td>
	<button type="submit" class='btn btn-success'><bean:message key='submit'/></button>
	<button id="saveAs" class='btn btn-warning'><bean:message key='save.a.new.file'/></button>
	<button type="reset" class='btn btn-danger'><bean:message key='global.give.up'/></button>
	</td>
</tr>
</table>
</form>
</div>
<script>
$("#saveAs").click(function() {
	$('input[name="id"]').remove();
	$('#thisForm').submit();
});
</script>
</body>
</html>