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
String id = request.getParameter("id");
String format = StringUtils.defaultString(request.getParameter("format"), "json");
String action = StringUtils.defaultString(request.getParameter("action"), "list");
Integer flag = request.getParameter("flag") != null ? new Integer(request.getParameter("flag")) : null;
Integer lastTestResult = StringUtils.trimToNull(request.getParameter("ltr")) != null ? new Integer(request.getParameter("ltr")) : null;
boolean exportDetails = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("ed"), "false")) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() > 0 || (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O6) != 0);
List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

Date afterDate = null;
Date beforeDate = null;
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

if (request.getParameter("timeRange") != null) {
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(request.getParameter("timeRange"));
	afterDate = timeRangePair[0];
	beforeDate = timeRangePair[1];
} else if (request.getParameter("afterDate")!=null && request.getParameter("beforeDate")!=null) {
	afterDate = sdf.parse(request.getParameter("afterDate"));
	beforeDate = sdf.parse(request.getParameter("beforeDate"));
}

System.out.println("from " + afterDate + " => " + beforeDate);

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

String logSource = StringUtils.trimToNull(request.getParameter("logSource"));
String qaCategoryIdx = StringUtils.trimToNull(request.getParameter("qaCategoryIdx"));
Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();
String qaCategory = StringUtils.trimToNull(StringUtils.isNotBlank(qaCategoryIdx) ? qaCategoryMap.get(Integer.parseInt(qaCategoryIdx)) : null);

if ("json".equals(format)) {
	int rows = Integer.parseInt( StringUtils.defaultString( request.getParameter("rows"), "1000") );
	int start = Integer.parseInt( StringUtils.defaultString( request.getParameter("start"), "0") );
	logs = EvaluationLogEntity.listAndGroupByQaAltTemplateMkeyAndKeywords(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId != -1 ? admId : null, kid, flag, lastTestResult, logSource, qaCategory, null, afterDate, beforeDate, start, rows);
	response.setContentType("application/json");
	JSONArray jsonLogs = new JSONArray(new JSONSerializer().deepSerialize(logs));
	
	for (int i=0; i < jsonLogs.length(); i++) {
		JSONObject jsonLog = jsonLogs.getJSONObject(i);
		String mkey = jsonLog.optString("suggestQaTemplateMkey");
		String keywords = jsonLog.optString("suggestQaTemplateKeywords");
		
		if (mkey != null) {
			int c = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId != -1 ? admId : null, kid, flag, lastTestResult, logSource, mkey, keywords, false, qaCategory, null, afterDate, beforeDate).intValue();
			jsonLog.put("count", c);
		}
	}
	
	out.print(jsonLogs.toString(2));
} 
%>
