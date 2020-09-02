<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.List"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.hithot.HitHotLocale"
	import="com.intumit.message.MessageUtil"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.WiSeUtils"
	import="com.intumit.systemconfig.*"
	import="org.apache.struts.Globals"
%><%

String r = request.getParameter("r");
String goodUrl = null;

try {
	java.net.URL u = new java.net.URL(r);
	goodUrl = r;
}
catch (Exception ex) {
	goodUrl = StringEscapeUtils.unescapeJavaScript(r);
	ex.printStackTrace();
}
%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    <title>Attention Please!</title>
    <jsp:include page="header-opinion.jsp" />
</head>
<body class='contrast-muted '>

    <div class='container'>
        <div class='row' id='content-wrapper'>
            <div class='span12'>
            <center>
			<h3>注意！您即將離開本系統，連往外部網站</h3><h6><%= StringUtils.trimToEmpty(goodUrl) %></h6>
			<br>
			<a class="btn btn-primary" href="<%= goodUrl %>">確定前往</a>
			&nbsp;
			<a class="btn btn-warning" href="#">放棄</a>
			</center>
            </div>
        </div>
    </div>

<jsp:include page="footer-opinion.jsp" />
</body>
</html>
