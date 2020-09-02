<%@ page 
	pageEncoding="UTF-8" language="java"
	import="it.exprivia.cnos.opencloud.*"
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
	import="org.json.*"
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
	import="org.apache.solr.request.*"
	import="com.intumit.hithot.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.util.*"
	import="com.intumit.solr.qparser.*"
	import="com.intumit.systemconfig.*"
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
	
	JSONObject tagToGroupJson(int id, JSONObject tag) {
		try {
			String title = tag.getString("label").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'").replaceAll("[\\n\\r]", "");
			int size = Math.max(tag.getInt("data"), 1);
			JSONObject obj = new JSONObject();
			obj.put("label", title);
			obj.put("weight", size);
			return obj;
		}
		catch (Exception e) {
			return null;
		}
	}
%><%@ include file="translate-map.jsp" %><%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	boolean deep = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("deep"), "false"));
	int targetDsId = request.getParameter("d") == null 
						? (session.getAttribute("d") == null ? 0 : Integer.parseInt(session.getAttribute("d").toString()))
						: Integer.parseInt(request.getParameter("d"));
	DataSet ds = (DataSet)session.getAttribute("logDs");
	JSONObject ftJson = new JSONObject();
	JSONArray firstLvTags = new JSONArray();
	ftJson.put("groups", firstLvTags);
	SolrServer mServer = t.getStatisticsSolrServer(); // SearchManager.getServer(ds.getCoreName());
	String dataField = request.getParameter("df");
	String dataSmoothing = request.getParameter("dsm");
	String groupField = StringUtils.trimToNull(request.getParameter("groupBy"));
	int dataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("num"), "10")), 1000);
	boolean restrictKeywordsUnderSpecificCategory = false;
	
	String selectedView = StringUtils.defaultString(request.getParameter("v"), "s");
	String selectedTarget = StringUtils.defaultString(request.getParameter("st"), "sl");
	AnalyticsUtil au = new AnalyticsUtil(com.intumit.solr.tenant.Tenant.getFromSession(session), selectedTarget);

	try {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
		SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
		multiCoreQ.setRows(0);
		multiCoreQ
			.setFacetLimit(dataNum).setFacetMinCount(1)
			.setParam("f.Source_s.facet.limit", "" + dataNum)
			.setParam("f.Tag_ms.facet.limit", "" + dataNum)
			.setParam("f.UserCategory_ms.facet.limit", "" + dataNum)
			.setParam("f.UserKeyword_ms.facet.limit", "" + dataNum);
		

		String ffName = au.findFieldNameByParam(dataField);
		String tagPrefix = null;
		
		String groupBy = null;
		String groupTagPrefix = null;
		
		if ("none".equals(groupField)) {
			groupBy = ffName;
			ffName = null;
		}
		else {
			groupBy = au.findFieldNameByParam(groupField);
		}
		
		SolrQuery baseQuery = multiCoreQ.getCopy();
		String mainKey = multiCoreQ.getQuery();
		multiCoreQ.addFacetField(groupBy);
		// System.out.println(multiCoreQ);
		QueryResponse mainRsp = mServer.query(multiCoreQ);
		FacetField ff = mainRsp.getFacetField(groupBy);
		
		response.setContentType("application/json");
		Date now = Calendar.getInstance().getTime();
		
		int no = 0;
		
		List<FacetField.Count> ffVal = ff.getValues();
		int id = 1;
		int gapCount = 10;
		int gap = (int)((float)ffVal.size() / ((float)dataNum/(float)gapCount));
		int tagCount = 0;
		
		for (int idx = 0; idx < ffVal.size(); idx++) {//FacetField.Count c: tagList) {
			FacetField.Count c = ffVal.get(idx);
			String tagName = c.getName();
			int ffValPos = idx;
			int ffValCount = (int)c.getCount();
			
			String title = tagName.replaceAll("\"", "\\\\\"").replaceAll("[\\n\\r]", "");
			int size = ffValCount;
			if ("equalDepth".equals(dataSmoothing)) {
				size = (int)((float)(ffVal.size() - ffValPos) / (float)gap);//(int)Math.pow(Math.max(tagCount, 1), 2);
				size = (int)Math.pow(size, 2);
			}
			else if ("sqrt".equals(dataSmoothing)) {
				size = (int)Math.sqrt(ffValCount);
			}
			else if ("power".equals(dataSmoothing)) {
				size = (int)Math.pow(ffValCount, 2);
			}
			
			JSONObject thisTag = new JSONObject();
			thisTag.put("label", translate(locale, au, null, title, false));
			thisTag.put("fq", c.getAsFilterQuery());
			thisTag.put("weight", size);
			firstLvTags.put(thisTag);

			if (ffName != null && deep && size > 0) {
				List<JSONObject> subTagList = new ArrayList<JSONObject>();
				JSONArray subGroupArr = new JSONArray();
				
				SolrQuery subQ = baseQuery.getCopy();
				subQ.addFacetField(ffName);
				subQ.addFilterQuery(c.getAsFilterQuery());
				
				QueryResponse subRsp = mServer.query(subQ);
				FacetField subff = subRsp.getFacetField(ffName);

				for (FacetField.Count subC: subff.getValues()) {
					String subTagName = subC.getName();
					
					if (subTagName.equals(title))
						continue; 
					
					int subValCount = (int)subC.getCount();
					
					JSONObject subTagJson = new JSONObject();
					subTagJson.put("label", translate(locale, au, null, subTagName, false));
					subTagJson.put("fq", subC.getAsFilterQuery());
					subTagJson.put("weight", subValCount);
					
					if (subTagJson != null)
						subGroupArr.put(subTagJson);
				}
				
				if (subGroupArr.length() > 0) {
					thisTag.put("groups", subGroupArr);
				}
			}
		}
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	out.println(ftJson);
%>
