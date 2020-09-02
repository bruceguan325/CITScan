<%@page import="org.apache.commons.collections.CollectionUtils"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugin"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="com.intumit.solr.robot.*"%>
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

import="com.intumit.solr.util.XssHttpServletRequestWrapper"
import="com.intumit.syslog.OperationLogEntity"

%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O8) == 0) {
	return;
}
%><%!
static List<String> getMultiValueParameters(HttpServletRequest req, String name){
	String param = req.getParameter(name);
	return QAUtil.parseMultiValue(param);
}
%><%

XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);

Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
if (log == null) {
	out.println("OperationLogEntity Missing");
	return;
}

AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

String id = StringUtils.trimToNull(request.getParameter("id"));
Long kid = null;
QA qa = null;
SolrServer server = QAUtil.getCasualServer(t, true);

if (id != null) {
	kid = QAUtil.id2Kid(id);
	SolrDocument origDoc = QAUtil.getCasualQASolrDocument(t, kid);

	if (origDoc == null)
		return;

	qa = new QA(origDoc);
}
else {
	qa = new QA();
	kid = System.currentTimeMillis();
	id = QAUtil.DATATYPE_CASUAL + "-" + kid;
}

qa.setId(id);
qa.setKid(kid);
qa.setDataType(QAUtil.DATATYPE_COMMON_SENSE);
qa.setQuestion(request.getParameter("question"));
qa.setAnswer(request.getParameter("answer"));
qa.setField("ANSWER_ALT_ms", Arrays.asList(StringUtils.split(request.getParameter("answerAlts"), "\n\r")));

String notBanned = request.getParameter("notBanned");
if (notBanned == null) {
	qa.setIsBanned(true);
}
else {
	qa.setIsBanned(false);
}
String forMlt = request.getParameter("forMlt");
if (forMlt == null) {
	qa.setIsNotForMLT(true);
}
else {
	qa.setIsNotForMLT(false);
}
String forSearch = request.getParameter("forSearch");
if (forSearch == null) {
	qa.setIsNotForSearch(true);
}
else {
	qa.setIsNotForSearch(false);
}
String appendMlt = request.getParameter("appendMlt");
if (appendMlt == null) {
	qa.setNotAppendMLT(true);
}
else {
	qa.setNotAppendMLT(false);
}

String choosedQAPlugIn = StringUtils.trimToNull(request.getParameter("choosedQAPlugIn"));
qa.setAndSavePlugin(choosedQAPlugIn, request);

String expireRadio = request.getParameter("expireRadio");
qa.setAndSaveExpire(expireRadio, request);

qa.setTags(getMultiValueParameters(request, "category"));

Date now = new Date();
qa.setCreateInfo(now, user);
qa.setUpdateInfo(now, user);

qa.setQuestionAltTemplates(getMultiValueParameters(request, "question_alt"));

Set<Long> binderIds = null;
List<String> binderIdStrs = getMultiValueParameters(request, "binderId");
if(CollectionUtils.isNotEmpty(binderIdStrs)){
	binderIds = new HashSet<Long>();
	for(String _id : binderIdStrs){
		binderIds.add(Long.valueOf(_id));
	}
}
qa.setBinderIds(binderIds);

CasualQASaver qaSaver = new CasualQASaver(qa, t);
qaSaver.save();
Thread.sleep(1000); // wait for softCommit
log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
log.update();
response.sendRedirect("qaDataEditor2.jsp?id=" + id + "&hideNavBar=" + Boolean.parseBoolean(request.getParameter("hideNavBar")));
%>