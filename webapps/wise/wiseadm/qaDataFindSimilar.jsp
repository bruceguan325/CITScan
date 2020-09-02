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
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
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

String q = request.getParameter("q");
Boolean casual = Boolean.valueOf(request.getParameter("casual"));
SolrDocumentList docs = null;
if(casual != null && casual)
	docs = qu.mltCasual(q, qaCtx);
else
	docs = qu.mltCommonSense(q, qaCtx, 10);

if (q != null && StringUtils.isNumeric(q)) {
	SolrDocument getByKid = qu.getMainQASolrDocument(new Integer(q));	
	
	if (getByKid != null) {
		if (docs == null) {
			docs = new SolrDocumentList();
		}
		getByKid.setField("score", 500f);
		docs.add(0, getByKid);
	}
}

if (docs != null) {
	for (SolrDocument doc: docs) {
		float score = (Float) doc.getFieldValue("score");
		if(score > QAUtil.MLT_COMMON_SENSE_Q_MIN_SCORE){
			JSONObject obj = new JSONObject();
			obj.put("id", doc.getFieldValue("id"));
			obj.put("kid", doc.getFieldValue("kid_l"));
			obj.put("question", doc.getFieldValue("QUESTION_s"));
			jsonRs.put(obj);
		}
	}
}
%>
<%= jsonRs %>