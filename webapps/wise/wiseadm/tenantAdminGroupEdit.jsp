<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.intumit.quartz.ScheduleUtils"%>
<%@page import="com.intumit.quartz.Job"%>

<%@page import="org.dom4j.*"%>
<%@page import="org.apache.commons.httpclient.methods.GetMethod"%>
<%@page import="org.apache.commons.httpclient.HttpClient"%>
<%@page import="org.apache.commons.httpclient.auth.AuthScope"%>
<%@page import="org.apache.commons.httpclient.UsernamePasswordCredentials"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page import="com.intumit.license.LicenseChecker"%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
String action = request.getParameter("action");
String idStr = request.getParameter("adminId");
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/wiseadm/css/jquery.dataTables.min.css" type="text/css" rel="stylesheet"/>
<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/datatables/jquery.dataTables.numeric-comma.js"></script>
<script src="<%= request.getContextPath() %>/scripts/bs3/bootstrap.min.js"></script>
<script>
function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

$(document).ready(function() {
	
	
	$('#saveBtn').click(function(e) {
		e.preventDefault();
		$('#myForm').submit();
	});
});
	

</script>
<style>
.dt-mt {
	margin-top: 10px;
}
</style>
</HEAD>
<BODY>
<%
String msg = null;
AdminUser admUser = AdminUserFacade.getInstance().get(new Integer(idStr));

if ("save".equalsIgnoreCase(action)) {
	List<TenantAdminGroup> existTags = TenantAdminGroup.listByAdmin(admUser.getId());
	
	for (TenantAdminGroup tag: existTags) {
		TenantAdminGroup.delete(tag.getId());
	}
	
	for (Integer tid: admUser.getTenantIdSet()) {
		String groupId = StringUtils.trimToNull(request.getParameter("tenant-" + tid));
		
		if (groupId != null) {
			TenantAdminGroup tag = new TenantAdminGroup(tid, admUser.getId(), new Integer(groupId));
			TenantAdminGroup.saveOrUpdate(tag);
		}
	}
	
	msg = "Saved";
}

%>
<h3><bean:message key="advanced.permission.config"/></h3>
<% if (msg != null) { %>
	<div class="alert alert-danger"><%= msg %></div>
<% } %>
<form action="tenantAdminGroupEdit.jsp" name="myForm" id="myForm">
<input type="hidden" name="action" value="save">
<table class="table table-striped">
<tr>
	<td class="col-md-2"><bean:message key="user.name"/></td>
	<td class="col-md-8">
		<input type="hidden" name="adminId" value="<%= admUser.getId() %>">
		<%= admUser.getName() %>  (<%= admUser.getId() %>)
	</td>
	<td>&nbsp;</td>
</tr>
<tr>
	<td><bean:message key="company.management.list"/></td>
	<td class="col-md-8">
		<table class="table table-bordered" id="company-list">
		<% 
		Set<String> selectedTenants = (admUser.getTenantIds() != null ? new HashSet<String>(Arrays.asList(StringUtils.split(admUser.getTenantIds(), ","))) : null);
		for (String tid: selectedTenants) { 
			Tenant t = Tenant.get(Integer.parseInt(tid));
			AdminGroup defaultAG = admUser.getDefaultGroup();
			TenantAdminGroup tag = TenantAdminGroup.getByTenantAndAdmin(t.getId(), admUser.getId());
		%>
			<tr>
				<td style="padding-right: 30px;"><%= t.getName() %> - <%= t.getNotes() %></td>
				<td>
					<select name="tenant-<%= t.getId() %>" class="col-md-12" >
						<option value=""> -- 帳號預設群組 (<%= defaultAG != null ? defaultAG.getName() : "N/A" %>) -- </option>
					<% for (AdminGroup ag: AdminGroupFacade.getInstance().listAll()) { %>
						    <option value="<%= ag.getId() %>" <%= ((tag != null && tag.getGroupId() == ag.getId()) ? " selected" : "") %>><%= ag.getName() %></option>
					<%  } %>
					</select>
				</td>
			</tr>
		<% 
		}
		%>
		</table>
	</td>
	<td></td>
</tr>
<tr>
	<td></td>
	<td>
		<input id="saveBtn" class="btn btn-danger" type="submit" value="<bean:message key='submit'/>">
	</td>
	<td></td>
</tr>
</table>
</form>
<%
%>
<script>
$(document).ready(function() {

	$('#company-list').DataTable(
		{ 
			"paging": true, 
			"stateSave": true,
	        "pagingType": "full_numbers",
			"dom": '<<"row"<"col-md-2"p><"col-md-8 dt-mt"f><"col-md-2 dt-mt"l>>',
			"order": [[ 2, "asc" ]],
			"columnDefs": [
	        ]
		});
});
</script>
</BODY>
</HTML>
