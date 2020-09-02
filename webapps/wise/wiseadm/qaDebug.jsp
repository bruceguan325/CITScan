<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="flexjson.JSONSerializer"
	import="com.intumit.message.MessageUtil"
	import="com.intumit.systemconfig.WiseSystemConfigFacade"
	import="com.intumit.systemconfig.WiseSystemConfig"
	import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qaplugin.*"
	import="com.intumit.solr.robot.qaplugin.QAPlugin"
	import="com.intumit.solr.robot.qaplugin.QAPlugins"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.robot.qarule.ForwardToCrmRule.Forward"
	import="com.intumit.solr.robot.qarule.ForwardToCrmRule.WaitingCmd"
	import="com.intumit.solr.robot.qarule.ForwardToCrmRule.OffHourCmd"
	import="com.intumit.solr.robot.qarule.ForwardToCrmRule.Cmd"
	import="com.intumit.solr.robot.QAUtil.CrmRedirectTag"
	import="com.intumit.solr.robot.TemplateUtil.Replacer"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.smartwiki.util.*"
	import="com.intumit.solr.util.*"
	import="java.io.*"
	import="java.util.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.common.*"
	import="org.apache.commons.codec.binary.Base64"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.lang.math.RandomUtils"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E3) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%! 
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<title>QA DEBUG</title>
<jsp:include page="header-qa.jsp"></jsp:include>
<style>
td {
word-wrap: break-word;
}

.fixed-panel {
  min-height: 250px;
  max-height: 250px;
  overflow-y: scroll;
}
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container-fluid">
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String qaId = request.getParameter("id");
if (StringUtils.isBlank(qaId)) {
	qaId = QAContextManager.generateQaId(t, null, null);
}

String q = request.getParameter("q");

%><br>
<form>
<input type="hidden" name="id" value="<%= qaId %>">
<textarea rows="3" cols="100" name="q"><%= StringUtils.trimToEmpty(q) %></textarea><br>
<button class="btn btn-default btn-success" type="submit">除錯</button>
</form>
<%
if (q == null) {
	return;
}

boolean enableDetail = QAUtil.getBooleanParameter(request, "details", true);
boolean testMode = true;

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	out.println("新的對話紀錄...<br>");
	qaCtx = QAContextManager.create(qaId);
	qaCtx.setTenant(t);
}
else {
	%>
	<h3>前一問</h3>
	<table class="table table-striped table-bordered">
	<tr>
		<td>問</td><td><%= qaCtx.getOriginalQuestion() %></td>
		<td>答</td><td><%= qaCtx.hasAnswerText() ? StringUtils.left(StringUtils.trimToEmpty(qaCtx.getAnswerText().toString()).replaceAll("<[^>]+>", " "), 50) + "..." : "" %></td>
	</tr>
	<tr>
		<td>問類型</td><td><%= qaCtx.getQuestionType() %></td>
		<td>答類型</td><td><%= qaCtx.getAnswerType() %></td>
	</tr>
	<tr>
		<td>知識主題</td><td><%= qaCtx.getCurrentKPs() != null ? Arrays.asList(qaCtx.getCurrentKPs()) : "" %></td>
		<td>Context Map</td><td><%= qaCtx.getCtxAttr() %></td>
	</tr>
	</table>
	<hr/>
	<%
}

try {
	com.intumit.solr.servlet.FakeHttpRequest fakeReq = new com.intumit.solr.servlet.FakeHttpRequest(request);
	fakeReq.setFakeParameter("tid", "" + t.getId());
	QAUtil.parseRequest(qaCtx, fakeReq, testMode, true);
}
catch (RobotException rbtEx) {
	out.println(rbtEx.getError().toString(2));
	return;
}

{	// Just for print out log
	String tmpQ = request.getParameter("q");
	
	boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
	System.out.println("Got Question [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (user != null ? "FROM (" + user.getLoginName() + ")" : "")
		+ "(Channel: " + qaCtx.getQaChannel() + ")"
		+ "(isClientSupportHTML: " + qaCtx.isClientSupportHtml() + ")"
		+ "(Tenant: " + qaCtx.getTenant().getName() + ")"
		);
	
	if (explainQA) {
		qaCtx.appendExplain("Enter QA-AJAX", "Got Question [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (user != null ? "FROM (" + user.getLoginName() + ")" : "")
		+ "(Channel: " + qaCtx.getQaChannel() + ")");
	}
}

// 這裡開始 check DemoFlow
DemoFlow df = DemoFlow.getInstance(qaCtx.getTenant().getId());

if (df.isEnable() && df.isOverwriteMode()) {
	if (df.getCurrentOffset() != -1 && df.getCurrentOffset() < df.getQuestions().size()) {
		String dfQ = df.getQuestions().get(df.getCurrentOffset());
		
		if (df.isAutopilotMode()) {
			df.setCurrentOffset(df.getCurrentOffset() + 1);
		}
		
		if (df.isSelfRepair()) { // 目前暫時拿來作為覆寫模式每次覆寫完就停止覆寫的啟用或關閉
			df.setOverwriteMode(false);
		}
		qaCtx.setOriginalQuestion(dfQ);
		qaCtx.setCurrentQuestion(dfQ);
	}
}

JSONObject resp = null;

if (qaCtx.getCurrentQuestion() != null) {
	JSONObject conversation = qaCtx.getConversationsJson();
	QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
	c.check(qaCtx);

	resp = qaCtx.genResponseJSON();
	conversation.getJSONArray("messages").put(resp);
	qaCtx.setConversations(conversation.toString());

	Map<String, Object> ctxAttr = qaCtx.getCtxAttr();
	resp.append("bundle", ctxAttr.get("bundle"));

	//System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
	QAContextManager.put(qaId, qaCtx);

	//System.out.println(qaCtx.explainPath());

	// DO LOG
	ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);
	if (testMode) {
		log = (ServiceLogEntity) session.getAttribute("TestModeServiceLogEntity");
		session.setAttribute("TestModeServiceLogEntity", log);
		//System.out.println(qaCtx.getExplain());
		//System.out.println("TestModeServiceLogEntity: " + log.getConversations());
	}
}


// 開始處理 debug visualization
List<Object[]> explains = qaCtx.getExplain();
List<Object[]> pathLog = (List<Object[]>)explains.get(explains.size() - 1)[2];

%>
<div class="row">
<%

for (int i=0; i < pathLog.size(); i++) {
	Object[] objArr = pathLog.get(i);
	
	String pathKey = (String)objArr[1];
	PreRuleCheckResult preR = (PreRuleCheckResult)objArr[2];
	PostRuleCheckResult postR = (PostRuleCheckResult)objArr[3];
	
	Boolean hasAnswer = (Boolean)objArr[4];
	if (hasAnswer == null) hasAnswer = Boolean.FALSE;
	
	StringBuilder answer = (StringBuilder)objArr[5];
	%>
	<div class="col-md-3">
	<div class="panel <%= hasAnswer ? "panel-danger" : "panel-primary" %>">
		<div class="panel-heading">
	    <h3 class="panel-title"><%= StringUtils.substringAfterLast((String)objArr[0], ".") %></h3>
		</div>
		<div class="panel-body fixed-panel">
		預處理結果：
		<% 
			switch (preR.getStatus()) {
				case NORMAL:
					out.println("<span class='text-success'><strong>正常</strong><span>");
					break;
				case SKIP_AND_CONTINUE:
					out.println("<span class='text-warning'><strong>略過</strong></span>");
				default:
			}
		%>
		<hr/>
		<% 
		if (postR != null) {
			out.println(postR.getStatus());
			out.println("<br>");
			
			if (postR.getForwardTo() != null) {
				out.println("<span class='glyphicon glyphicon-chevron-right'>" + postR.getForwardTo() + "</span>");
			}
			
			out.println("<hr/>");
			
			if (hasAnswer) {
				out.println("<h3 class='text-danger'>有答案</h3>");
				out.println("<hr/>");
				out.println(objArr[4]);
			}
			else {
				out.println("<h4 class='text-success'>尚無答案</h4>");
			}
		}
		
		if (enableDetail) {
		out.println("<hr/>");
		
		for (Object[] log: explains) {
			String thisPathKey = (String)log[0];
			if (StringUtils.equalsIgnoreCase(pathKey, thisPathKey)) {
				Object val = log[2];
				Long elasped = (Long)log[3];
				
				if (val != null) {
					if (val instanceof SolrDocumentList) {
						Object[] fieldVals = ((SolrDocumentList)val).toArray();
						
						for (Object o: fieldVals) {
							out.println(o);
							out.println("<HR/>");
						}
					}
					else if (val instanceof SolrDocument) {
						SolrDocument doc = ((SolrDocument)val);
						String[] fieldNames = doc.getFieldNames().toArray(new String[0]);
						for (String fn: fieldNames) {
							out.println(fn);
							out.println(doc.getFieldValues(fn));
							out.println("<BR>");
						}
						
					}
					else if (val instanceof SolrQuery) {
						SolrQuery sq = ((SolrQuery)val);
						String[] fieldNames = sq.getParameterNames().toArray(new String[0]);
						for (String fn: fieldNames) {
							out.println(fn);
							out.println(Arrays.asList(sq.getParams(fn)));
							out.println("<BR>");
						}
						out.println("<HR/>");
						out.println(sq);
					}
					else {
						out.println(val);
					}
				}
				else {
					out.println("");
				}
			}
		}
		}
		%>
		</div>
	</div>
	</div>
	<div class="col-md-1">
	<center>
		<br><br>
		<span class="glyphicon glyphicon-chevron-right"></span><br><br>
	</center>
	</div>
	<%
}

%>
</div>
<%--
<%= com.intumit.solr.util.WiSeUtils.nl2br(qaCtx.explainPath()) %>
<hr/>
<table class='table table-strip table-bordered' style="table-layout: fixed; width: 99%;">
<thead>
	<tr>
	<th style="width:80px;">No</th>
	<th class="col-md-1">Status</th>
	<th class="col-md-1">Detail Class</th>
	<th>Detail</th>
	</tr>
</thead>
<%
List<Object[]> logs = qaCtx.getExplain();

List<QAPattern> patterns = QAPattern.list(t.getId());
int no = 1;

for (Object[] log: logs) {
	Object val = log[1];
	Long elasped = (Long)log[2];
%>
<tr>
	<td>
		<%= no++ %><br>
		<%= elasped %>ms
	</td>
	<td><%= log[0] %></td>
	<td><%= val != null ? val.getClass().getName() : "null" %></td>
	<td>
	<%
	if (val != null) {
		if (val instanceof SolrDocumentList) {
			Object[] fieldVals = ((SolrDocumentList)val).toArray();
			
			for (Object o: fieldVals) {
				out.println(o);
				out.println("<HR/>");
			}
		}
		else if (val instanceof SolrDocument) {
			SolrDocument doc = ((SolrDocument)val);
			String[] fieldNames = doc.getFieldNames().toArray(new String[0]);
			for (String fn: fieldNames) {
				out.println(fn);
				out.println(doc.getFieldValues(fn));
				out.println("<BR>");
			}
			
		}
		else if (val instanceof SolrQuery) {
			SolrQuery sq = ((SolrQuery)val);
			String[] fieldNames = sq.getParameterNames().toArray(new String[0]);
			for (String fn: fieldNames) {
				out.println(fn);
				out.println(Arrays.asList(sq.getParams(fn)));
				out.println("<BR>");
			}
			out.println("<HR/>");
			out.println(sq);
		}
		else {
			out.println(val);
		}
	}
	else {
		out.println("");
	}
	%>
	</td>
</tr>
<%
}
%>
</table>
</div>
--%>
</body>
</html>