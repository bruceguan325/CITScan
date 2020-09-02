<%@ page pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.wink.json4j.*" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.user.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@ page
	import="com.intumit.solr.util.*"
	import="com.intumit.systemconfig.WiseSystemConfig"
%>
<%@ include file="/commons/taglib.jsp"%>
<%
User user = User.getFromSession(session);

if (user == null) {
	response.sendRedirect(request.getContextPath() + "/sign-in.jsp");
}

String idStr = request.getParameter("tid");
String redirectTo = request.getParameter("r");
Set<Integer> allowed = user.getTenantIdSet();

Tenant targetTanent = null;
if (StringUtils.isNotEmpty(idStr)) {
	targetTanent = Tenant.get(Integer.parseInt(idStr));
}
else if (allowed.size() == 1) {
	targetTanent = Tenant.get(allowed.iterator().next());
}

if (targetTanent != null && allowed.contains(targetTanent.getId())) {
	if (redirectTo != null) {
		response.sendRedirect(redirectTo + (StringUtils.contains(redirectTo, "?") ? "&" + targetTanent.getId() : "?") + "tid=" + targetTanent.getId());
	}
	else {
		if (targetTanent.getName().equalsIgnoreCase("opendata") )
			response.sendRedirect(request.getContextPath() + "/chats-t1.jsp?tid=" + targetTanent.getId());
		else
			response.sendRedirect(request.getContextPath() + "/chats.jsp?tid=" + targetTanent.getId());
	}
}

response.setContentType("text/html");
%>
<HTML>
<HEAD>
<TITLE>智能客服 - 選擇資料集</TITLE>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
</HEAD>
<BODY>
<div class="container">
<div class="hero-unit">
<h1><bean:message key="user.chooseTenant.title"/></h1>
<p>
<form action="chooseTenant.jsp" METHOD="GET">
<input type="hidden" name="redirectTo" value="<%= StringEscapeUtils.escapeHtml(StringUtils.defaultString(request.getParameter("r"))) %>" />
<TABLE style="width: 80%;" class="table table-bordered">
<TR>
	<TD valign="top">
	</TD>
	<TD valign="top">
	<select name="tid">
	<%
	for (Integer id: allowed) {
		Tenant t = Tenant.get(id);
	%>
	<option value="<%= t.getId() %>"><%= t.getNotes() %>(<%= t.getName() %>)</option>
	<%
	}
	%>
	</select>
	</TD>
</TR>
<TR>
	<TD valign="top">

	</TD>
	<TD valign="top">
	<input type="submit" class="btn" value="<bean:message key="global.submit"/>">
	</TD>
</TR>
</TABLE>
</form>
</div>
</div>
</BODY>
</HTML>
