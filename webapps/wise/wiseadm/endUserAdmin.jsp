<%@ include file="/commons/taglib.jsp"%>
<%@page import="org.apache.commons.validator.routines.EmailValidator"%>
<%@page import="com.intumit.solr.user.User"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.intumit.quartz.ScheduleUtils"%>
<%@page import="com.intumit.quartz.Job"%>

<%@page import="org.dom4j.*"%>
<%@page import="org.apache.commons.httpclient.methods.GetMethod"%>
<%@page import="org.apache.commons.httpclient.HttpClient"%>
<%@page import="org.apache.commons.httpclient.auth.AuthScope"%>
<%@page import="org.apache.commons.httpclient.UsernamePasswordCredentials"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script>
function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

</script>

</HEAD>
<BODY>
<%
String action = request.getParameter("action");
String idStr = request.getParameter("id");

if ("create".equalsIgnoreCase(action)) {
	
	User.save("New User");
	%>
	<%= "New User" %> created! <%
}
else if ("genapikey".equalsIgnoreCase(action)) {
	User admUser = User.get(new Integer(idStr));
	admUser.setApikey(User.genApikey());

	User.saveOrUpdate(admUser);
	%>
	<bean:message key="already.produce.enduser"/>(<%= admUser.getName() %>) APIKEY!
	<%
}
else if ("edit".equalsIgnoreCase(action)) {
	User admUser = User.get(new Integer(idStr));
	%>
	<bean:message key="edit.user"/>
	<form action="endUserAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-striped">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td><bean:message key="user.id"/></td>
		<td width="300">
			<input type="hidden" name="id" value="<%= admUser.getId() %>">
			<%= admUser.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key="user.name"/></td>
		<td width="300">
			<input type="text" size="40" name="name" value="<%= StringUtils.defaultString(admUser.getName()) %>">
		</td>
		<td><bean:message key="user.name.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="global.signin.email"/></td>
		<td width="300">
			<input type="text" size="40" name="loginName" value="<%= StringUtils.defaultString(admUser.getEmail()) %>">
		</td>
		<td><bean:message key="global.signin.pleaseEnterYourEmail"/></td>
	</tr>
	<tr>
		<td><bean:message key="user.password"/></td>
		<td width="300">
			<input type="password" size="40" name="password" value="">
		</td>
		<td><bean:message key="user.password.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="can.use.tenant.list"/></td>
		<td width="300">
			<input type="text" size="40" name="tenantIds" value="<%= StringUtils.trimToEmpty(admUser.getTenantIds()) %>">
		</td>
		<td><bean:message key="company.management.list.ex"/><br></td>
	</tr>
	<tr>
		<td></td>
		<td>
			<input class="btn btn-danger" type="submit" value="<bean:message key='submit'/>">
		</td>
		<td></td>
	</tr>
	</table>
	</form>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	User admUser = new User();
	if (idStr != null) {
		admUser = User.get(new Integer(idStr));
	}
	admUser.setName(request.getParameter("name"));
	admUser.setEmail(request.getParameter("loginName"));
	admUser.setTenantIds(request.getParameter("tenantIds"));

	if (EmailValidator.getInstance().isValid(admUser.getEmail())) {

		if (idStr != null && StringUtils.isEmpty(request.getParameter("password"))) {
			User oldUser = User.get(new Integer(idStr));
			admUser.setPassword(oldUser.getPassword());
		}
		else {
			admUser.setPassword(request.getParameter("password"));
		}
		User.saveOrUpdate(admUser);
		%>
		<bean:message key='user'/>(<%= admUser.getName() %>)<bean:message key='already.submit'/>
		<%
	}
	else {
		%>
		<bean:message key='user.email.illegal'/>(<%= admUser.getEmail() %>)!
		<%
	}
}
else if ("delete".equalsIgnoreCase(action)) {
	
	User.delete(new Integer(idStr));
	%>
	<bean:message key='user'/>(<%= idStr %>)<bean:message key='already.remove'/>
	<%
}
else if ("sudo".equalsIgnoreCase(action)) {
	User newUser = User.get(new Integer(idStr));
	User.setToSession(session, newUser);
	
	%>
	<bean:message key='already.transfiguration'/>(<%= newUser.getName() %>)<bean:message key='user'/>! <bean:message key='jump.front.desk'/>
	<script>
	setTimeout(function() {document.location='<%= request.getContextPath() %>/';}, 2000);
	</script>
	<%
}
%>
<TABLE width="100%" class="table table-striped">
	<THEAD>
	<TR>
		<TH valign="top"><bean:message key='item'/></TH>
		<TH valign="top"><bean:message key='user.name2'/></TH>
		<TH valign="top"><bean:message key='global.signin.email'/></TH>
		<TH valign="top"><bean:message key='tenant.apikey'/></TH>
		<TH valign="top"><bean:message key='operation'/></TH>
	</TR>
	</THEAD>
<%
List<User> list = User.list();
for (int i=0; i < list.size(); i++) {
	User endUser = list.get(i);
	int id = endUser.getId();
%>
	<TR>
		<TD align="center" valign="top" width="45"><%= i+1 %></TD>
		<TD align="center" valign="top"><%= endUser.getName() %></TD>
		<TD align="center" valign="top"><%= StringUtils.defaultString(endUser.getEmail()) %></TD>
		<TD align="center" valign="top"><%= StringUtils.defaultString(endUser.getApikey()) %></TD>
		<TD align="center" valign="top" width="300">
			<a class="btn btn-warn" href="endUserAdmin.jsp?action=SUDO&id=<%= id %>"><bean:message key='transfiguration'/></a>
			<a class="btn btn-primary" href="endUserAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key='modify'/></a>
			<a class="btn btn-primary" href="endUserAdmin.jsp?action=GENAPIKEY&id=<%= id %>"><bean:message key='production.apikey'/></a>
			<a class="btn btn-danger" onclick="return confirm('<bean:message key="sure.del.user"/>');" href="endUserAdmin.jsp?action=DELETE&id=<%= id %>"><bean:message key='delete'/></a>
		</TD>
	</TR>
<%
}
%>
</TABLE>
<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/endUserAdmin.jsp?action=create"><bean:message key="create.new.user"/></A>
</BODY>
</HTML>
