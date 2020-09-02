<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page isELIgnored ="false" %>

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
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<HTML>
<HEAD>
<TITLE>新聞標題股票名稱及代號解析</TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript">
</script>
</HEAD>
<BODY>

<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/capitalNewsUploadFile">
    <input type="file" name="File1" size="20" maxlength="20"> <br><br>
    <input type="submit" value="開始處理" />
</form>

</BODY>
</HTML>
