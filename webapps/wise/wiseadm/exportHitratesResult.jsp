<%@page import="java.util.concurrent.TimeUnit"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.text.*"
import="java.util.*"
import="com.intumit.message.*"
import="com.intumit.solr.robot.*"
import="org.apache.commons.lang.StringUtils"
import="org.json.*"
import="org.jsoup.Jsoup"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String start = request.getParameter("e_start").replace("T", " ") + ":00";
System.out.println(start);
String end = request.getParameter("e_end").replace("T", " ") + ":59";
System.out.println(end);
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
List<EvaluationLogEntity> logEvaluations = EvaluationLogEntity.listByDate(t.getId(), sdf.parse(start), sdf.parse(end));
response.setHeader("Content-Type","text/csv; charset=" + (com.intumit.hithot.HitHotLocale.zh_TW == t.getLocale() ? "BIG5" : "UTF-8"));
response.setHeader("Content-Disposition","inline; filename="+new Date().getTime()+"_Log_Hitrates.csv");
String outputData = "";
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String title = MessageUtil.getMessage(locale, "data") + "," +
	MessageUtil.getMessage(locale, "statistics.msgCount.noAnswer") + "," +
	MessageUtil.getMessage(locale, "statistics.msgCount.hasAnswer") + "," +
	MessageUtil.getMessage(locale, "statistics.msgCount.fromUser") + "," +
	MessageUtil.getMessage(locale, "dashboard.totalSessions") + "," +
	MessageUtil.getMessage(locale, "hitrates") + "\n";
out.clear();

SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
Date startDate = dateFormat.parse(start);
Date endDate = dateFormat.parse(end);

long diff = endDate.getTime() - startDate.getTime();
long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;

Object[][] statisticsByDay = ServiceLogEntity.statisticsByDay(t.getId(), startDate, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(end));

long[] peopleCount = new long[(int) days];
for (int i = 0; i < statisticsByDay.length; i++) {
	long dif =(dateFormat.parse(statisticsByDay[i][0].toString()).getTime() - startDate.getTime())/(1000*60*60*24);
	peopleCount[(int) dif] = (long)statisticsByDay[i][1];
}

for (int i = 0; i < days; i++) {
	Date after = new Date(startDate.getYear(), startDate.getMonth(), startDate.getDate() + i);
	Date before = new Date(startDate.getYear(), startDate.getMonth(), startDate.getDate() + i + 1);
	float[] data = ServiceLogEntity.hitrateStatistic(after, before, t.getId());
	outputData += "\"" + dateFormat.format(after) + "\",";
	outputData += "\"" + data[0] + "\",";
	outputData += "\"" + data[1] + "\",";
	outputData += "\"" + data[2] + "\",";
	outputData += "\"" + peopleCount[i] + "\",";
	outputData += "\"" + (data[3] * 100)+ "%\"\n";
}

out.print(title + outputData);
%>