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
<title>Page Not Found</title>
<style type="text/css">
.title {
	width: 747px;
	height: 150px;
	margin:auto;
}

.big-header {
	padding-left: 20px;
	font-size: 6em;
}

.big-sub-header {
	padding-left: 20px;
	font-size: 4em;
	color: grey;
}

.content_bg {
	height:326px;
	margin: 0px 120px 0px 0px;
	background-image:url(<%= request.getContextPath() %>/img/content.png);
	background-size: 100% 100%;
	background-repeat: no-repeat;
}
.content {
	padding: 50px 20px 50px 20px;
}
.attention{
	height:58px;
	padding-left:60px;
	padding-top:20px;
	background-image:url(<%= request.getContextPath() %>/img/attention.png);
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


/* Reset */
body,img,p,h1,h2,h3,h4,h5,h6,ul,ol {margin:0; padding:0; list-style:none; border:none;}
/* End Reset */

body {font-size:0.8em; font-family:Arial,Verdana,Sans-Serif; background: #f4f2ef;}
/*a {color:black;}*/

/* Head section */
#head {
	line-height: 50px;
    color: #FFF;
    text-align: center;
    width: 100%;
    background-color: #ffffff;
}
#head2 {
	line-height: 50px;
    color: #FFF;
    text-align: center;
    width: 100%;	
}
#head h1 {
    line-height: 100px;
    color: #FFF;
    text-align: center;
    background: url(http://skins.gmodules.com/ig/skin_fetch?fp=rQ7zQA&type=2&sfkey=http://ighosting.googlecode.com/svn/trunk/images/travel_3dcities/header1_tile.jpg) no-repeat center;
    text-indent: -9999em
}
/* End Head Section */

/* Columns section */
#columns .column {
    float: left;
    /*width: 49%;*/
		/* Min-height: */
		min-height: 400px;
		height: auto !important; 
		height: 400px;
}

#columns #column1 {	width: 55%;}
#columns #column2 {	width: 35%;}
#columns #column3 {	width: 10%;}
#columns #column1 .widget {	margin: 0 5px 0 5px;}
#columns #column2 .widget { margin: 0 0 0 0;}
#columns #column3 .widget { margin: 0 0 0 0;}
#columns .widget {
    margin: 10px 10px 0 10px;
    padding: 2px;
}

.bg {
	background: #D8D8D8 url(<%= request.getContextPath() %>/img/err_bg.jpg) center center fixed no-repeat;
	padding: 5px;
}

#columns .widget .widget-head {
    color: #000;
    overflow: hidden;
    height: 30px;
    line-height: 30px;
}
#columns .widget .widget-head h3 {
    padding: 0 5px;
    float: left;
}
#columns .widget .widget-content {
   /* background: #333 url(img/widget-content-bg.png) repeat-x;*/
   	background-color:white;
    padding: 5px 5px;
    color: #000000;
    -moz-border-radius-bottomleft: 2px;
    -moz-border-radius-bottomright: 2px;
    -webkit-border-bottom-left-radius: 2px;
    -webkit-border-bottom-right-radius: 2px;
    line-height: 1.2em;
    overflow: hidden;
    border: 1px solid #D8D8D8;
}
#columns .widget .widget-content p {
    padding: 0.8em 0;
}
#columns .widget .widget-content img {
    margin: 10px;
}
#columns .widget .widget-content pre {
    padding: 0.5em 5px;
    color: #EEE;
    font-size: 12px;
}
#columns .widget .widget-content ul {
    padding: 5px 0 5px 0px;
    list-style: disc;
}
#columns .widget .widget-content ul li {padding: 3px 0;}
#columns .widget .widget-content ul.images {
    padding: 7px 0 0 0;
    list-style: none;
    height: 1%;
}
#columns .widget .widget-content ul.images li {
    display: inline;
    float: left;
}
#columns .widget .widget-content ul.images img {
    display: inline;
    float: left;
    margin: 0 0 7px 7px;
}
/* End Columns section */

#feature_list
{
	border: 1px;
	border-style: solid;
	border-color: #ccc;
}

/* Foot section */
#foot {
	clear: both;
    text-align: center;
    width: 100%;
}
/* End Foot Section */


</style>
</head>
<%
String remoteAddr = com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request);
%>
<BODY class="bg">
<div id="wrapper" style="width: 1024px; margin: 0 auto;">


<div id="wrapper-left" style="width: 700px;">
<div style="height: 120px;"><br/></div>

<div class="title">
<h1 class="big-header">404 ERROR</h1>
<h1 class="big-sub-header">Page Not Found!</h1>
</div>
<div class="content_bg">
<div class="content">
<div class="attention"><h3>The page you requested was not found.</h3></div>
<div style="clear:both; margin-top:10px;">
You may have clicked an expired link or mistyped the address. Some web addresses are case sensitive.
<br/>(<%= remoteAddr %>)<br/>
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
</div>
<div style="height: 60px; clear: both;"><br/></div>
</BODY>
</HTML> 
