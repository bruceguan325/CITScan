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
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
ul.exclude, ul.exclude>li, li.exclude {
	background-color: #888 !important;
}

.fields-available ul, .fields-choosed ul {
	min-height: 100px;
}

.fields-available li, .fields-choosed li {
	margin: 3px;
	padding: 3px;
}

.aciSortablePlaceholder {
	height: 20px;
	border: 1px dashed #333;
}
</style>
</HEAD>
<BODY>
<form name="ff">
<%
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}

String coreName = request.getParameter("coreName");
CoreContainer cc = SearchManager.getLocalCores();
SolrCore core = cc.getCore(coreName);
SolrServer server = SearchManager.getServer(coreName);
SolrIndexSearcher search = core.getSearcher().get();
ArrayList<String> fieldNames = new ArrayList<String>(search.getFieldNames());
Collections.sort(fieldNames);
com.intumit.solr.config.ColumnNameMappingFacade cm = com.intumit.solr.config.ColumnNameMappingFacade.getInstance();
%>
<div class="container">
<div class="row">
	<table class="table table-bordered table-striped">
	<tr>
		<th>可選欄位 (可拖曳到右邊)</th>
		<th>已選欄位 (可拖曳上下調整順序)</th>
	</tr>
	<tr>
	<td class="col-xs-3 fields-available">
		<ul class='list-unstyled'>
			<%
			for (String fn: fieldNames) {
				if (fn.startsWith("_")) continue;
			%>
			<li class='bg-primary' data-fn='<%= fn %>'><%= fn %> <%= cm.getMappingName(fn)==null?"":" ("+cm.getMappingName(fn)+")" %> </li>
			<%
			}
			%>
		</ul>
	</td>
	<td class="col-xs-6 fields-choosed">
		<ul class='list-unstyled'>
		</ul>
	</td>
	</tr>
	</table>
</div>
</div>
</form>
<script>
$(function() {
    $('.fields-available').aciSortable({
        exclude: '.exclude',
        connectDrop: '.fields-choosed'
    });
    $('.fields-choosed').aciSortable({
        exclude: '.exclude',
        connectDrop: '.fields-available',
        placeHolder: '<li class="aciSortablePlaceholder"><div></div></li>',
        valid: function(item, hover, before, isContainer, placeholder, helper) {
            return $.fn.aciSortable.defaults.valid.apply(this, arguments);
        },
        end: function(item, hover, placeholder, helper) {
            $.fn.aciSortable.defaults.end.apply(this, arguments);
            // scroll to the end
            this._instance.jQuery.scrollTop(this._instance.jQuery.get(0).scrollHeight);
        }
    });

});
</script>
</BODY>
</HTML>
