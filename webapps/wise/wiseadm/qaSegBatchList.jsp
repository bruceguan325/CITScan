<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.XssHttpServletRequestWrapper"
import="com.intumit.syslog.OperationLogEntity"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='global.batch.segment.change'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
SegmentBatchTask.Status status = SegmentBatchTask.Status.valueOf(StringUtils.defaultString(request.getParameter("status"), "WAIT"));

String action = request.getParameter("action");
if (action != null) {
	if ("save".equals(action)) {
		String keyword = StringUtils.lowerCase(request.getParameter("keyword"));
		String renameTo = StringUtils.lowerCase(request.getParameter("renameTo"));
		String toBeMerged = StringUtils.lowerCase(request.getParameter("toBeMerged"));
		
		Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		
		if (log != null && StringUtils.isNotEmpty(keyword) && (StringUtils.isNotEmpty(renameTo) || StringUtils.isNotEmpty(toBeMerged))) {
			SegmentBatchTask sbTask = SegmentBatchTask.save(t.getId(), keyword, toBeMerged, renameTo, SegmentBatchTask.Purpose.MANUAL_ADD, SegmentBatchTask.Status.WAIT, "Manual added by " + AdminUser.getPrintableName(user.getId()));
			if (sbTask != null) {
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} else {
				String errorMsg = MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "seg.batch.column.warning");
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(errorMsg);
				
				out.println("<div class='alert alert-info'>");
				out.println(errorMsg);
				out.println("</div>");
			}
		}
		else {
			String errorMsg = MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "seg.batch.column.blank.warning");
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
			log.appendToMoreDetails(errorMsg);
			
			out.println("<div class='alert alert-info'>");
			out.println(errorMsg);
			out.println("</div>");
		}
		log.update();
	}
	else {
		Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		
		Long id = new Long(request.getParameter("id"));
		SegmentBatchTask sbt = SegmentBatchTask.get(id);
		if (sbt.getTenantId() != t.getId()) return; // 安全確認
		
		if ("done".equals(action)) {
			sbt.setStatus(SegmentBatchTask.Status.DONE);
			sbt.appendLog(user, "Set status to DONE.");
			SegmentBatchTask.saveOrUpdate(sbt);
		}
		else if ("delete".equals(action) && log != null) {
			sbt.setStatus(SegmentBatchTask.Status.REJECT);
			sbt.appendLog(user, "Set status to REJECT.");
			SegmentBatchTask.saveOrUpdate(sbt);
			log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			log.update();
		}
		else if ("viewLog".equals(action)) {
			out.println("<div class='alert alert-info'>");
			out.println(com.intumit.solr.util.WiSeUtils.nl2br(sbt.getLog()));
			out.println("</div>");
		}
	}
}

int no = 1;
List<SegmentBatchTask> patterns = SegmentBatchTask.list(t.getId(), null, status);
request.setAttribute("patterns", patterns);
%>
<table class='table table-strip'>
<thead>
	<tr>
	<th>No</th>
	<th>Keyword</th>
	<th><bean:message key='change.name'/></th>
	<th><bean:message key='keyword.be.merge'/></th>
	<th>Timestamp</th>
	<th><bean:message key='global.status'/></th>
	<th class='col-sm-5'><bean:message key='operation'/></th>
	</tr>
</thead>
<% int loop = 1; for (SegmentBatchTask p: patterns) { %>
<tr>
	<td><%= loop++ %></td>
	<td class='col-md-1'><strong><%= p.getKeyword() %></strong></td>
	<td class='col-md-1'><strong><%= StringUtils.trimToEmpty(p.getRenameTo()) %></strong></td>
	<td style="word-wrap:break-word;"><%= StringUtils.trimToEmpty(p.getToBeMerged()) %></td>
	<td><%= p.getEntryTimestamp() %></td>
	<td><%= p.getStatus() %></td>
	<td>
		<a href='qaSegBatchEditor.jsp?id=<%= p.getId() %>' class='btn btn-primary' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span>&nbsp;<bean:message key='qa.edit'/></a>
		<a href='qaSegBatchEditor.jsp?mode=template&id=<%= p.getId() %>' class='btn btn-primary' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-book"></span>&nbsp;<bean:message key="top.question.method"/></a>
		<a href='qaSegBatchList.jsp?action=viewLog&id=<%= p.getId() %>' class='btn btn-info' title="<bean:message key='global.show'/><bean:message key='qa.service.log'/>"><span class="glyphicon glyphicon-time"></span></a>
		<a href='qaSegBatchList.jsp?action=done&id=<%= p.getId() %>' class='btn btn-success btnDone' title="<bean:message key='global.finish'/>"><span class="glyphicon glyphicon-ok"></span></a>
		<a href='qaSegBatchList.jsp?action=delete&id=<%= p.getId() %>' class='btn btn-danger btnReject' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-remove"></span></a>
	</td>
</tr>
<% } %>
</table>
	<a href='qaSegBatchList.jsp?status=DONE' class='btn btn-success'><bean:message key='global.show'/><bean:message key='global.finish'/></a>
	<a href='qaSegBatchList.jsp?status=REJECT' class='btn btn-danger'><bean:message key='global.show'/><bean:message key='delete'/></a>
	<a href='#' class='btn btn-warning btnCreate'><bean:message key='global.add'/></a>
</div>
<div id="createFormDialog" title="<bean:message key='manual.new.add'/>" style="display:none; background-color: #F8F8F8;">
<form id="createForm" action="qaSegBatchList.jsp" class="form-horizontal" method="POST">
	<input type="hidden" name="action" value="save">
	<div class='col-md-12'>
		<div class='form-group'>
			<label class='col-md-3 control-label'><bean:message key='global.keyword'/></label>
			<div class='col-md-9'>
			<input name="keyword" class='form-control' placeholder='<bean:message key="input.required"/>' value=""/>
			</div>
		</div>
		<div class='form-group'>
			<label class='col-md-3 control-label'><bean:message key='change.name.to'/></label>
			<div class='col-md-9'>
			<input name="renameTo" class='form-control' placeholder='<bean:message key="change.name.to.ex"/>' value=""/>
			</div>
		</div>
		<div class='form-group'>
			<label class='col-md-3 control-label'><bean:message key='word.be.merge'/></label>
			<div class='col-md-9'>
			<textarea name="toBeMerged" rows="5" class='form-control' placeholder='<bean:message key="word.split.comma"/>'></textarea>
			</div>
		</div>
		<div class='form-group'>
			<label class='col-md-2 control-label'></label>
			<div class='col-md-10'>
			<div class='alert alert-danger'>
			<bean:message key='seg.batch.list.warning'/>
			</div>
			</div>
		</div>
		<div class='form-group'>
			<label class='col-md-2 control-label'></label>
			<div class='col-md-10'>
			<button type="submit" class='btn btn-danger'><bean:message key='global.submit'/></button>
			</div>
		</div>
	</div>
</form>
</div>
<script>

$('#createFormDialog').dialog({
		minWidth: 800,
		minHeight: 500,
		stack: true,
		autoOpen: false
	  });
	  
$('.btnCreate').click(function() {
	$('#createFormDialog').dialog('open');
});

$('.btnDone').click(function() {
	if (confirm('Mark this entry finished (Yes/No)?')) {
		return true;
	}
	return false;
});

$('.btnReject').click(function() {
	if (confirm('Reject this entry (Yes/No)?')) {
		return true;
	}
	return false;
});
</script>
</body>
</html>