<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="java.util.Locale"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page isELIgnored ="false" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.robot.*" %>
<%@ page import="com.intumit.solr.robot.dictionary.*" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser admUser = AdminUserFacade.getInstance().getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE><bean:message key='eventType.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script type="text/javascript">
function subForm(theform){
	if(theform.code.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		theform.code.focus();
		return false;
	}
	return true;
}	
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    parameterString = parameterString + '&searchKeyword=' + document.getElementById('searchKeyword').value;
    $.get('${pageContext.request.contextPath}/wiseadm/eventType/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function select(id,code,channel,builtIn,enabled) {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', true);
	myForm.find('#update').attr('disabled', false);
	myForm.find('[name=hiddenId]').val(id);
	myForm.find('[name=code]').val(code);
	myForm.find('[name=channel]').val(channel);

	if (builtIn == 'true')
		myForm.find('[name=builtIn][value=1]').prop( "checked", true );
	else
		myForm.find('[name=builtIn][value=0]').prop( "checked", true );

	if (enabled == 'true')
		myForm.find('[name=enabled][value=1]').prop( "checked", true );
	else
		myForm.find('[name=enabled][value=0]').prop( "checked", true );

	$('#myForm input:checkbox').prop( "checked", false );
	for (var i=0; i < arr.length; i++) {
		$('#myForm input[value=' + arr[i] + ']:checkbox').prop( "checked", true );
	}
}

function del(fieldName, fieldId) {
	return confirm("確定要刪除?");
}

function updateKeyword() {
	if(document.forms[0].code.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		document.forms[0].code.focus();
		return false;
	}
	document.forms[0].submitForm.disabled = false;
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/eventType?action=update";
	document.forms[0].submit();
}

function cancelButton() {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', false);
	myForm.find('#update').attr('disabled', true);
	myForm.find('[name=hiddenId]').val('');
	myForm.find('[name=code]').val('');
	myForm.find('[name=channel]').val('');
	myForm.find('[name=builtIn][value=0]').prop( "checked", true );
	myForm.find('[name=enabled][value=1]').prop( "checked", true );
}

function searchKeyword(keyword) {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/eventType/search");
	myForm.submit();
}

$(document).ready(function() {
	$('#myForm').submit(function(event) {
		myForm = $('#myForm');
		return true;
	});
	
	$(document).on('click', '.chooseChannel', function() {
		$('input[name=channel]').val($(this).attr('data-ch'));
	});
});


</script>
</HEAD>
<BODY>

<div id="fr">

<form id="myForm" action="<%= request.getContextPath() %>/wiseadm/eventType?action=save" method="post" onsubmit="return subForm(this);">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td><bean:message key='global.code'/>：</td>
	<td><input type="text" name="code" size="50"></td>
</tr>
<tr>
	<td>適用頻道</td>
	<td>
	<input type="text" name="channel" size="50">
	<br>（<a href='javascript:return false;' class='chooseChannel' data-ch='' >全部</a>&nbsp;&nbsp;
	<% 
	java.util.List<QAChannel> channels = QAChannel.list(t.getId());
	for (QAChannel ch: channels) { 
	%><a href='javascript:return false;' class='chooseChannel' data-ch='<%= ch.getCode() %>'><%= ch.getCode() %></a>&nbsp;&nbsp;<% 
	} 
	%>
	）
	</td>
</tr>
<tr>
	<td><bean:message key='global.builtIn'/>：</td>
	<td>
		<bean:message key='global.yes'/><input type="radio" name="builtIn" value="1" disabled=true>
		<bean:message key='global.no'/><input type="radio" name="builtIn" value="0" checked disabled=true>
	</td>
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

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/eventType/search" method="post">
<table>
<tr>
	<td><bean:message key='query.words'/></td>
	<td>
		<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>>
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
<%--
<bean:message key='eventType.csv'/><br>
<bean:message key='global.import.format'/>
<ul>
	<li><bean:message key='not.need.head'/></li>
	<li><bean:message key='comma.separated'/></li>
	<li><bean:message key='eventType.csv.ex'/></li>
</ul>
<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/qaWivoEntryUploadFile">
    <input type="file" name="File1" size="20" maxlength="20"> <br><br>
    <input type="submit"value="<bean:message key='global.import.csv'/>">
</form>
 --%>
</BODY>
</HTML>
