<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
%><%!

%><%
int status = 0;
JSONObject result = new JSONObject();
String rId = null;
String rToken = null;
String updateTime = null;
String id = request.getParameter("id");
String token = request.getParameter("token");
boolean hasId = StringUtils.isNotBlank(id);
boolean hasToken = StringUtils.isNotBlank(token);
if(hasId || hasToken){
	try{
		DeviceBinding db = null;
		if(hasId){
			db = DeviceBinding.getByNid(id);
		}else if(hasToken){
			db = DeviceBinding.getByDeviceToken(token);
		}
		if(db != null && db.getStatus() != DeviceBinding.Status.deleted){
			rId = db.getNid();
			rToken = db.getDeviceToken();
			updateTime = db.getFormattedUpdateTime();
		}
		status = 1;
	}catch(Exception e){
		e.printStackTrace();
	}
}
result.put("id", StringUtils.defaultString(rId));
result.put("token", StringUtils.defaultString(rToken));
result.put("updateTime", StringUtils.defaultString(updateTime));
JSONObject resp = new JSONObject();
resp.put("status", status);
resp.put("result", result);
%><%= resp.toString(2) %>
