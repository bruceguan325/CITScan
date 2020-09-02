<%@ include file="/commons/taglib.jsp"%>
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
	AdminLocationFacade.getInstance().save("127.0.0.1");
	%>
	<%= "New AdminLocation" %> created!
	<%
}
else if ("edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	AdminLocation admLoc = AdminLocationFacade.getInstance().get(new Integer(idStr));
	%>
    <bean:message key="edit.ip"/>
	<form action="locationAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-striped">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td>ID</td>
		<td width="300">
			<input type="hidden" name="id" value="<%= admLoc.getId() %>">
			<%= admLoc.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key="ip.range"/></td>
		<td width="300">
			<input type="text" size="40" name="ipAddress" value="<%= admLoc.getIpAddress() %>">
		</td>
		<td>
		(Ex. XXX.XXX.XXX.XXX)<br>
		(Ex. XXX.XXX.XXX)<br>
		(Ex. XXX.XXX)<br>
		(Ex. XXX)<br>
		</td>
	</tr>
	<tr>
		<td><bean:message key="global.account" /></td>
		<td width="300">
			<input type="text" size="40" name="loginName" value="<%= WiSeUtils.output(admLoc.getLoginName(), "", "", "") %>">
		</td>
		<td><bean:message key="global.account.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="web.site.rules"/></td>
		<td width="300">
			<input type="text" size="40" name="urlRegex" value="<%= WiSeUtils.output(admLoc.getUrlRegex(), "", "", "") %>">
		</td>
		<td><bean:message key="web.site.rules.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="not.to.login"/></td>
		<td width="300">
			TRUE：<input type="radio" name="noNeedLogin" value="true" <%= Boolean.TRUE.equals(admLoc.getNoNeedLogin()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="noNeedLogin" value="false" <%= Boolean.FALSE.equals(admLoc.getNoNeedLogin()) ? "checked" : "" %>>
		</td>
		<td><bean:message key="not.to.login.ex"/></td>
	</tr>
	<tr>
		<td>是否允許索引操作</td>
		<td width="300">
			TRUE：<input type="radio" name="allowCoreOperation" value="true" <%= Boolean.TRUE.equals(admLoc.getAllowCoreOperation()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="allowCoreOperation" value="false" <%= Boolean.FALSE.equals(admLoc.getAllowCoreOperation()) ? "checked" : "" %>>
		</td>
		<td></td>
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
	AdminLocation admLoc = new AdminLocation();
	if (idStr != null)
		admLoc.setId(new Integer(idStr));
	admLoc.setIpAddress(request.getParameter("ipAddress"));
	admLoc.setLoginName(request.getParameter("loginName"));
	admLoc.setUrlRegex(request.getParameter("urlRegex"));
	admLoc.setNoNeedLogin("true".equalsIgnoreCase(request.getParameter("noNeedLogin")));
	admLoc.setAllowCoreOperation("true".equalsIgnoreCase(request.getParameter("allowCoreOperation")));
	
	AdminLocationFacade.getInstance().saveOrUpdate(admLoc);
	%>
	<bean:message key="ip.range"/>(<%= admLoc.getIpAddress() %>)<bean:message key="already.submit"/>
	<%
}
else if ("delete".equalsIgnoreCase(action)) {
	AdminLocationFacade.getInstance().delete(new Integer(idStr));
	%>
	<bean:message key="ip.range"/>(<%= idStr %>)<bean:message key="already.remove"/>
	<%
}
else if ("check".equalsIgnoreCase(action)) {
	String ip = request.getParameter("ipAddress");
	String ln = request.getParameter("loginName");
	String url = request.getParameter("url");
	
	AdminLocation admLoc = AdminLocationFacade.getInstance().find(ip, ln);
	
	if (admLoc != null) {
		boolean match = false;
		if (admLoc.getUrlRegex() != null) {
			match = url.matches(admLoc.getUrlRegex());
		}
		%>
		<bean:message key="is.permission"/><%= match %><BR>
		<%= admLoc.getId() + ":" + admLoc.getIpAddress() + " / " + admLoc.getLoginName() + " / " + admLoc.getUrlRegex() + " / " + admLoc.getNoNeedLogin() %>
		<%
	}
	else {
		%><%= ln %><bean:message key="global.not.be"/><%= ip %><bean:message key="global.login"/><%	
	}
}
%>
<TABLE width="100%" class="table table-striped">
	<TR>
		<TH valign="top"><bean:message key="item"/></TH>
		<TH valign="top"><bean:message key="ip.range"/></TH>
		<TH valign="top"><bean:message key="free.login"/></TH>
		<TH valign="top">索引操作</TH>
		<TH valign="top"><bean:message key="account.limit"/></TH>
		<TH valign="top"><bean:message key="operation"/></TH>
	</TR>
<%
List<AdminLocation> list = AdminLocationFacade.getInstance().listAll();
for (int i=0; i < list.size(); i++) {
	AdminLocation admGrp = list.get(i);
	int id = admGrp.getId();
%>
	<TR>
		<TD align="center" valign="top" width="45"><%= i+1 %></TD>
		<TD align="center" valign="top"><%= admGrp.getIpAddress() %></TD>
		<TD align="center" valign="top"><%= admGrp.getNoNeedLogin() %></TD>
		<TD align="center" valign="top"><%= admGrp.getAllowCoreOperation() %></TD>
		<TD align="center" valign="top"><%= admGrp.getLoginName() %></TD>
		<TD align="center" valign="top" width="200">
			<a class="btn btn-primary" href="locationAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key="modify"/></a> 
			<a class="btn btn-danger" onclick="return confirm('<bean:message key="sure.del.ip.range"/>');" href="locationAdmin.jsp?action=DELETE&id=<%= id %>"><bean:message key="delete"/></a> 
			<a class="btn" onclick="return confirm('<bean:message key="sure.copy.ip.range"/>');" href="locationAdmin.jsp?action=DUPLICATE&id=<%= id %>"><bean:message key="copy"/></a>
		</TD>
	</TR>
<%
}
%>
</TABLE>
<form action="locationAdmin.jsp" name="myForm">
<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %>/wiseadm/locationAdmin.jsp?action=create"><bean:message key="create.new.ip.range"/></A>
<BR>
<BR>
<table>
	<tr>
	<td><bean:message key="global.ip"/></td>
	<td><input type="text" size="40" name="ipAddress" value="127.0.0.1"></td>
	</tr>
	<tr>
	<td><bean:message key="global.account"/></td>
	<td><input type="text" size="40" name="loginName" value="admin"></td>
	</tr>
	<tr>
	<td><bean:message key="web.site"/></td>
	<td><input type="text" size="40" name="url" value="/wiseadm/core0/select"></td>
	</tr>
	<tr>
	<td></td>
	<td><input class="btn btn-primary" type="submit" value="<bean:message key="global.detection"/>">
		<input type="hidden" name="action" value="check">
	</td>
	</tr>	
</table>
</form>
</BODY>
</HTML>
