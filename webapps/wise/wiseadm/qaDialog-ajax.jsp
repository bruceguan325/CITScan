<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="org.apache.wink.json4j.JSONObject"
import="com.hazelcast.core.ITopic"
import="com.intumit.message.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.NotificationEvent"
import="com.intumit.solr.NotificationEvent.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.qadialog.DialogLogEntry"
import="com.intumit.solr.servlet.*"
import="com.intumit.solr.tenant.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null) { 
	%>{"needLogin":true}<%
	return;
}

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) { 
	%>{"permissionDenied":true}<%
	return;
}
%><%! 

Object getRealFieldValue(SolrInputDocument doc, String name) {
	Object val = doc.containsKey(name) ? doc.get(name).getValue() : null;

	if (val instanceof Map) {
		Map m = (Map)val;

		if (m.containsKey("set")) {
			return m.get("set");
		}
		else if (m.containsKey("add")) {
			return m.get("add");
		}
		else {
			throw new RuntimeException("Unknown SolrInputDocument Field:" + m);
		}
	}
	else {
		return val;
	}
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
QAUtil qautil = QAUtil.getInstance(t);

response.addHeader("Pragma","no-cache");
response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");
String id = StringUtils.trimToNull(request.getParameter("id"));
String action = StringUtils.defaultString(request.getParameter("action"), "saveDraft");
QADialogConfig dlg = null;
JSONObject resp = new JSONObject();
Date now = new Date();

if ("saveAndPublish".equals(action)) {
	boolean createMode = Boolean.parseBoolean(request.getParameter("createMode"));
	
	if (createMode) {
		dlg = new QADialogConfig();
		dlg.setTenantId(t.getId());
	}
	else {
		dlg = QADialogConfig.get(Integer.parseInt(id));
		
		if (dlg == null && (dlg != null && dlg.getTenantId() != t.getId())) 
			return;
	}

	if (dlg != null) {
		dlg.setDialogName(request.getParameter("dialogName"));
		dlg.setDialogDesc(request.getParameter("dialogDesc"));
		dlg.setMkey(request.getParameter("mkey"));
		dlg.setDialogTimestamp(now);
		
		JSONObject cfg = QADialogPlugin.buildConfigFromRequest(request, createMode);
		
		cfg.put("lastUpdateByUser", user.getId());
		dlg.setDraftDialogConfig(cfg.toString());
		QADialogConfig.saveOrUpdate(dlg);   // 為了確保取得正確的 Mkey（p.s. 儲存時可能因為 dup 改 mkey）
		
		QADialogConfigVersion newVer = new QADialogConfigVersion();
		newVer.setMkey(dlg.getMkey());
		newVer.setDialogConfig(cfg.toString());
		newVer.setDialogTimestamp(now);
		newVer.setTenantId(t.getId());
		
		int newVerNum = QADialogConfigVersion.saveAndCommitNewVersion(newVer, request.getParameter("versionComment"), user);

		dlg.setDialogTimestamp(now);
		dlg.clearDraftConfig();//cfg.toString());
		dlg.setPublishedVersionNumber(newVerNum);
		QADialogConfig.saveOrUpdate(dlg);

		resp.put("status", "Done");
		resp.put("result", cfg);
		resp.put("id", dlg.getId());
		
		NotificationServiceServlet.doNotification(t, NotificationType.SUCCESS, "<bean:message key='scenario.operation.completed'/>", "<bean:message key='scenario.released.version'/>");
	}
}
else if ("saveDraft".equals(action)) {
	boolean createMode = Boolean.parseBoolean(request.getParameter("createMode"));
	
	if (createMode) {
		dlg = new QADialogConfig();
		dlg.setTenantId(t.getId());
	}
	else {
		dlg = QADialogConfig.get(Integer.parseInt(id));
		
		if (dlg == null && (dlg != null && dlg.getTenantId() != t.getId())) 
			return;
	}
	
	if (dlg != null) {
		dlg.setDialogName(request.getParameter("dialogName"));
		dlg.setDialogDesc(request.getParameter("dialogDesc"));
		dlg.setDialogTimestamp(now);
		dlg.setMkey(request.getParameter("mkey"));
		
		JSONObject cfg = QADialogPlugin.buildConfigFromRequest(request, createMode);
		cfg.put("lastUpdateByUser", user.getId());

		dlg.setDraftDialogConfig(cfg.toString());
		QADialogConfig.saveOrUpdate(dlg);

		resp.put("status", "Done");
		resp.put("result", cfg);
		resp.put("id", dlg.getId());

		NotificationServiceServlet.doNotification(t, NotificationType.SUCCESS, "<bean:message key='scenario.operation.completed'/>", "<bean:message key='scenario.temporarily.stored'/>");
	}
}
else if ("delete".equals(action)) {
	dlg = QADialogConfig.get(Integer.parseInt(id));
	
	if (dlg == null && (dlg != null && dlg.getTenantId() != t.getId())) 
		return;
	
	if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0
			|| ("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
		
		QADialogConfig.delete(t.getId(), dlg.getId());
		resp.put("status", "Done");
		
		NotificationServiceServlet.doNotification(t, NotificationType.SUCCESS, "<bean:message key='scenario.operation.completed'/>", "<bean:message key='scenario.temporarily.stored'/>");
	}
	else {
		resp.put("status", "Permission Denied");
		
		NotificationServiceServlet.doNotification(t, NotificationType.ERROR, "<bean:message key='scenario.operation.completed'/>", "<bean:message key='scenario.deleted'/>");
	}
}
else if ("checkDupMkey".equals(action)) {
	try {
		String mkey = request.getParameter("mkey");
		QADialogConfig dup = QADialogConfig.getByKey(t.getId(), mkey);
		
		if (dup != null) {
			resp.put("status", "DuplicateMkey");
			resp.put("errorMsg", "Duplicated Mkey.");
			NotificationServiceServlet.doNotification(t, NotificationType.ERROR, "操作失敗", "重複的 MKEY");
		}
		else {
			resp.put("status", "Done");
		}
	}
	catch (Exception ex) {
		ex.printStackTrace();
		resp.put("status", "Error");
		resp.put("errorMsg", ex.getMessage());
		NotificationServiceServlet.doNotification(t, NotificationType.ERROR, "操作失敗", ex.getMessage());
	}
}
else if ("peekLog".equals(action)) {
	String qaId = request.getParameter("qaId");
	QAContext ctx = QAContextManager.lookup(qaId);
	
	if (ctx == null && (ctx != null && ctx.getTenant().getId() != t.getId())) 
		return;
	
	DialogLogEntry log = (DialogLogEntry)ctx.getRequestAttribute("DialogLog");
	resp.put("dialogLog", log != null ? StringUtils.trimToEmpty(log.getLogDetail()) : "No log in dialog");
	resp.put("status", "Done");
}
else if ("get".equals(action)) {
	dlg = QADialogConfig.get(Integer.parseInt(id));
	String targetVer = StringUtils.defaultString(request.getParameter("target"), "head");
	
	if (dlg == null) {
		resp.put("status", "NotFound");
	}
	else {
		if (dlg.getTenantId() != t.getId())
			return;

		JSONObject data = null;
		if (StringUtils.equalsIgnoreCase("head", targetVer)) {
			data = dlg.getDialogConfigObject();
		}
		else if (StringUtils.equalsIgnoreCase("draft", targetVer)) {
			data = dlg.getDraftDialogConfigObject();
		}
		else {
			Integer iTargetVer = Integer.parseInt(targetVer);
			QADialogConfigVersion tv = QADialogConfigVersion.getByKeyAndVersionNumber(t.getId(), dlg.getMkey(), iTargetVer);
			if (tv != null) {
				data = tv.getDialogConfigObject();
			}
		}
		
		resp.put("status", "Done");
		resp.put("dlgConfig", data);
	}
}%><%= resp.toString(2) %>