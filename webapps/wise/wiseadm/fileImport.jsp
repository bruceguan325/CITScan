<%@page import="java.util.Locale"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.apache.solr.client.solrj.SolrServer"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.solr.SearchManager"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.intumit.solr.robot.ProcessQADataServlet"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.supercsv.prefs.CsvPreference"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="org.supercsv.io.CsvListReader"%>
<%@page import="org.supercsv.io.ICsvListReader"%>
<%@page import="java.io.InputStream"%>
<%@page import="javax.persistence.UniqueConstraint"%>
<%@page import="com.intumit.solr.util.fileupload.FileMeta"%>
<%@page import="com.intumit.solr.robot.dictionary.*"%>
<%@page import="com.intumit.solr.util.WiSeUtils"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.tenant.Tenant" %>
<%@ page import="com.intumit.solr.robot.QAAltBuild" %>
<%@ page import="com.intumit.solr.robot.QAAltBuildQueue" %>
<%@ page import="com.intumit.solr.robot.QAAltBuildQueue.Status" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%!
  int getProcessingQACount(Tenant t) {
	  List<QAAltBuild> currentQueue = QAAltBuild.listByTenant(t.getId());
	  int cnt = 0;
	  for(QAAltBuild build : currentQueue) {
		  Status s = QAAltBuildQueue.getStatus(t.getId(), build.getDocId());
		  if(!Status.DONE.equals(s) && !Status.FAILED.equals(s)) {
			  cnt++;
		  }
	  }
	  return cnt;
  }
%>
<%
Locale locale= (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
Tenant t = Tenant.getFromSession(session);
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0 || getProcessingQACount(t) > 0) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}

int value = Integer.parseInt(request.getParameter("f"));
List<FileMeta> files = (List<FileMeta>) request.getSession().getAttribute("uploaded");
FileMeta getFile = files.get(value);
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<title><bean:message key='data.upload.page'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<!-- we code these -->
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h3><bean:message key='knowledge.batch.import.test'/><br></h3>
	<%
	if (getFile != null) {
		InputStream input = getFile.getContent();
		if (input.available() == 0) {
	%>
	<div class="alert alert-warning">File content expired, please upload it again.</div>
	<%
		} else {
			ICsvListReader listReader = null;
			try {
				String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
				System.out.println("Detected charset:" + charset + " [" + getFile.getFileType() + "]");
				input.reset();

				if ("text/csv".equalsIgnoreCase(getFile.getFileType())) {
					listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.EXCEL_PREFERENCE);
				}
				else {
					listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.TAB_PREFERENCE);
				}
				String[] headers = ProcessQADataServlet.getQaDataHeaders(listReader);
				System.out.println(Arrays.asList(headers));
	%>
	<% 
		// 為了先取總比數，在前面先讀檔案存List
		List<List<String>> lines = new ArrayList<List<String>>();
		List<String> cells = null;
		int total = 0;
		while ((cells = listReader.read()) != null) {
			lines.add(cells);
			total++;
		}
	%>
	<a id="importBtn" href="processData?f=<%= value %>&dryRun=false&action=import&filename=<%= getFile.getFileName() %>&total=<%= total%>" class="btn btn-default"><bean:message key='global.import'/></a>
	&nbsp;
	<a href="fileUpload.jsp" class="btn btn-default"><bean:message key='global.cancel'/></a>
	<br/><br/>
	<table class="table table-striped table-hover">
		<tr>
			<th><bean:message key='smart.robot.knowledge.no'/></th>
			<th><bean:message key='standard.problem'/></th>
			<th><bean:message key='the.same.standard.problem'/></th>
			<th><bean:message key='similar.problems'/></th>
			<th><bean:message key="template.apply"/></th>
			<th><bean:message key="knowledge.point"/></th>
			<th><bean:message key='hierarchical.qa.check'/></th>
		</tr>
	<%
		try {
			float minScore = QAUtil.MLT_COMMON_SENSE_Q_MIN_SCORE * 2;

			int maxBatchUploadSize = t.getMaxBatchUploadSize();

			com.intumit.solr.robot.QAContext qaCtx = new com.intumit.solr.robot.QAContext();
			qaCtx.setClientSupportHtml(false);
			qaCtx.setTenant(t);
			QAUtil qu = QAUtil.getInstance(t);

			SolrServer server = t.getCoreServer();
			System.out.println("Headers (" + headers.length + ")[" + Arrays.asList(headers) + "]");
			int cnt = 0;
			boolean limitShown = false; 
			for(List<String> line:lines){
				cnt++;
				cells = ProcessQADataServlet.truncateTailNulls(line);
		
				//System.out.println(cells);
				String id = null;
				String indexId = null;
				String question = null;
				String kpStr = null;
				String qaAltTemplates = null;
				String hQaData = null;
				String qaCategory = null;
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					String header = headers[i];
					System.out.println("[" + header + "]");
					String val = cells.get(i);
					if ("id".equalsIgnoreCase(header)){
						id = val;
						indexId = QAUtil.DATATYPE_COMMON_SENSE + "-" + val;
					}else if("QUESTION".equalsIgnoreCase(header)){
						question = StringUtils.trimToEmpty(val);
					}else if("HIERARCHICAL_QA".equalsIgnoreCase(header)){
						hQaData = StringUtils.trimToEmpty(val);
					}else if("ALT_TEMPLATE".equalsIgnoreCase(header)){
						qaAltTemplates = StringUtils.trimToEmpty(val);
					}else if("KNOWLEDGE_POINT".equalsIgnoreCase(header)){
						kpStr = StringUtils.trimToEmpty(val);
					}else if("QA_CATEGORY_S".equalsIgnoreCase(header)){
						qaCategory = StringUtils.trimToEmpty(val);
					}
				}
				boolean existed = false;
				boolean sameQuestion = false;
				if (indexId != null) {
					SolrQuery q = new SolrQuery();
					q.setQuery("id:\"" + URLEncoder.encode(indexId, "UTF-8") + "\"");
					SolrDocumentList result = server.query(q).getResults();
					if (result.getNumFound() > 0){
						existed = true;
						sameQuestion =
								StringUtils.trimToEmpty((String)result.get(0).getFieldValue("QUESTION_s")).equals(question);
					}
				}
				Map<String ,String> similarQs1 = new LinkedHashMap<String, String>();
				Map<String ,String> similarQs2 = new LinkedHashMap<String, String>();
				qaCtx.setRestrictToQaCategory(qaCategory);
				
				System.out.println(cells + "(" + cells.size() + ") [" + question + "]");
				String preCleaned = QAUtil.preReconstructClean(question);
				
				DictionaryDatabase[] kws = OtherPossibleQuestionsBySearchDictionary.search(qaCtx.getTenant().getId(), preCleaned.toCharArray());
				StringBuffer sb = new StringBuffer();
				StringBuffer forView = new StringBuffer();
				
				for (DictionaryDatabase kw: kws) {
					if (sb.length() > 0) {
						sb.append(" ");
						forView.append("、");
					}
					
					sb.append(WiSeUtils.dblQuote(kw.getKeyword()));
					forView.append(kw.getKeyword());
				}
				
				SolrDocumentList searched = qu.searchCommonSense(sb.toString(), qaCtx, true, false, false, 0, 5);
				if(searched != null){
					for (SolrDocument doc: searched) {
						float score = (Float) doc.getFieldValue("score");
						String dId = (String) doc.getFieldValue("id");
						if(!dId.equals(indexId) && score > minScore){
							similarQs1.put(dId, (String) doc.getFieldValue("QUESTION_s"));
						}
					}
				}
				
				SolrDocumentList docs = QAUtil.getInstance(t).mltCommonSense(question, qaCtx, 5);
				if(docs != null){
					for (SolrDocument doc: docs) {
						float score = (Float) doc.getFieldValue("score");
						String dId = (String) doc.getFieldValue("id");
						if(!dId.equals(indexId) && score > minScore){
							similarQs2.put(dId, (String) doc.getFieldValue("QUESTION_s"));
						}
					}
				}
			%>
			<% if(cnt > maxBatchUploadSize && !limitShown) { limitShown = true;%>
	    <tr>
	      <td colspan="7" style="text-align:center;"><span style="color:red;font-weight:bolder;font-size:3em;"><bean:message key="over.maxBatchUploadSize.disable.import"/></span></td>
	    </tr>
			<% } %>
		<tr>
			<td><%= StringEscapeUtils.escapeHtml(id) %></td>
			<td><%= StringEscapeUtils.escapeHtml(question) %></td>
			<td>
				<% if(existed){ %><a target="_blank" href="qaDataEditor.jsp?id=<%= URLEncoder.encode(indexId, "UTF-8") %>"><% } %>
				<%= sameQuestion?MessageUtil.getMessage(locale, "global.yes"):MessageUtil.getMessage(locale, "global.no") %>
				<% if(existed){ %></a><% } %>
			</td>
			<td>
			<%
			if(similarQs1.isEmpty()){
			%>
			<bean:message key='global.without'/>1 &nbsp; <%= sb.toString() %>
			<%
			}else{
			%>
				<%= sb.toString() %>
				<ul>
				<%
				for(Entry<String, String> e : similarQs1.entrySet()){
				%>
					<li>
						<a target="_blank" href="qaDataEditor.jsp?id=<%= URLEncoder.encode(e.getKey(), "UTF-8") %>">
							<%= StringEscapeUtils.escapeHtml(e.getValue()) %>
						</a>
					</li>
				<%
				}
				%>
				</ul>
			<%
			}
			%>
			<hr/>
			<%
			if(similarQs2.isEmpty()){
			%>
			<bean:message key='global.without'/>2
			<%
			}else{
			%>
				<ul>
				<%
				for(Entry<String, String> e : similarQs2.entrySet()){
				%>
					<li>
						<a target="_blank" href="qaDataEditor.jsp?id=<%= URLEncoder.encode(e.getKey(), "UTF-8") %>">
							<%= StringEscapeUtils.escapeHtml(e.getValue()) %>
						</a>
					</li>
				<%
				}
				%>
				</ul>
			<%
			}
			%>
			</td>
			<td><%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(qaAltTemplates)) %></td>
			<td><%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(kpStr)) %></td>
			<td>
				<%
				if(StringUtils.isBlank(hQaData)){
				%>
				<bean:message key='have.no.data'/>
				<%
				}else{
					try{
						new org.json.JSONArray(hQaData);//QAPlugins.parseHQAData(hQaData);
				%>
				<span class="text-success"><bean:message key='global.normal'/></span>
				<%
					}catch(Exception e){
				%>
				<span class="text-danger"><bean:message key='format.error'/>：<%= StringEscapeUtils.escapeHtml(e.getMessage()) %>[<%= hQaData %>]</span>
				<%
					}
				}
				%>
			</td>
		</tr>
	<%
			out.flush();
		}
	}catch(Exception e){ e.printStackTrace(); }
	%>
	</table>
	<%
			}finally{
				IOUtils.closeQuietly(listReader);
			}
		}
	}
	%>
</div>
<script>
$(document).ready(function() {

});
</script>
</body>
</html>
