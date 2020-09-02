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
<jsp:include page="header-qa.jsp" />
<meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
<meta content='text/html;charset=utf-8' http-equiv='content-type'>
</head>
<%
boolean dryRun = !"false".equalsIgnoreCase(request.getParameter("dryRun"));
%>
<body class='contrast-muted '>
<div class="well">
<h1>
<% if (dryRun) { %>
準備匯出 <%= t.getNotes() %> (<%= t.getName() %>:<%= t.getId() %>) [<%= Calendar.getInstance().getTime() %>]
<% } else { %>
正在匯出 <%= t.getNotes() %> (<%= t.getName() %>:<%= t.getId() %>) [<%= Calendar.getInstance().getTime() %>]
<% } %>
</h1>
<h3 class="text-danger">
目前無法處理「檔案」類型的資料，如：格式文字內的附檔、貼圖。<br>
（知識點及特殊答案的資料各自最高匯出筆數上限為20000筆）<br>
批次校詞部分僅會匯出尚待處理的資料，已完成或者不處理的都不會匯出 <br>
若匯出的資料筆數很多（尤其是紀錄部分，大約超過5000筆就算多），選擇匯出後即可離開頁面
因為太多資料匯出一定頁面會 Timeout（但系統仍會繼續完成匯出動作，因此<b>不要重複點選</b>）
</h3>
</div>
<%
String fn = com.intumit.solr.util.WiSeEnv.getHomePath() + "/cache/export-" + t.getId() + "-" + t.getName().replaceAll("[^\\.][^-][^_]", "_") + "-" + new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime()) + ".json";

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

out.println("<form action='tenantExporter.jsp'>");
out.println("<input type='hidden' name='id' value='" + id + "'>");
out.println("<input type='hidden' name='dryRun' value=false>");
out.println("<table class='table table-bordered table-striped'>");
out.println("<tr><th width='10px'>是否匯出</th><th>欄位</th><th>範圍</th></tr>");

//開始查資料
QueryResponse resp = server.query(sqry);
SolrDocumentList docList = resp.getResults();
{
String pn = "docs";
boolean checked = dryRun || request.getParameter(pn) != null;
String rangeVal = StringUtils.trimToEmpty(request.getParameter(pn + "Range"));
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + docList.getNumFound() + " docs to export...", "h4")));
out.println(td("<input type='text' name='" + pn + "Range' value='" + rangeVal + "'" + (dryRun ? "" : " disabled") + ">"));
out.println("</tr>");
}

QueryResponse resp2 = server.query(sqry4customData);
SolrDocumentList docList2 = resp2.getResults();
{
String pn = "customDatas";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + docList2.getNumFound() + " custom data records to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<MultiChannelAnswer> mcaList = MultiChannelAnswer.list(t.getId());
{
String pn = "mcas";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + mcaList.size() + " multichannel answers to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listAll(t.getId());
{
String pn = "syns";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + syns.size() + " 同義詞組 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<DictionaryDatabase> dicts = DictionaryDatabase.listByTenantId(t.getId());
{
String pn = "dicts";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + dicts.size() + " 詞庫詞組 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAChannel> chList = QAChannel.list(t.getId());
{
String pn = "channels";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + chList.size() + " channels to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAUserType> utList = QAUserType.list(t.getId());
{
String pn = "userTypes";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + utList.size() + " usertypes to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAPattern> qpList = QAPattern.list(t.getId());
{
String pn = "qapatterns";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + qpList.size() + " 特殊答案 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<RobotFormalAnswers> rfaList = RobotFormalAnswers.list(t.getId());
{
String pn = "rfa";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + rfaList.size() + " 制式文案 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAIntent> intents = QAIntent.listByTenantId(t.getId());
{
String pn = "intents";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + intents.size() + " intents to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<QAEntity> entities = QAEntity.listByTenantId(t.getId());
{
String pn = "entities";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + entities.size() + " entities to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}


List<QADialogConfig> dlgConfigs = QADialogConfig.list(t.getId());
{
String pn = "dialogs";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + dlgConfigs.size() + " Dialogs to export...", "h4")));
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
out.println(td(wrap("" + altTpls.size() + " 例句範本 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}

List<SegmentBatchTask> sbts = SegmentBatchTask.list(t.getId(), null, SegmentBatchTask.Status.WAIT);
{
String pn = "sbts";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + sbts.size() + " 批次校詞（status == WAIT） to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}


int eleCount = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, null, null, null, null, null, null, false, null, null, null, null).intValue();
{
String pn = "ele";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + eleCount + " 測試紀錄 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}


int sleCount = ServiceLogEntity.countBy(t.getId(), null, null, null, null, null, null, null).intValue();
{
String pn = "sle";
boolean checked = dryRun || request.getParameter(pn) != null;
out.println("<tr>");
out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
out.println(td(wrap("" + sleCount + " 對話紀錄 to export...", "h4")));
out.println(td(""));
out.println("</tr>");
}


out.println("</table>");
%>
<% if (dryRun) { %>
<button class='btn btn-warning' onclick="return confirm('<bean:message key="search.export.confirm"/>');" type='submit'><bean:message key='search.export.start'/></button>
<% } %>
<%

if ("false".equalsIgnoreCase(request.getParameter("dryRun"))) {
JSONSerializer s = null;
boolean first = true; s = new JSONSerializer();

BufferedWriter writer = new BufferedWriter(new FileWriter(fn, true));
writer.write("{\n");

writer.write("\"tenant\":" + new JSONObject(
							s.exclude("id", "allowedLocalesList", "coreName", "casualCoreUrl", "apikey", "opendataCoreUrl", "coreServer", "coreServer4Write", "qaCategoryMap")
							.include("qaCategory", "specificEservice", "qaMatchCtrlFlow", "forceIgnoreCharacters", "permission", 
									"allowedCustomDataFieldNames", "otherMltQANum", 
									"enableEssentialKeywordMode", "enableContextMatch", 
									"enableRestrictToQaCategory", "enablePhoneticHomonym", "enableTextCrm", "enableMultiLocale", "enableScenario", "enableAutoLearning", 
									"threshold1", "threshold2", "threshold3", "tooManyRepeatThreshold", "boost1", "boost2", "boost3")
							.exclude("*")
							.deepSerialize(t)).toString(2));
writer.write(",\n");

first = true; s = new JSONSerializer();
writer.write("\"docs\": [\n");
String docRangeStr = StringUtils.trimToEmpty(request.getParameter("docsRange"));
String[] docRanges = StringUtils.split(docRangeStr, ",");
Set<Long> allowedDocIdSet = new HashSet<Long>();

for (String docRange: docRanges) {
	if (docRange.indexOf("-") != -1) {
		int start = Integer.parseInt(StringUtils.substringBefore(docRange, "-"));
		int end = Integer.parseInt(StringUtils.substringAfter(docRange, "-"));
		
		for (int i=start; i <= end; i++) {
			allowedDocIdSet.add(new Long(i));
		}
	}
	else {
		allowedDocIdSet.add(new Long(docRange));
	}
}

if (request.getParameter("docs") != null) for (SolrDocument doc: docList) 
{
	if (allowedDocIdSet.size() == 0) {
		if (first) { first = false; } else { writer.write(",\n"); }
		String str = s.exclude("_version_", "score").deepSerialize(doc);
		writer.write(new JSONObject(str).toString(2));
	}
	else {
		Long kid = (Long)doc.getFieldValue("kid_l");
		if (allowedDocIdSet.contains(kid)) {
			if (first) { first = false; } else { writer.write(",\n"); }
			String str = s.exclude("_version_", "score").deepSerialize(doc);
			writer.write(new JSONObject(str).toString(2));
		}
	}
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"customDatas\": [\n");
if (request.getParameter("customDatas") != null) for (SolrDocument doc: docList2) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("_version_", "score").deepSerialize(doc);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"mcas\": [\n");
if (request.getParameter("mcas") != null) for (MultiChannelAnswer mca: mcaList) 
{
	if (allowedDocIdSet.size() == 0) {
		if (first) { first = false; } else { writer.write(",\n"); }
		String str = s.exclude("id", "tenantId", "QAChannel").deepSerialize(mca);
		writer.write(new JSONObject(str).toString(2));
	}
	else {
		Long kid = QAUtil.id2Kid(mca.getQaId());
		if (allowedDocIdSet.contains(kid)) {
			if (first) { first = false; } else { writer.write(",\n"); }
			String str = s.exclude("id", "tenantId", "QAChannel").deepSerialize(mca);
			writer.write(new JSONObject(str).toString(2));
		}
	}
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"syns\": [\n");
if (request.getParameter("syns") != null) for (SynonymKeyword obj: syns) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	// Remove auto-generated synonyms.
	obj.setSynonymKeyword(obj.getSynonymKeywordForEditingOrReading());
	String str = s.exclude("id", "tenantId", "keywordAndSynonymList", "synonymList").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"dicts\": [\n");
if (request.getParameter("dicts") != null) for (DictionaryDatabase obj: dicts) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId", "purposeSet", "purposesJson").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"channels\": [\n");
if (request.getParameter("channels") != null) for (QAChannel obj: chList) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"userTypes\": [\n");
if (request.getParameter("userTypes") != null) for (QAUserType obj: utList) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"qapatterns\": [\n");
if (request.getParameter("qapatterns") != null) for (QAPattern obj: qpList) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"intents\": [\n");
if (request.getParameter("intents") != null) for (QAIntent obj: intents) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"entities\": [\n");
if (request.getParameter("entities") != null) for (QAEntity obj: entities) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"dialogs\": [\n");
if (request.getParameter("dialogs") != null) for (QADialogConfig obj: dlgConfigs) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId", "dialogConfigObject").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"dialogVers\": [\n");
if (request.getParameter("dialogVers") != null) for (QADialogConfigVersion obj: dlgConfigVers) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId", "dialogConfigObject").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"altTpls\": [\n");
if (request.getParameter("altTpls") != null) for (QAAltTemplate obj: altTpls) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"sbts\": [\n");
if (request.getParameter("sbts") != null) for (SegmentBatchTask obj: sbts) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"rfa\": [\n");
if (request.getParameter("rfa") != null) for (RobotFormalAnswers obj: rfaList) 
{
	if (first) { first = false; } else { writer.write(",\n"); }
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	writer.write(new JSONObject(str).toString(2));
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"ele\": [\n");
if (request.getParameter("ele") != null) 
{
	int rows = 500;
	int start = 0;
	
	do 
	{
		List<EvaluationLogEntity> logs = EvaluationLogEntity.listBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, null, null, null, null, null, null, false, null, null, null, null, start, rows);
		start += rows;

		for (EvaluationLogEntity obj: logs) 
		{
			if (first) { first = false; } else { writer.write(",\n"); }
			String str = s.exclude("id", "tenantId").deepSerialize(obj);
			writer.write(new JSONObject(str).toString(2));
		}
	} while (start < eleCount);
}
writer.write("],\n");

first = true; s = new JSONSerializer();
writer.write("\"sle\": [\n");
if (request.getParameter("sle") != null) 
{
	int rows = 500;
	int start = 0;
	
	do 
	{
		List<ServiceLogEntity> logs = ServiceLogEntity.listBy(t.getId(), null, null, null, null, null, null, start, rows);
		start += rows;

		for (ServiceLogEntity obj: logs) 
		{
			if (first) { first = false; } else { writer.write(",\n"); }
			String str = s.exclude("id", "tenantId").deepSerialize(obj);
			writer.write(new JSONObject(str).toString(2));
		}
	} while (start < sleCount);
}
writer.write("]\n");

writer.write("}\n");

writer.close();


out.println(wrap("匯出完畢... 檔名為[" + fn + "]", "h1"));

/*
for (SolrDocument doc: docList) 
{
	String str = s.exclude("_version_", "score").deepSerialize(docList);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (SolrDocument doc: docList2) 
{
	String str = s.exclude("_version_", "score").deepSerialize(docList);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (MultiChannelAnswer mca: mcaList) 
{
	String str = s.exclude("id", "tenantId", "QAChannel").deepSerialize(mca);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (SynonymKeyword obj: syns) 
{
	String str = s.exclude("id", "tenantId", "keywordAndSynonymList", "synonymList").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (DictionaryDatabase obj: dicts) 
{
	String str = s.exclude("id", "tenantId", "purposeSet", "purposesJson").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QAChannel obj: chList) 
{
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QAUserType obj: utList) 
{
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QAPattern obj: qpList) 
{
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QAIntent obj: intents) 
{
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QAEntity obj: entities) 
{
	String str = s.exclude("id", "tenantId").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (QADialogConfig obj: dlgConfigs) 
{
	String str = s.exclude("id", "tenantId", "dialogConfigObject").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}

for (RobotFormalAnswers obj: rfaList) 
{
	String str = s.exclude("id", "tenantId", "dialogConfigObject").deepSerialize(obj);
	out.write(p(br(pre(StringEscapeUtils.escapeHtml(new JSONObject(str).toString(2))))));
}
*/
}
%>
<jsp:include page="footer-qa.jsp" />
</body>
</html>
