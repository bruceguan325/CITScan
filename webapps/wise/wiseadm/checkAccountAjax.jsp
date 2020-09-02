<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="org.apache.commons.lang.StringUtils"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}

%><%
String account = request.getParameter("account");
String id = request.getParameter("id");
AdminUser oldUser = AdminUserFacade.getInstance().getByLoginName(account);
boolean duplicate = false;
if(oldUser != null && !StringUtils.equals(id, "" + oldUser.getId())) {
	duplicate = true;
}
%>
{"duplicate":<%=duplicate %>}