<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
		 import="java.net.URLEncoder"
		 import="java.net.URLDecoder"
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
         import="com.intumit.solr.*"
         import="com.intumit.solr.util.*"
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.searchKeywords.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
%><%!
MessageResources mr = MessageResourcesFactory.createFactory().createResources("messages");

public static String forwardJSP = "s.jsp";

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}
%>
<script type='text/javascript' src='<%= request.getContextPath() %>/script/jquery.fileDownload.js'></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-3d.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/exporting.js"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%@ include file="translate-map.jsp" %>
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
org.json.JSONObject defaultViewOptions = (org.json.JSONObject) pageContext.findAttribute("defaultViewOptions");
String searchType = StringUtils.defaultString(request.getParameter("searchType"), "all");
String originQ = URLDecoder.decode(StringUtils.defaultString(request.getParameter("originQ"), ""), "utf-8");
String selectedTarget = (String) pageContext.findAttribute("selectedTarget");
AnalyticsUtil au = (AnalyticsUtil) pageContext.findAttribute("analyticsUtil");

try {
	Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
	Calendar today = Calendar.getInstance();
	today.set(Calendar.HOUR_OF_DAY, 0);
	today.set(Calendar.MINUTE, 0);
	today.set(Calendar.SECOND, 0);

	String qtime = pageContext.findAttribute("QTime").toString();
	DataSet ds = (DataSet)session.getAttribute("logDs");
	Map<String,Map<String,List<String>>> highlights = (Map<String,Map<String,List<String>>>) pageContext.findAttribute("highlights");
	SolrDocumentList docs = (SolrDocumentList) pageContext.findAttribute("results");
	HashSet<String> blacks = com.intumit.solr.blackKeywords.BlackKeywordFacade.getInstance().loadBlackList();
    int start = query.getStart();

	String fqs = "";

    //For simplification, we are not using the JSTL
    SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
	SolrQuery sortByQuery = query.getCopy();

	String[] sortables = StringUtils.trimToEmpty(ds.getSortables()).split(",");

	StringBuilder paginationInfo = new StringBuilder();
	paginationInfo.append(mr.getMessage(locale, "search.number.found2", new Object[]{docs.getNumFound()}));
	StringBuilder pagination = new StringBuilder();

	int currPage = (int)Math.floor(start / query.getRows())+1;
	if (currPage > 1) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart(0);

		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP,  "v=" + selectedView + "&" + nextPageQuery.toString()));
		pagination.append("\"><i class=\"icon-backward\"></i>&nbsp;</A>");
	}
	for (int po=-4; po <= 4; po++) {
		int nStart = start + query.getRows() * po;
		if (nStart < 0 || nStart >= docs.getNumFound())
			continue;

		if (po == 0) {
			pagination.append("&nbsp;<A class=\"btn active\" HREF=\"#\">");
			pagination.append((currPage+po));
			pagination.append("</A>");
			continue;
		}
		SolrQuery toPageQuery = query.getCopy();
		toPageQuery.setStart(nStart);
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP, "v=" + selectedView + "&" + toPageQuery.toString()));
		pagination.append("\">");
		pagination.append((currPage+po));
		pagination.append("</A>");
	}
	int lastPage = (int)Math.floor((docs.getNumFound()-1) / query.getRows()) + 1;
	if (lastPage != currPage) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart((lastPage-1)*query.getRows());

		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP, "v=" + selectedView + "&" + nextPageQuery.toString()));
		pagination.append("\"><i class=\"icon-forward\"></i>&nbsp;</A>");
	}
    %>
    <div class='row' id='content-wrapper'>
        <div class='col-md-12'>
       		<bean:message key="analytics.type"/>
		    <select id="selectedView" name="selectedView" class="form-control optionPanel input-medium">
		    	<% for (String at: au.getAnalyticTypes()) { %>
				<option value="<%= at %>"><%= com.intumit.message.MessageUtil.getMessage(locale, (String)au.getAnalyticTypeConfig(at).get("resourceKey")) %></option>
		    	<% } %>
		    </select>
				    
			<span class="function-separator"></span>
				    
			<bean:message key="search.dataField.chartType"/>：
		    <select id="chartType" name="chartType" class="form-control optionPanel input-medium">
		    	<option value="pie"><bean:message key="search.view.bar.pie"/></option>
		    	<option value="donut"><bean:message key="search.view.bar.donut"/></option>
		    	<option value="semiDonut"><bean:message key="search.view.bar.semiDonut"/></option>
		    </select>
		    <select id="showType" name="showType" class="form-control optionPanel input-medium">
		    	<option value="2D" selected>2D</option>
		    	<option value="3D">3D</option>
		    </select>
				    
			<span class="function-separator"></span>
            
       		<bean:message key="search.dataField.column"/>：
		    <select id="dataField" name="dataField" class="form-control optionPanel input-medium">
		    	<% for (String param: (List<String>)au.getAnalyticTypeConfig(selectedView).get("field1")) { %>
		    	<option value="<%= param %>" ><%= translate(locale, au, null, au.getParamNameResourceKey(param), false) %></option>
		    	<% } %>
		    </select>
		    <select id="dataNum" name="dataNum" class="form-control optionPanel input-small">
		    	<option value="10" selected><bean:message key="global.number" arg0="10"/></option>
		    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
		    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
		    	<option value="50"><bean:message key="global.number" arg0="50"/></option>
		    	<option value="1000"><bean:message key="global.number.all"/></option>
		    </select>
				    
			<span class="function-separator"></span>
				    
			<button id='opRefresh' class='btn btn-primary hide'><bean:message key="global.refresh"/></button>
			<button id='btnExportCSV' class='btn btn-primary'><bean:message key="search.export"/></button>
					
		    <script>
		    $('#selectedView').val("<%= selectedView %>");
		    $('#dataField').val("<%= au.getAnalyticTypeConfig(selectedView).get("field1Selected") %>");
		    
		    $('#selectedView').on("change", function() { 
		    	turl = '<%= makeUrl(request, forwardJSP, "st=" + selectedTarget + "&" + query.toString()) %>&v=' + $('#selectedView').val();
		    	window.location = turl;
		    });
		    </script>
		</div>
    </div>
	<% if(docList !=null && docList.size() < 1){ %>
    	<div id="noresult">
    	<div class="alert alert-error">
			<bean:message key="search.noresult.message"/><br>
			<bean:message key="search.noresult.mailto"/>
   		</div>
   		</div>
	<% } else { %>
        <div class='row'>
            <div class='col-md-12'>
			    <div id="main"></div>
			    <br>
				<p id="copyright" style="margin-left: 30px; display: none;"><font color="red"></font><span>All rights reserved.|© 1999-2017 </span><a href="http://www.intumit.com/">Intumit Inc.</a></p>
            </div>
        </div>
   <% } %>

<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/analytics/adv-pie-stat-ajax.jsp?<%= query.toString() %>&st=<%= selectedTarget %>&v=<%= selectedView %>&d=<%= dsId %>&ts=<%= System.currentTimeMillis() %>";

var coption_origin = {
	    chart: {
            marginTop: 140,
            renderTo: 'main',
            events: {
                load: function () { 
                    $(".highcharts-data-labels g").attr("opacity", 1);
                }
            }
	    },
	    title: {
	        text: '<bean:message key="search.view.pie"/>'
	    },
	    subtitle: {
	        text: 'Source: SmartRobot'
	    },
	    xAxis: {
	        title: {
	            text: '',
	        }
	    },
	    yAxis: {
	        min: 0,
	        title: {
	            text: '<bean:message key="search.view.bar.inInternet"/>',
	        }
	    },
	    tooltip: {
	        valueSuffix: '<bean:message key="search.view.bar.1"/>'
	    },
	    plotOptions: {
	        bar: {
	        	groupPadding: 0,
	            dataLabels: {
	                enabled: true,
					allowOverlap: true,
	            }
	        },
	        column: {
	        	groupPadding: 0,
	            dataLabels: {
	                enabled: true,
					allowOverlap: true
	            }
            },
	        pie: {
	        	shadow: false,
                showInLegend: true,
                dataLabels: {
                    enabled: true,
                    format: '<b>{point.name}</b>: {point.percentage:.1f} %'
                }
            }
	    },
	    legend: {
	        layout: 'horizontal',
	        align: 'center',
	        verticalAlign: 'top',
	        y: 50,
	        floating: false,
	        borderWidth: 1,
	        backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
	        shadow: true,
	        reversed: true,
			maxHeight: 63,
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
                    menuItems: Highcharts.getOptions().exporting.buttons.contextButton.menuItems.splice(2)
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
	    	data: []
	    }]
	};

var coption;
var chart;

function prepareOptions() {
	coption = jQuery.extend(true, {}, coption_origin);
	var groupBy = null;
	var chartType = $('#chartType :selected').val();
	var showType = $('#showType :selected').val();
	var dataField = $('#dataField :selected').val();
	var dataNum = $('#dataNum :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();
	// chartType
	if (chartType == 'bar') {
		coption.chart.type = 'bar';
	}
	else if (chartType == 'column') {
		coption.chart.type = 'column';
	}
	else if (chartType == 'pie' || chartType == 'donut' || chartType == 'semiDonut') {
		coption.chart.type = 'pie';
		if (chartType == 'donut' || chartType == 'semiDonut') {
			coption.plotOptions.pie.innerSize = 100;
			
			if (chartType == 'semiDonut') {
				coption.plotOptions.pie.startAngle = -90;
				coption.plotOptions.pie.endAngle = 90;
			}
		}
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
	else if (dataField == 'author') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.author"/>'];
	}
	else if (dataField == 'keyword') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.keyword"/>'];
	}
	else if (dataField == 'opinion') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.opinion"/>'];
	}
	else if (dataField == 'category') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.category"/>'];
	}
	else if (dataField == 'tag') {
		coption.xAxis.categories = ['<bean:message key="search.dataField.tag"/>'];
		groupBy = 'category';
	}
	// showType
	if (showType == '3D') {
		coption.chart.options3d = {
			enabled: true,
            alpha: 0,
            beta: 0,
            depth: 0
		}
		if (chartType == 'bar') {
			coption.plotOptions.bar.depth = 25;
			coption.plotOptions.bar.dataLabels = false;
		}
		else if (chartType == 'column') {
			coption.plotOptions.column.depth = 25;
			coption.plotOptions.column.dataLabels = false;
		}
		else if (chartType == 'pie' || chartType == 'donut' || chartType == 'semiDonut') {
			coption.chart.options3d.alpha = 45;
			coption.plotOptions.pie.depth = 25;
		}
	}
	
	var jsonData = {
		chartType: chartType,
		df: dataField,
		dff: dataFieldFilter,
		num: dataNum,
		groupBy: groupBy,
	};

	return jsonData;
}

function drawChart() {
	$('#main').height( $(window.top).height() * 0.9);

	$.ajax({
		url: baseDataUrl,
		data: prepareOptions(),
		dataType: 'json',
		success: function(result) {
			coption.yAxis_fq =  result.yAxis_fq.reverse();
			var data = result.series.reverse();
			var chartType = coption.chart.type;

			if (chartType == "pie") {
				
				for (var i=0; i < data.length; i++) {
					data[i].y = data[i].data[0];
					delete data[i]["data"];
				}
				var jsonObj = {};
				var outer = [jsonObj];
				jsonObj.data = data;
				coption.series = outer;
				
				coption.series[0].point = {
					events:{
						click: function () {
							var addPart = "";
							var q = $('#searchText').val();
							if (q != '') {
								addPart = "&q=" + encodeURIComponent(q);
							}
							addPart += getUrlQueryPart( coption.yAxis_fq[this.x] );
                    		window.open('<%= request.getContextPath() %>/wiseadm/analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10'+addPart+'<%= fqs %>','_blank');
    					   	return false;
						}
					}
				}
			} else if (chartType == "bar" || chartType == "column") {
				coption.series = data;
				
				for (var i=0; i < coption.series.length; i++) {
					var s = coption.series[i];
					s.point = {
		                events: {
		                    click: (function (index) {
		                    	return function() {
									var addPart = "";
									var q = $('#searchText').val();
									if (q != '') {
										addPart = "&q=" + encodeURIComponent(q);
									}
									addPart += getUrlQueryPart( coption.yAxis_fq[index] );
							   		return false;
		                    	};
		                    })(i)
		                }
		            };
				}
			}
			// $('#main').highcharts(coption);
			chart = new Highcharts.Chart(coption);
			
			if (chartType == "bar" || chartType == "column") {
				//Add mouse events for rotation
				$(chart.container).bind('mousedown.hc touchstart.hc', function(eStart) {
				  eStart = chart.pointer.normalize(eStart);

				  var posX = eStart.pageX,
				      posY = eStart.pageY,
				      alpha = chart.options.chart.options3d.alpha,
				      beta = chart.options.chart.options3d.beta,
				      newAlpha,
				      newBeta,
				      sensitivity = 5; // lower is more sensitive

				  $(document).bind({
				    'mousemove.hc touchdrag.hc': function(e) {
				      // Run beta
				      newBeta = beta + (posX - e.pageX) / sensitivity;
				      chart.options.chart.options3d.beta = newBeta;

				      // Run alpha
				      newAlpha = alpha + (e.pageY - posY) / sensitivity;
				      chart.options.chart.options3d.alpha = newAlpha;

				      chart.redraw(false);
				    },
				    'mouseup touchend': function() {
				      $(document).unbind('.hc');
				    }
				  });
				});
			}
		}
	});
}

$('#opRefresh').click(function() {
	drawChart();
});

$('#btnExportCSV').click(function() {
	if (confirm('<bean:message key="search.export.confirm"/>')) {
		var jsonData = prepareOptions();
		jsonData.format = "csv";

		$.fileDownload(baseDataUrl,
			{
		        preparingMessageHtml: '<bean:message key="search.export.preparingMessage"/>',
		        failMessageHtml: '<bean:message key="search.export.failMessage"/>',
		        httpMethod: "POST",
		        dialogOptions: {
		            modal: true,
		            buttons: {
		              '<bean:message key="global.close"/>': function() {
		                $( this ).dialog( "close" );
		              }
		            }
		        },
		        data: jsonData
		    });
	}
});

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

$('#dataField').on('change', function() {
	var dataField = $('#dataField :selected').val();

	if (dataField == 'tag') {
		$('#dataFieldFilterBlock').show();
	}
	else {
		$('#dataFieldFilterBlock').hide();
	}
	
	drawChart();
});

$('#chartType,#showType,#dataNum').on('change', function() {
	drawChart();
});

$(document).ready(function() {
	chart = drawChart();
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
