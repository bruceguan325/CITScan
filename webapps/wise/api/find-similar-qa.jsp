<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
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
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.tenant.*"
%>
<%!
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

JSONObject resp = new JSONObject();
JSONArray jsonRs = new JSONArray();
com.intumit.solr.robot.QAContext fake = new com.intumit.solr.robot.QAContext();
fake.setTenant(t);
SolrDocumentList docs = QAUtil.getInstance(t).mltCommonSense(request.getParameter("q"), fake, 10);
if(docs != null){
	for (SolrDocument doc: docs) {
		float score = (Float) doc.getFieldValue("score");
		if(score > QAUtil.MLT_COMMON_SENSE_Q_MIN_SCORE){
	JSONObject obj = new JSONObject();
	obj.put("id", doc.getFieldValue("id"));
	obj.put("question", doc.getFieldValue("QUESTION_s"));
	obj.put("answer", doc.getFieldValue("ANSWER_s"));
	jsonRs.put(obj);
		}
	}
}
resp.put("results", jsonRs);
%>
<%= resp %>