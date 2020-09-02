<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<%@ page import="javax.servlet.ServletConfig"%>
<%@ page import="javax.servlet.ServletException"%>
<%@ page import="javax.servlet.http.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.net.*"%>
<%@ page import="java.text.*"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.httpclient.*"%>
<%@ page import="org.apache.commons.httpclient.methods.*"%>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams"%>
<%@ page import="org.apache.solr.core.*"%>
<%@ page import="org.apache.solr.servlet.*"%>
<%@ page import="org.apache.solr.client.solrj.*"%>
<%@ page import="com.intumit.solr.util.WiSeEnv"%>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<%@ page import="com.intumit.solr.admin.*"%>
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
<TITLE><bean:message key='synonym.manger.platform' /></TITLE>
<script type="text/javascript"
	src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript"
	src="<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript"
	src="<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/js/jmesa.js"></script>
<script type="text/javascript"
	src="<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/css/jmesa.css" />
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet" />
<script type="text/javascript">
function subForm(theform){
	if(theform.keyword.value.length == 0 || theform.synonymKeyword.value.length == 0){
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
    $.get('${pageContext.request.contextPath}<%=WiSeEnv.getAdminContextPath()%>/syn/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data)
        
        if (typeof window.parent != 'undefined') {
        	window.parent.autoResize('iframe1');
        }
    });

}


function select (id,keyword,synonymKeyword,reverse,nature) {
	myForm = $('#synForm');
	myForm.find('[name=id]').val(id);
	myForm.find('[name=keyword]').val(keyword);
	myForm.find('[name=nature]').val(nature);
	myForm.find('[name=synonymKeyword]').val(synonymKeyword);
	
	if (reverse == 'true'){
		myForm.find('[name=reverse][value=1]').prop( "checked", true );
	}
	else{
		myForm.find('[name=reverse][value=0]').prop( "checked", true );
	}
}

function del(fieldName, fieldId) {
	if(confirm('<bean:message key="sure.del" />'))
		{
			$.ajax({
				url:"<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/SynonymVersionServlet/add",
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

function cancelButton() {
	myForm = $('#synForm');
	myForm.find('#update').attr('disabled', true);
	myForm.find('[name=id]').val('');
	myForm.find('[name=keyword]').val('');
	myForm.find('[name=synonymKeyword]').val('');
	myForm.find('[name=nature]').val('');
	myForm.find('[name=reverse][value=1]').prop( "checked", true );
}

function keywordSearch(keyword) {
	myForm = $('#synForm');
	myForm.attr('action', "<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/syn/search");
	myForm.submit();
}

</script>
</HEAD>
<BODY>
	<div id="fr">
		<form id="synForm">
			<input type="hidden" name="action">
			<input type="hidden" name="id">
			<div class="form-group">
				<table class="table">
					<tr>
						<td><font color="#FF0000">*</font><bean:message key='global.keyword' />：</td>
						<td><input class="form-control" type="text" name="keyword" id="keyword" size="30"></td>
						<td><bean:message key='synonym.nature' />：</td>
						<td><input class="form-control" type="text" name="nature" size="10"></td>
					</tr>
					<tr>
						<td><font color="#FF0000">*</font><bean:message key='global.synonyms' />：</td>
						<td colspan=3><textarea class="form-control" name="synonymKeyword" id="synonymKeyword" cols="50"
								rows="5"></textarea> <br> <bean:message
								key='global.synonyms.ex' /></td>
					</tr>
					<tr>
						<td><bean:message key='global.two.way.words' />：</td>
						<td colspan=3>
							<div class="form-check">
								<bean:message key='global.yes' />
								<input class="form-check-input" type="radio" name="reverse" value="1" checked>
								<bean:message key='global.no' />
								<input class="form-check-input" type="radio" name="reverse" value="0">
							</div>
						</td>
					</tr>
				</table>
				<table class="table">
					<tr>
						<td>
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
						</td>
					</tr>
				</table>
			</div>
		</form>
		<br>
		<form id="searchForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/syn/search" method="post">
		<table>
		<tr>
			<td><b><bean:message key='query.words'/></b></td>
			<td>
				<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>>
				<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:keywordSearch();">
				(<bean:message key='query.words.synonym'/>)
			</td>
		</tr>
		</table>
		</form>
		<%
			out.println(request.getAttribute("myhtml"));
		%>
	</div>
	<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0){%>
		<hr>
		<h4><b>同義詞匯出</b></h4>
		<button id="export">
        	<bean:message key='search.export.csv' />
    	</button>
		<br>
	<%} %>
	<%if(user.isSuperAdmin() || (admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0){%>
		<hr>
		<h4><b>同義詞匯入</b></h4>
		<bean:message key='synonym.csv' />
		<br>
		<bean:message key='global.import.format' />
		<ul>
			<li><bean:message key='not.need.head' /></li>
			<li><bean:message key='comma.separated' /></li>
			<li><bean:message key='synonym.csv.ex' /></li>
		</ul>
		<form name="UploadForm" enctype="multipart/form-data" method="post"
			action="<%=request.getContextPath()%>/wiseadm/synUploadFile">
			<input type="file" name="File1" size="20" maxlength="20">
			<br>
			<input type="submit" value="<bean:message key='global.import.csv'/>">
		</form>
	<%} %>
<script type="text/javascript">
  $('#export').click(function() {window.open("<%=request.getContextPath()%>/wiseadm/synExport.jsp");});
  $("#processBtn").click(function(){
	  
		var keywords = document.getElementById('keyword');
		var synonymKeywords = document.getElementById('synonymKeyword');
			
		if(keywords.value.trim() != "" && synonymKeywords.value.trim() != "" ){
			//do nothing
		}else{
			alert("<bean:message key='global.explanation.essential.syn.ex'/>");
			return;
		}
	  
	    var id = $("input[name=id]").val()
	    if(id){
	    	$("input[name=action]").val("update")
	    }else{
	    	$("input[name=action]").val("save")
	    }
		var form  = $("#synForm")
		$.ajax({
			url:"<%=request.getContextPath()%><%=WiSeEnv.getAdminContextPath()%>/SynonymVersionServlet/add",
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
</BODY>
</HTML>
