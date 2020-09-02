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
import="com.intumit.hithot.HitHotLocale"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="org.json.JSONArray"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = HitHotLocale.determineLocale(request, true, true);
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<script type="text/javascript">
    /**
     * Timeago is a jQuery plugin that makes it easy to support automatically
     * updating fuzzy timestamps (e.g. "4 minutes ago" or "about 1 day ago").
     *
     * @name timeago
     * @version 1.2.0
     * @requires jQuery v1.2.3+
     * @author Ryan McGeary
     * @license MIT License - http://www.opensource.org/licenses/mit-license.php
     *
     * For usage and examples, visit:
     * http://timeago.yarp.com/
     *
     * Copyright (c) 2008-2013, Ryan McGeary (ryan -[at]- mcgeary [*dot*] org)
     */

    (function (factory) {
        if (typeof define === 'function' && define.amd) {
            // AMD. Register as an anonymous module.
            define(['jquery'], factory);
        } else {
            // Browser globals
            factory(jQuery);
        }
    }(function ($) {
      $.timeago = function(timestamp) {
        if (timestamp instanceof Date) {
          return inWords(timestamp);
        } else if (typeof timestamp === "string") {
          return inWords($.timeago.parse(timestamp));
            } else if (typeof timestamp === "number") {
                return inWords(new Date(timestamp));
        } else {
          return inWords($.timeago.datetime(timestamp));
        }
      };
      var $t = $.timeago;

      $.extend($.timeago, {
        settings: {
          refreshMillis: 60000,
          allowFuture: false,
                localeTitle: false,
                cutoff: 0,
          strings: {
            prefixAgo: null,
            prefixFromNow: null,
            suffixAgo: "<bean:message key='global.front'/>",
            suffixFromNow: "<bean:message key='global.ago'/>",
            seconds: "<bean:message key='less.one.mintues'/>",
            minute: "<bean:message key='about.one.minutes'/>",
            minutes: "%d<bean:message key='global.mintues.one'/>",
            hour: "<bean:message key='global.about'/>1<bean:message key='global.hour'/>",
            hours: "<bean:message key='global.about'/>%d<bean:message key='global.hour'/>",
            day: "1<bean:message key='global.sky'/>",
            days: "%d<bean:message key='global.sky'/>",
            month: "1<bean:message key='global.one.day'/>",
            months: "%d<bean:message key='global.one.day'/>",
            year: "1<bean:message key='global.year'/>",
            years: "%d<bean:message key='global.year'/>",
            wordSeparator: "",
            numbers: []
          }
        },
        inWords: function(distanceMillis) {
          var $l = this.settings.strings;
          var prefix = $l.prefixAgo;
          var suffix = $l.suffixAgo;
          if (this.settings.allowFuture) {
            if (distanceMillis < 0) {
              prefix = $l.prefixFromNow;
              suffix = $l.suffixFromNow;
            }
          }

                var seconds = Math.abs(distanceMillis) / 1000;
          var minutes = seconds / 60;
          var hours = minutes / 60;
          var days = hours / 24;
          var years = days / 365;

          function substitute(stringOrFunction, number) {
            var string = $.isFunction(stringOrFunction) ? stringOrFunction(number, distanceMillis) : stringOrFunction;
            var value = ($l.numbers && $l.numbers[number]) || number;
            return string.replace(/%d/i, value);
          }

          var words = seconds < 45 && substitute($l.seconds, Math.round(seconds)) ||
            seconds < 90 && substitute($l.minute, 1) ||
            minutes < 45 && substitute($l.minutes, Math.round(minutes)) ||
            minutes < 90 && substitute($l.hour, 1) ||
            hours < 24 && substitute($l.hours, Math.round(hours)) ||
                    hours < 42 && substitute($l.day, 1) ||
                    days < 30 && substitute($l.days, Math.round(days)) ||
                    days < 45 && substitute($l.month, 1) ||
                    days < 365 && substitute($l.months, Math.round(days / 30)) ||
                    years < 1.5 && substitute($l.year, 1) ||
                    substitute($l.years, Math.round(years));

                var separator = $l.wordSeparator || "";
                if ($l.wordSeparator === undefined) { separator = " "; }
                return $.trim([prefix, words, suffix].join(separator));
        },
        parse: function(iso8601) {
          var s = $.trim(iso8601);
                s = s.replace(/\.\d+/,""); // remove milliseconds
                s = s.replace(/-/,"/").replace(/-/,"/");
                s = s.replace(/T/," ").replace(/Z/," UTC");
          s = s.replace(/([\+\-]\d\d)\:?(\d\d)/," $1$2"); // -04:00 -> -0400
          return new Date(s);
        },
        datetime: function(elem) {
                var iso8601 = $t.isTime(elem) ? $(elem).attr("datetime") : $(elem).attr("title");
                return $t.parse(iso8601);
            },
            isTime: function(elem) {
          // jQuery's `is()` doesn't play well with HTML5 in IE
                return $(elem).get(0).tagName.toLowerCase() === "time"; // $(elem).is("time");
          }
        });

        // functions that can be called via $(el).timeago('action')
        // init is default when no action is given
        // functions are called with context of a single element
        var functions = {
            init: function(){
                var refresh_el = $.proxy(refresh, this);
                refresh_el();
                var $s = $t.settings;
                if ($s.refreshMillis > 0) {
                    setInterval(refresh_el, $s.refreshMillis);
          }
            },
            update: function(time){
                $(this).data('timeago', { datetime: $t.parse(time) });
                refresh.apply(this);
          }
        };

        $.fn.timeago = function(action, options) {
            var fn = action ? functions[action] : functions.init;
            if(!fn){
                throw new Error("Unknown function name '"+ action +"' for timeago");
        }
            // each over objects here and call the requested function
            this.each(function(){
                fn.call(this, options);
      });
            return this;
      };

      function refresh() {
        var data = prepareData(this);
            var $s = $t.settings;

        if (!isNaN(data.datetime)) {
                if ( $s.cutoff == 0 || distance(data.datetime) < $s.cutoff) {
          $(this).text(inWords(data.datetime));
        }
            }
        return this;
      }

      function prepareData(element) {
        element = $(element);
        if (!element.data("timeago")) {
          element.data("timeago", { datetime: $t.datetime(element) });
          var text = $.trim(element.text());
                if ($t.settings.localeTitle) {
                    element.attr("title", element.data('timeago').datetime.toLocaleString());
                } else if (text.length > 0 && !($t.isTime(element) && element.attr("title"))) {
            element.attr("title", text);
          }
        }
        return element.data("timeago");
      }

      function inWords(date) {
        return $t.inWords(distance(date));
      }

      function distance(date) {
        return (new Date().getTime() - date.getTime());
      }

      // fix for IE6 suckage
      document.createElement("abbr");
      document.createElement("time");
    }));
    </script>
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
String logSource = StringUtils.trimToNull(request.getParameter("logSource"));
if (logSource != null) {
	logSource = StringEscapeUtils.escapeJavaScript(logSource);
}
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

<div class="col-md-3">
排除測試者編號(多選)
<select style="width:200px; height:100px;" name="bypassAdmIds" id="bypassAdmIds" multiple>
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
<input type="checkbox" id="checkIgnoreFlag0" name="checkIgnoreFlag0" checked><bean:message key='ignore.not.select'/>
<button id='btnAutoTestAll' data-loading-text="<bean:message key='global.testing'/>" class='btn btn-default' style="margin: 0 10px;"><bean:message key='test.all'/><br><div class='testScore'></div></button>
</div>

</div>

<div class="row">
<div class="col-md-12">
<bean:message key='time.range'/>&nbsp
<input type="datetime-local" name="afterDate" >&nbsp-&nbsp<input type="datetime-local" name="beforeDate" >&nbsp
<button id="dateConfirm"><bean:message key='global.confirm'/></button>
</div>
</div>
</br>

<div class="row">
<div class="col-md-12">
	<table id="test-list" class="table table-striped table-bordered table-hover">
	<tr>
		<th><bean:message key='global.evaluation'/></th>
		<th><bean:message key="top.question.method"/></th>
		<th><bean:message key='knowledge.point'/></th>
		<th>Count</th>
		<th>其中某個<bean:message key='test.question'/></th>
		<th><bean:message key='test.expected.answer.number'/></th>
		<th><bean:message key='robot.answer.number'/></th>
		<th><bean:message key='robot.answer'/></th>
		<th><bean:message key='global.confidence.level'/></th>
		<th><bean:message key='test.time'/></th>
		<th><bean:message key='operation'/></th>
	</tr>
	</table>
	<button id="btnPrevPage"><bean:message key='previous.page'/></button>
	<button id="btnNextPage"><bean:message key='next.page'/></button>
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

		$(tr).find('.stat-num:eq(7) span').text( Math.round(good * 100 / totalWoFlag0And3) + '%' );
	}
	var byPassIds = "";
	if ($( "#bypassAdmIds" ).val() != null){
		byPassIds = $( "#bypassAdmIds" ).val().toString();
	}
	
	params = {bypassAdmIds: byPassIds};
	<% if (logSource != null) { %>
	params['logSource'] = '<%= logSource %>';
	<% } %>
	
	$.getJSON('qa-log-statistics-ajax.jsp', 
		params,
		function(data){
			<%
			for(Integer idx : qaCategoryMap.keySet()) {
			%>
			fillStat($('#test-stat-<%=idx%> tr:eq(1)'), "<bean:message key='past.twenty.four.hours'/>", data.flagStatistics_<%=idx%>_past24hr);
			fillStat($('#test-stat-<%=idx%> tr:eq(2)'), "<bean:message key='global.this.week'/>", data.flagStatistics_<%=idx%>_thisWeek);
			fillStat($('#test-stat-<%=idx%> tr:eq(3)'), "<bean:message key='global.last.week'/>", data.flagStatistics_<%=idx%>_lastWeek);
			fillStat($('#test-stat-<%=idx%> tr:eq(4)'), "<bean:message key='global.all'/>", data.flagStatistics_<%=idx%>);
	        <% } %>
		});
}

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

function getAndShowLog(s, qaCategoryIdx) {
	var options = {start: s, rows: rows};
	if (typeof(flag) != 'undefined') {
		options.flag = flag;
	}
	if (typeof(timeRange) != 'undefined') {
		options.timeRange = timeRange;
	}
	if(typeof(qaCategoryIdx) != 'undefined') {
		options.qaCategoryIdx = qaCategoryIdx;
	}
	
	var afterDate = $('[name=afterDate]').val();
	var beforeDate = $('[name=beforeDate]').val();
	if(afterDate != null && afterDate != '' && beforeDate != null && beforeDate != '' ) {
		options.afterDate = afterDate;
		options.beforeDate = beforeDate;
	} else if ( (afterDate != null && afterDate != '' && (beforeDate == null || beforeDate == '')) ||
		        ( (afterDate == null || afterDate == '') && beforeDate != null && beforeDate != '')) {
		alert('您的時間範圍沒有填寫完整故不會生效');
	}
	
	$.getJSON('qaEvaluationLogGrouping-ajax.jsp', options,
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

		var list = $('#test-list');
		list.find('tr:has(td)').remove();
		if(data.length == 0){
			list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
			$('#btnNextPage').attr("disabled", "disabled");
		}else{
			$('#btnNextPage').removeAttr("disabled");

			for (var i=0; i < data.length; i++) {
				var r = data[i];
				var tr = $('<tr/>');
				var cv = JSON.parse(r.conversations);
				var t = moment( r.timestamp );
				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
				formatted += "<br><span class='text-danger'>【" + r.admId + "】</span>";

				$('<td name="logId" data-id="' + r.id + '" data-flag="' + r.flag + '"><h3>' + flagGlyphs[r.flag] + '</h3></td>').appendTo(tr);
				$('<td name="suggestQaTemplateMkey">' + r.suggestQaTemplateMkey + '</td>').appendTo(tr);
				$('<td name="suggestQaTemplateKeywords">' + r.suggestQaTemplateKeywords + '</td>').appendTo(tr);
				$('<td name="count">' + r.count + '</td>').appendTo(tr);
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

		if (s == 0)
			$('#btnPrevPage').attr("disabled", "disabled");
		else
			$('#btnPrevPage').removeAttr("disabled");

		$( "#test-dialog").dialog('open');
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

$(document).on('click', 'button[name="btnSim"]', function() {
   	$question = $(this).parent().siblings('td[name="question"]').text();

   	$( "#find-similar-dialog").dialog('close');
   	$.getJSON('qaDataFindSimilar.jsp', {
   		q: $question
   	},
   	function(data){
   		var list = $('#check-list');
   		list.find('tr').remove();
   		list.append(
   				$('<tr class="text-warning"></tr>')
   				.append('<th>編號</th><th>原始問句： <span id="sim-dialog-question">' + $question + '</span></th>')
   				.append($('<th></th>').append('<form name="newQuestionForm" action="qaDataEditor.jsp" method="post" target="_new"><textarea name="fromQ" class="hide">' + $question + '</textarea><input name="firstAlt" type="hidden"><button type="submit" class="btnCreateNewQ btn btn-danger"><span class="glyphicon glyphicon-plus-sign"></span></button></form>')
   						)
   				);
   		if(data.length == 0){
   			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
   		}else{
   			for (var i=0; i < data.length; i++) {
   				var r = data[i];
   				list.append('<tr><td name="sim-kid">' + r.kid + '</td><td><a href="qaDataEditor.jsp?id=' + r.id + '" target="_blank">' 
   						+ r.question 
   						+ '</a></td><td><%if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) {%><button name="btnMagicFromSim" class="btn btn-primary" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class="glyphicon glyphicon-ok-sign"></span></button><% } %></td></tr>');
   			}
		    $('[data-toggle="tooltip"]').tooltip();
   		}
   		$( "#find-similar-dialog").dialog('open');
   	});
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
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>});

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
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>});

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
				$('<td></td>').appendTo(tr);
				$('<td></td>').appendTo(tr);
				$('<td></td>').appendTo(tr);
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

$('#btnPrevPage').click(function() {
	start -= rows;
	getAndShowLog(start);
});
$('#btnNextPage').click(function() {
	start += rows;
	getAndShowLog(start);
});
$('#dateConfirm').click(function() {
	start = 0;
	getAndShowLog(start);
});
$(document).ready(function() {
	getAndShowStat();
	getAndShowLog(start);
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