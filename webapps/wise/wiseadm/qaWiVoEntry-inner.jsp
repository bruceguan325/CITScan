<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.Locale"
	import="org.apache.wink.json4j.*" 
	import="com.intumit.solr.admin.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.util.*"
	import="com.intumit.syslog.OperationLogEntity"
	import="com.intumit.message.MessageUtil"
	isELIgnored ="false"
%>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
boolean isAdmin = user.isSuperAdmin();

JSONObject urlPatternAdmin = AdminGroupFacade.getInstance().getFromSession(request.getSession()).getUrlPatternAdminCRUD();
Integer permissionValue = urlPatternAdmin.optInt(AdminGroup.urlPatternMaps.get("qaWiVoEntry-inner.jsp"), AdminGroup.checkAdminVal(isAdmin));
%>
<HTML>
<HEAD>
<TITLE><bean:message key='wivoEntry.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet"/>
<style>
li {
	margin-top: 5px;
}
</style>
<script type="text/javascript">
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    parameterString = parameterString + '&searchKeyword=' + document.getElementById('searchKeyword').value;
    $.get('${pageContext.request.contextPath}/wiseadm/wivoEntry/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function select(id,channel,keyword,excludes,includes,enabled) {
	myForm = $('#myForm');
	if (myForm.find('#submitForm'))
		myForm.find('#submitForm').attr('disabled', true);
	if (myForm.find('#update'))
		myForm.find('#update').attr('disabled', false);
	myForm.find('[name=hiddenId]').val(id);
	myForm.find('[name=keyword]').val(keyword);
	myForm.find('[name=channel]').val(channel);
	myForm.find('[name=includes]').val(includes);
	myForm.find('[name=excludes]').val(excludes);

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

function saveKeyword() {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wivoEntry?action=<%= OperationLogEntity.SAVE %>");
	myForm.submit();
}

function updateKeyword() {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wivoEntry?action=<%= OperationLogEntity.UPDATE %>");
	if (myForm.find('#submitForm'))
		myForm.find('#submitForm').attr('disabled', false);
	myForm.submit();
}

function cancelButton() {
	myForm = $('#myForm');
	if (myForm.find('#submitForm'))
		myForm.find('#submitForm').attr('disabled', false);
	if (myForm.find('#update'))
		myForm.find('#update').attr('disabled', true);
	myForm.find('[name=hiddenId]').val('');
	myForm.find('[name=keyword]').val('');
	myForm.find('[name=channel]').val('');
	myForm.find('[name=includes]').val('');
	myForm.find('[name=excludes]').val('');
	myForm.find('[name=enabled][value=1]').prop( "checked", true );
}

function keywordSearch(keyword) {
	myForm = $('#searchForm');
	myForm.attr('action', '<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wivoEntry/search?action=<%= OperationLogEntity.LOAD %>&searchKeyword=' + encodeURIComponent(document.getElementById('searchKeyword').value));
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
<% if ((permissionValue & AdminGroup.U) > 0) { %>
<style type="text/css">.editWivo { display:inline-block; }</style>
<% } else { %>
<style type="text/css">.editWivo { display:inline-block; }</style>
<% } %>
<% if ((permissionValue & AdminGroup.D) > 0) { %>
<style type="text/css">.delWivo { display:inline-block; }</style>
<% } else { %>
<style type="text/css">.delWivo { display:inline-block; }</style>
<% } %>
</HEAD>
<BODY>
<%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
%>
<br>
<div id="fr">
<%
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String [] args= { MessageUtil.getMessage(locale, "wivoEntry"), ".csv" };
%>
<% if ((permissionValue & AdminGroup.C) > 0 && (permissionValue & AdminGroup.U) > 0) { %>
<div class="alert alert-info">
	<h4><bean:message key='wivoEntry'/><bean:message key='search.export'/></h4>
	<a style="width:100px" class="btn btn-primary" href="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/exportWivoEntry.jsp?action=<%= OperationLogEntity.EXPORT %>"><bean:message key='search.export'/></a>
	<hr style="border:1px solid #5bc0de;">
	<h4 style="margin-top:0px"><bean:message key='wivoEntry'/><bean:message key='global.import'/></h4>
	<form style="margin-bottom:0px" id="UploadForm" name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/qaWivoEntryUploadFile?action=<%= OperationLogEntity.IMPORT %>">
    	<bean:message key='wivoEntry.csv'/><br>
		<bean:message key='global.import.format'/>
		<ul>
			<li><bean:message key='comma.separated'/></li>
			<li><bean:message key='wivoEntry.csv.ex'/></li>
		</ul>
    	<a class="uploadFile" style="text-decoration:none">
    	  <label class="btn btn-info">
		  <input class="upload" id="upload_img" style="display:none" type="file" name="File1">
		  <i class="fa fa-file"></i> <bean:message key='global.upload.file'/></label> &nbsp;<input type="text" class="showFileName" value="<%= MessageUtil.getMessage(locale, "global.import.file.hint", args) %>" style="width:50%" disabled/>
		</a>&nbsp;
    	<label class="btn btn-warning">
		  <input type="submit" style="display:none;"><i class="fas fa-file-import"></i> <bean:message key='global.import.csv'/>
		</label>
	</form>
</div>
<%} %>
<form id="myForm" method="post">
<input type="hidden" name="hiddenId">
<table class="table table-striped">
<tr>
	<td><bean:message key='global.keyword'/>：</td>
	<td><input class="form-control" type="text" name="keyword" size="50"></td>
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
	<td><bean:message key='wivoEntry.includes'/>：</td>
	<td><textarea class="form-control" name="includes" rows="3" cols="50"></textarea></td>
</tr>
<tr>
	<td><bean:message key='wivoEntry.excludes'/>：</td>
	<td><textarea class="form-control" name="excludes" rows="3" cols="50"></textarea></td>
</tr>
<tr>
	<td><bean:message key='is.enabled'/>：</td>
	<td>
		<input type="radio" name="enabled" value="1" checked>&nbsp;<bean:message key='global.yes'/>&nbsp;&nbsp;
		<input type="radio" name="enabled" value="0">&nbsp;<bean:message key='global.no'/>
	</td>
</tr>
</table>
<table>
<tr>
		<td><input id="submitForm" class="form-control" type="button" value="<bean:message key='global.add'/>" onClick="javascript:saveKeyword();"></td>
		<td><input id="update" class="form-control" type="button" value="<bean:message key='modify'/>" disabled onClick="javascript:updateKeyword();"></td>
		<td><input id="cancel" class="form-control" type="button" value="<bean:message key='global.cancel'/>" onClick="javascript:cancelButton();"></td>
</tr>
</table>
</form>
<br>
<form id="searchForm" method="post">
<table>
<tr>
	<td><bean:message key='query.words'/></td>
	<td>
		<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getParameter("searchKeyword") == null ? "" : xssReq.getParameter("searchKeyword")) %>>
		<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:keywordSearch();">
	</td>
</tr>
</table>
</form>
<%
out.println(xssReq.getFakeAttribute("myhtml"));
%>
</div>
<script type="text/javascript">
var allowFileName = '<%= args[0] %>';
var allowExtName = '<%= args[1] %>';

$(document).ready(function() {
	$('#UploadForm').on('submit', uploadFile);
});

function uploadFile(event) {
	var tempF = $('input[name="File1"]')[0].files[0];
	var allowUpload = false;
	if (typeof tempF === 'undefined') {
		allowUpload = false;
	} else if (tempF.name.indexOf(allowFileName) != -1 && tempF.name.endWith(allowExtName)) {
		allowUpload = true;
	} else {
		allowUpload = false;
	}
		
	if (!allowUpload) {
		swal('ERROR', '<%=MessageUtil.getMessage(locale, "global.import.file.error") %>(<%= MessageUtil.getMessage(locale, "global.import.file.hint", args) %>)', 'error');
		return false;
	}
}

$(".uploadFile").on("change","input[type='file']",function(){
	var filePath = $(this).val();
	if (filePath.indexOf(allowFileName) != -1 && filePath.indexOf(allowExtName) != -1) {
		var arr = filePath.split('\\');
		var fileName = arr[arr.length - 1];
		$(".showFileName").val('<%=MessageUtil.getMessage(locale, "global.file.name") %>:' + fileName);
	} else {
		$(".showFileName").val('<%=MessageUtil.getMessage(locale, "global.import.file.error") %>(<%= MessageUtil.getMessage(locale, "global.import.file.hint", args) %>)');
		return false 
	}
})
</script>
</BODY>
</HTML>
