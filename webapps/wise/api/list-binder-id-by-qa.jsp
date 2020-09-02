<%@page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.tenant.Tenant"
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
String kid = request.getParameter("kid");

JSONObject resp = new JSONObject();
JSONArray results = new JSONArray();
try {
	SolrQuery sq = new SolrQuery()
		.setRequestHandler("/browse")
		.addFilterQuery("dataType_s:COMMON_SENSE")
		.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]")
		.setStart(0)
		.setRows(Integer.MAX_VALUE);
	StringBuffer query = new StringBuffer();
	if(StringUtils.isNotBlank(kid)) {
		sq.addFilterQuery("kid_l:" + kid);
	}
	QueryResponse qr = t.getCoreServer().query(sq);
	SolrDocumentList docs = qr.getResults();
	if(docs.size() > 0) {
		SolrDocument doc = docs.get(0);
		Collection<Object> binderIds = doc.getFieldValues("BINDER_ID_ml");
		for(Object binderId : binderIds) {
	results.add(binderId);
		}
	}
} catch (Exception e) {
	e.printStackTrace();
}
resp.put("results", results);
%><%= resp.toString(4) %>
