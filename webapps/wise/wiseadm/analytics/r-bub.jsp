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
         import="com.intumit.message.MessageUtil"
         import="com.intumit.solr.*"
         import="com.intumit.solr.util.*"
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.searchKeywords.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
%><%!
public static String forwardJSP = "s.jsp";

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}
%>
<script type='text/javascript' src='<%= request.getContextPath() %>/script/jquery.fileDownload.js'></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-more.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/exporting.js"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%@ include file="translate-map.jsp" %>
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
org.json.JSONObject defaultViewOptions = (org.json.JSONObject) pageContext.findAttribute("defaultViewOptions");
String searchType = StringUtils.defaultString(request.getParameter("searchType"), "all");
String originQ = URLDecoder.decode(StringUtils.defaultString(request.getParameter("originQ"), ""), "utf-8");
String selectedTarget = (String) pageContext.findAttribute("selectedTarget");
String selectedView = (String) pageContext.findAttribute("selectedView");
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

	String[] sortables = StringUtils.trimToEmpty(ds.getSortables()).split(",");

	StringBuilder paginationInfo = new StringBuilder();
	paginationInfo.append(MessageUtil.getMessageResources().getMessage(locale, "search.number.found2", new Object[]{docs.getNumFound()}));
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
       <div align="left" class="text hide">
      		<div class="row" style="margin-left: 30px;">
			<div class="col-md-12" align="right" >
			<%= "分析費時 " + qtime + " 秒" %>
			<%= docs.getNumFound() > 0 ? "，" + paginationInfo : "" %>
			</div>
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
        		<!-- HitFT Area -->
	        <div class='row' id='content-wrapper'>
	            <div class='col-md-12'>
	            
	           		<bean:message key="analytics.type"/>
				    <select id="selectedView" name="selectedView" class="form-control optionPanel input-medium">
				    	<% for (String at: au.getAnalyticTypes()) { %>
					    	<option value="<%= at %>"><%= MessageUtil.getMessage(locale, (String)au.getAnalyticTypeConfig(at).get("resourceKey")) %></option>
				    	<% } %>
				    </select>
				    
			    		<span class="function-separator"></span>
			    	
	        			<bean:message key="analytics.fn"/>
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
	            
					<bean:message key="search.dataField.axis.xy"/>：
					
				    <select id="chartType" name="chartType" class="form-control optionPanel input-medium">
				    	<option value="coverage"><bean:message key="search.view.bub.coverage"/></option>
				    	<option value="hotAndCoverage" selected><bean:message key="search.view.bub.hotAndCoverage"/></option>
				    	<%--option value="positiveAndNegative" selected><bean:message key="search.view.bub.positiveAndNegative"/></option>
				    	<option value="soundAndEmotion"><bean:message key="search.view.bub.soundAndEmotion"/></option--%>
				    </select>
					    
					<button id='opRefresh' class='btn btn-primary hide'><bean:message key="global.refresh"/></button>
					<!-- <button id='btnExportCSV' class='btn btn-primary'><bean:message key="search.export"/></button>  -->
					
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
	        <div class='row'>
	            <div class='col-md-12'>
					    <div id="main"></div>
					    <br>
					    <p id="copyright" style="margin-left: 30px; display: none;"><font color="red"></font><span>All rights reserved.|© 1999-2018 </span><a href="http://www.intumit.com/">Intumit Inc.</a></p>
	            </div>
	        </div>
        <% } %>

<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/analytics/adv-bub-stat-ajax.jsp?<%= query.toString() %>&st=<%= selectedTarget %>&v=<%= selectedView %>&d=<%= dsId %>&ts=<%= System.currentTimeMillis() %>";

// Positive And Negative
var coption_PN = {
	chart: {
	    type: 'bubble',
	    marginTop: 70,
	    plotBorderWidth: 1,
	    zoomType: 'xy'
	},
	
	title: {
	    text: '<bean:message key="search.view.bub.coverage"/>',
	},
	
	subtitle: {
        text: 'Source: SmartRobot'
	},
	
	xAxis: {
	    gridLineWidth: 1,
	    allowDecimals: false,
	    title: {
	        text: '<bean:message key="search.view.bub.coverage.hit"/>'
	    },
	    labels: {
	    	formatter: function() {
                if(this.value < 0){
                    return;
                } else {
                    return this.value + ' 篇';
                }
            }
	    }
	},
	
	yAxis: {
		allowDecimals: false,
	    title: {
	        text: '<bean:message key="search.view.bub.coverage.notHit"/>'
	    },
	    labels: {
        	formatter: function() {
                if(this.value < 0){
                    return;
                } else {
                    return this.value + ' 篇';
                }
            }
	    }
	},
	
	tooltip: {
	    useHTML: true,
	    headerFormat: '<table>',
	    pointFormat: '<tr><th colspan="2"><h3>{point.name}</h3></th></tr>' +
	        		 '<tr><th>覆蓋:</th><td>{point.x}</td></tr>' +
	                 '<tr><th>未覆蓋:</th><td>{point.y}篇</td></tr>' +
	                 '<tr><th>總量:</th><td>{point.z}篇</td></tr>',
	    footerFormat: '</table>',
	},
	
	plotOptions: {
		bubble:{
		    maxSize: 100
		},
	    series: {
	        dataLabels: {
	            enabled: false,
	            format: '{point.name}'
	        }
	    }
	},
	
    legend: {
    	enabled: true
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
    
    series: []
};

// Sound And Emotion
var coption_SE = {
	chart: {
	    type: 'bubble',
	    marginTop: 70,
	    plotBorderWidth: 1,
	    zoomType: 'xy'
	},
	
	title: {
	    text: '<bean:message key="search.view.bub.hotAndCoverage"/>'
	},
	
	subtitle: {
	    text: 'Source: SmartRobot',
	},
	
	xAxis: {
	    gridLineWidth: 1,
	    title: {
	        text: '搜尋量'
	    },
	    labels: {
	        format: '{value} 次'
	    }
	},
	
	yAxis: {
	    title: {
	        text: '<bean:message key="search.view.bub.coverage"/>'
	    },
	    labels: {
			format: '{value}'
	    },
		plotLines: [{
		    color: 'black',
		    dashStyle: 'dot',
		    width: 2,
		    value: 0,
		    label: {
		        align: 'right',
		        style: {
		            fontStyle: 'italic'
		        },
		        text: '',
		        x: -10
		    },
		    zIndex: 3
		}]
	},
	   
	tooltip: {
		useHTML: true,
	    headerFormat: '<table>',
	    pointFormat: '<tr><th colspan="2"><h3>{point.name}</h3></th></tr>' +
	        		 '<tr><th>搜尋量:</th><td>{point.x}篇</td></tr>' +
	                 '<tr><th>覆蓋率:</th><td>{point.y:.2f}</td></tr>',
	    footerFormat: '</table>',
	},
	
	plotOptions: {
		bubble:{
		    maxSize: 80
		},
		series: {
	    	dataLabels: {
               enabled: true,
               format: '{point.name}'
           }
	    }
	},
	
	legend: {
		enabled: true
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
	   
	series: []
        
};

function prepareOptions() {
	<% if (defaultViewOptions != null) { %>
	return <%= defaultViewOptions.toString() %>;
	<% } else { %>
	var groupBy = null;
	var chartType = $('#chartType :selected').val();
	var dataField = $('#dataField :selected').val();
	
	if (dataField == 'tag') {
		groupBy = 'category';
	}
	
	var dataNum = $('#dataNum :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();

	var jsonData = {
		chartType: chartType,
		df: dataField,
		dff: dataFieldFilter,
		num: dataNum,
		groupBy: groupBy,
	};

	return jsonData;
	<% } %>
}

function drawChart() {
	$('#main').height( $(window.top).height() * 0.9);
	
	var jsonData = prepareOptions();
	var chartType = jsonData.chartType;

	$.ajax({
		url: baseDataUrl,
		data: jsonData,
		dataType: 'json',
		success: function(result) {

			if (chartType == "coverage") {
				coption_PN.series = result.series;
				
				for (var i = 0; i < coption_PN.series.length; i++) {
					var s = coption_PN.series[i];
					s.point = {
		                events: {
		                    click: (function (index) {
		                    	return function() {
		                    		window.open('<%= request.getContextPath() %>/wiseadm/analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&originQ=<%= URLEncoder.encode(originQ, "utf-8") %>&selectType=<%= searchType %>&d=<%= dsId %>&fq='+result.fq[index],'_blank');
							   		return false;
		                    	};
		                    })(i)
		                }
		            };
				}
				$('#main').highcharts(coption_PN);
			}
			else if (chartType == "hotAndCoverage") {
				coption_SE.series = result.series;
				
				for (var i = 0; i < coption_SE.series.length; i++) {
					var s = coption_SE.series[i];
					s.point = {
		                events: {
		                    click: (function (index) {
		                    	return function() {
		                    		window.open('<%= request.getContextPath() %>/wiseadm/analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&originQ=<%= URLEncoder.encode(originQ, "utf-8") %>&selectType=<%= searchType %>&d=<%= dsId %>&fq='+result.fq[index],'_blank');
							   		return false;
		                    	};
		                    })(i)
		                }
		            };
				}
				$('#main').highcharts(coption_SE);
			}
		},
		error: function(jqXHR, textStatus, errorThrown) {
		  console.log(textStatus, errorThrown);
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
});

$('#dataField,#gap,#groupField,#dataNum,#chartType,#showType').on('change', function() {
	drawChart();
});

$(document).ready(function() {
	drawChart();
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
