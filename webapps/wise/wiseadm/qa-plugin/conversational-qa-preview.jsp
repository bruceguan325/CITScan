<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qaplugin.*"
%>
<%
String customQaPatternMkey = (String)request.getAttribute(CustomQA.QA_PATTERN_MKEY_PARAM);
%><i><bean:message key='special.problems.can.not.directly.preview'/></i>
