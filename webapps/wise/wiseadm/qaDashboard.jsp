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
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0 
     && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0
     && (AdminGroupFacade.getInstance().getFromSession(session).getStatisticsAdminCURD() & AdminGroup.R) == 0) {
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

Calendar cal = Calendar.getInstance();
Date _now = cal.getTime();
cal.add(Calendar.HOUR, -24);
cal.set(Calendar.MINUTE, 0);
cal.set(Calendar.SECOND, 0);
Date _24hrsAgo = cal.getTime();

Double avgSessionTime_past24hrs = ServiceLogEntity.averageSessionTime(t.getId(), _24hrsAgo, _now);
Number totalSessions = ServiceLogEntity.countBy(t.getId(), null, null, null, null, null, _24hrsAgo, _now);
Number forwardSessions = ServiceLogEntity.countBy(t.getId(), null, null, new Integer[] {1, 3}, null, null, _24hrsAgo, _now);
Number blackKeywordSessions = ServiceLogEntity.countBy(t.getId(), null, null, new Integer[] {2, 3}, null, null, _24hrsAgo, _now);
System.out.println("Average chat time of " + t.getName() + " for past 24hrs:" + avgSessionTime_past24hrs);

int avgSessionMin = 0;
int avgSessionSec = 0;

if (avgSessionTime_past24hrs != null) {
	avgSessionMin = (int)Math.floor(avgSessionTime_past24hrs / 60);
	avgSessionSec = (int)(avgSessionTime_past24hrs % 60);
}

Object[][] statisticsByHour = ServiceLogEntity.statisticsByHour(t.getId(), _24hrsAgo, _now);
Object[][] statisticsByChannel = ServiceLogEntity.statisticsByChannel(t.getId(), _24hrsAgo, _now);
Object[][] statisticsByLastQaCategory = ServiceLogEntity.statisticsByLastQaCategory(t.getId(), _24hrsAgo, _now);
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='oline.user'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta http-equiv="refresh" content="20">
<jsp:include page="header-qa.jsp"></jsp:include>
<script src='<%= request.getContextPath() %>/script/jquery.circliful.js' type='text/javascript'></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-more.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" type="text/css" media="all"/>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<div class="row" style="background-color: white;">
		<div class="col-md-12 box">
	        <div class='box-header muted-background'>
	            <div class='title'>
	                <i class='icon-star'></i>
	                <bean:message key='global.dashboard'/>（<bean:message key='past.twenty.four.hours'/>）
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
                <td><span class="timeago" title="<%= entity.getTimestamp() %>"><%= entity.getTimestamp() %></span></td>
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
$(".btn-view-detail").click(function() {
	var id = $(this).attr("data-id");
	window.location = 'qaServiceLogDetail.jsp?nab=true&id=' + id;
});

$( document ).ready(function() {
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
	var rawDataByLastQaCategory = <%= new flexjson.JSONSerializer().serialize(statisticsByLastQaCategory) %>;
	var _24hours = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

	seriesForPie1 = $.map(rawDataByChannel, function(piece) {
		return {name: piece[0] ? piece[0] : "<bean:message key='global.other'/>", y: piece[1]};	
	});
	seriesForPie2 = $.map(rawDataByLastQaCategory, function(piece) {
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
	$('#pie-chart1').height($("#hitrate2").height() + 5);
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
	        text: "<bean:message key='dashboard.channelDistribution'/> (24hrs)",
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
	        text: "<bean:message key='analytics.fn.questionCategory'/> (24hrs)",
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
	        name: "<bean:message key='analytics.fn.questionCategory'/>",
	        data: seriesForPie2
	    }]
	});
});
<%
java.util.Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, true, true);
%>
$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry() %>');
$(".timeago").timeago();
</script>
</BODY>
</HTML>