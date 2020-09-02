<%@page import="com.intumit.solr.SearchManager"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.tenant.*"
%><%!
%><%
Tenant t = null;
if (request.getParameter("apikey") != null) {
	String apikey = request.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");
	out.println(errorOutput.toString(2));
	return;
}

String kids = request.getParameter("kids");
JSONObject resp = new JSONObject();
if (StringUtils.isNotBlank(kids)) {
	String[] kidArray = kids.split(",");
	for(String kid : kidArray){
		SolrDocument oriDoc = QAUtil.getInstance(t).getMainQASolrDocument(Long.valueOf(kid));
		QA qa = new QA(oriDoc);
		qa.setIsKmsRelateExpiredMemo(false);
		SolrServer server = t.getCoreServer4Write();
		server.add(qa);
		server.commit();
		QAAltBuildQueue.add(t.getId(), qa.getId(), Long.valueOf(kid), qa.getQuestionAltTemplates(), "admin");
	}
}
%><%= resp.toString(4) %>
