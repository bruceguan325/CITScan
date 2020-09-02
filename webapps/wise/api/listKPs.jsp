<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" trimDirectiveWhitespaces="true"
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.robot.dictionary.*"
import="org.apache.commons.lang.StringUtils"
%>
<%!
%><%
	Tenant t = null;
if (request.getParameter("apikey") != null) {
	String apikey = request.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");

	out.println(errorOutput.toString(2));

	return;
}

String q = StringUtils.defaultString(request.getParameter("q"));

JSONObject resp = new JSONObject();
DictionaryDatabase[] kps = KnowledgePointDictionary.search(t.getId(), q.toCharArray(), null);
JSONArray kpArray = new JSONArray();
for(DictionaryDatabase kp : kps) {
	kpArray.put(kp.getKeyword());
}

resp.put("kp", kpArray);
%>
<%= resp.toString(2) %>