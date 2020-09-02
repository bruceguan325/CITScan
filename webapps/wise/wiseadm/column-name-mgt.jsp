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
<%@ page import="org.apache.lucene.index.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.request.*" %>
<%@ page import="org.apache.solr.search.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.config.ColumnNameMappingFacade" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
%>Access Denied<%
	return;
}
%>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<script>
</script>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
</HEAD>
<BODY>

<TABLE width="100%" class="table table-bordered">
<%
	if (request.getParameterMap().containsKey("key")) {
	String[] reqKeys = request.getParameterValues("key");
	String[] reqVals = request.getParameterValues("value");

	com.intumit.solr.config.ColumnNameMappingFacade cm = com.intumit.solr.config.ColumnNameMappingFacade.getInstance();

	Map<String,String> columns = new HashMap<String,String>();
	for(int i = reqKeys.length-1;i>=0;i--){
		columns.put(reqKeys[i],StringUtils.isEmpty(reqVals[i])?null:reqVals[i]);
	}
	if(columns.size() > 0){
		cm.saveMap(columns);
	}
	String[] clear = request.getParameterValues("clear");
	if(clear != null){
		for (String clearField:clear) {
			if(StringUtils.isNotBlank(clearField)){
				cm.delete(clearField);
			}
		}
	}
%>
		<tr><td><bean:message key='modify'/><bean:message key='global.success'/></td></tr>
		<%
}
%>
<TR>
<TD width="100%" valign="top">
	<%
		Map<String,String> columns = new TreeMap<String, String>();
		Map<String,String> idxColumns = SearchManager.getColumnNames();
		columns.putAll(ColumnNameMappingFacade.getInstance().getColumnMappingMap());
		
		org.restlet.engine.util.CaseInsensitiveHashSet names = new org.restlet.engine.util.CaseInsensitiveHashSet(columns.keySet());
		
		for (Map.Entry<String, String> entry: idxColumns.entrySet()) {
			if (!names.contains(entry.getKey())) {
				columns.put(entry.getKey(), entry.getValue());
				names.add(entry.getKey());
			}
		}
	%>
<bean:message key='manual.increase'/>：<BR>
<form method="GET" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/column-name-mgt.jsp">
	<TABLE width="100%">
		<tr>
			<th width="30%"><bean:message key='column'/>：<input type="text" value="" name="key" /></th>
			<th width="50%"><bean:message key='global.name'/>：<input type="text" size="70%" maxlength="16" name="value" value=""/></th>
			<th width="20%"><input type="submit" value="<bean:message key='manual.new.add'/>"></th>
		</tr>
	</TABLE>
</form>
<bean:message key='have.column'/><BR>
<form method="POST" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/column-name-mgt.jsp">
	<TABLE width="100%" class="table table-striped table-bordered">
		<tr>
			<th width="30%"><bean:message key='column'/></th>
			<th width="70%"><bean:message key='global.name'/></th>
		</tr>
		<%
			String name;
			for(String key:columns.keySet()){
				name = columns.get(key)==null?"":columns.get(key);
		%>
		<tr>
			<td><%= key %></td>
			<td>
				<input type="hidden" value='<%= key %>' name="key" />
				<input type="text" size="70%" maxlength="64" name="value" value='<%= name %>'>
				&nbsp;<bean:message key='global.eliminate'/><input type='checkbox' name='clear' value='<%= key %>' >
			</td>
		</tr>
		<%
			}
		%>
		<tr>
			<td width="30%"></td>
			<td width="70%"><input type="submit" class="btn btn-default" value="<bean:message key='submit'/>"></td>
		</tr>
	</TABLE>
</form>
</TD>
</TR>
<TR>
<TD width="100%" valign="top">
</TD>
</TR>
</TABLE>
</BODY>
</HTML>
