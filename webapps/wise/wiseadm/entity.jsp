<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="java.util.Locale"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page isELIgnored ="false" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.robot.QaCategory" %>
<%@ page import="com.intumit.solr.robot.dictionary.*" %>
<%@ page import="com.intumit.solr.robot.entity.QAEntityType" %>
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
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE><bean:message key='entity.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet" />
<script type="text/javascript">
function subForm(theform){
	if(theform.category.value.length == 0 || theform.code.value.length == 0 || theform.name.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		theform.category.focus();
		return false;
	}
	re=/^[<%= t.getLocale().getUnicodeRangeRegex() %>]+$/;
	if(!re.test(theform.code.value)){
		alert("<bean:message key='alert.symbol'/>");
		theform.code.focus();
     	return false;
 	}
	return true;
}
function onInvokeAction(id) {
    setExportToLimit(id, '');
    
    var parameterString = createParameterStringForLimit(id);
    
    parameterString = parameterString + '&searchKeyword=' + document.getElementById('searchKeyword').value;
    $.get('${pageContext.request.contextPath}/wiseadm/entity/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function del(fieldName, fieldId) {
    if(confirm('<bean:message key="sure.del" />'))
    {
        $.ajax({
            url:"<%=request.getContextPath()%>/wiseadm/EntityVersionServlet/add",
            type:"post",
            data:{id:fieldId,action:"delete"},
            success:function(data){
                alert(data.msg)
            },
            error:function(){
                alert("error!");
            }
        })
    }
}

function select(id,category,code,name,subEntities,entityType,entityValues,fromIndex,enabled) {
	myForm = $('#myForm');
    myForm.find('[name=id]').val(id);
    myForm.find('[name=category]').val(category);
	myForm.find('[name=code]').val(code);
	myForm.find('[name=name]').val(name);
	myForm.find('[name=subEntities]').val(subEntities);
	myForm.find('[name=entityValues]').val(entityValues);
	myForm.find("[name=entityType][value=" + entityType + "]").prop('checked', true);
	
	if (fromIndex == 'false')
		myForm.find('[name=fromIndex][value=0]').prop( "checked", true );
	else
		myForm.find('[name=fromIndex][value=1]').prop( "checked", true );
	if (enabled == 'true')
		myForm.find('[name=enabled][value=1]').prop( "checked", true );
	else
		myForm.find('[name=enabled][value=0]').prop( "checked", true );

}

function cancelButton() {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', false);
	myForm.find('#update').attr('disabled', true);
    myForm.find('[name=id]').val('');
	myForm.find('[name=category]').val('');
	myForm.find('[name=code]').val('');
	myForm.find('[name=name]').val('');
	myForm.find('[name=subEntities]').val('');
	myForm.find("[name=entityType][value=STRING]").prop('checked', true);
	myForm.find('[name=entityValues]').val('');
	myForm.find('[name=enabled][value=1]').prop( "checked", true );
	myForm.find('[name=fromIndex][value=0]').prop( "checked", true );
}

function keywordSearch(keyword) {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/entity/search");
	myForm.submit();
}

function exportCSV() {
	window.open("<%= request.getContextPath() %>/wiseadm/entityExport.jsp");
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

<div id="fr" style="margin-top:2%">
<form id="myForm" class="form">
    <input type="hidden" name="action">
    <input type="hidden" name="id">
	<div class="form-group row">
		<div class="col-sm-1">
			<font color="#FF0000">*</font><bean:message key='global.category'/>：
		</div>
		<div class="col-sm-6">
			<input type="text" class="form-control" id="category" name="category" size="60">
		</div>
	</div>
	<div class="form-group row">
		<div class="col-sm-1">
			<font color="#FF0000">*</font><bean:message key='global.code'/>：
		</div>
		<div class="col-sm-6">
			<input type="text" class="form-control" id="code" name="code" size="60">
		</div>
	</div>
	<div class="form-group row">
		<div class="col-sm-1">
			<font color="#FF0000">*</font><bean:message key='global.name'/>：
		</div>
		<div class="col-sm-6">
			<input type="text" class="form-control" id="name" name="name" size="60">
		</div>
	</div>	
	<div class="form-group row">
		<div class="col-sm-1">
			<bean:message key='global.subEntities'/>：
		</div>
		<div class="col-sm-6">
			<input type="text" class="form-control" id="subEntities" name="subEntities" size="60">
		</div>
	</div>
	<hr>
	<div class="form-group row">
		<div class="col-sm-1">
			<font color="#FF0000">*</font><bean:message key='global.values'/>：
		</div>
		<div class="col-sm-6">
			<textarea  class="form-control" id="entityValues" id="entityValues" name="entityValues" cols="50" rows="5"></textarea> 
			<br>(<bean:message key='intent.keywords.ex'/>)
		</div>
	</div>
	<hr>
	<div class="form-group row">
		<div class="col-sm-2">
			<bean:message key="global.entityType"/>:
		</div>
		<div class="col-sm-7">
		<% for (QAEntityType et: QAEntityType.values()) { %>
		<%= et.name() %><input type="radio" name="entityType" value="<%= et.name() %>" <%= (QAEntityType.STRING == et) ? "checked" : "" %>> &nbsp;
		<% } %>
		</div>
	</div>
	<div class="form-group row">
		<div class="col-sm-2">
			<bean:message key="entity.fromIndex"/>:
		</div>
		<div class="col-sm-7">
			<bean:message key='global.yes'/><input type="radio" name="fromIndex" value="1">
			<bean:message key='global.no'/><input type="radio" name="fromIndex" value="0" checked>
			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="qaCustomDataInspect.jsp" target="_new">Inspect Index</a>
		</div>
	</div>
    <hr>
    <div class="form-group row">
		<div class="col-sm-2">
			<bean:message key="is.enabled"/>:
		</div>
		<div class="col-sm-7">
		<bean:message key='global.yes'/><input type="radio" name="enabled" value="1" checked>
		<bean:message key='global.no'/><input type="radio" name="enabled" value="0">
		</div>
	</div>
	<hr>
	<div class="row">
		<div class="col-sm-1">
			<button id="processBtn" class="btn btn-success" type="button">
				<bean:message key='global.audit.process' />
			</button>
		</div>
	<div class="col-sm-1">
		<input id="cancel" class="btn btn-secondary" type="button"
			value="<bean:message key='global.cancel'/>"
			onClick="javascript:cancelButton();">
	</div>
</div>
</form>
<br>

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/entity/search" method="post">
<table>
<tr>
	<td><b><bean:message key='query.words'/></b></td>
	<td>
		<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>>
		<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:keywordSearch();">
		(<bean:message key='query.words.entity'/>)
	</td>
</tr>
</table>
</form>

<%
    out.println(request.getAttribute("myhtml"));
%>
<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0){ %>
	<hr>
	<h4><b>實體匯出</b></h4>
	<input type="button" id="export" value="<bean:message key='search.export.csv'/>" onclick="exportCSV()">
	<br>
<%} %>
<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0){ %>
	<hr>
	<h4><b>實體匯入</b></h4>
	<bean:message key='entity.csv'/><br>
	<bean:message key='global.import.format'/>

	<ul>
		<li><bean:message key='not.need.head'/></li>
		<li><bean:message key='comma.separated'/></li>
		<li><bean:message key='entity.csv.ex'/></li>
	</ul>

	<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/qaEntityUploadFile">
		<input type="file" name="File1" size="20" maxlength="20">
		<br>
		<input type="submit"value="<bean:message key='global.import.csv'/>">
	</form>
<%} %>
</div>
</BODY>
<script type="text/javascript">
$("#processBtn").click(function(){
    var id = $("input[name=id]").val()
    //防呆
    var category = document.getElementById('category');
    var code = document.getElementById('code');
    var name = document.getElementById('name');
    var entityValues = document.getElementById('entityValues');
    
    if (category.value.trim() != "" && code.value.trim() != "" && name.value.trim() != "" && entityValues.value.trim() != "") {
    	//do nothing
    } else {
		alert("<bean:message key='global.explanation.essential.ent.ex'/>");
		return;
    }
    
    if(id){
        $("input[name=action]").val("update")
    }else{
        $("input[name=action]").val("save")
    }
    var form  = $("#myForm")
    
    $.ajax({
		url:"<%=request.getContextPath()%>/wiseadm/EntityVersionServlet/add",
		type: "post",
		data:form.serialize(),
		success:function (data){
			alert(data.msg)
			if(data.reset){
				form[0].reset();
				history.go(0);
			}
		},error:function(){
			alert("error!")
        }
    })
})
</script>

</HTML>
