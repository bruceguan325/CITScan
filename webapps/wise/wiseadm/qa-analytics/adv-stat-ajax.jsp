<%@ page 
	pageEncoding="UTF-8" language="java"
	import="org.apache.commons.lang.StringUtils"
	import="it.exprivia.cnos.opencloud.*"
	import="java.net.URLEncoder"
	import="java.util.*"
	import="org.json.*"
	import="org.apache.commons.lang.*"
	import="java.util.*"
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
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
	import="org.apache.solr.core.*"
	import="org.apache.solr.request.*"
	import="org.apache.solr.servlet.*"
	import="com.intumit.hithot.*"
	import="com.intumit.solr.user.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.user.*"
	import="com.intumit.solr.qparser.*"
	import="com.intumit.systemconfig.*"
	import="com.google.common.collect.*"
	import="com.intumit.solr.admin.*"
%>
<%
JSONObject result = new JSONObject();
if((AdminGroupFacade.getInstance().getFromSession(session).getStatisticsAdminCURD() & AdminGroup.R) == 0){
	result.put("status", "denied");
	response.setContentType("application/json");
	out.println(result);
	return;
}
%><%!
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
%>
<%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	String format = StringUtils.defaultString(request.getParameter("format"), "json");
	boolean deep = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("deep"), "false"));
	HitHotLocale locale = HitHotLocale.zh_TW;
	User user = User.getFromSession(session);
	DataSet ds = (DataSet)session.getAttribute("logDs");
	SolrServer mServer = t.getStatisticsSolrServer(); // SearchManager.getServer(ds.getCoreName());
	String dataField = request.getParameter("df");
	String dataFieldFilter = StringUtils.trimToNull(request.getParameter("dff"));
	boolean hasDataFieldFilter = dataFieldFilter != null;
	String groupBy = StringUtils.trimToNull(request.getParameter("groupBy"));
	int dataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("num"), "10")), 1000);
	//JSONObject result = new JSONObject();

	try {
		SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
		Set<String> blackFF = new HashSet<String>();
		HashBiMap<String, String> ffToFq = HashBiMap.create(); 
		String ffName = "QuestionType_s";
		String tagPrefix = null;
		
		if ("questionType".equals(dataField)) {
			ffName = "QuestionType_s";
		}
		else if ("questionCategory".equals(dataField)) {
			ffName = "QuestionCategory_ms";
		}
		else if ("questionKeyword".equals(dataField)) {
			ffName = "QuestionKeyword_ms";
		}
		else if ("matchedQuestion".equals(dataField)) {
			ffName = "MatchedQuestion_s";
		}
		else if ("kp".equals(dataField)) {
			ffName = "KnowledgePoint_ms";
		}
		else if ("confidence".equals(dataField)) {
			ffName = "Confidence_s";
		}
		else if ("answerType".equals(dataField)) {
			ffName = "AnswerType_s";
		}
		else if ("tenant".equals(dataField)) {
			ffName = "TenantId_i";
		}
		else if ("identity".equals(dataField)) {
			ffName = "Identity_s";
		}
		else if ("opinion".equals(dataField)) {
			ffName = "ClassifiedOpinion_s";
		}
		else if ("eservice".equals(dataField)) {
			ffName = "Eservice_s";
		}
		else if ("questionTag".equals(dataField)) {
			ffName = "QuestionTag_ms";
		}
		else if ("userType".equals(dataField)) {
			ffName = "UserType_s";
		}
		
		SolrQuery secondLevelQueryBase = multiCoreQ.getCopy();

		multiCoreQ.addFacetField(ffName);
		multiCoreQ.setRows(0);
		multiCoreQ
				.setFacetLimit(dataNum).setFacetMinCount(1)
				.setParam("f.QuestionType_s.facet.limit", "" + dataNum)
				.setParam("f.AnswerType_s.facet.limit", "" + dataNum)
				.setParam("f.QuestionKeyword_ms.facet.limit", "" + dataNum)
				.setParam("f.AnswerCategory_s.facet.limit", "" + dataNum);
		
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
	                public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
	                		int result = e2.getValue().compareTo(e1.getValue());
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
			obj.put("name", tagName);
			
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
