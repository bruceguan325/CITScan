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
<%@ page import="org.apache.solr.common.cloud.*"%>
<%@ page import="org.apache.solr.request.*" %>
<%@ page import="org.apache.solr.search.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.util.WiSeEnv" %>
<%@ include file="/commons/taglib.jsp"%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) {
	return;
}

boolean allowD = (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0;
boolean allowU = (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.U) > 0;
%>
<%!
static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "Name_t", "Title_t", "Date_dt", "TopCategoryName_s", "FirstCategoryName_s", "TopCategory_ms", "Name", "LogTime"});
public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() +  WiSeEnv.getAdminContextPath() + "/" + jsp + "?coreName=" + coreName + (query == null ? "" : ("&" + (query)));
	return url;
}

%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe Administrator</TITLE>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
<script>
function doAdmin(core, action) {
	if (action == 1) {
		if (confirm("<bean:message key='delta.import'/>"))
		if (confirm("<bean:message key='sure.delta.import'/>")) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=delta-import&optimize=false&wt=xslt&tr=dih.xsl';
		}
	}
	else if (action == 2) {
		if (confirm("<bean:message key='full.import'/>"))
		if (confirm("<bean:message key='sure.full.import'/>")) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/' + core + '/dataimport?command=full-import&commit=false&optimize=false&clean=true&wt=xslt&tr=dih.xsl';
		}
	}
	else if (action == 3) {
		if (confirm("<bean:message key='sms'/>"))
		if (confirm("<bean:message key='sure.sms'/>")) {
			window.myFrame.location='<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/sms?command=commit&coreName=' + core + '';
		}
	}
	return false;
}

function fillCommand(cmd, core) {
	var myForm = document.getElementById('cmdArea');
	if (cmd == 'COMMIT') {
		myForm.COMMAND.value = '<commit/>';
	}
	else if (cmd == 'OPTIMIZE') {
		myForm.COMMAND.value = '<optimize/>';
	}
	else if (cmd == 'ROLLBACK') {
		myForm.COMMAND.value = '<rollback/>';
	}
	else if (cmd == 'DBI') {
		myForm.COMMAND.value = '<delete><id></id></delete>';
	}
	else if (cmd == 'DBQ') {
		myForm.COMMAND.value = '<delete><query></query></delete>';
	}
}

function deleteSelected() {
	if (confirm("<bean:message key='sure.del.data'/>")) {
		document.docMgrForm.mode.value = 'deleteSelected';
		document.docMgrForm.submit();

		return true;
	}
	else {
		return false;
	}
}

function updateSelected() {
	$('#updateForm').modal('hide');
	if (confirm("<bean:message key='sure.update.data'/>")) {
		document.docMgrForm.mode.value = 'updateSelected';
		document.docMgrForm.submit();

		return true;
	}
	else {
		return false;
	}
}
</script>
</HEAD>
<BODY>
<TABLE width="100%">
<TR>
<TD width="100%" valign="top">
	<div class="accordion" id="accordion-top">
	<%
		String coreName = request.getParameter("coreName");
		CoreContainer cc = SearchManager.getLocalCores();
		SolrServer server = SearchManager.getServer(coreName);
		
		Collection<String> fieldNames = new ArrayList<String>();

		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			fieldNames.addAll(core.getSearcher().get().getFieldNames());
		}

		//out.println(core.getInfoRegistry().get("searcher").getStatistics());
		String mode = request.getParameter("mode");
		if (allowD && "deleteSelected".equals(mode)) {
			String[] docIds = request.getParameterValues("docIds");
			if (docIds != null) {
				for (int i = 0; i < docIds.length; i++) {
					out.println("deleting [" + docIds[i] + "].....<BR>");
					server.deleteById(docIds[i]);
				}
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
			}
		} else if (allowD && "deleteAllQueried".equals(mode)) {
			String deleteQuery = request.getParameter("modeQuery");
			if (deleteQuery != null) {
				out.println("deleting by query [" + deleteQuery
						+ "].....<BR>");
				server.deleteByQuery(deleteQuery);
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
			}
		} else if (allowU && "updateSelected".equals(mode)) {

			SolrInputDocument docTpl = new SolrInputDocument();
			if (request.getParameterMap().containsKey("key")) {
				String[] reqKeys = request.getParameterValues("key");
				String[] reqVals = request.getParameterValues("value");
				HashSet<String> appendKeys = new HashSet<String>();
				
				String[] append = request.getParameterValues("append");
				if(append != null){
					for (String appendField:append) {
						if(StringUtils.isNotBlank(appendField)){
							appendKeys.add(appendField);
						}
					}
				}

				
				for(int i = reqKeys.length-1;i>=0;i--){
					if (StringUtils.isNotEmpty(reqVals[i])) {
						Map<String,String> partialUpdate = new HashMap<String,String>();
						partialUpdate.put(appendKeys.contains(reqKeys[i]) ? "add" : "set", reqVals[i]);
						
						docTpl.addField(reqKeys[i], partialUpdate);
					}
				}
			}
			
			
			String[] docIds = request.getParameterValues("docIds");
			if (docIds != null) {
				for (int i = 0; i < docIds.length; i++) {
					out.println("updating [" + docIds[i] + "].....<BR>");
					SolrInputDocument doc = docTpl.deepCopy();
					doc.addField("id", docIds[i]);
					
					server.add(doc);
				}
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
			}
		} else if (allowU && "updateAllQueried".equals(mode)) {


			SolrInputDocument docTpl = new SolrInputDocument();
			if (request.getParameterMap().containsKey("key")) {
				String[] reqKeys = request.getParameterValues("key");
				String[] reqVals = request.getParameterValues("value");
				HashSet<String> appendKeys = new HashSet<String>();
				
				String[] append = request.getParameterValues("append");
				if(append != null){
					for (String appendField:append) {
						if(StringUtils.isNotBlank(appendField)){
							appendKeys.add(appendField);
						}
					}
				}

				
				for(int i = reqKeys.length-1;i>=0;i--){
					if (StringUtils.isNotEmpty(reqVals[i])) {
						Map<String,String> partialUpdate = new HashMap<String,String>();
						partialUpdate.put(appendKeys.contains(reqKeys[i]) ? "add" : "set", reqVals[i]);
						
						docTpl.addField(reqKeys[i], partialUpdate);
					}
				}
			}
			
			String updateQuery = request.getParameter("modeQuery");
			if (updateQuery != null) {
				out.println("updating by query [" + updateQuery + "].....<BR>");
				%>
				<div class="progress progress-striped active">
				  <div class="progress-bar" id="updateProgress" role="progressbar" aria-valuenow="60" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
				    <span class="sr-only">0% Complete</span>
				  </div>
				</div>
				<%

				SolrQuery uq = new SolrQuery();
				uq.setQuery(updateQuery);
				uq.setFields("id");
				uq.setRows(1000000);
				QueryResponse resp = server.query(uq);
				SolrDocumentList docList = resp.getResults();
				long total = docList.getNumFound();
				long currPos = 0;
				int lastPercent = 0;
				
				for (SolrDocument idDoc: docList) {
					Object id = idDoc.getFieldValue("id");
					
					SolrInputDocument doc = docTpl.deepCopy();
					doc.addField("id", id);

					int percent = Math.round((float)currPos * 100f / (float)total);
					
					if (lastPercent != percent) {
						lastPercent = percent;
						
						if (percent % 5 == 0) {
							out.println("<script>$('#updateProgress').css({'width':'" + percent + "%'});</script>");
							out.flush();
						}
					}
					//out.println("updating [" + id + "]:" + doc + ".....<BR>");
					server.add(doc);
					
					currPos++;
				}
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
			}
		} else if ("reloadCore".equals(mode)) {
			cc.reload(coreName);
		}
		// reader.getFieldNames(IndexReader.FieldOption.ALL)
	%>
	<div><FONT SIZE=+2><A NAME="<%= coreName %>"><B><%= coreName %></B></A></FONT>
      <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
        <span class="glyphicon glyphicon-resize-small"></span>
        <span class="glyphicon glyphicon-resize-small"></span>
        <span class="glyphicon glyphicon-resize-small"></span>
      </a>
	</div>
	
	<form name="docMgrForm" action="indexAdmin.jsp" method="post">
	<input type="hidden" name="mode" value="nothing">
	<input type="hidden" name="modeQuery" value="nothing">
	<input type="hidden" name="coreName" value="<%= coreName %>">
	<div style="display: none">
	<textarea name="SELECT" cols="10" rows="10"><%= request.getParameter("SELECT") != null ? request.getParameter("SELECT") : "" %></textarea>
	</div>
	<%
	SolrQuery sqry = new SolrQuery();
    int start = 0;

	if (StringUtils.isNotBlank(request.getParameter("SELECT"))) {
		String select = request.getParameter("SELECT").trim();
		String[] clauses = select.split("\n");
		String query = "";
		for (int ccc = 0; ccc < clauses.length; ccc++) {
			String c = clauses[ccc].trim();
			if (c.length() == 0)
				continue;
			String key = c.substring(0, c.indexOf("="));
			String val = c.substring(c.indexOf("=") + 1);

			sqry.add(key, val);

			if (ccc > 0)
				query += "&";
			query += key + "=" + URLEncoder.encode(val, "UTF-8");
		}
	}
	else if (StringUtils.isNotBlank(request.getParameter("q"))) {
		String qqq = request.getParameter("q");
		start = request.getParameter("start") == null ? 0 : Integer
				.parseInt(request.getParameter("start"));
		String qt = request.getParameter("qt") == null ? "dismax"
				: request.getParameter("qt");
		String sort = request.getParameter("sort") == null ? "Date_dt desc"
				: request.getParameter("sort");
		String[] filterQuery = request.getParameterValues("fq");
		SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;

		if (sort.indexOf(" ") != -1) {
			String sortDir = sort.substring(sort.lastIndexOf(" "))
					.trim();
			sort = sort.substring(0, sort.lastIndexOf(" "));
			sortOrder = SolrQuery.ORDER.valueOf(sortDir);
		}
		for (int jjj = 0; filterQuery != null
				&& jjj < filterQuery.length; jjj++) {
			sqry.addFilterQuery(filterQuery[jjj].trim());
		}

		sqry.setQuery(qqq);
		sqry.addSortField(sort, sortOrder);
	}
	else {
		sqry.setQuery("*:*");
	}

	String deleteAllQuery = sqry.getQuery();
	if (sqry.getFilterQueries() != null)
		deleteAllQuery += " AND " + StringUtils.join(sqry.getFilterQueries(), " AND ");

	sqry.setRows(10);
	sqry.setStart(start);
	if (sqry.getFields() == null) {
		sqry.setFields(POSSIBLE_FIELDNAMES.toArray(new String[0]));
	}
	
	if (StringUtils.isEmpty(sqry.getSortField())) {
		sqry.setSort("Date_dt", SolrQuery.ORDER.desc);
	}

	QueryResponse resp = server.query(sqry);
	SolrDocumentList docList = resp.getResults();
	
	// Dunno why this could happened??!! So just a hack for no result in index.
	if (docList == null) docList = new SolrDocumentList();
    //For simplification, we are not using the JSTL
	SolrQuery sortByQuery = sqry.getCopy();
    %>
	<div id="collapseOne" class="accordion-body collapse in">
        <div align="left" class="text accordion-inner">
        	<bean:message key="follow.with"/>
        		<A HREF="<%= makeUrl(coreName, request, "indexAdmin.jsp",  sortByQuery.setSortField("Date_dt", SolrQuery.ORDER.desc).toString()) %>"><bean:message key="data"/></A>
        		、<A HREF="<%= makeUrl(coreName, request, "indexAdmin.jsp",  sortByQuery.setSortField("score", SolrQuery.ORDER.desc).toString()) %>"><bean:message key="relevance"/></A>
        	&nbsp;<bean:message key="sort"/>
        	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<bean:message key="all.search"/> <%= docList.getNumFound() %> <bean:message key="show.number"/> <%= start+1 + "-" + (start + sqry.getRows() > docList.getNumFound() ? docList.getNumFound() : start + sqry.getRows()) %> <bean:message key="number"/>
			<div class="btn-group">
			<%
			int currPage = (int)Math.floor(start / sqry.getRows())+1;
			if (currPage > 1) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(0);
				%>
				<A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "indexAdmin.jsp",  nextPageQuery.toString()) %>">
				&nbsp;<span class="glyphicon glyphicon-arrow-left"></span>
				</A>
				<%
			}
			%>
			<%
			for (int po=-4; po <= 4; po++) {
				int nStart = start + sqry.getRows() * po;
				if (nStart < 0 || nStart >= docList.getNumFound())
					continue;

				if (po == 0) {
					%><a class="btn btn-default active" href="#"><%= currPage+po %></a><%
					continue;
				}
				SolrQuery toPageQuery = sqry.getCopy();
				toPageQuery.setStart(nStart);
				%><A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "indexAdmin.jsp", toPageQuery.toString()) %>"><%= currPage+po %></A>
			<%
			}
			%>
			<%
			int lastPage = (int)Math.floor((docList.getNumFound()-1) / sqry.getRows()) + 1;
			if (lastPage != currPage) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart((lastPage-1) * sqry.getRows());
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, "indexAdmin.jsp", nextPageQuery.toString()) %>">
				<span class="glyphicon glyphicon-arrow-right"></span>&nbsp;</A>
				<%
			}
			%>
			</div>
        </div>
		<BR>
		<TABLE border="1" width="100%" class="table table-striped">
		<%
		int docOffset = 1;
		List<String> fields = new ArrayList<String>(Arrays.asList(sqry.getFields().split(",")));
		fields.retainAll(fieldNames);

		for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); docOffset++) {
			SolrDocument doc = docItr.next();
			String contentUrl = request.getContextPath() + WiSeEnv.getAdminContextPath() + "/" + coreName + "/select?q=id%3A%22" +  URLEncoder.encode((String)doc.getFieldValue("id"), "UTF-8") + "%22&sort=Date_dt+desc&wt=xslt&tr=content.xsl";
			%>
			<% if (docOffset==1) { // 印出欄位名稱 %>
			<THEAD>
			<TR>
				<TH>　</TH><TH>No.</TH><TH>ID</TH>
				<%
				for (String fn: fields) {
					if ("id".equalsIgnoreCase(fn))
						continue;
				%>
					<TH><%= fn %></TH>
				<%
				}
				%>
			</TR>
			</THEAD>
			<% } %>
			<TR>
				<TD><input type=checkbox name="docIds" value="<%= doc.getFieldValue("id") %>"></TD>
				<TD><%= docOffset %></TD>
				<TD>
				<A TARGET="_blank" HREF="<%= contentUrl %>"><%= doc.getFieldValue("id") %></A>
				</TD>
				<%
				// 印出每一個 document 的各欄位值
				for (String fn: fields) {

					if ("id".equalsIgnoreCase(fn))
						continue;

					Collection values = doc.getFieldValues(fn);
					String valStr = (values != null) ? StringUtils.left(values.toString(), 30) : "";
					if(values != null && values.size() > 30) valStr += "...";
					%>
					<td>
					<%= valStr %>
					</td>
					<%
				}
				%>
			</TR>
		<%
		}
		%>
		</TABLE>
		<script>
		function deleteAllQueried() {
			$('#updateForm').modal('hide');
			if (confirm("<bean:message key='del.all.accord1'/><%= deleteAllQuery %><bean:message key='del.all.accord2'/>")) {
				document.docMgrForm.mode.value = 'deleteAllQueried';
				document.docMgrForm.modeQuery.value = '<%= deleteAllQuery %>';
				document.docMgrForm.submit();

				return true;
			}
			else {
				return false;
			}
		}
		
		function updateAllQueried() {
			if (confirm("<bean:message key='del.all.update1'/><%= deleteAllQuery %><bean:message key='del.all.update2'/>")) {
				document.docMgrForm.mode.value = 'updateAllQueried';
				document.docMgrForm.modeQuery.value = '<%= deleteAllQuery %>';
				document.docMgrForm.submit();

				return true;
			}
			else {
				return false;
			}
		}
		
		function showUpdateForm(target) {
			var theSubmitFunc;
			if (target == 'updateSelected') {
				theSubmitFunc = updateSelected;
			}
			else if (target == 'updateAllQueried') {
				theSubmitFunc = updateAllQueried;
			}
			$('#updateFormSubmit').click(theSubmitFunc);
			$('#updateForm').modal('show');
		}
		</script>
		<% if (allowU) { %>
		<div class="btn-group">
		<button class="btn btn-primary" type="button" onclick="return showUpdateForm('updateSelected');"><bean:message key='update.select'/></button>
		<button class="btn btn-primary" type="button" onclick="return showUpdateForm('updateAllQueried');"><bean:message key='update.all'/></button>
		</div>
		<% } %>
		<% if (allowD) { %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<button class="btn btn-danger" type="button" onclick="return deleteSelected();"><bean:message key='delete.select'/></button>
		<button class="btn btn-danger" type="button" onclick="return deleteAllQueried();"><bean:message key='delete.all'/></button>
		</div>
		<% } %>
		
		<div id="updateForm" class="modal fade">
		    <div class="modal-dialog">
		    <div class="modal-content">
		    <div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h3><bean:message key='update.set'/></h3>
			</div>
			<%
			Map<String,String> columns = SearchManager.getColumnNames();
			%>
			<%-- 手動增加：<BR>
			<form method="GET" action="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/column-name-mgt.jsp">
				<TABLE width="100%">
					<tr>
						<th width="30%">欄位：<input type="text" value="" name="key" /></th>
						<th width="50%">名稱：<input type="text" size="70%" maxlength="16" name="value" value=""/></th>
						<th width="20%"><input type="submit" value="手動新增"></th>
					</tr>
				</TABLE>
			</form>	--%>
			<div class="modal-body">
				<bean:message key="have.column"/><BR>	
				<TABLE width="600" class="table table-striped table-bordered">
					<tr>
						<th width="100"><bean:message key="column"/></th>
						<th width="500"><bean:message key="update.value"/></th>
					</tr>
					<%
						String name;
						for (String key:columns.keySet()) {
							name = columns.get(key)==null?"":( "(" + columns.get(key) + ")" );
					%>
					<tr>
						<td><%= key + name %></td>
						<td>
							<input type="hidden" value='<%= key %>' name="key" />
							<input type="text" size="35" name="value" value=''>
							&nbsp;<bean:message key="add.ons"/><input type='checkbox' name='append' value='<%= key %>' >
						</td>
					</tr>
					<%
						}
					%>
				</TABLE>
				<bean:message key="manual.increase"/>：<BR>
				<TABLE width="600">
					<tr>
						<th width="100"><bean:message key="column"/>：<BR><input type="text" value="" name="key" /></th>
						<th width="500"><bean:message key="column.value"/>：<BR><input type="text" size="35" name="value" value=""/></th>
					</tr>
				</TABLE>
			</div>
	        <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
		        <button type="button" class="btn btn-primary" id="updateFormSubmit">Update changes</button>
	        </div>
			</div>
			</div>
		</div>
		
		</form>
	</div>
	<div>
		<TABLE>
		<TR>
		<TD>
			<FORM id="selectArea" action="indexAdmin.jsp">
				<TABLE>
				<TR>
				<TD valign="TOP">
							<%
							if (request.getParameter("SELECT") != null) {
								String select = request.getParameter("SELECT").trim();
								String[] clauses = select.split("\n");
								String query = "";
								for (int ccc = 0; ccc < clauses.length; ccc++) {
									String c = clauses[ccc].trim();
									if (c.length() == 0)
										continue;
									String key = c.substring(0, c.indexOf("="));
									String val = c.substring(c.indexOf("=") + 1);

									if (ccc > 0)
										query += "&";
									query += key + "=" + URLEncoder.encode(val, "UTF-8");
								}
								%>
								<A target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/select?<%= query %>">Query</A><BR>
								<%
							}
							%>
							<TEXTAREA NAME="SELECT" rows="3" cols="60" style="width: 400px;"><%= (request.getParameter("SELECT") != null) ? request.getParameter("SELECT").trim() : "q=*:*" %></TEXTAREA>
							<INPUT TYPE=HIDDEN NAME="coreName" VALUE="<%= coreName %>">
							<BR><INPUT class="btn btn-primary" TYPE="SUBMIT" VALUE="<bean:message key='query'/>"/>
				</TD>
				</TR>
				<TR>
				<TD valign="TOP">
				<%
					if (SearchManager.isCloudMode()) {
						ClusterState clusterState = SearchManager.getClusterState();
						Collection<Slice> activeSlices = clusterState.getActiveSlices(coreName);
						Map<String, Object> props = clusterState.getCollection(coreName).getProperties();
						List<String> realCoreNamesOnThisNode = new ArrayList<String>();
						
						
						for (String currCn: cc.getAllCoreNames()) {
							if (currCn.startsWith(coreName + "_")) {
								realCoreNamesOnThisNode.add(currCn);
							}
						}

						/*for (Slice slice : activeSlices) {

							for (Replica replica : slice.getReplicas()) {
								String realCoreName = coreName + "_" + slice.getName() + "_" + replica.getName();
								SolrCore realCore = cc.getCore(realCoreName);
								
								if (realCore != null) {
									realCoreNamesOnThisNode.add(realCoreName);
								}
								else {
									out.println(realCoreName + " not exists on this node.");
								}
							}
						}*/
						
						if (realCoreNamesOnThisNode.size() > 0) {
							String firstCore = realCoreNamesOnThisNode.get(0);
							%>
							<div class="btn-group">
							<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wise-admin.html#/<%= firstCore %>/dataimport//dataimport"><bean:message key="index.status"/></A> 
							<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/select?q=*%3A*&sort=Date_dt+desc"><bean:message key="view.index"/></A> 
							<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/dataimport?command=reload-config&wt=xslt&tr=dih.xsl"><bean:message key="reload.config"/></A> 
							<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wise-admin.html#/<%= coreName %>/replication"><bean:message key="replication"/></A> 
							<A class="btn btn-default" onclick="return confirm('<bean:message key='sure.reload'/>');" href="indexAdmin.jsp?mode=reloadCore&coreName=<%= coreName %>"><bean:message key="reload.core"/></A> 
							</div>
							<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '3');"><span class="glyphicon glyphicon-refresh"></span>&nbsp;<bean:message key='reload.searcher'/></A> 
							<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '1');"><span class="glyphicon glyphicon-plus"></span>&nbsp;<bean:message key='delta.import2'/></A> 
							<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '2');"><span class="glyphicon glyphicon-th"></span>&nbsp;<bean:message key='full.import2'/></A> 
							<%
						}
					}
					else {
						%>
						<div class="btn-group">
						<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wise-admin.html#/<%= coreName %>/dataimport//dataimport"><span class="glyphicon glyphicon-question-sign"></span>&nbsp;<bean:message key="index.status"/></A> 
						<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/select?q=*%3A*&sort=Date_dt+desc"><span class="glyphicon glyphicon-search"></span>&nbsp;<bean:message key="view.index"/></A> 
						<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/dataimport?command=reload-config&wt=xslt&tr=dih.xsl"><span class="glyphicon glyphicon-refresh"></span>&nbsp;<bean:message key="reload.config"/></A> 
						<A class="btn btn-default" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/wise-admin.html#/<%= coreName %>/replication"><span class="glyphicon glyphicon-stats"></span>&nbsp;<bean:message key="replication"/></A> 
						<A class="btn btn-default" onclick="return confirm('<bean:message key='sure.reload'/>');" href="indexAdmin.jsp?mode=reloadCore&coreName=<%= coreName %>"><span class="glyphicon glyphicon-refresh"></span>&nbsp;<bean:message key="reload.core"/></A> 
						</div>
						<div class="btn-group">
						<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '3');"><span class="glyphicon glyphicon-refresh"></span>&nbsp;<bean:message key='reload.searcher'/></A> 
						<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '1');"><span class="glyphicon glyphicon-plus"></span>&nbsp;<bean:message key='delta.import2'/></A> 
						<A class="btn btn-default" HREF="#" onclick="return doAdmin('<%= coreName %>', '2');"><span class="glyphicon glyphicon-th"></span>&nbsp;<bean:message key='full.import2'/></A> 
						</div>
						<%
					}
				%>
				</TD>
				</TR>
				</TABLE>
				
			</FORM>
			<BR>
		</TD>
		<TD valign="TOP">
			<FORM id="cmdArea" action="indexAdmin.jsp">
				<TABLE>
				<TR>
				<TD>
					<%
					if (request.getParameter("COMMAND") != null) {
						String cmd = request.getParameter("COMMAND").trim();
						%>
						<strong>
						<A class="btn btn-danger" target="myFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/<%= coreName %>/update?stream.body=<%= URLEncoder.encode(cmd, "UTF-8") %>">
						<bean:message key='generated.command'/><BR><bean:message key='attention'/>
						</A>
						</strong>
						<BR/>
						<%
					}
					%>
					<TEXTAREA NAME="COMMAND" rows="3" cols="60" style="width: 400px;"><%= (request.getParameter("COMMAND") != null) ? request.getParameter("COMMAND").trim() : "<commit/>" %></TEXTAREA>
					<INPUT TYPE=HIDDEN NAME="coreName" VALUE="<%= coreName %>">
					<BR><INPUT class="btn btn-primary" TYPE="SUBMIT" VALUE="<bean:message key='production.link'/>">
				</TD>
				</TR>
				<TR>
				<TD>
					<A style="cursor:hand; text-decoration: underline;" onclick="fillCommand('COMMIT', '<%= coreName %>');"><bean:message key='commit'/></A>
					&nbsp;<A style="cursor:hand; text-decoration: underline;" onclick="fillCommand('OPTIMIZE', '<%= coreName %>');"><bean:message key='optimize'/></A>
					&nbsp;<A style="cursor:hand; text-decoration: underline;" onclick="fillCommand('ROLLBACK', '<%= coreName %>');"><bean:message key='rollback'/></A>
					&nbsp;<A style="cursor:hand; text-decoration: underline;" onclick="fillCommand('DBI', '<%= coreName %>');"><bean:message key='delete.by.id'/></A>
					&nbsp;<A style="cursor:hand; text-decoration: underline;" onclick="fillCommand('DBQ', '<%= coreName %>');"><bean:message key='delete.by.query'/></A>
				    <BR><bean:message key='attention2'/><BR>
				</TD>
				</TR>
				</TABLE>
			</FORM>
			<BR>
		</TD>
		</TR>
		</TABLE>
	</div>
</TD>
</TR>
<TR>
<TD width="100%" valign="top">

      <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
        <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span>
        <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span>
        <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span> <span class="glyphicon glyphicon-resize-small"></span>
      </a>
	<iframe style=""  name="myFrame" width="100%" height="1024" src="Blank.html"></iframe>
</TD>
</TR>
</TABLE>
</BODY>
</HTML>
