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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) {
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%><%!
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
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<div class="row" style="margin-top: 10px;">

<div class="col-md-7">
<%
Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();
for(Integer idx : qaCategoryMap.keySet()) {
%>
<table id="test-stat-<%=idx %>" class="table table-striped table-bordered">

<tr>
	<th><span class="text-primary"><bean:message key='time.range'/></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span></th>
	<th><span data-qa-category="<%=idx %>" data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span></th>
	<th><span class="text-primary"><bean:message key='global.accuracy'/></span></th>
</tr>
<tr>
	<td class="stat-range" data-date-range='past24hr' data-qa-category='<%=idx%>'><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
</tr>
<tr>
	<td class="stat-range" data-date-range='thisWeek' data-qa-category='<%=idx%>'><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
</tr>
<tr>
	<td class="stat-range" data-date-range='lastWeek' data-qa-category='<%=idx%>'><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
</tr>
<tr>
	<td class="stat-range" data-date-range='all' data-qa-category='<%=idx%>'><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
	<td class="stat-num"><span></span></td>
</tr>
<tr>
  <td colspan="9" class="text-primary"><span><bean:message key="qacategory.name"/> : <%= qaCategoryMap.get(idx) %></span></td>
</tr>
</table>
<% } %>
</div>

<div class="col-md-2">
<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
<a href="qaEvaluationLog-ajax.jsp?format=xlsx" class="btn btn-danger" target="_download"><bean:message key='export.excel'/></a>
<% } %>
</div>

<div class="col-md-3">
<input type="checkbox" id="checkDistinctQuestion" data-size="mini" name="checkDistinctQuestion" checked> <bean:message key='check.distinct.question'/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<br>
<bean:message key='exclude.tester.num.multiple'/>
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
<button class="btn btn-warning" onclick="getAndShowStat()">更新</button>
<br>
<br>
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

var rows = 50;
var start = 0;
var flag;
var timeRange;

function getAndShowStat() {
	var distinctQuestion = $( "#checkDistinctQuestion" ).is(':checked');

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

	var qaCateKeys = <%= new JSONArray(qaCategoryMap.keySet()).toString()  %>;
	var qaCategories = <%= new org.json.JSONObject(qaCategoryMap).toString()  %>;
	
	loopAll = function(idx) {
		key = qaCateKeys[idx].toString();
		cate = qaCategories[key];
		
		$.getJSON('qa-log-statistics-ajax.jsp', {
			bypassAdmIds: byPassIds,
			allCategories: false,
			allTimeRanges: true,
			qaCategory: cate,
			dq: distinctQuestion
		},
		function(data){
			fillStat($('#test-stat-' + key + ' tr:eq(1)'), "<bean:message key='past.twenty.four.hours'/>", data.flagStatistics_past24hr);
			fillStat($('#test-stat-' + key + ' tr:eq(2)'), "<bean:message key='global.this.week'/>", data.flagStatistics_thisWeek);
			fillStat($('#test-stat-' + key + ' tr:eq(3)'), "<bean:message key='global.last.week'/>", data.flagStatistics_lastWeek);
			fillStat($('#test-stat-' + key + ' tr:eq(4)'), "<bean:message key='global.all'/>", data.flagStatistics);
			
			if (idx + 1 < qaCateKeys.length) {
				loopAll(idx+1);
			}
			else {
	    			$('.loading-panel').hide();
			}
		});
	};
	
	if (qaCateKeys.length > 0)
		loopAll(0);
}

$('[id^=test-stat-] span[data-flag]').click(function() {
	$this = $(this);
	flag = parseInt($this.attr('data-flag'));
	var qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	//getAndShowLog(start, qaCategoryIdx);
});

$('[id^=test-stat-] td.stat-range').click(function() {
	$this = $(this);
	timeRange = $this.attr('data-date-range');
	qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	//getAndShowLog(start, qaCategoryIdx);
});

$(document).ready(function() {
	$('.loading-panel').show();
	$('input[type="checkbox"]').bootstrapToggle();
	getAndShowStat();
	//getAndShowLog(start);
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