<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
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
<%@ page import="org.apache.solr.client.solrj.request.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");

Set<String> allCoreNames = new TreeSet<String>(new Comparator() {

	public int compare(Object o1, Object o2) {
		String s1 = (String)o1;
		String s2 = (String)o2;
		
		Pattern p = Pattern.compile("core([0-9]+)");
		Matcher m1 = p.matcher(s1);
		Matcher m2 = p.matcher(s2);
		
		if (m1.find() && m2.find()) {
			return new Integer(m1.group(1)).compareTo(new Integer(m2.group(1)));
		}
		else 
			return s1.compareTo(s2);
	}
	
});

%>
<HTML>
<HEAD>
<TITLE>WiSe - Field Statistics</TITLE>
</HEAD>
<BODY>
<TABLE WIDTH="100%">
<TR>
<TD WIDTH="200" valign="TOP">
	<FORM>
	<b><bean:message key="column.statistics"/></b><BR>
	<% 
	allCoreNames.addAll(SearchManager.getAllCoreNames());
	for (String coreName : allCoreNames) {
	%>
		<A target="mainFrame" onclick="return confirm('<bean:message key="attention3"/>');" href="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/admin/luke?wt=xslt&tr=luke.xsl"><%= coreName %></A>
		<BR>
	<%
	} 
	%>
	</FORM>
</TD>
</TR>
</TABLE>
</BODY>
</HTML>
