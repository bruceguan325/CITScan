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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qaplugin.*"
import="org.json.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null) { 
	%>{"needLogin":true}<%
	return;
}

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
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

response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");
String id = StringUtils.trimToNull(request.getParameter("id"));
Long kid = qautil.id2Kid(id);
SolrDocument doc = qautil.getMainQASolrDocument(kid, true);

JSONObject resp = new JSONObject();

if (doc != null) {
	QA qa = new QA(doc);
	String pluginId = (String)qa.getFieldValue("ANSWER_PLUGIN_ID_s");
	
	if (pluginId != null) {
		QAPlugin p = QAPlugins.get(pluginId);
		p.onEditorPageSave(qa, (HttpServletRequest)request, true);
		
		Date now = new Date();
		qa.setUpdateInfo(now, user);
		SolrServer server = t.getCoreServer4Write();
		server.add(qa);
		server.commit(true, true, false);
		
		try {
		// wait for softCommit
			Thread.sleep(1000);
		} catch (InterruptedException ignore) {
		} 
		
		QAAltBuildQueue.add(t.getId(), (String)qa.getFieldValue("id"), kid, qa.getQuestionAltTemplates(), user.getLoginName());
		
		String dataStr = (String)getRealFieldValue(qa, HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME);
		resp.put("result", HierarchicalQA.getDataFromString(dataStr));
	}
}

%><%= resp.toString(2) %>