<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="flexjson.*"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.json.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="com.intumit.solr.util.fileupload.FileMeta"
import="com.intumit.solr.util.fileupload.MultipartRequestHandler"
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
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.util.WiSeUtils"
import="com.intumit.solr.synonymKeywords.*"
%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page import="com.intumit.solr.admin.*" %>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<title><bean:message key='data.upload.page'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
</head>

<body>
<%
Locale locale= (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}

boolean dryRun = !"false".equalsIgnoreCase(request.getParameter("dryRun"));
String idStr = request.getParameter("id");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.get(Integer.parseInt(idStr));

%>
<div class='well'>
<h1>
<% if (dryRun) { %>
準備匯入 <%= t.getNotes() %> (<%= t.getName() %>:<%= t.getId() %>) [<%= Calendar.getInstance().getTime() %>]
<% } else { %>
正在匯入 <%= t.getNotes() %> (<%= t.getName() %>:<%= t.getId() %>) [<%= Calendar.getInstance().getTime() %>]
<% } %>
</h1>
</div>
<%
QAUtil qu = QAUtil.getInstance(t);
SolrServer server = t.getCoreServer4Write();

List<FileMeta> files = (List<FileMeta>)session.getAttribute("qaFileUploaded");
System.out.println("files size : "+files.size());

for (FileMeta getFile : files) {
	System.out.println("filename : "+getFile.getFileName());
	InputStream input = getFile.getContent();
	System.out.println("textContent size : "+input.available()/1024+ "Kb");
	
	BufferedReader br = null;
	StringBuilder sb = new StringBuilder();
	
	br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
	
	String line;
	while ((line = br.readLine()) != null) {
		sb.append(line);
	}
	//System.out.println(sb.toString());

	JSONObject data = new JSONObject(sb.toString());
	
	// 開始對資料
	out.println("<form action='tenantImport.jsp'>");
	out.println("<input type='hidden' name='id' value='" + idStr + "'>");
	out.println("<input type='hidden' name='dryRun' value=false>");
	out.println("<table class='table table-bordered table-striped'>");
	out.println("<tr><th width='10px'>是否匯入</th><th>欄位</th></tr>");

	{
		String pn = "tenant";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("是否要匯入公司設定本身資料（基本資料及API不會匯入，但其他如流程、門檻值、各項功能開關、通訊軟體設定等都會匯入）", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "docs";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " docs to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "customDatas";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " custom data records to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "mcas";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " multichannel answers to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "syns";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 同義詞組 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "dicts";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 詞庫詞組 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "channels";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " channels to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "userTypes";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " usertypes to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "qapatterns";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 特殊答案 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "rfa";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 制式文案 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "intents";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " intents to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "entities";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " entities to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "dialogs";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " Dialogs to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "dialogVers";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " Dialog Versions to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "altTpls";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 例句範本 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "sbts";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 批次校詞 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "ele";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 測試紀錄 to import...", "h4")));
		out.println("</tr>");
	}
	{
		String pn = "sle";
		boolean checked = dryRun || request.getParameter(pn) != null;
		out.println("<tr>");
		out.println(td("<input type='checkbox' data-toggle='toggle' name='" + pn + "' " + (checked ? "checked" : "") + (dryRun ? "" : " disabled") + ">"));
		out.println(td(wrap("" + countArr(data, pn) + " 對話紀錄 to import...", "h4")));
		out.println("</tr>");
	}

	
	out.println("</table>");
	
	%>
	<% if (dryRun) { %>
	<button class='btn btn-warning' type='submit' onclick="return confirm('Are you sure to import those data?');" >Import</button>
	<% } %>
	<%
	
	if (!dryRun) {
		JSONDeserializer s = new JSONDeserializer();
		List<SolrInputDocument> docList = new ArrayList<SolrInputDocument>();
		List<SolrInputDocument> docList2 = new ArrayList<SolrInputDocument>();
		List<MultiChannelAnswer> mcaList = new ArrayList<MultiChannelAnswer>();
		List<SynonymKeyword> syns = new ArrayList<SynonymKeyword>();
		List<DictionaryDatabase> dicts = new ArrayList<DictionaryDatabase>();
		List<QAChannel> chList = new ArrayList<QAChannel>();
		List<QAUserType> utList = new ArrayList<QAUserType>();
		List<QAPattern> qpList = new ArrayList<QAPattern>();
		List<QAAltTemplate> altTplList = new ArrayList<QAAltTemplate>();
		List<RobotFormalAnswers> rfaList = new ArrayList<RobotFormalAnswers>();
		List<QAIntent> intents = new ArrayList<QAIntent>();
		List<QAEntity> entities = new ArrayList<QAEntity>();
		List<QADialogConfig> dlgConfigs = new ArrayList<QADialogConfig>();
		
		if (request.getParameter("tenant") != null && data.has("tenant")) {
			Tenant tmpT = (Tenant)s.deserialize(data.getJSONObject("tenant").toString(), Tenant.class);
			// 下列這些欄位都保持原樣，不匯入
			tmpT.setId(t.getId());
			tmpT.setCoreName(t.getCoreName());
			tmpT.setCasualCoreUrl(t.getCasualCoreUrl());
			tmpT.setOpendataCoreUrl(t.getOpendataCoreUrl());
			tmpT.setName(t.getName());
			tmpT.setNotes(t.getNotes());
			tmpT.setAddress(t.getAddress());
			tmpT.setEmail(t.getEmail());
			tmpT.setUuid(t.getUuid());
			tmpT.setCrmApiBaseUrl(t.getCrmApiBaseUrl());
			tmpT.setDefaultTextCrmDepartmentId(t.getDefaultTextCrmDepartmentId());
			tmpT.setApikey(t.getApikey());
			
			Tenant.saveOrUpdate(tmpT);
		}
		
		if (request.getParameter("docs") != null) {
			try {
				for (int i=0; i < data.getJSONArray("docs").length(); i++) 
				{
					JSONObject obj = data.getJSONArray("docs").getJSONObject(i);
					SolrInputDocument instant = new SolrInputDocument();
					HashMap<String, Object> map = (HashMap<String, Object>)s.deserialize(obj.toString(), HashMap.class);
					
					for (String key: map.keySet()) {
						if (key.endsWith("_dt")) {
							long lDate = (Long)map.get(key);
							instant.setField(key, new Date(lDate));
						}
						else {
							instant.setField(key, map.get(key));
						}
					}
					
					docList.add(instant);
				}
				if(!docList.isEmpty()) {
					server.add(docList);
				}
			}
			catch(Exception ignore) {
				ignore.printStackTrace();
			}
		}
		
		if (request.getParameter("customDatas") != null) {
			for (int i=0; i < data.getJSONArray("customDatas").length(); i++) 
			{
				JSONObject obj = data.getJSONArray("customDatas").getJSONObject(i);
				SolrInputDocument instant = new SolrInputDocument();
				HashMap<String, Object> map = (HashMap<String, Object>)s.deserialize(obj.toString(), HashMap.class);
				
				for (String key: map.keySet()) {
					if (key.endsWith("_dt")) {
						long lDate = (Long)map.get(key);
						instant.setField(key, new Date(lDate));
					}
					else {
						instant.setField(key, map.get(key));
					}
				}
				
				docList2.add(instant);
			}
			if(!docList2.isEmpty()) {
				server.add(docList2);
			}
		}
		
		if (request.getParameter("docs") != null || request.getParameter("customDatas") != null) {
			out.println("<h1>索引資料已轉換... 正在寫入索引...</h1>");
			out.flush();
			server.commit(true, true, false);
		}
		
		// 處理詞庫們
		if (request.getParameter("syns") != null) for (int i=0; i < data.getJSONArray("syns").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("syns").getJSONObject(i);
			SynonymKeyword instant = (SynonymKeyword)s.deserialize(obj.toString(), SynonymKeyword.class);
			SynonymKeywordFacade.getInstance().save(t.getId(), instant.getKeyword(), instant.getSynonymKeyword(), instant.isReverse(), instant.getNature());
		}
		if (request.getParameter("dicts") != null) for (int i=0; i < data.getJSONArray("dicts").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("dicts").getJSONObject(i);
			DictionaryDatabase instant = (DictionaryDatabase)s.deserialize(obj.toString(), DictionaryDatabase.class);
			instant.setTenantId(t.getId());
			DictionaryDatabase.save(t.getId(), instant.getKeyword(), instant.getPurposes(), instant.isEnabled(), instant.getCategory(), instant.getEnableQaScopeRestriction());
		}
		if (request.getParameter("intents") != null) for (int i=0; i < data.getJSONArray("intents").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("intents").getJSONObject(i);
			QAIntent instant = (QAIntent)s.deserialize(obj.toString(), QAIntent.class);
			instant.setTenantId(t.getId());
			QAIntent.saveOrUpdate(instant);
		}
		if (request.getParameter("entities") != null) for (int i=0; i < data.getJSONArray("entities").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("entities").getJSONObject(i);
			QAEntity instant = (QAEntity)s.deserialize(obj.toString(), QAEntity.class);
			instant.setTenantId(t.getId());
			QAEntity.saveOrUpdate(instant);
		}
		
		// 處理剩下的
		if (request.getParameter("qapatterns") != null) for (int i=0; i < data.getJSONArray("qapatterns").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("qapatterns").getJSONObject(i);
			QAPattern instant = (QAPattern)s.deserialize(obj.toString(), QAPattern.class);
			instant.setTenantId(t.getId());
			QAPattern.saveOrUpdate(instant);
		}
		if (request.getParameter("dialogs") != null) for (int i=0; i < data.getJSONArray("dialogs").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("dialogs").getJSONObject(i);
			QADialogConfig instant = (QADialogConfig)s.deserialize(obj.toString(), QADialogConfig.class);
			instant.setTenantId(t.getId());
			QADialogConfig.saveOrUpdate(instant);
		}
		if (request.getParameter("dialogVers") != null) for (int i=0; i < data.getJSONArray("dialogVers").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("dialogVers").getJSONObject(i);
			QADialogConfigVersion instant = (QADialogConfigVersion)s.deserialize(obj.toString(), QADialogConfigVersion.class);
			instant.setTenantId(t.getId());
			QADialogConfigVersion.saveOrUpdate(instant);
		}
		if (request.getParameter("altTpls") != null && data.has("altTpls")) for (int i=0; i < data.getJSONArray("altTpls").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("altTpls").getJSONObject(i);
			QAAltTemplate instant = (QAAltTemplate)s.deserialize(obj.toString(), QAAltTemplate.class);
			instant.setTenantId(t.getId());
			QAAltTemplate.saveOrUpdate(instant);
		}
		if (request.getParameter("sbts") != null && data.has("sbts")) for (int i=0; i < data.getJSONArray("sbts").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("sbts").getJSONObject(i);
			SegmentBatchTask instant = (SegmentBatchTask)s.deserialize(obj.toString(), SegmentBatchTask.class);
			instant.setTenantId(t.getId());
			SegmentBatchTask.saveOrUpdate(instant);
		}
		if (request.getParameter("rfa") != null) for (int i=0; i < data.getJSONArray("rfa").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("rfa").getJSONObject(i);
			RobotFormalAnswers instant = (RobotFormalAnswers)s.deserialize(obj.toString(), RobotFormalAnswers.class);
			instant.setTenantId(t.getId());
			RobotFormalAnswers.saveOrUpdate(instant);
		}
		
		
	
		// 處理 Channel / UserType / MultiChannelAnswer
		if (request.getParameter("channels") != null) for (int i=0; i < data.getJSONArray("channels").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("channels").getJSONObject(i);
			QAChannel instant = (QAChannel)s.deserialize(obj.toString(), QAChannel.class);
			instant.setTenantId(t.getId());
			if (QAChannel.get(t.getId(), instant.getName(), instant.getCode()) == null)
				QAChannel.saveOrUpdate(instant);
		}
		if (request.getParameter("userTypes") != null) for (int i=0; i < data.getJSONArray("userTypes").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("userTypes").getJSONObject(i);
			QAUserType instant = (QAUserType)s.deserialize(obj.toString(), QAUserType.class);
			instant.setTenantId(t.getId());
			if (QAUserType.get(t.getId(), instant.getName(), instant.getCode()) == null)
				QAUserType.saveOrUpdate(instant);
		}
		
		List<QAChannel> channels = QAChannel.list(t.getId());
		Set<String> channelCodes = new HashSet<String>();
		for (QAChannel ch: channels) {
			channelCodes.add(ch.getCode());
		}
		if (request.getParameter("mcas") != null) for (int i=0; i < data.getJSONArray("mcas").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("mcas").getJSONObject(i);
			MultiChannelAnswer instant = (MultiChannelAnswer)s.deserialize(obj.toString(), MultiChannelAnswer.class);
			
			if (channelCodes.contains(instant.getChannel())) {
				instant.setTenantId(t.getId());
				MultiChannelAnswer.saveOrUpdate(instant);
			}
			else {
				out.println("<h3>" + instant.getChannel() + "頻道不存在，MultiChannelAnswer[" + instant.getQaId() +"]此頻道資料無法寫入，跳過...</h3>");
			}
		}
		if (request.getParameter("ele") != null) for (int i=0; i < data.getJSONArray("ele").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("ele").getJSONObject(i);
			EvaluationLogEntity instant = (EvaluationLogEntity)s.deserialize(obj.toString(), EvaluationLogEntity.class);
			instant.setTenantId(t.getId());
			EvaluationLogEntity.save(instant);
		}
		if (request.getParameter("sle") != null) for (int i=0; i < data.getJSONArray("sle").length(); i++) 
		{
			JSONObject obj = data.getJSONArray("sle").getJSONObject(i);
			ServiceLogEntity instant = (ServiceLogEntity)s.deserialize(obj.toString(), ServiceLogEntity.class);
			instant.setTenantId(t.getId());
			ServiceLogEntity.saveWithoutUpdateTimestamp(instant);
		}
	
		if (request.getParameter("docs") != null) {
			out.println("<h1 class='alert alert-danger'>匯入完畢，排程建立各種問法...</h1>");
		
			Thread.sleep(1000); // wait for softCommit
			for (SolrInputDocument doc: docList) {
				QA qa = new QA(doc);
				QAAltBuildQueue.add(t.getId(), qa.getId(), qa.getKid(), qa.getQuestionAltTemplates(), user.getLoginName());
			}
		}
		else {
			out.println("<h1 class='alert alert-danger'>匯入完畢...</h1>");
		}
	}
	
	out.flush();
}



%>
</body>
</html>
<%!
int countArr(JSONObject obj, String propName) {
	if (obj.has(propName)) {
		JSONArray arr = obj.optJSONArray(propName);
		
		if (arr != null)
			return arr.length();
	}
	
	return 0;
}
String wrap(String text, String tag) { return "<" + tag + ">" + text + "</" + tag + ">"; }
String td(String text) { return wrap(text, "td"); }
String pre(String text) { return wrap(text, "pre"); }
String p(String text) { return wrap(text, "p"); }
String br(String text) { return text + ("<br>"); }
%>