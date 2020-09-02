<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.ServletConfig"
	 import="javax.servlet.ServletException"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="com.mchange.io.FileUtils"
	 import="org.apache.commons.io.*"
	 import="org.apache.commons.httpclient.*"
	 import="org.apache.commons.httpclient.methods.*"
	 import="org.apache.commons.httpclient.params.HttpMethodParams"
	 import="org.apache.commons.lang.*"
	 import="org.apache.solr.core.*"
	 import="org.apache.solr.servlet.*"
	 import="org.apache.solr.client.solrj.*"
	 import="org.apache.solr.client.solrj.embedded.*"
	 import="org.apache.solr.client.solrj.response.*"
	 import="org.apache.solr.common.*"
	 import="org.apache.wink.json4j.*"
	 import="com.intumit.solr.SearchManager"
	 import="com.intumit.solr.tenant.*"
	 import="com.intumit.solr.util.*"
	 import="com.intumit.solr.robot.*"
	 import="com.intumit.solr.robot.qarule.*"
	 import="com.intumit.systemconfig.*"
	 import="org.apache.commons.lang.StringUtils"
	 import="com.intumit.quartz.ScheduleUtils"
	 import="com.intumit.quartz.Job"
     import="org.dom4j.*"
	 import="org.apache.commons.httpclient.methods.GetMethod"
	 import="org.apache.commons.httpclient.HttpClient"
	 import="org.apache.commons.httpclient.auth.AuthScope"
	 import="org.apache.commons.httpclient.UsernamePasswordCredentials"
	 import="org.apache.solr.client.solrj.SolrQuery"
	import="com.intumit.solr.admin.*"
%><%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%><%
	WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String action = StringUtils.defaultString(request.getParameter("action"), "save");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

if ("saveNew".equalsIgnoreCase(action)) {
	String jsonData = request.getParameter("data");
	JSONObject dCfg = new JSONObject(jsonData);
	String name = dCfg.getString("name");
	
	String idStr = request.getParameter("id");
	QADialogConfig dlg = QADialogConfig.get(Integer.parseInt(idStr));
	if (dlg.getTenantId() != t.getId()) return;
	
	dlg.setDraftDialogConfig(jsonData);
	QADialogConfig.saveOrUpdate(dlg);
	
	out.println("{\"status\": \"Done\"}");
}
else if ("save".equalsIgnoreCase(action)) {
	String jsonData = request.getParameter("data");
	JSONObject dlgConfig = new JSONObject(jsonData);
	
	if (dlgConfig == null)
		return;
	
	// migrate 幾個欄位
	if (!dlgConfig.has("mkey") && dlgConfig.has("name")) {
		dlgConfig.put("mkey", StringUtils.left(dlgConfig.getString("name"), 32));
	}
	if (dlgConfig.has("name")) {
		dlgConfig.put("dialogName", dlgConfig.get("name"));
	}
	if (dlgConfig.has("description")) {
		dlgConfig.put("dialogDesc", dlgConfig.get("description"));
	}

	QADialogConfig dlg = new QADialogConfig();
	dlg.setTenantId(t.getId());
	dlg.setOldMode(true);
	dlg.setDraftDialogConfig(dlgConfig.toString());
	
	QADialogConfig.saveOrUpdate(dlg);
	
	out.println("{\"status\": \"Done\"}");
}
else if ("migrate".equalsIgnoreCase(action)) {
	String dlgName = request.getParameter("name");
	
	JSONObject dlgConfig = null;
	
	File theDialogJsonFile = new File(new File(WiSeEnv.getHomePath()), "dialogs/" + t.getId() + ".json");
	String dialogsConfigStr = FileUtils.getContentsAsString(theDialogJsonFile, "UTF-8");
	if (StringUtils.isNotEmpty(dialogsConfigStr)) {
		JSONObject dialogsConfigJson = new JSONObject(dialogsConfigStr);
		System.out.println("Got Dialogs for tenant[" + t.getId() + "]:" + dialogsConfigJson.toString(2));
		JSONArray dialogsConfig = dialogsConfigJson.getJSONArray("dialogConfig");
		for (int i=0; i < dialogsConfig.length(); i++) {
	JSONObject cfgTmp = dialogsConfig.getJSONObject(i);
	String dialogName = cfgTmp.getString("name");
	
	if (dialogName.equals(dlgName)) {
		dlgConfig = cfgTmp;
		break;
	}
		}
	}
	
	if (dlgConfig == null)
		return;
	
	// migrate 幾個欄位
	if (!dlgConfig.has("mkey") && dlgConfig.has("name")) {
		dlgConfig.put("mkey", StringUtils.left(dlgConfig.getString("name"), 32));
	}
	if (dlgConfig.has("name")) {
		dlgConfig.put("dialogName", dlgConfig.get("name"));
	}
	if (dlgConfig.has("description")) {
		dlgConfig.put("dialogDesc", dlgConfig.get("description"));
	}

	QADialogConfig dlg = new QADialogConfig();
	dlg.setTenantId(t.getId());
	dlg.setOldMode(true);
	dlg.setDraftDialogConfig(dlgConfig.toString());
	
	QADialogConfig.saveOrUpdate(dlg);
}
%>