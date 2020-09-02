<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" trimDirectiveWhitespaces="true"
import="com.intumit.solr.tenant.*"
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

JSONObject resp = new JSONObject();
resp.put("qaCategory", t.getQaCategory());
%>
<%= resp %>