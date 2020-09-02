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
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script>
function doAdmin(core, action) {
	if (action == 1) {
		if (confirm('確定要進行 Delta-import？')) 
		if (confirm('真的確定要進行 Delta-import？')) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=delta-import&optimize=false';
		}
	}
	else if (action == 2) {
		if (confirm('確定要進行 Full-import？')) 
		if (confirm('真的確定要進行 Full-import？')) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=full-import&commit=false&optimize=false&clean=true';
		}
	}
	else if (action == 3) {
		if (confirm('確定要進行 SMS？')) 
		if (confirm('真的確定要進行 SMS？')) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/sms?command=commit&coreName=' + core + '';
		}
	}
	return false;
}

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



function doReloadAhoCorasick() {
		if (confirm('確定要Reload AhoCorasick？')) 
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/acimport?command=reload';
	return false;
}



</script>
<style>
html,body{ 
background:url(nothing.txt) white fixed; /* prevent screen flash in IE6 */
margin:0; padding:0;
overflow: auto;
}

iframe {
height: 1024px;
width: 100%;
position:fixed; 
top:0;
z-index:1;
_position:absolute; /* position fixed for IE6 */
_top: expression(eval(document.compatMode &&
            document.compatMode=='CSS1Compat') ?
            documentElement.scrollTop+15 :
            document.body.scrollTop );

}
</style>
</HEAD>
<BODY>
<CENTER>
<strong>排程管理</strong>
<TABLE>
<TR><TD width="50%" valign="top">
	<TABLE>
		<%
		List<String> allCoreNames = SearchManager.getAllCoreNames();
		for (int iii=0; iii < allCoreNames.size(); iii++) {
			String coreName = allCoreNames.get(iii);
			%>
			<TR><TD>
				<A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/coresAdmin.jsp?coreName=<%= coreName %>&t=schedule"><%= coreName %></A>
			</TD></TR>
			<%
		}
		%>
	</TABLE>
</TD>
</TR>
</TABLE>
</CENTER>
<script>
window.parent.mainFrame.location="Blank.html";
</script>
</BODY>
</HTML>
