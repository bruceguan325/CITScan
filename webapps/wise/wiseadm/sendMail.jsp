<%@page import="java.io.FileWriter"%>
<%@page import="com.intumit.solr.util.EmailUtil"%>
<%@page import="com.intumit.solr.admin.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%
	String to = "";;
	String qaCategory = request.getParameter("qaCategory");
	String tid = request.getParameter("tid");
	if(qaCategory != null){
		to = AdminGroupFacade.getInstance().findAuditorByQaCategory(qaCategory, tid);
	}
	String subject = request.getParameter("subject");
	String text = request.getParameter("text");
	String msg = EmailUtil.sendmail(to, "", subject, text);
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
</body>
</html>