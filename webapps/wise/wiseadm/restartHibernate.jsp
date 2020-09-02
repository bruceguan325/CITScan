<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%><%
com.intumit.hibernate.HibernateUtil.shutdown();
com.intumit.hibernate.HibernateUtil.init();
%>
done.
