<%@page import="com.intumit.solr.robot.qaplugin.HierarchicalQA"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.regex.*"%>
<%@page import="javax.persistence.UniqueConstraint"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page import="org.apache.solr.client.solrj.SolrServer"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.wink.json4j.*"%>
<%@page import="org.supercsv.io.CsvListReader"%>
<%@page import="org.supercsv.io.ICsvListReader"%>
<%@page import="org.supercsv.prefs.CsvPreference"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="com.intumit.solr.robot.*"%>
<%@page import="com.intumit.solr.SearchManager"%>
<%@page import="com.intumit.solr.util.fileupload.FileMeta"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %><%!

void appendToHierarchicalJSON(JSONObject root, int lv, String question, String candidateText, String answer, boolean EOL) throws Exception {
	if (lv < 2) return;
	int depth = lv - 1;
	JSONObject ta = root;

	for (int i=1; i < depth; i++) {

		if (ta.has("children")) {
			JSONArray children = ta.getJSONArray("children");

			// 最後一個選項，因為選項是一直往後面 append 上去
			ta = children.getJSONObject(children.length() - 1);
		}
	}

	if (!ta.has("children")) {
		JSONArray children = new JSONArray();
		ta.put("children", children);
	}

	if (ta != null) {
		if (StringUtils.isNotEmpty(question))
			ta.put("question", StringUtils.defaultString(question));

		JSONObject rs = new JSONObject();
		String id = UUID.randomUUID().toString();
		rs.put("id", id);
		rs.put("text", StringUtils.defaultString(candidateText));
		rs.put("answer", StringUtils.defaultString(answer));

		ta.getJSONArray("children").put(rs);
	}
}

%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}

int value = Integer.parseInt(request.getParameter("f"));
boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("dryRun"), "true"));
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
	<h3><bean:message key='hierarchical.data.import.check'/> <br></h3>
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
				System.out.println("Detected charset:" + charset);
				input.reset();

				if ("text/csv".equalsIgnoreCase(getFile.getFileType())) {
					listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.EXCEL_PREFERENCE);
				}
				else {
					listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.TAB_PREFERENCE);
				}
				String[] headers = ProcessQADataServlet.getNonJsonHierarchicalQaDataHeaders(listReader);
				System.out.println(Arrays.asList(headers));
	%>
	<a href="fileUpload?c=i&f=<%= value %>&dryRun=false" class="btn btn-default"><bean:message key='global.import'/></a>
	&nbsp;
	<a href="fileUpload.jsp" class="btn btn-default"><bean:message key='global.cancel'/></a>
	<br/><br/>
	<table class="table table-striped table-hover">
		<tr>
			<th><bean:message key='smart.robot.knowledge.no'/></th>
			<th><bean:message key='standard.problem'/></th>
			<th><bean:message key='kinds.of.questions'/></th>
			<th><bean:message key='the.same.standard.problem'/></th>
			<th><bean:message key='hierarchical.qa.check'/></th>
		</tr>
	<%
	List<String> cells = null;
	try {
		float minScore = QAUtil.MLT_COMMON_SENSE_Q_MIN_SCORE * 2;

		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
		QAUtil qu = QAUtil.getInstance(t);
		SolrServer server = t.getCoreServer4Write();
		JSONObject root = null;
		String question = null;
		String directAnswer = null;
		List<String> qaTPL = null;
		Long currKid = null;
		Long kidForFinishQuestion = null;

		while (true) {
			cells = listReader.read();
			boolean finishQuestion = false;

			// 找 ID，若發現 ID 欄位或者 cells == null 代表本題結束
			if (cells != null) {
				cells = ProcessQADataServlet.truncateTailNulls(cells);

				//System.out.println(cells);
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					String header = headers[i];
					String val = cells.get(i);

					if ("id".equalsIgnoreCase(header) && val != null){
						kidForFinishQuestion = currKid;
						finishQuestion = true;
						currKid = new Long(val);
					}
				}
			}
			else {
				kidForFinishQuestion = currKid;
				finishQuestion = true;
			}

			if (finishQuestion) {
				if (root != null) {
					// 這裡應該要進行存檔（如果不是 dryRun）
					boolean existed = false;
					boolean sameQuestion = false;

					Long kid = kidForFinishQuestion;
					String indexId = QAUtil.DATATYPE_COMMON_SENSE + "-" + kidForFinishQuestion;
					QA qa = null;

					if (indexId != null) {
						SolrDocument mainDoc = qu.getMainQASolrDocument(new Long(kid), true);

						if (mainDoc != null) {
							existed = true;
							sameQuestion =
									StringUtils.trimToEmpty((String)mainDoc.getFieldValue("QUESTION_s")).equals(question);

							qa = new QA(mainDoc);
						}
					}
					if (qa == null) qa = new QA();

					if (!dryRun) {
						QAAltBuild build = new QAAltBuild();
						build.setCreatedTime(Calendar.getInstance().getTime());
						build.setDocId(indexId);
						build.setQAltTpls(StringUtils.join(qaTPL, "\n"));
						Date now = new Date(System.currentTimeMillis());
						qa.setId(indexId);
						qa.setKid(new Long(kid));
						qa.setQuestion(question);
						qa.setAnswer(directAnswer);
						qa.setQuestionAltTemplates(qaTPL);
						qa.setCreateInfo(now, user);
						qa.setUpdateInfo(now, user);

						// 多層次部分
						qa.setField(HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME, root.toString(2));
						qa.setField("ANSWER_PLUGIN_ID_s", QAPlugins.HQA_ID);

						server.add(qa);
						server.commit(true, true, false);
						
						try {
							// wait for softCommit
							Thread.sleep(1000);
						} catch (InterruptedException ignore) {
						} 
						QAAltBuildQueue.add(t.getId(), indexId, kid, qa.getQuestionAltTemplates(), user.getLoginName());
					}
					%>
						<tr>
							<td><%= kid %></td>
							<td><%= StringEscapeUtils.escapeHtml(question) %></td>
							<td><%= "<ul><li>" + StringUtils.join(qaTPL,"</li><li>") + "</li></ul>" %></td>
							<td>
								<% if(existed){ %><a target="_blank" href="qaDataEditor.jsp?id=<%= URLEncoder.encode(indexId, "UTF-8") %>"><% } %>
								<%= sameQuestion?MessageUtil.getMessage(locale, "global.yes"):MessageUtil.getMessage(locale, "global.no") %>
								<% if(existed){ %></a><% } %>
							</td>
							<td>
								<%
								if(root == null){
								%>
								<bean:message key='have.no.data'/>
								<%
								}else{
									try{
									%>
									<span class="text-success"><pre><%= StringEscapeUtils.escapeHtml(root.toString(2)) %></pre></span>
									<%
									}catch(Exception e){
									%>
									<span class="text-danger"><bean:message key='format.error'/>：<%= StringEscapeUtils.escapeHtml(e.getMessage()) %></span>
									<%
									}
								}
								%>
							</td>
						</tr>
					<%
					out.flush();
				}
				root = new JSONObject();
			}

			if (cells != null) {

				int currLv = -1;
				String fixText = null;
				String optText = null;
				boolean isEndOfLevel = false;
				String answer = null;
				Pattern p = Pattern.compile("(?s)第([0-9]+)階問題-(.*?)$");

				//System.out.println("**************************");
				//System.out.println(cells);
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					String header = headers[i];
					String val = cells.get(i);
					Matcher m = p.matcher(headers[i]);

					if (val != null) {
						if (m.find()) {
							currLv = Integer.parseInt(m.group(1));
							String type = m.group(2);

							if ("固定文字".equals(type)) {
								fixText = val;
							}
							else if ("選項".equals(type)) {
								optText = val;
							}
						}
						else if ("ANSWER".equals(headers[i])) {
							answer = val;
						}
						else if ("EOL".equals(headers[i])) {
							isEndOfLevel = QAUtil.isConfirmWithYes(val);
						}
					}
				}

				if (currLv != -1) {
					if (currLv == 1) {
						List<String> lines = QAUtil.parseMultiValue(fixText);
						question = lines.remove(0);
						directAnswer = answer;
						qaTPL = lines;
						root.put("question", question);
					}
					else {
						appendToHierarchicalJSON(root, currLv, fixText, optText, answer, isEndOfLevel);
						//System.out.println( String.format("%d:%s:%s:%s:%b", currLv, fixText, optText, answer, isEndOfLevel));
					}
				}
			}
			else {
				break;
			}

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
