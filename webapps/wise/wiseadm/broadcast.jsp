<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"  pageEncoding="UTF-8" %>
<%@ page isELIgnored ="false" %>

<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.client.solrj.request.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="com.hazelcast.core.*" %>
<%@ page import="com.intumit.solr.servlet.*" %>
<%@ page import="com.intumit.solr.*" %>
<%@ page import="com.intumit.solr.NotificationEvent.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%!
%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) {
	return;
}
%>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='system.boardcast'/></TITLE>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>

<!-- / bootstrap3 -->
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>

<!-- / jquery ui -->
<link href='<%= request.getContextPath() %>/assets/stylesheets/jquery_ui/jquery-ui-1.10.0.custom.css' media='all' rel='stylesheet' type='text/css'>
<link href='<%= request.getContextPath() %>/assets/stylesheets/jquery_ui/jquery.ui.1.10.0.ie.css' media='all' rel='stylesheet' type='text/css'>
<script src='<%= request.getContextPath() %>/assets/javascripts/jquery_ui/jquery-ui.min.js' type='text/javascript'></script>

<script src='<%= request.getContextPath() %>/script/jquery.fileDownload.js' type='text/javascript'></script>
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
</HEAD>
<BODY>

<form id="myForm">
<h2><bean:message key='boardcast.title2'/>：</h2>
<ul>
<li><bean:message key='boardcast.description1'/></li>
<li><bean:message key='boardcast.description2'/></li>
<li><bean:message key='boardcast.description3'/></li>
</ul>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
String msgTitle = request.getParameter("msgTitle");
String content = request.getParameter("content");
Integer tenantId = new Integer(StringUtils.defaultString(request.getParameter("tenantId"), "0"));
%>
<bean:message key='boardcast.tenant'/>：<select name="tenantId">
<%
List<Tenant> tenants = Tenant.list();
for (Tenant t: tenants) {
%>
<option value="<%= t.getId() %>" <%= t.getId() == tenantId ? "selected" : "" %>><%= t.getName() %>(<%= t.getNotes() %>)</option>
<%
}
%>
<option value="-1">!!全部!!</option>
</select><br>
<bean:message key='boardcast.title'/>：<input type="text" name="msgTitle" value="<%= StringUtils.trimToEmpty(msgTitle) %>"><br>
<bean:message key='boardcast.body'/>：
<textarea name="content" cols="100" rows="10"><%= StringUtils.trimToEmpty(content) %></textarea>
<input type="hidden" name="dryRun" value="true">
<input type="submit" value="test">
<input type="button" id="doIt" value="doIt">
</form>
<script>
$('#doIt').click(function() {
	$('input[name="dryRun"]').val("false");
	$('#myForm').submit();
});
</script>
<%

if (StringUtils.isNotEmpty(msgTitle) || StringUtils.isNotEmpty(content)) {
    try {
		boolean dryRun = !"false".equalsIgnoreCase(request.getParameter("dryRun"));

    	if (!dryRun) {
    		List<Integer> tenantIds = new ArrayList<Integer>();
    		if (tenantId == -1) {
    			for (Tenant ttt: tenants) {
	    			tenantIds.add(ttt.getId());
    			}
    		}
    		else {
    			tenantIds.add(tenantId);
    		}
    		
    		try {
    			for (Integer tid: tenantIds) {
	    			NotificationEvent e = new NotificationEvent();
	    			e.setSource("Broadcast by :[" + user.getName() + "]");
	    			e.setTargetType(TargetType.TENANT);
	    			e.setTarget(tid);
	    			e.setStackType(StackType.STACK);
	    			e.setTitle(msgTitle);
	    			e.setContent(content);
	    			e.setNotificationType(NotificationType.ERROR);
	    			e.setHidden(true);
	
	    			ITopic topic = HazelcastUtil.getTopic( "system-notification" );
	    			topic.publish(e);
    			}
    		}
    		catch (Exception e) {
    			HazelcastUtil.log().error("Cannot publish notification message", e);
    		}
    	}
    }
    catch (Exception ex) {
    	ex.printStackTrace();
    }
}
%>

</BODY>
</HTML>
