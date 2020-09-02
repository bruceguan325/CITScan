<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.io.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.mchange.io.FileUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) {
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
<meta charset="utf-8">
<title><bean:message key='scenario.management'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<table class='table table-strip'>
<thead>
	<tr>
	<th><bean:message key='global.name'/></th>
	<th><bean:message key='global.description'/></th>
	<th><bean:message key='operation'/></th>
	</tr>
</thead>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

if (!t.getEnableScenario()) {
	out.println("Scenario management not enable.");
	return;
}

Map<String, JSONObject> scenarios = new HashMap<String, JSONObject>();
File theDialogJsonFile = new File(new File(WiSeEnv.getHomePath()), "dialogs/" + t.getId() + ".json");
String dialogsConfigStr = FileUtils.getContentsAsString(theDialogJsonFile, "UTF-8");
if (StringUtils.isNotEmpty(dialogsConfigStr)) {
	JSONObject dialogsConfigJson = new JSONObject(dialogsConfigStr);
	System.out.println("Got Dialogs for tenant[" + t.getId() + "]:" + dialogsConfigJson.toString(2));
	JSONArray dialogsConfig = dialogsConfigJson.getJSONArray("dialogConfig");
	for (int i=0; i < dialogsConfig.length(); i++) {
		JSONObject cfg = dialogsConfig.getJSONObject(i);
		String dialogName = cfg.getString("name");
		scenarios.put(dialogName, cfg);
	}
}

int no = 1;

for (JSONObject p: scenarios.values()) {
	String name = p.getString("name");
%>
<tr>
	<td><%= name %></td>
	<td><%= p.getString("description") %></td>
	<td>
	<%-- 
	<a href='qaScenarioEditor.jsp?name=<%= name %>' class='btn btn-success'><bean:message key='modify'/></a>
	<a href='qaScenarioEditor.jsp?name=<%= name %>' class='btn btn-danger'><bean:message key='delete'/></a>
	--%>
	<a href='#' data-name='<%= name %>' class='btn btn-warning btnMigrate'>移轉</a>
	</td>
</tr>
<%
}
%>
</table>
	<h3 class="alert alert-danger">
	舊的情境對話都需要經過移轉，才能正常被使用，另外原本在問答內用代號觸發的機制將無法再使用，若原本是一般問答的可以用新的「情境式問答」的模式選擇。
	移轉僅需點選一次，點選多次將會造成 bug...
	</h3>
	<%-- 
	<a href='qaScenarioEditor.jsp' class='btn btn-success'><bean:message key='global.add'/></a>
	--%>
</div>
<script>
$('.btnMigrate').click(function() {
	$name = $(this).attr('data-name');
	$.ajax({
		url: "qaScenarioEditor-ajax.jsp",
		type: "post",
		data: { action: "migrate", name: $name },
		dataType: "json",
		success: function() {
			alert('成功!');
		}
	});
});
</script>
</body>
</html>