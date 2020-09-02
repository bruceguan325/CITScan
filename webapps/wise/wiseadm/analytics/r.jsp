<%@ include file="/commons/taglib.jsp"%><%@ page import="java.net.URLEncoder" 
	import="java.util.*"
	import="java.util.regex.*" import="org.apache.solr.core.*"
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
	import="org.dom4j.Document" import="org.dom4j.DocumentHelper"
	import="org.dom4j.Element" import="com.intumit.solr.*"
	import="com.intumit.solr.util.*" import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.dataset.DataSet.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.searchKeywords.*"
	import="com.intumit.solr.config.ColumnNameMappingFacade"
	import="com.intumit.solr.config.ColumnNameMapping"
	pageEncoding="UTF-8"
	language="java"%><%!
public static String forwardJSP = "s.jsp";

public String getHighlightString(Map<String,Map<String,List<String>>>highlights, String field, String id, String def) {
	List<String> hlList = highlights.get(id).get(field);
	if (hlList != null) {
		for (String hl: hlList) {
			 return hl;
		}
	}

	return def;
}

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}

%>
<%@ include file="translate-map.jsp" %>
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedTarget = (String) pageContext.findAttribute("selectedTarget");
String selectedView = (String) pageContext.findAttribute("selectedView");
AnalyticsUtil au = (AnalyticsUtil) pageContext.findAttribute("analyticsUtil");

boolean usingApi = false;
long numFound = 0;
String apikey = request.getParameter("apikey");
String basicMakeUrlParams = (usingApi ? "apikey=" + apikey + "&" : "") + "v=" + selectedView + "&st=" + selectedTarget;

if ("x".equals(selectedView)) {
	request.getRequestDispatcher("downloadExcel.jsp").forward(request, response);
}
else {
	response.setContentType("text/html");
	response.setCharacterEncoding("UTF-8");
}
%>
<!DOCTYPE html>
<html>
<head>
<meta
	content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'
	name='viewport'>
<meta content='text/html;charset=utf-8' http-equiv='content-type'>

<jsp:include page="header-analytics.jsp" />
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/font-awesome/font-awesome.min.css"></link>
<style>
em { font-style: normal; color: red; }
.ui-state-default { font-size: 0.9em; text-align: left; }
.cattext { color: green; }
.text-positive { color: GREEN; }
.text-negative { color: RED; }
.text-neutral { color: BLACK; }
.op-title { font-size: 1.25em; }
.op-title .op-icon { font-size: 1.5em; }
.op-title span.normal,a:link { color: #0000FD; TEXT-DECORATION: none; }
.op-title span.meta-area { font-size: 0.7em; color: #FD0000; TEXT-DECORATION: none; margin-left: 10px; }
.op-title a:visited { color: #0000FD; TEXT-DECORATION: none; }
.op-title .keyword { font-size: 0.7em; color: RED; TEXT-DECORATION: none; }
.facet-block { overflow-x: hidden; padding-left: 25px; }
.btnChangeView { margin-left: 10px; }
.search-bar { height: 34px; }
.function-separator { padding-right: 15px; }
.input-small { width: auto; }
.input-medium { width: auto; }

#suggest {
	display: none;
	position: absolute;
	background-color: #FFFFFF;
	border: 1px solid black;
	font-size: 12px;
	width: 150px;
}

#suggest div {
	display: block;
	width: 150px;
	overflow: hidden;
	white-space: nowrap;
}

#suggest div.select { /* keydown, keyup */
	color: #000;
	background-color: #99CCFF;
}

#suggest div.over { /* mouse over */
	color: #000;
	background-color: #99CCFF;
}

.btnChangeTarget {
	cursor: pointer;
}
</style>
<script>
		function clickSearch(kw) {
			var theForm = $('#topSearchForm');
			theForm.find('input:first').val(kw);
			doSubmit();
		}
		
		function doSubmit() {
			var theForm = $('#topSearchForm');
			
			if ($("#searchText").val().length > 0) {
				theForm.submit();
			}
			else {
				alert("請輸入查詢關鍵字");
			}
		
			return false;
		}

		function getUrlQueryPart(fq) {
			var addPart = "&fq=" + encodeURIComponent(fq);
			return addPart;
		}
	
		window.clk=function(u,t){
			//var a=encodeURIComponent||escape;window.location=["<%= request.getContextPath() %>/url.jsp?url=",a(u),"&i=",a(t),"&mp=",mp].join("")
			return true;
		};
		
		$(document).ready(function() {
			$("#searchText").keypress(function(event) {
				var currText = $('#searchText').val();
				if (event.keyCode == 13) {
					doSubmit();
				}
			});
			
			$('#searchBtn').on('click', function() { doSubmit(); });
			$('#searchText').focus();
		});
	</script>
</head>
<body class='contrast-fb '>
	<header>
	    <jsp:include page="header2-analytics.jsp" />
	</header>

    <div class='container-fluid'>
        <div class='row-fluid' id='content-wrapper'>
            <div class='col-md-12'>

	<table width="100%">
	<tr><td>
        <form id="topSearchForm">
        <div style="height: 61px; padding: 10px;">
          <div class="col-md-12" id="searchBox">
            <%
            String cleanQ = StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(request.getParameter("q")));
            if ("*:*".equals(cleanQ)) {
            	cleanQ = ""; // 為了不出現 "*:*" 在搜尋框當中
            }
            %>
            <input id="searchText" type="text" class="search-bar col-md-8 clearable" name="q"
			  value="<%= cleanQ %>"
			  placeholder="請輸入篩選關鍵字"
              autocomplete="off">
            <span id='searchBtn'
              style="border: none; background: none; position: relative; right: 32px; top: 5px; z-index: 2; display: inline-block; cursor: pointer;">
              <img src="<%= request.getContextPath() %>/images/search.png">
            </span>
            <input type="hidden" name="qt" value="browse">
            <input type="hidden" name="st" value="<%= selectedTarget %>">
            <input type="hidden" name="v" value="<%= selectedView %>">
          </div>
        </div>
          </form>
	</td></tr>
	</table>
    
	<div id='wrapper'>

		<div class='container-fluid'>
			<div class='row-fluid' id='content-wrapper'>
				<div class='col-md-12'>
		<div class="row">
		<ul class="nav nav-tabs">
			<!-- <a class="btn btn-warning" id="btnDownloadExcel">下載報表</a> -->
			<li class="top-tab"><a class="btnChangeTarget" data-view="qa" title='問答分析' ><span class="glyphicon glyphicon-comment"></span> 問答分析</a></li>
			<li class="top-tab"><a class="btnChangeTarget" data-view="chat" title='進線分析' ><span class="glyphicon glyphicon-stats"></span> 進線分析</a></li>
			<li class="top-tab"><a class="btnChangeTarget" data-view="user" title='使用者分析' ><span class="glyphicon glyphicon-user"></span> 使用者分析</a> </li>
		</ul>
		</div>
		
<div class="row">
<%
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
    numFound = docs.getNumFound();
    
    %>
	<div class="col-lg-2 col-md-3 hidden-sm hidden-xs">
		<ol class="unstyled facet-block">
		<%
		Map<String, Integer> facetQuerys = (Map<String, Integer>) pageContext.findAttribute("facetQueryResults");
    		List<String> queryList = new ArrayList<String>();
        com.intumit.solr.dataset.DataSet.FacetQuery[] dsFQs = ds.getFQ();
        
        for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
			queryList = dsFQs[jjj].facetQuries();
			facetQuerys = (Map<String, Integer>) pageContext.findAttribute("facetQueryResults");
			
			if (facetQuerys != null)
			{
          		%> <H3><%= translate(locale, au, dsFQs, dsFQs[jjj].getFieldName(), false) %></H3> <%
				int prevCcc = 0;
	
	            	for (String key: queryList) {
		        		if (facetQuerys.containsKey(key)) {
		        			int ccc = facetQuerys.get(key).intValue();
		
		        			if (ccc > 0 && ccc != prevCcc) {
		        				prevCcc = ccc;
			                	SolrQuery newQuery = query.getCopy();
			                	newQuery.setStart(0);
			                	newQuery.addFilterQuery("(" + key + ")");
			                	String mappedName = dsFQs[jjj].mappingName(key);
				            	%>
							<span class="facet"> <a
								href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&d=" + dsId + "&" + newQuery.toString()) %>"><%= mappedName %>
									<span class="badge"><%= ccc %></span></a>
							</span>
							<BR>
							<%
		                	}
		            	}
	            }
	            	
	            if ("Date_dt".equals(dsFQs[jjj].getFieldName())) {
	            		%> <a class="btn btn-info btn-mini" id="btnAdvForm">日期篩選</a> <%
				}
	            else {
	            		out.println(dsFQs[jjj].getFieldName());
	            }
				%>
				<BR />
				<%
			}
		}

		List<FacetField> facetFields = (List<FacetField>) pageContext.findAttribute("facetFieldResults");
        if (facetFields != null) {
    			List<String> drillDownFacetField = Arrays.asList(new String[0]);

            for (Iterator<FacetField> iterator = facetFields.iterator(); iterator.hasNext();) {
                FacetField facetEnt = iterator.next();

                if (facetEnt.getValues() != null && facetEnt.getValues().size() > 0) {
	                	boolean isDrillDownMode = drillDownFacetField.contains(facetEnt.getName());
	                	%>
					<H3><%= translate(locale, au, dsFQs, au.getFieldNameResourceKey(facetEnt.getName()), false) %></H3>
					<%
                  	int count = 0;
		            for (Iterator<FacetField.Count> iterator2 = facetEnt.getValues().iterator(); iterator2.hasNext();) {
	                		FacetField.Count facetCount = iterator2.next();
	
	                		if (facetCount.getCount() == 0)
	                			continue;
	                		
	                		if (blacks.contains(facetCount.getName()))
	               			continue;
	
			            	if (count++ == 10) {
			            		%>
							<div id="more<%= facetEnt.getName() %>" class="hide">
							<%
			            	}
			            	
		                	SolrQuery newQuery = query.getCopy();
		                	newQuery.setStart(0);
		                	String qStr = facetEnt.getName() + ":\"" + facetCount.getName() + "\"";
		                	
		                	if (isDrillDownMode) {
		                		qStr = "{!tag=" + facetEnt.getName() + "}" + qStr;
		                	}
		                	boolean alreadyChecked = newQuery.removeFilterQuery(qStr);
		                	newQuery.addFilterQuery( qStr );
	                	
	                    %>
						<span class="facet"> <a
							href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&" + newQuery.toString()) %>">
								<%= alreadyChecked ? "<i class='icon-ok'></i>" : "" %> <%= translate(locale, au, dsFQs, facetCount.getName(), false)%>
								<span class="badge"><%= facetCount.getCount() %></span>
						</a>
						</span><BR>
						<%
                    }
		            
		            if (count > 10) {
		            		%>
						</div>
						<span class="pull-right"><a class="btnMore" data-target="more<%= facetEnt.getName() %>"><span class="icon-fullscreen"></span>&nbsp;more</a>&nbsp;&nbsp;</span>
						<%
		            }
                		%><BR><%
                }
            }
        }
	    %>
		<div style="clear: both; width: 10px; height: 50px; padding: 0">&nbsp;</div>

		</ol>
	</div>
	<div class="col-lg-10 col-md-9 col-sm-12">
		<div class="col-md-12">
			
			<%
			// 麵包屑
			String[] fqs = query.getFilterQueries();
			
			if (!usingApi && fqs != null) {
		        for (String fq : fqs) {
			        	String eachString = StringUtils.strip(fq, "\"()");
			        	eachString = eachString.replaceAll("^\\{[^}]+\\}", "");
	                String meaningfulStr = translate(locale, au, ds.getFQ(), eachString, true);
	            		SolrQuery queryHistoryQuery = query.getCopy();
				    queryHistoryQuery.setStart(0);
	                queryHistoryQuery.removeFilterQuery(fq); // 標準麵包屑
					%><a
						href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&" + queryHistoryQuery.toString()) %>"
						class="btn btn-primary"> <%= meaningfulStr %>&nbsp;&nbsp;<i
						class="icon-remove"></i></a>
					<%
		        }
			}
			%>
			</div>
			<div class="col-md-12 form-inline" style="margin-top: 10px;">
			<%
			String viewFile = "";
			if ("s".equals(selectedView)) {
				viewFile = "r-s.jsp";
			}
			else if ("hitft".equals(selectedView)) {
				viewFile = "r-hitft.jsp";
			}
			else if ("bar".equals(selectedView)) {
				viewFile = "r-bar.jsp";
			}
			else if ("click".equals(selectedView)) {
				viewFile = "r-bar-click.jsp";
			}
			else if ("op2".equals(selectedView)) {
				viewFile = "r-op2.jsp";
			}
			else if ("op".equals(selectedView)) {
				viewFile = "r-op.jsp";
			}
			else if ("bub".equals(selectedView)) {
				viewFile = "r-bub.jsp";
			}
			else if ("line".equals(selectedView)) {
				viewFile = "r-line.jsp";
			}
			else if ("doc".equals(selectedView)) {
				viewFile = "r-line-doc.jsp";
			}
			else if ("pie".equals(selectedView)) {
				viewFile = "r-pie.jsp";
			}
			%>
			<jsp:include page="<%= viewFile %>" flush="true" />
		</div>
	</div>
</div>
<%
}
catch (Exception e) {
	e.printStackTrace();
%>
	<div id="noresult">
		<div class="alert alert-error">
			很抱歉，我們無法找到符合條件的查詢結果。<br /> 
			建議您：<br /> 
			1. 請縮短查詢字組來擴大您的搜尋範圍。<br />
			2. 請檢查查詢字有無錯別字。<br /> 
			3. 請以新的關鍵字或相似詞重新查詢。<br />
		</div>
	</div>
	<%
}
%>
				</div>
			</div>
		</div>
	</div>

	<div id="advSearchForm" style="display: none;">
		<form class="pf-form pform_custom" method="post" action="s.jsp">
			<div class="pf-element pf-heading">
				<h3>日期篩選</h3>
			</div>
			<div class="pf-element">
				<label> <span class="pf-label">時間範圍</span>
				</label> <input class="pf-field" size="20" type="text" name="dateFrom" /> <input class="pf-field" size="20" type="text" name="dateTo" />
			</div>
			<div class="pf-element pf-buttons pf-centered">
				<input class="pf-button btn btn-primary" type="button" name="doit" value="Submit" /> <input class="pf-button btn btn-default" type="button" name="cancel" value="Cancel" />
			</div>
		</form>
	</div>
	<div id="downloadExcelForm" style="display: none;">
		<form class="pf-form pform_custom" action="<%= request.getContextPath() %>/downloadExcel.jsp" method="post">
			<div class="pf-element pf-heading">
				<h3>匯出Excel</h3>
				匯出功能相關注意事項：
				<ol>
					<li>匯出資料最多十萬筆</li>
					<li>匯出動作將視資料筆數花費時間從1分鐘到十多分鐘不等</li>
				</ol>
			</div>
			<div class="pf-element pf-buttons pf-centered">
				<input class="pf-button btn btn-primary btnChangeView" data-view="x" type="button" name="doit" value="開始匯出" /> 
				<input class="pf-button btn btn-default" type="button" name="cancel" value="Cancel" />
			</div>
		</form>
	</div>
	<script>
$('.btn-adjust-opinion').click(function(e) {
	var dataUrl = $(this).attr('data-url');
	var dataOpinion = $(this).attr('data-opinion');
	var $this = $(this);	
	
	$.ajax({
		url: 'adjust-opinion.jsp',
		dataType: 'json',
		data: {url: dataUrl, opinion: dataOpinion},
		success: function(result) {
			$this.addClass('text-success');
		}
	});
});

$('.btn-block-url').click(function(e) {
	var dataUrl = $(this).attr('data-url');
	var $this = $(this);	
	
	if (confirm("此動作將刪除此文章，並且同一網址之文章將不會再收錄至本帳號，確定要刪除？")) {
		$.ajax({
			url: 'block-url.jsp',
			dataType: 'json',
			data: {url: dataUrl},
			success: function(result) {
				$this.addClass('text-success');
			}
		});
	}
});

$(document).ready(function() {
	$('.btnMore').click(function() {
		$targetDiv = $(this).attr('data-target');
		$('#'+$targetDiv).toggle();
	});
	$('a[data-view=<%= selectedTarget %>]').closest('li').addClass('active');
	
	$('a[data-view=<%= selectedView %>]').closest('li').addClass('active');
	
	//$.datepicker.setDefaults({
	//	dateFormat: 'yy/mm/dd',
	//});

	$('#btnAdvForm').click(function(e) {
		var notice = new PNotify({
		    text: $('#advSearchForm').html(),
		    icon: false,
		    width: '50%',
		    height: '50%',
		    hide: false,
		    buttons: {
		        closer: false,
		        sticker: false
		    },
		    insert_brs: false
		});
		
		notice.get().find('input[name="dateFrom"]').datepicker({
			defaultDate: "-1w"
		});
		notice.get().find('input[name="dateTo"]').datepicker({
			defaultDate: new Date()
		});
		notice.get().find('input[name="dateFrom"]').datepicker("setDate", "-1w");
		notice.get().find('input[name="dateTo"]').datepicker("setDate", new Date());
		notice.get().find('form.pf-form').on('click', '[name=cancel]', function() {
		    notice.remove();
		});
		notice.get().find('form.pf-form').on('click', '[name=doit]', function() {
			var dateFrom = notice.get().find('input[name="dateFrom"]').datepicker("getDate");
			var dateTo = notice.get().find('input[name="dateTo"]').datepicker("getDate");
			var urlMainPart = window.location.href.split('?')[0];
			
			dateFrom.setDate(dateFrom.getDate() - 1);
			
			var params = window.location.search;
			params += '&fq=Date_dt:[' 
				+ $.datepicker.formatDate('yy-mm-dd', dateFrom) + 'T16:00:00.000Z TO '
				+ $.datepicker.formatDate('yy-mm-dd', dateTo) + 'T15:59:59.000Z]';
			
			document.location = urlMainPart + params;
		});
		
		return true;
	});
	
	$('#btnDownloadExcel').click(function(e) {
		var notice = new PNotify({
		    text: $('#downloadExcelForm').html(),
		    icon: false,
		    width: '50%',
		    height: '50%',
		    hide: false,
		    buttons: {
		        closer: false,
		        sticker: false
		    },
		    insert_brs: false
		});
		
		notice.get().find('input[name="query"]').val($('#searchText').val());
		notice.get().find('input[name="dateFrom"]').datepicker({
			defaultDate: "-1w"
		});
		notice.get().find('input[name="dateTo"]').datepicker({
			defaultDate: new Date()
		});
		notice.get().find('input[name="dateFrom"]').datepicker("setDate", "-1w");
		notice.get().find('input[name="dateTo"]').datepicker("setDate", new Date());
		notice.get().find('form.pf-form').on('click', '[name=cancel]', function() {
		    notice.remove();
		});

		return true;
	});

	$(document).on(
		'click',
		'.btnChangeView',
		function(e) {
			var $this = $(this);
			var urlMainPart = window.location.href.split('?')[0]
			var params = window.location.search;
			console.log($.query);
			params = params.replace("v=" + $.query.get('v'), "v=" + $this.attr('data-view'));
			console.log(params);

			document.location = urlMainPart + params;
		});
	

	$(document).on(
		'click',
		'.btnChangeTarget',
		function(e) {
			var $this = $(this);
			var urlMainPart = window.location.href.split('?')[0]
			var params = window.location.search;
			if ($.query.get('st') == '') {
				params += "&st=" + $this.attr('data-view');
			}
			else {
				params = params.replace("st=" + $.query.get('st'), "st=" + $this.attr('data-view'));
			}
			document.location = urlMainPart + params;
		});
});

	</script>
</body>
</html>
