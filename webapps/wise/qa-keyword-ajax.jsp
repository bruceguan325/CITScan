<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"

import="com.intumit.systemconfig.WiseSystemConfig"
import="com.intumit.solr.robot.*"
import="java.util.*"
import="com.intumit.solr.tenant.*"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.robot.dictionary.*"
%>
<%
String q = request.getParameter("q");
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

DictionaryDatabase[] akps = AttentionKeywordDictionary.search(t.getId(), q.toCharArray(), null);
ArrayList<Map<String, Object>> keyword = new ArrayList<Map<String, Object>>();
JSONObject wo = new JSONObject();

boolean marketingWord = false;
boolean negativeWord = false;
String sensitiveAnswer = "";

if (akps != null && akps.length > 0) {
	List<String> k = new ArrayList<String>();

	for (DictionaryDatabase kp: akps) {
		k.add(kp.getKeyword());
		Map<String, Object> keywordData = new HashMap<String, Object>();
		keywordData.put( "keyword", kp.getKeyword() );
		keywordData.put( "flag", kp.getPurposesJson() );
		keyword.add(keywordData);

		// 敏感詞直接跳選項，選項在 options 變數裡頭
		if (kp.getPurposeSet().contains(DictionaryDatabase.Purpose.BLACKLIST)) {
			negativeWord = true;
		}
		
		// 行銷詞目前不是給選項，之後應該也用 options 之類的機制來做
		if (kp.getPurposeSet().contains(DictionaryDatabase.Purpose.MARKETING)) {
			marketingWord = true;
		}
	}
}

try {
	wo.put("marketingKeywords", marketingWord);
	wo.put("negativeKeywords", negativeWord);
	wo.put("keyword", keyword);
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