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

void createCollection(String collectionName, int numShards,
			int numReplicas, int maxShardsPerNode, SolrServer client, String createNodeSetStr)
			throws SolrServerException, IOException {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("action", CollectionParams.CollectionAction.CREATE.toString());
		params.set(OverseerCollectionProcessor.NUM_SLICES, numShards);
		params.set(OverseerCollectionProcessor.REPLICATION_FACTOR, numReplicas);
		params.set(OverseerCollectionProcessor.MAX_SHARDS_PER_NODE, maxShardsPerNode);
		if (createNodeSetStr != null) params.set(OverseerCollectionProcessor.CREATE_NODE_SET, createNodeSetStr);

		params.set("name", collectionName);
		SolrRequest request = new QueryRequest(params);
		request.setPath("/admin/collections");

		client.request(request);
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
String targetCollection = request.getParameter("cn");
String type = request.getParameter("t");

if ("create".equalsIgnoreCase(action)) {
	SolrServer client = SearchManager.getServer();
    int numLiveNodes = SearchManager.getClusterState().getLiveNodes().size();
    List<String> createNodeList = null;
    int numOfCreateNodes = numLiveNodes/2;

    if (numOfCreateNodes > 0) {
	    int i = 0;
	    createNodeList = new ArrayList<String>();

	    for (String liveNode : SearchManager.getClusterState().getLiveNodes()) {
	      if (i < numOfCreateNodes) {
	        createNodeList.add(liveNode);
	        i++;
	      } else {
	        break;
	      }
	    }
    }

    int maxShardsPerNode = 2;
    int numShards = 2;
    int replicationFactor = 1;

    createCollection(targetCollection, numShards, replicationFactor, maxShardsPerNode, client, null);//StringUtils.join(createNodeList, ','));
	%>
    <p><strong><%=targetCollection%></strong> created!</p>
<%
}
else if ("reload".equalsIgnoreCase(action)) {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("action", CollectionParams.CollectionAction.RELOAD.toString());
    params.set("name", targetCollection);
    QueryRequest qr = new QueryRequest(params);
    qr.setPath("/admin/collections");

    SearchManager.getServer(targetCollection).request(qr);
	%>
	<%=targetCollection%> reloaded!
	<%
}
else if ("unload".equalsIgnoreCase(action)) {
	CoreContainer cc = SearchManager.getLocalCores();
    //SolrCore core = cc.remove(targetCollection);
    //core.close();
	%>
	<%=targetCollection%> removing function currently not implemented at Solr4.10!
	<%
}
%>
<TABLE width="100%" class="table table-striped">
	<THEAD>
	<TR>
		<TH valign="top">項次</TH>
		<TH valign="top">Colleciton Name</TH>
		<TH valign="top">Shards / Replicas</TH>
		<TH valign="top">操作</TH>
	</TR>
	</THEAD>
	<%
	ClusterState clusterState = SearchManager.getClusterState();
	Set<String> liveNodes = new HashSet<String>(clusterState.getLiveNodes());
	Set<String> idleNodes = new HashSet<String>(clusterState.getLiveNodes());
	int no = 1;

	Collection<String> collNames = new ArrayList<String>();
	if (StringUtils.isNotEmpty(targetCollection)) {
	    collNames.add(targetCollection);
	}
	else {
	    collNames.addAll(SearchManager.getAllCollectionNames());
	}

	for (String collectionName: collNames) {
		Collection<Slice> activeSlices = clusterState.getActiveSlices(collectionName);
		Map<String, Object> props = clusterState.getCollection(collectionName).getProperties();
		%>
		<TR>
			<TD align="center" valign="top"><%= no++ %></TD>
			<TD align="center" valign="top" style="width: 100px;"><h3><%= collectionName %></h3></TD>
			<TD align="left" valign="top">
				<%
				for (Slice slice: activeSlices) {
					out.println("<h4><span class=" + slice.getState() + ">" + slice.getName() + "</span></h4>");

					for (Replica replica: slice.getReplicas()) {
						idleNodes.remove(replica.getNodeName());
						out.println("<div class='row'>");
						String replicaState = replica.getStr("state");
						String replicaCoreName = replica.getStr("core");
						URL baseUrl = new URL(replica.getStr("base_url"));
						out.println("<div class='col-md-1'><span class=" + replicaState + ">(" + replicaState + ")</span></div>");
						out.println("<div class='col-md-4'>" + baseUrl.getHost() + " / " + replicaCoreName + " / " + replica.getName() + "</div>");
						out.println("<div class='col-md-4'>");
						out.println("<a class='btn btn-primary core-btn' data-loading-text='執行中...' coll='" + collectionName + "' action='RELOAD' shard='" + slice.getName() + "' replica='" + replica.getName() + "'>RELOAD</a>");
						out.println("<a class='btn btn-danger collection-btn' data-loading-text='刪除中...' coll='" + collectionName + "' action='" + CollectionAction.DELETEREPLICA.name() + "' shard='" + slice.getName() + "' replica='" + replica.getName() + "'>DEL</a>");
						out.println("</div>");
						out.println("</div>");
					}
					out.println("<hr>");
				}

				%>
			</TD>
			<TD align="center" valign="top">
				<a class="btn btn-primary" onclick="return confirm('確定要 RELOAD？');" href="collectionsAdmin.jsp?action=RELOAD&cn=<%= collectionName %>">RELOAD</a>
				<!--a class="btn btn-danger" onclick="return confirm('確定要刪除此資料叢集？');" href="collectionsAdmin.jsp?action=UNLOAD&cn=<%= collectionName %>">DELETE</a-->
			</TD>
		</TR>
	<%
	}
	%>
		<TR>
			<TD align="center" valign="top"><%= no++ %></TD>
			<TD align="center" valign="top"><h3>現存節點</h3></TD>
			<TD align="left" valign="top">
			<%
			for (String liveNode: liveNodes) {
			%>
			<%= liveNode %><BR>
			<%
			}
			%>
			</TD>
			<TD align="center" valign="top">
			</TD>
		</TR>
		<TR>
			<TD align="center" valign="top"><%= no++ %></TD>
			<TD align="center" valign="top"><h3>閒置節點</h3></TD>
			<TD align="left" valign="top">
			<%
			for (String idleNode: idleNodes) {
			%>
			<%= idleNode %><BR>
			<%
			}
			%>
			</TD>
			<TD align="center" valign="top">
			</TD>
		</TR>
</TABLE>
<script>
$('.core-btn').click(function() {
	var coll = $(this).attr('coll');
	var action = $(this).attr('action');
	var shard = $(this).attr('shard');
	var replica = $(this).attr('replica');
	var thisBtn = $(this);

	if (confirm('Are you sure to delete this replica?')) {
		thisBtn.button('loading');

		$.getJSON('collectionsAdminAjax.jsp',
				{type: 'core', collection: coll, action: action, shard: shard, replica: replica},
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
	var replica = $(this).attr('replica');
	var thisBtn = $(this);

	if (confirm('Are you sure to delete this replica?')) {
		thisBtn.button('loading');

		$.getJSON('collectionsAdminAjax.jsp',
				{type: 'collection', collection: coll, action: action, shard: shard, replica: replica},
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
</script>
<form action="collectionsAdmin.jsp" class="well form-vertical">
<label>Create New One:</label>
<input type="hidden" name="action" value="create">
<input class="input-medium" style="height:2em;" placeholder="Enter Collection Name" type="text" name="cn"><br/>
<button type="submit" class="btn">建立</button>
</form>
</BODY>
</HTML>
