<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="javax.servlet.ServletConfig" 
	import="javax.servlet.ServletException" 
	import="javax.servlet.http.*" 
	import="java.io.*" 
	import="java.net.*" 
	import="java.text.*" 
	import="java.util.*" 
	import="org.apache.commons.io.*" 
	import="org.apache.commons.httpclient.*" 
	import="org.apache.commons.httpclient.methods.*" 
	import="org.apache.commons.httpclient.params.HttpMethodParams" 
	import="org.apache.solr.core.*" 
	import="org.apache.solr.servlet.*" 
	import="org.apache.solr.client.solrj.*" 
	import="org.apache.solr.client.solrj.embedded.*" 
	import="org.apache.solr.client.solrj.request.*" 
	import="org.apache.solr.client.solrj.response.*" 
	import="org.apache.solr.cloud.*" 
	import="org.apache.solr.common.*" 
	import="org.apache.solr.common.params.*" 
	import="org.apache.solr.common.cloud.*" 
	import="org.apache.commons.lang.StringUtils"
	import="org.apache.wink.json4j.*" 
	import="com.intumit.solr.SearchManager" 
	import="com.intumit.quartz.ScheduleUtils"
	import="com.intumit.quartz.Job"
	import="com.intumit.solr.util.*" 
	import="com.intumit.solr.admin.*" %>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script>
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
<h3>叢集</h3>
<ul class="unstyled">
<%

ClusterState clusterState = SearchManager.getClusterState();
CollectionsAdminUtils cautil = new CollectionsAdminUtils(clusterState);
Collection<String> collNames = cautil.listCollectionNames();

for (String collName: collNames) {
%>
<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/collectionsAdmin.jsp?cn=<%= collName %>"><%= collName %></A></li>
<%
}
%>
<li> </li>
<li> </li>
<li> </li>
<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/coresAdmin.jsp?action=create">建立新的叢集</A></li>
</ul>
<BR>
<h3>節點</h3>
<ul class="unstyled">
<%
Collection<String> liveNodes = clusterState.getLiveNodes();

for (String node: liveNodes) {
%>
<li><A target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/nodeAdmin.jsp?n=<%= java.net.URLEncoder.encode(node, "UTF-8") %>"><%= node %></A></li>
<%
}
%>
<li> </li>
</ul>
</CENTER>
<script>
window.parent.mainFrame.location="Blank.html";
</script>
</BODY>
</HTML>
