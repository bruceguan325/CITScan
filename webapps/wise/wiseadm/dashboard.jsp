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
import="com.intumit.solr.tenant.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
return;
}
%>
<%!
String getHitRatesTextColorClass(float rate) {
	if (rate < 0.7f) {
		return "text-danger";
	}
	else if (rate < 0.9f) {
		return "text-warning";
	}
	return "text-success";
}

String getCircleColorCode(float rate) {
	if (rate < 0.7f) {
		return "#FF0000";
	}
	else if (rate < 0.9f) {
		return "#F6A332";
	}
	return "#00FF00";
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
		float rate = ServiceLogEntity.hitrateStatistic(timeRangePair[0], timeRangePair[1], null)[3];
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

List<ServiceLogEntity> onlineSessions = ServiceLogEntity.listLatestByStatus(null, "robot:cathay:chat", ForwardToCrmRule.Status.ENTERING.name(), _30minAgo, 1000);

Calendar cal = Calendar.getInstance();
Date _now = cal.getTime();
cal.add(Calendar.HOUR, -24);
cal.set(Calendar.MINUTE, 0);
cal.set(Calendar.SECOND, 0);
Date _24hoursAgo = cal.getTime();

cal.add(Calendar.DATE, -7);
cal.set(Calendar.HOUR, 0);
cal.set(Calendar.MINUTE, 0);
cal.set(Calendar.SECOND, 0);
Date _7daysAgo = cal.getTime();

Double avgSessionTime = ServiceLogEntity.averageSessionTime(null, _7daysAgo, _now);
Number totalSessions = ServiceLogEntity.countBy(null, null, null, null, null, null, _7daysAgo, _now);
Number forwardSessions = ServiceLogEntity.countBy(null, null, null, new Integer[] {1, 3}, null, null, _7daysAgo, _now);
Number blackKeywordSessions = ServiceLogEntity.countBy(null, null, null, new Integer[] {2, 3}, null, null, _7daysAgo, _now);
System.out.println("Average chat time all tenants for past 7days:" + avgSessionTime);

int avgSessionMin = 0;
int avgSessionSec = 0;

if (avgSessionTime != null) {
	avgSessionMin = (int)Math.floor(avgSessionTime / 60);
	avgSessionSec = (int)(avgSessionTime % 60);
}

Object[][] statisticsByHour = ServiceLogEntity.statisticsByHour(null, _24hoursAgo, _now);
Object[][] statisticsByChannel = ServiceLogEntity.statisticsByChannel(null, _7daysAgo, _now);
Object[][] statisticsByTenant = ServiceLogEntity.statisticsByTenantId(_7daysAgo, _now);

for (Object[] o: statisticsByTenant) {
	Tenant t = Tenant.get(((Number)o[0]).intValue());
	
	if (t != null) {
		o[0] = String.format("%s - %s (%d)", t.getNotes(), t.getName(), t.getId());
	}
	else {
		o[0] = "Unknown (" + o[0] + ")";
	}
}
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='oline.user'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<jsp:include page="header-qa.jsp"></jsp:include>
<script src='<%= request.getContextPath() %>/script/jquery.circliful.js' type='text/javascript'></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-more.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" type="text/css" media="all"/>
</HEAD>
<BODY>
<div class="container pull-left">
	<div class="row" style="background-color: white;">
		<div class="col-md-12 box">
		<br>
		<input id="toggleAutoRefresh" type="checkbox"  data-toggle="toggle"  data-on="Auto Refresh" data-off="Stop Refresh">
		</div>
		<div class="col-md-12 box">
	        <div class='box-header muted-background'>
	            <div class='title'>
	                <i class='icon-star'></i>
	                <bean:message key='global.dashboard'/>（<bean:message key='past.seven.days'/>）
	            </div>
	            <div class='actions'>
	                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
	                </a>
	                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
	                </a>
	            </div>
	        </div>
	        <div class='box-content' >
			    <div class="col-md-3 ">
			            <div id="hitrate1" style="width: 100%;">
			            </div>
			    </div>
			    <div class="col-md-3 statusRow1">
			    		<div class="box-content box-statistic text-right blue-background">
                          <h3 class="title text-primary"><%= totalSessions %></h3>
                          <small><bean:message key='dashboard.totalSessions'/></small>
                          <div class="text-primary icon-comments align-left"></div>
                    </div>
			    		<div class="box-content box-statistic text-right green-background">
                          <h3 class="title text-primary"><%= avgSessionMin %>:<%= avgSessionSec %></h3>
                          <small><bean:message key='dashboard.averageChattime'/></small>
                          <div class="text-primary icon-time align-left"></div>
                    </div>
			    		<div class="box-content box-statistic text-right orange-background">
                          <h3 class="title text-primary"><%= forwardSessions %></h3>
                          <small><bean:message key='dashboard.forwardCount'/></small>
                          <div class="text-primary icon-user align-left"></div>
                    </div>
			    		<div class="box-content box-statistic text-right red-background">
                          <h3 class="title text-primary"><%= blackKeywordSessions %></h3>
                          <small><bean:message key='dashboard.badKeywordCount'/></small>
                          <div class="text-primary icon-remove align-left"></div>
                    </div>
			    </div>
			    <div class="col-md-6">
			    		<div id="line-chart" style="width: 100%">
			    		</div>
					<%-- <h3 id="brqslm-h" class="">
						<bean:message key='hitrates.today'/>：<span class="<%= ratesColor[0] %>"><%= String.valueOf(rates[0] * 100) %>%</span><br>
						<bean:message key='hitrates.yesterday'/>：<span class="<%= ratesColor[1] %>"><%= String.valueOf(rates[1] * 100) %>%</span><br>
						<bean:message key='hitrates.thisweek'/>：<span class="<%= ratesColor[2] %>"><%= String.valueOf(rates[2] * 100) %>%</span>
					</h3> --%>
				</div>
			</div>
		</div>
	</div>
	<div class="row" style="background-color: white;">
		<div class="col-md-12 box">
	        <div class='box-header muted-background'>
	            <div class='title'>
	                <i class='icon-star'></i>
	                <bean:message key='global.dashboard'/>（<bean:message key='global.other'/>）
	            </div>
	            <div class='actions'>
	                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
	                </a>
	                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
	                </a>
	            </div>
	        </div>
	        <div class='box-content' >
			    <div class="col-md-3 ">
			            <div id="hitrate2" style="width: 100%;">
			            </div>
			    </div>
			    <div class="col-md-3 ">
			            <div id="hitrate3" style="width: 100%;">
			            </div>
			    </div>
			    <div class="col-md-3 ">
			            <div id="pie-chart1" style="width: 100%;">
			            </div>
			    </div>
			    <div class="col-md-3 ">
			            <div id="pie-chart2" style="width: 100%;">
			            </div>
			    </div>
			</div>
		</div>
	</div>
	<br>
	<font color=FFFFFF>更新日期:2020-09-02</font><br>
	<h3><bean:message key='present.online.user'/>：<%= onlineSessions.size() %><bean:message key='global.persion'/></h3><br>
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
					<button type="button" data-id="<%= entity.getId() %>" class="btn-view-detail btn btn-success" id="brqslm-i" <%= (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0 ? "disabled" : ""%>><bean:message key='global.monitoring'/></button>
                    <br>
                </td>
            </tr>
        <%
        }
        %>
        </tbody>
    </table>
</div>
<script>
var autoRefreshHandle = null;
var autoRefreshFunction = function() {
	if ($('#toggleAutoRefresh').prop("checked")) {
    	window.location.hash = 'auto';
	    location.reload();
	}
	else {
		window.location.hash = '';
	}
};

$(".timeago").timeago();

$(".btn-view-detail").click(function() {
	var id = $(this).attr("data-id");
	window.location = 'qaServiceLogDetail.jsp?nab=true&id=' + id;
});

$( document ).ready(function() {
	if (window.location.hash == '#auto') {
		$('#toggleAutoRefresh').bootstrapToggle('on')
	}
	setInterval(autoRefreshFunction, 20000);
	
	$("#hitrate1").circliful({
        animationStep: 10,
        text: "<bean:message key='hitrates.today'/>",
        textBelow: true,
        foregroundBorderWidth: 15,
        backgroundBorderWidth: 15,
        percentageTextSize: 37,
        fontColor: "#000",
        foregroundColor: '<%= getCircleColorCode(rates[0]) %>',
        backgroundColor: "#eee",
        fillColor: "#eee",
        percent: <%= (int)(rates[0] * 100f) %>
   	});
	$("#hitrate2").circliful({
        animationStep: 10,
        text: "<bean:message key='hitrates.yesterday'/>",
        textBelow: true,
        foregroundBorderWidth: 15,
        backgroundBorderWidth: 15,
        percentageTextSize: 37,
        fontColor: "#000",
        foregroundColor: '<%= getCircleColorCode(rates[1]) %>',
        backgroundColor: "#eee",
        fillColor: "#eee",
        percent: <%= (int)(rates[1] * 100f) %>
    });
	$("#hitrate3").circliful({
        animationStep: 10,
        text: "<bean:message key='hitrates.thisweek'/>",
        textBelow: true,
        foregroundBorderWidth: 15,
        backgroundBorderWidth: 15,
        percentageTextSize: 37,
        fontColor: "#000",
        foregroundColor: '<%= getCircleColorCode(rates[2]) %>',
        backgroundColor: "#eee",
        fillColor: "#eee",
        percent: <%= (int)(rates[2] * 100f) %>
    });
	
	var rawDataByHour = <%= new flexjson.JSONSerializer().serialize(statisticsByHour) %>;
	var rawDataByChannel = <%= new flexjson.JSONSerializer().serialize(statisticsByChannel) %>;
	var rawDataByTenant = <%= new flexjson.JSONSerializer().serialize(statisticsByTenant) %>;
	var _24hours = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

	seriesForPie1 = $.map(rawDataByChannel, function(piece) {
		return {name: piece[0] ? piece[0] : "<bean:message key='global.other'/>", y: piece[1]};	
	});
	seriesForPie2 = $.map(rawDataByTenant, function(piece) {
		return {name: piece[0] ? piece[0] : "<bean:message key='global.other'/>", y: piece[1]};	
	});
	
	for (var idx in rawDataByHour) {
		d = rawDataByHour[idx];	
		_24hours[d[0]] = d[1];
	}
	var now = new Date();
	var hrOfDay = now.getHours();
	
	var categories = [];
	var counterOrderByCategories = [];
	
	/* 從 24hour 前排到目前這個小時 */
	for (var i = hrOfDay + 1;; i++) {
		if (i > 23) i -= 24;
		categories.push("" + i);	
		counterOrderByCategories.push(_24hours[i]);
		
		if (i == hrOfDay)
			break;
	}
	
	$('#line-chart').height($(".statusRow1").height() + 5);
	$('#line-chart').closest('div.box-content').height($('.statusRow1').height() + 5);
	$('#hitrate2').closest('div.box-content').height($('#hitrate2').height() + 15);
	
	hitRate2Top = $('#hitrate2').offset().top;
	pieChart1Top = $('#pie-chart1').offset().top;
	
	$('#pie-chart1').height($("#hitrate2").height() + 5);
	if (hitRate2Top != pieChart1Top) { // 窄螢幕（or 手機）
		$('#hitrate1').closest('div.box-content').height(($('#hitrate1').height() + 5) * 4);
		$('#hitrate2').closest('div.box-content').height(($('#hitrate2').height() + 5) * 4);
	}
	
	$('#pie-chart2').height($("#hitrate2").height() + 5);
	
	// 24hour 直條圖
	chart = new Highcharts.Chart({
	    chart: {
	        type: 'column',
	        renderTo: 'line-chart'
	    },
	    credits: { enabled: false },
	    title: { text: "<bean:message key='past.twenty.four.hours.sessions'/>" },
	    xAxis: {
	        categories: categories,
	        crosshair: true
	    },
	    yAxis: {
	        min: 0,
	        title: {
	            text: "<bean:message key='dashboard.sessions'/>"
	        }
	    },
	    tooltip: {
	        headerFormat: '<span style="font-size:10px">Hour: {point.key}</span><table>',
	        pointFormat: '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
	            '<td style="padding:0"><b>{point.y:.1f} sessions</b></td></tr>',
	        footerFormat: '</table>',
	        shared: true,
	        useHTML: true
	    },
	    plotOptions: {
	        column: {
	            pointPadding: 0.2,
	            borderWidth: 0
	        }
	    },
	    series: [{
	        name: "<bean:message key='past.twenty.four.hours'/>",
	        showInLegend: false,
	        data: counterOrderByCategories

	    }]
	});
	
	
	pieChart1 = new Highcharts.Chart({
	    chart: {
	        plotBackgroundColor: null,
	        plotBorderWidth: null,
	        plotShadow: true,
	        type: 'pie',
	        renderTo: 'pie-chart1'
	    },
	    title: {
	        text: "<bean:message key='dashboard.channelDistribution'/> (7days)",
	        verticalAlign: "bottom"
	    },
	    credits: { enabled: false },
	    tooltip: {
	        pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
	    },
	    plotOptions: {
	        pie: {
	            allowPointSelect: false,
	            cursor: 'pointer',
	            dataLabels: {
	                enabled: false,
	                format: '<b>{point.name}</b>: {point.percentage:.1f} %',
	                style: {
	                    color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
	                },
	                connectorColor: 'silver'
	            }
	        }
	    },
	    series: [{
	        name: 'Channels',
	        data: seriesForPie1
	    }]
	});
	
	pieChart2 = new Highcharts.Chart({
	    chart: {
	        plotBackgroundColor: null,
	        plotBorderWidth: null,
	        plotShadow: true,
	        type: 'pie',
	        renderTo: 'pie-chart2'
	    },
	    title: {
	        text: "<bean:message key='analytics.fn.tenant'/> (7days)",
	        verticalAlign: "bottom"
	    },
	    credits: { enabled: false },
	    tooltip: {
	        pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
	    },
	    plotOptions: {
	        pie: {
	            allowPointSelect: false,
	            cursor: 'pointer',
	            dataLabels: {
	                enabled: false,
	                format: '<b>{point.name}</b>: {point.percentage:.1f} %',
	                style: {
	                    color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
	                },
	                connectorColor: 'silver'
	            }
	        }
	    },
	    series: [{
	        name: "<bean:message key='analytics.fn.tenant'/>",
	        data: seriesForPie2
	    }]
	});
});

</script>
</BODY>
</HTML>