<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.user.*"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="java.util.*"
	import="com.intumit.solr.admin.*"
%><%
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) { 
	return;
}

//HitHotLocale locale = HitHotLocale.zh_CN;
List<String> pinned = new ArrayList<String>();


%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    
    <jsp:include page="header-qa.jsp" />
	<script src='<%= request.getContextPath() %>/script/d3.v3.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/script/nv.d3.min.js' type='text/javascript'></script>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/styles/nv.d3.min.css" type="text/css" media="all"/>
    
    <script src='<%= request.getContextPath() %>/assets/javascripts/plugins/flot/excanvas.js' type='text/javascript'></script>
    <script src='<%= request.getContextPath() %>/script/jquery.circliful.min.js' type='text/javascript'></script>
    
	<!--[if (gte IE 9) | (!IE)]><!-->
	<script src='<%= request.getContextPath() %>/script/d3.v3.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/script/nv.d3.min.js' type='text/javascript'></script>
	<!--<![endif]-->
	
	<!-- / flot charts -->
	<script src='<%= request.getContextPath() %>/assets/javascripts/plugins/flot/flot.min.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/assets/javascripts/plugins/flot/flot.resize.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/assets/javascripts/plugins/flot/flot.pie.js' type='text/javascript'></script>
	<script src='<%= request.getContextPath() %>/assets/javascripts/plugins/flot/flot.categories.js' type='text/javascript'></script>
	
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" type="text/css" media="all"/>
    <style>
    label {display: inline;}
    #stats-chart1,#stats-chart2,#stats-chart3,#stats-chart4,#stats-chart5,#stats-chart6,#stats-chart7,#stats-chart8,#stats-chart9,#stats-chart10,.stats-chart{height:200px}.color-settings .color-title{font-weight:bold;padding-left:10px}.box-nomargin{margin-bottom:20px}@media screen and (min-width:768px){.box-nomargin{margin-bottom:0}}.wysihtml5-sandbox{box-sizing:border-box}
    </style>
</head>
<body class='contrast-fb'>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div id='wrapper'>
<section>

<div class='row-fluid'>
    <div class='col-md-6 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-star'></i>
                <bean:message key='past.twenty.four.hours.performance'/>
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
        <div class='box-content'>
            <div id='nv-stats-chart1' style='width: 100%;'>
            	<svg class='stats-chart'></svg>
            </div>
        </div>
    </div>
    <div class='col-md-6 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-signal'></i>
                <bean:message key='past.three.days.performance'/>
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
        <div class='box-content'>
            <div id='nv-stats-chart2' style='width: 100%;'>
            	<svg class='stats-chart'></svg>
            </div>
        </div>
    </div>
</div>

<hr class='hr-drouble'>
<div class='row-fluid'>
    <div class='col-md-6 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-star'></i>
               <bean:message key='past.ten.days.performance'/> 
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
            <div id='nv-stats-chart3' style='width: 100%; height: 400px;'>
            	<svg class='stats-chart' style='height: 400px;'></svg>
            </div>
    </div>
    <div class='col-md-6 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-signal'></i>
               <bean:message key='past.three.months.performance'/> 
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
            <div id='nv-stats-chart4' style='width: 100%; height: 400px;'>
            	<svg class='stats-chart' style='height: 400px;'></svg>
            </div>
    </div>
</div>

<hr class='hr-drouble'>

<div class='row-fluid'>
    <div class='col-md-4 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-star'></i>
                <bean:message key='service.status.statistics.past.twenty.four.hours'/>
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
        <div class='box-content'>
            <div id='stats-chart5' class='stats-chart'></div>
        </div>
    </div>
    <div class='col-md-4 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-star'></i>
                <bean:message key='service.status.statistics.this.week'/>
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
        <div class='box-content'>
            <div id='stats-chart6' class='stats-chart'></div>
        </div>
    </div>
    <div class='col-md-4 box'>
        <div class='box-header'>
            <div class='title'>
                <i class='icon-signal'></i>
                <bean:message key='service.status.statistics.this.month'/> 
            </div>
            <div class='actions'>
                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
                </a>
                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
                </a>
            </div>
        </div>
        <div class='box-content'>
            <div id='stats-chart7' class='stats-chart'></div>
        </div>
    </div>
</div>

	<div class='row-fluid'>
	    <div class='col-md-4 box'>
	        <div class='box-header'>
	            <div class='title'>
	                <i class='icon-signal'></i>
	                <bean:message key='average.processing.time.past.twent.four.hours'/> 
	            </div>
	            <div class='actions'>
	                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
	                </a>
	                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
	                </a>
	            </div>
	        </div>
	        <div class='box-content'>
	            <div id='stats-chart8'></div>
	        </div>
	    </div>
	    <div class='col-md-4 box'>
	        <div class='box-header'>
	            <div class='title'>
	                <i class='icon-signal'></i>
	               <bean:message key='average.processing.time.this.week'/>
	            </div>
	            <div class='actions'>
	                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
	                </a>
	                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
	                </a>
	            </div>
	        </div>
	        <div class='box-content'>
	            <div id='stats-chart9'></div>
	        </div>
	    </div>
	    <div class='col-md-4 box'>
	        <div class='box-header'>
	            <div class='title'>
	                <i class='icon-signal'></i>
	               <bean:message key='average.processing.time.this.month'/>
	            </div>
	            <div class='actions'>
	                <a href="#" class="btn box-remove btn-mini btn-link"><i class='icon-remove'></i>
	                </a>
	                <a href="#" class="btn box-collapse btn-mini btn-link"><i></i>
	                </a>
	            </div>
	        </div>
	        <div class='box-content'>
	            <div id='stats-chart10'></div>
	        </div>
	    </div>
	</div>

<hr class='hr-drouble'>

<hr class='hr-drouble'>
    <div class='container-fluid'>
        <div class='row-fluid' id='content-wrapper'>
            <div class='col-md-12'>

            </div>
        </div>
    </div>
</section>
</div>

<jsp:include page="footer-qa.jsp" />

<%
String pinnedToQuery = StringUtils.join(pinned, " OR ");

if (pinned.size() == 0) {
	pinnedToQuery = "ANIMPOSSIBLEEXISTKEYWORD";
}
%>
<script>
$('#reportAsUser').on('change', function() {
	window.location = 'report.jsp?uid=' + this.value;
});

function toXYStyle(cell) {
	return {x: parseInt(cell[0]), y: cell[1]};
}

function labelFormatter(label, series) {
	return "<div style='font-size:8pt; text-align:center; padding:2px; color:white;'>" + label + "<br/>" + Math.round(series.percent) + "%</div>";
}

function timeago(totalhours) {
	var settings = {
		strings: {
            prefixAgo: null,
            prefixFromNow: null,
            suffixAgo: "<bean:message key='global.front'/>",
            suffixFromNow: "<bean:message key='global.ago'/>",
		    hours: "%d<bean:message key='global.hour'/>",
		    day: "1<bean:message key='global.sky'/>",
		    days: "%d<bean:message key='global.sky'/>",
		    month: "1<bean:message key='global.one.day'/>",
		    months: "%d<bean:message key='global.one.day'/>",
		    year: "1<bean:message key='global.year'/>",
		    years: "%d<bean:message key='global.year'/>",
		    wordSeparator: "",
		    numbers: [],
		},
	    substitute: function(stringOrFunction, number) {
	        var string = $.isFunction(stringOrFunction) ? stringOrFunction(number, distanceMillis) : stringOrFunction;
	        var value = ($l.numbers && $l.numbers[number]) || number;
	        return string.replace(/%d/i, value);
	    }
	};

	var totaldays = Math.floor(totalhours / 24);
	
    var hours = totalhours % 24;
    var days = totaldays % 365;
    var years = totaldays / 365;
    
    var $l = settings.strings;
    var prefix = $l.prefixAgo;
    var suffix = $l.suffixAgo;

    var words = years >= 1 && settings.substitute($l.years, Math.floor(years)) || "";
    words += days > 0 && settings.substitute($l.days, days) || "";
    words += hours > 0 && settings.substitute($l.hours, hours) || "";

    var separator = $l.wordSeparator || "";
    if ($l.wordSeparator === undefined) { separator = " "; }
    return $.trim([prefix, words, suffix].join(separator));
}

if ($("#nv-stats-chart1").length !== 0) {
	var thisSg = 24;
	$.when(
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "ENTERING",
					   sg: thisSg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "ACTIVE",
					   sg: thisSg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "IDLE",
					   sg: thisSg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "CLOSED",
					   sg: thisSg
					   }
					})
		).then(
			function(set1, set2, set3, set4) {
				
				var nvdatasets = [
						{
                            values: set1[0].map(toXYStyle),
                            key: "ENTERING"
                        }, 
                        {
                            values: set2[0].map(toXYStyle),
                            key: "ACTIVE"
                        }, 
                        {
                            values: set3[0].map(toXYStyle),
                            key: "IDLE"
                        }, 
                        {
                            values: set4[0].map(toXYStyle),
                            key: "CLOSED"
                        }
				];

				nv.addGraph(function() {
				    var chart = nv.models.multiBarChart()
			    	  .margin({top:0, right:10, bottom:20, left:50});
				    chart.xAxis
				      .showMaxMin(false)
				      .tickFormat(function(d) {
				    	  if (d == thisSg || d == 0) {
				    		  return '<bean:message key="global.now"/>';
				    	  }
				    	  else {
				        	return (-1*d) + '<bean:message key="global.hour"/><bean:message key="global.front"/>';
				    	  }
				      });
				    chart.yAxis.tickFormat(d3.format(',.d'));

				    d3.select('#nv-stats-chart1 svg')
				    	.datum(nvdatasets)
				    	.transition().duration(100).call(chart);

				    nv.utils.windowResize(chart.update);

				    return chart;
				});
			}
		);
    
}

if ($("#nv-stats-chart2").length !== 0) {
	var chart2Sg = 3;
	$.when(
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "DAY",
					   sType: "ENTERING",
					   
					   sg: chart2Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "DAY",
					   sType: "ACTIVE",
					   
					   sg: chart2Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "DAY",
					   sType: "IDLE",
					   
					   sg: chart2Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "DAY",
					   sType: "CLOSED",
					   
					   sg: chart2Sg
					   }
					})
		).then(
			function(set1, set2, set3, set4) {
				
				var nvdatasets = [
									{
			                            values: set1[0].map(toXYStyle),
			                            key: "ENTERING"
			                        }, 
			                        {
			                            values: set2[0].map(toXYStyle),
			                            key: "ACTIVE"
			                        }, 
			                        {
			                            values: set3[0].map(toXYStyle),
			                            key: "IDLE"
			                        }, 
			                        {
			                            values: set4[0].map(toXYStyle),
			                            key: "CLOSED"
			                        }
				];

				nv.addGraph(function() {
				    var chart = nv.models.multiBarChart()
			    	  .margin({top:0, right:10, bottom:20, left:50});
				    chart.xAxis
				      .showMaxMin(false)
				      .tickFormat(function(d) {
				    	  if (d == chart2Sg || d == 0 || d == NaN) {
				    		  return '<bean:message key="global.nowadays"/>';
				    	  }
				    	  else {
				        	return (-1*d) + '<bean:message key="global.before.yesterday"/>';
				    	  }
				      });
				    chart.yAxis.tickFormat(d3.format(',.1d'));

				    d3.select('#nv-stats-chart2 svg')
				    	.datum(nvdatasets)
				    	.transition().duration(100).call(chart);

				    nv.utils.windowResize(chart.update);

				    return chart;
				});
			}
		);
    
}

if ($("#nv-stats-chart3").length !== 0) {
	var chart3Sg = 240;
	$.when(
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "ENTERING",
					   
					   sg: chart3Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "ACTIVE",
					   
					   sg: chart3Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "HOUR",
					   sType: "IDLE",
					   
					   sg: chart3Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%=locale.toString() %>',
					   g: "HOUR",
					   sType: "CLOSED",
					   
					   sg: chart3Sg
					   }
					})
		).then(
			function(set1, set2, set3, set4) {
				
				var nvdatasets = [
								{
		                            values: set1[0].map(toXYStyle),
		                            key: "ENTERING"
		                        }, 
		                        {
		                            values: set2[0].map(toXYStyle),
		                            key: "ACTIVE"
		                        }, 
		                        {
		                            values: set3[0].map(toXYStyle),
		                            key: "IDLE"
		                        }, 
		                        {
		                            values: set4[0].map(toXYStyle),
		                            key: "CLOSED"
		                        }
				];

				nv.addGraph(function() {
				    var chart = nv.models.lineWithFocusChart()
			    	  .margin({top:0, right:10, bottom:20, left:50});
				    chart.xAxis
				      .showMaxMin(false)
				      .tickFormat(function(d) {
				    	  if (d == chart3Sg || d == 0) {
				    		  return '<bean:message key="global.now"/>';
				    	  }
				    	  else {
				        	return timeago(-1*d);
				    	  }
				      });
				    chart.x2Axis
				      .tickFormat(function(d) {
				    	  if (d == chart3Sg || d == 0) {
				    		  return '<bean:message key="global.now"/>';
				    	  }
				    	  else {
				        	return timeago(-1*d);
				    	  }
				      });
				    chart.yAxis.tickFormat(d3.format(',.d'));

				    d3.select('#nv-stats-chart3 svg')
				    	.datum(nvdatasets)
				    	.transition().duration(100).call(chart);

				    nv.utils.windowResize(chart.update);

				    return chart;
				});
			}
		);
    
}

if ($("#nv-stats-chart4").length !== 0) {
	var chart4Sg = 3;
	$.when(
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "MONTH",
					   sType: "ENTERING",
					   
					   sg: chart4Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString()%>',
					   g: "MONTH",
					   sType: "ACTIVE",
					   
					   sg: chart4Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%= locale.toString() %>',
					   g: "MONTH",
					   sType: "IDLE",
					   
					   sg: chart4Sg
					   }
					}),
			$.ajax({
				url: "stat-ajax.jsp",
				type : 'GET',
				dataType : 'json',
				data: {
					   locale: '<%=locale.toString() %>',
					   g: "MONTH",
					   sType: "CLOSED",
					   
					   sg: chart4Sg
					   }
					})
		).then(
			function(set1, set2, set3, set4) {
				
				var nvdatasets = [
						{
                            values: set1[0].map(toXYStyle),
                            key: "ENTERING"
                        }, 
                        {
                            values: set2[0].map(toXYStyle),
                            key: "ACTIVE"
                        }, 
                        {
                            values: set3[0].map(toXYStyle),
                            key: "IDLE"
                        }, 
                        {
                            values: set4[0].map(toXYStyle),
                            key: "CLOSED"
                        }
				];

				nv.addGraph(function() {
				    var chart = nv.models.stackedAreaChart()
			    	  .margin({top:0, right:10, bottom:20, left:50});
				    chart.xAxis
				      .showMaxMin(false)
				      .tickFormat(function(d) {
				    	  if (d == chart4Sg || d == 0) {
				    		  return '<bean:message key="global.this.month"/>';
				    	  }
				    	  else {
				        	return (-1*d) + '<bean:message key="global.one.day"/><bean:message key="global.front"/>';
				    	  }
				      });
				    chart.yAxis.tickFormat(d3.format(',.d'));

				    d3.select('#nv-stats-chart4 svg')
				    	.datum(nvdatasets)
				    	.transition().duration(100).call(chart);

				    nv.utils.windowResize(chart.update);

				    return chart;
				});
			}
		);
}


if ($("#stats-chart5").length !== 0) {
	data = [{"data":1,"label":"ENTERING"},{"data":1,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart5"), data, {
	        series: {
	        pie: {
	            show: true,
	            radius: 1,
	            label: {
	                show: true,
	                radius: 3/4,
	                formatter: labelFormatter,
	                background: {
	                    opacity: 0
	                }
	            }
	        }
	    	}, legend: { show: false }
    });
}
if ($("#stats-chart6").length !== 0) {
	data = [{"data":1,"label":"ENTERING"},{"data":1,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart6"), data, {
	        series: {
	        pie: {
	            show: true,
	            radius: 1,
	            label: {
	                show: true,
	                radius: 3/4,
	                formatter: labelFormatter,
	                background: {
	                    opacity: 0
	                }
	            }
	        }
	    	}, legend: { show: false }
    });
}
if ($("#stats-chart7").length !== 0) {
	data = [{"data":1,"label":"ENTERING"},{"data":1,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart7"), data, {
	        series: {
	        pie: {
	            show: true,
	            radius: 1,
	            label: {
	                show: true,
	                radius: 3/4,
	                formatter: labelFormatter,
	                background: {
	                    opacity: 0
	                }
	            }
	        }
	    	}, legend: { show: false }
    });
}
if ($("#stats-chart8").length !== 0) {
	data = [{"data":1,"label":"ENTERING"},{"data":1,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart8"), data, {
        series: {
        pie: {
            show: true
        }
    }});
}
if ($("#stats-chart9").length !== 0) {
	data = [{"data":50,"label":"ENTERING"},{"data":80,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart9"), data, {
        series: {
        pie: {
            show: true
        }
    }});
}
if ($("#stats-chart10").length !== 0) {
	data = [{"data":1,"label":"ENTERING"},{"data":1,"label":"ACTIVE"},{"data":1,"label":"IDLE"},{"data":1,"label":"CLOSED"}];
	plot = $.plot($("#stats-chart10"), data, {
        series: {
        pie: {
            show: true
        }
    }});
}

</script>

</body>
</html>
