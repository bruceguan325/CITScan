<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.ServletConfig"
	 import="javax.servlet.ServletException"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="org.json.*"
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
	 import="com.intumit.solr.SearchManager"
	 import="com.intumit.solr.tenant.*"
	 import="com.intumit.solr.util.*"
	 import="com.intumit.solr.robot.QAChannel"
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
String action = StringUtils.defaultString(request.getParameter("action"), "edit");
String target = StringUtils.defaultString(request.getParameter("target"), "tenant");
String idStr = request.getParameter("id");

if ("loadConfig".equalsIgnoreCase(action)) {
	JSONObject srbtJson = QAMatchRuleController.getQAMatchControllerFlowConfig();
	
	if (srbtJson == null) {
		out.println("{'error': 'Config Not Found'}");
		return;
	}
	
	String flowJsonStr = null;
	
	if ("tenant".equals(target)) {
		Tenant tenant = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
		flowJsonStr = tenant.getQaMatchCtrlFlow();
	}
	else if ("channel".equals(target)) {
		QAChannel ch = QAChannel.get(Integer.parseInt(idStr));
		flowJsonStr = ch.getQaMatchCtrlFlow();
	}
	
	JSONObject tQaCtrlFlowJson = null;
	try {
		tQaCtrlFlowJson = new JSONObject(flowJsonStr);
	}
	catch (Exception ignore) {
		tQaCtrlFlowJson = QAMatchRuleController.getDefaultQAMatchControllerFlow();
		
		if (tQaCtrlFlowJson == null) {
			tQaCtrlFlowJson = new JSONObject();
		}
	}
	
	JSONObject resp = new JSONObject();
	
	resp.put("srbtConfig", srbtJson);
	resp.put("tenantQaMatchFlowConfig", tQaCtrlFlowJson);
	out.println(resp.toString(2));
}
else if ("save".equalsIgnoreCase(action)) {
	String flowJsonStr = request.getParameter("data");
	
	if ("tenant".equals(target)) {
		Tenant tenant = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
		tenant.setQaMatchCtrlFlow(flowJsonStr);
		Tenant.saveOrUpdate(tenant);
		QAMatchRuleController.clear("tenant:" + tenant.getId());
		List<QAChannel> channels = QAChannel.list(tenant.getId());
		for (QAChannel channel : channels) {
			QAMatchRuleController.clear("channel:" + channel.getId());
		}
	}
	else if ("channel".equals(target)) {
		QAChannel ch = QAChannel.get(Integer.parseInt(idStr));
		ch.setQaMatchCtrlFlow(flowJsonStr);
		QAChannel.saveOrUpdate(ch);
		QAMatchRuleController.clear("channel:" + ch.getId());
	}
	
	out.println("{\"status\": \"Done\"}");
}
%>