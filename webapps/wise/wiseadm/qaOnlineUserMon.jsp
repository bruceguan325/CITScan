<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%!
String getHitRatesTextColorClass(float rate) {
	if (rate < 0.5f) {
		return "text-danger";
	}
	else if (rate < 0.85f) {
		return "text-warning";
	}
	return "text-success";
}
%>
<%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	if (t == null) {
		response.sendRedirect(request.getContextPath() + "/wiseadm/chooseTenant.jsp?r=" + URLEncoder.encode(request.getRequestURL().toString(), "UTF-8"));
		return;
	}
%>
<%
String[] timeRanges = {"today", "yesterday", "thisWeek"};
float[] rates = {0f, 0f, 0f};
String[] ratesColor = {"", "", ""};

for (int i=0; i < timeRanges.length; i++) {
	String timeRange = timeRanges[i];
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(timeRange);
	System.out.println(timeRangePair[0]);
	System.out.println(timeRangePair[1]);
	try {
		float rate = ServiceLogEntity.hitrateStatistic(timeRangePair[0], timeRangePair[1], t.getId())[3];
		rates[i] = rate;
		ratesColor[i] = getHitRatesTextColorClass(rate);
	}
	catch (Exception e) {
		float rate = 0;
		rates[i] = rate;
		ratesColor[i] = getHitRatesTextColorClass(rate);
	}
}

Calendar _30minAgo = Calendar.getInstance();
_30minAgo.add(Calendar.MINUTE, -30);

List<ServiceLogEntity> onlineSessions = ServiceLogEntity.listLatestByStatus(t.getId(), "robot:cathay:chat", ForwardToCrmRule.Status.ENTERING.name(), _30minAgo, 1000);
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='oline.user'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<jsp:include page="header-qa.jsp"></jsp:include>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h3 id="brqslm-h" class="">
		<bean:message key='present.online.user'/>：<%= onlineSessions.size() %><bean:message key='global.persion'/> ,
		<bean:message key='hitrates.today'/>：<span class="<%= ratesColor[0] %>"><%= String.valueOf(rates[0] * 100) %>%</span> ,
		<bean:message key='hitrates.yesterday'/>：<span class="<%= ratesColor[1] %>"><%= String.valueOf(rates[1] * 100) %>%</span> ,
		<bean:message key='hitrates.thisweek'/>：<span class="<%= ratesColor[2] %>"><%= String.valueOf(rates[2] * 100) %>%</span>
	</h3>
	<br>
    <table class="table table-bordered table-condensed table-striped table-hover" id="brqslm-r">
        <thead>
            <tr>
                <th><small><bean:message key='session.number'/></small></th>
                <th><small><bean:message key='enter.time.record'/></small></th>
                <th><small><bean:message key='end.record.time'/></small></th>
                <th><small><bean:message key='ip.position'/></small></th>
                <th><small><bean:message key='last.message'/></small></th>
                <th><small><bean:message key='last.qaCategory'/></small></th>
                <th><small><bean:message key='statistics.msgCount.fromUser'/></small></th>
                <th><small><bean:message key='statistics.msgCount.hasAnswer'/></small></th>
                <th><small><bean:message key='statistics.msgCount.noAnswer'/></small></th>
                <th><small><bean:message key='global.action'/></small></th>
            </tr>
        </thead>
        <tbody>
        <%
        for (ServiceLogEntity entity: onlineSessions) {
        %>
            <tr>
                <td>#<%= entity.getIdentity() %></td>
                <td>
                	<%= entity.getTsCreated() %>
                	(<span class="timeago" title="<%= entity.getTsCreated() %>"><%= entity.getTsCreated() %></span>)
                </td>
                <td><span class="timeagooo" title="<%= entity.getTimestamp() %>"><%= entity.getTimestamp() %></span></td>
                <td><%= entity.getClientIp() %></td>
                <td><%= StringUtils.defaultString(entity.getLastMessage(), "N/A") %></td>
                <td><%= StringUtils.defaultString(entity.getLastQaCategory(), "N/A") %></td>
                <td><%= entity.getStatMsgCountFromUser() %></td>
                <td><%= entity.getStatMsgCountHasAnswer() %></td>
                <td><%= entity.getStatMsgCountNoAnswer() %>
                	<%= (entity.getStatForward() & 1) > 0 ? "&nbsp;<span class='text-warning'>(轉)</span>" : "" %>
                	<%= (entity.getStatForward() & 2) > 0 ? "&nbsp;<span class='text-danger'>(敏)</span>" : "" %>
                <td>
					<button type="button" data-id="<%= entity.getId() %>" class="btn-view-detail btn btn-success" id="brqslm-i" <%= (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0 ? "disabled" : ""%>><bean:message key='global.monitoring'/></button>
                    <br>
                </td>
            </tr>
        <%
        }
        %>
        </tbody>
    </table>
</div>
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
<script>
$(".timeago").timeago();

$(".btn-view-detail").click(function() {
	var id = $(this).attr("data-id");
	window.location = 'qaServiceLogDetail.jsp?nab=true&id=' + id;
});
</script>
</BODY>
</HTML>