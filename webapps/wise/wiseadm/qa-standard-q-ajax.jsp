<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.robot.QAUtil"
import="org.apache.wink.json4j.*"
import="org.apache.solr.common.SolrDocumentList"
import="org.apache.commons.lang.StringUtils"
%><%
JSONObject resp = new JSONObject();
JSONArray listQ = new JSONArray();
Tenant t = null;
String keyword = request.getParameter("keyword");
String apikey = request.getParameter("apikey");
String rows = request.getParameter("rows");
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

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("status", 403);
	errorOutput.put("msg", "apikey is not valid");
	out.println(errorOutput.toString(2));
	return;
}

if (StringUtils.isEmpty(keyword)) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("status", 403);
	errorOutput.put("msg", "keyword is not blank");
	out.println(errorOutput.toString(2));
	return;
}

// 預設5筆
int rowCount = -1;
if (rows != null) {
	try {
		rowCount = Integer.parseInt(rows);
	} catch (Exception e) {
		rowCount = 5;
	}
} else {
	rowCount = 5;
}

SolrDocumentList docs = QAUtil.getInstance(t).searchStandardQuestion(t, keyword);
if(docs != null){
	int docCount = rowCount < docs.size() ? rowCount : docs.size();
	if (docCount < 0) docCount = docs.size();
	for (int i = 0; i < docCount; i++) {
		JSONObject obj = new JSONObject();
		obj.put("id", docs.get(i).getFieldValue("id"));
		obj.put("question", docs.get(i).getFieldValue("QUESTION_s"));
		listQ.put(obj);
	}
	resp.put("questions", listQ);
}
resp.put("keyword", keyword);

%>
<%= resp %>