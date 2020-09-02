<%@page import="com.intumit.solr.tenant.Apikey"%>
<%@page import="com.intumit.solr.tenant.Tenant"%>
<%@page import="org.apache.wink.json4j.JSONObject"%>
<%@page language="java" contentType="application/json; charset=utf-8"
	pageEncoding="UTF-8" %>
<%
// 同源政策
response.setHeader("Access-Control-Allow-Origin", "*");

JSONObject result = new JSONObject();
String apikey = request.getParameter("apikey");
String msg = "gateway validation success";
int status = 200;

if (apikey != null) {
	Apikey key = Apikey.getByApiKey(apikey);
	if (key != null && key.isValid()) {
		key.incCallCounter();
		Apikey.saveOrUpdate(key);
	} else {
		msg = "apikey is not valid";
		status = 403;
	}
} else {
	msg = "not permission";
	status = 403;

}

result.put("msg", msg);
result.put("status", status);
System.out.println(msg);
out.println(result.toString(2));
%>