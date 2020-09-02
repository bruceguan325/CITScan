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
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) {
	return;
}
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE><bean:message key='compulsory.division.manage'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script type="text/javascript">
function subForm(theform){
	if(theform.sentence.value.length == 0 || theform.disambiguation.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		theform.sentence.focus();
		return false;
	}
	return true;
}
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    $.get('${pageContext.request.contextPath}/wiseadm/ambiguity/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}


function select (id,sentence,disambiguation,enabled) {
	document.forms[0].hiddenId.value = id;
	document.forms[0].sentence.value = sentence;
	document.forms[0].disambiguation.value = disambiguation;
	if (enabled == 'true')
		document.forms[0].enabled[0].checked = true;
	else
		document.forms[0].enabled[1].checked = true;

	document.forms[0].submitForm.disabled = true;
	document.forms[0].update.disabled = false;
}

function updateKeyword() {
	document.forms[0].submitForm.disabled = false;
	if(document.forms[0].sentence.value.length == 0 || document.forms[0].disambiguation.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		document.forms[0].sentence.focus();
		return false;
	}
	document.forms[0].action = "<%= request.getContextPath() %>/wiseadm/ambiguity?action=update";
	document.forms[0].submit();
}

function cancelButton() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].update.disabled = true;
	document.forms[0].hiddenId.value = '';
	document.forms[0].sentence.value = '';
	document.forms[0].disambiguation.value = '';
	document.forms[0].enabled[0].checked = true;
}

function searchKeyword(keyword) {
	document.forms[0].action = "<%= request.getContextPath() %>/wiseadm/ambiguity/search";
	document.forms[0].submit();
}


</script>
</HEAD>
<BODY>


<div id="fr">

<form id="synForm" action="<%= request.getContextPath() %>/wiseadm/ambiguity?action=save" method="post" onsubmit="return subForm(this);">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td><bean:message key='global.sentence'/>：</td>
	<td><input type="text" name="sentence" size="30"></td>
</tr>
<tr>
	<td><bean:message key='compulsory.division.set'/>：</td>
	<td><input type="text" name="disambiguation" size="60"><bean:message key='compulsory.division.set.ex'/></td>
</tr>
<tr>
	<td><bean:message key='is.enabled'/>：</td>
	<td>
		<bean:message key='global.yes'/><input type="radio" name="enabled" value="1" checked>
		<bean:message key='global.no'/><input type="radio" name="enabled" value="0">
	</td>
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

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/ambiguity/search" method="post">
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
</BODY>
</HTML>
