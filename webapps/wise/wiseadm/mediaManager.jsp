<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.3.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/script/jmesa.css" />

<title>多媒體檔案管理  </title>
<script type="text/javascript">



function onInvokeAction(id) {
    setExportToLimit(id, '');
        
    var parameterString = createParameterStringForLimit(id);
    $.get('${pageContext.request.contextPath}<%= WiSeEnv.getAdminContextPath() %>/mediaManager/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data)
    });
    
}



function cancelButton() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].update.disabled = true;
	document.forms[0].hiddenId.value = '';
	document.forms[0].title.value = '';
}

function searchKeyword(keyword) {
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/mediaManager/search";
	document.forms[0].submit();
}


function insert(){
	document.mediaaddForm.submit();
}
</script>
</head>
<body>
(db連線在com/intumit/multimedia/db_config.xml)
<div id="fr">
<form name="mediaaddForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/mediaManager" method="post">
	<table width="500">
	<tr>
		<td width="20%">標題:</td><td><input type="text" name="title"/></td>
	</tr>
	<tr>
		<td width="20%">描述內容:</td><td><textarea rows="4" cols="40" name="description"></textarea></td>
	</tr>
	<tr>
		<td width="20%">路徑(url):</td><td><input type="text" name="path"/></td>
	</tr>
	<tr>
		<td width="20%">編輯者:</td><td><input type="text" name="editAuthor"/></td>
	</tr>
	<tr>
		<td colspan="2"><div align="center"><input type="button" name="send" value="送出新增" onclick="insert();"/></div></td>
	</tr>
	</table>
	<input type="hidden" name="action" value="add">
	<input type="hidden" name="hiddenId">
</form>


<form id="mediaList" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/mediaManager" method="post">
<table>
<tr>
	<td>請輸入查詢標題字串：</td>
	<td>
		<input type="text" name="title">
		<input id="submitMediaForm" type="submit" value="查詢">
		<input type="hidden" name="action" value="query">
	</td>
</tr>
</table>
</form>
<%  
    out.println(request.getAttribute("myhtml"));  
%>  
</div>

</body>
</html>
