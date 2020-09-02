<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page isELIgnored ="false" %>

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
<%@ page import="com.intumit.solr.dataset.*" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>

<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='recommendation.management'/></TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jquery.bgiframe.pack.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jmesa.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/jquery.jmesa.js"></script>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/css/jmesa.css" />
<script type="text/javascript">
function onInvokeAction(id) {
    setExportToLimit(id, '');

    var parameterString = createParameterStringForLimit(id);
    $.get('${pageContext.request.contextPath}<%= WiSeEnv.getAdminContextPath() %>/recommend/list?ajax=true&' + parameterString, function(data) {
        $("#fr").html(data)
    });

}


function select (id,keyword,os,sort,url,target) {
	document.forms[0].hiddenId.value = id;
	document.forms[0].keyword.value = keyword;
	document.forms[0].sort.value = sort;
	document.forms[0].url.value = url;
	document.forms[0].os.value = os;
	document.forms[0].target.value = target;

	document.forms[0].submitForm.disabled = true;
	document.forms[0].update.disabled = false;
}

function updateKeyword() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/recommend/update";
	document.forms[0].submit();
}

function cancelButton() {
	document.forms[0].submitForm.disabled = false;
	document.forms[0].update.disabled = true;
	document.forms[0].hiddenId.value = '';
	document.forms[0].keyword.value = '';
	document.forms[0].sort.value = '';
	document.forms[0].os.value = '';
	document.forms[0].url.value = '';
	document.forms[0].target.value = '';
}

function searchKeyword(keyword) {
	document.forms[0].action = "<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/recommend/search";
	document.forms[0].submit();
}

function setFieldOs(osVal) {
	document.forms[0].os.value = osVal;
}


</script>
</HEAD>
<BODY>


<div id="fr">

<form id="recommendForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/recommend/save" method="post">
<input type="hidden" name="hiddenId">
<table>
<tr>
	<td width="150"><bean:message key='recommend.hot.word'/>：</td>
	<td width="450"><input type="text" name="keyword" size="30"></td>
</tr>

<tr>
	<td><bean:message key='global.channel'/>：</td>
	<td><input type="text" name="os" size="30"></td>
</tr>

<tr>
  <td colspan="2">
(<a onclick="setFieldOs('-1');"><bean:message key='global.channel.ex'/>
<%
List<DataSet> list = DataSetFacade.getInstance().listAll();
for (int i=0; i < list.size(); i++) {
	DataSet ds = list.get(i);
	int id = ds.getId();
	String name = ds.getName();
	
	%>
	<a onclick="setFieldOs('<%= id %>');"><u><%= name %>:<%= id %></u></a>&nbsp;
	<%
}
%>)
  </td>
</tr>

<tr>
	<td><bean:message key='global.ranking'/>：</td>
	<td><input type="text" name="sort" size="30"></td>
</tr>

<tr>
	<td><bean:message key='global.ranking'/><bean:message key='link.url'/>：</td>
	<td><input type="text" name="url" size="30"></td>
</tr>
<tr>
	<td><bean:message key='link.url'/><bean:message key='web.page.open.target'/>：</td>
	<td><input type="text" name="target" size="30"></td>
</tr>
<tr>
  <td colspan="2">
<bean:message key='web.page.open.target.ex'/>
  </td>
</tr>

</table>
<table>
<tr>
		<td><input id="submitForm" type="submit" value="<bean:message key='global.add'/>"></td>
		<td><input id="update" type="button" value="<bean:message key='modify'/>" disabled="true" onClick="javascript:updateKeyword();"></td>
		<td><input id="cancel" type="button" value="<bean:message key='global.cancel'/>" onClick="javascript:cancelButton();"></td>
</tr>
</table>
</form>
<br>

<form id="searchForm" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/recommend/search" method="post">
<table>
<tr>
	<td><bean:message key='query.words'/></td>
	<td>
		<input type="text" name="searchKeyword">
		<input id="submitSearchForm" type="submit" value="<bean:message key='query'/>" onClick="javascript:searchKeyword();">
	</td>
</tr>
</table>
</form>




<%
    out.println(request.getAttribute("myhtml"));
%>
</div>

</form>

</BODY>
</HTML>
