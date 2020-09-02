<%@ page isErrorPage="true" contentType="text/html; charset=UTF-8"%>
<%!
boolean isDebugModeOrIpFromIntumit(String remoteAddr) {
	if ("127.0.0.1".equals(remoteAddr)
			|| "60.251.57.43".equals(remoteAddr)
			|| "60.251.57.44".equals(remoteAddr)
			|| "59.124.26.193".equals(remoteAddr)
			|| "59.124.26.194".equals(remoteAddr)
		){
		return true;
	}
	
	return false;
}
%>
<HTML>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Oops! Something Wrong!</title>
<link href="/styles/inettuts.css" rel="stylesheet" type="text/css" />
<style type="text/css">
.title {
	width: 747px;
	height: 80px;
	margin:auto;
}

.big-header {
	font-size: 3em;
}

.content_bg {
	width: 747px;
	height:326px;
	margin:auto;
	background-image:url(<%= request.getContextPath() %>/img/content.png);
	background-repeat: no-repeat;
}
.content {
	padding:60px;
}
.attention{
	height:58px;
	padding-left:60px;
	padding-top:20px;
	background-image:url(/img/attention.png);
	background-repeat: no-repeat;
}
.link a {
	color:#03C;
	text-decoration: none;
}

.link a:hover {
	text-decoration: underline;
	cursor: pointer;
}

h3 {
	font-size: 24px;
}

.list{
	font-size: 18px;
	list-style: disc;
	padding: 20px;
}
</style>
</head>
<%
String remoteAddr = com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request);
%>
<BODY>
<div id="wrapper" style="width: 960px; margin: 0 auto;">

<div style="height: 60px;"><br/></div>

<div class="title">
<h1 class="big-header">Captain, We Have a Problem!</h1>
</div>
<div class="content_bg">
<div class="content">
<div class="attention"><h3>Looks like something went wrong!</h3></div>
<div style="clear:both; margin-top:10px;">
We track these errors automatically, but if the problem persists feel free to contact us. In the meantime, try refreshing.
<br/>(Your IP has been logged: <%= remoteAddr %>)<br/>
<ul class="list link">
  <li><a href="<%= request.getContextPath() %>">Home</a></li>
  <li><a onclick='history.back()'>Go back to the previous page</a></li>
</ul>
</div>
</div>
<%
if (isDebugModeOrIpFromIntumit(remoteAddr)) { 
%>
	<BR><BR>
	<H1>Error Detail</H1>
	<BR>Your IP:  (<%= remoteAddr %>)
	<BR>Error Message is: <%=exception != null ? exception.getMessage() : "No Exception" %>
	<BR><BR>Stack Trace is :
	<PRE>
		<FONT COLOR="RED">
<%
	java.io.CharArrayWriter cw = new java.io.CharArrayWriter();
	java.io.PrintWriter pw = new java.io.PrintWriter(cw, true);
	if (exception != null) exception.printStackTrace(pw);
	out.println(cw.toString());
%>
		</FONT>
	</PRE>
	<BR>
<% } else { %>
	 
<% } %>
</div>
<div style="height: 60px; clear: both;"><br/></div>
</BODY>
</HTML> 
