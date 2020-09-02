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
String[] binderIds = StringUtils.defaultString(request.getParameter("binderIds")).split(",");

JSONObject resp = new JSONObject();
Integer count = 0;
try {
	SolrQuery sq = new SolrQuery()
		.setRequestHandler("/browse")
		.addFilterQuery("dataType_s:COMMON_SENSE")
		.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]")
		.addFilterQuery("BINDER_ID_ml:(" + StringUtils.join(binderIds, " ") + ")")
		.setStart(0);
	QueryResponse qr = t.getCoreServer().query(sq);
	count = qr.getResults().size();
} catch (Exception e) {
	e.printStackTrace();
}
resp.put("count", count);
%><%= resp %>
