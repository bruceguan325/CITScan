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
import="com.intumit.solr.robot.QAUtil.FormalAnswerReplacer"
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

String binderId = request.getParameter("binderId");
JSONObject resp = new JSONObject();
JSONArray results = new JSONArray();
try {
	SolrQuery sq = new SolrQuery()
		.setRequestHandler("/browse")
		.addFilterQuery("dataType_s:COMMON_SENSE")
		.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]")
		.setQuery("BINDER_ID_ml:" + Long.parseLong(binderId))
		.setStart(0)
		.setRows(Integer.MAX_VALUE);
	QueryResponse qr = t.getCoreServer().query(sq);
	SolrDocumentList docs = qr.getResults();
	for(SolrDocument doc : docs){
		JSONObject r = new JSONObject();
		Long kid = (Long)doc.getFieldValue("kid_l");
		Collection<Object> binderIds = doc.getFieldValues("BINDER_ID_ml");
		JSONArray binderIdArray = new JSONArray();
		for(Object binder : binderIds) {
			if(binder != null) {
				binderIdArray.add((Long)binder);
			}
		}
		resp.put(kid.toString(), binderIdArray);
	}
} catch (Exception e) {
	e.printStackTrace();
}
%><%= resp.toString(4) %>
