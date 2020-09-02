<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.log4j.Logger"
import="com.intumit.difflib.text.*"
import="com.intumit.message.MessageUtil"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.WiSeUtils.SimplePagination"
import="com.intumit.solr.util.*"
import="com.intumit.syslog.*"
import="com.intumit.syslog.OperationLogEntity.Status"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E3) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
Logger infoLog = Logger.getLogger("qaOpLogList");
response.addHeader("Expires","-1");
response.addHeader("Pragma","no-cache");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
String targetUser = StringUtils.trimToNull(xssReq.getParameter("targetUser"));
String targetPath = StringUtils.trimToNull(xssReq.getParameter("targetPath"));
String targetEvent = StringUtils.trimToNull(xssReq.getParameter("targetEvent"));
String targetStatus = StringUtils.trimToNull(xssReq.getParameter("targetStatus"));
String targetStart = StringUtils.trimToNull(xssReq.getParameter("targetStart"));
String targetEnd = StringUtils.trimToNull(xssReq.getParameter("targetEnd"));
infoLog.info("User:" + targetUser+", Path:"+targetPath+", Event:"+targetEvent+", Status:"+targetStatus+", Start:"+targetStart+", End:"+targetEnd);
int start = Integer.parseInt(StringUtils.defaultString(xssReq.getParameter("start"), "0"));
int rows = 50;
List<OperationLogEntity> logs = OperationLogEntity.listBy(t.getId(), start, rows, targetUser, targetPath, targetEvent, targetStatus, targetStart, targetEnd);
int total = OperationLogEntity.countBy(t.getId(), targetUser, targetPath, targetEvent, targetStatus, targetStart, targetEnd).intValue();
SimplePagination pagination = new SimplePagination(rows, total, 5, "qaOpLogList.jsp?targetUser="+(targetUser==null?"":targetUser)+"&targetPath="+(targetPath==null?"":targetPath)+"&targetEvent="+(targetEvent==null?"":targetEvent)+"&targetStatus="+(targetStatus==null?"":targetStatus)+"&targetStart="+(targetStart==null?"":targetStart)+"&targetEnd="+(targetEnd==null?"":targetEnd)+"");
List<AdminUser> adminUsers = AdminUserFacade.getInstance().listAll();
HashMap<String, String> paths = OperationLogEntity.getPaths();
HashMap<String, String> restPaths = OperationLogEntity.getRestPaths();
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='top.operation.log'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>

<script src='<%= request.getContextPath() %>/wiseadm/js/My97DatePicker/WdatePicker.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/script/json-viewer/jquery.json-viewer.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/json-viewer/jquery.json-viewer.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" type="text/css" media="all"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css">
<script type="text/javascript">
$( document ).ready(function() {
	$('.btnShowDlg').click(function() {
		var dataId = $(this).attr('data-id');
		var dataAction = $(this).attr('data-action');
		if(dataAction == 'test') {
			try {
				var contents = JSON.parse($('#'+dataId).text());
				$('#'+dataId).jsonViewer(contents);
			}
			catch(err) {}
		}
		$('#'+dataId).dialog('open');
	});
});
</script>
</head>
<body class='contrast-fb'>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<%= pagination.toHtml(start, true) %>
<form id="form">
<table class="table table-striped table-bordered">
<thead>
	<tr>
	<th>
		<button type="submit" name="submit" value="submit" class="btn btn-danger"><bean:message key='operation.log.advanced.search'/></button>
		<br>
		<a href='qaOpLogList.jsp' class='btn btn-success'><bean:message key='operation.log.without.search'/></a>
	</th>
	<th>
		<select name="targetUser">
			<option value=""><bean:message key='operation.log.user'/></option>
			<%
			for (AdminUser adminUser : adminUsers) {
			%>
			<option value="<%=adminUser.getName()%>" <%=(targetUser != null && targetUser.equals(adminUser.getName()+"")) ? "selected" : ""%>><%=adminUser.getName()%>(<%=adminUser.getId()%>)</option>
			<%
			}
			%>
		</select>
	</th>
	<th>
		<select  name="targetPath">
			<option value=""><bean:message key='operation.log.path'/></option>
			<%
			for (String path : paths.keySet()) {
			%>
			<option value="<%=path%>" <%=(targetPath != null && targetPath.equals(path)) ? "selected" : ""%>><%=MessageUtil.getMessage(locale, paths.get(path))%></option>
			<%
			}
            for (String restPath : restPaths.keySet()) {
            %>
            <option value="<%=restPath%>" <%=(targetPath != null && targetPath.equals(restPath)) ? "selected" : ""%>><%=MessageUtil.getMessage(locale, restPaths.get(restPath))%></option>
            <%
            }
			%>
		</select>
	</th>
	<th>
		<select name="targetEvent">
			<option value=""><bean:message key='operation.log.event'/></option>
			<option value="save" <%=(targetEvent != null && targetEvent.equals("save")) ? "selected" : ""%>><bean:message key='operation.log.save'/></option>
			<option value="update" <%=(targetEvent != null && targetEvent.equals("update")) ? "selected" : ""%>><bean:message key='operation.log.update'/></option>
			<option value="copy" <%=(targetEvent != null && targetEvent.equals("copy")) ? "selected" : ""%>><bean:message key='operation.log.copy'/></option>
			<option value="delete" <%=(targetEvent != null && targetEvent.equals("delete")) ? "selected" : ""%>><bean:message key='operation.log.delete'/></option>
			<option value="test" <%=(targetEvent != null && targetEvent.equals("test")) ? "selected" : ""%>><bean:message key='operation.log.test'/></option>
			<option value="import" <%=(targetEvent != null && targetEvent.equals("import")) ? "selected" : ""%>><bean:message key='operation.log.import'/></option>
			<option value="pass" <%=(targetEvent != null && targetEvent.equals("pass")) ? "selected" : ""%>><bean:message key='global.audit.status.pass'/></option>
			<option value="reject" <%=(targetEvent != null && targetEvent.equals("reject")) ? "selected" : ""%>><bean:message key='global.audit.status.reject'/></option>
			<option value="cancel" <%=(targetEvent != null && targetEvent.equals("cancel")) ? "selected" : ""%>><bean:message key='global.audit.cancel'/></option>
		</select>
	</th>
	<th>
		<select name="targetStatus">
			<option value=""><bean:message key='operation.log.status'/></option>
			<option value="<%= Status.SUCCESS %>" <%=(targetStatus != null && targetStatus.equals(Status.SUCCESS.toString())) ? "selected" : ""%>><%=MessageUtil.getMessage(locale, Status.SUCCESS.getTitle())%></option>
			<option value="<%= Status.FAILED %>" <%=(targetStatus != null && targetStatus.equals(Status.FAILED.toString())) ? "selected" : ""%>><%=MessageUtil.getMessage(locale, Status.FAILED.getTitle())%></option>
			<option value="NONE" <%=(targetStatus != null && targetStatus.equals("NONE")) ? "selected" : ""%>>NONE</option>
		</select>
	</th>
	<th>
		<input type="text" name="targetStart" class="Wdate" id="d1" autocomplete="off" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d2\')}'})" value="<%= targetStart == null ? "" : targetStart %>" />&nbsp-&nbsp
	 	<input type="text" name="targetEnd" class="Wdate" id="d2" autocomplete="off" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d1\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= targetEnd == null ? "" : targetEnd %>" />	  
	</th>
	<th>IP</th>
	<th><bean:message key='operation.log.parameter'/></th>
	<th><bean:message key='operation.log.detail'/></th>
	</tr>
</thead>
<% for (OperationLogEntity log: logs) { %>
    <tr>
    <td></td>
    <td class="span1">
    <i class='icon-user text-success'></i>
    <% String identityStr = log.getIdentity();
    int i = identityStr.indexOf(':'); 
    %>
    <%= identityStr.substring(i+1) %> 
    </td>
    <td class="span6">
    <%
    if (OperationLogEntity.getPath(log.getNamespace()) != null) {
    %>
    <%= MessageUtil.getMessage(locale, OperationLogEntity.getPath(log.getNamespace())) %>
    <%
    } else if (OperationLogEntity.getRestPath(log.getNamespace()) != null) {
    %>
    <%= MessageUtil.getMessage(locale, OperationLogEntity.getRestPath(log.getNamespace())) %>
    <%
    }
    %>
    </td>
    <td class="span2">
    <% String eventStr =  log.getEvent(); %>
    <% if (eventStr.equals("save")) { %>
    	<%= "儲存" %>
    <% } else if (eventStr.equals("update")) { %>
    	<%= "修改" %>
    <% } else if (eventStr.equals("delete")) { %>
    	<%= "刪除" %>
    <% } else if (eventStr.equals("pass")) { %>
    	<%= "通過" %>
    <% } else if (eventStr.equals("reject")) { %>
    	<%= "駁回" %>
    <% } else if (eventStr.equals("import")) { %>
    	<%= "匯入" %>
    <% } else { %>
    	<%= log.getEvent() %>
    <% } %>
    </td>
    <td class="span1">
    <%
    if (log.getStatusMessage() != null && log.getStatusMessage().equals(Status.SUCCESS)) {
    %>
    <span class="glyphicon glyphicon-ok text-success"></span>
    <%
    } else if (log.getStatusMessage() != null && log.getStatusMessage().equals(Status.FAILED)) {
    %>
    <span class="glyphicon glyphicon-remove text-error"></span>
    <%
    } else {
   	%>
    <span class="glyphicon glyphicon-minus"></span>
    <%	
    }
    %>
    </td>
    <td class="span1">
    <%= log.getTimestamp() %>
    </td>
    <td class="span1">
    <small class='text-error'>(<%= log.getClientIp() %>)</small>
    </td>
    <td class="span6">
    <div id="parameterDlg<%= log.getId() %>" style="display:none; z-index:999; background-color: white;"><%= log.getParameters() %></div>
    <a href="#" data-id="parameterDlg<%= log.getId() %>" class="btn btn-success btnShowDlg" role="button"><bean:message key='operation.log.show.more'/></a>
    <script type="text/javascript">
    $( "#parameterDlg<%= log.getId() %>").dialog({
		minWidth: 600,
		minHeight: 300,
		height: 300,
		autoOpen: false
	});
    </script>
    </td>
    <td class="span6">
    <div id="moreDetailsDlg<%= log.getId() %>" style="display:none; z-index:999; background-color: white;"><%= log.getMoreDetails().replace("\n", "<br>") %></div>
    <a href="#" data-id="moreDetailsDlg<%= log.getId() %>" data-action="<%= log.getEvent() %>" class="btn btn-success btnShowDlg" role="button"><bean:message key='operation.log.show.more'/></a>
    <script type="text/javascript">
    $( "#moreDetailsDlg<%= log.getId() %>").dialog({
		minWidth: 600,
		minHeight: 300,
		height: 300,
		autoOpen: false
	});
    </script>
    </td>
    </tr>
<% } %>
</table>
</form>
<%= pagination.toHtml(start, true) %>
</body>
</html>