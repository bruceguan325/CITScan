<%@page import="java.util.Locale"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
%>
<% 
String link = (String)request.getAttribute("link");
if(StringUtils.isNotBlank(link)){
	String answer = (String) request.getAttribute("answer");
	if(StringUtils.isNotBlank(answer)){
%>
<br/><br/>
<%
	}
	String [] args = {"_blank",StringEscapeUtils.escapeHtml(link)};
%>
<i><%=MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "link.preview", args) %></i>
<% 
}
%>