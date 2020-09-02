<%@page import="com.intumit.message.MessageUtil"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.util.WiSeUtils"
%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%!
static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "kid_l", "Name_t", "QUESTION_s", "ANSWER_s", "Title_t", "Date_dt", "TopCategoryName_s", "FirstCategoryName_s", "TopCategory_ms", "Name", "LogTime", "updatedAdminUserId_i", "updated_dt", "ANSWER_LINK_s", "ANSWER_RICHTEXT_s"});
static Collection<String> NOT_FOR_LIST_FIELDNAMES = new ArrayList<String>(Arrays.asList(new String[] {"id", "kid_l", "updatedAdminUserId_i", "ANSWER_LINK_s", "ANSWER_RICHTEXT_s"}));

public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	try {
		String url = req.getContextPath() + "/wiseadm/" + jsp + "?coreName=" + URLEncoder.encode(coreName, "UTF-8") + (query == null ? "" : ("&" + (query)));
		return url;
	}
	catch (Exception ignore) {}
	return null;
}

public String buildStatusIcon(QAAltBuildQueue.Status buildStatus, Locale locale) {
	String buildStatusText="";
	if (buildStatus == QAAltBuildQueue.Status.PENDING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PENDING) {
		 buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml(buildStatus.text));
		return "<div class='text-danger'><span class='text-danger glyphicon glyphicon-time'></span>" + buildStatusText + "</div>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.PROCESSING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PROCESSING) {
		buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml(buildStatus.text));
		return "<div class='text-warning'><span class='text-warning glyphicon glyphicon-refresh'></span>" + buildStatusText + "</div>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.DONE) {
		buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml(buildStatus.text));
		return "<div class='text-success'><span class='text-successs glyphicon glyphicon-ok-sign'></span>" + buildStatusText + "</div>";
	}
	buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml(buildStatus.text));
	return "<div class='text-danger'><span class='text-danger glyphicon glyphicon-exclamation-sign'></span>" + buildStatusText + "</div>";
}

public String getColumnWidth(String fn) {
	if ("QUESTION_s".equals(fn)) {
		return "col-md-3";
	}
	else if ("ANSWER_s".equals(fn)) {
		return "col-md-4";
	}
	else if (StringUtils.endsWith(fn, "_dt")) {
		return "col-md-2";
	}
	
	return "col-md-1";
}

/**
 * 做一個乾淨的 Query 物件 目的為 1. 怕 URL 過長（這樣 Google AdSense 可能會有 Error） 2. Facet
 * Fileds, Facet Queries 其實不是給 User 自訂的，不需要包含在 URL 裡頭
 */
protected static SolrQuery buildCleanQuery(SolrQuery query) {
    SolrQuery cleanQuery = new SolrQuery(query.getQuery());
    for (int iii = 0; query.getFilterQueries() != null
        && iii < query.getFilterQueries().length; iii++) {
    	
    	String fq = query.getFilterQueries()[iii].trim();
    	
    	if (fq.startsWith("{!tag=DF}"))
    		continue;
        cleanQuery.addFilterQuery(fq);
    }
    cleanQuery.setParam("sort", query.getSortField());
    cleanQuery.setStart(query.getStart());
    cleanQuery.setRows(query.getRows());
    cleanQuery.setRequestHandler(query.getRequestHandler());
    
    Set<String> paramNames = query.getParameterNames();
    //cleanQuery.remove("qt");

    return cleanQuery;
}

%>
<HTML>
<HEAD>
<TITLE><bean:message key='qa.data.manger'/> </TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<script>
function doAdmin(core, action) {
	if (action == 1) {
		if (confirm('<bean:message key="delta.import"/>'))
		if (confirm('<bean:message key="sure.delta.import"/>')) {
			window.myFrame.location='<%= request.getContextPath() %>/wiseadm/' + core + '/dataimport?command=delta-import&optimize=false&wt=xslt&tr=dih.xsl';
		}
	}
	else if (action == 2) {
		if (confirm('<bean:message key="full.import"/>'))
		if (confirm('<bean:message key="sure.full.import"/>')) {
			window.myFrame.location='<%= request.getContextPath() %>/wiseadm/' + core + '/dataimport?command=full-import&commit=false&optimize=false&clean=true&wt=xslt&tr=dih.xsl';
		}
	}
	else if (action == 3) {
		if (confirm('<bean:message key="sms"/>'))
		if (confirm('<bean:message key="sure.sms"/>')) {
			window.myFrame.location='<%= request.getContextPath() %>/wiseadm/sms?command=commit&coreName=' + core + '';
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

function cancelSelected() {
	if (confirm("<bean:message key='sure.stop.calculation'/>")) {
		document.docMgrForm.mode.value = 'cancelSelected';
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
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<%
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	    boolean showExpired = new Boolean(StringUtils.defaultString(request.getParameter("expired"), "false"));
		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
		String coreName = t.getCoreName();
		CoreContainer cc = SearchManager.getLocalCores();
		SolrServer server = t.getCoreServer4Write();
		StringBuilder kids = new StringBuilder();
		
		for (QAAltBuild qaab: QAAltBuild.list()) {
			if (kids.length() > 0) 
				kids.append(" ");
			kids.append(qaab.getKid());
		}

		Collection<String> fieldNames = new ArrayList<String>();

		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			fieldNames.addAll(core.getSearcher().get().getFieldNames());
		}

		//out.println(core.getInfoRegistry().get("searcher").getStatistics());
		String mode = request.getParameter("mode");
		if ("deleteSelected".equals(mode) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0) {
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
		} else if ("deleteAllQueried".equals(mode) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0) {
			String deleteQuery = request.getParameter("modeQuery");
			if (deleteQuery != null) {
				out.println("deleting by query [" + deleteQuery
						+ "].....<BR>");
				server.deleteByQuery(deleteQuery + " AND dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
			}
		} else if ("cancelSelected".equals(mode) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0) {
			String[] docIds = request.getParameterValues("docIds");
			if (docIds != null) {
				for (int i = 0; i < docIds.length; i++) {
					out.println("canceling [" + docIds[i] + "].....<BR>");
					QAAltBuildQueue.cancelRunningBuilds(t.getId(), docIds[i]);
				}
			}
		} else if ("cancelAllQueried".equals(mode) && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0) {

			String updateQuery = request.getParameter("modeQuery");
			if (updateQuery != null) {
				out.println("canceling by query [" + updateQuery + "].....<BR>");
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
				uq.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
				uq.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");
				uq.setRows(1000000);
				QueryResponse resp = server.query(uq);
				SolrDocumentList docList = resp.getResults();
				long total = docList.getNumFound();
				long currPos = 0;
				int lastPercent = 0;
				
				for (SolrDocument idDoc: docList) {
					String idStr = (String)idDoc.getFieldValue("id");
					QAAltBuildQueue.cancelRunningBuilds(t.getId(), idStr);

					int percent = Math.round((float)currPos * 100f / (float)total);
					
					if (lastPercent != percent) {
						lastPercent = percent;
						
						if (percent % 5 == 0) {
							out.println("<script>$('#updateProgress').css({'width':'" + percent + "%'});</script>");
							out.flush();
						}
					}
					
					currPos++;
				}
			}
		} else if ("updateSelected".equals(mode)) {

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
		} else if ("updateAllQueried".equals(mode)) {


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
				uq.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
				uq.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");
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
	<br>
	<h1 class="alert alert-danger hide">
	<bean:message key='system.attention'/>
	</h1>
	<div><A NAME="<%= coreName %>"></A>
      <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
      </a>
	</div>
	<form name="docMgrForm" action="qaAdminQueue.jsp" method="post">
	<input type="hidden" name="mode" value="nothing">
	<input type="hidden" name="modeQuery" value="nothing">
	<input type="hidden" name="coreName" value="<%= coreName %>">
	<div style="display: none">
	<textarea name="SELECT" cols="10" rows="10"><%= request.getParameter("SELECT") != null ? request.getParameter("SELECT") : "" %></textarea>
	</div>
	<%
	SolrQuery sqry = new SolrQuery();
	sqry.setRequestHandler("/browse");
	sqry.setParam("qf", "QUESTION_t QUESTION_ALT_mt");
	sqry.setParam("mm", "1");
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
			System.out.println(String.format("%s, %s", key, val));

			if ("q".equalsIgnoreCase(key)) {
				sqry.setQuery(val);
			}
			else {
				sqry.add(key, val);
			}

			if (ccc > 0)
				query += "&";
			query += key + "=" + URLEncoder.encode(val, "UTF-8");
		}
	}
	else if (StringUtils.isNotBlank(request.getParameter("q"))) {
		String qqq = request.getParameter("q");
		start = request.getParameter("start") == null ? 0 : Integer
				.parseInt(request.getParameter("start"));
		String qt = request.getParameter("qt") == null ? "browse"
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
	
	sqry.addFilterQuery("{!tag=DF}dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
	sqry.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");
	sqry.addFilterQuery("{!tag=DF}kid_l:(" + kids.toString() + ")");
	if (showExpired)
		sqry.addFilterQuery("EXPIRE_dt:[* TO NOW/DAY]");

	String deleteAllQuery = sqry.getQuery();
	if (sqry.getFilterQueries() != null)
		deleteAllQuery += " AND " + StringUtils.join(sqry.getFilterQueries(), " AND ");

	sqry.setRows(10);
	sqry.setStart(start);
	if (sqry.getFields() == null) {
		sqry.setFields(POSSIBLE_FIELDNAMES.toArray(new String[0]));
	}
	
	if (StringUtils.isEmpty(sqry.getSortField())) {
		sqry.setSort("kid_l", SolrQuery.ORDER.asc);
	}

	QueryResponse resp = server.query(sqry);
	SolrDocumentList docList = resp.getResults();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	
	// Dunno why this could happened??!! So just a hack for no result in index.
	if (docList == null) docList = new SolrDocumentList();
    //For simplification, we are not using the JSTL
	SolrQuery sortByQuery = sqry.getCopy();
			
	String[] offeredSortOptions = {"updated_dt", "kid_l", "score"};
	String[] offeredSortOptionsName = {MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "update.time"), MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "knowledge.num"), MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "relevance")};
    %>
    
	<div class="row">
	<div class="col-md-12">
	    <div align="left" class="text accordion-inner">
	       	<bean:message key='search.orderby.1'/>
	       	<%
	       	for (int i=0; i < offeredSortOptions.length; i++) {
	       		if (i > 0) out.println("、");
	       		out.println(
	       				String.format("%s <a href='%s'><span class='glyphicon glyphicon-arrow-up'></span></a><a href='%s'><span class='glyphicon glyphicon-arrow-down'></span></a>",
	       						offeredSortOptionsName[i],
	       						makeUrl(coreName, request, "qaAdminQueue.jsp",  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.asc)).toString()),
	       						makeUrl(coreName, request, "qaAdminQueue.jsp",  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.desc)).toString())
	       						)
	       				);
	       	}
	       	%>
	       	&nbsp;<bean:message key='search.orderby.2'/>
	       	<br>
			<bean:message key='all.search'/> <%= docList.getNumFound() %> <bean:message key='show.number'/> <%= start+1 + "-" + (start + sqry.getRows() > docList.getNumFound() ? docList.getNumFound() : start + sqry.getRows()) %> <bean:message key='number'/>
			<div class="btn-group">
			<%
			int JUMP_N_PAGE = 5;
			
			int currPage = (int)Math.floor(start / sqry.getRows())+1;
			if (currPage > 1) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(0);
				%>
				<A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "qaAdminQueue.jsp",  buildCleanQuery(nextPageQuery).toString()) %>">
				&nbsp;<span class="glyphicon glyphicon-fast-backward"></span>
				</A>
				<%
			}
			%>
			<%
			int prevNPageStart = start - sqry.getRows() * JUMP_N_PAGE;
			
			if (prevNPageStart >= 0 && prevNPageStart < docList.getNumFound()) {
				SolrQuery prevPageQuery = sqry.getCopy();
				prevPageQuery.setStart(prevNPageStart);
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, "qaAdminQueue.jsp", buildCleanQuery(prevPageQuery).toString()) %>">
				<span class="glyphicon glyphicon-step-backward"></span>&nbsp;</A>
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
				%><A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "qaAdminQueue.jsp", buildCleanQuery(toPageQuery).toString()) %>"><%= currPage+po %></A>
			<%
			}
			%>
			<%
			int nextNPageStart = start + sqry.getRows() * JUMP_N_PAGE;
			
			if (nextNPageStart >= 0 && nextNPageStart < docList.getNumFound()) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(nextNPageStart);
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, "qaAdminQueue.jsp", buildCleanQuery(nextPageQuery).toString()) %>">
				<span class="glyphicon glyphicon-step-forward"></span>&nbsp;</A>
				<%
			}
			%>
			<%
			int lastPage = (int)Math.floor((docList.getNumFound()-1) / sqry.getRows()) + 1;
			if (lastPage != currPage) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart((lastPage-1) * sqry.getRows());
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, "qaAdminQueue.jsp", buildCleanQuery(nextPageQuery).toString()) %>">
				<span class="glyphicon glyphicon-fast-forward"></span>&nbsp;</A>
				<%
			}
			%>
			</div>
			<div>
			<bean:message key='jump.knowledge.num'/>
			<input id="goKnowledgeNo" type="text" class="input-xs" size="3">
			</div>
	    </div>
		</div>
    </div>
	<div class="row">
	<div class="col-md-12">
		<TABLE class="table table-striped table-bordered table-hover">
		<%
		int docOffset = 1;
		List<String> fields = new ArrayList<String>(Arrays.asList(sqry.getFields().split(",")));
		fields.retainAll(fieldNames);

		for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); docOffset++) {
			SolrDocument doc = docItr.next();
			String id = (String)doc.getFieldValue("id");
			Long kid = (Long)doc.getFieldValue("kid_l");
			String contentUrl = request.getContextPath() + "/wiseadm/qaDataEditor.jsp?id=" + URLEncoder.encode((String)doc.getFirstValue("id"), "UTF-8");
			%>
			<% if (docOffset==1) { // 印出欄位名稱 %>
			<THEAD>
			<TR>
				<TH width="10">　</TH><TH width="10"><bean:message key='knowledge.num.br'/></TH>
				<%
				for (String fn: fields) {
					if (NOT_FOR_LIST_FIELDNAMES.contains(fn))
						continue;
					%>
					<TH><%= ColumnNameMappingFacade.getInstance().getMappingName(fn) %></TH>
				<%
				}
				%>
				<TH><bean:message key='updater'/></TH>
				<TH><bean:message key='advanced.br.answer'/></TH>
				<TH><bean:message key='system.br.state'/></TH>
			</TR>
			</THEAD>
			<% } %>
			<TR>
				<TD><input type=checkbox name="docIds" value="<%= id %>"></TD>
				<TD>
					<A TARGET="_blank" HREF="<%= contentUrl %>">
					<%= kid == null ? StringUtils.substringAfter(id, "-") : kid %>
					</A>
				</TD>
				<%
				// 印出每一個 document 的各欄位值
				for (String fn: fields) {
					if (NOT_FOR_LIST_FIELDNAMES.contains(fn))
						continue;

					Collection values = doc.getFieldValues(fn);
					String valStr = null;
					if (values != null) {
						if (values.size() > 1) {
							valStr = StringUtils.left(StringEscapeUtils.escapeHtml(values.toString()), 500);
						}
						else {
							Object val = values.iterator().next();
							if (val instanceof Date) {
								valStr = sdf.format((Date)val);
							}
							else {
								valStr = StringUtils.left(StringEscapeUtils.escapeHtml(val.toString()), 500);
							}
						}
						if(valStr != null && valStr.length() == 500) valStr += "...";
					}
					else {
						valStr = "";
					}
					%>
					<td class="<%= getColumnWidth(fn) %>">
					<div class="row col-md-12">
					<% if ("QUESTION_s".equalsIgnoreCase(fn)) { %><A TARGET="_blank" HREF="<%= contentUrl %>"><% } %>
					<%= valStr %>
					<% if ("QUESTION_s".equalsIgnoreCase(fn)) { %></A><% } %>
					</div>
					</td>
					<%
				}
				
				Integer updateAdminId = (Integer)doc.getFieldValue("updatedAdminUserId_i");
				AdminUser updateAdmin = updateAdminId == null ? null : AdminUserFacade.getInstance().get(updateAdminId);
				boolean hasRichtext = doc.getFieldValue("ANSWER_RICHTEXT_s") != null;
				boolean hasExternalLink = doc.getFieldValue("ANSWER_LINK_s") != null;
				
				
				%>
				<td>
				<%
				if (updateAdminId == null) {
					out.println("unknown");
				}
				else {
					SolrQuery userQuery = sqry.getCopy();
		       		out.println(
		       				String.format("<a href='%s'>%s</a>",
		       						makeUrl(coreName, request, "qaAdminQueue.jsp",  buildCleanQuery(userQuery.addFilterQuery("updatedAdminUserId_i:" + updateAdminId).setStart(0)).toString()),
		       						(updateAdmin == null ? "gone (" + updateAdminId + ")" : updateAdmin.getLoginName())
		       						)
		       				);
				}
				
				%>
				</td>
				<td>
				<%
				if (hasRichtext) {
					%><bean:message key='format.text'/><%
				}
				else if (hasExternalLink) {
					%><bean:message key='external.link'/><%
				}
				else {
					%><bean:message key='no.advance'/><%
				}
				%>
				</td>
				<td><%= buildStatusIcon(QAAltBuildQueue.getStatus(t.getId(), id), locale) %></td>
			</TR>
		<%
		}
		%>
		</TABLE>
	</div>
	</div>
	
	<div class="row">
	<div class="col-md-12">
		<script>
		</script>
		<!-- div class="btn-group">
		<button class="btn btn-primary" type="button" onclick="return showUpdateForm('updateSelected');">更新選擇</button>
		<button class="btn btn-primary" type="button" onclick="return showUpdateForm('updateAllQueried');">更新全部</button>
		</div -->
		<% if (!showExpired && (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() & AdminGroup.D) > 0) { %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<button class="btn btn-danger" type="button" onclick="return deleteSelected();"><bean:message key='delete.select'/></button>
		<button class="btn btn-danger" type="button" onclick="return deleteAllQueried();"><bean:message key='delete.all'/></button>
		</div>
		<% } %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<button class="btn btn-danger" type="button" onclick="return cancelSelected();">(Cancel)<bean:message key='global.select'/></button>
		<button class="btn btn-danger" type="button" onclick="return cancelAllQueried();">(Cancel)<bean:message key='global.all'/></button>
		</div>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<a class="btn btn-success" href="qaDataEditor.jsp"><bean:message key='global.add'/></a>
		</div>
		<% if (!showExpired) { %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<a class="btn btn-warning" href="qaAdminQueue.jsp?expired=true"><bean:message key='expired.qa'/></a>
		</div>
		<% } %>
			<div id="updateForm" class="modal fade">
			    <div class="modal-dialog" style="width: 750px;">
			    <div class="modal-content">
			    <div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h3><bean:message key='update.set'/></h3>
				</div>
				<%
				Map<String,String> columns = SearchManager.getColumnNames();
				%>
				<%-- 手動增加：<BR>
				<form method="GET" action="<%= request.getContextPath() %>/wiseadm/column-name-mgt.jsp">
					<TABLE width="100%">
						<tr>
							<th width="30%">欄位：<input type="text" value="" name="key" /></th>
							<th width="50%">名稱：<input type="text" size="70%" maxlength="16" name="value" value=""/></th>
							<th width="20%"><input type="submit" value="手動新增"></th>
						</tr>
					</TABLE>
				</form>	--%>
				<div class="modal-body">
					<bean:message key='have.column'/><BR>	
					<TABLE width="600" class="table table-striped table-bordered">
						<tr>
							<th width="100"><bean:message key='column'/></th>
							<th width="500"><bean:message key='update.value'/></th>
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
								&nbsp;附加<input type='checkbox' name='append' value='<%= key %>' >
							</td>
						</tr>
						<%
							}
						%>
					</TABLE>
					<bean:message key='manual.increase'/>：<BR>
					<TABLE width="600">
						<tr>
							<th width="100"><bean:message key='column'/>：<BR><input type="text" value="" name="key" /></th>
							<th width="500"><bean:message key='column.value'/>：<BR><input type="text" size="35" name="value" value=""/></th>
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
			</div>
		</div>
	</form>
		

	
	<div class="row">
	<div class="col-md-12">
		<div class="col-md-6">
			<FORM id="selectArea" action="qaAdminQueue.jsp">
			<div class="row">
				<div class="col-md-12">
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
						<!-- A target="myFrame" HREF="<%= request.getContextPath() %>/wiseadm/<%= coreName %>/select?<%= query %>">Query</A><BR -->
						<%
					}
					%>
					<TEXTAREA NAME="SELECT" rows="6" cols="60" class="col-md-12"><%= (request.getParameter("SELECT") != null) ? request.getParameter("SELECT").trim() : "q=*:*" %></TEXTAREA>
					<INPUT TYPE=HIDDEN NAME="coreName" VALUE="<%=coreName %>">
					<BR>
					<INPUT class="btn btn-primary" TYPE="SUBMIT" VALUE="<bean:message key='query'/>">
				</div>
			</div>
			</FORM>
		</div>
		<div class="col-md-6 hide">
			Last search:
			<ul>
				<%
				%>
			</ul>
		</div>
	</div>
	</div>
</div>
<script>
$('#goKnowledgeNo').keypress(function(e) {
    if (e.which == '13') {
       window.location='qaAdminQueue.jsp?q=kid_l:' + $(this).val();
    }
});

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

function cancelAllQueried() {
	if (confirm("<bean:message key='del.all.stop1'/><%= deleteAllQuery %><bean:message key='del.all.stop2'/>")) {
		document.docMgrForm.mode.value = 'cancelAllQueried';
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

$(document).ready(function() {
	$('#goKnowledgeNo').focus();
});
</script>
</BODY>
</HTML>