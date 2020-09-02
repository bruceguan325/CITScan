<%@page import="org.apache.struts.Globals"%>
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
<%@ page import="com.intumit.hithot.HitHotLocale" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="com.intumit.citi.CitiUtil" %>
<%@ include file="/commons/taglib.jsp"%>
<%
HitHotLocale[] availableLocales = new HitHotLocale[] { HitHotLocale.zh_TW, HitHotLocale.zh_CN, HitHotLocale.ja_JP };

String loginName = StringEscapeUtils.escapeHtml(request.getParameter("loginName"));
String password = StringEscapeUtils.escapeHtml(request.getParameter("password"));

Locale locale = HitHotLocale.determineLocale(request, true, true);


// 如果同一個 session 曾經驗證過影像密碼，則不需要重複驗證
boolean captchaValidated = true;//"TRUE".equals((String)session.getAttribute("captchaValidated"));
boolean outputJson = "json".equalsIgnoreCase(StringUtils.defaultString(request.getParameter("responseType"), "html"));
JSONObject json = new JSONObject();
String msg = null;
String redirectTo = null;
boolean enableReferrerCheck = false;
String[] allowedReferers =
	{
	 "//127.0.0.1",
	 "//13.75.47.116",
	 };


if (StringUtils.isNotEmpty(loginName) && StringUtils.isNotEmpty(password)) {

	if (!captchaValidated) {
		String captcha = request.getParameter("captcha");
		String rightCaptcha = com.intumit.solr.servlet.CaptchaServlet.getValidationCode(session, "captcha");
		captchaValidated = StringUtils.equalsIgnoreCase(captcha, rightCaptcha);
	}
	if (captchaValidated) {
		request.getSession(false).invalidate();
		session = request.getSession(true);
		
		String referer = StringUtils.substringAfter(StringEscapeUtils.escapeHtml(request.getHeader("referer")), ":");
		AdminUser user = AdminUserFacade.getInstance().login(loginName, password);

		if (user == null) {
			json.put("status", "failed");
			msg = MessageUtil.getMessage(locale, "admin.login.failed");
		}
		else if (user != null && (!enableReferrerCheck || StringUtils.startsWithAny(referer.trim(), allowedReferers))) {
			com.intumit.solr.robot.QAUtil.CURRENT_EVALUATION_LOG_NS = "qa:cathaybk:test:week1:";
			//com.intumit.solr.robot.QAUtil.getInstance();
		    session.invalidate();
		    session = request.getSession(true);
		    locale = HitHotLocale.determineLocale(request, true, true);

			AdminUserFacade.getInstance().setSession(session, user);
			session.setAttribute("captchaValidated", "TRUE");
			json.put("status", "ok");

			String redirectToParam = StringEscapeUtils.escapeHtml(request.getParameter("redirectTo"));
			if(StringUtils.isNotBlank(redirectToParam)){
				redirectTo = redirectToParam;
			}
			else if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0) {
				redirectTo = request.getContextPath() + "/wiseadm/index.jsp";
			}
			else if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) > 0 // 問答編輯權限
					|| (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0  // 匯出紀錄權限
					|| (AdminGroupFacade.getInstance().getFromSession(session).getStatisticsAdminCURD() & AdminGroup.R) > 0 // 統計權限
					) {
				redirectTo = request.getContextPath() + "/wiseadm/qaDashboard.jsp";
			}
			else if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.R) > 0) {
				redirectTo = "./test.jsp";
			}
			else {
				msg = MessageUtil.getMessage(locale, "admin.permission.denied");
			}
		}
	}
	else {
		json.put("status", "failed");
		msg = MessageUtil.getMessage(locale, "admin.login.code");
	}
}
else {
	Cookie hazelcast = new Cookie("hazelcast.sessionId", "");
    hazelcast.setHttpOnly(true);
    hazelcast.setPath(request.getContextPath());
    hazelcast.setMaxAge(0);
    response.addCookie(hazelcast);
    
    // 可能可以避免弱點掃描
    String ssoErrType = request.getParameter("ssoErrorType");
    msg = CitiUtil.getSsoErrorMessage(locale, ssoErrType);
}

if (!outputJson) {
	response.setContentType("text/html");

	if (redirectTo != null) {
			%>
			<script>
		window.location = "<%= redirectTo %>";
			</script>
			<%
		}
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key="admin.login.smartrobot"/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<style>
body {
	font-size: 23px;
	line-height: 33px;
	background: url(../img/login-bg.jpg) center center fixed no-repeat;
    background-attachment: fixed;
    background-size: cover;
}

.login-style-1 {
	margin:50px 0px;
}
.login-style-1 .ui-form {
	margin:30px auto;
	background:rgba(255,255,255,0.7);	
	padding:20px 25px;
	border:1px solid rgba(0,0,0,0.8);
	border-radius:5px;
	box-shadow:0px 0px 10px rgba(0,0,0,0.75);
}
.login-style-1 h1,h3 {
	margin-bottom:30px;
}
.login-style-1 .form-group {
	margin-bottom:20px;
}
.login-style-1 form .btn{
	border:0px;
	border-radius:5px;
	padding:10px 20px;
	border:1px solid rgba(0,0,0,0.5);
	color:#000;
	text-transform:uppercase;
	background:transparent;
	font-size: 0.9em;
	margin-top:15px;
	-webkit-transition: all 0.5s ease;
	-moz-transition: all 0.5s ease;
	-o-transition: all 0.5s ease;
	-ms-transition: all 0.5s ease;
	transition: all 0.5s ease;	
}
.login-style-1 form .btn:hover{
	background:#666;
	color:#fff;
}
</style>
</HEAD>
<BODY class="bg-blue">
<div class="container">
<div class="login-style-1">
<div class="col-md-offset-3 col-md-6 col-sm-12">
<div class="ui-form">
<h1 class="text-center"><bean:message key="admin.login.title"/></h1>
<%
if (StringUtils.isNotBlank(msg)) {
%>
<div class="alert alert-danger" role="alert"><%= msg %></div>
<%
}
%>
<form action="login.jsp" METHOD="POST">
<input type="hidden" name="redirectTo" value="<%= StringEscapeUtils.escapeHtml(StringUtils.defaultString(request.getParameter("r"))) %>" />
<div class="form-group">
	<input type="text" name="loginName" placeholder="<bean:message key="admin.login.account"/>" class="form-control">
</div>
<div class="form-group">
	<input type="password" name="password" placeholder="<bean:message key="global.password"/>" autocomplete="off" class="form-control">
</div>
<div class="form-group">
	<SELECT name="selectedLocale" class="form-control">
		<% for (HitHotLocale l: availableLocales) { boolean selected = l.getLocale().equals(locale); %>
		<OPTION value="<%= StringEscapeUtils.escapeHtml(l.name()) %>" <%= selected ? "selected" : "" %>><%= l.getText() %></OPTION>
		<% } %>
	</SELECT>
</div>

<% if (!captchaValidated) { %>
<div class="form-group">
	<img src="<%= request.getContextPath() %>/captcha/<%= System.currentTimeMillis() %>.jpg">
	<input type="text" name="captcha" placeholder="<bean:message key="admin.login.enterCaptcha"/>" class="form-control">
</div>
<% } %>
	<input type="submit" class="btn btn-block" value="<bean:message key="admin.login.submit"/>">
</form>
</div>
</div>
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
</BODY>
</HTML>
<%
}
else {
	if (msg != null)
		json.put("msg", msg);
	response.setContentType("application/json");
	out.println(json.toString(2));
}
%>
