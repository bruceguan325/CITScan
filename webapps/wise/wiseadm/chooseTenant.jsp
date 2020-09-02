<%@ page pageEncoding="UTF-8" language="java" %>
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
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.wink.json4j.*" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ include file="/commons/taglib.jsp"%>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

String idStr = request.getParameter("tid");
String redirectTo = request.getParameter("r");
String msg = null;
boolean countdown = true;

if (user == null) {
	response.sendRedirect(request.getContextPath() + WiSeEnv.getAdminContextPath() + "/login.jsp");
}
Set<Integer> allowed = user.getTenantIdSet();
Tenant targetTenant = null;
if (StringUtils.isNotEmpty(idStr)) {
	targetTenant = Tenant.get(Integer.parseInt(idStr));
}
else if (allowed.size() == 1) {
	targetTenant = Tenant.get(allowed.iterator().next());
}

String lastTenantIdFromCookie = WiSeUtils.getFromCookie("SmartRobot.lastTenantIds", request);
List<Integer> lastTenantIds = new ArrayList<Integer>();

if (StringUtils.trimToNull(lastTenantIdFromCookie) != null) {
	String[] tidStrs = StringUtils.split(lastTenantIdFromCookie, ',');
	
	for (String tidStr: tidStrs) {
		try {
			Integer tid = new Integer(tidStr);
			if (allowed.contains(tid)) {
				lastTenantIds.add(tid);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

if (targetTenant != null) {
	if (allowed.contains(targetTenant.getId()) || "admin".equals(user.getLoginName())) {
		Integer latest = new Integer(targetTenant.getId());
		lastTenantIds.remove(latest);
		lastTenantIds.add(0, latest);
		
		if (lastTenantIds.size() > 6) {
			lastTenantIds = lastTenantIds.subList(0, 6);
		}
		
		WiSeUtils.setCookie("SmartRobot.lastTenantIds", StringUtils.join(lastTenantIds, ","), 60*60*24*30, "/", response);
		System.out.println(StringUtils.join(lastTenantIds, ","));
		Tenant.setSession(session, targetTenant);
	
		if (redirectTo != null) {
			response.sendRedirect(redirectTo);
		}
		else {
			response.sendRedirect(request.getContextPath() + WiSeEnv.getAdminContextPath() + "/qaAdmin.jsp");
		}
	}
	else {
		msg = "Forbidden";
		countdown = false;
	}
}

response.setContentType("text/html");
%>
<HTML>
<HEAD>
<TITLE><bean:message key="select.company"/></TITLE>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<link href="<%= request.getContextPath()%>/wiseadm/css/select2.min.css" type="text/css" rel="stylesheet" />
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src="<%= request.getContextPath()%>/wiseadm/js/select2.min.js"></script>
<style>
.clickToChooseTenant {
    margin: 5px 0 0 8px;
	color: #000;
	font-size: 0.8em;
	text-decoration: underline;
}
</style>
</HEAD>
<BODY>
<div class="container">
<div class="jumbotron">
<h1><bean:message key="admin.chooseTenant.title"/></h1>
<p>
<%
if (msg != null) {
%>
<div class="alert alert-danger" role="alert"><%= msg %></div>
<%
}
%>
<form action="chooseTenant.jsp" METHOD="POST">
<input type="hidden" name="r" value="<%= StringEscapeUtils.escapeHtml(StringUtils.defaultString(request.getParameter("r"))) %>" />
<TABLE style="width: 80%;" class="table table-bordered">
<TR>
	<TD valign="top">
	<select name="tid" class="col-md-6">
	<%
	boolean preselected = false;
	List<Tenant> tenants = Tenant.list();
	Integer choosed = lastTenantIds.size() > 0 ? lastTenantIds.get(0) : null;
	
	for (Tenant t: tenants) {
		if (!allowed.contains(t.getId()) || !t.getEnableTenant()) continue;
		
		boolean selected = false;
		if (choosed != null && choosed.intValue() == t.getId()) {
			selected = preselected = true;
		}
	%>
	<option value="<%= t.getId() %>" <%= selected ? "selected" : "" %>><%= t.getNotes() %>(<%= t.getName() %>)</option>
	<%
	}
	%>
	</select>
	</TD>
</TR>
<TR>
	<TD valign="top">
	<%
	for (Integer tid: lastTenantIds) {
		if (tid == choosed) continue;
		Tenant t = Tenant.get(tid);
		if (!t.getEnableTenant()) continue;
		%><a href="#" class="clickToChooseTenant" data-tenant-id="<%= tid %>"><h4><%= t.getNotes() %>(<%= t.getName() %>)</h4></a><br><%
	}
	%>
	</TD>
</TR>
<TR>
	<TD valign="top">
	<input type="submit" class="btn btnSubmit" value="<bean:message key="global.submit"/>">
	<% if (choosed != null && preselected) { %>
	&nbsp;&nbsp;<input type="button" class="btn btn-warning btnCancelAutoSubmit" value="按我取消自動跳轉... ">
	<% } %>
	</TD>
</TR>
</TABLE>
</form>
</div>
</div>
<script>
var countDown;
var counter = 4;
$(document).ready(function() {
	$("select[name=tid]").select2();
	$("select[name=tid]").focus();
	$("select[name=tid]").change(function() {
		$('.btnSubmit').click();
	});
	
	<% if (countdown && choosed != null && preselected) { %>
	countDown = setInterval(function() {
		counter--;
		$('.btnCancelAutoSubmit').val('按我取消自動跳轉... ' + counter + 'sec');
		
		// If the count down is finished, write some text 
		if (counter <= 0) {
		  	clearInterval(countDown);
		  	$('form').submit();
		}
	}, 1000);
	
	$(document).keyup(function(e) {
		if (e.keyCode == 27) { // escape key maps to keycode `27`
	 	  	clearInterval(countDown);
	 	  	$('.btnCancelAutoSubmit').remove();
	 		$("select[name=tid]").focus();
	    }
	});
	
	$('select[name=tid]').on('select2:open', function (e) {
 	  	clearInterval(countDown);
 	  	$('.btnCancelAutoSubmit').remove();
	});
	
	$('.btnCancelAutoSubmit').click(function() {
	  	clearInterval(countDown);
	  	$(this).remove();
		$("select[name=tid]").focus();
	});
	
	$(".clickToChooseTenant").on("click", function(e) {
		e.preventDefault();
	  	clearInterval(countDown);
	  	tid = $(this).attr("data-tenant-id");
	  	$("select[name=tid]").val(tid);
	  	$('form').submit();
	});
	
	$("select[name=tid]").on("click", function() {
	  	clearInterval(countDown);
	  	$('.btnCancelAutoSubmit').remove();
	});
	<% } %>
	
});
</script>
</BODY>
</HTML>
