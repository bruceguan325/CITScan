<%@ page 
	pageEncoding="UTF-8" language="java"
	import="it.exprivia.cnos.opencloud.*"
	import="java.net.URLEncoder"
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
	import="org.json.*"
	import="org.apache.struts.Globals"
	import="org.apache.struts.util.MessageResources"
	import="org.apache.struts.util.MessageResourcesFactory"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.httpclient.*"
	import="org.apache.commons.httpclient.methods.*"
	import="org.apache.commons.httpclient.params.HttpMethodParams"
	import="org.apache.solr.core.*"
	import="org.apache.solr.request.*"
	import="org.apache.solr.servlet.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
	import="com.intumit.hithot.*"
	import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.qparser.*"
	import="com.intumit.systemconfig.*"
	import="com.google.common.collect.*"
%><%!
	MessageResources mr = MessageResourcesFactory.createFactory().createResources("messages");

	Comparator nameComparator = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			try {
				FacetField.Count r1 = (FacetField.Count)arg0;
				FacetField.Count r2 = (FacetField.Count)arg1;
				return r1.getName().compareTo(r2.getName());
			}
			catch (Exception e) {
				return -1;
			}
		}};
		
	String tagToGroupXml(int id, JSONObject tag) {
		try {
			String title = tag.getString("label").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'").replaceAll("[\\n\\r]", "");
			int size = Math.max(tag.getInt("data"), 1);
			
			return "	<group id=\"" + title + "\" size=\"" + size + "\">\n"
			 	+ "		<title><phrase>" + title + "</phrase></title>\n"
			 	+ "	</group>\n";
		}
		catch (Exception e) {
			return "";
		}
	}
%><%@ include file="translate-map.jsp" %><% 
response.setHeader("Cache-Control","no-cache"); 
response.setHeader("Pragma","no-cache"); 
response.setDateHeader ("Expires", -1);
%><%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	Locale sessionLocale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
	String format = StringUtils.defaultString(request.getParameter("format"), "json");
	boolean deep = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("deep"), "false"));
	DataSet ds = (DataSet)session.getAttribute("logDs");
	SolrServer mServer = t.getStatisticsSolrServer(); // SearchManager.getServer(ds.getCoreName());
	SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
	String dataField = request.getParameter("df");
	String dataFieldFilter = StringUtils.trimToNull(request.getParameter("dff"));
	boolean hasDataFieldFilter = dataFieldFilter != null;
	String groupBy = StringUtils.trimToNull(request.getParameter("groupBy"));
	String chartType = StringUtils.trimToNull(request.getParameter("chartType"));
	String gap = StringUtils.defaultString(request.getParameter("gap"), "1day");
	String type = StringUtils.defaultString(request.getParameter("type"), "amount");

	String xAxisDataField = request.getParameter("xadf");
	String xAxisDataFieldFilter = StringUtils.trimToNull(request.getParameter("xadff"));
	boolean hasXAxisDataFieldFilter = xAxisDataFieldFilter != null;
	int xAxisDataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("xadn"), "10")), 1000);
	int yAxisDataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("yadn"), "10")), 1000);

	DateFormat dfParser = new SimpleDateFormat("yyyy/MM/dd");
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat df2 = new SimpleDateFormat("MM/dd");

	String selectedView = StringUtils.defaultString(request.getParameter("v"), "s");
	String selectedTarget = StringUtils.defaultString(request.getParameter("st"), "sl");
	AnalyticsUtil au = new AnalyticsUtil(com.intumit.solr.tenant.Tenant.getFromSession(session), selectedTarget);
	
	JSONObject result = new JSONObject();
	
	try {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
		// 鎖定時間區間 （這裡有點 tricky，除非我們另外弄一個時間區間）
		Calendar endDate = Calendar.getInstance();
		Calendar startDate = Calendar.getInstance();
		org.apache.solr.util.DateMathParser dmp = new org.apache.solr.util.DateMathParser(TimeZone.getTimeZone("Asia/Taipei"), Locale.ROOT);
		boolean hasDateFQ = false;
		
		String[] fqs = multiCoreQ.getFilterQueries();
		
		if (fqs != null) {
			for (String fq: fqs) {
				if (StringUtils.startsWith(fq, "Date_dt:")) {
					Pattern dateP = Pattern.compile("Date_dt:\\[([^\\s\\]]*) TO ([^\\s\\]]*)\\]");	
					Matcher dateM = dateP.matcher(fq);
					
					if (dateM.find()) {
						String opDateFrom = dateM.group(1);
						String opDateTo = dateM.group(2);
						
						if (StringUtils.startsWith("NOW", opDateFrom)) {
							startDate.setTime(dmp.parseMath(StringUtils.substringAfter(opDateFrom, "NOW")));
						}
						else {
							startDate = WiSeUtils.fromSolrDateStr(opDateFrom).toCalendar(Locale.US);
						}
						
						if (StringUtils.startsWith("NOW", opDateTo)) {
							endDate.setTime(dmp.parseMath(StringUtils.substringAfter(opDateTo, "NOW")));
						}
						else {
							endDate = WiSeUtils.fromSolrDateStr(opDateTo).toCalendar(Locale.US);
						}
						
						hasDateFQ = true;
					}
				}
			}
		}
		
		if (!hasDateFQ) {
			SolrQuery tmpQ = multiCoreQ.getCopy();
			tmpQ
				.setRows(1)
				.setSort("Date_dt", SolrQuery.ORDER.asc);
			SolrDocumentList docs = mServer.query(tmpQ).getResults();
			if (docs.size() > 0) {
				startDate.setTime((Date)docs.get(0).getFirstValue("Date_dt"));
			}
		}
		
		/*if (request.getParameter("opDateTo") != null) {
			endDate.setTime(dfParser.parse(request.getParameter("opDateTo")));
			WiSeUtils.setCookie("opDateTo", request.getParameter("opDateTo"), -1, null, response);
		}
		
		if (request.getParameter("opDateFrom") != null) {
			startDate.setTime(dfParser.parse(request.getParameter("opDateFrom")));
			WiSeUtils.setCookie("opDateFrom", request.getParameter("opDateFrom"), -1, null, response);
		}
		else {
			startDate.add(Calendar.DATE, -7);
		}*/
		
		// 開始計算所需的X軸區段
		List<String> facetQuery = new ArrayList<String>();
		JSONArray xAxisCategoryDataArr = new JSONArray();
		JSONArray xAxisFqDataArr = new JSONArray();
		JSONArray yAxisFqDataArr = new JSONArray();
		JSONArray seriesArray = new JSONArray();
		String totalDateRange = "Date_dt:[" + WiSeUtils.toSolrDateStr(startDate) + " TO " + WiSeUtils.toSolrDateStr(endDate) + "]";
		
		if ("date".equals(xAxisDataField)) {
			int gapType = Calendar.DATE;
			int gapAmount = 1;
			if ("1day".equals(gap)) {
				
			}
			else if ("3days".equals(gap)) {
				gapAmount = 3;
			}
			else if ("1week".equals(gap)) {
				gapAmount = 1;
				gapType = Calendar.WEEK_OF_YEAR;
			}
			else if ("2weeks".equals(gap)) {
				gapAmount = 2;
				gapType = Calendar.WEEK_OF_YEAR;
			}
			else if ("1month".equals(gap)) {
				gapAmount = 1;
				gapType = Calendar.MONTH;
			}
	
			Calendar rgStart = (Calendar)startDate.clone();
			Calendar rgEnd = (Calendar)startDate.clone();
			rgEnd.add(gapType, gapAmount);
			rgEnd.add(Calendar.SECOND, -1);
			
			for (; rgStart.before(endDate); rgStart.add(gapType, gapAmount), rgEnd.add(gapType, gapAmount)) {
				if (facetQuery.size() > 100) {
					result.put("warning", mr.getMessage(sessionLocale, "search.view.top100"));
					break;
				}
				
				String fq = "Date_dt:[" + WiSeUtils.toSolrDateStr(rgStart) + " TO " + WiSeUtils.toSolrDateStr(rgEnd) + "]";
				facetQuery.add(fq);
				xAxisCategoryDataArr.put(df2.format(rgStart.getTime()));
				xAxisFqDataArr.put(fq);
			}

		}
		else {
			String xAxisFacetName = null;
			String xAxisTagPrefix = null;
			String addtionalFilterForGetXAxis = null;
			Set<String> whiteFF = new HashSet<String>();

			xAxisFacetName = au.findFieldNameByParam(xAxisDataField);
			List<String> xAxisFacetValues = com.intumit.solr.robot.QAUtil.getAllPossibleFacetTerms(SearchManager.getServer(au.getTargetCore()), xAxisFacetName, addtionalFilterForGetXAxis);

			for (String xAxisFacetValue: xAxisFacetValues) {
				if (whiteFF.size() > 0 && !whiteFF.contains(xAxisFacetValue))
					continue;
				String fq = xAxisFacetName + ":" + WiSeUtils.dblQuote(xAxisFacetValue);
				facetQuery.add(fq);
				xAxisCategoryDataArr.put(xAxisFacetValue);
				xAxisFqDataArr.put(fq);
				
				if (xAxisFqDataArr.length() >= xAxisDataNum)
					break;
			}
		}
		// 所需的X軸區段計算完畢，存在 facetQuery 當中

		Set<String> blackFF = new HashSet<String>();
		Set<String> whiteFF = new HashSet<String>();
		String ffName = au.findFieldNameByParam(dataField);;
		String tagPrefix = null;
		
		multiCoreQ.setRows(0);
		multiCoreQ.setFacetLimit(xAxisDataNum).setFacetMinCount(1);
		
		for (String fn: au.getFacets()) {
			multiCoreQ.setParam("f." + fn + ".facet.limit", "" + xAxisDataNum);
		}
		
		SolrQuery secondLevelQueryBase = multiCoreQ.getCopy();
		
		multiCoreQ.addFacetField(ffName);
		multiCoreQ.addFilterQuery(totalDateRange);//"Date_dt:[" + WiSeUtils.toSolrDateStr(startDate) + " TO " + WiSeUtils.toSolrDateStr(endDate) + "]"); // 只計算所需時段之間的目標分析欄位的 Facets

		if (tagPrefix != null) {
			multiCoreQ.setFacetPrefix("Tag_ms", tagPrefix);
		}
		
		String mainKey = multiCoreQ.getQuery();
		blackFF.add(mainKey);
		// System.out.println(multiCoreQ);
		QueryResponse mainRsp = mServer.query(multiCoreQ);
		FacetField ff = mainRsp.getFacetField(ffName);
		List<FacetField.Count> ffVal = ff.getValues();
		int id = 1;
		
		for (FacetField.Count c: ffVal) {
			String tagName = c.getName();
			
			if (blackFF.contains(tagName))
				continue; 
			if (whiteFF.size() > 0 && !whiteFF.contains(tagName))
				continue;
			
			JSONObject obj = new JSONObject();
			obj.put("name", translate(locale, au, null, tagName, false));
			
			String thisFQ = ff.getName() + ":" + WiSeUtils.dblQuote(c.getName());

			// 開始跑每個時間區段
			SolrQuery tmpQ = secondLevelQueryBase.getCopy();
			tmpQ.addFilterQuery(thisFQ);
			
			for (String key: facetQuery) {
				tmpQ.addFacetQuery(key);
			}

			JSONArray dataArr = new JSONArray();
			QueryResponse tmpResp = mServer.query(tmpQ);
			for (String key: facetQuery) {
				int tagCount = 0;
	            if (tmpResp.getFacetQuery().containsKey(key)) {
	            	tagCount = tmpResp.getFacetQuery().get(key).intValue();
	            }
	            dataArr.put(tagCount);
			}
			obj.put("data", dataArr);
			// 時間區段結束
			
			yAxisFqDataArr.put(thisFQ);
			seriesArray.put(obj);
		}
		
		// 總結
		if (chartType.equals("heatmap")) {
			JSONArray seriesArray1 = new JSONArray();
			JSONArray yAxisCategoryDataArr = new JSONArray();
			for (int i = 0; i < seriesArray.length(); i++) {
				JSONObject obj = seriesArray.getJSONObject(i);
				if (obj.has("name") && obj.has("data")) {
					yAxisCategoryDataArr.put(obj.get("name"));
					JSONArray arr = obj.getJSONArray("data");
					for (int j = 0; j < arr.length(); j++) {
						JSONObject dataObj = new JSONObject();
						dataObj.put("x", j);
						dataObj.put("y", i);
						dataObj.put("value", arr.get(j));
						seriesArray1.put(dataObj);
					}
				}
			}
			result.put("xAxis_category", xAxisCategoryDataArr);
			result.put("yAxis_category", yAxisCategoryDataArr);
			result.put("xAxis_fq", xAxisFqDataArr);
			result.put("yAxis_fq", yAxisFqDataArr);
			result.put("series", seriesArray1);
			
		} else {
			result.put("xAxis_category", xAxisCategoryDataArr);
			result.put("xAxis_fq", xAxisFqDataArr);
			result.put("yAxis_fq", yAxisFqDataArr);
			result.put("series", seriesArray);
		}
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
 	
	if ("json".equals(format)) {
		response.setContentType("application/json");%><%= result.toString() %><% 
	} else { 
		//response.setContentType("text/plain");
		response.setContentType("application/octet-stream; charset=ms950");
		response.setHeader("Content-Disposition", "attachment;filename=\"" + "export" + ".csv\"");
		%><%= CDL.toString(result.getJSONArray("series")) %>
		<% 
	} 
	%>
	
