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
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"
	import="com.intumit.solr.servlet.*" 
	import="com.intumit.solr.util.*"
	import="com.intumit.systemconfig.*"
	import="com.intumit.hithot.*"
	import="com.google.common.collect.*"
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
	
%><% 
response.setHeader("Cache-Control","no-cache"); 
response.setHeader("Pragma","no-cache"); 
response.setDateHeader ("Expires", -1);
%><%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	String format = StringUtils.defaultString(request.getParameter("format"), "json");
	int targetDsId = request.getParameter("d") == null 
						? (session.getAttribute("d") == null ? 0 : Integer.parseInt(session.getAttribute("d").toString()))
						: Integer.parseInt(request.getParameter("d"));
	DataSet ds = (DataSet)session.getAttribute("logDs");
	SolrServer mServer = t.getStatisticsSolrServer(); // SearchManager.getServer(ds.getCoreName());
	String chartType = request.getParameter("chartType");
	String dataField = request.getParameter("df");
	String dataFieldFilter = StringUtils.trimToNull(request.getParameter("dff"));
	boolean hasDataFieldFilter = dataFieldFilter != null;
	String groupBy = StringUtils.trimToNull(request.getParameter("groupBy"));
	int dataNum = Math.min(Integer.parseInt(StringUtils.defaultString(request.getParameter("num"), "10")), 1000);
	String selectedView = StringUtils.defaultString(request.getParameter("v"), "s");
	String selectedTarget = StringUtils.defaultString(request.getParameter("st"), "qa");
	AnalyticsUtil au = new AnalyticsUtil(com.intumit.solr.tenant.Tenant.getFromSession(session), selectedTarget);

	JSONObject result = new JSONObject();
	JSONArray fqDataArr = new JSONArray();
	JSONArray seriesArray = new JSONArray();
	
	try {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
		SolrQuery multiCoreQ = WiSeUtils.parseUrlSearchParameters(ds, true, request.getParameterMap(), null);
		HashBiMap<String, String> ffToFq = HashBiMap.create(); 
		String ffName = au.findFieldNameByParam(dataField);
		String tagPrefix = null;
		SolrQuery ffQuery = multiCoreQ.getCopy();
		
		ffQuery.setRows(0)
		       .addFacetField(ffName)
		       .setFacetLimit(dataNum).setFacetMinCount(1)
		       .setParam("f.Source_s.facet.limit", Integer.toString(dataNum))
		       .setParam("f.Tag_ms.facet.limit", Integer.toString(dataNum))
		       .setParam("f.UserCategory_ms.facet.limit", Integer.toString(dataNum))
		       .setParam("f.UserKeyword_ms.facet.limit", Integer.toString(dataNum));
		
		if (tagPrefix != null) {
			ffQuery.setFacetPrefix("Tag_ms", tagPrefix);
		}
		
		QueryResponse mainRsp = mServer.query(ffQuery);
		Map<String, Integer> dataMap = new HashMap<String, Integer>();
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
	            	int result = e2.getValue().compareTo(e1.getValue());
	                return result == 0 ? 1 : result;
	            }
			}
		);
		
		if ("positiveAndNegative".equals(chartType)) {
			
			for (FacetField.Count c: ffVal) {
				String tagName = c.getName();
				
				JSONObject obj = new JSONObject();
				JSONArray dataArr = new JSONArray();
				JSONObject dataObj = new JSONObject();
				// init
				dataObj.put("x", 0);
				dataObj.put("y", 0);
				dataObj.put("z", c.getCount());
				dataObj.put("name", tagName);
				
				SolrQuery fqQuery = multiCoreQ.getCopy();
				fqQuery.setRows(0)
				       .addFilterQuery(ffName + ":" + c.getName());
				
				QueryResponse rsp = mServer.query(fqQuery);
				FacetField ffOpinion = rsp.getFacetField("ClassifiedOpinion_s");
				for (FacetField.Count op: ffOpinion.getValues()) {
					if ("POSITIVE".equalsIgnoreCase(op.getName())) {
						dataObj.put("x", op.getCount());
					}
					if ("NEGATIVE".equalsIgnoreCase(op.getName())) {
						dataObj.put("y", op.getCount());
					}
				}
				dataArr.put(dataObj);
				obj.put("data", dataArr);
				obj.put("name", tagName);
				seriesArray.put(obj);
				
				fqDataArr.put(ffToFq.get(c.getName()));
				
				result.put("fq", fqDataArr);
				result.put("series", seriesArray);
			}
		}
		else if ("coverage".equals(chartType)) {
				
			for (FacetField.Count c: ffVal) {
				String tagName = c.getName();
				
				JSONObject obj = new JSONObject();
				JSONArray dataArr = new JSONArray();
				JSONObject dataObj = new JSONObject();
				// init
				dataObj.put("x", 0);
				dataObj.put("y", 0);
				dataObj.put("z", c.getCount());
				dataObj.put("name", tagName);
				long positiveNum = 0;
				long negativeNum = 0;
				
				SolrQuery fqQuery = multiCoreQ.getCopy();
				fqQuery.setRows(0)
				       .addFilterQuery(ffName + ":" + c.getName());
				
				QueryResponse rsp = mServer.query(fqQuery);
				FacetField ffOpinion = rsp.getFacetField("AnswerType_s");
				for (FacetField.Count op: ffOpinion.getValues()) {
					if (QAContext.ANSWER_TYPE.NO_ANSWER.name().equalsIgnoreCase(op.getName())) {
						if (op.getCount() > 0) {
							negativeNum = op.getCount();
						}
					}
					else {
						if (op.getCount() > 0) {
							positiveNum += op.getCount();
						}
					}
				}
				dataObj.put("x", positiveNum);
				dataObj.put("y", negativeNum);
				dataObj.put("z", positiveNum + negativeNum);
				dataArr.put(dataObj);
				obj.put("data", dataArr);
				obj.put("name", tagName);
				seriesArray.put(obj);
				
				fqDataArr.put(ffToFq.get(c.getName()));
				
				result.put("fq", fqDataArr);
				result.put("series", seriesArray);
			}
		}
		else if ("hotAndCoverage".equals(chartType)) {
			JSONObject obj1 = new JSONObject();
			JSONObject obj2 = new JSONObject();
			JSONArray dataArr1 = new JSONArray();
			JSONArray dataArr2 = new JSONArray();

			for (FacetField.Count c: ffVal) {
				String tagName = c.getName();
				
				JSONObject obj = new JSONObject();
				JSONArray dataArr = new JSONArray();
				JSONObject dataObj = new JSONObject();
				// init
				dataObj.put("name", tagName);
				dataObj.put("x", 0);
				dataObj.put("y", 0);
				// dataObj.put("z", 1);
				long positiveNum = 0;
				long negativeNum = 0;
				
				SolrQuery fqQuery = multiCoreQ.getCopy();
				fqQuery.setRows(0)
				       .addFilterQuery(ffName + ":" + c.getName());
				
				QueryResponse rsp = mServer.query(fqQuery);
				FacetField ffOpinion = rsp.getFacetField("AnswerType_s");
				for (FacetField.Count op: ffOpinion.getValues()) {
					if (QAContext.ANSWER_TYPE.NO_ANSWER.name().equalsIgnoreCase(op.getName())) {
						if (op.getCount() > 0) {
							negativeNum = op.getCount();
						}
					}
					else {
						if (op.getCount() > 0) {
							positiveNum += op.getCount();
						}
					}
				}
				double opinionProp = (Math.max(positiveNum, 1) * 1.0 / Math.max(positiveNum+negativeNum, 1));
				//opinionProp = Math.log10(opinionProp);
				dataObj.put("x", positiveNum + negativeNum);
				dataObj.put("y", opinionProp);
				
				dataArr.put(dataObj);
				obj.put("data", dataArr);
				obj.put("name", tagName);
				
				if (opinionProp > 0.95) {
					obj.put("color", "rgba(190, 218, 246, 1)");
				} 
				else if (opinionProp > 0.5) {
					obj.put("color", "rgba(206, 150, 164, 1)");
				}
				else {
					obj.put("color", "rgba(223, 83, 83, 1)");
				}
				seriesArray.put(obj);
				
				fqDataArr.put(ffToFq.get(c.getName()));
				
				result.put("fq", fqDataArr);
				result.put("series", seriesArray);
			}
		}
		else if ("soundAndEmotion".equals(chartType)) {
			JSONObject obj1 = new JSONObject();
			JSONObject obj2 = new JSONObject();
			JSONArray dataArr1 = new JSONArray();
			JSONArray dataArr2 = new JSONArray();

			for (FacetField.Count c: ffVal) {
				String tagName = c.getName();
				
				JSONObject obj = new JSONObject();
				JSONArray dataArr = new JSONArray();
				JSONObject dataObj = new JSONObject();
				// init
				dataObj.put("name", tagName);
				dataObj.put("x", c.getCount());
				dataObj.put("y", 0);
				// dataObj.put("z", 1);
				long positiveNum = 1;
				long negativeNum = 1;
				
				SolrQuery fqQuery = multiCoreQ.getCopy();
				fqQuery.setRows(0)
				       .addFilterQuery(ffName + ":" + c.getName());
				
				QueryResponse rsp = mServer.query(fqQuery);
				FacetField ffOpinion = rsp.getFacetField("ClassifiedOpinion_s");
				for (FacetField.Count op: ffOpinion.getValues()) {
					if ("POSITIVE".equalsIgnoreCase(op.getName())) {
						if (op.getCount() > 0) {
							positiveNum = op.getCount();
						}
					}
					if ("NEGATIVE".equalsIgnoreCase(op.getName())) {
						if (op.getCount() > 0) {
							negativeNum = op.getCount();
						}
					}
				}
				double opinionProp = (positiveNum * 1.0 / negativeNum);
				opinionProp = Math.log10(opinionProp);
				dataObj.put("y", opinionProp);
				
				dataArr.put(dataObj);
				obj.put("data", dataArr);
				obj.put("name", tagName);
				
				if (opinionProp > 0) {
					obj.put("color", "rgba(190, 218, 246, 1)");
				} 
				else if (opinionProp == 0) {
					obj.put("color", "rgba(206, 150, 164, 1)");
				}
				else {
					obj.put("color", "rgba(223, 83, 83, 1)");
				}
				seriesArray.put(obj);
				
				fqDataArr.put(ffToFq.get(c.getName()));
				
				result.put("fq", fqDataArr);
				result.put("series", seriesArray);
			}
		}
	} catch (Exception ex) {
		ex.printStackTrace();
	}
System.out.println(result);
%><%= result %>