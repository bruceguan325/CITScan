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
String id = request.getParameter("id");
String token = request.getParameter("token");
if(StringUtils.isNotBlank(id) && StringUtils.isNotBlank(token)){
	try{
		boolean success = DeviceBinding.setStatusToNormal(id, token);
		if(success){
			status = 1;
		}
	}catch(Exception e){
		e.printStackTrace();
	}
}
JSONObject resp = new JSONObject();
resp.put("status", status);
%><%= resp.toString(2) %>
