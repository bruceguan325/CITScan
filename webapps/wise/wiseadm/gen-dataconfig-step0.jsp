<%@ include file="/commons/taglib.jsp"%>
<%@ page
		contentType="text/html;charset=UTF-8"
        pageEncoding="UTF-8"
        language="java"

		import="java.util.HashMap"
		import="java.io.File"
		import="java.sql.*"
		import="java.util.*"
		import="com.intumit.solr.*"
		import="com.intumit.solr.dataimport.*"
		import="com.intumit.solr.util.VelocityManager"
		import="org.apache.velocity.VelocityContext"
		import="org.apache.commons.lang.StringUtils"
		import="org.apache.commons.io.FileUtils"
		import="org.apache.commons.lang.StringEscapeUtils"
		import="java.util.regex.Pattern"
		import="java.util.regex.Matcher" %>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}

String targetCore = request.getParameter("coreName");
%>
<html>
<head>
<title>Data Configuration</title>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bootstrap-responsive.min.css" type="text/css" rel="stylesheet"/>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap.min.js" type="text/javascript"></script>
<style>
.my-block {
	height: 520px;
}
.my-block .description {
	height: 120px;
}
.my-block .img-block {
	height: 200px;
}
.img-block img {
	height: 100%;
}
</style>
</head>
<body>
<div class="container-fluid">
	<div class="row-fluid">
		<div class="span4 well my-block">
			<h2>
				Database
			</h2>
			<p class="description">
				<bean:message key="database.set"/>
			</p>
			<p class="img-block">
				<img src="<%= request.getContextPath() %>/img/dataconfig-database.jpg">
			</p>
			<p>
				<a class="btn" href="gen-dataconfig-db.jsp?step=1&coreName=<%= targetCore %>">Go & Configure »</a>
			</p>
		</div>
		<div class="span4 well my-block">
			<h2>
				File System
			</h2>
			<p class="description">
				<bean:message key="file.system.set"/>
			</p>
			<p class="img-block">
				<img src="<%= request.getContextPath() %>/img/dataconfig-filesystem.jpg">
			</p>
			<p>
				<a class="btn" href="gen-dataconfig-fs.jsp?step=1&coreName=<%= targetCore %>">Go & Configure »</a>
			</p>
		</div>
		<div class="span4 well my-block">
			<h2>
				RSS
			</h2>
			<p class="description">
				<bean:message key="rss.set"/>
			</p>
			<p class="img-block">
				<img src="<%= request.getContextPath() %>/img/dataconfig-RSS.jpg">
			</p>
			<p>
				<a class="btn" href="gen-dataconfig-rss.jsp?step=1&coreName=<%= targetCore %>">Go & Configure »</a>
			</p>
		</div>
	</div>
</div>
</body>
</html>
