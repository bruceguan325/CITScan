<%@ include file="/commons/taglib.jsp"%><%@ page 
	pageEncoding="UTF-8" language="java"
	
	import="java.util.regex.Matcher"
	import="java.util.regex.Pattern"
	import="javax.servlet.ServletConfig"
	import="javax.servlet.ServletException"
	import="javax.servlet.http.*"
	import="java.io.*"
	import="java.net.*"
	import="java.text.*"
	import="java.util.*"
	import="java.math.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.httpclient.*"
	import="org.apache.commons.httpclient.methods.*"
	import="org.apache.commons.httpclient.params.HttpMethodParams"
	import="org.apache.solr.core.*"
	import="org.apache.solr.servlet.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
    import="com.intumit.solr.dataset.*"
    import="com.intumit.solr.dataset.DataSet.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.qparser.*"
	import="com.intumit.systemconfig.*" %><%!
	
	private static final String FACET_RESULTS_JSP = "r.jsp";
	private static final String PRICE_FIELD_NAME = "";
	private static final String DATE_FIELD_NAME = "Date_dt";

	HashSet<String> stopwords = new HashSet<String>();
	{
		//stopwords.add("");
	};

	/** 
	 * 做一個乾淨的 Query 物件
	 * 目的為
	 * 1. 怕 URL 過長（這樣 Google AdSense 可能會有 Error）
	 * 2. Facet Fileds, Facet Queries 其實不是給 User 自訂的，不需要包含在 URL 裡頭
	 */
	private static SolrQuery buildCleanQuery(SolrQuery query) {
		SolrQuery cleanQuery = new SolrQuery(query.getQuery());
        for (int iii = 0; query.getFilterQueries() != null
                && iii < query.getFilterQueries().length; iii++) {
            	
            	String fq = query.getFilterQueries()[iii].trim();
            	
            	if (fq.startsWith("{!tag=DF}"))
            		continue;
                cleanQuery.addFilterQuery(fq);
        }
		cleanQuery.setParam("sort", query.getSortField());
		cleanQuery.setStart(query.getStart());
		cleanQuery.setRows(query.getRows());
        cleanQuery.setRequestHandler(query.getRequestHandler());
        
        Set<String> paramNames = query.getParameterNames();
        if (paramNames.contains(ExtendedDismaxQParserPlugin.SEGMENT)) {
        	cleanQuery.setParam(ExtendedDismaxQParserPlugin.SEGMENT, query.getBool(ExtendedDismaxQParserPlugin.SEGMENT));
        }
        if (paramNames.contains(ExtendedDismaxQParserPlugin.SYNONYM)) {
        	cleanQuery.setParam(ExtendedDismaxQParserPlugin.SYNONYM, query.getBool(ExtendedDismaxQParserPlugin.SYNONYM));
        }
        cleanQuery.remove("qt");

		return cleanQuery;
	}

%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getStatisticsAdminCURD()) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	String selectedTarget = StringUtils.defaultString(request.getParameter("st"), "qa");
	AnalyticsUtil au = new AnalyticsUtil(t, selectedTarget);
	String selectedView = StringUtils.defaultString(request.getParameter("v"), au.getDefaultAnalyticType());
	if (!au.getAnalyticTypes().contains(selectedView)) {
		selectedView = au.getDefaultAnalyticType();
	}

	int targetDsId = 1;
	DataSet ds = DataSetFacade.getInstance().get(targetDsId);
	{
		// 為了分析 core-log 又不新增 ds 而做
		ds.setCoreName(au.getTargetCore());
		ds.setFacets(StringUtils.join(au.getFacets(), ","));
		ds.setFieldWeight(au.getFieldWeight());
		ds.setFieldHighlight(au.getFieldHighlight());
		ds.setFilters(au.getAdditionalFQs());
	}
	session.setAttribute("d", "" + targetDsId);
	session.setAttribute("logDs", ds);
	
	String advFilter = null;
	if ("true".equals(request.getParameter("adv"))) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String dateFromStr = StringUtils.trimToNull(request.getParameter("dateFrom"));
		String dateToStr = StringUtils.trimToNull(request.getParameter("dateTo"));
		
		Date dateFrom = null;
		Date dateTo = null;
		
		if (dateFromStr != null) {
			dateFrom = sdf.parse(dateFromStr + " 00:00");
		}
		if (dateToStr != null) {
			dateTo = sdf.parse(dateToStr + " 23:59");
		}
		String dateFilterStr = "Date_dt:[" + (dateFrom == null ? "*" : WiSeUtils.toSolrDateStr(new org.joda.time.DateTime(dateFrom))) 
                + " TO " + (dateTo == null ? "*" : WiSeUtils.toSolrDateStr(new org.joda.time.DateTime(dateTo))) + "]";
		
		advFilter = dateFilterStr;
	}
	
	Map<String, Long> coreCount = new TreeMap<String, Long>();
	QueryResponse mainRsp = null;
	SolrDocumentList docs = null;
	SolrQuery query = null;
	int totalCoreCount = 0;
	SolrServer mServer = t.getStatisticsSolrServer(); //SearchManager.getServer(ds.getCoreName());
	SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), advFilter);
	
	mainRsp = mServer.query(multiCoreQ);
	query = multiCoreQ.getCopy();
	
	request.setAttribute("QTime",(double)mainRsp.getQTime()/(double)1000);
	docs = mainRsp.getResults();
	coreCount.put("" + targetDsId, mainRsp.getResults().getNumFound());
	totalCoreCount += mainRsp.getResults().getNumFound();
	
	request.setAttribute("q", buildCleanQuery(query));
	request.setAttribute("fullQuery", query);
	request.setAttribute("d", "" + targetDsId);
	request.setAttribute("coreCount", coreCount);
	request.setAttribute("facetQueryResults", mainRsp.getFacetQuery());
	request.setAttribute("facetFieldResults", mainRsp.getFacetFields());
	request.setAttribute("highlights", mainRsp.getHighlighting());
	request.setAttribute("results", docs);
	session.setAttribute("output", docs);
	session.setAttribute("selectedView", selectedView);
	session.setAttribute("selectedTarget", selectedTarget);
	session.setAttribute("analyticsUtil", au);
	
	request.getRequestDispatcher(FACET_RESULTS_JSP).forward(request, response);
%>
<%!
//check參數是否為數字
public boolean isNumeric(String str) {
	Pattern pattern = Pattern.compile("[0-9]*");
	Matcher isNum = pattern.matcher(str);
	if( !isNum.matches() ) {
		return false;
	}
	return true;
}
%>
