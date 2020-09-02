<%@ include file="/commons/taglib.jsp"%>
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
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<style>
html,body{
background:url(nothing.txt) white fixed; /* prevent screen flash in IE6 */
margin:0; padding:0;
overflow: auto;
}

iframe {
height: 1024px;
width: 100%;
position:fixed;
top:0;
z-index:1;
_position:absolute; /* position fixed for IE6 */
_top: expression(eval(document.compatMode &&
            document.compatMode=='CSS1Compat') ?
            documentElement.scrollTop+15 :
            document.body.scrollTop );

}
</style>
</HEAD>
<BODY>
<CENTER>
<h3><bean:message key='top.system.management'/></h3>
<ul class="unstyled">
	<li><%
		if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) != 0) {
		%>
		<A HREF="syn" target="mainFrame"><bean:message key='global.synonyms'/><bean:message key='global.manage'/></A>
		<%
		}
		%>
	</li>
	<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) != 0) {
	%>
	<li><A HREF="ambiguity" target="mainFrame"><bean:message key='compulsory.division.set'/></A></li>
	<li><A HREF="white" target="mainFrame"><bean:message key='wikipedia.thesaurus'/><bean:message key='global.manage'/></A></li>
	<li><A HREF="black" target="mainFrame"><bean:message key='black.list'/><bean:message key='global.manage'/></A></li>
	<%
	}
	%>
	<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) != 0) {
	%>
	<li><A HREF="broadcast.jsp" target="mainFrame"><bean:message key="system.boardcast"/></A></li>
	<li><A HREF="recommend" target="mainFrame"><bean:message key='recommend.manage'/></A></li>
	<li><A HREF="wiseSystemConfig.jsp" target="mainFrame"><bean:message key='wisesystemconfig.set'/></A></li>
	<li><A HREF="column-name-mgt.jsp" target="mainFrame"><bean:message key='column.name.mgt.set'/></A></li>
	<li><A HREF="jobs/job-schedule.jsp" target="mainFrame"><bean:message key='job.schedule.manage'/></A></li>
	<li><A HREF="jobs/job-schedule-others.jsp" target="mainFrame"><bean:message key='job.schedule.others.manage'/></A></li>
	<%
	}
	%>
	<% if (SearchManager.isCloudMode() && AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0) { %>
	<li><A HREF="collectionsAdmin.jsp" target="mainFrame"><bean:message key='collectionsAdmin.manage'/></A></li>
	<% } %>
</ul>
</CENTER>
</BODY>
</HTML>
