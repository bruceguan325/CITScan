<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ include file="/commons/taglib.jsp"%>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}
else if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
	&& AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0
	) {
	response.sendRedirect("qaDashboard.jsp");
	return;
}
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key="smart.robot"/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<STYLE>
body {
	margin: 0;
	padding-top: 0;
	overflow: hidden;
}

iframe {
	border: 0;
}

.menuFrame {
	height: 100%;
	width: 100%;
}

.mainFrame {
	height: 100%;
	width: 100%;
	overflow: hidden;
}

.bottomDiv {
	padding-top: 50px;
}

</STYLE>
</HEAD>
<BODY>
<div class="container-fluid">
<nav class="navbar navbar-inverse navbar-fixed-top" id="top-bar">
  <div class="container-fluid">
    <!-- Brand and toggle get grouped for better mobile display -->
    <div class="navbar-header">
      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#top-menu-buttons" aria-expanded="false">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
	  <a class="navbar-brand" href="index.jsp"><bean:message key="smart.robot.back.management"/></a>
    </div>

    <!-- Collect the nav links, forms, and other content for toggling -->
    <div class="collapse navbar-collapse" id="top-menu-buttons">
	    <jsp:include page="top.jsp" />
    </div><!-- /.navbar-collapse -->
  </div><!-- /.container-fluid -->
</nav>
	<div id="bottomDiv" class="row bottomDiv">
		<div class="col-xs-2" id="menuDiv" style="padding-right: 0px;">
		<iframe src="Blank.html" class='menuFrame' name="leftFrame" onLoad="iframeOnLoad(this.contentWindow.location);" scrolling="auto"></iframe>
		</div>
		<div class="col-xs-10" id="mainDiv" style="">
		
		<% if (!com.intumit.solr.util.WiSeEnv.isRobotIndexMode()) { %>
		<iframe src="dashboard.jsp" class="mainFrame" name="mainFrame" scrolling="auto"></iframe>
		<% } else { %>
		<iframe src="wise-admin.html#" class="mainFrame" name="mainFrame" scrolling="auto"></iframe>
		<% } %>
		</div>
	</div>
</div>
<SCRIPT>
function iframeOnLoad(loc) {
    var path = loc.pathname;
	if ( path.indexOf('statistics') != -1 ) {
		$('#menuDiv').removeClass();
		$('#menuDiv').addClass('col-xs-3');

		$('#mainDiv').removeClass();
		$('#mainDiv').addClass('col-xs-9');
	}
	else {
		$('#menuDiv').removeClass();
		$('#menuDiv').addClass('col-xs-2');

		$('#mainDiv').removeClass();
		$('#mainDiv').addClass('col-xs-10');
	}
}

$(document).ready(function() {
	$('#bottomDiv').css({"padding-top": $('#top-bar').height()});
	$('#bottomDiv').height($('#bottomDiv').height() - $('#top-bar').height());
	$('iframe').height($(window).height() - $('#top-bar').height());
});
</SCRIPT>
</BODY>
</HTML>
