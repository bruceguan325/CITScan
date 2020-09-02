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
         import="com.intumit.message.*"
         import="com.intumit.solr.*"
         import="com.intumit.solr.util.*"
         import="com.intumit.solr.user.*"
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.searchKeywords.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
         import="it.exprivia.cnos.opencloud.*"
%><%!
public static String forwardJSP = "s.jsp";

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}
%>
<script type='text/javascript' src='<%= request.getContextPath() %>/script/jquery.fileDownload.js'></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-more.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/exporting.js"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%
Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);

SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
org.json.JSONObject defaultViewOptions = (org.json.JSONObject) pageContext.findAttribute("defaultViewOptions");
User user = User.getFromSession(session);
String searchType = StringUtils.defaultString(request.getParameter("searchType"), "all");
String originQ = URLDecoder.decode(StringUtils.defaultString(request.getParameter("originQ"), ""), "utf-8");

try {
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
// 	paginationInfo.append("共查詢到 ");
// 	paginationInfo.append(docs.getNumFound());
// 	paginationInfo.append(" 筆");
	paginationInfo.append(MessageUtil.getMessage(locale, "search.number.found2", new String[]{ "" + docs.getNumFound()}));
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
       <div align="left" class="text">
      		<div class="row" style="margin-left: 30px;">
			<div class="col-md-12" align="right" >
			<%= MessageUtil.getMessage(locale, "search.spend.second", new String[] {"" + qtime}) %>
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
	            <div class='col-md-4'>
	            		<bean:message key="search.dataField.column"/>：<br>
					    <select id="dataField" name="dataField" class="optionPanel input-medium">
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
					    <div id="dataFieldFilterBlock" style="display: none;">
	            		<bean:message key="search.dataField.filterBlock"/><span data-toggle="tooltip" title='<bean:message key="search.dataField.filterBlock.tooltip"/>'><i class="icon icon-question-sign"></i></span><br>
					    <select id="dataFieldFilter" name="dataFieldFilter" class="optionPanel input-medium">
					    	<option value="" selected>全部</option>
					    </select>
					    </div>
					    <div>
	            		<bean:message key="search.dataField.selectAmount"/>：<br>
					    <select id="dataNum" name="dataNum" class="optionPanel input-small">
					    	<option value="10" selected><bean:message key="global.number" arg0="10"/></option>
					    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
					    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
					    	<option value="50"><bean:message key="global.number" arg0="50"/></option>
					    	<option value="1000"><bean:message key="global.number.all"/></option>
					    </select>
				</div>
				</div>
				<div class='col-md-4'>
				<bean:message key="search.dataField.axis.xy"/>：<br>
					    <select id="chartType" name="chartType" class="optionPanel input-medium">
					    	<option value="coverage"><bean:message key="search.view.bub.coverage"/></option>
					    	<option value="hotAndCoverage" selected><bean:message key="search.view.bub.hotAndCoverage"/></option>
					    	<%--option value="positiveAndNegative" selected><bean:message key="search.view.bub.positiveAndNegative"/></option>
					    	<option value="soundAndEmotion"><bean:message key="search.view.bub.soundAndEmotion"/></option--%>
					    </select>
				</div>
				<div class='col-md-4'>
					<button id='opRefresh' class='btn btn-primary'><bean:message key="global.refresh"/></button>
					<!-- <button id='btnExportCSV' class='btn btn-primary'><bean:message key="search.export"/></button>  -->
				</div>
	        </div>
	        <div class='row'>
	            <div class='col-md-12'>
					    <div id="main"></div>
					    <p id="copyright" style="margin-left: 30px;"><font color="red"></font><span>All rights reserved.<span>|</span>© 1999-2017 <a href="http://www.intumit.com/">Intumit Inc.</a></p>
	            </div>
	        </div>
        <% } %>

<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/qa-analytics/adv-bub-stat-ajax.jsp?<%= query.toString() %>&d=<%= dsId %>&ts=<%= System.currentTimeMillis() %>";

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
	    text: 'Source: SmartOMS',
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
                    return this.value + " <bean:message key='search.doc.unit'/>";
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
                    return this.value + " <bean:message key='search.doc.unit'/>";
                }
            }
	    }
	},
	
	tooltip: {
	    useHTML: true,
	    headerFormat: '<table>',
	    pointFormat: "<tr><th colspan='2'><h3>{point.name}</h3></th></tr>" +
	        		 "<tr><th><bean:message key='search.view.bub.coverage.hit'/>:</th><td>{point.x}</td></tr>" +
	                 "<tr><th><bean:message key='search.view.bub.coverage.notHit'/>:</th><td>{point.y}<bean:message key='search.doc.unit'/></td></tr>" +
	                 "<tr><th><bean:message key='total.question.count'/>:</th><td>{point.z}<bean:message key='search.doc.unit'/></td></tr>",
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
	        text: "<bean:message key='search.view.bar.1'/>"
	    },
	    labels: {
	        format: "{value} <bean:message key='search.doc.unit'/>"
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
	        		 '<tr><th><bean:message key="total.question.count"/>:</th><td>{point.x}<bean:message key="search.doc.unit"/></td></tr>' +
	                 '<tr><th><bean:message key="search.view.bub.coverage.hit.rate"/>:</th><td>{point.y:.2f}</td></tr>',
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
		                    		window.open('<%= request.getContextPath() %>/wiseadm/qa-analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&originQ=<%= URLEncoder.encode(originQ, "utf-8") %>&selectType=<%= searchType %>&d=<%= dsId %>&fq='+result.fq[index],'_blank');
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
		                    		window.open('<%= request.getContextPath() %>/wiseadm/qa-analytics/s.jsp?v=s&sort=Date_dt+desc&start=0&rows=10&<%= query.toString() %>&originQ=<%= URLEncoder.encode(originQ, "utf-8") %>&selectType=<%= searchType %>&d=<%= dsId %>&fq='+result.fq[index],'_blank');
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
