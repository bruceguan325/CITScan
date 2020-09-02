<%@ page pageEncoding="UTF-8" language="java"
import="flexjson.*"
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
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) {
	return;
}
%><%!

%><%

boolean distinctQuesiton = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("dq"), "false"));
boolean calculateAllQaCategories = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("allCategories"), "false"));
boolean calculateAllTimeRanges = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("allTimeRanges"), "false"));

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String format = StringUtils.defaultString(request.getParameter("format"), "json");
List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
String ns = StringUtils.defaultString(request.getParameter("ns"), QAUtil.CURRENT_EVALUATION_LOG_NS);
String qaCategory = StringUtils.trimToNull(request.getParameter("qaCategory"));
String logSource = StringUtils.trimToNull(request.getParameter("logSource"));

Integer admId = new Integer(StringUtils.defaultString(request.getParameter("admId"), "-1"));
if (admId != -1) {
	AdminUser targetAdm = AdminUserFacade.getInstance().get(admId);
	
	if (!targetAdm.getTenantIdSet().contains(t.getId())) {
		return;
	}
}
else {
	admId = null;
}

String bypassAdmIds = null;
if(request.getParameterValues("bypassAdmIds") != null && request.getParameterValues("bypassAdmIds").length > 0){
	bypassAdmIds = StringUtils.trimToNull(StringUtils.join(request.getParameterValues("bypassAdmIds"), ","));
}

Date afterDate = null;
Date beforeDate = null;
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

if (request.getParameter("timeRange") != null) {
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(request.getParameter("timeRange"));
	afterDate = timeRangePair[0];
	beforeDate = timeRangePair[1];
} else if (request.getParameter("afterDate")!=null && request.getParameter("beforeDate")!=null) {
	afterDate = sdf.parse(request.getParameter("afterDate"));
	beforeDate = sdf.parse(request.getParameter("beforeDate"));
}

Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();
if ("json".equals(format)) {
	JSONObject json = new JSONObject();
	
	if (calculateAllQaCategories) {
	    for(Integer qaCateIdx : qaCategoryMap.keySet()) {
			Number[] flagCount = new Number[] {
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 0, null, logSource, null, null, false, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 1, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 2, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 3, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 4, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 5, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
					EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 6, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, null, null),
			};
			json.put("flagStatistics_" + qaCateIdx, flagCount);
	
			if (calculateAllTimeRanges) {
				for (String timeRange: WiSeUtils.availableTimeRanges) {
					Date[] timeRangePair = WiSeUtils.getTimeRangePair(timeRange);
		
					Number[] flagCount4TR = new Number[] {
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 0, null, logSource, null, null, false, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 1, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 2, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 3, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 4, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 5, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
						EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 6, null, logSource, null, null, distinctQuesiton, qaCategoryMap.get(qaCateIdx), bypassAdmIds, timeRangePair[0], timeRangePair[1]),
					};
		
					json.put("flagStatistics_" + qaCateIdx + "_" + timeRange, new JSONArray(flagCount4TR));
				}
			}
	    }
	}
    
    Number[] allFlagCount = new Number[] {
    		EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 0, null, logSource, null, null, false, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 1, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 2, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 3, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 4, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 5, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
			EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 6, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate),
    };
    json.put("flagStatistics", allFlagCount);

    if (calculateAllTimeRanges) {
	    for (String timeRange: WiSeUtils.availableTimeRanges) {
			Date[] timeRangePair = WiSeUtils.getTimeRangePair(timeRange);
	
			Number[] flagCount4TR = new Number[] {
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 0, null, logSource, null, null, false, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 1, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 2, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 3, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 4, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 5, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
				EvaluationLogEntity.countBy(t.getId(), ns, admId, null, 6, null, logSource, null, null, distinctQuesiton, qaCategory, bypassAdmIds, timeRangePair[0], timeRangePair[1]),
			};
	
			json.put("flagStatistics_" + timeRange, new JSONArray(flagCount4TR));
		}
    }

	response.setContentType("application/json");
	out.print(json);

}
%>
