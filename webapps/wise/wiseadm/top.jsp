<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@page import="com.intumit.solr.tenant.Tenant"%>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@ include file="/commons/taglib.jsp"%>
<%--
// 機器人權限對應
// 1. Admin / CoreAdmin 才能進傳統 WiSe 後台
// 2. IndexAdmin 可以操作幾乎全部機器人後台（qaXXXXXX.jsp）
// 3. SystemAdmin 可以進入測試介面
// 4. 還沒想到
--%>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
//com.intumit.solr.robot.dictionary.bank.DepositInterestRateDict.clear(7);
//com.intumit.solr.robot.dictionary.KnowledgePointDictionary.clear(7);
if (user == null) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}
else if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
	&& AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0
	) {
	response.sendRedirect("qaOnlineUserMon.jsp");
	return;
}
%>
      <ul class="nav navbar-nav">
		<% if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() > 0) { %>
			<li><A HREF="index-admin-menu.jsp" target="leftFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.solr.management"/></A></li>

			<% if (!WiSeEnv.isRobotIndexMode()) { %>
			<li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="robot.management"/> <span class="caret"></span></a>
	          <ul class="dropdown-menu">
				<li><A HREF="qaAdmin.jsp" target="parent"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.qa"/></A></li>
				<li><a href='<%= request.getContextPath() %>/wiseadm/test.jsp' target='_new'><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.test"/></a></li>

            		<li role="separator" class="divider"></li>
				<li><A HREF="qaAdmin2.jsp" target="parent"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.life.langue"/></A></li>
				<li><A HREF="qaEvaluationLogAutoTest.jsp" target="parent"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.eat"/></A></li>
				<li><A HREF="qaAdminQueue.jsp" target="parent"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.queue"/></A></li>
	          </ul>
	        </li>
			<% } %>
		<% } %>
		<li class="dropdown">
          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.system.management"/> <span class="caret"></span></a>
          <ul class="dropdown-menu">
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) != 0) {
			%>
			<li><A class="system-button" HREF="white" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="white.management"/></A></li>
			<li><A class="system-button" HREF="black" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="black.management"/></A></li>
			<%
			}
			%>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) != 0) {
			%>
			<li><A class="system-button" HREF="recommend" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="recommend.manage"/></A></li>
            <li role="separator" class="divider"></li>
			<li><A class="system-button" HREF="broadcast.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="system.boardcast"/></A></li>
			<li><A class="system-button" HREF="wiseSystemConfig.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="wisesystemconfig.set"/></A></li>
			<li><A class="system-button" HREF="wiseReplicationConfig.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;Master/Slave</A></li>
			<li><A class="system-button" HREF="column-name-mgt.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="column.name.mgt.set"/></A></li>
            <li role="separator" class="divider"></li>
			<li><A class="layout-0-10" HREF="jobs/job-schedule.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="job.schedule.manage"/></A></li>
			<li><A class="layout-0-10" HREF="jobs/job-schedule-others.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="job.schedule.others.manage"/></A></li>
			<%
			}
			%>
			<% if (SearchManager.isCloudMode() && AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0) { %>
            <li role="separator" class="divider"></li>
			<li><A class="layout-0-10" HREF="collectionsAdmin.jsp" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="collectionsAdmin.manage"/></A></li>
			<% } %>
          </ul>
        </li>
		<% if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0
				|| ("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) { %>
			<li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="top.authority.management"/> <span class="caret"></span></a>
	          <ul class="dropdown-menu">
				<li><A target="mainFrame" class="admin-button" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/tenantAdmin.jsp"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="tenant.admin"/></A></li>
            <li role="separator" class="divider"></li>
				<li><A target="mainFrame" class="admin-button" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/userAdmin.jsp"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="user.admin"/></A></li>
				<li><A target="mainFrame" class="admin-button" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/groupAdmin.jsp"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="group.management"/></A></li>
				<li><A target="mainFrame" class="admin-button" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/locationAdmin.jsp"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="address.management"/></A></li>
            <li role="separator" class="divider"></li>
				<li><A target="mainFrame" class="admin-button" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/endUserAdmin.jsp"><span class="glyphicon glyphicon-lock"></span>&nbsp;<bean:message key="end.user.management"/></A></li>
	          </ul>
	        </li>
		<% } %>
		<li class="dropdown">
          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="other.menu"/> <span class="caret"></span></a>
          <ul class="dropdown-menu">
			<% if (false && AdminGroupFacade.getInstance().getFromSession(session).getCoreAdminCURD() > 0) { %>
				<li><A HREF="cores-admin-menu.jsp" target="leftFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="source.management"/></A></li>
			<% } %>
			<% if (AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() > 0) { %>
				<li><A HREF="dataset-admin-menu.jsp" target="leftFrame"><span class="glyphicon glyphicon-list"></span>&nbsp;<bean:message key="top.data.management"/></A></li>
			<% } %>
			<% if (AdminGroupFacade.getInstance().getFromSession(session).getStatisticsAdminCURD() > 0) { %>
				<li><A HREF="qa-analytics/s.jsp?v=bar" target="parentFrame"><span class="glyphicon glyphicon-stats"></span>&nbsp;<bean:message key="top.statistical.analysis"/></A></li>
			<% } %>
			<% if (AdminGroupFacade.getInstance().getFromSession(session).getAdphraseAdminCURD() > 0) { %>
				<li><A HREF="ad-admin-menu.jsp" target="leftFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;<bean:message key="top.marketing.management"/></A></li>
			<% } %>
			<% if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() > 0) { %>
				<li><A HREF="wise-admin.html#/" target="mainFrame"><span class="glyphicon glyphicon-th-large"></span>&nbsp;System Status</A></li>
			<% } %>
          </ul>
        </li>
		<%--
		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
			<li><A HREF="file-admin-menu.jsp" target="leftFrame">檔案管理</A></li>
		<% } %>
		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
			<li><A HREF="clickstreams.jsp" target="mainFrame">流量管理</A></li>
		<% } %>
		--%>
      </ul>
      <ul class="nav navbar-nav navbar-right">
		<li><A HREF="../" target="_new"><span class="glyphicon glyphicon-home"></span>&nbsp;<bean:message key="top.reception"/></A></li>
		<li><A HREF="logout.jsp" target="_parent"><span class="glyphicon glyphicon-log-out"></span>&nbsp;<bean:message key="top.logout"/></A></li>
      </ul>
<script>
$('.layout-0-10').click(function(e) {
	$('#menuDiv').removeClass();
	$('#menuDiv').addClass('hide');

	$('#mainDiv').removeClass();
	$('#mainDiv').addClass('col-xs-12');
});
$('.system-button').click(function() {
	top.leftFrame.location = 'system-admin-menu.jsp';
});
$('.admin-button').click(function() {
	top.leftFrame.location = 'admin-admin-menu.jsp';
});
</script>
