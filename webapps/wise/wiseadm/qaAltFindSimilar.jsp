<%@page import="com.intumit.solr.robot.QAContextManager"%>
<%@page import="com.intumit.solr.robot.QAContext"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.dataset.*"
import="com.intumit.solr.qparser.*"
import="com.intumit.solr.config.ColumnNameMappingFacade"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) { 
	return;
}
%><%!
%><%
JSONArray jsonRs = new JSONArray();
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String qaId = session.getId();
if(StringUtils.isBlank(qaId)){
	qaId = "" + System.currentTimeMillis();
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}
qaCtx.setTenant(t);
QAUtil qu = QAUtil.getInstance(t);

Map<String, Long> coreCount = new TreeMap<String, Long>();
QueryResponse mainRsp = null;
SolrDocumentList docs = null;
SolrQuery query = null;
int totalCoreCount = 0;
SolrServer mServer = SearchManager.getServer("core-log");
SolrQuery multiCoreQ = new SolrQuery();//com.intumit.solr.util.WiSeUtils.parseUrlSearchParameters(ds, true, new HashMap<String, String[]>(), null);
multiCoreQ.addFacetField("Question_ms");
multiCoreQ.setFacet(true);
multiCoreQ.setRequestHandler("browse");
multiCoreQ.set("qf", "Name_t^1");
multiCoreQ.addFilterQuery("+SpecialMark_s:QAMessage")// +TenantId_i:" + t.getId())
		.setParam("mm", "0")
		.setParam("enableElevation", true)
		.setParam("forceElevation", true)
		.setParam("fuzzy", true)
		.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + t.getId())
		.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
		.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true);

multiCoreQ.setQuery(request.getParameter("q"));
mainRsp = mServer.query(multiCoreQ);

docs = mainRsp.getResults();
if(docs != null){
	for (SolrDocument doc: docs) {
		JSONObject obj = new JSONObject();
		obj.put("id", doc.getFieldValue("id"));
		obj.put("question", doc.getFieldValue("Name_t"));
		obj.put("score", doc.getFieldValue("score"));
		jsonRs.put(obj);
	}
}
%>
<%= jsonRs %>