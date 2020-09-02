<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="org.apache.commons.lang.*"
	 import="org.apache.solr.core.*"
	 import="org.apache.solr.servlet.*"
	 import="org.apache.solr.common.*"
	 import="com.intumit.solr.SearchManager"
	 import="com.intumit.solr.tenant.*"
	 import="com.intumit.solr.util.*"
	 import="com.intumit.solr.robot.*"
	 import="org.apache.commons.lang.StringUtils"
     import="org.dom4j.*"
	 import="com.intumit.solr.admin.*"
	 import="com.intumit.license.LicenseChecker"
	 import="com.intumit.message.MessageUtil"
%><%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}

Locale sessionLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
if(sessionLocale == null) sessionLocale = Locale.TAIWAN;
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/wiseadm/css/dropzone.css" type="text/css" rel="stylesheet" />
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-slider.css" rel="stylesheet">
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciPlugin.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciSortable.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.iframe-transport.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.fileupload.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-slider.js"></script>
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

function deleteApikey(url){
	if(confirm('<bean:message key="sure.del.apikey"/>')){
	document.dataSetListForm.action=url;
	document.dataSetListForm.method='POST';
	document.dataSetListForm.submit();
	top.leftFrame.location.reload();
	}else{
	return;
	}
}
</script>
<style>
td-form {
	width: 50%;
}

td-form input,textarea {
	width: 100%;
}
</style>
</HEAD>
<BODY>
<%
String action = request.getParameter("action");
String idStr = request.getParameter("id");
String tidStr = request.getParameter("tid");
Tenant t = Tenant.get(Integer.parseInt(tidStr));

if ("create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	Apikey key = null;
	
	if (idStr == null) {
		key = new Apikey();
		key.setTenantId(t.getId());
	}
	else {
		key = Apikey.get(Long.parseLong(idStr));
	}
	%>
	<form action="apikeyAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<input type="hidden" name="tid" value="<%= t.getId() %>">
	<table class="table table-bordered">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td><bean:message key="apikey.id"/></td>
		<td class="td-form">
			<input type="hidden" name="id" value="<%= key.getId() %>">
			<%= key.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key="apikey.name"/></td>
		<td class="td-form">
			<input type="text" size="40" name="name" value="<%= StringUtils.trimToEmpty(key.getName()) %>">
		</td>
		<td><bean:message key="apikey.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="apikey.publishDate"/></td>
		<td class="td-form">
			<input type="text" size="40" name="publishDate" value="<%= key.getPublishDate() != null ? sdf.format(key.getPublishDate()) : "" %>">
		</td>
		<td>Format: yyyy/MM/dd HH:mm (ex. 2017/12/01 08:00)</td>
	</tr>
	<tr>
		<td><bean:message key="apikey.expireDate"/></td>
		<td class="td-form">
			<input type="text" size="40" name="expireDate" value="<%= key.getExpireDate() != null ? sdf.format(key.getExpireDate()) : "" %>">
		</td>
		<td>Format: yyyy/MM/dd HH:mm (ex. 2017/12/31 23:00)</td>
	</tr>
	<tr>
		<td><bean:message key="tenant.apikey"/></td>
		<td class="td-form">
			<label><%= StringUtils.trimToEmpty(key.getApikey()) %></label>
		</td>
		<td><bean:message key="tenant.apikey.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="function.setting"/></td>
		<td class="col-md-8">
       	<%-- 限流設定 --%>
		<%
	    String checked = null;
	    checked = key.getEnableRateLimitByIP() ? "checked" : "";
		%>
	    	<input type="checkbox" name="enableRateLimitByIP" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.rateLimitByIP"/> 
	    	(<bean:message key="request.per.second"/> <input type="text" name="rateLimitByIpPerSec" size="4" value="<%= key.getRateLimitByIpPerSec() %>">)<br/>
		<%
	    checked = key.getEnableRateLimitByQAContext() ? "checked" : "";
		%>
		<br>
	    	<input type="checkbox" name="enableRateLimitByQAContext" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.rateLimitByQAContext"/> 
	    	(<bean:message key="request.per.second"/> <input type="text" name="rateLimitByQAContextPerSec" size="4" value="<%= key.getRateLimitByQAContextPerSec() %>">)<br/>
		<%
    		checked = key.getEnableApikey() ? "checked" : "";
    		%>
	    	<input type="checkbox" name="enableApikey" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled"/> <br/>
		</td>
		<td></td>
	</tr>
	<tr>
		<td colspan=3>
			<input class="btn btn-primary" type="submit" value="<bean:message key='save'/>">
		</td>
	</tr>
	</table>
	</form>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	Apikey key = null;
	
	if (idStr == null) {
		key = new Apikey();
		key.setTenantId(t.getId());
	}
	else {
		key = Apikey.get(Long.parseLong(idStr));
	}

	if (key.getTenantId() != t.getId())
		return;

	key.setName(request.getParameter("name"));
	key.setEnableApikey(request.getParameter("enableApikey") != null);
	
	if (StringUtils.isNotEmpty(request.getParameter("publishDate"))) {
		Date d = sdf.parse(request.getParameter("publishDate"));
		key.setPublishDate(d);
	}
	
	if (StringUtils.isNotEmpty(request.getParameter("expireDate"))) {
		Date d = sdf.parse(request.getParameter("expireDate"));
		key.setExpireDate(d);
	}

	// 限流區
	key.setEnableRateLimitByIP(request.getParameter("enableRateLimitByIP") != null);
	key.setEnableRateLimitByQAContext(request.getParameter("enableRateLimitByQAContext") != null);
	key.setRateLimitByIpPerSec(Integer.parseInt(request.getParameter("rateLimitByIpPerSec")));
	key.setRateLimitByQAContextPerSec(Integer.parseInt(request.getParameter("rateLimitByQAContextPerSec")));

	if (request.getParameter("enableApikey") != null) {
		if (StringUtils.isEmpty(key.getApikey())) {
			key.setApikey(Apikey.genApikey());
		}
	}
	else {
		key.setApikey(null);
	}

	Apikey.saveOrUpdate(key);
	//com.intumit.solr.robot.dictionary.bank.DepositInterestRateDict.clear(key.getId());
	%>
	Saved!
	<%
}
else if ("delete".equalsIgnoreCase(action)) {
	Apikey key = Apikey.get(Long.parseLong(idStr));
	
	if (key.getTenantId() != t.getId())
		return;
	
	Apikey.delete(key.getId());
}
%>
<script>
$(function() {
	$.datepicker.setDefaults({
		dateFormat: 'yy/mm/dd'
	});
    $( 'input[name^="date"]' ).datepicker();
});

</script>
<TABLE width="100%" class="table table-striped">
	<THEAD>
	<TR>
		<TH align="center" valign="top"><bean:message key='num'/></TH>
		<TH align="center" valign="top"><bean:message key='apikey.name'/></TH>
		<TH align="center" valign="top">APIKEY</TH>
		<TH align="center" valign="top">Date range</TH>
		<TH align="center" valign="top"><bean:message key='is.enabled'/></TH>
		<TH align="center" valign="top"><bean:message key='apikey.callCounter'/></TH>
		<TH align="center" valign="top"><bean:message key='operation'/></TH>
	</TR>
	</THEAD>
<%
if (t != null) {
List<Apikey> list = Apikey.list(t.getId());
for (int i=0; i < list.size(); i++) {
	Apikey admGrp = list.get(i);
	Long id = admGrp.getId();
%>
	<TR>
		<TD valign="top" class="col-md-1"><%= admGrp.getId() %></TD>
		<TD valign="top" class="col-md-1"><%= admGrp.getName() %></TD>
		<TD valign="top" class="col-md-2"><%= admGrp.getApikey() %></TD>
		<TD valign="top" class="col-md-2">
		<%= (admGrp.getPublishDate() != null ? admGrp.getPublishDate() : "*") 
			+ " ~ " 
			+ (admGrp.getExpireDate() != null ? admGrp.getExpireDate() : "*") %></TD>
		<TD valign="top" class="col-md-2"><%= admGrp.getEnableApikey() %></TD>
		<TD valign="top" class="col-md-2"><%= admGrp.getCallCounter() != null ? admGrp.getCallCounter() : "尚未使用" %></TD>
		<TD valign="top" class="col-md-4">
			<a class="btn btn-primary" href="apikeyAdmin.jsp?action=EDIT&id=<%= id %>&tid=<%= t.getId() %>"><bean:message key='modify'/></a>
			<a class="btn btn-danger" onclick="return confirm('<bean:message key="sure.del.apikey2"/>');" href="apikeyAdmin.jsp?action=DELETE&id=<%= id %>&tid=<%= t.getId() %>"><bean:message key='delete'/></a>
			<a class="btn btn-default" onclick="return confirm('<bean:message key="sure.copy.apikey"/>');" href="apikeyAdmin.jsp?action=DUPLICATE&id=<%= id %>&tid=<%= t.getId() %>"><bean:message key='copy'/></a>
		</TD>
	</TR>
<%
}
%>
</TABLE>
<a class="btn btn-warning" target="mainFrame" href="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/tenantAdmin.jsp?action=EDIT&id=<%= t.getId() %>">Back</a>
&nbsp;<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/apikeyAdmin.jsp?action=create&tid=<%= t.getId() %>"><bean:message key='create.new.apikey'/></A>
<script>
$('.slider').slider();
</script>
<%
}
%>
</BODY>
</HTML>
