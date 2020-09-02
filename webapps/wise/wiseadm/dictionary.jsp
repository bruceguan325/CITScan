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

AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE><bean:message key='dictionary.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/wiseadm/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/wiseadm/css/jmesa.css" />
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet" />
<script type="text/javascript">
function subForm(theform){
	if(theform.keyword.value.length == 0){
		alert("<bean:message key='alert.noString'/>");
		theform.keyword.focus();
		return false;
	}
	re=/^[\w\u4e00-\u9fa5|\w\u0800-\u4e00]+$/; //chinese + japaness
	if(!re.test(theform.keyword.value)){
		alert("<bean:message key='alert.symbol'/>");
		theform.keyword.focus();
     	return false;
 	}
	return true;
}	
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    parameterString = parameterString + '&searchKeyword=' + document.getElementById('searchKeyword').value;
    $.get('${pageContext.request.contextPath}/wiseadm/dict/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data);

        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}

function select(id,keyword,purposes,enabled,category,enableQaScopeRestriction) {
	myForm = $('#myForm');
	myForm.find('[name=id]').val(id);
	myForm.find('[name=keyword]').val(keyword);
	myForm.find('[name=purposes]').val(purposes);
	myForm.find('[name=purpose]:checkbox').prop( "checked", false );

	if (enabled == 'true')
		myForm.find('[name=enabled][value=1]').prop( "checked", true );
	else
		myForm.find('[name=enabled][value=0]').prop( "checked", true );

	myForm.find('[name=category][value='+category+']').prop( "checked", true );

	if (enableQaScopeRestriction == 'true')
		myForm.find('[name=enableQaScopeRestriction][value=1]').prop( "checked", true );
	else
		myForm.find('[name=enableQaScopeRestriction][value=0]').prop( "checked", true );

	arr = purposes.split(",");
	$('#myForm input:checkbox').prop( "checked", false );
	for (var i=0; i < arr.length; i++) {
		$('#myForm input[value=' + arr[i] + ']:checkbox').prop( "checked", true );
	}
}

function cancelButton() {
	myForm = $('#myForm');
	myForm.find('#submitForm').attr('disabled', false);
	myForm.find('#update').attr('disabled', true);
	myForm.find('[name=id]').val('');
	myForm.find('[name=keyword]').val('');
	myForm.find('[name=purposes]').val('');
	myForm.find('[name=purpose]:checkbox').prop( "checked", false );
	myForm.find('[name=enabled][value=1]').prop( "checked", true );
	myForm.find('[name=category]:radio').prop( "checked", false );
	myForm.find('[name=category][value="通用"]').prop( "checked", true );
	myForm.find('[name=enableQaScopeRestriction][value=0]').prop( "checked", true );
}

function keywordSearch(keyword) {
	myForm = $('#myForm');
	myForm.attr('action', "<%= request.getContextPath() %>/wiseadm/dict/search");
	myForm.submit();
}

function del(fieldName, fieldId) {
	if(confirm('<bean:message key="sure.del" />'))
	{
		$.ajax({
			url:"<%=request.getContextPath()%>/wiseadm/DictionaryVersionServlet/add",
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

function exportCSV() {
	window.open("<%= request.getContextPath() %>/wiseadm/dictionaryExport.jsp");
}

$(document).ready(function() {
	$('#myForm').submit(function(event) {
		myForm = $('#myForm');
		var arr = [];

		myForm.find('input[name=purpose]:checked').each(function() {
			$this = $(this);
			arr.push($this.val());
		});

		myForm.find('input[name=purposes]').val(arr.join());
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
		<div class="col-sm-2">
			<font color="#FF0000">*</font><bean:message key='global.keywords'/>：
		</div>
		<div class="col-sm-6">
			<input type="text" class="form-control" id="keyword" name="keyword" size="30">
		</div>
	</div>
	<hr>
	<div class="form-group row">
		<div class="col-sm-2">
			<font color="#FF0000">*</font><bean:message key='usage.set'/>：
		</div>
		<div class="col-sm-4">
			<input type="hidden" id="purposes" name="purposes" >
			<%
			Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
			for (DictionaryDatabase.Purpose p: DictionaryDatabase.Purpose.values()) { %>
			<% if(p.name().equals("KNOWLEDGE_POINT")) { %>
				<input type="checkbox" id="purpose" name="purpose" class="form-check-input" value="<%= p.name() %>" checked ><%= MessageUtil.getMessage(locale, p.getDesc())  %><BR>
			<% } else { %>
				<input type="checkbox" id="purpose" name="purpose" class="form-check-input" value="<%= p.name() %>"><%= MessageUtil.getMessage(locale, p.getDesc())  %><BR>
			<% } %>
		<% } %>
		</div>
	</div>
	<hr>
	<div class="form-group row">
		<div class="col-sm-2">
			<bean:message key="global.category"/>:
		</div>
		<div class="col-sm-4">
		<%
		  com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
       	  String[] qaCategorys = null;
     	  if(t.getQaCategory() != null){
     		qaCategorys = t.getQaCategory().split(",");
     	  }
       	  for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) {
       		if(qaCategorys[i].equals("通用")){
       		%>
       			<input type="radio" class="form-check-input" name="category" value="<%=qaCategorys[i]%>" checked ><%=qaCategorys[i]%>
       		<%
       		} else {
       		%>
       			<input type="radio" class="form-check-input" name="category" value="<%=qaCategorys[i]%>" ><%=qaCategorys[i]%>
       		<%
       		}
		  }%>
		</div>
	</div>
	<hr>

<% if (t.getEnableRestrictToKnowledgePoint()) { %>
<div class="form-group row">
	<div class="col-sm-3">
		<bean:message key='is.restrictToKnowledgePoint.enable'/>：
	</div>
	<div class="col-sm-2">
		<bean:message key='global.yes'/><input type="radio" class="form-check-input" name="enableQaScopeRestriction" value="1" >
		<bean:message key='global.no'/><input type="radio" class="form-check-input" name="enableQaScopeRestriction" value="0" checked>
	</div>
</div>
<hr>
<% } %>
<div class="form-group row">
	<div class="col-sm-2">
		<bean:message key='is.enabled'/>
	</div>
	<div class="col-sm-2">
		<bean:message key='global.yes'/><input type="radio" class="form-check-input" name="enabled" value="1" checked>
		<bean:message key='global.no'/><input type="radio" class="form-check-input" name="enabled" value="0">	
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

<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/dict/search" method="post">
<table>
<tr>
	<td><b><bean:message key='query.words'/></b></td>
	<td>
		<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>>
		<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:keywordSearch();">
		(<bean:message key='query.words.dictionary'/>)
	</td>
</tr>
</table>
</form>

<%
    out.println(request.getAttribute("myhtml"));
%>
<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0 ){ %>
	<hr>
	<h4><b>辭典匯出</b></h4>
	<input type="button" id="export" value="<bean:message key='search.export.csv'/>" onclick="exportCSV()">
	<br>
<%} %>
<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0 ){ %>
	<hr>
	<h4><b>辭典匯入</b></h4>
	<bean:message key='dictionary.csv'/><br>
	<bean:message key='global.import.format'/>
	<ul>
		<li><bean:message key='not.need.head'/></li>
		<li><bean:message key='comma.separated'/></li>
	<% if (t.getEnableRestrictToKnowledgePoint()) { %>
		<li><bean:message key='dictionary.enableQaScopeRestriction.ex'/></li>
	<% } %>
	<li><bean:message key='dictionary.csv.ex'/></li>
	</ul>
	<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/dicUploadFile">
		<input type="file" name="File1" size="20" maxlength="20"> 
		<br>
		<input type="submit"value="<bean:message key='global.import.csv'/>">
	</form>
<%} %>
</div>
</BODY>
<script type="text/javascript">
$("#processBtn").click(function(){
	
	var keywords = document.getElementById('keyword');
	var usages = document.getElementsByName('purpose');
	var usageslen = usages.length;
	var checked = false;
	
    for (var i = 0; i < usageslen; i++ ) {
    	if (usages[i].checked == true) {
    		checked = true;
    		break;
    	}
    }
	
	if(keywords.value.trim() != "" && checked == true ){
		//do nothing
	}else{
		alert("<bean:message key='global.explanation.essential.dic.ex'/>");
		return;
	}
	
    var id = $("input[name=id]").val()
    if(id){
    	$("input[name=action]").val("update")
    }else{
    	$("input[name=action]").val("save")
    }
	var form  = $("#myForm")
	$.ajax({
		url:"<%=request.getContextPath()%>/wiseadm/DictionaryVersionServlet/add",
		type: "post",
		data:form.serialize(),
		success:function (data){
			alert(data.msg);
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
