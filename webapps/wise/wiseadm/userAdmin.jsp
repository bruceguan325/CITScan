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
String idStr = request.getParameter("id");
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
	var id = $('[name=id]') ? $('[name=id]').val() : ''; 
	$('[name=loginName]').blur(function(e) {
		$.ajax({
		  	  type: 'POST',
		  	  url: 'checkAccountAjax.jsp',
		  	  dataType: 'json',
		  	  data: {
		  		  account: $('[name=loginName]').val(),
		  		  id: id
		  	  },
		  	  success: function(resp) {
		  		  if(resp.duplicate) {
		  			  $('#dupAlert').show();
		  		  }
		  		  else {
		  			  $('#dupAlert').hide();
		  		  }
		  	  }
		});
	});
	
	$('#saveBtn').click(function(e) {
		e.preventDefault();
		$.ajax({
		  	  type: 'POST',
		  	  url: 'checkAccountAjax.jsp',
		  	  dataType: 'json',
		  	  data: {
		  		  account: $('[name=loginName]').val(),
		  		  id: id
		  	  },
		  	  success: function(resp) {
		  		  if(resp.duplicate) {
		  			  alert('<bean:message key="user.account.duplicate"/>');
		  		  }
		  		  else {
		  			  $('#myForm').submit();
		  		  }
		  	  }
		});
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
boolean exceedMaxUser = LicenseChecker.isExceedMaxUser();
if ("create".equalsIgnoreCase(action)) {
	if(exceedMaxUser) return;
	AdminUserFacade.getInstance().save("New AdminUser");
	%>
	<%= "New AdminUser" %> created!
	<%
}
else if ("edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	if(exceedMaxUser && "duplicate".equalsIgnoreCase(action)) return;
	AdminUser admUser = AdminUserFacade.getInstance().get(new Integer(idStr));
	%>
	<bean:message key="edit.user"/>
	<form action="userAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-striped">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td><bean:message key="user.id"/></td>
		<td class="col-md-5">
			<input type="hidden" name="id" value="<%= admUser.getId() %>">
			<%= admUser.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key="user.name"/></td>
		<td class="col-md-5">
			<input type="text" size="40" name="name" value="<%= admUser.getName() %>">
		</td>
		<td><bean:message key="user.name.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="user.account"/></td>
		<td class="col-md-5">
			<input type="text" size="40" name="loginName" value="<%= admUser.getLoginName() %>"><br>
			<span id="dupAlert" style="display:none;"><font color="red" size="2"><bean:message key="user.account.duplicate"/></font></span>
		</td>
		<td><bean:message key="user.account.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="user.password"/></td>
		<td class="col-md-5">
			<input type="password" size="40" name="password" value="">
		</td>
		<td><bean:message key="user.password.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="global.department"/></td>
		<td width="300">
			<input type="text" size="40" name="department" value="<%= admUser.getDepartment() %>">
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key="global.signin.email"/></td>
		<td width="300">
			<input type="text" size="40" name="email" value="<%= admUser.getEmail() %>">
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key="group.list"/></td>
		<td class="col-md-5">
			<select name="adminGroups" multiple="multiple" class="col-md-12" style="height: 120px;">
			<% 
				Set<String> selectedAGs = (admUser.getAdminGroups() != null ? new HashSet<String>(Arrays.asList(StringUtils.split(admUser.getAdminGroups(), ","))) : null);
			    for (AdminGroup ag: AdminGroupFacade.getInstance().listAll()) { %>
				    <option value="<%= ag.getId() %>" <%= selectedAGs != null ? (selectedAGs.contains("" + ag.getId()) ? " selected" : "") : "" %>><%= ag.getName() %></option>
			<%  } %>
			</select>
		</td>
		<td><bean:message key="group.list.ex1"/><br><b><bean:message key="group.list.ex2"/></b></td>
	</tr>
	<tr>
		<td><bean:message key="company.management.list"/></td>
		<td class="col-md-5">
			<select name="tenantIds" multiple="multiple" class="col-md-12" style="height: 220px;">
			<% 
			Set<String> selectedTenants = (admUser.getTenantIds() != null ? new HashSet<String>(Arrays.asList(StringUtils.split(admUser.getTenantIds(), ","))) : null);
			for (Tenant t: Tenant.list()) { 
				TenantAdminGroup tag = TenantAdminGroup.getByTenantAndAdmin(t.getId(), admUser.getId());
				String append = "";
				String prepend = "";
				
				if (tag != null) {
					prepend = " *** ";
					append = " - << " + tag.getAdminGroup().getName() + " >>";
				}
			%>
				<option value="<%= t.getId() %>" <%= selectedTenants != null ? (selectedTenants.contains("" + t.getId()) ? " selected" : "") : "" %>><%= prepend %><%= t.getName() %> - <%= t.getNotes() %><%= append %></option>
			<% 
			}
			%>
			</select>
		</td>
		<td><bean:message key="company.management.list.ex"/><br></td>
	</tr>
	<tr>
		<td></td>
		<td>
			<input id="saveBtn" class="btn btn-danger" type="submit" value="<bean:message key='submit'/>">
			<a class="btn btn-warning" href="tenantAdminGroupEdit.jsp?action=EDIT&adminId=<%= admUser.getId() %>"><bean:message key="advanced.permission.config"/></a>
		</td>
		<td></td>
	</tr>
	</table>
	</form>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	AdminUser admUser = new AdminUser();
	if (idStr != null) {
		admUser.setId(new Integer(idStr));
	}
	else if(exceedMaxUser) return;
	
	admUser.setName(request.getParameter("name"));
	admUser.setLoginName(request.getParameter("loginName"));
	admUser.setDepartment(request.getParameter("department"));
	admUser.setEmail(request.getParameter("email"));
	
	if (request.getParameterValues("adminGroups") != null)
		admUser.setAdminGroups(StringUtils.join(Arrays.asList(request.getParameterValues("adminGroups")), ","));
	else
		admUser.setAdminGroups("");
	
	if (request.getParameterValues("tenantIds") != null)
		admUser.setTenantIds(StringUtils.join(Arrays.asList(request.getParameterValues("tenantIds")), ","));
	else
		admUser.setTenantIds("");
	

	if (admUser.getLoginName().matches("(?m)^[0-9a-zA-Z]+$")) {
		if (idStr != null && StringUtils.isEmpty(request.getParameter("password"))) {
			AdminUser oldUser = AdminUserFacade.getInstance().get(new Integer(idStr));
			admUser.setPassword(oldUser.getPassword());
		}
		else {
			admUser.setPassword(AdminUser.encryptPassword(request.getParameter("password")));
		}
		AdminUser oldUser = AdminUserFacade.getInstance().getByLoginName(admUser.getLoginName());
		if(StringUtils.isBlank(idStr) && AdminUserFacade.getInstance().getByLoginName(admUser.getLoginName()) != null) {
		%>
		   (<%= admUser.getName() %>)<bean:message key="user.account.duplicate"/>
		<%	
		}
		else {
			AdminUserFacade.getInstance().saveOrUpdate(admUser);
			%>
			<bean:message key="user"/>(<%= admUser.getName() %>)<bean:message key="already.submit"/>
			<%
		}
	}
	else {
		%>
		<bean:message key="user.account.illegal"/>(<%= admUser.getLoginName() %>)!
		<%
	}
}
else if ("sudo".equalsIgnoreCase(action)) {
	AdminUser admUser = AdminUserFacade.getInstance().get(new Integer(idStr));
	AdminUserFacade.getInstance().setSession(session, admUser);
	%>
	<bean:message key="already.transfiguration"/>(<%= admUser.getName() %>)<bean:message key="user"/>!
	<script>
	window.parent.topFrame.location.reload();
	</script>
	<%
}
else if ("delete".equalsIgnoreCase(action)) {
	AdminUserFacade.getInstance().delete(new Integer(idStr));
	%>
	<bean:message key="user"/>(<%= idStr %>)<bean:message key="already.remove"/>
	<%
}
%>
<TABLE width="100%" class="table table-striped" id="users">
	<THEAD>
	<TR>
		<TH valign="top"><bean:message key="item"/></TH>
		<TH valign="top"><bean:message key="user"/><bean:message key="user.name"/></TH>
		<TH valign="top"><bean:message key="user"/><bean:message key="user.account"/></TH>
		<TH valign="top"><bean:message key="group"/></TH>
		<TH valign="top"><bean:message key="tenant.list"/></TH>
		<TH valign="top"><bean:message key="operation"/></TH>
	</TR>
	</THEAD>
<%
List<AdminUser> list = AdminUserFacade.getInstance().listAll();
for (int i=0; i < list.size(); i++) {
	AdminUser admGrp = list.get(i);
	int id = admGrp.getId();
%>
	<TR>
		<TD align="center" valign="top" width="45"><%= i+1 %></TD>
		<TD align="center" valign="top" class="col-md-3"><%= admGrp.getName() %> (<%= admGrp.getId() %>)</TD>
		<TD align="center" valign="top"><%= admGrp.getLoginName() %></TD>
		<TD align="center" valign="top"><%= admGrp.getAdminGroups() %></TD>
		<TD align="center" valign="top" class="col-md-2" style='word-wrap: break-word;'><%= admGrp.getTenantIds() %></TD>
		<TD align="center" valign="top" class="col-md-4">
			<a class="btn btn-success" href="userAdmin.jsp?action=SUDO&id=<%= id %>"><bean:message key="transfiguration"/></a>
			<a class="btn btn-primary" href="userAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key="modify"/></a>
			<a class="btn btn-danger" onclick="return confirm('<bean:message key="sure.del.user"/>');" href="userAdmin.jsp?action=DELETE&id=<%= id %>"><bean:message key="delete"/></a>
			<% if(!exceedMaxUser) { %>
			<a class="btn btn-default" onclick="return confirm('<bean:message key="sure.copy.user"/>');" href="userAdmin.jsp?action=DUPLICATE&id=<%= id %>"><bean:message key="copy"/></a>
			<% } %>
			<a class="btn btn-warning" href="tenantAdminGroupEdit.jsp?action=EDIT&adminId=<%= id %>"><bean:message key="advanced.permission.config"/></a>
		</TD>
	</TR>
<%
}
%>
</TABLE>
<% if(!exceedMaxUser) {%>
<br>
<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/userAdmin.jsp?action=create"><bean:message key="create.new.user"/></A>
<% } %>
<script>
$(document).ready(function() {

	$('#users').DataTable(
		{ 
			"paging": true, 
			"stateSave": true,
	        "pagingType": "full_numbers",
			"dom": '<<"row"<"col-md-6"p><"col-md-3 dt-mt"f><"col-md-3 dt-mt"l>><"clear">t<"clear"><"row"<"col-md-6"p><"col-md-3 dt-mt"f><"col-md-3 dt-mt"l>>>',
			"order": [[ 2, "asc" ]],
			"columnDefs": [
			 { type: "numeric-comma", targets: 0 },
			 { type: "numeric-comma", targets: 3 },
			 { type: "numeric-comma", targets: 4, 
			   render: function ( data, type, row ) {
		            return (data.length > 10) ? data.substr( 0, 10 ) + "..." : data;
		       } 
			 }
	        ]
		});
});
</script>
</BODY>
</HTML>
