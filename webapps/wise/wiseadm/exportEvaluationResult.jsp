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
response.setHeader("Content-Disposition","inline; filename="+new Date().getTime()+"_Log_Evaluation.csv");
String outputData = "";
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String title = MessageUtil.getMessage(locale, "end.record.time")+","+MessageUtil.getMessage(locale, "evaluation.correct.answer.id")+","+MessageUtil.getMessage(locale, "evaluation.correct.answer.des")+","+MessageUtil.getMessage(locale, "evaluation.robot.answer.id")+","+MessageUtil.getMessage(locale, "test.question")+","+MessageUtil.getMessage(locale, "test.time")+","+MessageUtil.getMessage(locale, "evaluation.current.use")+","+MessageUtil.getMessage(locale, "test.question")+","+MessageUtil.getMessage(locale, "robot.answer")+",score\n";
out.clear();

for(EvaluationLogEntity logEvaluation : logEvaluations){
	outputData += "\"" + logEvaluation.getTimestamp() + "\",";
	outputData += "\"" + logEvaluation.getCorrectAnswerId() + "\",";
	outputData += "\"" + logEvaluation.getDescription() + "\",";
	outputData += "\"" + logEvaluation.getRobotAnswerId() + "\",";
	outputData += "\"" + logEvaluation.getQuestion() + "\"\n";
	JSONObject conversation = new JSONObject(logEvaluation.getConversations());
	if (true) {            
        	Object obj = conversation;
        	if (obj instanceof JSONObject) {
        		JSONObject msg = (JSONObject)obj;
            
	        	String dt = msg.has("datetime") ? msg.getString("datetime") : "";
	        	String currentQaUse = msg.has("currentQaUse") ? msg.getString("currentQaUse") : "";
	        	String question = msg.has("originalQuestion") ? msg.getString("originalQuestion") : "N/A";
	        	String output = msg.has("output") ? msg.getString("output") : "N/A";
	        	String score = msg.has("score") ? msg.getString("score") : "";
	        	
	        	outputData += "-,-,-,-,-,";
	        	outputData += "\"" + dt + "\",";
	        	outputData += "\"" + currentQaUse + "\",";
	        	outputData += "\"" + Jsoup.parse(StringUtils.replaceChars(question, ",\"\\", " ")).text() + "\",";
	        	outputData += "\"" + Jsoup.parse(StringUtils.replaceChars(output, ",\"\\", " ")).text() + "\",";
	        	outputData += "\"" + score + "\"\n";
        	
        	}
    }
}

out.print(title+outputData);
%>