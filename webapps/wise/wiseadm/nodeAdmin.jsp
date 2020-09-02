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
	import="org.apache.solr.common.cloud.*"
	import="org.apache.solr.common.params.*"
	import="org.apache.solr.common.params.CollectionParams.*"
	import="org.apache.commons.lang.StringUtils"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.quartz.ScheduleUtils"
	import="com.intumit.quartz.Job"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.admin.*" %><%!

	String statusToIcon(String status) {
	    if (StringUtils.equalsIgnoreCase("active", status)) {
	    	return "<span class=\"glyphicon glyphicon-thumbs-up\"></span>";
	    }
	    else if (StringUtils.equalsIgnoreCase("recovering", status)) {
	    	return "<span class=\"glyphicon glyphicon-adjust\"></span>";
	    }
	    else if (StringUtils.equalsIgnoreCase("down", status)) {
	    	return "<span class=\"glyphicon glyphicon-remove\"></span>";
	    }
    	return "<span class=\"glyphicon glyphicon-question-sign\"></span>";
	}
%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) {
	return;
}

if (!SearchManager.isCloudMode()) {
	return;
}
%>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap-theme.min.css" type="text/css" rel="stylesheet"/>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js" type="text/javascript"></script>
<script>
</script>
<style>
.active { color: GREEN; }
.recovering { color: ORANGE; }
.down { color: RED; }
</style>
</HEAD>
<BODY>
<%
String action = request.getParameter("action");
String nodeName = request.getParameter("n");
String type = request.getParameter("t");

%>
<H1>目前節點：<%= nodeName %><a onclick='javascript:location.reload()'><span class='glyphicon glyphicon-refresh'></span></a></H1>
	<%
	ClusterState clusterState = SearchManager.getClusterState();
	CollectionsAdminUtils cautil = new CollectionsAdminUtils(clusterState);
	int no = 1;

	Collection<String> collNames = new ArrayList<String>();
    collNames.addAll(SearchManager.getAllCollectionNames());

	for (String collectionName: collNames) {
	    Map<Slice, Collection<Replica>> detailMap = cautil.getDetailOfNode(collectionName, nodeName);

	    out.println("<h2>叢集：" + collectionName + "</h2>");
		%>
		<TABLE width="90%" class="table table-striped table-hover">
		<THEAD>
		<TR>
			<TH valign="top">Shard</TH>
			<TH valign="top">Replicas</TH>
		</TR>
		</THEAD>
		<%
		for (Slice slice: detailMap.keySet()) {
		    Collection<Replica> replicasOnThisNode = detailMap.get(slice);
			out.println("<tr>");
			out.println("<td>");
			out.println("<h4>");
			out.println("<span class=" + slice.getState() + ">" + slice.getName() + "</span>");

			if (replicasOnThisNode.size() == 0) {
				out.println("<a class='btn btn-default collection-btn' data-loading-text='執行中' coll='" + collectionName + "' action='" + CollectionAction.ADDREPLICA.name() + "' shard='" + slice.getName() + "' node='" + nodeName + "'>加入本節點</a>");
			}
			out.println("</h4>");
			out.println("</td>");


			out.println("<td>");
			for (Replica replica: replicasOnThisNode) {
				out.println("<div class='row'>");
				String replicaState = replica.getStr("state");
				String replicaCoreName = replica.getStr("core");
				URL baseUrl = new URL(replica.getStr("base_url"));
				out.println("<div class='col-md-2'><span class=" + replicaState + ">" + statusToIcon(replicaState) + "</span>&nbsp;" + replica.getName() + "</div>");
				out.println("<div class='col-md-3'>" + replicaCoreName + "</div>");
				out.println("<div class='col-md-4'>");
				out.println("<a class='btn btn-primary core-btn' data-loading-text='執行中...' coll='" + collectionName + "' action='RELOAD' shard='" + slice.getName() + "' replica='" + replica.getName() + "'>RELOAD</a>");
				out.println("<a class='btn btn-danger collection-btn' data-loading-text='刪除中...' coll='" + collectionName + "' action='" + CollectionAction.DELETEREPLICA.name() + "' shard='" + slice.getName() + "' replica='" + replica.getName() + "'>DEL</a>");
				out.println("</div>");
				out.println("</div>");
			}
			out.println("</td>");
			out.println("</tr>");
		}

		%>
		</TABLE>
	<%
	}
	%>
<script>
$('.core-btn').click(function() {
	var coll = $(this).attr('coll');
	var action = $(this).attr('action');
	var shard = $(this).attr('shard');
	var node = $(this).attr('node');
	var replica = $(this).attr('replica');
	var thisBtn = $(this);

	if (confirm('Are you sure to delete this replica?')) {
		thisBtn.button('loading');

		$.getJSON('collectionsAdminAjax.jsp',
				{type: 'core', collection: coll, action: action, shard: shard, replica: replica, node: node},
				function(data) {

					if (typeof(data.error) != 'undefined') {
						alert("執行失敗：" + data.errorMessage);
						thisBtn.button('reset');
					}
					if (typeof(data.success) != 'undefined' && data.success) {
						//alert("成功刪除 Replica[" + replica + "] on Shard[" + shard + "]");
						thisBtn.parents('.row').remove();
					}
				});
    }
});
$('.collection-btn').click(function() {
	var coll = $(this).attr('coll');
	var action = $(this).attr('action');
	var shard = $(this).attr('shard');
	var node = $(this).attr('node');
	var replica = $(this).attr('replica');
	var thisBtn = $(this);

	if (confirm('Are you sure to delete this replica [' + replica + ']?')) {
		thisBtn.button('loading');

		$.getJSON('collectionsAdminAjax.jsp',
				{type: 'collection', collection: coll, action: action, shard: shard, replica: replica, node: node},
				function(data) {

					if (typeof(data.error) != 'undefined') {
						alert("執行失敗：" + data.errorMessage);
						thisBtn.button('reset');
					}
					if (typeof(data.success) != 'undefined' && data.success) {
						//alert("成功刪除 Replica[" + replica + "] on Shard[" + shard + "]");
						location.reload();
					}
				});
    }
});
</script>
</BODY>
</HTML>
