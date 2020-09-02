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
         import="com.intumit.solr.dataset.DataSet.*"
         import="com.intumit.solr.SearchManager"
         import="com.intumit.solr.searchKeywords.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
         import="com.intumit.solr.user.User"
         import="it.exprivia.cnos.opencloud.*" 
		 pageEncoding="UTF-8" language="java" 
%><%!
public static String forwardJSP = "s.jsp";

static final Pattern breadcrumbPattern1 = Pattern.compile("\\(.*\\)");
static final Pattern breadcrumbPattern2 = Pattern.compile("\\[.*\\]");

java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
java.text.SimpleDateFormat sdfToday = new java.text.SimpleDateFormat("HH:mm");
java.text.DecimalFormat df = new java.text.DecimalFormat("$###,###");
String pricePattern = "(.*?):\\[([0-9\\*]+) TO ([0-9\\*]+)\\]";
String datePattern = "(.*?Date.*?):\\[([^\\s\\]]*) TO ([^\\s\\]]*)\\]";
String fgPattern = "(.*?):\\[([^\\s\\]]* TO [^\\s\\]]*)\\]";

HashMap<String, String> translateMap = new HashMap<String, String>();
{
	translateMap.put("", "(N/A)");
	translateMap.put("PROFESSIONAL", "analytics.questionType.professional");
	translateMap.put("PROFESSIONAL_PLUGIN", "hierarchical.qa");
	translateMap.put("PREPARE_FORWARD", "forward.option.webChat.ready");
	translateMap.put("UNKNOWN", "analytics.questionType.unknown");
	translateMap.put("CASUAL", "analytics.questionType.casual");
	translateMap.put("REPEAT_TOO_MANY_TIMES", "analytics.questionType.repeatTooManyTimes");
	translateMap.put("INTERCEPTED", "analytics.questionType.intercepted");
	translateMap.put("BAD_QUESTION", "analytics.questionType.badQuestion");
	translateMap.put("ANSWER_OPTION", "analytics.questionType.answerOption");
	translateMap.put("ANSWER_FORWARD", "analytics.questionType.answerForward");
	translateMap.put("ANSWER_FORWARD_OPTION", "analytics.questionType.answerForwardOption");
	translateMap.put("無匹配", "smart.robot.qa.confidence.noAnswer");
	translateMap.put("NO_ANSWER", "analytics.answerType.noAnswer");
	translateMap.put("QA_PATTERN", "analytics.answerType.qaPattern");
	translateMap.put("MLT_ONLY", "analytics.answerType.mltOnly");
	translateMap.put("SEARCH", "analytics.answerType.search");
	translateMap.put("STAY_WITH_ROBOT", "analytics.answerType.stayWithRobot");
	translateMap.put("FORWARD", "analytics.answerType.forward");
	// translateMap.put("", "");
}

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
	String url = req.getContextPath() + "/wiseadm/qa-analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}

public String translate(Locale locale, FacetQuery[] fqs, String org, boolean includeFieldName) {
	String translated = org;
	
	if (org == null)
		return "";
	
	if (fqs != null)
	for (FacetQuery fq: fqs) {
		String t = fq.mappingName(org);
		if (t != null)
			return t;
	}

	if (org.matches(fgPattern)) {
		if (org.matches(pricePattern)) {
			Matcher m = Pattern.compile(pricePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String priceFrom = m.group(2);
			String priceTo = m.group(3);

			if ("*".equals(priceFrom)) {
				translated = df.format(Integer.parseInt(priceTo)) + " 以下";
			}
			else if ("*".equals(priceTo)) {
				translated = df.format(Integer.parseInt(priceFrom)) + " 以上";
			}
			else {
				translated = df.format(Integer.parseInt(priceFrom)) + " ~ " + df.format(Integer.parseInt(priceTo));
			}
		}
		if (org.matches(datePattern)) {
			Matcher m = Pattern.compile(datePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String dateFrom = m.group(2);
			String dateTo = m.group(3);

			if (translateMap.containsKey(dateFrom + " TO " + dateTo)) {
				translated = translateMap.get(dateFrom + " TO " + dateTo);
			}
			else {
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm");
				try {
					Calendar calFrom = WiSeUtils.fromSolrDateStr(dateFrom).toCalendar(Locale.TAIWAN);
					//fix -8 hours when Solr display
					calFrom.add(Calendar.HOUR, +8);
					dateFrom = sdf.format(calFrom.getTime());
				}
				catch (Exception ignored) {}
				
				try {
					Calendar calTo = WiSeUtils.fromSolrDateStr(dateTo).toCalendar(Locale.TAIWAN);
					//fix -8 hours when Solr display
					calTo.add(Calendar.HOUR, +8);
					dateTo = sdf.format(calTo.getTime());
				}
				catch (Exception ignored) {}
				translated = (includeFieldName ? "日期：" : "") + dateFrom + " 至 " + dateTo;
			}
				
		}
	}
	else {
		if(StringUtils.isNotBlank(ColumnNameMappingFacade.getInstance().getMappingName(org))){
			translated = ColumnNameMappingFacade.getInstance().getMappingName(org);
		}
		if (translateMap.containsKey(org))
			translated = translateMap.get(org);
	}
	
    if (StringUtils.equals(translated, org)) {
        String targetField = null;
        String criteria = null;
        
        if (StringUtils.indexOf(org, ":") != -1) {
            	targetField = StringUtils.substringBefore(org, ":");
            	criteria = StringUtils.substringAfter(org, ":");
            	targetField = targetField.replaceAll("^\\{[^}]+\\}", "");
        }
        else {
	        	criteria = org;
        }
        
		if (criteria != null) {
            Matcher m = breadcrumbPattern1.matcher(criteria);
            
            if (m.find()) {
            		criteria = m.group();

                if (criteria != null) {
                		criteria = criteria.replace("(", "");
                    criteria = criteria.replace(")", "");
                }
            }
		}

        if (criteria != null) {
            // shellQuery.removeFilterQuery(eachString); //
            // 單獨去掉一個 fq
            criteria = StringUtils.strip(criteria, "\"");
            
            if (targetField != null) {
            		if ("Tag_ms".equals(targetField)) {
            			if (criteria.startsWith("1")) {
            				translated = (includeFieldName ? "專案代號：" : "") + StringUtils.substringAfterLast(criteria, "/");;
            			}
            			else if (criteria.startsWith("2")) {
            				translated = (includeFieldName ? "標籤分類：" : "") + StringUtils.substringAfterLast(criteria, "/");
            			}
            			else if (criteria.startsWith("3")) {
            				translated = (includeFieldName ? "標籤：" : "") + StringUtils.substringAfterLast(criteria, "/");;
            			}
            		}
            		else {
            			translated = (includeFieldName ? translate(locale, fqs, targetField, true) + "：" : "") + translate(locale, fqs, criteria, false);
            		}
            }
            else {
            		translated = criteria;
            }
        }
    }

	return MessageUtil.returnKeyIfNotExist(locale, translated);
}
%><%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
User user = User.getFromSession(session);
Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, true);
boolean usingApi = false;
long numFound = 0;
String apikey = request.getParameter("apikey");

if (user == null && apikey != null) {
	user = User.getByApiKey(apikey);
	if (user != null) usingApi = true;
}
String basicMakeUrlParams = (usingApi ? "apikey=" + apikey + "&" : "") + "v=" + selectedView;

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
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    
    <jsp:include page="header-analytics.jsp" />
	<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/font-awesome/font-awesome.min.css"></link>
	
	<style>
	.ui-state-default {
		font-size: 0.9em;
		text-align: left;
	}
	
	.cattext {
		color: green;
	}
	
	.ad {
		list-style: none;
		margin: 0;
		padding: 2px 5px 0 6px;
	}
	
	em {
		font-style: normal;
		font-weight: bold;
		color: red;
	}
	
	.text-positive { color: GREEN; }
	.text-negative { color: RED; }
	.text-neutral { color: BLACK; }
	
	.op-title {
		font-size: 1.25em;
	}
	
	.op-title .op-icon {
		font-size: 1.5em;
	}
	
	.op-title span.normal,a:link {
		color: #0000FD;
		TEXT-DECORATION: none;
	}
	
	.op-title span.meta-area {
		font-size: 0.7em;
		color: #FD0000;
		TEXT-DECORATION: none;
		margin-left: 10px;
	}
	
	.op-title a:visited {
		color: #0000FD;
		TEXT-DECORATION: none;
	}
	
	.op-title .keyword {
		font-size: 0.7em;
		color: RED;
		TEXT-DECORATION: none;
	}
	
	.facet-block {
		overflow-x: hidden;
	}
	
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
	
	</style>
	<script>
		function clickSearch(kw) {
			var theForm = $('form.form-search:visible:first');
			theForm.find('input:first').val(kw);
			doSubmit();
		}
		
		function doSubmit() {
			var theForm = $('form.form-search:visible:first')
			if (theForm.find('input:first').val().length > 0) {
				theForm.submit();
			}
			else {
				alert("請輸入查詢關鍵字");
			}
		
			return false;
		}
	
		window.clk=function(u,t){
			//var a=encodeURIComponent||escape;window.location=["<%= request.getContextPath() %>/url.jsp?url=",a(u),"&i=",a(t),"&mp=",mp].join("")
			return true;
		};
		
		$(document).ready(function() {
			//$('.timeago').timeago();
		});
	</script>
</head>
<body class='contrast-fb '>
<header>
    <jsp:include page="header2-analytics.jsp" />
</header>
<div id='wrapper'>

    <div class='container-fluid'>
        <div class='row-fluid' id='content-wrapper'>
            <div class='col-md-12'>

	<table width="100%">
	<tr><td>
		<form class="well form-search">
	        <input id="searchText" type="text" class="search-query inputSearch"
					name="q" value="<%= StringEscapeUtils.escapeHtml(StringUtils.trimToEmpty(request.getParameter("q"))) %>" autocomplete="off" size="60" <%= usingApi ? "readOnly=true" : "" %>>
			<input type="hidden" name="ro" value="true">
			<input type="hidden" name="v" value="<%= selectedView %>" />
			<% if (request.getParameter("d") != null) { %>
			<input type="hidden" name="d" value="<%= request.getParameter("d") %>">
			<% } %>
	        <button type="submit" class="btn btn-primary" onClick="replaceSpecial(); return doSubmit()" <%= usingApi ? "disabled" : "" %>>Search</button>
			<input type="hidden" name="qt" value="dismax">
			<div id="suggest"></div>
			
			<% if (!usingApi) { %>
			<!-- <a class="btn btn-warning" id="btnDownloadExcel">下載報表</a> -->
			<a class="btn btn-info pull-right btnChangeView" data-view="line" title="<bean:message key='analytics.chartType.line'/>"><img src="<%=request.getContextPath()%>/wiseadm/img/chart_line.png" height="14" width="14"></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="bub" title="<bean:message key='analytics.chartType.bubble'/>"><img src="<%=request.getContextPath()%>/wiseadm/img/chart_bubble.png" height="14" width="14"></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="pie" title="<bean:message key='analytics.chartType.pie'/>"><i class="fa fa-pie-chart" aria-hidden="true"></i></span></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="bar" title="<bean:message key='analytics.chartType.topQuestion'/>"><span class="icon-align-left"></span></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="op" title="<bean:message key='analytics.chartType.pivot'/>"><span class="icon-comments"></span></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="hitft" title="<bean:message key='analytics.chartType.wordCloud'/>"><span class="icon-th"></span></a>
			<a class="btn btn-info pull-right btnChangeView" data-view="s" title="<bean:message key='search.view.search'/>"><span class="icon-search"></span></a>
			<% } %>
			
			<%--
			<h6>熱門：
			<%
			List<String> hottestKeywords = SearchKeywordLogFacade.getInstance().listMostPopular(5);
	
			for (Iterator<String> itr = hottestKeywords.iterator(); itr.hasNext(); ) {
				String keyword = itr.next();
				keyword = keyword.trim();
				%><A HREF="#" onclick="clickSearch('<%= keyword %>')"><%= keyword %></A><%= itr.hasNext() ? "、" : "" %>
			<%
			}
			%></h6>
			 --%>
		</form>
	</td></tr>
	</table>
<%
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
    numFound = docs.getNumFound();
    
	Map<String, Long> coreCount = (Map<String, Long>) pageContext.findAttribute("coreCount");
	if (coreCount != null && coreCount.size() > 1)
	{
		%>
		<div id="tabs1" class="hidden-phone">
		<ul class="nav nav-tabs">
		<%
	   		List<DataSet> dsList = DataSetFacade.getInstance().listAllEnabled();
            if ("0".equals(dsId))
           		dsId = "" + dsList.get(0).getId();

			for (DataSet currDs: dsList)
           	{
	       		if (Boolean.FALSE.equals(currDs.getVisible()))
	    			continue;
        	   	if (usingApi && currDs.getId() != new Integer(dsId))
        			continue;

                String tabName = currDs.getName();
               
                Long ccValue = coreCount.get("" + currDs.getId());//iterator.next();
                %>
			<li <%= currDs.getId() == new Integer(dsId) ? "id='current' class='active'" : "" %>>
				<a href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&d=" + currDs.getId() + "&q=" + URLEncoder.encode(query.getQuery(), "UTF-8") ) %>">
					<h4><%= tabName %> (<%= ccValue %>)</h4>
				</a>
			</li>
			<%
		}
		%>
		</ul>
		</div>
		<%
	}
    %>
	<div class="row">
    <div class="col-md-3" >
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
	           	%>
				<H3><%= translate(locale, dsFQs, dsFQs[jjj].getFieldName(), false) %></H3>
	           	<%
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
		                <span class="facet">
				            <a href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&d=" + dsId + "&" + newQuery.toString()) %>"><%= mappedName %> 
		                	<span class="badge"><%= ccc %></span></a>
		                </span><BR>
		                	<%
	                }
	            	}
	            }
	            	%>
	            	<%
	            	if ("Date_dt".equals(dsFQs[jjj].getFieldName())) {
	            	%>
				<a class="btn btn-info btn-mini" id="btnAdvForm"><bean:message key='search.dataField.dateFilter'/></a>
				<%
				}
				%>
            	<BR/><%
			}
            	
		}

		List<FacetField> facetFields = (List<FacetField>) pageContext.findAttribute("facetFieldResults");
        if (facetFields != null)
        {
    		List<String> drillDownFacetField = Arrays.asList(new String[0]);

            for (Iterator<FacetField> iterator = facetFields.iterator(); iterator.hasNext();)
            {
                FacetField facetEnt = iterator.next();

                if (facetEnt.getValues() != null && facetEnt.getValues().size() > 0)
                {

                	boolean isDrillDownMode = drillDownFacetField.contains(facetEnt.getName());
                	%>
    				<H3><%= translate(locale, dsFQs, facetEnt.getName(), false) %></H3>
                  	<%
                  	int count = 0;
		            for (Iterator<FacetField.Count> iterator2 = facetEnt.getValues().iterator(); iterator2.hasNext();)
		            {
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
	                    <span class="facet">
	                    	<a href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&" + newQuery.toString()) %>">
	                    	<%= alreadyChecked ? "<i class='icon-ok'></i>" : "" %>
	                    	<%=translate(locale, dsFQs, qStr, false)%>
	                    	<span class="badge"><%= facetCount.getCount() %></span></a>
	                    </span><BR>
	                    <%
                    }
		            
		            if (count > 10) {
		            	%></div>
		            	<span class="pull-right"><a class="btnMore" data-target="more<%= facetEnt.getName() %>"><span class="icon-fullscreen"></span>&nbsp;more</a>&nbsp;&nbsp;</span>
		            	<%
		            }
                	%><BR><%
                }
            }
        }
	    %>
		<div style="clear: both;width: 10px; height:50px; padding: 0">&nbsp;</div>		
		
		</ol>
	</div>
	<div class="col-md-9">
		<div class="col-md-12">
			<%
			// 麵包屑
			String[] fqs = query.getFilterQueries();
			
			if (!usingApi && fqs != null) {
		        for (String fq : fqs) {
			        	String eachString = StringUtils.strip(fq, "\"()");
			        	eachString = eachString.replaceAll("^\\{[^}]+\\}", "");
	                String meaningfulStr = translate(locale, ds.getFQ(), eachString, true);
	            		SolrQuery queryHistoryQuery = query.getCopy();
				    queryHistoryQuery.setStart(0);
	                queryHistoryQuery.removeFilterQuery(fq); // 標準麵包屑
					%><a href="<%= makeUrl(request, forwardJSP,  basicMakeUrlParams + "&" + queryHistoryQuery.toString()) %>" class="btn btn-primary">
					<%= meaningfulStr %>&nbsp;&nbsp;<i class="icon-remove"></i></a>
					<%
		        }
			}
			%>
		</div>
		<div class="col-md-12" style="margin-top: 10px;">
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
			else if ("op".equals(selectedView)) {
				viewFile = "r-op.jsp";
			}
			else if ("bub".equals(selectedView)) {
				viewFile = "r-bub.jsp";
			}
			else if ("line".equals(selectedView)) {
				viewFile = "r-line.jsp";
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
	1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
	2.	請檢查查詢字有無錯別字。<br />
	3.	請以新的關鍵字或相似詞重新查詢。<br />
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
	<form class="pf-form pform_custom" method="post" action="s-opinion.jsp">
		<div class="pf-element pf-heading">
			<h3><bean:message key='search.dataField.dateFilter'/></h3>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label"><bean:message key='time.range'/></span>
			</label>
			<input class="pf-field" size="20" type="text" name="dateFrom" />
			<input class="pf-field" size="20" type="text" name="dateTo" />
		</div>
		<div class="pf-element pf-buttons pf-centered">
			<input class="pf-button btn btn-primary" type="button" name="doit" value="Submit" />
			<input class="pf-button btn btn-default" type="button" name="cancel" value="Cancel" />
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
	

	$('#btnBlockUrlByQuery').click(function(e) {
		var $this = $(this);	
		
		if (confirm("警告！此動作將刪除共 <%= numFound %> 篇文章（所有頁次，但僅含本頁籤），並且同一網址之文章將不會再收錄至本帳號，確定要刪除？")) {
			if (confirm("再次警告：確定要刪除？")) {
				$.ajax({
					url: 'block-url-by-query.jsp',
					dataType: 'json',
					data: {query: "<%= URLEncoder.encode("d=" + dsId + "&" + query.toString(), "UTF-8") %>"},
					success: function(result) {
						alert("成功刪除" + result.blocked + "筆資料");
					}
				});
			}
		}
	});
	
	$(document).on('click', '.btnChangeView', function(e) {
		var $this = $(this);
		
		var urlMainPart = window.location.href.split('?')[0]

		var params = window.location.search;
		console.log($.query);
		params = params.replace("v="+$.query.get('v'), "v="+$this.attr('data-view'));
		console.log(params);
		
		document.location = urlMainPart + params;
		
	});
});
</script>
</body>
</html>
