<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
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
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.dataset.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<style>
html, body{ 
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
<form action="<%= WiSeEnv.getAdminContextPath() %>/datasetAdmin.jsp" method="get" name="datasetForm">
	<input name="action" type="hidden" value="create">
<CENTER>
<h3><bean:message key='top.data.management'/></h3>
<ul class="unstyled">
	<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.C) != 0) {
	%>
		<%
		int coreMax=Integer.parseInt(StringUtils.defaultString(System.getProperty("searchresult_coremax"), "10"));
		List<DataSet> list = DataSetFacade.getInstance().listAll();
		
		
		for (DataSet ds: list) {
			%>
			<li><A target="mainFrame" href="<%=request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/datasetAdmin.jsp?action=EDIT&id=<%= ds.getId() %>"><%= ds.getName() %></A>
			<%
		}
	}
	%>
</ul>
</CENTER>
<script>
window.parent.mainFrame.location="datasetAdmin.jsp";
</script>
<script type="text/javascript">

function insertDataSet(url){
	document.datasetForm.submit();
	location.reload(true);		      
}
</script>
</form>
</BODY>
</HTML>
