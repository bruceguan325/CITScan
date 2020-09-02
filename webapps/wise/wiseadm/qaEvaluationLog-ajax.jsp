<%@ page pageEncoding="UTF-8" language="java"
import="flexjson.*"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.ArrayList"
import="java.util.Calendar"
import="java.util.Date"
import="java.util.List"
import="java.util.Map"
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
import="org.supercsv.io.*"
import="org.supercsv.cellprocessor.*"
import="org.supercsv.cellprocessor.ift.*"
import="org.supercsv.cellprocessor.constraint.*"
import="org.supercsv.exception.*"
import="org.supercsv.prefs.*"
import="org.supercsv.util.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) != 0 ||
	(AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O6) != 0) {
} else {
	return;
}
%><%!
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

String id = request.getParameter("id");
String format = StringUtils.defaultString(request.getParameter("format"), "json");
Integer flag = request.getParameter("flag") != null ? new Integer(request.getParameter("flag")) : null;
Integer lastTestResult = StringUtils.trimToNull(request.getParameter("ltr")) != null ? new Integer(request.getParameter("ltr")) : null;
boolean distinctQuestion = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("dq"), "false"));
boolean exportDetails = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("ed"), "false")) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() > 0 || (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O6) != 0);
String qaCategory = StringUtils.trimToNull(request.getParameter("qaCategory"));

if (flag != null && flag == 0 && distinctQuestion) {
	distinctQuestion = false;
}

Date afterDate = null;
Date beforeDate = null;
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

if (request.getParameter("timeRange") != null) {
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(request.getParameter("timeRange"));
	afterDate = timeRangePair[0];
	beforeDate = timeRangePair[1];
} else if (StringUtils.trimToNull(request.getParameter("afterDate"))!=null && StringUtils.trimToNull(request.getParameter("beforeDate"))!=null) {
	afterDate = sdf.parse(request.getParameter("afterDate"));
	beforeDate = sdf.parse(request.getParameter("beforeDate"));
}

System.out.println("from " + afterDate + " => " + beforeDate);

List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();

Long kid = id != null ? QAUtil.id2Kid(id) : null;
if (kid == null && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	System.err.println("QA[" + id + "] not found or permission denied");
	return;
}

Integer admId = new Integer(StringUtils.defaultString(request.getParameter("admId"), "-1"));
if (admId != -1) {
	AdminUser targetAdm = AdminUserFacade.getInstance().get(admId);
	
	if (!targetAdm.getTenantIdSet().contains(t.getId())) {
		System.err.println("Target admin permission deinied");
		return;
	}
}

String bypassAdmIds = null;
if(request.getParameterValues("bypassAdmIds") != null && request.getParameterValues("bypassAdmIds").length > 0){
	bypassAdmIds = StringUtils.trimToNull(StringUtils.join(request.getParameterValues("bypassAdmIds"), ","));
}

String qaCategoryIdx = StringUtils.trimToNull(request.getParameter("qaCategoryIdx"));

if ("json".equals(format)) {
	int rows = Integer.parseInt( StringUtils.defaultString( request.getParameter("rows"), "1000") );
	int start = Integer.parseInt( StringUtils.defaultString( request.getParameter("start"), "0") );
	logs = EvaluationLogEntity.listBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId != -1 ? admId : null, kid, flag, lastTestResult, null, null, null, distinctQuestion, qaCategory, bypassAdmIds, afterDate, beforeDate, start, rows);
	response.setContentType("application/json");
	JSONArray jsonLogs = new JSONArray(new JSONSerializer().deepSerialize(logs));
	
	for (int i=0; i < jsonLogs.length(); i++) {
		JSONObject jsonLog = jsonLogs.getJSONObject(i);
		
		if (jsonLog.has("admId")) {
			String name = AdminUser.getPrintableName(jsonLog.getInt("admId"));
			jsonLog.put("admName", name);
		}
	}
	
	out.print(jsonLogs.toString(2));
} else if ("csv".equals(format)) {
	response.setContentType("text/csv");
	response.setCharacterEncoding("UTF-8");
	response.setHeader("Content-disposition", "attachment; filename=" + new Date().getTime() + "_Evaluation_Log.csv");
    com.intumit.quartz.AutoEvaluationJob.generateDetailLogCsv(out, exportDetails, t, admId != -1 ? admId : null, kid, flag, lastTestResult, afterDate, beforeDate, distinctQuestion, qaCategory, bypassAdmIds, -1);
}else if ("xlsx".equals(format)){
 response.setContentType("application/vnd.ms-excel");
 response.setCharacterEncoding("UTF-8");
 response.setHeader("Content-disposition", "attachment;filename="+new Date().getTime()+ "_Evaluation_Log.xlsx");
 // 不設的話 fileDownload的dialog不會關掉
 response.setHeader("Set-Cookie", "fileDownload=true; path=/");
 final String[] header =
 		exportDetails
 	? new String[] { "id", "admName", "clientIp", "flag", "correctAnswerId", "description", "robotAnswerId", "qaCategory", "question", "score", "originalTime", "timestamp", "conversations", "lastTestResult", "lastTestResultTimestamp", "eservice", "userType" }
 	: new String[] { "id", "admName", "clientIp", "flag", "correctAnswerId", "description", "robotAnswerId", "qaCategory", "question", "score", "originalTime", "timestamp", "eservice", "userType" };
    logs = EvaluationLogEntity.listBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId != -1 ? admId : null, kid, flag, lastTestResult, null, null, null, distinctQuestion, qaCategory, bypassAdmIds, afterDate, beforeDate);
 com.intumit.quartz.AutoEvaluationJob.generateDetailLogXls(header,logs,response.getOutputStream());
 out.clear();
 out = pageContext.pushBody();
}
%>
