<%@page import="org.apache.struts.Globals"%>
<%@ page pageEncoding="UTF-8" language="java" contentType="text/html; charset=UTF-8" %>
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
<%@ page import="com.intumit.hithot.HitHotLocale" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.robot.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ include file="/commons/taglib.jsp"%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE>SmartRobot 帳號綁定</TITLE>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap-theme.min.css" type="text/css" rel="stylesheet"/>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js" type="text/javascript"></script>
</HEAD>
<BODY style="font-size: 4.5em;">
<%
HitHotLocale[] availableLocales = new HitHotLocale[] { HitHotLocale.zh_TW, HitHotLocale.zh_CN };

String loginName = StringEscapeUtils.escapeHtml(request.getParameter("loginName"));
String password = StringEscapeUtils.escapeHtml(request.getParameter("password"));
String selectedHitHotLocaleStr = StringEscapeUtils.escapeHtml(request.getParameter("selectedLocale"));
Locale browserLocale = request.getLocale();
Locale sessionLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

if (selectedHitHotLocaleStr != null) {
	HitHotLocale hithotLocale = HitHotLocale.valueOf(selectedHitHotLocaleStr);

	if (hithotLocale != null) {
	    request.getSession().setAttribute("org.apache.struts.action.LOCALE", hithotLocale.getLocale());
	    Locale.setDefault(hithotLocale.getLocale());
	}
}
else if (sessionLocale != null) {
    Locale.setDefault(sessionLocale);
}
else if (browserLocale != null) {
    Locale.setDefault(browserLocale);
}

// 如果同一個 session 曾經驗證過影像密碼，則不需要重複驗證
boolean captchaValidated = true;//"TRUE".equals((String)session.getAttribute("captchaValidated"));
String msg = null;
boolean enableReferrerCheck = false;
String[] allowedReferers = {};

if (StringUtils.isNotEmpty(loginName) && StringUtils.isNotEmpty(password)) {
	Integer targetTenantId = (Integer)session.getAttribute("bind-tenantId");
	String userType = (String)session.getAttribute("bind-from");
	String userId = (String)session.getAttribute("bind-userId");

	if (!captchaValidated) {
		String captcha = request.getParameter("captcha");
		String rightCaptcha = com.intumit.solr.servlet.CaptchaServlet.getValidationCode(session, "captcha");
		captchaValidated = StringUtils.equalsIgnoreCase(captcha, rightCaptcha);
	}
	if (captchaValidated) {
		String referer = StringUtils.substringAfter(StringEscapeUtils.escapeHtml(request.getHeader("referer")), ":");
		AdminUser user = AdminUserFacade.getInstance().login(loginName, password);

		if (user == null) {
			msg = MessageUtil.getMessage(browserLocale, "admin.login.failed");
		}
		else if (user != null && (!enableReferrerCheck || StringUtils.startsWithAny(referer.trim(), allowedReferers))) {
			UserClue c = UserClue.getByAdminUserId(targetTenantId, user.getId());
			
			if (c == null) {
				c = new UserClue();
				c.setAdminUserId(user.getId());
				c.setTenantId(targetTenantId);
			}
			
			if (user.getTenantIdSet().contains(targetTenantId)) {
				if (StringUtils.equalsIgnoreCase(userType, "line")) {
					c.setLineUserId(userId);
					UserClue.saveOrUpdate(c);
					msg = "綁定成功！";
				}
				else {
					msg = "無法找到欲綁定的來源資訊";
				}
			}
			else {
				msg = "權限不足";
			}
		}
	}
	else {
		msg = MessageUtil.getMessage(browserLocale, "admin.login.code");
	}
	
	%>
	<div class="container-fluid">
	<div class="jumbotron">
	<%
	if (msg != null) {
	%>
	<div class="alert alert-danger" role="alert"><%= msg %></div>
	<%
	}
	%>
	</div>
	<%
}
else {
//response.setContentType("text/html");
Integer targetTenantId = Integer.parseInt(request.getParameter("tid"));
String userType = request.getParameter("from");
String userId = request.getParameter("userId");

session.setAttribute("bind-from", userType);
session.setAttribute("bind-userId", userId);
session.setAttribute("bind-tenantId", targetTenantId);

%>
<div class="container-fluid">
<div class="jumbotron">
<%
if (msg != null) {
%>
<div class="alert alert-danger" role="alert"><%= msg %></div>
<%
}
%>
<h1>SmartRobot 帳號綁定</h1>
<form action="bind-adm-user.jsp" METHOD="POST">
<div class="row">
<table class="table table-bordered">
<TR>
	<TD valign="top">
	<bean:message key="admin.login.account"/>
	</TD>
	<TD valign="top">
	<input type="text" name="loginName">
	</TD>
</TR>
<TR>
	<TD valign="top">
	<bean:message key="global.password"/>
	</TD>
	<TD valign="top">
	<input type="password" name="password" autocomplete="off">
	</TD>
</TR>
<TR>
	<TD valign="top">
		<bean:message key='login.prefer.locale'/>
	</TD>
	<TD valign="top">
		<SELECT name="selectedLocale">
			<% for (HitHotLocale locale: availableLocales) { boolean selected = locale.getLocale().equals(browserLocale); %>
			<OPTION value="<%= StringEscapeUtils.escapeHtml(locale.name()) %>" <%= selected ? "selected" : "" %>><%= locale.getText() %></OPTION>
			<% } %>
		</SELECT>
	</TD>
</TR>
<% if (!captchaValidated) { %>
<TR>
	<TD valign="top">
	<bean:message key="admin.login.enterCaptcha"/>
	</TD>
	<TD valign="top">
	<img src="<%= request.getContextPath() %>/captcha/<%= System.currentTimeMillis() %>.jpg">
	<input type="text" name="captcha">
	</TD>
</TR>
<% } %>
<TR>
	<TD valign="top">

	</TD>
	<TD valign="top">
	<button type="submit" class="btn btn-primary"><h1>綁定</h1></button>
	</TD>
</TR>
</TABLE>
</div>
</div>
</form>
</div>
</div>
<script>
$(document).ready(function() {
	var i = $("input[name='loginName']");
	if (i.val() == '') { i.focus(); }
	else {
		i = $("input[name='password']");
		if (i.val() == '') { i.focus(); }
		else {
			i = $("input[name='captcha']");
			if (i.length && i.val() == '') { i.focus(); }
			else {
				$("input[name='loginName']").focus();
			}
		}
	}
});
</script>
<%
}
%>
</BODY>
</HTML>
