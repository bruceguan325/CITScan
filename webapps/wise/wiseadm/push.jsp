<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.dataimport.*" %>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%>
<%
String coreName = request.getParameter("coreName");
String entityName = request.getParameter("entityName");
String primaryKey = request.getParameter("pk");
String action = request.getParameter("action");

if (coreName == null || entityName == null || primaryKey == null || action == null) {
	System.out.println("Not complete push command [" + coreName + "/" + entityName + "/" + primaryKey + "/" + action + "]");
	
	return;
}

System.out.println("Command [" + coreName + "/" + entityName + "/" + primaryKey + "/" + action + "]");

if (CURDLog.DELETE.equals(action)) {

	CURDLogFacade.getInstance().save(coreName,entityName,primaryKey,action);	
}
else if (CURDLog.CREATE.equals(action)) {

	CURDLogFacade.getInstance().save(coreName,entityName,primaryKey,action);
}
else if (CURDLog.UPDATE.equals(action)) {

	CURDLogFacade.getInstance().save(coreName,entityName,primaryKey,action);
}
else {
	System.out.println("NOT A LEGAL ACTION:" + action);
}
%>
