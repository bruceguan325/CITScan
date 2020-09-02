<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"
	language="java"%>
<%@ page import="javax.servlet.ServletConfig"%>
<%@ page import="javax.servlet.ServletException"%>
<%@ page import="javax.servlet.http.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.net.*"%>
<%@ page import="java.text.*"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.io.*"%>
<%@ page import="org.apache.commons.httpclient.*"%>
<%@ page import="org.apache.commons.httpclient.methods.*"%>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams"%>
<%@ page import="org.apache.solr.core.*"%>
<%@ page import="org.apache.solr.servlet.*"%>
<%@ page import="org.apache.solr.client.solrj.*"%>
<%@ page import="org.apache.solr.client.solrj.embedded.*"%>
<%@ page import="org.apache.solr.client.solrj.response.*"%>
<%@ page import="org.apache.solr.common.*"%>
<%@ page import="org.apache.solr.common.cloud.*"%>
<%@ page import="com.intumit.solr.SearchManager"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="com.intumit.quartz.ScheduleUtils"%>
<%@ page import="com.intumit.quartz.Job"%>

<%@ page import="com.intumit.solr.admin.*"%>
<%
	if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) {
	return;
}

boolean allowC = (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.C) > 0;
boolean allowD = (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0;
%>
<HTML>
<HEAD>
<TITLE>WiSe - Helper Links</TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script>
	function fillCommand(cmd, core) {
		var myForm = document.getElementById('cmd' + core);
		if (cmd == 'COMMIT') {
			myForm.COMMAND.value = '<commit/>';
		} else if (cmd == 'OPTIMIZE') {
			myForm.COMMAND.value = '<optimize/>';
		} else if (cmd == 'ROLLBACK') {
			myForm.COMMAND.value = '<rollback/>';
		} else if (cmd == 'DBI') {
			myForm.COMMAND.value = '<delete><id></id></delete>';
		} else if (cmd == 'DBQ') {
			myForm.COMMAND.value = '<delete><query></query></delete>';
		}
	}

	function doSubmit() {
		document.myForm.cmd.value = "add";
		document.myForm.submit();
	}
	function doDelete(theForm) {
		theForm.cmd.value = "delete";
		theForm.submit();
	}
	function doUpdate(theForm) {
		theForm.cmd.value = "update";
		theForm.submit();
	}
</script>
</HEAD>
<BODY>
	<%
	String action = request.getParameter("action");
	String targetCore = request.getParameter("coreName");
	String type = request.getParameter("t");

	if (allowC && "create".equalsIgnoreCase(action)) {
		CoreContainer cc = SearchManager.getLocalCores();
		int maxCoreCount = SearchManager.getCoreCount();
		String coreName = "core" + maxCoreCount;

		File baseDir = new File(System.getProperty("solr.solr.home"));
		File dir = new File(baseDir, coreName);
		if (!(dir.exists() && dir.isDirectory())) {
			File templateDir = new File(baseDir, "core-template");
			FileUtils.copyDirectory(templateDir, dir);
		}

		List<String> lines = new ArrayList<String>();
		lines.add("name=" + coreName);
		lines.add("loadOnStartup=true");
		FileUtils.writeLines(new File(dir, "core.properties"), lines);
		CoreDescriptor dcore = new CoreDescriptor(cc, coreName,
				coreName);

		SolrCore core = cc.create(dcore);
		//cc.register(coreName, core, false);
		%>
		<p>
			<strong><%=coreName%></strong> created!
		</p>
		<%
	} else if ("reload".equalsIgnoreCase(action)) {
		CoreContainer cc = SearchManager.getLocalCores();
		cc.reload(targetCore);
		%>
		<%=targetCore%>
		reloaded!
		<%
	} else if (allowD && "unload".equalsIgnoreCase(action)) {
		CoreContainer cc = SearchManager.getLocalCores();
		SolrCore core = cc.getCore(targetCore);
		cc.unload(targetCore);
		core.close();
		%>
		<%=targetCore%>
		removed!
		<%
	}
	%>
	<TABLE width="100%" class="table table-striped">
		<THEAD>
			<TR>
				<TH valign="top"><bean:message key="item"/></TH>
				<TH valign="top"><bean:message key="source.name"/></TH>
				<TH valign="top"><bean:message key="directory"/></TH>
				<TH valign="top"><bean:message key="open.number"/></TH>
				<TH valign="top"><bean:message key="uptime"/></TH>
				<TH valign="top"><bean:message key="operation"/></TH>
			</TR>
		</THEAD>
		<%
		if (SearchManager.isCloudMode()) {
			ClusterState clusterState = SearchManager.getClusterState();
			int no = 1;

			for (String collectionName : SearchManager.getAllCollectionNames()) {
				Collection<Slice> activeSlices = clusterState.getActiveSlices(collectionName);
				Map<String, Object> props = clusterState.getCollection(collectionName).getProperties();
				%>
				<TR>
					<TD align="center" valign="top"><%=no++%></TD>
					<TD align="center" valign="top"><%=collectionName%></TD>
					<TD align="center" valign="top">
					<%
						for (Slice slice : activeSlices) {
							out.println(slice.getName() + ":" + slice.getState() + "<BR>");

							for (Replica replica : slice.getReplicas()) {
								out.println("&nbsp;&nbsp;&nbsp;>> " + replica.getNodeName() + "<BR>");
							}
						}
					%>
					</TD>
					<TD align="center" valign="top"><%=""%></TD>
					<TD align="center" valign="top"></TD>
					<TD align="center" valign="top">
						<div class="btn-group">
						<button type="button" class="btn btn-primary" onclick="return confirm('<bean:message key='sure.reload'/>');" href="coresAdmin.jsp?action=RELOAD&coreName=<%=collectionName%>"><span class="glyphicon glyphicon-refresh"></span>&nbsp;<bean:message key="reload"/></button>
						<button type="button" class="btn btn-info" href="stats.jsp?coreName=<%=collectionName%>"><span class="glyphicon glyphicon-stats"></span>&nbsp;STATUS</button>
						<button type="button" class="btn btn-danger" onclick="return confirm('<bean:message key='sure.del.resource'/>');" href="coresAdmin.jsp?action=UNLOAD&coreName=<%=collectionName%>"><span class="glyphicon glyphicon-remove"></span>&nbsp;DELETE</button>
						<button type="button" class="btn btn-danger" onclick="return confirm('<bean:message key='data.resource.set'/>');" href="genDataConfig.jsp?step=1&coreName=<%=collectionName%>"><span class="glyphicon glyphicon-edit"></span>&nbsp;DATA-CONFIG</button>
						</div>
					</TD>
				</TR>
				<%
			}
		} else {
			for (int i = 0; i < SearchManager.getCoreCount(); i++) {
				String coreName = "core" + i;
				CoreContainer cc = SearchManager.getLocalCores();
				SolrCore core = cc.getCore(coreName);
				String instanceDir = core.getResourceLoader() .getInstanceDir();
				String dataDir = core.getDataDir();
				Date startTime = new Date(core.getStartTime());
				long uptime = (System.currentTimeMillis() - core.getStartTime()) / 1000 / 60;
				%>
				<TR>
					<TD align="center" valign="top"><%=i + 1%></TD>
					<TD align="center" valign="top"><%=coreName%></TD>
					<TD align="center" valign="top"><%=instanceDir%></TD>
					<TD align="center" valign="top"><%=core.getOpenCount()%></TD>
					<TD align="center" valign="top"><%=uptime%></TD>
					<TD align="center" valign="top">
						<div class="btn-group btn-group-xs">
						<a class="btn btn-primary btn-small" onclick="return confirm('<bean:message key='sure.reload'/>');" href="coresAdmin.jsp?action=RELOAD&coreName=<%=coreName%>"><span class="glyphicon glyphicon-refresh"></span>&nbsp;RELOAD</a>
						<a class="btn btn-info btn-small" href="stats.jsp?coreName=<%=coreName%>"><span class="glyphicon glyphicon-stats"></span>&nbsp;STATUS</a>
						<a class="btn btn-danger btn-small" onclick="return confirm('<bean:message key='sure.del.resource'/>');" href="coresAdmin.jsp?action=UNLOAD&coreName=<%=coreName%>"><span class="glyphicon glyphicon-remove"></span>&nbsp;DELETE</a>
						<a class="btn btn-danger btn-small" href="gen-dataconfig-step0.jsp?step=1&coreName=<%=coreName%>"><span class="glyphicon glyphicon-edit"></span>&nbsp;DATA-CONFIG</a>
						</div>
					</TD>
				</TR>
				<%
			}

		}
		%>
	</TABLE>
</BODY>
</HTML>
