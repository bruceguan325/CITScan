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
	import="org.json.*" 
	import="com.intumit.solr.SearchManager" 
	import="com.intumit.quartz.ScheduleUtils"
	import="com.intumit.quartz.Job"
	import="com.intumit.solr.util.*" 
	import="com.intumit.solr.admin.*" %><%!
	

void coresAdmin(HttpServletRequest request, JSONObject jresp) {
	String collName = request.getParameter("collection");
	String action = request.getParameter("action");
	String shardName = request.getParameter("shard");
	String replicaName = request.getParameter("replica");
	String replicaCoreName = request.getParameter("replicaCoreName");
	ClusterState clusterState = SearchManager.getClusterState();
	DocCollection coll = clusterState.getCollection(collName);
	Slice slice = coll.getSlice(shardName);
	
	CollectionsAdminUtils cautil = new CollectionsAdminUtils(clusterState);
	if ("RELOAD".equals(action)) {
		try {
			Replica replica = cautil.findReplica(replicaName);
			if (replica == null) {
				jresp.put("error", "true");
				jresp.put("errorMessage", "Replica:[" + replicaName + "] not exist in Shard:[" + shardName + "]");
				
				return;
			}
		
			String url = CollectionsAdminUtils.getBaseUrl(replica) + "/admin/cores?wt=json&action=" + action + "&core=" + replica.get("core");
		
			String jsonStr = getUrlContent(url);
			System.out.println(jsonStr);
			JSONObject json = new JSONObject(jsonStr);
			
			if (json.has("error")) {
				JSONObject error = json.getJSONObject("error");

				jresp.put("error", "true");
				jresp.put("errorMessage", error.getString("msg"));
			}
			else {
				jresp.put("success", "true");
			}
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			try {
				jresp.put("error", "true");
				jresp.put("errorMessage", ex.getMessage());
			}
			catch (Exception jex) {
				jex.printStackTrace();
			};
		}
	}
}


String getUrlContent(String urlStr) throws IOException {
	StringBuilder builder = new StringBuilder();
	URL url = new URL(urlStr);
	URLConnection connection = url.openConnection();
	connection
			.setRequestProperty(
					"User-Agent",
					"Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-TW; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
	connection.setRequestProperty("Content-Type",
			"text/xml; charset:UTF-8;");
	connection.setRequestProperty("Accept-Language",
			"zh-tw,en-us;q=0.7,en;q=0.3");
	connection.setRequestProperty("Accept-Charset", "utf-8");
	
	HttpURLConnection httpConn = (HttpURLConnection)connection;
	InputStream _is;
	if (httpConn.getResponseCode() >= 400) {
	     /* error from server */
	    _is = httpConn.getErrorStream();
	} else {
	    _is = httpConn.getInputStream();
	}

	String line;
	BufferedReader reader = new BufferedReader(new InputStreamReader(_is, "UTF-8"));
	while ((line = reader.readLine()) != null) {
		builder.append(line);
	}
	
	return builder.toString();
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
<%
JSONObject jresp = new JSONObject();

String type = request.getParameter("type");

if ("core".equals(type)) {
	coresAdmin(request, jresp);
}
else if ("collection".equals(type)) {
	String collName = request.getParameter("collection");
	String action = request.getParameter("action");
	String shardName = request.getParameter("shard");
	String nodeName = request.getParameter("node");
	String replicaName = request.getParameter("replica");
	ClusterState clusterState = SearchManager.getClusterState();
	DocCollection coll = clusterState.getCollection(collName);
	Slice slice = coll.getSlice(shardName);
	
	CollectionsAdminUtils cautil = new CollectionsAdminUtils(clusterState);
	
	if (CollectionAction.DELETEREPLICA.name().equals(action)) {
		try {
			JSONObject json = cautil.deleteReplica(collName, shardName, replicaName);
			
			if (json.has("error")) {
				JSONObject error = json.getJSONObject("error");

				jresp.put("error", "true");
				jresp.put("errorMessage", error.getString("msg"));
			}
			else {
				jresp.put("success", "true");
			}
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			try {
				jresp.put("error", "true");
				jresp.put("errorMessage", ex.getMessage());
			}
			catch (Exception jex) {
				jex.printStackTrace();
			};
		}
	}
	else if (CollectionAction.ADDREPLICA.name().equals(action)) {
		try {
			JSONObject json = cautil.addReplica(collName, nodeName, shardName);
			
			if (json.has("error")) {
				JSONObject error = json.getJSONObject("error");

				jresp.put("error", "true");
				jresp.put("errorMessage", error.getString("msg"));
			}
			else {
				jresp.put("success", "true");
			}
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			try {
				jresp.put("error", "true");
				jresp.put("errorMessage", ex.getMessage());
			}
			catch (Exception jex) {
				jex.printStackTrace();
			};
		}
	}
}
%>
<%= jresp.toString() %>
