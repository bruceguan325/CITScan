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
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
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
<h3><bean:message key="authority.management"/></h3>
<ul class="unstyled">
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/userAdmin.jsp"><bean:message key="user.management"/></A></li>
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/tenantAdmin.jsp"><bean:message key="company.management"/></A></li>
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/endUserAdmin.jsp"><bean:message key="reception.user.management"/></A></li>
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/groupAdmin.jsp"><bean:message key="group.management"/></A></li>
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/locationAdmin.jsp"><bean:message key="address.management"/></A></li>
</ul>
</CENTER>
</BODY>
</HTML>
