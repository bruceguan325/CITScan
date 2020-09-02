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
String qaCategory = request.getParameter("qaCategory");
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
	if(StringUtils.isNotBlank(qaCategory)) {
		if(QAUtil.GENERAL_CATEGORIES.contains(qaCategory)) {
	query.append("QA_CATEGORY_s:(" + StringUtils.join(QAUtil.GENERAL_CATEGORIES, " ") + ")");
		}
		else {
	query.append("QA_CATEGORY_s:(").append(qaCategory).append(")");
		}
		query.append(" QA_CATEGORY_s:(*:* -[\"\" TO *])");
		sq.addFilterQuery(query.toString());
	}
	if(StringUtils.isNotBlank(kid)) {
		sq.addFilterQuery("kid_l:" + kid);
	}
	System.out.println(sq);
	QueryResponse qr = t.getCoreServer().query(sq);
	SolrDocumentList docs = qr.getResults();
	for(SolrDocument doc : docs){
		JSONObject r = new JSONObject();
		r.put("id", doc.getFirstValue("id"));
		r.put("kid", doc.getFirstValue("kid_l"));
		r.put("question", doc.getFirstValue("QUESTION_s"));
		r.put("answer", doc.getFirstValue("ANSWER_s"));
		results.put(r);
	}
} catch (Exception e) {
	e.printStackTrace();
}
resp.put("results", results);
%><%= resp.toString(4) %>
