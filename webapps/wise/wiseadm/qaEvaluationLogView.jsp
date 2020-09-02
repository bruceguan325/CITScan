<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="flexjson.*"
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
import="org.json.JSONArray"
import="com.intumit.hithot.HitHotLocale"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) {
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = HitHotLocale.determineLocale(request, true, true);
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
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-common.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-build-status.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-ml.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/i18n.js"></script>
<script src='<%= request.getContextPath() %>/wiseadm/js/My97DatePicker/WdatePicker.js' type='text/javascript'></script>
<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
#test-stat th span {
	font-size: 2em;
	margin: 0 auto;
}
#test-stat td span {
	font-size: 1.5em;
	margin: 0 auto;

}
.testScore span {
    margin: 0 5px;
}
.flagFilterIcon  {
	font-size: 3em;
}
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<div class="row" style="margin-top: 10px;">

<%

boolean checkDistinctQuestion = Boolean.parseBoolean(request.getParameter("dq"));
boolean checkIgnoreFlag0 = request.getParameter("checkIgnoreFlag0") != null;
String flagFilter = StringUtils.defaultString(request.getParameter("flag"), "");
String qaCategory = StringUtils.trimToNull(request.getParameter("qaCategory"));
String afterDateStr = StringUtils.trimToNull(request.getParameter("afterDate"));
String beforeDateStr = StringUtils.trimToNull(request.getParameter("beforeDate"));

Date afterDate = null;
Date beforeDate = null;
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

if (request.getParameter("timeRange") != null) {
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(request.getParameter("timeRange"));
	afterDate = timeRangePair[0];
	beforeDate = timeRangePair[1];
} else if (StringUtils.trimToNull(request.getParameter("afterDate"))!=null && StringUtils.trimToNull(request.getParameter("beforeDate"))!=null) {
	afterDate = sdf.parse(request.getParameter("afterDate"));
	beforeDate = sdf.parse(request.getParameter("beforeDate"));
}

String bypassAdmIds = null;
if(request.getParameterValues("bypassAdmIds") != null && request.getParameterValues("bypassAdmIds").length > 0){
	bypassAdmIds = StringUtils.trimToNull(StringUtils.join(request.getParameterValues("bypassAdmIds"), ","));
	System.out.println("bypassAdmIds:" + bypassAdmIds);
}

Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();

int total = EvaluationLogEntity.countBy(t.getId(), null, null, null, StringUtils.isNotEmpty(flagFilter) ? Integer.parseInt(flagFilter) : null, null, null, null, null, checkDistinctQuestion, qaCategory, bypassAdmIds, afterDate, beforeDate).intValue();
int rows = 20;
int start = Integer.parseInt(StringUtils.defaultString(request.getParameter("start"), "0"));

MyPagination pagination = new MyPagination(rows, total, 5, "");
%>
<form id="filterForm" action="qaEvaluationLogView.jsp">
<input type="hidden" name="format" value="json">
<input type="hidden" name="ed" value="true">
<div class="col-md-12">
	<div class="col-md-7" style="margin-top: 10px;">
	
		<bean:message key='qacategory.select'/><br>
		<select  class="form-control" name="qaCategory" id="qaCategory" >
			<option value="" ><bean:message key='global.all'/></option>
		<%
		for (Integer idx : qaCategoryMap.keySet()) {
		%>
			<option value="<%= qaCategoryMap.get(idx) %>" ><%= qaCategoryMap.get(idx) %></option>
		<%
		}
		%>
		</select>
		<br>
	
	
	</div>
	
	<div class="col-md-5">
		<bean:message key='global.evaluation'/><bean:message key='filter.condition'/><br>
		<div class="pull-left text-center">
			<span data-flag="0" class="flagFilterIcon text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>
			<br><input type="radio" name="flag" value="0">
		</div>
		<div class="pull-left text-center">
			<span data-flag="1" class="flagFilterIcon text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>
			<br><input type="radio" name="flag" value="1">
		</div>
		<div class="pull-left text-center">
			<span data-flag="2" class="flagFilterIcon text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>
			<br><input type="radio" name="flag" value="2">
		</div>
		<div class="pull-left text-center">
			<span data-flag="3" class="flagFilterIcon text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>
			<br><input type="radio" name="flag" value="3">
		</div>
		<div class="pull-left text-center">
			<span data-flag="4" class="flagFilterIcon text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span>
			<br><input type="radio" name="flag" value="4">
		</div>
		<div class="pull-left text-center">
			<span data-flag="5" class="flagFilterIcon text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>
			<br><input type="radio" name="flag" value="5">
		</div>
		<div class="pull-left text-center">
			<span data-flag="6" class="flagFilterIcon text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span>
			<br><input type="radio" name="flag" value="6">
		</div>
	</div>
</div>
<div class="col-md-12" style="margin-top: 10px;">
	<div class="col-md-7">
		<bean:message key='exclude.tester.num.multiple'/><br>
		<select class="form-control" name="bypassAdmIds" id="bypassAdmIds" multiple>
		<%
		for (AdminUser u: AdminUserFacade.getInstance().listAll()) {
			if (!u.getTenantIdSet().contains(t.getId())) continue;
		%>
			<option value="<%= u.getId() %>" >(<%= u.getId() %>).<%= u.getName() %></option>
		<%
		}
		%>
		</select>
		<br>
		<bean:message key='time.range'/><br>
	    <input type="text" name="afterDate" class="Wdate" id="afterDate" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'beforeDate\')}'})" />&nbsp-&nbsp
	    <input type="text" name="beforeDate" class="Wdate" id="beforeDate" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'afterDate\')}', maxDate:'%y-%M-%d 23:59:59'})" />&nbsp
	</div>
	
	<div class="col-md-5" style="margin-top: 10px;">
		<bean:message key='global.other'/><bean:message key='filter.condition'/><br>
		<input type="checkbox" id="checkDistinctQuestion" name="dq" data-size="mini" value=true checked>
		<bean:message key='check.distinct.question'/> 
		<br>
		<bean:message key='check.distinct.question.ex'/>
		<br>
		<br>
		<br>
		<br>
		
		<button class="btn btn-warning" type="submit"><bean:message key='filter.conidtion.submit'/></button>
		<button class="btn btn-info btnReset" type="button"><bean:message key='filter.conidtion.reset'/></button>
		<br>
	</div>
</div>
<div class="col-md-12" style="margin-top:10px;">
	<div class="col-md-7">
	</div>
	<div class="col-md-5">
	<br>
	</div>
</div>
</form>
<br><br>
<div class="col-md-12">
	<div class="col-md-7">
		<table id="test-stat" class="table table-striped table-bordered">
		<tr>
			<th><span data-qa-category="" data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
			<th><span data-qa-category="" data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
			<th><span data-qa-category="" data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span></th>
			<th><span data-qa-category="" data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span></th>
			<th><span data-qa-category="" data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span></th>
			<th><span data-qa-category="" data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span></th>
			<th><span data-qa-category="" data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span></th>
			<th><span class="text-primary"><bean:message key='global.accuracy'/></span></th>
		</tr>
		<tr>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
			<td class="stat-num"><span></span></td>
		</tr>
		</table>
	</div>

	<div class="col-md-5">
	</div>
</div>
</div>

<hr/>

<div class="row">
<div class="col-md-12">
	<div class="col-md-8">
	<%= pagination.toHtml(start, true) %>
	</div>
	<div class="col-md-4">
		<input type="checkbox" id="checkIgnoreFlag0" name="checkIgnoreFlag0" data-on="<bean:message key='ignore.not.select'/>" data-off="全部" checked>
		<button id='btnAutoTestAll' data-loading-text="<bean:message key='global.testing'/>" class='btn btn-default' style="margin: 0 10px;"><bean:message key='test.all'/><br><div class='testScore'></div></button>
		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
		<button class="btn btn-danger btnExportXlsx"><bean:message key='export.excel'/></button>
		<% } %>
	</div>
</div>
<div class="col-md-12" style="margin-top:15px;">
	<table id="test-list" class="table table-striped table-bordered table-hover">
	<thead>
		<tr>
			<th><bean:message key='global.evaluation'/></th>
			<th><bean:message key='test.question'/></th>
			<th><bean:message key='test.expected.answer.number'/></th>
			<th><bean:message key='robot.answer.number'/></th>
			<th><bean:message key='robot.answer'/></th>
			<th><bean:message key='global.confidence.level'/></th>
			<th><bean:message key='test.time'/></th>
			<th style='width:10px;'><bean:message key='operation'/></th>
		</tr>
	</thead>
	<tbody>
	</tbody>
	</table>
</div>
<div>
	<div class="pull-left">
	<%= pagination.toHtml(start, true) %>
	</div>
</div>
</div>
<br>

<div id="find-similar-dialog" title="<bean:message key='similar.problems'/>" style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>

<div id="magic-dialog" title="<bean:message key='robot.magicBtn.title'/>" style="display:none; z-index:999; ">
</div>

<div class='loading-panel'></div>

<script>
var $qaEntryData = {};
var $qaDialogDatas = [];
var $simEntryData;

var filterData = {
	dq: <%= checkDistinctQuestion %>,
	<% if (StringUtils.isNotEmpty(flagFilter)) { %>
	flag: <%= flagFilter %>,
	<% } %>
	<% if (StringUtils.isNotEmpty(qaCategory)) { %>
	qaCategory: "<%= qaCategory %>",
	<% } %>
	<% if (bypassAdmIds != null) { %>
	bypassAdmIds: "<%= bypassAdmIds %>",
	<% } %>
	<% if (afterDateStr != null) { %>
	afterDate: "<%= afterDateStr %>",
	<% } %>
	<% if (beforeDateStr != null) { %>
	beforeDate: "<%= beforeDateStr %>",
	<% } %>
	start: <%= start %>,
	rows: <%= rows %>
};

function getQuestionLink(kid, textClass) {
	if (kid != null && typeof(kid) != 'undefined') {
		var a = $('<a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + kid + '" target="_blank"/>');
		if (typeof(textClass) != 'undefined') {
			a.append($('<span/>').addClass(textClass).text(kid));
		}
		else {
			a.text(kid);
		}
		return a;
	}

	if (typeof(textClass) != 'undefined') {
		return $('<span/>').addClass(textClass);
	}
	return $('<span/>');
}

$('.btnExportXlsx').click(function() {
	theForm = $('#filterForm');
	theForm.attr('action', 'qaEvaluationLog-ajax.jsp');
	theForm.attr('target', '_blank');
	theForm.find('input[name=format]').val('xlsx');
	theForm.submit();
	theForm.attr('action', 'qaEvaluationLogView.jsp');
	theForm.attr('target', '_self');
	theForm.find('input[name=format]').val('json');
});

var timeRange;

$('.btnReset').click(function() {
    $('input[name=flag]').prop('checked', false);
});

function getAndShowStat() {

	function fillStat(tr, rangeStr, stats) {
		var good = 0, bad = 0;
		var totalWoFlag0And3 = 0;

		$(tr).find('.stat-range span').text(rangeStr);

		for (var i=0; i < stats.length; i++) {
			$(tr).find('.stat-num:eq(' + i + ') span').text(stats[i]);

			if (i == 1 || i == 2 || i == 4) {
				good += stats[i];
			}
			if (i == 5 || i == 6) {
				bad += stats[i];
			}
			if (i != 0 && i != 3) {
				totalWoFlag0And3 += stats[i];
			}
		}

		$(tr).find('.stat-num:eq(7) span').text( Math.round(good * 100 / Math.max(1, totalWoFlag0And3)) + '%' );
	}
	var byPassIds = "";
	if($( "#bypassAdmIds" ).val() != null){
		byPassIds = $( "#bypassAdmIds" ).val().toString();
	}
	$.getJSON('qa-log-statistics-ajax.jsp', 
			filterData,
			function(data){
				fillStat($('#test-stat tr:eq(1)'), "<bean:message key='global.all'/>", data.flagStatistics);
			});
}

<%--
$('[id^=test-stat-] span[data-flag]').click(function() {
	$this = $(this);
	flag = parseInt($this.attr('data-flag'));
	var qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	getAndShowLog(start, qaCategoryIdx);
});

$('[id^=test-stat-] td.stat-range').click(function() {
	$this = $(this);
	timeRange = $this.attr('data-date-range');
	qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	getAndShowLog(start, qaCategoryIdx);
});
 --%>
 
function getAndShowLog(s, qaCategoryIdx) {
	var options = $.extend({}, filterData);
	if (typeof(timeRange) != 'undefined') {
		options.timeRange = timeRange;
	}
	
	$.getJSON('qaEvaluationLog-ajax.jsp', options,
	function(data){
		var flagGlyphs = [
		          		'<span data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
		          		'<span data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
		          		'<span data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>',
		          		'<span data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>',
		          		'<span data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span>',
		          		'<span data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>',
		          		'<span data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span>',
		                  ];

		var list = $('#test-list tbody');
		list.find('tr:has(td)').remove();
		if (data.length == 0){
			list.append('<tr class="text-center"><td colspan="8"><bean:message key="have.no.data"/></td></tr>');
		} else{
			for (var i=0; i < data.length; i++) {
				var r = data[i];
				var tr = $('<tr/>');
				var cv = JSON.parse(r.conversations);
				var t = moment( r.timestamp );
				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
				formatted += "<br><span class='text-danger'>【" + r.admName + "】</span>";

				$('<td name="logId" data-id="' + r.id + '" data-flag="' + r.flag + '"><h3>' + flagGlyphs[r.flag] + '</h3></td>').appendTo(tr);
				$('<td name="question">' + r.question + '</td>').appendTo(tr);
				$('<td name="correctAnswerId"></td>').append(getQuestionLink(r.correctAnswerId)).appendTo(tr);
				$('<td name="robotAnswerId"></td>').append(getQuestionLink(r.robotAnswerId)).appendTo(tr);
				$('<td>' + cv.output + '</td>').appendTo(tr);
				$('<td>' + (cv.hasOwnProperty('confidence') ? cv.confidence : '') + '</td>').appendTo(tr);
				$('<td>' + formatted + '</td>').appendTo(tr);

				operTd = '<td><button name="btnAutoTest" class="btn btn-info" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="test.btn.tooltip"/>' + '"><span class="glyphicon glyphicon-question-sign"></span></button>';
				operTd += '<button class="btn btn-success" name="btnNlp" data-loading-text="' + '<bean:message key="broken.words"/>' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="global.broken.tooltip"/>' + '"><span class="glyphicon glyphicon-scissors"></span></button>';

		    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
		    		operTd += '<button name="btnSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>';
			    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) { %>
			    		operTd += '<button name="btnMagic" class="btn btn-primary" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.magicButton.tooltip"/>' + '"><span class="glyphicon glyphicon-ok-sign"></span></button>';
			    		<% } %>
		    		<% } %>
		    	
		    		<% if (t.getEnableScenario() && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) { %>
		    		operTd += '<button name="btnScenario" class="btn btn-danger" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.dialogButton.tooltip"/>' + '"><span class="glyphicon glyphicon-list-alt"></span></button>';
		    		<% } %>

				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) { %>
				operTd += '<button name="btnReviewHide" class="btn btn-warning" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="global.hidden"/>' + '"><span class="glyphicon glyphicon-eye-close"></span></button>';
				<% } %>

				operTd += '</td>';
				$(operTd).appendTo(tr);

				list.append(tr);
			}
		}

	    $('[data-toggle="tooltip"]').tooltip();
		$( "#test-dialog").dialog('open');
		$('.loading-panel').hide();
	});
}

var myConfirm = function(title, msg, okCallback) {
	if (!$('#dataConfirmModal').length) {
		$('body').append('<div id="dataConfirmModal" class="modal fade" tabindex="-1" role="dialog"> <div class="modal-dialog"> <div class="modal-content"> <div class="modal-header"> <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button> <h4 class="modal-title">Modal title</h4> </div> <div class="modal-body"> <p>One fine body&hellip;</p> </div> <div class="modal-footer"> <button type="button" class="btn btn-default" data-dismiss="modal">Close</button> <button type="button" class="btn btn-primary" id="dataConfirmOK" >Ok</button> </div> </div><!-- /.modal-content --> </div><!-- /.modal-dialog --> </div><!-- /.modal -->');
	}

	$('#dataConfirmModal').find('.modal-title').text(title);
	$('#dataConfirmModal').find('.modal-body').html(msg);
	$('#dataConfirmModal').modal({show:true});

	var onClick = function() {
		$('#dataConfirmModal').modal('toggle');
		okCallback();
	};

	$(document).on("click", '#dataConfirmOK', onClick);

	$('#dataConfirmModal').on('hidden.bs.modal', function () {
		$(document).off("click", '#dataConfirmOK', onClick);
	});

	return false;
}

$(document).on('click', 'button[name="btnScenario"]', function() {
	$thisBtn = $(this);
	$thisTr = $(this).parent().parent();
   	$question = $(this).parent().siblings('td[name="question"]').text();
   	$confidence = $(this).parent().siblings('td[name="confidence"]').text();
	$correctAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text();

   	$qaDialogDataOffset = $(this).parent().siblings('td[name="logId"]').attr("data-qa-dialog-offset");

   	data = { originalQuestion: $question, intents: [], entities: [] };

   	if ($qaDialogDataOffset != -1) {
   		data = $qaDialogDatas[$qaDialogDataOffset];
   	}
   	else {
   		data = { originalQuestion: $question, intents: [], entities: [] };
   	}

   	localStorage.setItem("qaDialogData", JSON.stringify(data));
   	window.open('qaDialogML.jsp');
});

var checkSimAndShowPanel = function($question, $q4sim) {
   	$( "#find-similar-dialog").dialog('close');
	$.getJSON('qaDataFindSimilar.jsp', {
   		q: typeof($q4sim) == 'undefined' ? $question : $q4sim
   	},
   	function(data){
   		var list = $('#check-list');
   		list.find('tr').remove();
   		list.append(
   				$('<tr class="text-warning"></tr>')
   				.append('<th><bean:message key="num"/></th><th><bean:message key="original.question"/>： <span id="sim-dialog-question">' + $question + '</span></th>')
   				.append($('<th></th>').append('<form name="newQuestionForm" action="qaDataEditor.jsp" method="post" target="_new"><textarea name="fromQ" class="hide">' + $question + '</textarea><input name="firstAlt" type="hidden"><button type="submit" class="btnCreateNewQ btn btn-danger"><span class="glyphicon glyphicon-plus-sign"></span></button></form>')
   						)
   				);
   		
		list.append('<tr><td colspan=2><input type="text" placeholder="<bean:message key='enter.question.or.kp'/>" name="question" class="form-control"></td>'
				+ '<td><button name="btnSearchAndSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
				+ '</td>');
		
		if (typeof($q4sim) != 'undefined') {
			list.find('input[name=question]').val($q4sim);
		}
		
   		if(data.length == 0){
   			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
   		}else{
   			for (var i=0; i < data.length; i++) {
   				var r = data[i];
   				list.append('<tr><td name="sim-kid">' + r.kid + '</td><td name="question" data-question="' + r.question + '"><a href="qaDataEditor.jsp?id=' + r.id + '" target="_blank">' 
   						+ r.question 
   						+ '</a></td>'
   						+ '<td><button name="btnSimFromSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
   						+ '<%if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) {%><button name="btnMagicFromSim" class="btn btn-primary" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class="glyphicon glyphicon-ok-sign"></span></button><% } %></td></tr>');
   			}
		    $('[data-toggle="tooltip"]').tooltip();
   		}
   		$( "#find-similar-dialog").dialog('open');
   	});
};

$(document).on('click', 'button[name="btnSearchAndSim"]', function() {
   	$question = $('#sim-dialog-question').text();
   	$q4sim = $(this).closest('tr').find('input[name="question"]').val();
   	checkSimAndShowPanel($question, $q4sim);
});

$(document).on('click', 'button[name="btnSimFromSim"]', function() {
   	$question = $('#sim-dialog-question').text();
   	$q4sim = $(this).parent().siblings('td[name="question"]').text();
   	checkSimAndShowPanel($question, $q4sim);
});

$(document).on('click', 'button[name="btnSim"]', function() {
   	$question = $(this).parent().siblings('td[name="question"]').text();
   	checkSimAndShowPanel($question);
});

$(document).on('click', '.btnCreateNewQ', function() {
	var $form = $(this).closest('form');
   	$question = $form.find('[name=fromQ]').val();

    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			dataType: 'json',
			data: {
	    		  q: $question,
			  replaceSyn: true,
	    		  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				$form.find('[name=firstAlt]').val(resp.nlp);
				$form.submit();
			},
	    	complete: function() {
	    	}});
    
	return false;
});

$(document).on('click', 'button[name="btnMagicFromSim"]', function() {
   	$question = $('#sim-dialog-question').text();
	$correctAnswerId = $(this).parent().siblings('td[name="sim-kid"]').text();

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $simEntryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%=locale.getLanguage() + locale.getCountry()%>'});

	$ml.start($data, (function(data, btn) {return function(stat) {
		msg = "";
		msg += "成功例句數量：" + stat.good + "<br>";
		msg += "失敗例句數量：" + stat.bad + "<br>";

		/*myConfirm("學習結果", msg, function() {
			$("#magic-dialog").dialog('close');
		});*/

		$dialog = $("#magic-dialog");
		if (stat.good + stat.bad > 1) {
			$("<h3 class='text text-danger'></h3>").appendTo($dialog).html(msg);
		}
		else {
			$("<h3/>").appendTo($dialog);
		}
		
		$("<button class='btn btn-default btnCloseMagicDialog'>Close</button>").appendTo($dialog);
	};})($data));
});

$(document).on('click', 'button[name="btnMagic"]', function() {
	$thisBtn = $(this);
	$thisTr = $(this).parent().parent();
   	$question = $(this).parent().siblings('td[name="question"]').text();
	$correctAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text();
	$userAnswerId = $(this).parent().siblings('td[name="correctAnswerId"]').text();

	$data = {
		q: $question,
		correctKid: $userAnswerId.trim() != '' ? $userAnswerId : $correctAnswerId,
		entryData: {}
	};

	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%=locale.getLanguage() + locale.getCountry()%>'});

	$ml.start($data, (function(data, btn) {return function(stat) {
		msg = "";
		msg += "成功例句數量：" + stat.good + "<br>";
		msg += "失敗例句數量：" + stat.bad + "<br>";

		/*myConfirm("學習結果", msg, function() {
			$("#magic-dialog").dialog('close');
		});*/

		$dialog = $("#magic-dialog");
		if (stat.good + stat.bad > 1) {
			$("<h3 class='text text-danger'></h3>").appendTo($dialog).html(msg);
		}
		else {
			$("<h3/>").appendTo($dialog);
		}
		
		$("<button class='btn btn-default btnCloseMagicDialog'>Close</button>").appendTo($dialog);
	};})($data, $thisBtn));
});

$(document).on('click', ".btnCloseMagicDialog", function(e) {
	$("#magic-dialog").dialog('close');
});

$(document).on('click', 'button[name="btnReviewHide"]', function() {
	$thisTr = $(this).parent().parent();
	$logId = parseInt($(this).parent().siblings('td[name="logId"]').attr("data-id"));

	if (confirm("<bean:message key='sure.hidden.record'/>")) {
		$('.loading-panel').show();
	    $.ajax({
			url: 'qa-log-status-ajax.jsp',
			dataType: 'json',
			data: {
	  		  id: $logId,
	  		  status: -1
			},
			error: function() {
				alert('<bean:message key="detection.network"/>');
			},
			success: function(resp) {
				$thisTr.remove();
			},
			complete: function() {
				setTimeout(function() {$('.loading-panel').hide();}, 300);
			}
	    });
	}
});

function testCallback(event, btn, stat) {
    var $thisBtn = btn;
	var $stat = stat;
	if (typeof(btn) == 'undefined') {
    	$thisBtn = $(this);
	}
	if (typeof($stat) == 'undefined') {
		$stat = {good:0, bad:0, unknown:0};
	}

	if (event != null) $('.loading-panel').show();

    return (function($insertAfterThisTr, $question, $correctAnswerId, $robotAnswerId) {return $.ajax({
   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
   		dataType: 'json',
   		async: true,
   		data: {
     		  q: $question,
     		  testMode: true,
			  tid: <%= t.getId() %>,
      		  html: true
   		},
   		error: function() {
   			alert('<bean:message key="detection.network"/>');
   		},
   		success: function(resp) {
   			var t = moment((new Date()).getTime());
				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
				var kid = null;
				var addClass = "text-primary";
				if (resp.hasOwnProperty("kid")) {
					kid = resp.kid;
				}
				if ((isNaN($correctAnswerId) && $robotAnswerId == kid) || $correctAnswerId == kid) {
					addClass = "text-success glyphicon glyphicon-ok-circle";
					$stat.good++;
				}
				else if (isNaN($correctAnswerId) && isNaN($robotAnswerId)){
					addClass = "text-warning glyphicon glyphicon-question-sign";
					$stat.unknown++;
				}
				else {
					addClass = "text-danger glyphicon glyphicon-remove-circle";
					$stat.bad++;
				}

				var tr = $('<tr/>');
				$('<td><h3></h3></td>').appendTo(tr);
				$('<td><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span></td>').appendTo(tr);
				$('<td></td>').appendTo(tr);
				$('<td/>').append($('<h3/>').append(getQuestionLink(kid, addClass))).appendTo(tr);
				$('<td>' + resp.output + '</td>').addClass('answer-text').appendTo(tr);
				$('<td>' + (resp.hasOwnProperty('confidence') ? resp.confidence : '') + '</td>').appendTo(tr);
				$('<td class="text-danger timeago" title="' + formatted + '">' + formatted + '</td>').appendTo(tr);
				$('<td></td>').appendTo(tr);

				tr.insertAfter($insertAfterThisTr);
     	},
   		complete: function() {
   	    	if (event != null) setTimeout(function() {$('.loading-panel').hide();}, 300);
   	    		$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry()%>');
				$(".timeago").timeago();
   		}
    });})
    ($thisBtn.parent().parent(),
    $thisBtn.parent().siblings('td[name="question"]').text(),
    parseInt($thisBtn.parent().siblings('td[name="correctAnswerId"]').text()),
    parseInt($thisBtn.parent().siblings('td[name="robotAnswerId"]').text())
    );
}

$(document).on('click', 'button[name="btnAutoTest"]', testCallback);
$('button[id="btnAutoTestAll"]').on('click', function() {
	$('.loading-panel').show();

	$thisBtn = $(this);
	$thisBtn.button('loading');
	var igFlag0 = $( "#checkIgnoreFlag0" ).prop( "checked" );
	var result = {good:0, bad:0, unknown:0};
	var btns = $('button[name="btnAutoTest"]');

	var finishAndPublishScore = function() {
    	$thisBtn.button('reset');

    	$div = $thisBtn.find('div');
    	$div.html('');
    	$h3 = $('<h4/>').appendTo($div);
    	$('<span/>').addClass("text-success glyphicon glyphicon-ok-circle").text(result.good).appendTo($h3);
    	$('<span/>').addClass("text-primary glyphicon glyphicon-question-sign").text(result.unknown).appendTo($h3);
    	$('<span/>').addClass("text-danger glyphicon glyphicon-remove-circle").text(result.bad).appendTo($h3);
    	$('.loading-panel').hide();
	}

	var deferredAjax = function(index, max) {
		$btn = $(btns[index]);
		var skipTest = false;

		if (igFlag0) {
           	var flag = parseInt($btn.parent().siblings('td[name="logId"]').attr("data-flag"));
           	if (flag == 0) {
           		skipTest = true;
           	}
		}

		if (!skipTest) {
			$.when(
		    		(function(ievent,ibtn) {
			    		return testCallback(ievent, ibtn, result);
		    		})(null, $btn)
	    		).then(
	    			function() {
	    				if (index < max) {
	    					deferredAjax(index + 1, max);
	    				}
	    				else {
	    					finishAndPublishScore();
	    				}
	    			});
		}
		else {
			if (index < max) {
				deferredAjax(index + 1, max);
			}
			else {
				finishAndPublishScore();
			}
		}

	};

	deferredAjax(0, btns.length - 1);
});

var systemNlp = '';

$(document).on('click', "button[name=btnNlp]", function(e) {
	var $thisBtn = $(this);
   	$question = $thisBtn.parent().siblings('td[name="question"]').text();

    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			dataType: 'json',
			data: {
    		  q: $question,
			  replaceSyn: true,
    		  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				alert(resp.nlp);
				systemNlp = resp.nlp;
			},
	    	complete: function() {
	    	}});
	return false;
});

$('#dateConfirm').click(function() {
	start = 0;
	getAndShowLog(filterData.start);
});

var jumpPage = function(start, rows) {
	filterData.start = start;
	filterData.rows = rows;
	
	window.location = 'qaEvaluationLogView.jsp?' + $.param(filterData);
	return false;
};

$(document).ready(function() {
	$('.loading-panel').show();
	$('#checkIgnoreFlag0').prop('checked', <%= checkIgnoreFlag0 %>);
	$('#checkDistinctQuestion').prop('checked', filterData.dq);

	if (filterData.hasOwnProperty("flag")) {
		$('input[name=flag][value=' + filterData.flag + ']').prop('checked', true);
	}

	if (filterData.hasOwnProperty("qaCategory")) {
		$('#qaCategory').val(filterData.qaCategory);
	}

	if (filterData.hasOwnProperty("afterDate")) {
		$('#afterDate').val(filterData.afterDate);
	}

	if (filterData.hasOwnProperty("beforeDate")) {
		$('#beforeDate').val(filterData.beforeDate);
	}
	
	if (filterData.hasOwnProperty("bypassAdmIds")) {
		$('#bypassAdmIds').val(filterData.bypassAdmIds.split(','));
	}

	$('input[type="checkbox"]').bootstrapToggle();
	
	getAndShowStat();
	getAndShowLog(filterData.start);
	
    $( "#magic-dialog").dialog({
		width: 600,
		minHeight: 300,
		position: ['center', 80],
		autoOpen: false
	});

    $( "#find-similar-dialog").dialog({
		width: 800,
		minHeight: 300,
		position: ['center', 50],
		autoOpen: false
	});
});
</script>

</div>
</body>
</html>