<%@page import="com.intumit.solr.robot.function.GETQ"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
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
import="com.intumit.solr.tenant.*"
import="com.intumit.citi.MisLogReport"
%>
<%!
public static Date getFirstMonthDay(Calendar calendar) {
       calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
       calendar.set(Calendar.HOUR, 0);                    
       calendar.set(Calendar.MINUTE, 0);                     
       calendar.set(Calendar.SECOND, 0);                
       calendar.set(Calendar.MILLISECOND, 0);
       return calendar.getTime();
}

public static Date getLastMonthDay(Calendar calendar) {
       calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE) - 1);
       calendar.set(Calendar.HOUR, 23 + 12);                    
       calendar.set(Calendar.MINUTE, 59);                     
       calendar.set(Calendar.SECOND, 59);                
       calendar.set(Calendar.MILLISECOND, 99);
       return calendar.getTime();
}
%><%
String ch = request.getParameter("ch"); //channel

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String yearStr = request.getParameter("year");
String monthStr = request.getParameter("month");
String after = null; //每月第一天
String before = null; //每月最後一天
if (yearStr != null && monthStr != null) {
	Calendar calendar = Calendar.getInstance();
	calendar.set(Calendar.YEAR, Integer.parseInt(yearStr));
    calendar.set(Calendar.MONTH, Integer.parseInt(monthStr)-1);
    calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
    after = sdf.format(getFirstMonthDay(calendar));
    before = sdf.format(getLastMonthDay(calendar));
    
} 

//auto(點擊)  or manual(對話框)
String action = ""; 

//true(主選單) or false + has father(子選項)  or false(一般選項) 
Boolean menu = false;

//選擇報表  FaF (主選單) / FaS(子選項) / EbY(手動輸入) / CkO(點擊項目) / NoA(No_Answer)
String branch = request.getParameter("branch"); 
if (branch != null && branch.equals("FaF")) {
	menu = true;
	action = "auto";
} else if (branch != null && branch.equals("FaS")) {
	menu = false;
	action = "auto";
} else if (branch != null && branch.equals("EbY")) {
	menu = false;
	action = "manual";
} else if (branch != null && branch.equals("CkO")) {
	menu = false;
	action = "auto";
} else if (branch != null && branch.equals("NoA")) {
	menu = false;
	action = "manual";
}

JSONObject obj = new JSONObject();
JSONArray dataArray = new JSONArray();
try {
	obj.put("ch", ch);
	List<String> kidList = MisLogReport.getKidList(ch, branch);
	for (int i = 0; i < kidList.size(); i++){
		JSONObject kidJO = new JSONObject();
		String kid = kidList.get(i);
		String question = "";
		String father = "";
		String currentCategory = "";
		int aa = 0;
		List<Object[]> misLogReportList = MisLogReport.getCountByKid(ch, branch, after, before, kid, action, menu);
		JSONObject dateJO = new JSONObject();
		
		if (branch != null && branch.equals("FaF")) {
			for (int j = 0; j < misLogReportList.size(); j++) {
				Object[] object = (Object[]) misLogReportList.get(j);
				String date = (java.sql.Timestamp) object[0] + "";
				String count = (Long) object[1] + "";
				question = (String) object[2];
				menu = (Boolean) object[3];
				dateJO.put(date, count);
			}
			kidJO.put("kid", kid);
			kidJO.put("question", question);
			kidJO.put("date", dateJO);
		} else if (branch != null && branch.equals("FaS")) {
			for (int j = 0; j < misLogReportList.size(); j++) {
				Object[] object = (Object[]) misLogReportList.get(j);
				String date = (java.sql.Timestamp) object[0] + "";
				String count = (Long) object[1] + "";
				question = (String) object[2];
				father = (String) object[3];
				dateJO.put(date, count);
			}
			kidJO.put("kid", kid);
			kidJO.put("question", question);
			kidJO.put("date", dateJO);
			kidJO.put("father", father);
		} else if (branch != null && branch.equals("EbY")) {
			for (int j = 0; j < misLogReportList.size(); j++) {
				Object[] object = (Object[]) misLogReportList.get(j);
				String date = (java.sql.Timestamp) object[0] + "";
				String count = (Long) object[1] + "";
				question = (String) object[2];
				currentCategory = (String) object[3];
				dateJO.put(date, count);
			}
			kidJO.put("kid", kid);
			kidJO.put("question", question);
			kidJO.put("date", dateJO);
			kidJO.put("currentCategory", currentCategory);
		} else if (branch != null && branch.equals("CkO")) {
			for (int j = 0; j < misLogReportList.size(); j++) {
				Object[] object = (Object[]) misLogReportList.get(j);
				String date = (java.sql.Timestamp) object[0] + "";
				String count = (Long) object[1] + "";
				question = (String) object[2];
				currentCategory = (String) object[3];
				dateJO.put(date, count);
			}
			kidJO.put("kid", kid);
			kidJO.put("question", question);
			kidJO.put("date", dateJO);
			kidJO.put("currentCategory", currentCategory);
		} else if (branch != null && branch.equals("NoA")) {
			for (int j = 0; j < misLogReportList.size(); j++) {
				Object[] object = (Object[]) misLogReportList.get(j);
				String date = (java.sql.Timestamp) object[0] + "";
				String count = (Long) object[1] + "";
				dateJO.put(date, count);
			}
			kidJO.put("date", dateJO);
			kidJO.put("answerType", "No_Answer");
		}
		
		if(branch.equals("NoA")){
			dataArray.put(kidJO);
			break;
		}
		
		dataArray.put(kidJO);
	}
	obj.put("data", dataArray);
} catch (Exception e) {
	System.out.println("MIS: " + e);
}

%>
<%= obj %>