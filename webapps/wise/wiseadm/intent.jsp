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
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE><bean:message key='intent.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script type="text/javascript">
function subForm(theform){
	if(theform.category.value.length == 0 || theform.tag.value.length == 0 || theform.keywords.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		theform.category.focus();
		return false;
	}
	re=/^[<%= t.getLocale().getUnicodeRangeRegex() %>]+$/;
	if(!re.test(theform.tag.value)){
		alert("<bean:message key='alert.symbol'/>");
		theform.tag.focus();
     	return false;
 	}
	return true;
}
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    parameterString = parameterString + '&searchKeyword=' + document.getElementById('searchKeyword').value;
    $.get('${pageContext.request.contextPath}/wiseadm/intent/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function select(id,tag,keywords,enabled,category) {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', true);
	myForm.find('#update').attr('disabled', false);
	myForm.find('[name=hiddenId]').val(id);
	myForm.find('[name=tag]').val(tag);
	myForm.find('[name=keywords]').val(keywords);
	myForm.find('[name=category]').val(category);

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
	myForm = $('#myForm');
	if(myForm.find('[name=category]').val().length == 0 || myForm.find('[name=tag]').val().length == 0 || myForm.find('[name=keywords]').val().length == 0){
		alert("<bean:message key='alert.noString'/>");
		myForm.find('[name=category]').focus();
		return false;
	}
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/intent?action=update");
	myForm.find('#submitForm').attr('disabled', false);
	myForm.submit();
}

function cancelButton() {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', false);
	myForm.find('#update').attr('disabled', true);
	myForm.find('[name=hiddenId]').val('');
	myForm.find('[name=tag]').val('');
	myForm.find('[name=category]').val('');
	myForm.find('[name=keywords]').val('');
	myForm.find('[name=enabled][value=1]').prop( "checked", true );
}

function searchKeyword(keyword) {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/intent/search");
	myForm.submit();
}

$(document).ready(function() {
	$('#myForm').submit(function(event) {
		myForm = $('#myForm');
		return true;
	});
});


</script>
</HEAD>
<BODY>

<div id="fr">

<form id="myForm" action="<%= request.getContextPath() %>/wiseadm/intent?action=save" method="post" onsubmit="return subForm(this);">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td>Intent分類：</td>
	<td><input type="text" name="category" size="30"></td>
</tr>
<tr>
	<td><bean:message key='global.tag'/>：</td>
	<td><input type="text" name="tag" size="30"></td>
</tr>
<tr>
	<td><bean:message key='global.keywords'/>：</td>
	<td><input type="text" name="keywords" size="30">
		<br><bean:message key='intent.keywords.ex'/>
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

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/intent/search" method="post">
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
<bean:message key='intent.csv'/><br>
<bean:message key='global.import.format'/>
<ul>
	<li><bean:message key='not.need.head'/></li>
	<li><bean:message key='comma.separated'/></li>
	<li><bean:message key='intent.csv.ex'/></li>
</ul>
<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/qaIntentUploadFile">
    <input type="file" name="File1" size="20" maxlength="20"> <br><br>
    <input type="submit"value="<bean:message key='global.import.csv'/>">
</form>
</BODY>
</HTML>
