<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.lucene.index.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.request.*" %>
<%@ page import="org.apache.solr.search.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.robot.RobotFormalAnswers" %>
<%@ page import="com.intumit.solr.robot.RobotFormalAnswersVersion" %>
<%@ page import="com.intumit.solr.robot.RobotFormalAnswersVersionService" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.syslog.OperationLogEntity" %>
<%@ page import="com.vdurmont.emoji.*" %>
<%@ page import="com.intumit.solr.robot.AuditStatus" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='formal.answer.maintenance'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>

<script type="text/javascript" src="<%=request.getContextPath()%>/wiseadm/js/lib/jquery.tablesorter.min.js"></script>

<script type="text/javascript">
function autoResize(id){
    var newheight;
    var newwidth;

    if(document.getElementById){
        newheight=document.getElementById(id).contentWindow.document .body.scrollHeight;
        newwidth=document.getElementById(id).contentWindow.document .body.scrollWidth;
    }

    document.getElementById(id).height= (newheight) + "px";
    document.getElementById(id).width= (newwidth) + "px";
}

</script>

</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<%
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	String msg = "";
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	RobotFormalAnswersVersionService service = RobotFormalAnswersVersionService.getInstance();
	Integer opLogId = (Integer) request.getAttribute("opLogId");
	OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
	if (log != null) {
		Set<Integer> tIdSet = user.getTenantIdSet();
		if (tIdSet.contains(t.getId())) {
			String action = request.getParameter("action");
			String id = request.getParameter("id");
			String key = request.getParameter("key");
			String value = request.getParameter("value");
			value = StringUtils.isEmpty(value)?"":EmojiParser.parseToHtmlDecimal(value);
			RobotFormalAnswersVersion entity = new RobotFormalAnswersVersion();
			List<String> values = Arrays.asList(value.split("\\r?\\n"));
			entity.setKeyName(key);
			entity.setAnswers(values);
			entity.setEditorId(user.getId());
			entity.setEditorName(user.getName());
			entity.setTenantId(t.getId());
			RobotFormalAnswersVersionService.Result res = null;
			if ("save".equals(action)) {
				res = service.addNewVesion(entity);
			} else if ("update".equals(action)) {
				entity.setPublicId(Long.valueOf(id));
				res = service.update(entity);
			} else if ("delete".equals(action)) {
				res = service.delete(t.getId(), user.getId(), Long.parseLong(id));
			}
			msg = res.getMessage();
			log.setStatusMessage(res.getStatus());
		} else {
			msg = "權限不足!";
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
		}
		log.update();
		
		if(!msg.contains("成功")){
			request.setAttribute("defkey", request.getParameter("key"));
			request.setAttribute("defvalue", request.getParameter("value"));
		}else{
			request.setAttribute("defkey", "");
			request.setAttribute("defvalue", "");
		}
			
%>
		<div class="alert alert-warning" role="alert"><%= msg %></div>
		<%
	}
%>
<BR>
<div class="alert alert-info">
<ul>
<%
	String [] args={"{{F:XXX}}","{{F:ADDRESS}}"};
	String alertInfo = MessageUtil.getMessage(locale, "alert.info", args);
%>
<%=alertInfo %>
</ul>
</div>
<%
Map<String, RobotFormalAnswers> keyVal = null;
if(request.getParameter("searchKeyword") != null){
	String searchKeyword = (String)request.getParameter("searchKeyword");
	
	AuditStatus status = null;
	if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.status.audit"))) {
		status = AuditStatus.AUDIT;
	} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.in.force"))) {
		status = AuditStatus.HISTORY;
	}
	
 	keyVal = RobotFormalAnswers.fullSearchBySQL(t.getId(), searchKeyword, status);
}
else
	keyVal = RobotFormalAnswers.getKeyValueMap(t.getId());

%>
<hr />
<h4><b><bean:message key='manual.increase'/>：</b></h4>
<form method="POST" action="<%= request.getContextPath() %>/wiseadm/qaFormalAnswer.jsp">
	<input type="hidden" value="save" name="action" />
	<TABLE width="100%">
		<tr>
			<th width="5%"><font color="#FF0000">*</font>KEY：</th>
			<th width="25%">
				<input class="form-control" type="text" value="<%= (request.getAttribute("defkey") == null ? "" : request.getAttribute("defkey")) %>" name="key" />
			</th>
			<th width="3%"></th>
			<th width="5%"><font color="#FF0000">*</font>VAL：</th>
			<th width="50%">
				<textarea class="form-control" name="value" style="width: 400px;"><%= (request.getAttribute("defvalue") == null ? "" : request.getAttribute("defvalue")) %></textarea>
			</th>
			<th width="10%" valign="middle">
			<input type="button" name="save" value="<bean:message key='global.audit.process'/>" class="btn btn-success">
			</th>
		</tr>
	</TABLE>
</form>
<hr />
<form id="searchForm" action="<%= request.getContextPath() %>/wiseadm/qaFormalAnswer.jsp" method="post">
<table>
<tr>
	<td><b><bean:message key='query.words'/></b></td>
	<td>
		<input id="searchKeyword" type="text" name="searchKeyword" value=<%= (request.getAttribute("searchKeyword") == null ? "" : request.getAttribute("searchKeyword")) %>>
		<input id="submitSearchForm" type="button" name="search" value="<bean:message key='query'/>"> 
		(<bean:message key='query.words.formal'/>)
	</td>
</tr>
</table>
</form>
<br>
<h4><b><bean:message key='have.set'/>：</b></h4>
<form id="myForm" method="POST" action="<%= request.getContextPath() %>/wiseadm/qaFormalAnswer.jsp">
	<input type="hidden" name="action" />
	<input type="hidden" name="id" />
	<input type="hidden" name="key" />
	<input type="hidden" name="value" />
</form>
	<input type="hidden" value="update" name="action" />
	<TABLE id="myTable" width="100%" class="table table-striped table-bordered tablesorter">
		<thead>
			<tr>
				<th width="auto">ID</th>
				<th width="auto">KEY</th>
				<th width="auto">VAL</th>
				<th width="auto"><bean:message key='global.status'/></th>
				<th width="auto"><bean:message key='global.audit.date'/></th>
				<th width="auto"></th>
			</tr>
		 </thead>
		<%
			List<String> answersList;
			for(Map.Entry<String, RobotFormalAnswers> entry : keyVal.entrySet()) {
				String key = entry.getKey();
				RobotFormalAnswers val = entry.getValue();
				answersList = val.getAnswersList();
				String concated = StringUtils.join(answersList, "\n");
				Long id = val.getId();
				boolean systemDefault = val.isSystemDefault();
				boolean inAudit = service.publicIdInAudit(t.getId(), val.getId());
	            String passDate = StringUtils.trimToEmpty(service.getLastPassDate(t.getId(), val.getId()));
		%>
		<tr>
			<td><%= id %></td>
			<td><%= key %></td>
			<td>
				<input type="hidden" value='<%= id %>' name="id" />
				<input type="hidden" value='<%= key %>' name="key" />
				<textarea name="value" rows=3 style="width: 400px;"><%= concated %></textarea>
			</td>
			<td><%= inAudit ? MessageUtil.getMessage(locale, "global.audit.status.audit") : MessageUtil.getMessage(locale, "global.audit.in.force")  %></td>
			<td><%= passDate %></td>
			<td>
				<% if (!systemDefault && !inAudit) { %><input type="button" name="delete" class="btn btn-danger" value="<bean:message key='delete'/>"><% } %>
				<% if (!inAudit) { %><input type="button" name="update" class="btn btn-warning" value="<bean:message key='global.audit.process'/>"><% } %>
			</td>
		</tr>
		<%
			}
		%>
	</TABLE>
</div>
<script>
$('body').on('click', '[name=update]', function (){
	var $tr = $(this).closest("tr");
	var $myForm = $("#myForm");
	$myForm.find("[name=action]").val($(this).attr("name"));
	$myForm.find("[name=id]").val($tr.find("[name=id]").val());
	$myForm.find("[name=key]").val($tr.find("[name=key]").val());
	$myForm.find("[name=value]").val($tr.find("[name=value]").val());
	if ($myForm.find("[name=value]").val().trim() != "") {
		$myForm.submit();
	}else{
		alert("<bean:message key='global.explanation.essential.formal.ex'/>");
	}
});
$('body').on('click', '[name=delete]', function (){
	if(confirm('<bean:message key="sure.del" />')){
		var $tr = $(this).closest("tr");
		var $myForm = $("#myForm");
		$myForm.find("[name=action]").val($(this).attr("name"));
		$myForm.find("[name=id]").val($tr.find("[name=id]").val());
		$myForm.find("[name=key]").val($tr.find("[name=key]").val());
		$myForm.find("[name=value]").val($tr.find("[name=value]").val());
		if ($myForm.find("[name=value]").val()) {
			$myForm.submit();
		}
	}
});
$('body').on('click', '[name=save]', function (){
	var $form = $(this).closest("form");
	if ($form.find("[name=key]").val().trim() != "" && $form.find("[name=value]").val().trim() != "") {
		$form.submit();
	}else{
		alert("<bean:message key='global.explanation.essential.formal.ex'/>");
	}
});

$('body').on('click', '[name=search]', function (){
	var $searchForm = $("#searchForm");
	$searchForm.submit();
});

$("#myTable").tablesorter({
	sortList: [[0]],
	widgets: ['zebra']
})
</script>
</body>
</html>
