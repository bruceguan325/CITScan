<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.dataset.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="com.intumit.quartz.ScheduleUtils"%>
<%@ page import="com.intumit.quartz.Job"%>

<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
String dsId = request.getParameter("d");
int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, Integer.parseInt(dsId));
if ((acl & GroupDataSet.O4) == 0) return; // 無權限
String action = request.getParameter("action");
String esId = request.getParameter("id");
String docId = request.getParameter("docId");

ElevatorSet eSet = ElevatorSetFacade.getInstance().get(new Long(esId));

if ("removeFAT".equalsIgnoreCase(action)) {
	List<String> list = new ArrayList<String>(eSet.getFixedAtTopAsList());
	list.remove(docId);
	eSet.setFixedAtTop(StringUtils.join(list, ","));
	ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}
else if ("addFAT".equalsIgnoreCase(action)) {
	List<String> list = new ArrayList<String>(eSet.getFixedAtTopAsList());
	list.add(0, docId);
	eSet.setFixedAtTop(StringUtils.join(list, ","));
	ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}
else if ("removeFQFAT".equalsIgnoreCase(action)) {
	String fq = request.getParameter("fq");
	if (eSet.removeFacetQueryFixedAtTop(fq))
		ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}
else if ("addFQFAT".equalsIgnoreCase(action)) {
	String fq = request.getParameter("fq");
	eSet.addFacetQueryFixedAtTop(fq);
	ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}
else if ("removeH".equalsIgnoreCase(action)) {
	List<String> list = new ArrayList<String>(eSet.getHiddenList());
	list.remove(docId);
	eSet.setHidden(StringUtils.join(list, ","));
	ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}
else if ("addH".equalsIgnoreCase(action)) {
	List<String> list = new ArrayList<String>(eSet.getHiddenList());
	list.add(docId);
	eSet.setHidden(StringUtils.join(list, ","));
	ElevatorSetFacade.getInstance().saveOrUpdate(eSet);
}

response.sendRedirect("qaElevator-search.jsp?d=" + dsId + "&id=" + esId);
%>