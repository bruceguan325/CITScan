<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.solr.common.*"
import="org.apache.solr.client.solrj.*"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%! 
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<title>QA DEBUG</title>
<jsp:include page="header-qa.jsp"></jsp:include>
<style>
td {
word-wrap: break-word;
}
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container-fluid">
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String qaId = request.getParameter("id");
QAContext qaCtx = QAContextManager.lookup(qaId);
%>
<%= com.intumit.solr.util.WiSeUtils.nl2br(qaCtx.explainPath()) %>
<hr/>
<table class='table table-strip table-bordered' style="table-layout: fixed; width: 99%;">
<thead>
	<tr>
	<th style="width:80px;">No</th>
	<th class="col-md-1">Status</th>
	<th class="col-md-1">Detail Class</th>
	<th>Detail</th>
	</tr>
</thead>
<%
List<Object[]> logs = qaCtx.getExplain();

List<QAPattern> patterns = QAPattern.list(t.getId());
int no = 1;

for (Object[] log: logs) {
	Object val = log[2];
	Long elasped = (Long)log[3];
%>
<tr>
	<td>
		<%= no++ %><br>
		<%= elasped %>ms
	</td>
	<td><%= log[0] %> / <%= log[1] %></td>
	<td><%= val != null ? val.getClass().getName() : "null" %></td>
	<td>
	<%
	if (val != null) {
		if (val instanceof SolrDocumentList) {
			Object[] fieldVals = ((SolrDocumentList)val).toArray();
			
			for (Object o: fieldVals) {
				out.println(o);
				out.println("<HR/>");
			}
		}
		else if (val instanceof SolrDocument) {
			SolrDocument doc = ((SolrDocument)val);
			String[] fieldNames = doc.getFieldNames().toArray(new String[0]);
			for (String fn: fieldNames) {
				out.println(fn);
				out.println(doc.getFieldValues(fn));
				out.println("<BR>");
			}
			
		}
		else if (val instanceof SolrQuery) {
			SolrQuery sq = ((SolrQuery)val);
			String[] fieldNames = sq.getParameterNames().toArray(new String[0]);
			for (String fn: fieldNames) {
				out.println(fn);
				out.println(Arrays.asList(sq.getParams(fn)));
				out.println("<BR>");
			}
			out.println("<HR/>");
			out.println(sq);
		}
		else {
			out.println(val);
		}
	}
	else {
		out.println("");
	}
	%>
	</td>
</tr>
<%
}
%>
</table>
</div>
</body>
</html>