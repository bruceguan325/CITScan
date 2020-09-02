<%@ page 
	pageEncoding="UTF-8" language="java" contentType="application/json"
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
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
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
%><%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	JSONObject result = new JSONObject();
	JSONArray kpArr = new JSONArray();
	result.put("kpArr", kpArr);
	result.put("weightFactor", 1);
	
	SolrServer mServer = t.getCoreServer(); 
	int dataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("num"), "30")), 1000);
	boolean restrictKeywordsUnderSpecificCategory = false;

	try {
		DataSet ds = new DataSet();
		ds.setFacetQueries("");
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
		SolrQuery sqry = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
		sqry.setRows(0);
		sqry.setRequestHandler("/browse");
		sqry.setFacetLimit(dataNum).setFacetMinCount(1);
		sqry.setParam("qf", "QUESTION_t^1000000 QUESTION_ALT_mt^0.01");
		sqry.setParam("mm", "1");
		sqry.setParam("fuzzy", false);
		sqry.setQuery("*:*");
	    int start = 0;

		sqry.setFacet(true);
		sqry.setFacetLimit(dataNum);
		sqry.setFacetMinCount(1);
		sqry.addFilterQuery("{!tag=DF}dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
		sqry.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");

		Set<String> kps = DictionaryDatabase.getDictionary(t.getId(), DictionaryDatabase.Purpose.KNOWLEDGE_POINT);
		for (String kp: kps) {
			sqry.addFacetQuery("{!key=__" + kp.replaceAll("\\s+", "_") + "}QUESTION_t:" + WiSeUtils.dblQuote(StringUtils.trim(kp)));
		}

		if (sqry.getFields() == null) {
			sqry.setFields(new String[] {"id", "QUESTION_t", "score"});
		}

		System.out.println(sqry);
		QueryResponse resp = mServer.query(sqry);
		Map<String, Integer> facetQueryResults = resp.getFacetQuery();
		float max = 1;

		for (String key: facetQueryResults.keySet()) {
			if (key.startsWith("__")) {
				int count = facetQueryResults.get(key);
				if (count == 0) continue;
				
				max = Math.max(max, count);
				kpArr.put(new JSONArray().put(key.substring(2).replaceAll("_", " ")).put(count));			
			}
		}
		
		result.put("weightFactor", Math.ceil(50f / max));
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	out.println(result.toString(2));
%>
