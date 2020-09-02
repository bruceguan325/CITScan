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
         import="com.intumit.message.*" 
         import="com.intumit.solr.*" 
         import="com.intumit.solr.util.*" 
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.SearchManager"
         import="com.intumit.solr.user.*"
		 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
%><%!
public static String forwardJSP = "s.jsp";

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/qa-analytics/" + jsp + (query == null ? "" : ("?" + (query)));
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
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
User user = User.getFromSession(session);

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
很抱歉，我們無法找到符合“<em><%= query.getQuery() %></em>”的查詢結果。<br />
建議您：<br />
1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
2.	請檢查查詢字有無錯別字。<br />
3.	請以新的關鍵字或相似詞重新查詢。<br />
若您對搜尋查詢結果仍有疑問，歡迎寫信到<a style='font-size: medium;' href='mailto:support@intumit.com'>support@intumit.com</a>，獲得更多建議。
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
	            		<bean:message key="search.dataField.filterBlock"/>：<br>
					    <select id="dataFieldFilter" name="dataFieldFilter" class="optionPanel input-medium">
					    	<option value="" selected>全部</option>
					    </select>
					    </div>
				</div>
	            <div class='col-md-4'>
	            		<bean:message key="search.dataField.selectAmount"/>：<br>
					    <select id="dataNum" name="dataNum" class="optionPanel input-small">
					    	<option value="10" selected><bean:message key="global.number" arg0="10"/></option>
					    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
					    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
					    	<option value="50"><bean:message key="global.number" arg0="50"/></option>
					    	<option value="1000"><bean:message key="global.number.all"/></option>
					    </select>
				</div>
				<div class='col-md-2'>
					<button id='opRefresh' class='btn btn-primary'><bean:message key="global.refresh"/></button>
					<button id='btnExportCSV' class='btn btn-primary'><bean:message key="search.export"/></button>
				</div>
	        </div>
	        <div class='row'>
	            <div class='col-md-12'>
					    <div id="main">
					     
					    </div>
					    <p id="copyright" style="margin-left: 30px;"><font color="red"></font><span>All rights reserved.<span>|</span>© 1999-2017 <a href="http://www.intumit.com/">Intumit Inc.</a></p>
	            </div>
	        </div>
        <% } %>

<script type="text/javascript">

var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/qa-analytics/adv-stat-ajax.jsp?<%= query.toString() %>" + "&ts=<%= System.currentTimeMillis() %>";

var coption = {
	    chart: {
	        type: 'bar',
            marginTop: 80,
            marginRight: 40
	    },
	    title: {
	        text: "<bean:message key='analytics.chartType.topQuestion'/>"
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
	            text: "<bean:message key='total.question.count'/>",
	            align: 'high'
	        },
	        labels: {
	            overflow: 'justify'
	        }
	    },
	    tooltip: {
	        valueSuffix: " <bean:message key='total.count'/>"
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

	var groupBy = null;
	var dataField = $('#dataField :selected').val();
	if (dataField == 'questionType') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.questionType'/>"];
	}
	else if (dataField == 'questionCategory') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.questionCategory'/>"];
	}
	else if (dataField == 'matchedQuestion') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.matchedQuestion'/>"];
	}
	else if (dataField == 'questionKeyword') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.questionKeyword'/>"];
	}
	else if (dataField == 'kp') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.kp'/>"];
	}
	else if (dataField == 'answerType') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.answerType'/>"];
	}
	else if (dataField == 'tenant') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.tenant'/>"];
	}
	else if (dataField == 'identity') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.identity'/>"];
	}
	else if (dataField == 'opinion') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.opinion'/>"];
	}
	else if (dataField == 'eservice') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.eservice'/>"];
	}
	else if (dataField == 'questionTag') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.questionTag'/>"];
	}
	else if (dataField == 'userType') {
		coption.xAxis.categories = ["<bean:message key='analytics.fn.userType'/>"];
	}
	var dataNum = $('#dataNum :selected').val();
	var dataFieldFilter = $('#dataFieldFilter :selected').val();
	

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
			coption.series = result.series.reverse();
			coption.yAxis_fq =  result.yAxis_fq.reverse();
			
			for (var i=0; i < coption.series.length; i++) {
				var s = coption.series[i];
				s.point = {
	                events: {
	                    click: (function (index) {
	                    	return function() {
	                    		window.open('<%= request.getContextPath() %>/wiseadm/qa-analytics/s.jsp?v=s&q=*%3A*&sort=Date_dt+desc&start=0&rows=10&fq='+coption.yAxis_fq[index]+'<%= fqs %>','_blank');
						   	return false; 
	                    	};
	                    })(i)
	                }
	            };
			}
			$('#main').highcharts(coption);	
		}
	});
}

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

