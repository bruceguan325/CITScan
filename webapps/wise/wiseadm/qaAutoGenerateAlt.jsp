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
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) { 
	return;
}
%><%!
String getQuestionWithoutKP(QAContext qaCtx, String question) {
	DictionaryDatabase[] kps = KnowledgePointDictionary.search(qaCtx.getTenant().getId(), question.toCharArray(), qaCtx.getRestrictToQaCategory());
	String questionWithoutKP = question;
	for (DictionaryDatabase kp: kps) {
		questionWithoutKP = questionWithoutKP.replaceAll(kp.getKeyword(), "");
	}
	
	return questionWithoutKP;
}

void discoverAltTpls(QAContext qaCtx, SolrDocument simDoc, String simQ, String origQ, List<String> altTpls) {
	QA qa = new QA(simDoc);

	DictionaryDatabase[] simKPs = KnowledgePointDictionary.search(qaCtx.getTenant().getId(), simQ.toCharArray(), qaCtx.getRestrictToQaCategory());
	DictionaryDatabase[] origKPs = KnowledgePointDictionary.search(qaCtx.getTenant().getId(), origQ.toCharArray(), qaCtx.getRestrictToQaCategory());
	List<String> simTpls = qa.getQuestionAltTemplates();
	
	if (simKPs.length == origKPs.length) {
		for (String simTpl: simTpls) {
			Map<String, String> altData = QA.parseQAAlt(simTpl);
			simTpl = StringUtils.trimToEmpty(altData.get("alt"));
			String newTpl = simTpl;
			
			DictionaryDatabase[] simTplKPs = KnowledgePointDictionary.search(qaCtx.getTenant().getId(), simTpl.toCharArray(), qaCtx.getRestrictToQaCategory());
			if (simTplKPs.length > simKPs.length) {
				System.out.println("此問句範本有多餘的知識點，可能無法適用：" + simTpl);
				continue;
			}
			
			for (int i=0; i < simKPs.length; i++) {
				DictionaryDatabase simKP = simKPs[i];
				DictionaryDatabase origKP = origKPs[i];
				
				newTpl = newTpl.replaceAll(simKP.getKeyword(), origKP.getKeyword());
			}
			
			if (!StringUtils.equalsIgnoreCase(newTpl, simTpl)) {
				System.out.println("Got replaced tpl: " + newTpl + " (FROM: " + simTpl + ")");
				
				if (!altTpls.contains(newTpl)) {
					altTpls.add(newTpl);
				}
			}
		}
	}
}
%><%
JSONObject result = new JSONObject();
JSONArray jsonRs = new JSONArray();
result.put("sims", jsonRs);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String qaId = session.getId();
if (StringUtils.isBlank(qaId)){
	qaId = "" + System.currentTimeMillis();
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}
qaCtx.setTenant(t);
QAUtil qu = QAUtil.getInstance(t);

String question = request.getParameter("q");
String questionWithoutKP = getQuestionWithoutKP(qaCtx, question);
System.out.println("questionWithoutKP:" + questionWithoutKP);

List<String> altTpls = new ArrayList<String>();

if (!StringUtils.equalsIgnoreCase(question, questionWithoutKP)) {
	SolrDocumentList docs = qu.searchCommonSense(questionWithoutKP, qaCtx, true, false, false, 0, 3);
	if(docs != null){
		for (SolrDocument doc: docs) {
			float score = (Float) doc.getFieldValue("score");
			
			if(score > t.getThreshold3()){
				String simQ = (String)doc.getFieldValue("QUESTION_s");
				String simQwithoutKP = getQuestionWithoutKP(qaCtx, simQ);
				
				if (StringUtils.equalsIgnoreCase(questionWithoutKP, simQwithoutKP)) {
					discoverAltTpls(qaCtx, doc, simQ, question, altTpls);
					
					JSONObject obj = new JSONObject();
					obj.put("id", doc.getFieldValue("id"));
					obj.put("question", doc.getFieldValue("QUESTION_s"));
					jsonRs.put(obj);
				}
			}
		}
	}
}
else {
	SolrDocumentList docs = qu.mltCommonSense(question, qaCtx, 3);
	if(docs != null){
		for (SolrDocument doc: docs) {
			float score = (Float) doc.getFieldValue("score");
			if(score > t.getThreshold3()){
				JSONObject obj = new JSONObject();
				obj.put("id", doc.getFieldValue("id"));
				obj.put("question", doc.getFieldValue("QUESTION_s"));
				jsonRs.put(obj);
			}
		}
	}
}

result.put("newAltTpls", new JSONArray(altTpls));
%>
<%= result %>