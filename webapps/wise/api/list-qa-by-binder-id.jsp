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

String qaId = request.getParameter("id");
if (StringUtils.isBlank(qaId)) {
	qaId = java.util.UUID.randomUUID().toString();
}
QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}
qaCtx.setClientSupportHtml(true);
qaCtx.setTenant(t);

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
		r.put("id", doc.getFirstValue("id"));
		r.put("question", doc.getFirstValue("QUESTION_s"));
		String answer = (String)doc.getFirstValue("ANSWER_s");
		FormalAnswerReplacer far = QAUtil.getInstance(t).getFormalAnswerReplacer(qaCtx);
		answer = TemplateUtil.process(answer, far);
		answer = TemplateUtil.processByTagName(answer, "ROBOT_NAME", far);
		r.put("answer", answer);
		results.put(r);
	}
} catch (Exception e) {
	e.printStackTrace();
}
resp.put("results", results);
%><%= resp.toString(4) %>
