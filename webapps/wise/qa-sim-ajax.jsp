<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="com.intumit.solr.robot.*"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="com.intumit.solr.robot.dictionary.*"%>
<%@page import="com.intumit.systemconfig.WiseSystemConfig"%>
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
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.tenant.*"
%><%
	JSONArray jsonRs = new JSONArray();
Tenant t = null;
String q = request.getParameter("q");
String apikey = request.getParameter("apikey");

// 如果 enable 前台帳號，則呼叫任何 qa 都需要有 apikey 對應的 Tenant 才行
if (apikey != null) {
	Apikey key = Apikey.getByApiKey(apikey);

	if (key != null && key.isValid()) {
		t = key.getTenant();

		if (t != null) {
			key.incCallCounter();
			Apikey.saveOrUpdate(key);
		}
	}
}

if (t == null) {
	return ;
}
QAContext qaCtx = new QAContext();
qaCtx.setTenant(t);
DictionaryDatabase[] currentKPs = new DictionaryDatabase[0];
currentKPs = KnowledgePointDictionary.search(t.getId(), q.toCharArray(), null);
if(currentKPs.length > 0){
	qaCtx.setRestrictToQaCategory(currentKPs[0].getCategory());
}
SolrDocumentList docs = QAUtil.getInstance(t).mltCommonSense(q, qaCtx, 10);
if(docs != null){
	for (SolrDocument doc: docs) {
		float score = (Float) doc.getFieldValue("score");
		if(score > QAUtil.MLT_COMMON_SENSE_Q_MIN_SCORE){
	JSONObject obj = new JSONObject();
	obj.put("id", doc.getFieldValue("id"));
	obj.put("question", doc.getFieldValue("QUESTION_s"));
	jsonRs.put(obj);
		}
	}
}

%>
<%= jsonRs %>