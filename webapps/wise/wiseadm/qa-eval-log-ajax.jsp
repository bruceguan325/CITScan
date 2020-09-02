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
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}
%><%
String data = request.getParameter("data");
Integer logId = new Integer(StringUtils.defaultString(request.getParameter("logId"), "-1"));

if (logId != null && logId != -1) {
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) {
		EvaluationLogEntity log = EvaluationLogEntity.get(logId);
		EvaluationLogEntityUpdateLog.checkAndCreateFirstLog(log);
		
		JSONObject jobj = new JSONObject(data);
		int flag = jobj.getInt("flag");
		log.setFlag(flag);
		if (jobj.has("correctAnswer")) {
			log.setCorrectAnswerId(new Long(jobj.getString("correctAnswer")));
		}
		if (jobj.has("robotAnswer")) { 
			log.setRobotAnswerId(new Long(jobj.getString("robotAnswer")));
		}
		if (jobj.has("kid")) {
			log.setRobotAnswerId(new Long(jobj.getString("kid")));
		}
		// 以後不再異動原始記錄的 user Id，但會另外記 log 
		// log.setAdmId(user.getId());

		EvaluationLogEntity.update(log);
		EvaluationLogEntityUpdateLog.addUpdateLog(log, user);
		
		%>{"logId":<%= log.getId() %>, "status": "good"}<%
	}
	else {
		%>{"needLogin":true}<%
		return;
	}
}
else {
	EvaluationLogEntity log = EvaluationLogEntity.log(request, QAUtil.CURRENT_EVALUATION_LOG_NS, data, null);
	%>{"logId":<%= log.getId() %>}
<%
}
%>
