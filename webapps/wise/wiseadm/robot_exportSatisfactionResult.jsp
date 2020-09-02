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
response.setHeader("Cache-Control", "no-cache");
response.setHeader("Pragma","no-cache");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String start = request.getParameter("start").replace("T", " ") + ":00";
System.out.println(start);
String end = request.getParameter("end").replace("T", " ") + ":59";
System.out.println(end);
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
List<ServiceLogEntity> logEntitys = ServiceLogEntity.listByDate(t.getId(), "robot:cathay:chat", sdf.parse(start), sdf.parse(end));

response.setHeader("Content-Type","text/csv; charset=" + (com.intumit.hithot.HitHotLocale.zh_TW == t.getLocale() ? "BIG5" : "UTF-8"));
response.setHeader("Content-Disposition","inline; filename="+new Date().getTime()+"_Log_Entity.csv");
String outputData = "";
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String title = MessageUtil.getMessage(locale, "session.number")+","+MessageUtil.getMessage(locale, "enter.time.record")+","+"總應答時間"+","+"拋送問卷"+","+"滿意度執行結果Q1"+"\n";
out.clear();
out.write(title);

for(ServiceLogEntity logEntity : logEntitys){
	long startT = logEntity.getTsCreated().getTime();
	long endT = logEntity.getTimestamp().getTime();
	long diff = endT - startT;
	long days = diff / (1000 * 60 * 60 * 24);
	long hours = (diff-days*(1000 * 60 * 60 * 24))/(1000* 60 * 60);
	long minutes = (diff-days*(1000 * 60 * 60 * 24)-hours*(1000* 60 * 60))/(1000* 60);
	long second = (diff-days*(1000 * 60 * 60 * 24)-hours*(1000* 60 * 60)-minutes*(1000*60))/(1000);
	out.write("\"" + logEntity.getQaId() + "\",");
	out.write("\"" + sdf.format(logEntity.getTsCreated()) + "\",");
	out.write("\"" + hours + "時" + minutes + "分" + second + "秒" + "\",");
	out.write("\"" + "是" + "\",");
	
	JSONObject statusMessages = new JSONObject(logEntity.getStatusMessage() == null ? "{}":logEntity.getStatusMessage());
	if (statusMessages.has("questionary")){
		JSONArray statusMessage = new JSONArray();
		try{
			statusMessage = statusMessages.getJSONArray("questionary");
		}
		catch (Exception ex) {};
		
		Object obj = statusMessage.get(0);
		if (obj instanceof JSONObject) {
			JSONObject questionary = (JSONObject)obj;
			String q1 = questionary.has("questionary1") ? questionary.getString("questionary1") : "N/A";
			out.write("\"" + q1 + "\"\n");
		}
	} else {
		out.write("\"" + "N/A" + "\"\n");
	}
}
%>