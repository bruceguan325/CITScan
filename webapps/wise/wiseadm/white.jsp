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
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	return;
}
%>

<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='white.words.manger'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/css/jmesa.css" />
<script type="text/javascript">
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    $.get('${pageContext.request.contextPath}<%= WiSeEnv.getAdminContextPath() %>/white/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data)
    });

}


function select (id,keyword) {
	document.forms[0].hiddenId.value = id;
	document.forms[0].keyword.value = keyword;
	document.forms[0].submitForm.disabled = true;
	document.forms[0].update.disabled = false;
}

function updateKeyword() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/white/update";
	document.forms[0].submit();
}

function cancelButton() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].update.disabled = true;
	document.forms[0].hiddenId.value = '';
	document.forms[0].keyword.value = '';
}

function searchKeyword(keyword) {
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/white/search";
	document.forms[0].submit();
}


</script>
</HEAD>
<BODY>


<div id="fr">

<form id="synForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/white/save" method="post">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td><bean:message key='global.keyword'/>：</td>
	<td><input type="text" name="keyword" size="30"></td>
</tr>
<tr>
	<td><bean:message key='nature.words'/>：</td>
	<td><input type="text" name="nature" size="10"></td>
</tr>
<tr>
	<td></td>
	<td><bean:message key='part.words.ex'/><a href="https://gist.github.com/luw2007/6016931" target="_new"><bean:message key='here'/></a></td>
</tr>
</table>
<table>
<tr>
		<td><input id="submitForm" type="submit" value="<bean:message key='global.add'/>"></td>
		<td><input id="update" type="button" value="<bean:message key='modify'/>" disabled="true" onClick="javascript:updateKeyword();"></td>
		<td><input id="cancel" type="button" value="<bean:message key='global.cancel'/>" onClick="javascript:cancelButton();"></td>
</tr>
</table>
</form>
<br>

<form id="searchForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/white/search" method="post">
<table>
<tr>
	<td><bean:message key='query.words'/></td>
	<td>
		<input type="text" name="searchKeyword">
		<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:searchKeyword();">
	</td>
</tr>
</table>
</form>




<%
    out.println(request.getAttribute("myhtml"));
%>
</div>
<br>
<bean:message key='synonym.csv'/>
<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/WhiteUploadFile">
    <input type="file" name="File1" size="20" maxlength="20"> <br>
    <input type="hidden" name="File2" size="20" maxlength="20" value="keywrod.csv"> <br>
    <input type="submit"value="<bean:message key='global.import.csv'/>">
</form>

</BODY>
</HTML>
