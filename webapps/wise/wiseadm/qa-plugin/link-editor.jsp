<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
%>
<% 
String link = StringUtils.defaultString((String)request.getAttribute("link"));
%>
<input type="text" class="form-control" name="answer_link" value="<%= StringEscapeUtils.escapeHtml(link) %>"></input>