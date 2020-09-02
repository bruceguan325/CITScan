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
if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) {
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
<TITLE><bean:message key='question.answer.automatic.test.page'/></TITLE>
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
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>

<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src='<%= request.getContextPath() %>/script/jquery.fileDownload.js' type='text/javascript'></script>

<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
.test-stat th span {
	font-size: 2em;
	margin: 0 auto;
}
.test-stat td span {
	font-size: 1.5em;
	margin: 0 auto;

}
.test-stat td,th{
text-align: center;
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
<h2><bean:message key='record.statistics'/></h2>

<input type="checkbox" id="checkDistinctQuestion" name="checkDistinctQuestion" checked><bean:message key='check.distinct.question'/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<input type="checkbox" id="checkLTRstatus" name="checkLTRstatus" ><bean:message key="check.ltr.status"/><br>
<br>
<bean:message key='exclude.tester.num.multiple'/>
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
<button class="btn btn-warning" onclick="getAndShowStat()"><bean:message key="global.update" /></button>
<br>
<br>
<bean:message key="global.screening.tester"/> <select id="admId" name="admId">
	<option value="-1" selected>ALL</option>
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
<br>
<%
  Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();
  for(Integer idx : qaCategoryMap.keySet()) {
%>
<table id="test-stat-category-<%=idx %>" class="test-stat table table-striped table-bordered">
<tr>
	<th><span class="text-primary"><bean:message key='time.range'/></span></th>
	<th><span data-flag="0" data-qa-category="<%=idx%>" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
	<th><span data-flag="1" data-qa-category="<%=idx%>" class="text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
	<th><span data-flag="2" data-qa-category="<%=idx%>" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span></th>
	<th><span data-flag="3" data-qa-category="<%=idx%>" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span></th>
	<th><span data-flag="4" data-qa-category="<%=idx%>" class="text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span></th>
	<th><span data-flag="5" data-qa-category="<%=idx%>" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span></th>
	<th><span data-flag="6" data-qa-category="<%=idx%>" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span></th>
	<th><span class="text-primary"><bean:message key='global.accuracy'/></span></th>
</tr>
<tr>
	<td class="stat-range" data-date-range='today' data-qa-category="<%=idx%>"><span></span></td>
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
	<td class="stat-range" data-date-range='yesterday' data-qa-category="<%=idx%>"><span></span></td>
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
	<td class="stat-range" data-date-range='thisWeek' data-qa-category="<%=idx%>"><span></span></td>
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
	<td class="stat-range" data-date-range='lastWeek' data-qa-category="<%=idx%>"><span></span></td>
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
	<td class="stat-range" data-date-range='all' data-qa-category="<%=idx%>"><span></span></td>
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
<button class="btn btn-danger" id="btnExport" target="_download"><bean:message key='export.excel'/></button>
</div>

</div>

<div class="row" style="margin-top: 10px;">
	<div class="col-md-7">
	<h2><bean:message key='automatic.test.result'/></h2>
	<h4><bean:message key='test.time'/>：</h4>
	<table id="test-stat-eat-N" class="test-stat table table-striped table-bordered">
	<tr>
		<th><span class="text-primary"><bean:message key='time.range'/></span></th>
		<th><span data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key='test.no.select.evaluation'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
		<th><span data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span></th>
		<th><span data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span></th>
		<th><span data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span></th>
		<th><span data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span></th>
		<th><span data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span></th>
		<th><span data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span></th>
		<th><span class="text-primary"><bean:message key='global.accuracy'/></span></th>
	</tr>
	<tr>
		<td class="stat-range"><span><button name="btnMassiveAutoTest" data-test-type="today" data-loading-text="<bean:message key='global.testing'/>"><bean:message key="global.today"/></button></span></td>
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
		<td class="stat-range"><span><button name="btnMassiveAutoTest" data-test-type="yesterday" data-loading-text="<bean:message key='global.testing'/>"><bean:message key="global.yesterday"/></button></span></td>
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
		<td class="stat-range"><span><button name="btnMassiveAutoTest" data-test-type="thisWeek" data-loading-text="<bean:message key='global.testing'/>"><bean:message key='global.this.week'/></button></span></td>
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
		<td class="stat-range"><span><button name="btnMassiveAutoTest" data-test-type="lastWeek" data-loading-text="<bean:message key='global.testing'/>"><bean:message key='global.last.week'/></button></span></td>
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
		<td class="stat-range"><span><button name="btnMassiveAutoTest" data-test-type="all" data-loading-text="<bean:message key='global.testing'/>"><bean:message key='global.all'/></button></span></td>
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

	<div class="col-md-3">
	<div id="massive-autotest-progress-bar-1" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    1
	  </div>
	</div>
	<div id="massive-autotest-progress-bar-2" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    2
	  </div>
	</div>
	<div id="massive-autotest-progress-bar-3" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    3
	  </div>
	</div>
	<div id="massive-autotest-progress-bar-4" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    4
	  </div>
	</div>
	<div id="massive-autotest-progress-bar-5" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    5
	  </div>
	</div>
	<div id="massive-autotest-progress-bar-6" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    6
	  </div>
	</div>
	</div>
</div>

<div class="row">
<div class="col-md-12">
	<table id="test-list" class="table table-striped table-bordered table-hover">
	<tr>
		<th><bean:message key='global.evaluation'/></th>
		<th><bean:message key='global.tester'/></th>
		<th><bean:message key='test.question'/></th>
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
<div class='loading-panel'></div>

<script>
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
var flagGlyphs = [
	          		'<span data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key="test.no.select.evaluation"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	          		'<span data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key="test.record.well"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	          		'<span data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key="test.record.good"/>" aria-hidden="true"></span>',
	          		'<span data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key="test.record.no.answer"/>" aria-hidden="true"></span>',
	          		'<span data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key="test.record.no.expect"/>" aria-hidden="true"></span>',
	          		'<span data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key="test.record.bad"/>" aria-hidden="true"></span>',
	          		'<span data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key="test.record.worst"/>" aria-hidden="true"></span>',
	                  ];

var rows = 100;
var start = 0;
var flag;
var timeRange;
var logStat;
var lastTestResult;
var qaCategoryIdx;

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
	
	loopAll = function() {

		$.getJSON('qa-log-statistics-ajax.jsp', {
			admId: $( "#admId" ).val(),
			bypassAdmIds: byPassIds,
			allCategories: true,
			allTimeRanges: true,
			dq: distinctQuestion
		},
		function(data){
			for(var idx = 0 ; idx < qaCateKeys.length ; idx++) {
				var key = qaCateKeys[idx].toString();
				fillStat($('#test-stat-category-' + key + ' tr:eq(1)'), "<bean:message key='global.today'/>", data['flagStatistics_' + idx + '_today']);
				fillStat($('#test-stat-category-' + key + ' tr:eq(2)'), "<bean:message key='global.yesterday'/>", data['flagStatistics_' + idx + '_yesterday']);
				fillStat($('#test-stat-category-' + key + ' tr:eq(3)'), "<bean:message key='global.this.week'/>", data['flagStatistics_' + idx + '_thisWeek']);
				fillStat($('#test-stat-category-' + key + ' tr:eq(4)'), "<bean:message key='global.last.week'/>", data['flagStatistics_' + idx + '_lastWeek']);
				fillStat($('#test-stat-category-' + key + ' tr:eq(5)'), "<bean:message key='global.all'/>", data['flagStatistics_' + idx]);
			}			
			$('.loading-panel').hide();
		});
	};
	
	if (qaCateKeys.length > 0)
		loopAll();
}

$('#btnExport').click(function() {
	var url = "qaEvaluationLog-ajax.jsp";

	options = { format: "xlsx", ed: true };

	var dq = $( "#checkDistinctQuestion" ).is(':checked');

	if(typeof(dq) != 'undefined') {
		options.dq = dq;
	}

	if (typeof(flag) != 'undefined') {
		options.flag = flag;
	}
	if (typeof(timeRange) != 'undefined') {
		options.timeRange = timeRange;
	}
	if (typeof(lastTestResult) != 'undefined') {
		options.ltr = lastTestResult;
	}
	var admId = $( "#admId" ).val();
	if(typeof(admId) != 'undefined') {
		options.admId = admId;
	}

	$.fileDownload(url,
		{
	        preparingMessageHtml: "We are preparing your report, please wait...",
	        failMessageHtml: "There was a problem generating your report, please try again.",
	        httpMethod: "GET",
	        data: options
	    });
});

$('#checkDistinctQuestion').change(function() {
	getAndShowStat();
});

$('#checkLTRstatus').change(function() {
	var checkLTRstatus = $( "#checkLTRstatus" ).is(':checked');
	if (checkLTRstatus) {
		lastTestResult = 0;
	}
	else {
		lastTestResult = null;
	}
	start = 0;
	getAndShowLog(start);
});

$('#admId').on('change', function() {
	getAndShowStat();
	start = 0;
	getAndShowLog(start);
});

$('[id^=test-stat-category] span[data-flag]').click(function() {
	$this = $(this);
	flag = parseInt($this.attr('data-flag'));
	qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	getAndShowLog(start);
});

$('[id^=test-stat-category] td.stat-range').click(function() {
	$this = $(this);
	timeRange = $this.attr('data-date-range');
	qaCategoryIdx = $this.attr('data-qa-category');
	start = 0;
	getAndShowLog(start);
});

function getAndShowLog(s) {
	var distinctQuestion = $( "#checkDistinctQuestion" ).is(':checked');
	var admId = $( "#admId" ).val();
	var options = {admId: admId, start: s, rows: rows};
	if (typeof(flag) != 'undefined') {
		options.flag = flag;
	}
	if (typeof(timeRange) != 'undefined') {
		options.timeRange = timeRange;
	}
	if (typeof(lastTestResult) != 'undefined') {
		options.ltr = lastTestResult;
	}
	if(typeof(qaCategoryIdx) != 'undefined') {
		options.qaCategoryIdx = qaCategoryIdx;
	}
	options.dq = distinctQuestion;

	$.getJSON('qaEvaluationLog-ajax.jsp', options,
		function(data){
			var list = $('#test-list');
			list.find('tr:has(td)').remove();
			if(data.length == 0){
				list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
				$('#btnNextPage').attr("disabled", "disabled");
			}else{
				$('#btnNextPage').removeAttr("disabled");

				console.log("Got logs [" + data.length + "]");

				for (var i=0; i < data.length; i++) {
					var r = data[i];
					var tr = $('<tr/>');
					var cv = JSON.parse(r.conversations);
					var t = moment( r.timestamp );
					var formatted = t.format("YYYY/MM/DD HH:mm:ss");

					$('<td name="logId" data-id="' + r.id + '" data-flag="' + r.flag + '"><h3>' + flagGlyphs[r.flag] + '</h3></td>').appendTo(tr);
					$('<td name="admId">' + r.admId + '</td>').appendTo(tr);
					$('<td name="question">' + r.question + '</td>').appendTo(tr);
					$('<td name="correctAnswerId"></td>').append(getQuestionLink(r.correctAnswerId)).appendTo(tr);
					$('<td name="robotAnswerId"></td>').append(getQuestionLink(r.robotAnswerId)).appendTo(tr);
					$('<td>' + cv.output + '</td>').appendTo(tr);
					$('<td>' + (cv.hasOwnProperty('confidence') ? cv.confidence : '') + '</td>').appendTo(tr);
					$('<td>' + formatted + '</td>').appendTo(tr);
					$('<td><button name="btnAutoTest" class="btn btn-warning"><bean:message key="global.test"/></button><button name="btnReviewHide" class="btn btn-danger"><bean:message key="global.hidden"/></button></td>').appendTo(tr);

					list.append(tr);
				}
			}

			if (s == 0)
				$('#btnPrevPage').attr("disabled", "disabled");
			else
				$('#btnPrevPage').removeAttr("disabled");

			$( "#test-dialog").dialog('open');
		});
}

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
	var $thisBtn = $(this);
	var $stat = stat;
	if (typeof(btn) != 'undefined') {
		$thisBtn = btn;
	}
	if (typeof($stat) == 'undefined') {
		$stat = {good:0, bad:0, unknown:0};
	}
   	$('.loading-panel').show();
   	$insertAfterThisTr = $thisBtn.parent().parent();
   	$correctAnswerId = parseInt($thisBtn.parent().siblings('td[name="correctAnswerId"]').text());
   	$robotAnswerId = parseInt($thisBtn.parent().siblings('td[name="robotAnswerId"]').text());
   	var message = $thisBtn.parent().siblings('td[name="question"]').text();

    return $.ajax({
   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
   		dataType: 'json',
   		async: false,
   		data: {
     		  q: message,
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
				$('<td><h3></h3></td><td/>').appendTo(tr);
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
   			setTimeout(function() {$('.loading-panel').hide();}, 300);
				$(".timeago").timeago();
   		}
    });
}

$(document).on('click', 'button[name="btnAutoTest"]', testCallback);

$('button[name="btnMassiveAutoTest"]').on('click', function() {
	$thisBtn = $(this);
	$thisBtn.button('loading');
	$theTR = $(this).parents('tr');
	$testType = $thisBtn.attr("data-test-type");
	var t = moment((new Date()).getTime());
	var formatted = t.format("YYYY/MM/DD HH:mm:ss");
	$thisBtn.parent().append('<div>' + formatted + '</div>');
	var stats;

	if ($testType == 'today') {
		stats = logStat.flagStatistics_today;
	}
	else if ($testType == 'yesterday') {
		stats = logStat.flagStatistics_yesterday;
	}
	else if ($testType == 'thisWeek') {
		stats = logStat.flagStatistics_thisWeek;
	}
	else if ($testType == 'lastWeek') {
		stats = logStat.flagStatistics_lastWeek;
	}
	else if ($testType == 'all') {
		stats = logStat.flagStatistics;
	}

	var distinctQuestion = $( "#checkDistinctQuestion" ).is(':checked');
	var totalGood = 0, totalBad = 0, totalWoFlag0And3 = 0;
	var runners = {};

	for (var i=1; i < stats.length; i++) {
		if (i == 3) continue;
		if (i != 0 && i != 3)
			totalWoFlag0And3 += stats[i];

		runners['flag' + i] = (function(idx){
			var good = 0, bad = 0, unknown = 0, total = stats[idx];

			return $.ajax({	url: 'qaEvaluationLog-ajax.jsp',
				data: {start: start, rows: total, flag: idx, timeRange: $testType, dq: distinctQuestion},
				dataType: 'json',
				//async: false,
				success: function(data) {
					var deferredQAAjax = (function(data) {
						var logData = data;

						return function(index, max) {

							ajaxOption = {
							   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
							   		dataType: 'json',
							   		data: {
							     		  q: logData[index].question,
							     		  testMode: true,
							    		  tid: <%= t.getId() %>,
							    		  elid: logData[index].id,
						          		  html: true
							   		},
							   		error: function() {
							   			alert('[' + r.id + ']<bean:message key="detection.network"/>');
							   		},
							   		success: function(resp) {
									   	$correctAnswerId = logData[index].correctAnswerId;
									   	$robotAnswerId = logData[index].robotAnswerId;
									   	var ltr = 0;
										var kid = null;
										if (resp.hasOwnProperty("kid")) {
											kid = resp.kid;
										}
										if ($correctAnswerId == kid) {
											good++;
											totalGood++;
											ltr = 1;
										}
										else if (idx == 4 && $robotAnswerId == kid) {
											good++;
											totalGood++;
											ltr = 1;
										}
										else {
											bad++;
											totalBad++;
											ltr = 0;
										}

										(function(id, ltr) {
											$.ajax({
												url: "qa-log-ltr-ajax.jsp",
												data: {id:id, ltr:ltr},
												dataType: "json",
												success: function() {}
											});
										})(logData[index].id, ltr);
						     		},
							   		complete: function() {
							   			var pb = $('#massive-autotest-progress-bar-' + idx + ' .progress-bar');
							   			var percent = Math.round((good+bad+unknown)*100 / total) + "%";
							   			pb.css({"width": percent});
							   			var symbol = $(flagGlyphs[idx]).removeClass('text-danger').removeClass('text-success').removeClass('text-warning');
							   			pb.html('');
							   			pb.append(symbol);
							   			pb.append("<span>" + percent + "(" + (good+bad+unknown) + "/" + total + ")</span>");
										$theTR.find('.stat-num:eq(' + idx + ')').text("Good:" + good + "/ Bad:" + bad);
										$theTR.find('.stat-num:eq(7)').text(Math.round(totalGood * 100 / totalWoFlag0And3) + '%' );
							   		}
							    };

							if (index < max) {
								return $.ajax(ajaxOption).then(function() {deferredQAAjax(index+1, max)});
							}
							else {
								return $.ajax(ajaxOption);
							}
						}
					})(data);

					if (data.length > 0)
						deferredQAAjax(0, data.length - 1);
				}
			});

			//return {good:good, bad:bad, unknown:unknown, total:total};
		})(i);
	}
	console.log(runners);

	$thisBtn.button('reset');
});

$('#btnPrevPage').click(function() {
	start -= rows;
	getAndShowLog(start);
});
$('#btnNextPage').click(function() {
	start += rows;
	getAndShowLog(start);
});

$(document).ready(function() {
	$('.loading-panel').show();
	getAndShowStat();
	getAndShowLog(start);
});
</script>

</div>
</body>
</html>