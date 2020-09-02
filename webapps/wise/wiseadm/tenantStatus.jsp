<%@ include file="/commons/taglib.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="flexjson.*"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.wink.json4j.JSONObject"
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
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.entity.*"
import="com.intumit.solr.robot.intent.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.util.WiSeUtils"
import="com.intumit.solr.synonymKeywords.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
return;
}

String id = request.getParameter("id");
if (id == null) {
	out.println("You need choose a tenant first!");
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.get(Integer.parseInt(id));

if (t == null) {
	out.println("You need choose a tenant first!");
	return;
}
%>
<%!
String wrap(String text, String tag) { return "<" + tag + ">" + text + "</" + tag + ">"; }
String td(String text) { return wrap(text, "td"); }
String pre(String text) { return wrap(text, "pre"); }
String p(String text) { return wrap(text, "p"); }
String br(String text) { return text + ("<br>"); }
%>
<!DOCTYPE html>
<html>
<head>
<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<meta content='text/html;charset=utf-8' http-equiv='content-type'>
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
</head>
<%
boolean dryRun = !"false".equalsIgnoreCase(request.getParameter("dryRun"));
String callback = request.getParameter("cb");
callback = StringEscapeUtils.escapeJavaScript(callback);
%>
<body class='contrast-muted '>
<%
QAUtil qautil = QAUtil.getInstance(t);
SolrServer server = t.getCoreServer4Write();

SolrQuery sqry = new SolrQuery();
sqry.setRequestHandler("/browse");
sqry.setParam("qf", "QUESTION_t^1000000 QUESTION_ALT_mt^0.01");
sqry.setParam("mm", "1");
sqry.setParam("fuzzy", false);
sqry.setRows(20000);
sqry.setStart(0);
sqry.setQuery("*:*");

SolrQuery sqry4customData = sqry.getCopy();
sqry.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
sqry.addFilterQuery("-isPart_i:[2 TO *]");

// 非知識點的資料
sqry4customData.addFilterQuery("-dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);

out.println("<form action='XX' id='tenantStatusForm'>");
out.println("<input type='hidden' name='id' value='" + id + "'>");
out.println("<input type='hidden' name='dryRun' value=false>");
out.println("<button class='btn btn-warning btnTenantStatusSubmit' type='submit'>確定</button>");
out.println("<table class='table table-bordered table-striped'>");
out.println("<tr><th width='10px'>選擇</th><th>欄位</th><th></th></tr>");

//開始查資料
QueryResponse resp = server.query(sqry);
SolrDocumentList docList = resp.getResults();
{
String pn = "docs";
boolean checked = dryRun || request.getParameter(pn) != null;
String rangeVal = StringUtils.trimToEmpty(request.getParameter(pn + "Range"));
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + docList.getNumFound() + " docs"
		// + "，範圍：<input class='form-control' placeholder='不填代表全部，填寫id範圍（ex. 10000-20000）' type='text' name='" + pn + "Range' value='" + rangeVal + "'" + (dryRun ? "" : " disabled") + ">"
		, "h4")));
out.println(td(""));
out.println("</tr>");
}

QueryResponse resp2 = server.query(sqry4customData);
SolrDocumentList docList2 = resp2.getResults();
{
String pn = "customDatas";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + docList2.getNumFound() + " custom data records", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<MultiChannelAnswer> mcaList = MultiChannelAnswer.list(t.getId());
{
String pn = "mcas";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + mcaList.size() + " multichannel answers", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listAll(t.getId());
{
String pn = "syns";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + syns.size() + " 同義詞組", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<DictionaryDatabase> dicts = DictionaryDatabase.listByTenantId(t.getId());
{
String pn = "dicts";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + dicts.size() + " 詞庫詞組", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAChannel> chList = QAChannel.list(t.getId());
{
String pn = "channels";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + chList.size() + " channels", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAUserType> utList = QAUserType.list(t.getId());
{
String pn = "userTypes";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + utList.size() + " usertypes", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAPattern> qpList = QAPattern.list(t.getId());
{
String pn = "qapatterns";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + qpList.size() + " 特殊答案", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<RobotFormalAnswers> rfaList = RobotFormalAnswers.list(t.getId());
{
String pn = "rfa";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + rfaList.size() + " 制式文案", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAIntent> intents = QAIntent.listByTenantId(t.getId());
{
String pn = "intents";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + intents.size() + " intents", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAEntity> entities = QAEntity.listByTenantId(t.getId());
{
String pn = "entities";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + entities.size() + " entities", "h4")));
out.println(td(""));
out.println("</tr>");
}


List<QADialogConfig> dlgConfigs = QADialogConfig.list(t.getId());
{
String pn = "dialogs";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + dlgConfigs.size() + " Dialogs", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QADialogConfigVersion> dlgConfigVers = QADialogConfigVersion.list(t.getId());
{
String pn = "dialogVers";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + dlgConfigVers.size() + " Dialog Versions to export (若只匯出 Dialog 不匯出各版本，只有最新保留)...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAAltTemplate> altTpls = QAAltTemplate.list(t.getId());
{
String pn = "altTpls";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + altTpls.size() + " 例句範本", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<SegmentBatchTask> sbts = SegmentBatchTask.list(t.getId(), null, SegmentBatchTask.Status.WAIT);
{
String pn = "sbts";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + sbts.size() + " 批次校詞（status == WAIT）", "h4")));
out.println(td(""));
out.println("</tr>");
}


int eleCount = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, null, null, null, null, null, null, false, null, null, null, null).intValue();
{
String pn = "ele";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + eleCount + " 測試紀錄", "h4")));
out.println(td(""));
out.println("</tr>");
}


int sleCount = ServiceLogEntity.countBy(t.getId(), null, null, null, null, null, null, null).intValue();
{
String pn = "sle";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + sleCount + " 對話紀錄", "h4")));
out.println(td(""));
out.println("</tr>");
}


out.println("</table>");
%>
<% if (dryRun) { %>
<button class='btn btn-warning btnTenantStatusSubmit' type='submit'>確定</button>
<% } %>
<%
%>
<script>
$('.btnTenantStatusSubmit').click(function(e) {
	e.preventDefault();
	data = {};
	$.each($('#tenantStatusForm').find('input[type=checkbox]'), function(idx, elm) {
		name = $(elm).attr('name');
		
		$('#tenantStatusForm').find('input[type=text][name=' + name + 'Range]').each(function(idx, elm) {
			data[name + 'Range'] = $('#tenantStatusForm input[name=' + name + 'Range]').val();
		});
		data[name] = $('#tenantStatusForm input[name=' + name + ']').prop('checked');
	});
	$('#tenantStatusDlg').dialog('close');
	<% if (StringUtils.isNotEmpty(callback)) { %>
	<%= callback %>(data);
	<% } %>
	
	return true;
});
</script>
</body>
</html>
