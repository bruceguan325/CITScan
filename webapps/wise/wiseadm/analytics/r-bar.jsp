<%@ include file="/commons/taglib.jsp"%>
<%@ page import="java.net.URLEncoder"
		 import="java.util.*"
		 import="java.util.regex.*"
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
         import="com.intumit.solr.SearchManager"
		 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
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
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/highcharts-3d.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/script/highcharts/modules/exporting.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/script/jquery.query-object.js"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%@ include file="translate-map.jsp" %>
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
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
			if (StringUtils.startsWith(fq, "Keyword_ms:")) {
				fq = StringUtils.substringAfter(fq, "Keyword_ms:");
			}
			fqs += "&fq=" + URLEncoder.encode(fq, "UTF-8");
		}
		catch (Exception ignored) {}
	}

       //For simplification, we are not using the JSTL
       SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
	SolrQuery sortByQuery = query.getCopy();
	
	String[] sortables = StringUtils.trimToEmpty(ds.getSortables()).split(",");

  	StringBuilder paginationInfo = new StringBuilder();
  	paginationInfo.append(MessageUtil.getMessage(locale, "search.number.found2", new String[] {"" + docs.getNumFound()}));
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
		<%= MessageUtil.getMessage(locale, "search.spend.second", new String[] {"" + qtime}) %>
		<%= docs.getNumFound() > 0 ? "，" + paginationInfo : "" %>
		</div>
	</div>
   </div>
    	<!-- HitFT Area -->
        <div class='row' id='content-wrapper'>
            <div class='col-md-12'>
           		<bean:message key="analytics.type"/>
			    <select id="selectedView" name="selectedView" class="form-control optionPanel input-medium">
			    	<% for (String at: au.getAnalyticTypes()) { %>
				    	<option value="<%= at %>"><%= translate(locale, au, null, (String)au.getAnalyticTypeConfig(at).get("resourceKey"), false) %></option>
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
很抱歉，我們無法找到符合“<em><%= query.getQuery() %></em>”的分析結果。<br />
建議您：<br />
1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
2.	請檢查查詢字有無錯別字。<br />
3.	請以新的關鍵字或相似詞重新查詢。<br />
若您對搜尋查詢結果仍有疑問，歡迎寫信到<a style='font-size: medium;' href='mailto:support@intumit.com'>support@intumit.com</a>，獲得更多建議。
		</div>
		</div>
	<% } else { %>
        <div class='row'>
            <div class='col-md-12'>
				    <div id="main">
				     
				    </div>
				    <p id="copyright" style="margin-left: 30px; display: none;"><font color="red"></font><span>All rights reserved.|© 1999-2017 </span><a href="http://www.intumit.com/">Intumit Inc.</a></p>
            </div>
        </div>
    <% } %>

<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/analytics/adv-stat-ajax.jsp?<%= query.toString() %>&st=<%= selectedTarget %>&v=<%= selectedView %>&ts=<%= System.currentTimeMillis() %>";

var coption = {
    chart: {
        type: 'bar',
        marginTop: 80,
        marginRight: 40
    },
    title: {
        text: '<bean:message key='analytics.chartType.topQuestion'/>'
    },
    subtitle: {
        text: 'Source: SmartRobot'
    },
    plotOptions: {
        column: {
            stacking: 'normal',
            depth: 40
        }
    },
    xAxis: {
        title: {
            text: '',
        }
    },
    yAxis: {
        min: 0,
        title: {
            text: '<bean:message key='total.question.count'/>',
            align: 'high'
        },
        labels: {
            overflow: 'justify'
        }
    },
    tooltip: {
        valueSuffix: ' <bean:message key='total.count'/>'
    },
    plotOptions: {
        bar: {
            dataLabels: {
                enabled: true
            }
        }
    },
    legend: {
        layout: 'vertical',
        align: 'right',
        verticalAlign: 'bottom',
        y: -40,
        floating: true,
        borderWidth: 1,
        backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
        shadow: true,
        reversed: true,
    },
    credits: {
        enabled: false
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
        name: 'Year 1800',
        data: [107]
    }, {
        name: 'Year 1900',
        data: [133]
    }, {
        name: 'Year 2008',
        data: [973]
    }]
};

function drawChart() {
	$('#main').height( $(window.top).height() - 250 );

	// dataField
	dataFieldNameMap = {
		<% for (String key: au.getParamNameToResourceKey().keySet()) { %>	
		'<%= key %>': '<%= com.intumit.message.MessageUtil.getMessage(locale, au.getParamNameResourceKey(key)) %>',
		<% } %>
	}

	var groupBy = null;
	var dataField = $('#dataField :selected').val();
	var dataNum = $('#dataNum :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();
	coption.xAxis.categories = [dataFieldNameMap[dataField]];
	
	$.ajax({
		url: baseDataUrl,
		data: {
			df: dataField,
			dff: dataFieldFilter,
			num: dataNum,
			groupBy: groupBy,
		},
		dataType: 'json',
		success: function(result) {
			coption.series = result.series;
			coption.yAxis_fq =  result.yAxis_fq;
			
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
	                    		window.open('<%= request.getContextPath() %>/wiseadm/analytics/s.jsp?v=s'+addPart+'<%= fqs %>','_blank');
							   	return false; 
	                    	};
	                    })(i)
	                }
	            };
			}
			$('#main').highcharts(coption);	
		}
	});
};

$('#opRefresh').click(function() {
	drawChart();
});

$('#btnExportCSV').click(function() {
	var groupBy = null;
	var dataField = $('#dataField :selected').val();
	if (dataField == 'tag') {
		groupBy = 'category';
	}
	var dataNum = $('#dataNum :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();
	
	$.fileDownload(baseDataUrl,
		{
	        preparingMessageHtml: "正在準備資料，完成後會開始下載。下載完成後請點選關閉。",
	        failMessageHtml: "資料準備錯誤，請確認網路以及登入狀況正常後再試一次",
	        httpMethod: "POST",
	        dialogOptions: {
	            modal: true,
	            buttons: {
	              "關閉": function() {
	                $( this ).dialog( "close" );
	              }
	            }
	        },
	        data: {
				df: dataField,
				dff: dataFieldFilter,
				num: dataNum,
				groupBy: groupBy,
				format: "csv"
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

$('#dataNum').on('change', function() {
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
	很抱歉，我們無法找到符合條件的查詢結果。<br />
	建議您：<br />
	1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
	2.	請檢查查詢字有無錯別字。<br />
	3.	請以新的關鍵字或相似詞重新查詢。<br />
	</div>
	</div>
<%
}
%>

