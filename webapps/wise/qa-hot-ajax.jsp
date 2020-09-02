<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"

import="com.intumit.systemconfig.WiseSystemConfig"
import="com.intumit.solr.robot.*"
import="java.util.*"
import="com.intumit.solr.tenant.*"
import="org.apache.wink.json4j.*"
%>
<%!
Map<String, Object> getPayload (HttpServletRequest request) {
	//System.out.println( "getParameterNames : " + request.getParameterNames() );
	
	Map<String, Object> payloadData = new HashMap<String, Object>();
	java.util.Enumeration enu = request.getParameterNames();
	while(enu.hasMoreElements()){
		String paramName = (String)enu.nextElement();
		//System.out.println( "paramName : " + paramName );
		payloadData.put( paramName, request.getParameter(paramName));
	}
	
	return payloadData;
}
%>
<%
boolean testMode = false;
if (request.getParameter("testMode") != null) {
	if (Boolean.parseBoolean(request.getParameter("testMode")))
	{
		testMode = true;
	}
}

String apikey = request.getParameter("apikey");
Tenant t = null;

if (apikey != null) {
	Apikey key = Apikey.getByApiKey(apikey);
	if (key != null && key.isValid()) {
		t = key.getTenant();
		if (t != null) {
			key.incCallCounter();
			Apikey.saveOrUpdate(key);
		}
	}
}

//如果還是沒有 tenant，則失敗
if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");

	out.println(errorOutput.toString(2));

	return;
}

String channel = request.getParameter("ch");
if (channel == null || channel.equals("")) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine channel input.");

	out.println(errorOutput.toString(2));

	return;
}

JSONArray faqJsonArray = null;
try {
	faqJsonArray = new JSONArray(t.getFaqJson());
	
	if (faqJsonArray == null || faqJsonArray.isEmpty()) {
		JSONObject errorOutput = new JSONObject();
		errorOutput.put("errorCode", 400);
		errorOutput.put("errorMessage", "Cann't determine faqJson.");

		out.println(errorOutput.toString(2));

		return;
	}
	
	
	JSONObject wo = new JSONObject();
	wo.put("answerType", "QAHOT");
	wo.put("questionType", "QAHOT");
	wo.put("link", JSONObject.NULL);
	wo.put("bundle",new JSONObject(getPayload(request)));
	wo.put("datetime", QAUtil.formatDatetime(Calendar.getInstance().getTime()));
	wo.put("channel", channel);
	
	
	for (Object tmp : faqJsonArray) {
		Map<String, Object> data = (Map<String, Object>)tmp;
		if ( channel.equals(data.get("channel")) ) {
			//System.out.println(data.get("questions").toString());
			wo.put("output", "熱門問答資料如下：");
			wo.put("questions", data.get("questions"));
			out.println(wo.toString(2));
			return;
		}
	}
	
	wo.put("output", "指定channel無資料：");
	wo.put("questions", JSONObject.NULL);
	out.println(wo.toString(2));
}
catch (Exception ex) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine faqJson or faqJson error.");

	out.println(errorOutput.toString(2));

	return;
}

%>