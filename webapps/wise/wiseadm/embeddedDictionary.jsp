<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="java.util.Locale"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page isELIgnored ="false" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.robot.QaCategory" %>
<%@ page import="com.intumit.solr.robot.dictionary.*" %>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<HTML>
<HEAD>
<TITLE><bean:message key='embedded.dictionary.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script type="text/javascript">
function onInvokeAction(id) {
    setExportToLimit(id, '');
    var parameterString = createParameterStringForLimit(id);
    parameterString = parameterString + '&searchKeyword=' + $('#searchKeyword').val() + '&searchType=' + $('#searchType').val();
    $.get('${pageContext.request.contextPath}/wiseadm/embedded/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);
        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function select(id,keyword,type) {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', true);
	myForm.find('#update').attr('disabled', false);
	myForm.find('[name=hiddenId]').val(id);
	myForm.find('[name=keyword]').val(keyword);
	myForm.find('[name=type]').val(type);
}

function updateKeyword() {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/embedded/update");
	myForm.find('#submitForm').attr('disabled', false);
	myForm.submit();
}

function cancelButton() {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', false);
	myForm.find('#update').attr('disabled', true);
	myForm.find('[name=hiddenId]').val('');
	myForm.find('[name=keyword]').val('');
	myForm.find('[name=type]').val('');
}

function searchKeyword() {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/embedded/search");
	myForm.submit();
}

$(document).ready(function() {
	$('#myForm').submit(function(event) {
		return true;
	});
});


</script>
</HEAD>
<BODY>

<div id="fr">

<form id="myForm" action="<%= request.getContextPath() %>/wiseadm/embedded/save" method="post">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td><bean:message key='global.keywords'/>：</td>
	<td><input type="text" name="keyword" size="30"></td>
</tr>
<tr>
	<td><bean:message key='global.type'/>：</td>
	<td><input type="text" name="type" ></td>
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

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/embedded/search" method="post">
<table>
<tr>
	<td><bean:message key='query.words'/></td>
	<td><input id="searchKeyword" type="text" name="searchKeyword" value='<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>'></td>
</tr>
<tr>
    <td><bean:message key='query.type'/></td>
    <td><input id="searchType" type="text" name="searchType" value=<%= (request.getAttribute("searchType") == null ? "" : request.getAttribute("searchType")) %>></td>
</tr>
<tr>
	<td colspan="2" style="text-align:right;"><input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:search();"></td>
</tr>
</table>
</form>

<%
    out.println(request.getAttribute("myhtml"));
%>
</div>
<br>
<bean:message key='embedded.dictionary.csv'/><br>
<bean:message key='global.import.format'/>
<ul>
	<li><bean:message key='not.need.head'/></li>
	<li><bean:message key='comma.separated'/></li>
	<li><bean:message key='embedded.dictionary.csv.ex'/></li>
</ul>
<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/embeddedUploadFile">
    <input type="file" name="File1" size="20" maxlength="20"> <br><br>
    <input type="submit"value="<bean:message key='global.import.csv'/>">
</form>
</BODY>
</HTML>
