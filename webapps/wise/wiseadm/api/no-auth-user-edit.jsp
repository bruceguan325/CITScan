<%@page import="com.intumit.solr.SearchManager"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.tenant.*"
import="com.intumit.util.DesUtil"
import="java.net.URLDecoder"
%><%!
%><%
	JSONObject errorOutput = new JSONObject();
errorOutput.put("errorCode", 400);
JSONObject resp = new JSONObject();
String action = request.getParameter("action");
Tenant t = null;

if (request.getParameter("apikey") != null) {
	String apikey = request.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
	errorOutput.put("errorMessage", "Tenant info error.");
	out.println(errorOutput.toString(2));
	return;
}

String encodedUserInfo = request.getParameter("user");
if(StringUtils.isBlank(encodedUserInfo)) {
	errorOutput.put("errorMessage", "User info leak.");
	out.println(errorOutput.toString(2));
	return;
}

String[] userInfos = DesUtil.decrypt(encodedUserInfo).split("@@");
if("del".equalsIgnoreCase(action)) {
	if(userInfos.length < 2) {
		errorOutput.put("errorMessage", "User info leak.");
		out.println(errorOutput.toString(2));
		return;
	}

	Long userInfoTime = Long.valueOf(userInfos[1]);
	if(System.currentTimeMillis() - 5*60*1000 > userInfoTime) {
		errorOutput.put("errorMessage", "User info error!");
		out.println(errorOutput.toString(2));
		return;
	}

	AdminUser editUser = AdminUserFacade.getInstance().getByLoginName(userInfos[0]);
	if(editUser != null) {
		AdminUserFacade.getInstance().delete(editUser.getId());
	}
	resp.put("success", true);
}
else {
	if(userInfos.length < 4) {
		errorOutput.put("errorMessage", "User info leak.");
		out.println(errorOutput.toString(2));
		return;
	}

	Long userInfoTime = Long.valueOf(userInfos[1]);
	if(System.currentTimeMillis() - 5*60*1000 > userInfoTime) {
		errorOutput.put("errorMessage", "User info error!");
		out.println(errorOutput.toString(2));
		return;
	}

	AdminUser editUser = AdminUserFacade.getInstance().getByLoginName(userInfos[0]);
	if(editUser == null) {
		editUser = new AdminUser();
		editUser.setLoginName(userInfos[0]);
		editUser.setName(userInfos[2]);
		AdminGroup g = AdminGroupFacade.getInstance().getByName(userInfos[3]);
		System.out.println(URLDecoder.decode(userInfos[3], "UTF-8"));
		System.out.println((g==null) + "*************");
		editUser.setAdminGroups(g == null ? "" : g.getId() + "");
		editUser.setTenantIds(t.getId() + "");
	}
	else {
		editUser.setName(userInfos[2]);
		AdminGroup g = AdminGroupFacade.getInstance().getByName(userInfos[3]);
		System.out.println(userInfos[3]);
		System.out.println((g==null) + "*************");
		editUser.setAdminGroups(g == null ? "" : g.getId() + "");
		editUser.setTenantIds(t.getId() + "");
	}
	AdminUserFacade.getInstance().saveOrUpdate(editUser);

	resp.put("user", editUser.getId());
	resp.put("success", true);
}
%><%= resp.toString(4) %>
