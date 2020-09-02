<%@ include file="/commons/taglib.jsp"%>
<%@ page import="java.net.URLEncoder"
		 import="java.util.*"
		 import="java.util.regex.*"
	import="org.apache.struts.Globals"
	import="org.apache.struts.util.MessageResources"
	import="org.apache.struts.util.MessageResourcesFactory"
	 import="org.apache.solr.core.*"
	 import="org.apache.solr.servlet.*"
	 import="org.apache.solr.client.solrj.*"
	 import="org.apache.solr.client.solrj.embedded.*"
	 import="org.apache.solr.client.solrj.response.*"
	 import="org.apache.solr.common.*"
	 import="org.apache.solr.common.util.*"
     import="org.apache.commons.lang.*"
     import="org.apache.commons.httpclient.methods.GetMethod"
     import="org.apache.commons.httpclient.HttpClient"
     import="org.apache.commons.httpclient.auth.AuthScope"
     import="org.apache.commons.httpclient.UsernamePasswordCredentials"
     import="org.dom4j.Document" 
     import="org.dom4j.DocumentHelper" 
     import="org.dom4j.Element" 
     import="com.intumit.message.*"
     import="com.intumit.solr.*" 
     import="com.intumit.solr.util.*" 
     import="com.intumit.solr.dataset.*"
     import="com.intumit.solr.SearchManager"
     import="com.intumit.solr.searchKeywords.*"
     import="com.intumit.solr.config.ColumnNameMappingFacade"
     import="com.intumit.solr.config.ColumnNameMapping"
     import="com.intumit.solr.user.*"
     import="it.exprivia.cnos.opencloud.*" 
	 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
%><%!
MessageResources mr = MessageResourcesFactory.createFactory().createResources("messages");
public static String forwardJSP = "s.jsp";

static final Pattern breadcrumbPattern1 = Pattern.compile("\\(.*\\)");
static final Pattern breadcrumbPattern2 = Pattern.compile("\\[.*\\]");

java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
java.text.SimpleDateFormat sdfToday = new java.text.SimpleDateFormat("HH:mm");
java.text.DecimalFormat df = new java.text.DecimalFormat("$###,###");

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}
%><% 
response.setHeader("Cache-Control","no-cache"); 
response.setHeader("Pragma","no-cache"); 
response.setDateHeader ("Expires", -1);
%>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/heatmap.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-more.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/exporting.js"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%
Locale sessionLocale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
User user = User.getFromSession(session);
boolean usingApi = false;
long numFound = 0;
String apikey = request.getParameter("apikey");
String searchType = StringUtils.defaultString(request.getParameter("searchType"), "all");
String originQ = StringUtils.defaultString(request.getParameter("originQ"), "");

if (user == null && apikey != null) {
	user = User.getByApiKey(apikey);
	if (user != null) usingApi = true;
}

query.setQuery(originQ);

String basicMakeUrlParams = (usingApi ? "apikey=" + apikey + "&" : "") + "v=" + selectedView + "&originQ=" + URLEncoder.encode(originQ, "utf-8") + "&searchType=" + searchType;

if ("x".equals(selectedView)) {
	request.getRequestDispatcher("downloadExcel.jsp").forward(request, response);
}
else {
	response.setContentType("text/html");
	response.setCharacterEncoding("UTF-8");
}

String opDateFrom = WiSeUtils.getFromCookie("opDateFrom", request);
String opDateTo = WiSeUtils.getFromCookie("opDateTo", request);

try {

	Calendar today = Calendar.getInstance();
	today.set(Calendar.HOUR_OF_DAY, 0);
	today.set(Calendar.MINUTE, 0);
	today.set(Calendar.SECOND, 0);

	String qtime = pageContext.findAttribute("QTime").toString();
	DataSet ds = DataSetFacade.getInstance().get(new Integer(dsId));
	Map<String,Map<String,List<String>>> highlights = (Map<String,Map<String,List<String>>>) pageContext.findAttribute("highlights");
	SolrDocumentList docs = (SolrDocumentList) pageContext.findAttribute("results");
	HashSet<String> blacks = com.intumit.solr.blackKeywords.BlackKeywordFacade.getInstance().loadBlackList();
    int start = query.getStart();
	String fqs = "";
	if (query.getFilterQueries() != null)
	for (String fq: query.getFilterQueries()) {
		try {
			fqs += "&fq=" + URLEncoder.encode(fq, "UTF-8");
		}
		catch (Exception ignored) {}
	}

        //For simplification, we are not using the JSTL
        SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
		SolrQuery sortByQuery = query.getCopy();
        %>
        <% if(docList !=null && docList.size() < 1){ %>
		    	<div id="noresult">
		    	<div class="alert alert-error">
					<bean:message key="search.noresult.message"/><br>
					<bean:message key="search.noresult.mailto"/>
         		</div>
         		</div>
        <% } else { %>
         		<!-- HitFT Area -->
		        <div class='row' id='content-wrapper'>
		        	<div class='col-md-2'>
	            		<bean:message key="search.dataField.axis.x"/>：<br>
					    <select id="xAxisDataField" name="xAxisDataField" class="optionPanel input-small">
						    	<option value="questionType"><bean:message key="analytics.fn.questionType"/></option>
						    	<option value="questionCategory"><bean:message key="analytics.fn.questionCategory"/></option>
						    	<option value="questionKeyword"><bean:message key="analytics.fn.questionKeyword"/></option>
						    	<option value="matchedQuestion"><bean:message key="analytics.fn.matchedQuestion"/></option>
						    	<option value="kp" selected><bean:message key="analytics.fn.kp"/></option>
						    	<option value="answerType"><bean:message key="analytics.fn.answerType"/></option>
						    	<option value="confidence"><bean:message key="analytics.fn.confidence"/></option>
						    	<option value="identity"><bean:message key="analytics.fn.identity"/></option>
						    	<option value="eservice"><bean:message key="analytics.fn.eservice"/></option>
							<option value="questionTag"><bean:message key="analytics.fn.questionTag"/></option>
							<option value="userType"><bean:message key="analytics.fn.userType"/></option>
					    </select>
					    <br>
					    <div id="xAxisDateDataFieldFilterBlock">
	            		<bean:message key="search.dataField.timeGap"/>
					    <select id="gap" name="gap" class="optionPanel input-small">
					    	<option value="1day"><bean:message key="search.dataField.timeGap.1day"/></option>
					    	<option value="3days" selected><bean:message key="search.dataField.timeGap.3days"/></option>
					    	<option value="1week"><bean:message key="search.dataField.timeGap.1week"/></option>
					    	<option value="2weeks"><bean:message key="search.dataField.timeGap.2weeks"/></option>
					    	<option value="1month"><bean:message key="search.dataField.timeGap.1month"/></option>
					    </select>
					    </div>
					    <div id="xAxisTagCategoryDataFieldFilterBlock" style="display: none;">
	            		<bean:message key="search.dataField.filterBlock"/><span data-toggle="tooltip" title='<bean:message key="search.dataField.filterBlock.tooltip"/>'><i class="icon icon-question-sign"></i></span><br>
					    <select id="xAxisTagCategoryDataFieldFilter" name="xAxisTagCategoryDataFieldFilter" class="optionPanel input-small">
					    	<option value="" selected><bean:message key="global.all"/></option>
					    </select>
					    </div>
					    <div id="xAxisDataNumFieldFilterBlock" style="display: none;">
			            <div>
			            		<bean:message key="search.dataField.selectAmount"/>：<br>
							    <select id="xAxisDataNum" name="xAxisDataNum" class="optionPanel input-small">
							    	<option value="10" selected><bean:message key="global.number" arg0="10"/></option>
							    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
							    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
							    	<option value="50"><bean:message key="global.number" arg0="50"/></option>
							    	<option value="1000"><bean:message key="global.number.all"/></option>
							    </select>
						</div>
					    </div>
					</div>
	            	<div class='col-md-2'>
	            		<bean:message key="search.dataField.axis.y"/>：<br>
					    <select id="dataField" name="dataField" class="optionPanel input-small">
						    	<option value="questionType" selected><bean:message key="analytics.fn.questionType"/></option>
						    	<option value="questionCategory"><bean:message key="analytics.fn.questionCategory"/></option>
						    	<option value="questionKeyword"><bean:message key="analytics.fn.questionKeyword"/></option>
						    	<option value="matchedQuestion"><bean:message key="analytics.fn.matchedQuestion"/></option>
						    	<option value="kp"><bean:message key="analytics.fn.kp"/></option>
						    	<option value="answerType"><bean:message key="analytics.fn.answerType"/></option>
						    	<option value="confidence"><bean:message key="analytics.fn.confidence"/></option>
						    	<option value="identity"><bean:message key="analytics.fn.identity"/></option>
						    	<option value="eservice"><bean:message key="analytics.fn.eservice"/></option>
							<option value="questionTag"><bean:message key="analytics.fn.questionTag"/></option>
							<option value="userType"><bean:message key="analytics.fn.userType"/></option>
					    </select>
					    <br>
					    <div id="dataFieldFilterBlock" style="display: none;">
	            		<bean:message key="search.dataField.filterBlock"/><span data-toggle="tooltip" title='<bean:message key="search.dataField.filterBlock.tooltip"/>'><i class="icon icon-question-sign"></i></span><br>
					    <select id="dataFieldFilter" name="dataFieldFilter" class="optionPanel input-small">
					    	<option value="" selected><bean:message key="global.all"/></option>
					    </select>
					    </div>
					    <div id="yAxisDataNumFieldFilterBlock" style="display: none;">
			            <div>
			            		<bean:message key="search.dataField.selectAmount"/>：<br>
							    <select id="yAxisDataNum" name="yAxisDataNum" class="optionPanel input-small">
							    	<option value="10" selected><bean:message key="global.number" arg0="10"/></option>
							    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
							    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
							    	<option value="50"><bean:message key="global.number" arg0="50"/></option>
							    	<option value="1000"><bean:message key="global.number.all"/></option>
							    </select>
						</div>
					    </div>
					</div>
		            
		            <div class='col-md-4'>
		            	<%--
		            		日期範圍：
						<input size="12" style="width:80px;" type="text" name="opDateFrom" /> ~
						<input size="12" style="width:80px;" type="text" name="opDateTo" />
					    <br>
					--%>
	            		<bean:message key="search.dataField.chartType"/>：<br>
					    <select id="chartType" name="chartType" class="optionPanel input-medium">
					    	<option value="stacked-bar" selected><bean:message key="search.dataField.chartType.stackedBar"/></option>
					    	<option value="grouped-bar"><bean:message key="search.dataField.chartType.groupedBar"/></option>
					    	<option value="heatmap"><bean:message key="search.dataField.chartType.heatmap"/></option>
					    </select>
					    <br>
	            		<bean:message key="search.dataField.showType"/>：<br>
					    <select id="showType" name="showType" class="optionPanel input-medium">
					    	<option value="percentage" selected><bean:message key="search.dataField.showType.percentage"/></option>
					    	<option value="amount"><bean:message key="search.dataField.showType.amount"/></option>
					    </select>
					</div>
					<div class='col-md-4'>
						<button id='opRefresh' class='btn btn-primary'><bean:message key="global.refresh"/></button>
					</div>
		        </div>
		        <div class="row">
		            <div class='col-md-12'>
					    <div id="main">
					    </div>
					    <p id="copyright" style="margin-left: 30px;"><font color="red"></font><span>All rights reserved.|</span>© 1999-2017 <a href="http://opinion.hithot.cc/">HitHot Inc.</a></p>
		            </div>
		        </div>
          	<% } %>
<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/qa-analytics/adv-op-stat-ajax.jsp?<%= query.toString() %>&d=<%= dsId %>&ts=<%= System.currentTimeMillis() %>";

Highcharts.theme = {
	    colors: ['#058DC7', '#50B432', '#ED561B', '#DDDF00', '#24CBE5', '#64E572',
	             '#FF9655', '#FFF263', '#6AF9C4']};
Highcharts.setOptions(Highcharts.theme);

var amount = '<span style="color:{series.color}">●</span><span>{series.name}</span>: <b>{point.y:,.0f}</b><br/>';
var proportion = '<span style="color:{series.color}">●</span><span>{series.name}</span>: <b>{point.percentage:.1f} %</b><br/>';
var amountPro = '<span style="color:{series.color}">●</span><span>{series.name}</span>: <b>{point.y:,.1f} %</b><br/>';

var column_chart = {
	    chart: {
	        type: 'column',
	        zoomType: 'x',
            panning: true,
            panKey: 'shift',
			height: Math.max($(window.top).height() - 300, 300),
			marginTop: 100,
			renderTo: 'main'
	    },
	    title: {
	    	text: '',
	    	style: {
				display: 'none'
			}
	    },
	    subtitle: {
	        text: '',
			style: {
				display: 'none'
			}
	    },
	    xAxis: {
			labels: {
                style: {
                    color: 'black'
                }
            }
        },
	    yAxis: {
	        min: 0,
	        title: {
	            text: '',
	            align: 'center',
				style: {
                    color: 'black'
                }
	        },
	        stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                }
            }
	    },
	    tooltip: {
	        valueSuffix: '',
	        crosshairs: true
	    },
	    plotOptions: {
            column: {
            	stacking: null,
                dataLabels: {
                    enabled: false
                }
            },
            line: {
				stacking: null,
				dataLabels: {
				    enabled: false
				}
			},
			spline: {
				stacking: null,
				dataLabels: {
				    enabled: false
				}
			},
			radar: {
				stacking: null,
				dataLabels: {
				    enabled: false
				}
			}
        },
	    legend: {
	        layout: 'vertical',
	        align: 'right',
	        verticalAlign: 'middle',
	        floating: false,
	        borderWidth: 0,
	        backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
	        shadow: false,
	        reversed: false,
	    },
	    credits: {
	        enabled: false
	    },
	    lang: {
	        printButtonTitle: "print",
	        downloadButtonTitle: "download"
	    },
	    navigation: {
	    	buttonOptions: {
	    		theme: {
	    			'stroke-width': 1,
	    			stroke: 'silver'
	    		}
	    	}
	    },
        exporting: {
            buttons: {
                contextButton: {
                    enabled: false
                },
                exportButton: {
                	_titleKey: "downloadButtonTitle",
                	symbol: 'url(<%=request.getContextPath()%>/img/download.png)',
                	height: 24,
                    width: 24,
                    symbolX: 12,
                    symbolY: 12,
                    menuItems: Highcharts.getOptions().exporting.buttons.contextButton.menuItems.slice(2)
                },
                printButton: {
                	_titleKey: "printButtonTitle",
                	symbol: 'url(<%=request.getContextPath()%>/img/print.png)',
                	height: 24,
                    width: 24,
                    symbolX: 12,
                    symbolY: 12,
                    onclick: function () {
                        this.print();
                    }
                }
            }
        },
        series: [{
            name: '<bean:message key="global.negaitive"/>',
			color: '#f74b25',
            cursor: 'pointer',
            point: {
                events: {
                    click: function () {

                    	 window.open('/wise/wiseadm/qa-analytics/s.jsp?v=s&q=*%3A*&sort=Date_dt+desc&start=0&rows=10&fq=ClassifiedOpinion_s%3ANEGATIVE'+
                        	'&fq='+coption.xAxis.fq[this.index]+'<%= fqs %>','_blank');
					   	return false;
                    }
                }
            }
        }, {
            name: '<bean:message key="global.positive"/>',
			color: '#85b804',
            cursor: 'pointer',
            point: {
                events: {
                    click: function () {

                    	 window.open('/wise/wiseadm/qa-analytics/s.jsp?v=s&q=*%3A*&sort=Date_dt+desc&start=0&rows=10&fq=ClassifiedOpinion_s%3APOSITIVE'+
                        	'&fq='+coption.xAxis.fq[this.index]+'<%= fqs %>','_blank');
					   	return false;
                    }
                }
            }
        }]
	};

var heatmap_chart = {
		chart: {
            type: 'heatmap',
            height: Math.max($(window.top).height() - 300, 300),
			marginTop: 100,
			renderTo: 'main',
            plotBorderWidth: 1
        },
        title: {
        	text: '',
	    	style: {
				display: 'none'
			}
        },
        subtitle: {
	        text: '',
			style: {
				display: 'none'
			}
	    },
        xAxis: {
            categories: []
        },
        yAxis: {
            categories: [],
            title: null
        },
        colorAxis: {
            stops: [
                [0, '#66FF66'],
                [0.5, '#FFFF66'],
                [0.9, '#FF3333']
            ],
            min: 0
        },
        legend: {
            align: 'center',
            layout: 'horizontal',
            margin: 0,
            verticalAlign: 'bottom',
        },
        tooltip: {
    	    formatter: function () {
                return 'X: ' + this.series.xAxis.categories[this.point.x] + '<br/>' +
                       'Y: ' + this.series.yAxis.categories[this.point.y] + '<br/>' +
                       'Z: ' + this.point.value + " (<bean:message key='search.doc.unit'/>)";
            }
        },
        plotOptions: {
        	
        },
        credits: {
	        enabled: false
	    },
	    lang: {
	        printButtonTitle: "print",
	        downloadButtonTitle: "download"
	    },
	    navigation: {
	    	buttonOptions: {
	    		theme: {
	    			'stroke-width': 1,
	    			stroke: 'silver'
	    		}
	    	}
	    },
        exporting: {
            buttons: {
                contextButton: {
                    enabled: false
                },
                exportButton: {
                	_titleKey: "downloadButtonTitle",
                	symbol: 'url(<%=request.getContextPath()%>/img/download.png)',
                	height: 24,
                    width: 24,
                    symbolX: 12,
                    symbolY: 12,
                    menuItems: Highcharts.getOptions().exporting.buttons.contextButton.menuItems.slice(2)
                },
                printButton: {
                	_titleKey: "printButtonTitle",
                	symbol: 'url(<%=request.getContextPath()%>/img/print.png)',
                	height: 24,
                    width: 24,
                    symbolX: 12,
                    symbolY: 12,
                    onclick: function () {
                        this.print();
                    }
                }
            }
        },
        series: [{
            name: null,
            borderWidth: 1,
            data: null,
            dataLabels: {
                enabled: true,
                color: '#000'
            }
        }]
};

var coption;
var chart;

function prepareOptions() {
	var chartType  = $('#chartType :selected').val();
	var showType   = $('#showType :selected').val();
	var gap        = $('#gap :selected').val();
	var opDateFrom = $('input[name="opDateFrom"]').val();
	var opDateTo   = $('input[name="opDateTo"]').val();
	var groupBy    = null;
	var dataField  = $('#dataField :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();
	var xAxisDataNum    = $('#xAxisDataNum :selected').val();
	var xAxisDataField  = $('#xAxisDataField :selected').val();
	var xAxisTagCategoryDataFieldFilter = $('#xAxisTagCategoryDataFieldFilter :selected').val();
	var yAxisDataNum = $('#yAxisDataNum :selected').val();
	// chart
	if (chartType == 'stacked-bar' || chartType == 'grouped-bar') {
		coption = jQuery.extend(true, {}, column_chart);
		coption.tooltip.shared = true;
		
		if (chartType == 'stacked-bar') {
			
			if (showType == 'percentage') {
				coption.tooltip.pointFormat = proportion;
				coption.plotOptions.column.stacking = 'percent';
			}
			else {
				coption.tooltip.pointFormat = amount;
				coption.plotOptions.column.stacking = 'normal';
			}
		}
		else if (chartType == 'grouped-bar') {
			
			if (showType == 'percentage') {
				coption.tooltip.pointFormat = amountPro;
			}
			else {
				coption.tooltip.pointFormat = amount;
			}
		}
		
	}
	else if (chartType == 'heatmap') {
		coption = jQuery.extend(true, {}, heatmap_chart);
	}
	// dataField
	if (dataField == 'sourceType') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.sourceType"/>'];
	}
	else if (dataField == 'source') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.source"/>'];
	}
	else if (dataField == 'board') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.board"/>'];
	}
	else if (dataField == 'keyword') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.keyword"/>'];
	}
	else if (dataField == 'opinion') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.opinion"/>'];
	}
	else if (dataField == 'category') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.keyword"/>'];
	}
	else if (dataField == 'tag') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.tag"/>'];
		groupBy = 'category';
	}
	
	var jsonData = {
			chartType: chartType,
			showType: showType,
			type: showType,
			gap: gap,
			df: dataField,
			dff: dataFieldFilter,
			xadf: xAxisDataField,
			xadff: xAxisTagCategoryDataFieldFilter,
			xadn: xAxisDataNum,
			yadn: yAxisDataNum,
			opDateFrom: opDateFrom,
			opDateTo: opDateTo,
		};

	return jsonData;
}

function drawChart() {
	$('#main').height( Math.max($(window.top).height() - 300, 300) );

	var jsonData = prepareOptions();
	var chartType = jsonData.chartType;
	var showType = jsonData.showType;

	$.ajax({
		url: baseDataUrl,
		data: jsonData,
		dataType: 'json',
		success: function(result) {
			if (result.warning) {
				alert(result.warning);
			}
			
			if (chartType == 'heatmap') {
				coption.xAxis.categories = result.xAxis_category;
				coption.yAxis.categories = result.yAxis_category;
				coption.xAxis_fq =  result.xAxis_fq;
				coption.yAxis_fq =  result.yAxis_fq;
				coption.series[0].data = result.series;

				coption.plotOptions.series = {
					events: {
						click: function (event) {
							window.open('<%= request.getContextPath() %>/wiseadm/qa-analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&d=<%= dsId %>&fq='+heatmap_chart.xAxis_fq[event.point.x]+'&fq='+heatmap_chart.yAxis_fq[event.point.y],'_blank');
						}
					}
				};
			}
			else if (chartType == 'stacked-bar' || chartType == 'grouped-bar') {
				coption.xAxis.categories = result.xAxis_category;
				coption.xAxis_fq =  result.xAxis_fq;
				coption.yAxis_fq =  result.yAxis_fq;
				coption.series = result.series;
	
				if (chartType == 'grouped-bar' && showType == 'percentage') {
					
					if (coption.series.length > 0) {
						for (var i=0; i < coption.series[0].data.length; i++) {
							var total = 0;
							for (var j=0; j < coption.series.length; j++) {
								total += coption.series[j].data[i];
							}
							for (var j=0; j < coption.series.length; j++) {
								coption.series[j].data[i] = total == 0 ? 0 : Math.round(coption.series[j].data[i] * 100 * Math.pow(10,2) / total) / Math.pow(10,2);
							}
						}
					}
				}
	
				for (var i=0; i < coption.series.length; i++) {
					var s = coption.series[i];
					s.point = {
		                events: {
		                    click: (function (yIndex) {
		                    	return function() {
		                    		window.open('<%= request.getContextPath() %>/wiseadm/qa-analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&d=<%= dsId %>&fq='+coption.xAxis_fq[this.index]+'&fq='+coption.yAxis_fq[yIndex],'_blank');
							   		return false;
		                    	};
		                    })(i)
		                }
		            };
				}
			}
			// drawChart
			chart = new Highcharts.Chart(coption);
		}
	});
}

$('#btnSaveSearch').click(function() {
	var jsonData = prepareOptions();
	var theName = prompt('<bean:message key="search.save.message"/>');
	
	if (theName != null) {
		$.ajax({
			url: "<%= request.getContextPath() %>/saved-search-ajax.jsp?<%= query.toString() %>&d=<%= dsId %>&ts=<%= System.currentTimeMillis() %>",
			data: {
				name: theName,
				v: '<%= selectedView %>',
				viewOptions: JSON.stringify(jsonData)
			},
			dataType: 'json',
			success: function(result) {
				alert('<bean:message key="global.save.already"/>');
			}
		});
	}
});
$('#btnRemoveSearch').click(function() {

	$.ajax({
		url: "<%= request.getContextPath() %>/saved-search-ajax.jsp?command=remove&id=<%= request.getAttribute("currentSavedSearchId") %>",
		dataType: 'json',
		success: function(result) {
			alert('<bean:message key="global.delete.already"/>');
		}
	});
});

$('#opRefresh').click(function() {
	drawChart();
});

$(document).ready(function() {
	if ($('#dataField').val() == 'tag') {
		$('#dataFieldFilterBlock').show();
	} else {
		$('#dataFieldFilterBlock').hide();
	}
	
	if ($('#dataField').val() == 'opinion') {
		$('#yAxisDataNumFieldFilterBlock').hide();
	} else {
		$('#yAxisDataNumFieldFilterBlock').show();
	}
	
	if ($('#xAxisDataField').val() == 'date') {
		$('#xAxisDateDataFieldFilterBlock').show();
		$('#xAxisDataNumFieldFilterBlock').hide();
	} else {
		$('#xAxisDateDataFieldFilterBlock').hide();
		$('#xAxisDataNumFieldFilterBlock').show();
	}
	
	if ($('#xAxisDataField').val() == 'tag') {
		$('#xAxisTagCategoryDataFieldFilterBlock').show();
	} else {
		$('#xAxisTagCategoryDataFieldFilterBlock').hide();
	}
	
	if ($('#chartType').val() == 'heatmap') {
		$("#showType option[value='percentage']").hide();
	} else {
		$("#showType option[value='percentage']").show();
	}
	
	drawChart();
});

$('#dataField').on('change', function() {
	var dataField = $('#dataField :selected').val();

	if (dataField == 'tag') {
		$('#dataFieldFilterBlock').show();
	}
	else {
		$('#dataFieldFilterBlock').hide();
	}
	if (dataField == 'opinion') {
		$('#yAxisDataNumFieldFilterBlock').hide();
	}
	else {
		$('#yAxisDataNumFieldFilterBlock').show();
	}
});

$('#xAxisDataField').on('change', function() {
	var dataField = $('#xAxisDataField :selected').val();

	if (dataField == 'date') {
		$('#xAxisDateDataFieldFilterBlock').show();
		$('#xAxisDataNumFieldFilterBlock').hide();
	}
	else {
		$('#xAxisDateDataFieldFilterBlock').hide();
		$('#xAxisDataNumFieldFilterBlock').show();
	}
	if (dataField == 'tag') {
		$('#xAxisTagCategoryDataFieldFilterBlock').show();
	}
	else {
		$('#xAxisTagCategoryDataFieldFilterBlock').hide();
	}
});

$('#chartType').on('change', function() {
	var dataField = $('#chartType :selected').val();

	if (dataField == 'heatmap') {
		$("#showType option[value='percentage']").hide();
		$("#showType").val("amount").change();
	}
	else {
		$("#showType option[value='percentage']").show();
	}
});
</script>
<%
}
catch (Exception e) {
%>
	<div id="noresult">
	<div class="alert alert-error">
		<bean:message key="search.noresult.message"/>
	</div>
	</div>
<%
}
%>


