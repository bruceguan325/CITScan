<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.response.QueryResponse"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.dataset.*"
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.robot.qaplugin.*"
%>
<%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);

Tenant t = null;
if (xssReq.getParameter("apikey") != null) {
	String apikey = xssReq.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;
}

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");

	out.println(errorOutput.toString(2));

	return;
}
Map<String, Integer> advStat = WiSeUtils.getAdvStat(2);

JSONArray results = new JSONArray();
try {
	SolrQuery sq = new SolrQuery()
		.setRequestHandler("/browse")
		.addFilterQuery("dataType_s:COMMON_SENSE")
		.addFilterQuery("-" + QA.FN_BANNED + ":true")
		.addFilterQuery("-(-ENABLE_dt:[* TO NOW] AND ENABLE_dt:[* TO *])")
		.addFilterQuery("-(-EXPIRE_dt:[NOW TO *] AND EXPIRE_dt:[* TO *])")
		.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]")
		.addFilterQuery("-ANSWER_PLUGIN_ID_s:" + CustomQA.ID)
		//.addFilterQuery("-" + QA.FN_IS_NOT_FOR_SEARCH + ":true")
		.setStart(0)
		.setRows(Integer.MAX_VALUE);
	QueryResponse qr = t.getCoreServer().query(sq);
	SolrDocumentList docs = qr.getResults();

	for(SolrDocument doc : docs){
		JSONObject r = new JSONObject();
		r.put("id", doc.getFirstValue("id"));
		r.put("kid", doc.getFirstValue("kid_l"));
		r.put("question", doc.getFirstValue("QUESTION_s"));
		String currentCategory = doc.getFirstValue("CATEGORY_mt") + "";
		if(!currentCategory.equals("null")){
			r.put("currentCategory", currentCategory);
		} else {
			r.put("currentCategory", "");
		}
		
		try {
			int value = advStat.get(doc.getFirstValue("QUESTION_s"));
			r.put("count", value);
		} catch (Exception e) {
			r.put("count", 0);
		}
		results.put(r);
	}
} catch (Exception e) {
	e.printStackTrace();
}
%><%= results.toString(4) %>
