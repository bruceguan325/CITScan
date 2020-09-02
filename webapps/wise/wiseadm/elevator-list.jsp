<%@ include file="/commons/taglib.jsp"%>
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
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
if (admGrp == null || admGrp.getDataSetAdminCURD() == 0) {
	return;
}
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
%>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<script>
function fillCommand(cmd, core) {
	var myForm = document.getElementById('cmd' + core);
	if (cmd == 'COMMIT') {
		myForm.COMMAND.value = '<commit/>';
	}
	else if (cmd == 'OPTIMIZE') {
		myForm.COMMAND.value = '<optimize/>';
	}
	else if (cmd == 'ROLLBACK') {
		myForm.COMMAND.value = '<rollback/>';
	}
	else if (cmd == 'DBI') {
		myForm.COMMAND.value = '<delete><id></id></delete>';
	}
	else if (cmd == 'DBQ') {
		myForm.COMMAND.value = '<delete><query></query></delete>';
	}
}

function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

</script>
<link href="../styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
</HEAD>
<BODY>
<%
String dsId = request.getParameter("d");
String esId = request.getParameter("id");
String action = request.getParameter("action");

int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, Integer.parseInt(dsId));
if ((acl & GroupDataSet.O4) == 0) return; // 無權限

if ("create".equalsIgnoreCase(action)) {
	ElevatorSet newES = new ElevatorSet();
	newES.setDsId(dsId);
	newES.setQuery(request.getParameter("q"));
	newES.setTimestamp(new Date());
	newES.setCreator(AdminUserFacade.getInstance().getFromSession(session).getLoginName());
	ElevatorSetFacade.getInstance().saveOrUpdate(newES);
	%>
	created!
	<%
}
else if ("delete".equalsIgnoreCase(action)) {
	ElevatorSetFacade.getInstance().delete(esId, dsId);
}

%>
<TABLE width="100%" class="table table-striped">
	<TR>
		<TH valign="top"><bean:message key='item'/></TH>
		<TH valign="top"><bean:message key='data.group'/><bean:message key='num'/></TH>
		<TH valign="top"><bean:message key='global.condition'/></TH>
		<TH valign="top"><bean:message key='top.number'/></TH>
		<TH valign="top"><bean:message key='excluded.quantity'/></TH>
		<TH valign="top"><bean:message key='operation'/></TH>
	</TR>
<%
   
    List<ElevatorSet> list = null;

	if (user.getLoginName().equals("admin") || user.toAdminGroupCollection().iterator().next().getId() == 1) {
		list = ElevatorSetFacade.getInstance().listAll();
	}
	else {
		list = ElevatorSetFacade.getInstance().listAll(dsId);
	}
    int counter = 1;
    
    for (ElevatorSet eSet: list) {
%>
	<TR>
		<TD align="center" valign="top"><%= counter++ %></TD>
		<TD align="center" valign="top"><%= eSet.getDsId() %></TD>
		<TD align="center" valign="top"><%= eSet.getQuery() %></TD>
		<TD align="center" valign="top"><%= eSet.getFixedAtTopAsList().size() %></TD>
		<TD align="center" valign="top"><%= eSet.getHiddenList().size() %></TD>
		<TD align="center" valign="top">
			<a class="btn btn-primary" href="elevator-search.jsp?d=<%=dsId%>&id=<%= eSet.getId() %>">EDIT</a>
			<a class="btn btn-primary" href="elevator-list.jsp?d=<%=dsId%>&id=<%= eSet.getId() %>&action=disable">DISABLE</a>
			<a class="btn btn-danger" href="elevator-list.jsp?d=<%=dsId%>&id=<%= eSet.getId() %>&action=delete">DELETE</a>

		</TD>
	</TR>
<% 	} %>
<%
//}
%>
</TABLE>
<form action="elevator-list.jsp" class="well form-vertical">
<label>Create New One:</label>
<input type="hidden" name="d" value="<%= dsId %>">
<input type="hidden" name="action" value="create">
<input class="input-medium" style="height:2em;" placeholder="Enter Query" type="text" name="q"><br/>
<button type="submit" class="btn"><bean:message key='global.create'/></button>
</form>
</BODY>
</HTML>
