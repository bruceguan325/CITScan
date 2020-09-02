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
	import="org.apache.solr.servlet.*"
	import="org.apache.solr.request.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.qparser.*"
	import="com.intumit.systemconfig.*"
	import="com.intumit.hithot.*"
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

	DataSet ds = (DataSet)session.getAttribute("logDs");
	SolrServer mServer = t.getStatisticsSolrServer(); // SearchManager.getServer(ds.getCoreName());
	String dataField = request.getParameter("df");
	String dataFieldFilter = StringUtils.trimToNull(request.getParameter("dff"));
	boolean hasDataFieldFilter = dataFieldFilter != null;
	String groupBy = StringUtils.trimToNull(request.getParameter("groupBy"));
	int dataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("num"), "10")), 1000);
	String selectedView = StringUtils.defaultString(request.getParameter("v"), "s");
	String selectedTarget = StringUtils.defaultString(request.getParameter("st"), "sl");
	AnalyticsUtil au = new AnalyticsUtil(com.intumit.solr.tenant.Tenant.getFromSession(session), selectedTarget);

	JSONObject result = new JSONObject();

	try {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
		SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
		Set<String> blackFF = new HashSet<String>();
		HashBiMap<String, String> ffToFq = HashBiMap.create(); 
		String ffName = null;
		String tagPrefix = null;

		ffName = au.findFieldNameByParam(dataField);
		SolrQuery secondLevelQueryBase = multiCoreQ.getCopy();
		
		multiCoreQ.addFacetField(ffName);
		multiCoreQ.setRows(0);
		multiCoreQ.setFacetLimit(dataNum).setFacetMinCount(1);
		
		for (String fn: au.getFacets()) {
			multiCoreQ.setParam("f." + fn + ".facet.limit", "" + dataNum);
		}
		
		if (tagPrefix != null) {
			multiCoreQ.setFacetPrefix("Tag_ms", tagPrefix);
		}
		String mainKey = multiCoreQ.getQuery();
		blackFF.add(mainKey);
		
		//System.out.println("*****" + multiCoreQ);
		QueryResponse mainRsp = mServer.query(multiCoreQ);
		JSONArray yAxisFqDataArr = new JSONArray();
		JSONArray seriesArray = new JSONArray();
		Map<String, Integer> dataMap = new HashMap<String, Integer>();

		int id = 1;
		FacetField ff = mainRsp.getFacetField(ffName);
		List<FacetField.Count> ffVal = ff.getValues();
		
		for (FacetField.Count c: ffVal) {
			dataMap.put(c.getName(), (int)c.getCount());
			ffToFq.put(c.getName(), c.getAsFilterQuery());
		}
		
		SortedSet<Map.Entry<String, Integer>> sortedset = new TreeSet<Map.Entry<String, Integer>>(
	            new Comparator<Map.Entry<String, Integer>>() {
	                @Override
	                public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
	                	int result = e1.getValue().compareTo(e2.getValue());	// 從小排到大，然後 highchart 是逆時針畫圓餅圖，因此看起來就會是順時針從大排到小（為了行政院要求順時針）
	                    return result == 0 ? 1 : result;
	                }
	            });

	  	sortedset.addAll(dataMap.entrySet());
		
		for (Map.Entry<String, Integer> c: sortedset) {
			String tagName = c.getKey();
			String fq = tagName;
			
			if (blackFF.contains(tagName))
				continue; 
			
			JSONObject obj = new JSONObject();
			int tagCount = (int)c.getValue();
			obj.put("name", translate(locale, au, null, tagName, false));
			
			JSONArray dataArr = new JSONArray();
			dataArr.put(tagCount);
			if ("json".equals(format)) {
				obj.put("data", dataArr);
			}
			else {
				obj.put("data", tagCount);
			}
			
			yAxisFqDataArr.put(ffToFq.get(fq));
			seriesArray.put(obj);
			
			if (yAxisFqDataArr.length() == dataNum) {
				break;
			}
		}

		result.put("yAxis_fq", yAxisFqDataArr);
		result.put("series", seriesArray);
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
