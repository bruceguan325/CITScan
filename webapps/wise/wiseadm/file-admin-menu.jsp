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
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script>
function doReloadAhoCorasick() {
		if (confirm('確定要Reload AhoCorasick？'))
			window.parent.mainFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/acimport?command=reload';
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
<h3>檔案管理</h3>
<ul class="unstyled">
	<li><A HREF="FileSpiderManager" target="mainFrame">非結構化檔案管理</A></li>
	<li><A HREF="mediaManager" target="mainFrame">多媒體檔案管理</A></li>
</ul>
</CENTER>
</BODY>
</HTML>
