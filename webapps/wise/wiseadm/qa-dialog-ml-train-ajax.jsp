<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
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
import="org.apache.wink.json4j.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.robot.qadialog.*"
import="com.intumit.solr.util.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}

%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

String q = request.getParameter("originalQuestion");
String dialogName = request.getParameter("dialog");
String[] intents = request.getParameterValues("intents[]");
String[] entities = request.getParameterValues("entities[]");
String entityValuesStr = request.getParameter("entityValues");

NlpResult nlp = QAUtil.nlp(t, QAUtil.getInstance(t), q, locale, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING);
Collection<String> alts = QASaver.quickExpand(nlp + "");

System.out.println("Utterance alts:" + alts);

SolrInputDocument doc = new SolrInputDocument();
String id = WiSeUtils.sha256((dialogName != null ? dialogName + ":" : "") + q);
doc.setField("id", id);
doc.setField("dataType_s", QADialog.UTTERANCE_INDEX_DATATYPE);
doc.setField("QUESTION_s", q);
doc.setField("QUESTION_t", q);
doc.setField("QUESTION_ALT_ms", alts);

if (dialogName != null)
	doc.setField("DialogName_s", dialogName);

if (intents != null && intents.length > 0)
	doc.setField("INTENT_ms", Arrays.asList(intents));

if (entities != null && entities.length > 0)
	doc.setField("ENTITY_ms", Arrays.asList(entities));

if (StringUtils.trimToNull(entityValuesStr) != null) {
	doc.setField("ENTITY_VALUES_JSON_s", entityValuesStr);
}


SolrServer server = t.getCoreServer4Write();
server.add(doc);
server.commit();
%>{}