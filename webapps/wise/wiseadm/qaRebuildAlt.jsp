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
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%!
static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "kid_l", "QUESTION_s", "ANSWER_s", "updatedAdminUserId_i", "updated_dt", "QUESTION_ALT_TPL_ms", "ANSWER_LINK_s", "ANSWER_RICHTEXT_s"});
static Collection<String> NOT_FOR_LIST_FIELDNAMES = new ArrayList<String>(Arrays.asList(new String[] {"id", "kid_l", "updatedAdminUserId_i", "ANSWER_LINK_s", "ANSWER_RICHTEXT_s", "QUESTION_ALT_TPL_ms"}));

public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/" + jsp + "?coreName=" + coreName + (query == null ? "" : ("&" + (query)));
	return url;
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
<TITLE>問答資料管理</TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<%
	    boolean showExpired = new Boolean(StringUtils.defaultString(request.getParameter("expired"), "false"));
		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
		String coreName = t.getCoreName();
		CoreContainer cc = SearchManager.getLocalCores();

		Collection<String> fieldNames = new ArrayList<String>();

		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			fieldNames.addAll(core.getSearcher().get().getFieldNames());
		}
	%>
	<br>
	<div><A NAME="<%= coreName %>"></A>
      <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
      </a>
	</div>
	<div id="progress" class="progress">
    	<div id="progress-bar" class="progress-bar" style="width: 0%;"></div>
	</div>
	<form name="docMgrForm" action="qaRebuildAlt.jsp" method="post">
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
	
	sqry.addFilterQuery("{!tag=DF}dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
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

	SolrServer server = t.getCoreServer4Write();
	QueryResponse resp = server.query(sqry);
	SolrDocumentList docList = resp.getResults();
	SimpleDateFormat sdf = new SimpleDateFormat();
	
	// Dunno why this could happened??!! So just a hack for no result in index.
	if (docList == null) docList = new SolrDocumentList();
    //For simplification, we are not using the JSTL
	SolrQuery sortByQuery = sqry.getCopy();
			
	String[] offeredSortOptions = {"updated_dt", "kid_l", "score"};
	String[] offeredSortOptionsName = {"更新時間", "知識編號", "相關性"};
    %>
    
	<div class="row">
	<div class="col-md-12">
	    <div align="left" class="text accordion-inner">
	       	依照
	       	<%
	       	for (int i=0; i < offeredSortOptions.length; i++) {
	       		if (i > 0) out.println("、");
	       		out.println(
	       				String.format("%s <a href='%s'><span class='glyphicon glyphicon-arrow-up'></span></a><a href='%s'><span class='glyphicon glyphicon-arrow-down'></span></a>",
	       						offeredSortOptionsName[i],
	       						makeUrl(coreName, request, "qaRebuildAlt.jsp",  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.asc)).toString()),
	       						makeUrl(coreName, request, "qaRebuildAlt.jsp",  buildCleanQuery(sortByQuery.setSortField(offeredSortOptions[i], SolrQuery.ORDER.desc)).toString())
	       						)
	       				);
	       	}
	       	%>
	       	&nbsp;排序
	       	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			共查詢到 <%= docList.getNumFound() %> 筆 / 目前顯示第 <%= start+1 + "-" + (start + sqry.getRows() > docList.getNumFound() ? docList.getNumFound() : start + sqry.getRows()) %> 筆
			<div class="btn-group">
			<%
			int currPage = (int)Math.floor(start / sqry.getRows())+1;
			if (currPage > 1) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart(0);
				%>
				<A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "qaRebuildAlt.jsp",  buildCleanQuery(nextPageQuery).toString()) %>">
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
				%><A class="btn btn-default"  HREF="<%= makeUrl(coreName, request, "qaRebuildAlt.jsp", buildCleanQuery(toPageQuery).toString()) %>"><%= currPage+po %></A>
			<%
			}
			%>
			<%
			int lastPage = (int)Math.floor((docList.getNumFound()-1) / sqry.getRows()) + 1;
			if (lastPage != currPage) {
				SolrQuery nextPageQuery = sqry.getCopy();
				nextPageQuery.setStart((lastPage-1) * sqry.getRows());
				%>
				<A class="btn btn-default" HREF="<%= makeUrl(coreName, request, "qaRebuildAlt.jsp", buildCleanQuery(nextPageQuery).toString()) %>">
				<span class="glyphicon glyphicon-arrow-right"></span>&nbsp;</A>
				<%
			}
			%>
			</div>
	    </div>
		</div>
    </div>
	<div class="row">
	<div class="col-md-12">
		<TABLE class="table table-striped table-bordered table-hover">
		<%
		int docOffset = 1;
		long total = docList.getNumFound();
		long currPos = 0;
		int lastPercent = 0;
		List<String> fields = new ArrayList<String>(Arrays.asList(sqry.getFields().split(",")));
		fields.retainAll(fieldNames);
		
		boolean run = true;
		boolean dryRun = new Boolean(StringUtils.defaultString(request.getParameter("dryRun"), "true"));
		
		
		while (run) {
			
			for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); docOffset++) {
				SolrDocument doc = docItr.next();
				currPos++;
				String id = (String)doc.getFieldValue("id");
				Long kid = (Long)doc.getFieldValue("kid_l");
				String question = (String)doc.getFirstValue("QUESTION_s");
				String contentUrl = request.getContextPath() + "/wiseadm/qaDataEditor.jsp?id=" + URLEncoder.encode((String)doc.getFirstValue("id"), "UTF-8");
				
				List<String> qaTPL = (List<String>)doc.getFieldValue("QUESTION_ALT_TPL_ms");

				QAAltBuild build = new QAAltBuild();
				build.setCreatedTime(Calendar.getInstance().getTime());
				build.setDocId(id);
				build.setQAltTpls(StringUtils.join(qaTPL, "\n"));

				QA docNew = new QA();
				docNew.setId(id);
				docNew.setKid(kid);
				docNew.setQuestion(question);
				docNew.setQuestionAltTemplates(qaTPL);
				
				QASaver saver = new QASaver(build, docNew, question);

				if (!dryRun) {
					saver.save();
				}
			
				int percent = Math.round((float)currPos * 100f / (float)total);
				
				if (lastPercent != percent) {
					lastPercent = percent;
					
					if (percent % 5 == 0) {
						out.println("<script>$('#progress-bar').css({'width':'" + percent + "%'});</script>");
						out.flush();
					}
				}
				%>
				<% if (docOffset==1) { // 印出欄位名稱 %>
				<THEAD>
				<TR>
					<TH width="10">　</TH><TH width="10">知識編號</TH>
					<%
					for (String fn: fields) {
						if (NOT_FOR_LIST_FIELDNAMES.contains(fn))
							continue;
						%>
						<TH><%= ColumnNameMappingFacade.getInstance().getMappingName(fn) %></TH>
					<%
					}
					%>
					<TH>更新者</TH>
					<TH>進階<BR>回答</TH>
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
						<td class="<%= "QUESTION_s".equals(fn) || "ANSWER_s".equals(fn) ? "col-md-4" : "col-md-2" %>">
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
			       						makeUrl(coreName, request, "qaRebuildAlt.jsp",  buildCleanQuery(userQuery.addFilterQuery("updatedAdminUserId_i:" + updateAdminId).setStart(0)).toString()),
			       						(updateAdmin == null ? "gone (" + updateAdminId + ")" : updateAdmin.getLoginName())
			       						)
			       				);
					}
					
					%>
					</td>
					<td>
					<%
					if (hasRichtext) {
						%>格式文字<%
					}
					else if (hasExternalLink) {
						%>外部連結<%
					}
					else {
						%>無進階<%
					}
					%>
					</td>
				</TR>
			<%
			}
			
			if (currPos < total - 1) {
				sqry.setStart((int)currPos);
				resp = server.query(sqry);
				docList = resp.getResults();
			}
			else {
				break;
			}
		}

		if (!dryRun) {
			server.commit(true, true, false);
		
			try {
				// wait for softCommit
				Thread.sleep(1000);
			} catch (InterruptedException ignore) {
			} 
		}
		%>
		</TABLE>
	</div>
	</div>
	</form>
</div>
</BODY>
</HTML>