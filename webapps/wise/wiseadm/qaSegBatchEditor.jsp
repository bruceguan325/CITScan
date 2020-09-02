<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.json.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.collections.*"
import="org.apache.solr.common.*"
import="org.apache.solr.client.solrj.*"
import="com.intumit.solr.util.*"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%!

static class MyPagination extends WiSeUtils.SimplePagination {
	public MyPagination(int rows, long total, int pageGap, String baseUrl) {
		super(rows, total, pageGap, baseUrl);
	}
	
	@Override
	public String makeUrl(int start, int rows) {
		return "javascript: jumpPage(" + start + "," + rows + ");";
	}
}
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='global.batch.segment.change'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/codemirror.css">
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.mode.simple.js"></script>
<style>
.CodeMirror {
  background: rgba(0, 0, 0, 0);
  height: auto;
}
.CodeMirror-question {
  border: 1px solid #aaa;
  height: auto;
}
.CodeMirror-gutters { z-index: 0}
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
String mode = StringUtils.defaultString(request.getParameter("mode"), "qa");
Long id = new Long(request.getParameter("id"));
SegmentBatchTask sbt = SegmentBatchTask.get(id);
if (sbt.getTenantId() != t.getId()) return;

int no = 1;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
request.setAttribute("sbt", sbt);

QAUtil qautil = QAUtil.getInstance(t);
Set<String> tkws = sbt.toKeywordSet();
Set<String> sortedByLength = new TreeSet<String>(new Comparator() {
    public int compare(Object o1, Object o2) {
    		String s1 = (String)o1;
    		String s2 = (String)o2;

        return s2.length() - s1.length() > 0 ? 1 : -1;
    }
});

boolean aggressiveMerge = request.getParameter("mergeOption") != null;
sortedByLength.addAll(tkws);
System.out.println(sortedByLength);

String kpRegex = "碩網資訊";
kpRegex = "(" + StringUtils.join(sortedByLength, "|") + ")";

int start = Integer.parseInt(StringUtils.defaultString(request.getParameter("start"), "0"));
int rows = 10;

// 有勾選要套用的資料，因此開始處理
String toBeProcess = StringUtils.trimToNull(request.getParameter("toBeProcess"));
if (toBeProcess != null) {
	List<String> idStrs = Arrays.asList(StringUtils.split(toBeProcess, ","));
	StringBuilder dirtyIds = new StringBuilder();
	
	for (String idStr: idStrs) {
		List<String> altTpls = null;
		List<String> newAltTpls = new ArrayList<String>();
		boolean dirty = false;

		Long tid = new Long(idStr);
		QA qa = null;
		if ("qa".equals(mode)) {
			SolrDocument dd = qautil.getMainQASolrDocument(tid);
			if (dd != null) {
				qa = new QA(dd);	
				altTpls = qa.getQuestionAltTemplates();
			}
		}
		else if ("template".equals(mode)) {
			QAAltTemplate qaAltTpl = QAAltTemplate.get(tid.intValue());
			altTpls = QAUtil.parseMultiValue(qaAltTpl.getTemplate());
		}
		
		if (altTpls != null)
		// 接著這裡要來判斷所有替換斷句的邏輯了
		for (String altTplRow: altTpls) {
			Map<String, String> data = QA.parseQAAlt(altTplRow);
			String altTpl = data.get("alt");
			String newAltTpl = sbt.processMergeAndReplace(altTpl, tkws, aggressiveMerge);
			
			if (!StringUtils.equals(altTpl, newAltTpl)) {
				dirty = true;
				//out.println("<textarea class='alts'>" + altTpl + "\n==> " + newAltTpl + "</textarea><br>");
				
				if ("qa".equals(mode)) {
					// 應該要想辦法把 testCase 加回來
					JSONObject append = new JSONObject();
					if (data.get("testCase") != null) {
						append.put("testCase", data.get("testCase"));
						append.put("testCase.editor", user.getId());
					}
					append.put("alt.editor", user.getId());
					newAltTpls.add(newAltTpl + "\t// " + append.toString());
				}
				else { // QAAltTemplate 不需要加上後面那堆資訊
					newAltTpls.add(newAltTpl);
				}
			}
			else {
				newAltTpls.add(altTplRow);
			}
		}
		
		if (dirty) {
			// 儲存 Template
			out.println("Processing " + tid + " ... CHANGED... Going to rebuild index....<br>");
			if ("qa".equals(mode)) {
				// if dirty == true, 更新 QA index
						
				sbt.addProcessedKid(tid);
				if (dirtyIds.length() > 0) dirtyIds.append(',');
				dirtyIds.append(tid);
				
				Date now = new Date();
				qa.setUpdateInfo(now, user);
				qa.setQuestionAltTemplates(newAltTpls);
		
				SolrServer server = t.getCoreServer4Write();
				server.add(qa);
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
				
				QAAltBuildQueue.add(t.getId(), (String)qa.getFieldValue("id"), tid, qa.getQuestionAltTemplates(), user.getLoginName());
			}
			else if ("template".equals(mode)) {
				// if dirty == true, 更新 QAAltTemplate
				sbt.addProcessedKid(tid);
				if (dirtyIds.length() > 0) dirtyIds.append(',');
				dirtyIds.append(tid);
				
				QAAltTemplate qaAltTpl = QAAltTemplate.get(tid.intValue());
				qaAltTpl.setTemplate(StringUtils.join(newAltTpls, "\n"));
				QAAltTemplate.saveOrUpdate(qaAltTpl);
				QAAltTemplate.updateEffectedQA(t, qaAltTpl, user.getLoginName());
			}
		}
		else {
			out.println("Processing " + tid + " ... NOT CHANGED<br>");
		}
	}
	
	sbt.appendLog(user, "Trying to proccess " + mode + " [" + toBeProcess + "].");
	sbt.appendLog(user, "Successfully changed " + mode + " [" + dirtyIds.toString() + "].");
	
	SegmentBatchTask.saveOrUpdate(sbt);
}



// 開始來 Preview 可能要修正的資料
List objs = null;
MyPagination pagination = null;

if ("qa".equals(mode)) {
	objs = qautil.lowLevelSearchCommonSense("QUESTION_ALT_TPL_ms", "(" + sbt.toQuery() + ")", start, rows);
	pagination = new MyPagination(rows, ((SolrDocumentList)objs).getNumFound(), 5, "");
}
else if ("template".equals(mode)) {
	objs = QAAltTemplate.list(t.getId());
	pagination = new MyPagination(rows, 5, 5, ""); // QAAltTemplate 的清單不需要分頁全列
}
%>
<div class='alert alert-info'>
<h3>主要詞：${sbt.keyword} <c:if test='${sbt.renameTo}'> => ${sbt.renameTo}</c:if>
	<div class='pull-right'>
	<a href='qaSegBatchList.jsp?action=done&id=${sbt.id}' class='btn btn-success btnDone' title="<bean:message key='global.finish'/>"><span class="glyphicon glyphicon-ok"></span></a>
	<a href='qaSegBatchList.jsp?action=reject&id=${sbt.id}' class='btn btn-danger btnReject' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-remove"></span></a>
	</div>
</h3>
<h4><bean:message key="word.be.merge"/>：${ sbt.toBeMerged }</h4>
<form id="theForm" action="qaSegBatchEditor.jsp" method="POST">
<bean:message key="correction.option"/>
<br><input type="checkbox" name="mergeOption" data-toggle="toggle" data-on="最大合併" data-off="最小合併" <%= aggressiveMerge ? "checked" : "" %>>  
<input type="hidden" name="id" value="<%= id %>">
<input type="hidden" name="mode" value="<%= mode %>">
<input type="hidden" name="start" value="<%= start %>">
<input type="hidden" name="rows" value="<%= rows %>">
<input type="hidden" name="action" value="list">
<input type="hidden" name="toBeProcess" value="">
</form>
</div>

<div id="topToolbar">
	<button class='btn btn-danger btnApplyChoosed'><bean:message key="apply.selected"/></button>
	<button class='btn btn-danger btnApplyAll'><bean:message key="global.all"/><bean:message key="global.apply"/></button>
	<button class='btn btn-warning btnToggleCheckAll'><bean:message key="global.cancel"/><bean:message key="global.select"/></button>
	<div class="pull-right">
	<%= pagination.toHtml(start) %>
	</div>
</div>
<br>
<table class='table table-striped'>
<thead>
	<tr>
	<th style="width:10px;">No</th>
	<th><bean:message key='num'/></th>
	<th><bean:message key='standard.problem'/> / <bean:message key='global.description'/></th>
	<th class='col-sm-3'><bean:message key='operation'/></th>
	</tr>
</thead>
<% 
int count=start; 
for (Object obj: objs) { 
	count++; 
	List<String> altTpls = null;
	List<String> effectedAlts = new ArrayList<String>();
	boolean suspicion = false;
	boolean dirty = false;
	
	if ("qa".equals(mode)) {
		QA qa = new QA((SolrDocument)obj);	
		altTpls = qa.getQuestionAltTemplates();
		Long kid = (Long)qa.getFieldValue("kid_l");
		request.setAttribute("currId", kid);
		request.setAttribute("link", "qaDataEditor.jsp?id=" + QAUtil.DATATYPE_COMMON_SENSE + "-" + kid);
		request.setAttribute("linkTitle", kid);
		request.setAttribute("title", qa.getQuestion());
	}
	else if ("template".equals(mode)) {
		QAAltTemplate qaAltTpl = (QAAltTemplate)obj;
		altTpls = QAUtil.parseMultiValue(qaAltTpl.getTemplate());
		request.setAttribute("currId", qaAltTpl.getId());
		request.setAttribute("link", "qaAltTemplateEdit.jsp?id=" + qaAltTpl.getId());
		request.setAttribute("linkTitle", "" + qaAltTpl.getId() + " (" + qaAltTpl.getMkey() + ")");
		request.setAttribute("title", qaAltTpl.getName());
	}
	
	if (altTpls != null)
	for (String altTplRow: altTpls) {
		String altTpl = QA.parseAndGetQAAltOnly(altTplRow);
		Sentence s = new Sentence(altTpl);
		boolean toBeCheck = false;
		
		for (Sentence.Block b: s.getBlocks()) {
			if (b.isInParentheses()) {
				if (CollectionUtils.containsAny(b.getTerms(), tkws)) {
					toBeCheck = true;
				}
			}
		}
		
		if (toBeCheck) {
			suspicion = true;
			String newAltTpl = sbt.processMergeAndReplace(altTpl, tkws, aggressiveMerge);
			
			// 只是為了呈現
			if (!StringUtils.equals(altTpl, newAltTpl)) {
				dirty = true;
				effectedAlts.add("🔴 " + altTpl);
				effectedAlts.add("➡➡➡\t" + newAltTpl);
				effectedAlts.add("");
			}
			else {
				effectedAlts.add("⚫ " + altTpl);
			}
		}
	}
	
	request.setAttribute("dirty", dirty);
	
	if ("template".equals(mode)) { 
		// 對於 QAAltTemplate 來說，沒有可能性的根本連列都不需要列出
		if (!suspicion) continue; 
	}
%>
<tr>
	<td style="width:10px;"><%= count %></td>
	<td class='col-md-1'><strong><a href="${ link }" target="_new">${ linkTitle }</a></strong></td>
	<td class='col-md-1'><strong>${ title }</strong></td>
	<td>
	<c:if test="${ dirty }">
	<input type="checkbox" class="applyToggle" data-toggle="toggle" checked data-kid="${ currId }" data-on="<bean:message key="global.apply"/>" data-off="<bean:message key="global.disable"/>">
	</c:if>
	<c:if test="${ !dirty }">
	<input type="checkbox" class="applyToggle" data-toggle="toggle" data-kid="${ currId }" data-on="<bean:message key="global.apply"/>" data-off="<bean:message key="global.disable"/>">
	</c:if>
	</td>
	<td> </td>
</tr>
<tr>
	<td colspan=5 style="padding-left: 25px;">
	<textarea class='alts'><%
	for (String s: effectedAlts) {
		out.println(s);
	}
	%></textarea>
	</td>
</tr>
<% } %>
</table>
<div id="bottomToolbar">
</div>
</div>
<script>
$('#topToolbar').clone().appendTo($('#bottomToolbar'));

$('.btnApplyChoosed').click(function() {
	var kids = "";
	$(".applyToggle:checked:enabled").each(function() {
		kid = $(this).attr('data-kid');
		if (kids.length > 0) kids += ",";
		kids += kid;
	});
	
	$('input[name=toBeProcess]').val(kids);
	$('#theForm').submit();
});

$('.btnApplyAll').click(function() {
	var kids = "";
	$(".applyToggle").each(function() {
		kid = $(this).attr('data-kid');
		if (kids.length > 0) kids += ",";
		kids += kid;
	});
	
	$('input[name=toBeProcess]').val(kids);
	$('#theForm').submit();
});

$('.btnToggleCheckAll').click(function() {
	$(".applyToggle").bootstrapToggle('off');
});

$('input[name=mergeOption]').change(function() {
	$('#theForm').submit();  
});

var jumpPage = function(start, rows) {
	$('input[name=start]').val(start);
	$('input[name=rows]').val(rows);
	$('#theForm').submit();  
	return false;
};

CodeMirror.defineSimpleMode("qaalt", {
	  // The start state contains the rules that are intially used
	  start: [
	     {regex: /\/\/.*/, token: "comment"},
	   	 {regex: /<%= kpRegex %>/, token: "string-2" },
	   	 {regex: /➡➡➡/, token: "keyword" },
	     {regex: /'(?:[^\\]|\\.)*?'/, token: "string" },
	     {regex: /\{\{\[a-zA-Z$]+\}\}/, token: "variable-2" },
	     {regex: /\(/, push: "synonym", token: "bracket" },
	  ],
	  comment: [
	    {regex: /.*?\*\//, token: "comment", next: "start"},
	    {regex: /.*/, token: "comment"}
	  ],
	  // The meta property contains global information about the mode. It
	  // can contain properties like lineComment, which are supported by
	  // all modes, and also directives like dontIndentStates, which are
	  // specific to simple modes.
	  variable: [
	     {regex: /\}\}/, pop: true, token: "bracket", next: "start" },
	     {regex: /\$[a-zA-Z]+/, token: "variable-2" },
	     {regex: /"(?:[^\\]|\\.)*?"/, token: "string" },
	     {regex: /'(?:[^\\]|\\.)*?'/, token: "string" }
	  ],
	  synonym: [
	   	 {regex: /<%= kpRegex %>/, token: "string-2", next: "synonym" },
	     {regex: /\)/, pop: true, token: "bracket", next: "start" },
	     {regex: /\|/, token: "comment" },
	     {regex: /\{\{[a-zA-Z$]+\}\}/, token: "variable-2", next: "synonym" },
	     {regex: /\#[a-zA-Z]+/, token: "variable-2" },
	     {regex: /[^)|]*/, token: "variable-3" },
	  ],
	  knowledgepoint: [
	  ]
	});
	
$('.alts').each(function() {
	$this = $(this);
	cm = CodeMirror.fromTextArea($this[0], {
		matchBrackets: true,
		mode: "qaalt",
   		readonly: true
	});
});
</script>
</body>
</html>