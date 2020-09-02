<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang3.*"
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
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.util.WiSeUtils"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%--

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
static final String THIS = "qaAdmin2.jsp";
static final String EDITOR = "qaDataEditor2.jsp";
static final String DATATYPE = QAUtil.DATATYPE_CASUAL;

coreName = QAUtil.CASUAL_CORE
 --%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
%>
<%!
static final String THIS = "qaAdminEdit.jsp";
static final String EDITOR = "qaDataEditor.jsp";
static final String DATATYPE = QAUtil.DATATYPE_QA_AUDIT;
static final String PROCESS = "auditEdit";

static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "kid_l", "Name_t", "ANSWER_PLUGIN_ID_s", "AltCount_i", "QUESTION_s", "Status_s","Reject_s", "ANSWER_s", "Title_t", "Date_dt", "TopCategoryName_s", QA.FN_BANNED, QA.FN_IS_NOT_FOR_MLT, QA.FN_IS_NOT_FOR_SEARCH, QA.FN_NOT_APPEND_MLT, "Category_ms", "Name", "LogTime", "updatedAdminUserId_i", "updated_dt", "ANSWER_LINK_s", "ANSWER_RICHTEXT_s"});
static Collection<String> NOT_FOR_LIST_FIELDNAMES = new ArrayList<String>(Arrays.asList(new String[] {"id", "kid_l", "updatedAdminUserId_i", "ANSWER_LINK_s", QA.FN_BANNED, QA.FN_IS_NOT_FOR_MLT, QA.FN_IS_NOT_FOR_SEARCH, QA.FN_NOT_APPEND_MLT, "ANSWER_RICHTEXT_s"}));

public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/" + jsp + "?" + StringUtils.trimToEmpty(query);
	return url;
}

public String buildQueueStatusIcon(QAAltBuildQueue.Status buildStatus,Locale locale) {
	String buildStatusText="";
	if (buildStatus == QAAltBuildQueue.Status.PENDING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PENDING) {
		 buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml4(buildStatus.text));
		return "<div class='text-danger'><span class='text-danger glyphicon glyphicon-time'></span>" + buildStatusText + "</div>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.PROCESSING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PROCESSING) {
		buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml4(buildStatus.text));
		return "<div class='text-warning'><span class='text-warning glyphicon glyphicon-refresh'></span>" + buildStatusText + "</div>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.DONE) {
		buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml4(buildStatus.text));
		return "<div class='text-success'><span class='text-successs glyphicon glyphicon-ok-sign'></span>" + buildStatusText + "</div>";
	}
	buildStatusText = MessageUtil.getMessage(locale, StringEscapeUtils.escapeHtml4(buildStatus.text));
	return "<div class='text-danger'><span class='text-danger glyphicon glyphicon-exclamation-sign'></span>" + buildStatusText + "</div>";
}




public String buildQAStatusIcon(QA qa,Locale locale) {

	StringBuffer sb = new StringBuffer();
	String notRecommended = MessageUtil.getMessage(locale, "not.recommended");
	String notSelected = MessageUtil.getMessage(locale, "not.selected");
	String noAdditionalRecommendation = MessageUtil.getMessage(locale, "no.additional.recommendation");
	String alreadyClose = MessageUtil.getMessage(locale, "already.close");
	if (qa.getIsNotForMLT()) {
		sb.append("<span class='text-danger glyphicon glyphicon-eye-close' title='"+notRecommended+"'></span>");
	}
	else {

	}
	if (qa.getIsNotForSearch()) {
		sb.append("<span class='text-danger glyphicon glyphicon-zoom-out' title='"+notSelected+"'></span>");
	}
	else {

	}
	if (qa.getNotAppendMLT()) {
		sb.append("<span class='text-danger glyphicon glyphicon-sort-by-attributes-alt' title='"+noAdditionalRecommendation+"'></span>");
	}
	else {

	}
	if (qa.getIsBanned()) {
		sb.append("<span class='text-danger glyphicon glyphicon-ban-circle' title='"+alreadyClose+"'></span>");
	}
	else {

	}
	return sb.toString();
}

public String getColumnWidth(String fn) {
	if ("QUESTION_s".equals(fn)) {
		return "col-md-5";
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
    cleanQuery.remove("qt");

    return cleanQuery;
}

%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<jsp:include page="header-qa.jsp"></jsp:include>
<script>
function doAdmin(core, action) {
	if (action == 3) {
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

</script>
<style>
.channelIcon {
	width: 30px;
	height: 30px;
}
</style>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<%
		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
		AdminUser admUser = AdminUserFacade.getInstance().getFromSession(session);
	    boolean showExpired = new Boolean(StringUtils.defaultString(request.getParameter("expired"), "false"));
		String coreName = t.getCoreName();
		CoreContainer cc = SearchManager.getLocalCores();
		SolrServer server = t.getCoreServer4Write();

		Collection<String> fieldNames = new ArrayList<String>();

		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			fieldNames.addAll(core.getSearcher().get().getFieldNames());
		}

		//out.println(core.getInfoRegistry().get("searcher").getStatistics());
		String mode = request.getParameter("mode");
		if ("deleteSelected".equals(mode) && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) {
			String[] docIds = request.getParameterValues("docIds");
			if (docIds != null) {
				System.out.println(admUser.getLoginName() + " deleting by docIds [" + Arrays.asList(docIds) + "].....<BR>");
				
				for (int i = 0; i < docIds.length; i++) {
					out.println("deleting [" + docIds[i] + "].....<BR>");
					server.deleteByQuery("id:" + docIds[i]);
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
	<%
	String searchStr = StringUtils.defaultString(request.getParameter("SEARCH_QUESTION"), request.getParameter("SEARCH_ANSWER"));
	%>
	<div class="row">
		<div class="col-md-8">
			<div class="input-group">
			<input id="SEARCH_FIELD" type="text" class="form-control" value="<%= StringEscapeUtils.escapeHtml4(StringUtils.trimToEmpty(searchStr)) %>">
			<span class="input-group-btn">
			<button id="searchQuestion" class="btn btn-primary" type="button"><bean:message key='query.question'/></button>
			<button id="searchAnswer" class="btn btn-primary" type="button"><bean:message key='query.answer'/></button>
			</span>
			</div>
		</div>
	</div>
	<hr/>
	
	<form name="docMgrForm" action="<%= THIS %>" method="post">
	<input type="hidden" name="mode" value="nothing">
	<input type="hidden" name="modeQuery" value="nothing">
	<input type="hidden" name="coreName" value="<%= coreName %>">
	<div style="display: none">
	<textarea name="SELECT" cols="10" rows="10"><%= request.getParameter("SELECT") != null ? StringEscapeUtils.escapeHtml4(request.getParameter("SELECT")) : "" %></textarea>
	</div>
	<%
	SolrQuery sqry = new SolrQuery();
	sqry.setRequestHandler("/browse");
	sqry.setParam("qf", "QUESTION_t^1000000 QUESTION_ALT_mt^0.01");
	sqry.setParam("mm", "1");
	sqry.setParam("fuzzy", false);
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
	else if (StringUtils.isNotBlank(request.getParameter("SEARCH_QUESTION"))) {
		sqry.setQuery("*:*");
		sqry.addFilterQuery("QUESTION_s:*"+request.getParameter("SEARCH_QUESTION")+"*");
	}
	else if (StringUtils.isNotBlank(request.getParameter("SEARCH_ANSWER"))) {
		sqry.setQuery("*:*");
		sqry.addFilterQuery("ANSWER_s:*"+request.getParameter("SEARCH_ANSWER")+"*");
	}
	else {
		sqry.setQuery("*:*");
	}

	sqry.setFacet(true);
	sqry.setFacetLimit(10);

	if (t.getEnableMultiLocale())
		sqry.addFacetField(QA.FN_QA_LOCALES);
	sqry.addFacetField("CATEGORY_ms");
	sqry.addFacetField("ANSWER_PLUGIN_ID_s");
	sqry.addFacetField("QA_CATEGORY_s");
	sqry.addFacetField("InheritantAltTemplateMkeys_ms");
	sqry.addFacetField("IsKmsRelateExpiredMemo_b");
	sqry.addFacetField("EXPIRE_dt");
	sqry.addFacetField(QA.FN_BANNED);
	sqry.addFilterQuery("{!tag=DF}dataType_s:" + DATATYPE);
	sqry.addFilterQuery("{!tag=DF}"+QA.FN_AUDIT+":" + PROCESS);
	sqry.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");
	if (showExpired)
		sqry.addFilterQuery("EXPIRE_dt:[* TO NOW/DAY]");

	String deleteAllQuery = sqry.getQuery();
	if (sqry.getFilterQueries() != null)
		deleteAllQuery += " AND " + StringUtils.join(sqry.getFilterQueries(), " AND ");

	sqry.setRows(50);
	sqry.setStart(start);
	if (sqry.getFields() == null) {
		sqry.setFields(POSSIBLE_FIELDNAMES.toArray(new String[0]));
	}

	if (StringUtils.isEmpty(sqry.getSortField())) {
		if ("*:*".equals(sqry.getQuery()) && sqry.getStart() == 0) {
			sqry.setSort("kid_l", SolrQuery.ORDER.asc);
		}
		else {
			sqry.setSort("score", SolrQuery.ORDER.desc);
		}
	}

	QueryResponse resp = server.query(sqry);
	SolrDocumentList docList = resp.getResults();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");


	// Dunno why this could happened??!! So just a hack for no result in index.
	if (docList == null) docList = new SolrDocumentList();
    //For simplification, we are not using the JSTL
	SolrQuery sortByQuery = sqry.getCopy();
	String updateTime = MessageUtil.getMessage(locale, "update.time");
	String knowdageNo = MessageUtil.getMessage(locale, "knowledge.num");
	String relevance = MessageUtil.getMessage(locale, "relevance");
	String[] offeredSortOptions = {"updated_dt", "kid_l", "score"};
	String[] offeredSortOptionsName = {updateTime, knowdageNo, relevance};
    %>

	<div class="col-md-12">
	    <div align="left" class="text accordion-inner">
	       	<bean:message key='follow.with'/>
	       	<%--
	       	for (int i=0; i < offeredSortOptions.length; i++) {
	       		if (i > 0) out.println("、");
	       		out.println(
	       				String.format("%s <a href='%s'><span class='glyphicon glyphicon-arrow-up'></span></a><a href='%s'><span class='glyphicon glyphicon-arrow-down'></span></a>",
	       						offeredSortOptionsName[i],
	       						makeUrl(coreName, request, THIS,  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.asc)).toString()),
	       						makeUrl(coreName, request, THIS,  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.desc)).toString())
	       						)
	       				);
	       	}
	       	&nbsp;<bean:message key='sort'/>
	       	&nbsp; &nbsp; &nbsp;
	       	--%>
			<bean:message key='all.search'/><%= docList.getNumFound() %> <bean:message key='show.number'/> <%= start+1 + "-" + (start + sqry.getRows() > docList.getNumFound() ? docList.getNumFound() : start + sqry.getRows()) %> <bean:message key='number'/>

			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<br><bean:message key='jump.knowledge.num'/>
			<input id="goKnowledgeNo" type="text" class="input-xs" size="3">
			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<bean:message key='qa.directly.open'/>
			<input id="goKnowledgeNoDirect" type="text" class="input-xs" size="3">
			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<div class="btn-group">
			<%
			int JUMP_N_PAGE = 5;

			int currPage = (int)Math.floor(start / sqry.getRows())+1;
			if (currPage > 1) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(0);
				%>
				<A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, THIS,  buildCleanQuery(nextPageQuery).toString()) %>">
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
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(prevPageQuery).toString()) %>">
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
				%><A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(toPageQuery).toString()) %>"><%= currPage+po %></A>
			<%
			}
			%>
			<%
			int nextNPageStart = start + sqry.getRows() * JUMP_N_PAGE;

			if (nextNPageStart >= 0 && nextNPageStart < docList.getNumFound()) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(nextNPageStart);
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(nextPageQuery).toString()) %>">
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
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(nextPageQuery).toString()) %>">
				<span class="glyphicon glyphicon-fast-forward"></span>&nbsp;</A>
				<%
			}
			%>
			</div>
	    </div>
    </div>
    <div class="row">
	<div class="col-md-9">
		<TABLE class="table table-striped table-bordered table-hover">
		<%
		int docOffset = 1;
		List<String> fields = new ArrayList<String>(Arrays.asList(sqry.getFields().split(",")));
		fields.retainAll(fieldNames);
		String noWayAsk = MessageUtil.getMessage(locale, "no.way.ask");
		String problemCombination = MessageUtil.getMessage(locale, "problem.combination");
		for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); docOffset++) {
			SolrDocument doc = docItr.next();
			QA qa = new QA(doc);
			String isReject = (String)doc.getFieldValue("Reject_s") == null? "N" : (String)doc.getFieldValue("Reject_s");
			String id = (String)doc.getFieldValue("id");
			Long kid = (Long)doc.getFieldValue("kid_l");
			Integer updateAdminId = (Integer)doc.getFieldValue("updatedAdminUserId_i");
			AdminUser updateAdmin = updateAdminId == null ? null : AdminUserFacade.getInstance().get(updateAdminId);
			String altCountStr = doc.getFieldValue("AltCount_i") == null ? noWayAsk : (problemCombination+"：" + doc.getFieldValue("AltCount_i"));
			String updateAdminName = null;
			if (updateAdminId == null) {
				updateAdminName = "unknown";
			}
			else {
				SolrQuery userQuery = sqry.getCopy();
				updateAdminName =
	       				String.format("<a href='%s'>%s</a>",
	       						makeUrl(coreName, request, THIS,  buildCleanQuery(userQuery.addFilterQuery("updatedAdminUserId_i:" + updateAdminId).setStart(0)).toString()),
	       						(updateAdmin == null ? "gone (" + updateAdminId + ")" : updateAdmin.getLoginName())
	       						);
			}

			String contentUrl = request.getContextPath() + "/wiseadm/" + EDITOR + "?id=" + URLEncoder.encode((String)doc.getFirstValue("id"), "UTF-8");
			String answerPluginId = (String)doc.getFieldValue("ANSWER_PLUGIN_ID_s");
			String answerPluginName = null;
			
			List<MultiChannelAnswer> mcas = MultiChannelAnswer.list(t.getId(), id, QAUserType.DEFAULT_USERTYPE_CODE);
			String showIcons = "";
			for (MultiChannelAnswer mca: mcas) {
				if (mca.getQAChannel() != null) {
				    if (StringUtils.trimToNull(mca.getAnswer()) != null) {
						if ("web".equalsIgnoreCase(mca.getQAChannel().getCode())) {
							showIcons += "<img class='channelIcon' src='" + request.getContextPath() + "/img/icon_Web.png'>&nbsp;";
						}
						else if ("app".equalsIgnoreCase(mca.getQAChannel().getCode())) {
							showIcons += "<img class='channelIcon' src='" + request.getContextPath() + "/img/icon_App.png'>&nbsp;";
						}
						else if ("line".equalsIgnoreCase(mca.getQAChannel().getCode())) {
							showIcons += "<img class='channelIcon' src='" + request.getContextPath() + "/img/icon_LINE.png'>&nbsp;";
						}
						else if ("voice".equalsIgnoreCase(mca.getQAChannel().getCode())) {
							showIcons += "<img class='channelIcon' src='" + request.getContextPath() + "/img/icon_Voice.png'>&nbsp;";
						}
						else if ("fbmessenger".equalsIgnoreCase(mca.getQAChannel().getCode())) {
							showIcons += "<img class='channelIcon' src='" + request.getContextPath() + "/img/icon_FacebookMessenger.png'>&nbsp;";
						}
				    }
				}
				else {
					System.out.println("QAChannel Not Found! :: " + mca);
				}
			}

			if (answerPluginId != null)
				answerPluginName = QAPlugins.get(answerPluginId).getName();

			%>
			<% if (docOffset==1) { // 印出欄位名稱 
				String[] headerFnInOrder = {"kid_l", "QUESTION_s", "ANSWER_s", "Status_s", "updated_dt"};
				String[] headerWidthInOrder = {"col-lg-2", "col-lg-4", "col-lg-4", "col-lg-4", "col-lg-2"};
				Boolean[] headerSortableInOrder = {true, true, false, false, true};
			%>
			<THEAD>
			<TR>
				<TH style="width: 30px;">
					<a class="btn btn-xs btn-danger" href="<%= EDITOR %>"><span class="glyphicon glyphicon-plus-sign"></span></a>
				</TH>
				<% for (int i=0; i < headerFnInOrder.length; i++) { %>
				<TH <%= headerWidthInOrder[i] != null ? "class='" + headerWidthInOrder[i] + "'" : "style='width: 30px;'" %>>
					<%
					{ String fn = headerFnInOrder[i];
					  if (headerSortableInOrder[i]) {
		       		  	out.println(
		       				String.format("%s <a href='%s'><span class='glyphicon glyphicon-arrow-up'></span></a><a href='%s'><span class='glyphicon glyphicon-arrow-down'></span></a>",
		       						ColumnNameMappingFacade.getInstance().getMappingName(fn),
		       						makeUrl(coreName, request, THIS,  buildCleanQuery(sortByQuery.setSortField(fn, SolrQuery.ORDER.asc)).toString()),
		       						makeUrl(coreName, request, THIS,  buildCleanQuery(sortByQuery.setSortField(fn, SolrQuery.ORDER.desc)).toString())
		       						)
		       				);
					  }
					  else {
						  out.println(ColumnNameMappingFacade.getInstance().getMappingName(fn));
					  }
					}
					%>
				</TH>
				<% } %>
			</TR>
			</THEAD>
			<% } %>
			<TR>
				<TD class="text-center" style="width: 30px;"><input type=checkbox name="docIds" value="<%= id %>"></TD>
				<TD style="width: 30px;">
					<A TARGET="_blank" HREF="<%= contentUrl %>">
					<%= kid == null ? StringUtils.substringAfter(id, "-") : kid %>
					</A>
					<div style="margin-top: 10px;">
						<div class="row">
						<div class="col-md-12"><small><%= buildQueueStatusIcon(QAAltBuildQueue.getStatus(t.getId(), id), locale) %></small></div>
						</div>
						<div class="row">
						<div class="col-md-12"><small><%= buildQAStatusIcon(qa,locale) %></small></div>
						</div>
					</div>
				</TD>
				<% { String fn = "QUESTION_s"; String valStr = doc.getFirstValue(fn) == null ? "" : doc.getFirstValue(fn).toString(); %>
				<td style="word-break: break-all;">
					<div class="">
					<A TARGET="_blank" HREF="<%= contentUrl %>"><%= StringUtils.left(StringEscapeUtils.escapeHtml4(valStr), 500) %></A>
					</div>
					<div style="margin-top: 10px;">
						<div class="row">
						<div class="col-xs-12"><small><span class="glyphicon glyphicon-plus-sign"></span><%= StringUtils.defaultString(MessageUtil.getMessage(locale, answerPluginName), MessageUtil.getMessage(locale, "no.plug.in")) %></small></div>
						</div>
						<div class="row">
						<div class="col-xs-12"><small><%= altCountStr %></small></div>
						</div>
					</div>
				</td>
				<% } %>
				<% { String fn = "ANSWER_s"; String valStr = doc.getFirstValue(fn) == null ? "" : doc.getFirstValue(fn).toString(); %>
				<td>
					<div class="col-md-12" style="word-break: break-all;">
					<%= StringUtils.left(StringEscapeUtils.escapeHtml4(valStr), 300) %>
					</div>
					<%= showIcons %>
				</td>
				<% } %>                                
				<% { String fn = "Status_s"; String valStr = doc.getFirstValue(fn) == null ? "" : doc.getFirstValue(fn).toString(); %>
				<td style="word-break: break-all;">
					<% if (isReject.equals("Y")) { %>
						<font color="red">駁回</font></br>說明:<%= valStr %>
					<% }else {%>
						<font color="blue">編輯中</font></br>說明:<%= valStr %>
					<% } %>
				</td>
				<% } %>
				<% { String fn = "updated_dt"; String valStr = doc.getFirstValue(fn) == null ? "" : sdf.format((Date)doc.getFirstValue(fn)); %>
				<td>
					<div class="row">
						<div class="col-md-12"><small><span class="glyphicon glyphicon-user"></span><%= StringUtils.defaultString(updateAdminName, "unknown") %></small></div>
					</div>
					
					<div class="row">
						<div class="col-md-12"> <%= StringUtils.left(StringEscapeUtils.escapeHtml4(valStr), 100) %> </div>
					</div>
				</td>
				<% } %>
			</TR>
		<%
		}
		%>
		</TABLE>
	</div>
	<div class="col-md-3">
		<% if (t.getEnableMultiLocale()) { %>
		<div>
			<h4><bean:message key='qa.locales'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField(QA.FN_QA_LOCALES);
			for (FacetField.Count ffc: ff.getValues()) {
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				%>
				<li><A class=""
				   HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><%= ffc.getName() %> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}
			}
			%>
			</ul>
	    </div>
	    <% } %>
		<div>
			<h4><bean:message key='global.category'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField("CATEGORY_ms");
			for (FacetField.Count ffc: ff.getValues()) {
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				%>
				<li><A class=""
				   HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><%= ffc.getName() %> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}
			}
			%>
			</ul>
	    </div>
		<div>
			<h4><bean:message key='plug.in'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField("ANSWER_PLUGIN_ID_s");
			for (FacetField.Count ffc: ff.getValues()) {
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				%>
				<li><A class=""
				   HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><%= MessageUtil.getMessage(locale, QAPlugins.get(ffc.getName()).getName()) %> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}
			}
			%>
			</ul>
	    </div>
	    <div>
			<h4><bean:message key='qacategory'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField("QA_CATEGORY_s");
			for (FacetField.Count ffc: ff.getValues()) {
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				%>
				<li><A class=""
				   HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><%=(StringUtils.isBlank(ffc.getName()) ? "通用" : ffc.getName()) %> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}
			}
			%>
			</ul>
	    </div>
	    <div>
			<h4><bean:message key='inheritantAltTemplate'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS);
			for (FacetField.Count ffc: ff.getValues()) {
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				String mkey = ffc.getName();
				
				QAAltTemplate qaAltTpl = QAAltTemplate.getByKey(t.getId(), mkey);
				%>
				<li><A class=""
				   HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><%= qaAltTpl != null ? qaAltTpl.getName() : "<del>" + mkey + "</del>" %> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}
			}
			%>
			</ul>
	    </div>
	    <div>
			<h4><bean:message key='qa.status'/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField ff = resp.getFacetField("IsKmsRelateExpiredMemo_b");
			for (FacetField.Count ffc: ff.getValues()) {
				if("false".equalsIgnoreCase(ffc.getName())) continue;
				SolrQuery ffq = sqry.getCopy();
				ffq.setStart(0);
				ffq.addFilterQuery(ffc.getAsFilterQuery());
				%>
				<li><A class="" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq).toString()) %>"
					><bean:message key="kms.relate.expired.memo"/> <span class="badge"><%= ffc.getCount() %></span></A></li>
				<%
			}

			FacetField banned_ff = resp.getFacetField(QA.FN_BANNED);
			long total = docList.getNumFound();
			long bannedCount = 0;
			for (FacetField.Count ffc: banned_ff.getValues()) {
				if("true".equalsIgnoreCase(ffc.getName())) {
					bannedCount = ffc.getCount();
					break;
				}
			}
			SolrQuery ffq_notBanned = sqry.getCopy()
					.setStart(0)
					.addFilterQuery("-" + QA.FN_BANNED + ":true");
			SolrQuery ffq_banned = sqry.getCopy()
					.setStart(0)
					.addFilterQuery(QA.FN_BANNED + ":true");
			%>
				<li><A class="" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq_notBanned).toString()) %>"
					><bean:message key="global.able"/> <span class="badge"><%= total-bannedCount %></span></A></li>
				<li><A class="" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq_banned).toString()) %>"
					><bean:message key="global.disable"/> <span class="badge"><%= bannedCount %></span></A></li>
			<%
			}
			%>
			</ul>
	    </div>
	    <div>
			<h4><bean:message key="custom.expiration.date"/></h4>
			<ul class="list-unstyled">
			<%
			{
			FacetField expire_ff = resp.getFacetField("EXPIRE_dt");
			SimpleDateFormat sdfExpire = new SimpleDateFormat("yyyy-MM-dd HH");
			Date dateNow = new Date();
			long total = docList.getNumFound();
			long expireCount = 0;
			long unExpireCount = 0;
			for (FacetField.Count ffc: expire_ff.getValues()) {
				Date dateExpire = sdfExpire.parse(ffc.getName().replace("T", " ").substring(0, 13));
				long timeExpire = dateExpire.getTime() + (60*60*8*1000);
				dateExpire.setTime(timeExpire);
				if(dateExpire.before(dateNow)){
					expireCount += ffc.getCount();
				} else {
					unExpireCount += ffc.getCount();
				}
			}
			SolrQuery ffq_notExpire = sqry.getCopy()
					.setStart(0)
					.addFilterQuery("EXPIRE_dt:[NOW/DAY+1DAY TO *]");
			SolrQuery ffq_expire = sqry.getCopy()
					.setStart(0)
					.addFilterQuery("EXPIRE_dt:[* TO NOW/DAY]");
			%>
				<li><A class="" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq_notExpire).toString()) %>"
					><bean:message key="global.able"/> <span class="badge"><%= unExpireCount %></span></A></li>
				<li><A class="" HREF="<%= makeUrl(coreName, request, THIS, buildCleanQuery(ffq_expire).toString()) %>"
					><bean:message key="global.expired"/> <span class="badge"><%= expireCount %></span></A></li>
			<%
			}
			%>
			</ul>
	    </div>
	</div>
    </div>

	<div class="row">
	<div class="col-md-12">
		<script>
		</script>
		<% if (!showExpired && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) { %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<button class="btn btn-danger" type="button" onclick="return deleteSelected();"><bean:message key='delete.select'/></button>
		</div>
		<% } %>
		&nbsp;&nbsp;&nbsp;
		<div class="btn-group">
		<a class="btn btn-success" href="<%= EDITOR %>"><bean:message key='global.add'/></a>
		</div>

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
								&nbsp;<bean:message key='add.ons'/><input type='checkbox' name='append' value='<%= key %>' >
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



	<div class="row" style="margin-top:10px;">
	<div class="col-md-12">
		<div class="col-md-6">
			<FORM id="selectArea" action="<%= THIS %>">
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
						<%
					}
					%>
					<TEXTAREA NAME="SELECT" rows="6" cols="60" class="col-md-12"><%= (request.getParameter("SELECT") != null) ? request.getParameter("SELECT").trim() : "q=*:*" %></TEXTAREA>
					<INPUT TYPE=HIDDEN NAME="coreName" VALUE="<%= coreName %>">
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
       window.location='qaAdmin.jsp?q=kid_l:' + $(this).val();
    }
});

$('#goKnowledgeNoDirect').keypress(function(e) {
    if (e.which == '13') {
       window.location='qaDataEditor.jsp?id=COMMON_SENSE-' + $(this).val();
    }
});

$('#searchQuestion').click(function(e) {
    window.location='qaAdmin.jsp?SEARCH_QUESTION=' + $('#SEARCH_FIELD').val();
});

$('#searchAnswer').click(function(e) {
    window.location='qaAdmin.jsp?SEARCH_ANSWER=' + $('#SEARCH_FIELD').val();
});

$(document).ready(function() {
	$('#goKnowledgeNo').focus();
});
</script>
</BODY>
</HTML>