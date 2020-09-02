<%@ include file="/commons/taglib.jsp"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qaplugin.*"
%>
<%
JSONObject data = (JSONObject) request.getAttribute(HierarchicalQA.REQ_ATTR_QA_DATA);
JSONArray _1stLvChildren = data.getJSONArray("children");
Map<String, String> hIdQaMap = (Map<String, String>) request.getAttribute(HierarchicalQA.REQ_ATTR_ID_QA_MAP);
if(_1stLvChildren.length() > 0){
	String answer = (String) request.getAttribute("answer");
	if(StringUtils.isNotBlank(answer)){
%>
<br/><br/>
<%	
	}
	for(int i=0; i<_1stLvChildren.length(); i++){
		Object obj = _1stLvChildren.opt(i);
		String text = null;
		if(obj instanceof String){
			text = hIdQaMap.get(obj);
		}else if(obj instanceof JSONObject){
			JSONObject json = (JSONObject)obj;
			text = json.optString("text");
		}
		if(i > 0){
%>
<br/>
<%
		}
%>
<bean:message key='global.enter'/><%= i + 1 %>.<%= StringEscapeUtils.escapeHtml(text) %>
<%
	}
}
%>