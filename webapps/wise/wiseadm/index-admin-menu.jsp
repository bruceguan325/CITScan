<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
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
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>

<script>
function doAdmin(core, action) {
	if (action == 1) {
		if (confirm("<bean:message key='delta.import'/>"))
		if (confirm("<bean:message key='sure.delta.import'/>")) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=delta-import&optimize=false';
		}
	}
	else if (action == 2) {
		if (confirm("<bean:message key='full.import'/>"))
		if (confirm("<bean:message key='sure.full.import'/>")) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=full-import&commit=false&optimize=false&clean=true';
		}
	}
	else if (action == 3) {
		if (confirm("<bean:message key='sms'/>"))
		if (confirm("<bean:message key='sure.sms'/>")) {
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

</script>
<style>
html, body {
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
<h3><bean:message key="top.solr.management"/></h3>
<ul class="list-unstyled">
<%
Set<String> allCoreNames = new TreeSet<String>(new Comparator() {

	public int compare(Object o1, Object o2) {
		String s1 = (String)o1;
		String s2 = (String)o2;

		Pattern p = Pattern.compile("core([0-9]+)");
		Matcher m1 = p.matcher(s1);
		Matcher m2 = p.matcher(s2);

		if (m1.find() && m2.find()) {
			return new Integer(m1.group(1)).compareTo(new Integer(m2.group(1)));
		}
		else
			return s1.compareTo(s2);
	}

});

allCoreNames.addAll(SearchManager.getAllCoreNames());
Set<String> showAtBottom = new TreeSet<String>();

for (String coreName : allCoreNames) {
	if (coreName.contains("-")) {
		showAtBottom.add(coreName);
		continue;
	}

	%>
	<li>
		<A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/indexAdmin.jsp?coreName=<%= coreName %>"><%= coreName %></A>
	</li>
	<%
}
%>
</ul>
<hr/>
<ul class="list-unstyled">
	<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/coresAdmin.jsp?action=create"><bean:message key="create.new.index"/></A></li>
	<li><A HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/statistics-field-menu.jsp"><bean:message key="data.field.statistics"/></A></li>
</ul>
<hr/>
<ul class="list-unstyled">
<%
for (String coreName : showAtBottom) {
	%>
	<li>
		<A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/indexAdmin.jsp?coreName=<%= coreName %>"><%= coreName %></A>
	</li>
	<%
}
%>
</ul>
</CENTER>
<script>
window.parent.mainFrame.location="coresAdmin.jsp";
</script>
</BODY>
</HTML>
