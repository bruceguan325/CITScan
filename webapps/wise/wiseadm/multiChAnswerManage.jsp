<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@ page import="com.intumit.solr.robot.QAChannel" %>
<%@ page import="com.intumit.solr.robot.QAUserType" %>

<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<%--
 此頁面已經放入 tenantAdmin.jsp 當中，不再維護
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title><bean:message key='multiChannelAnswer.manage'/></title>
<!--  <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">-->
<link rel="stylesheet" href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css">

<style>

.marginTop{
	margin-top: 20px;
}
.addMarginTop{
	margin-top: 5px;
}

.treeButton{
	margin-left: 15px;
}

.treeButton2{

	padding-top: 0px;
	padding-bottom: 0px;
}


.dropdown-menu {

}
.scrollList {
    overflow: scroll;
    height: 200px;
    border:1px solid #ddd;
    padding-left: 15px;
    overflow-x: hidden;

}
.listObject {
    border:1px solid #ddd;
}
.listObjectButton{
	margin-left: 5px;
	padding-top: 0px;
	padding-bottom: 0px;
}
</style>

</head>
<body>
<!-- <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script> -->
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>

<script>
$(document).ready(function() {
	$("#tenant").change(function() {
		if ($(this).val() < 1) return;
		var $selected = $(this).find("option:selected");
		$.post('multiChannelUserTypeList.jsp',
			{
			tenantId:$selected.val()
			},
			loadList,
			"json"
		);
	});
	function loadList(data) {
		loadChannel(data.channel);
		loadUserType(data.userType);
	}

	$.post('multiChannelUserTypeList.jsp',
		{
		tenantId:$("#tenant").find("option:selected").val()
		},
		loadList,
		"json"
	);
});

function delChannelUserType(type, id) {
	$.post('multiChannelUserTypeDel.jsp',
		{
		tenantId:$("#tenant").find("option:selected").val(),
		type:type,
		id:id
		},
		reLoadSingleSide,
		"json"
	);
}

function addChannelUserType(type) {

	var postData = {
			tenantId:$("#tenant").find("option:selected").val(),
			type:type,
		};
	if (type=='channel') {
		postData['name'] = $("#channelName").val();
		postData['code'] = $("#channelCode").val();
	} else {
		postData['name'] = $("#userTypeName").val();
		postData['code'] = $("#userTypeCode").val();
	}
	$.post('multiChannelUserTypeAdd.jsp',
		postData,
		reLoadSingleSide,
		"json"
	);
}

function reLoadSingleSide(data) {
	if (data.fail != null) {
		alert(data.fail);
		return;
	}
	if (data.channel != null) {
		loadChannel(data.channel);
	} else {
		loadUserType(data.userType);
	}
}

function loadChannel(chData) {
	var len = chData.length;
	$("#channelContent").empty();
	for (var i=0; i<len; i++) {
		//alert("id:"+chData[i].id+" name:"+chData[i].name+" code:"+chData[i].code);

		var html = '<div class="btn-group col-sm-12">';
		html += '<button type="button" class="btn btn-default col-sm-9">';
		html += chData[i].name+' - ' + chData[i].code;
		html += '</button>';
		if (chData[i].code != 'web' && chData[i].code != 'app') {
			html += '<button type="button" class="btn btn-default" onclick="delChannelUserType(\'channel\','+chData[i].id+')">';
			html += '<span class="glyphicon glyphicon-remove" aria-hidden="true"></span>';
			html += '</button>';
		}
		html += '</div>';
		$("#channelContent").append(html);
	}
}

function loadUserType(utData) {
	var len = utData.length;
	$("#userTypeContent").empty();
	for (var i=0; i<len; i++) {
		//alert("id:"+utData[i].id+" name:"+utData[i].name+" code:"+utData[i].code);

		var html = '<div class="btn-group col-sm-12">';
		html += '<button type="button" class="btn btn-default col-sm-9">';
		html += utData[i].name+' - ' + utData[i].code;
		html += '</button>';
		if (utData[i].code != 'unknown') {
			html += '<button type="button" class="btn btn-default" onclick="delChannelUserType(\'userType\','+utData[i].id+')">';
			html += '<span class="glyphicon glyphicon-remove" aria-hidden="true"></span>';
			html += '</button>';
		}
		html += '</div>';
		$("#userTypeContent").append(html);
	}
}

</script>


<div class="col-sm-12 marginTop">

<div class="row marginTop">
<div class="col-sm-offset-1">
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
String msgTitle = request.getParameter("msgTitle");
String content = request.getParameter("content");
Integer tenantId = new Integer(StringUtils.defaultString(request.getParameter("tenantId"), "0"));
//System.out.println( "request tenantId : " + tenantId );
%>
標的：<select id="tenant" name="tenantId">
<%
List<Tenant> tenants = Tenant.list();
for (Tenant t: tenants) {
	//System.out.println( "tenantId : " + t.getId() );
	QAChannel.checkData(t.getId());
	QAUserType.checkData(t.getId());
%>
<option value="<%= t.getId() %>" <%= t.getId() == tenantId ? "selected" : "" %>><%= t.getName() %>(<%= t.getNotes() %>)</option>
<%
}
%>
</select>
</div>
</div>
<div class="row marginTop">
<div id="channelContent" class="col-sm-offset-1 col-sm-3 scrollList">

</div>

<div id="userTypeContent" class="col-sm-offset-1 col-sm-3 scrollList">

</div>


</div>
<div class="row addMarginTop">

<div class="col-sm-offset-1 col-sm-3">
<div class="form-group">
新增channel
</div>
<div class="form-group">
<div>名稱：<input type="text" id="channelName" name="channelName"></div>
</div>
<div class="form-group">
<div>代號：<input type="text" id="channelCode" name="channelCode"> </div>
</div>
<div class="form-group">
<button type="button" class="btn btn-default" onclick="addChannelUserType('channel')">新增</button>
</div>
</div>

<div class="col-sm-offset-1 col-sm-3">
<div class="form-group">
新增userType
</div>
<div class="form-group">
<div>名稱：<input type="text" id="userTypeName" name="userTypeName"></div>
</div>
<div class="form-group">
<div>代號：<input type="text" id="userTypeCode" name="userTypeCode"> </div>
</div>
<div class="form-group">
<button type="button" class="btn btn-default" onclick="addChannelUserType('userType')">新增</button>
</div>
</div>

</div>

</div>
</body>
</html>

 --%>
