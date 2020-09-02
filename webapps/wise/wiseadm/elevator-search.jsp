<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"%>
<%@ page import="javax.servlet.ServletConfig"%>
<%@ page import="javax.servlet.ServletException"%>
<%@ page import="javax.servlet.http.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.net.*"%>
<%@ page import="java.text.*"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.lang.*"%>
<%@ page import="org.apache.commons.httpclient.*"%>
<%@ page import="org.apache.commons.httpclient.methods.*"%>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams"%>
<%@ page import="org.apache.solr.core.*"%>
<%@ page import="org.apache.solr.servlet.*"%>
<%@ page import="org.apache.solr.client.solrj.*"%>
<%@ page import="org.apache.solr.client.solrj.embedded.*"%>
<%@ page import="org.apache.solr.client.solrj.response.*"%>
<%@ page import="org.apache.solr.common.*"%>
<%@ page import="com.intumit.solr.SearchManager"%>
<%@ page import="com.intumit.solr.dataset.*"%>
<%@ page import="com.intumit.solr.searchKeywords.SearchKeywordLogFacade"%>
<%@ page import="com.intumit.solr.util.*"%>
<%@ page import="org.apache.solr.request.*"%>
<%@ page import="com.intumit.solr.admin.*" %>
<%!
	private static final String FACET_RESULTS_JSP = "elevator-search-r.jsp";

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
	private static SolrQuery buildCleanQuery(SolrQuery query, DataSet ds) {
		String filters = ds == null ? "" : (StringUtils.isEmpty(ds.getFilters()) ? "" : ds.getFilters());
		System.out.println("****" + query);
		SolrQuery cleanQuery = new SolrQuery(query.getQuery());
		for (int iii = 0; query.getFilterQueries() != null
				&& iii < query.getFilterQueries().length; iii++) {
			String fq = query.getFilterQueries()[iii].trim();

			if (filters.indexOf(fq) == -1) {
				cleanQuery.addFilterQuery(fq);
			}
		}
		cleanQuery.setParam("sort", query.getSortField());
		cleanQuery.setStart(query.getStart());
		cleanQuery.setRows(query.getRows());
		cleanQuery.setQueryType(query.getQueryType());

		return cleanQuery;
	}
	
	String fieldMap(String code) {
		if ("BODY".equals(code)) {
			return "BODY_mt";
		}
		else if ("TTL".equals(code)) {
			return "Name_t";
		}
		else if ("DESC".equals(code)) {
			return "Description_mt";
		}
		
		return "BODY_mt";
	}
%>
<%
	AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
	if (admGrp == null || admGrp.getDataSetAdminCURD() == 0) {
		return;
	}

	int targetDsId = request.getParameter("d") == null ? DataSetFacade.getInstance().listAllEnabled().get(0).getId() : Integer.parseInt(request.getParameter("d"));
	String dsId = "" + targetDsId;

	int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, Integer.parseInt(dsId));
	if ((acl & GroupDataSet.O4) == 0) return; // 無權限
	
	session.setAttribute("d", dsId);
	ElevatorSet eSet = ElevatorSetFacade.getInstance().get(new Long(request.getParameter("id")));

	int start = request.getParameter("start") == null ? 0 : Integer.parseInt(request.getParameter("start"));
	int facetLimit = request.getParameter("facet.limit") == null ? 10 : Integer.parseInt(request
			.getParameter("facet.limit"));

	String sort = request.getParameter("sort") == null ? "score desc"
			: request.getParameter("sort");
	String[] filterQuery = request.getParameterValues("fq");
	String qqq = eSet.getQuery();//request.getParameter("q");
	if (StringUtils.isNotBlank(qqq)) {
		qqq = qqq.replaceAll("[\\<\\>\\/\\=\\%\\&\\*\\#\\'\\\"\\$\\*\\!,]", "");
	}
	session.setAttribute("qqq", qqq);
	SearchKeywordLogFacade.getInstance().log(qqq, dsId);
	// 打開這裡啟動延伸搜尋  (關掉改成  = new ArrayList<String>(); )
	List<String> relatedWords = SearchKeywordLogFacade.getInstance().listMostPopular(dsId, 5);
	//WiSeUtils.getRelateWikiWords(qqq,		"5ae3c525-3130-11dc-a600-9b4ef35943ed", 40, null);

	String qt = StringUtils.defaultString(request.getParameter("qt"), "dismax");
	int rows = Integer.parseInt(StringUtils.defaultString(request.getParameter("rows"), "15"));

	Map<String, Long> coreCount = new TreeMap<String, Long>();
	QueryResponse mainRsp = null;
	SolrDocumentList docs = null;
	DataSet targetDs = null;
	SolrQuery query = null;
	List<DataSet> dsList = DataSetFacade.getInstance().listAllEnabled();
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	String setDataGroup = MessageUtil.getMessage(locale, "please.set.the.data.group");
	if (dsList.size() == 0) {
		out.println(setDataGroup);
		return;
	}
	if (targetDsId == 0) {
		// 如果 targetDsId == 0, 代表目前沒有指定 ds，那麼就以 dsList 中第一個當作預設 ds
		targetDsId = dsList.get(0).getId();
		session.setAttribute("d", "" + targetDsId);
	}

	int totalCoreCount = 0;
	for (DataSet ds : dsList) {
		SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;
		String qf = "Body_mt";
		String highlightField = "Body_mt";
		
		String[] facetField = StringUtils.isEmpty(ds.getFacets()) ? new String[0] : ds.getFacets().split(",");
		String[] dsFilters = StringUtils.isEmpty(ds.getFilters()) ? new String[0] : ds.getFilters().split(",");
		if (StringUtils.isNotEmpty(ds.getFieldWeight()))
			qf = ds.getFieldWeight();
		if (StringUtils.isNotEmpty(ds.getFieldHighlight()))
			highlightField = ds.getFieldHighlight();
		
		String[] facetQuery = new String[0];

		// 這段是屬於延伸搜尋的 code，利用透過 SmartWiki 取得的相關關鍵字。加入 facetQuery 當中，
		// 用來產生延伸搜尋的效果 (前面的 WiSeUtils.getRelateWikiWords() 要打開）
		String facetQueryStr = "";
        for (int www = 0; www < relatedWords.size(); www++) {
            String word = relatedWords.get(www).trim().replaceAll("[　\\s\\(\\)]+", "_");
            word = word.replaceAll(" ", "");
            word = word.replaceAll("\\\\", "");
            word = word.replaceAll(":", "");
            if (word.length() <= 1)
                continue;
            facetQueryStr += "{!key=__" + word + "}" + word + "\n";
        }
        System.out.println("**[" + relatedWords + "]**");

		if (sort.indexOf(" ") != -1) {
			String sortDir = sort.substring(sort.lastIndexOf(" "))
					.trim();
			sort = sort.substring(0, sort.lastIndexOf(" "));
			sortOrder = SolrQuery.ORDER.valueOf(sortDir);
		}
		
		//使用者可以在前端自行輸入一頁顯示筆數，這邊判斷如果跟系統預設不一樣就以使用者輸入為主
		int rowSize=request.getParameter("rowSize")==null?Integer.parseInt(ds.getDisplayRows()):Integer.parseInt(request.getParameter("rowSize"));
		int DisplayRow=Integer.parseInt(ds.getDisplayRows());
		if(rowSize!=DisplayRow){
			DisplayRow=rowSize;
		}
		request.setAttribute("rowSize",DisplayRow);		
		SolrQuery multiCoreQ = new SolrQuery();
		multiCoreQ.setQuery(qqq)
				.setQueryType(qt)
                .setParam("d", "" + ds.getId())
				.setParam("qf", qf)
				.setFacet(true)
				.setFacetMinCount(1)
				.setParam("enableElevation", false)
				.setParam("forceElevation", false)
				.setHighlight(true)
				.setRows(DisplayRow)
				.setStart(start);

		SolrServer mServer = SearchManager.getServer(ds.getCoreName());
		
		if (ds.getId() != targetDsId) {
			multiCoreQ.setRows(1);
			for (int jjj = 0; filterQuery != null
				&& jjj < filterQuery.length; jjj++) {
				multiCoreQ.addFilterQuery(filterQuery[jjj].trim());
			}
			for (int jjj = 0; dsFilters != null
					&& jjj < dsFilters.length; jjj++) {
				multiCoreQ.addFilterQuery(dsFilters[jjj].trim());
			}
			QueryResponse rsp = mServer.query(multiCoreQ);
			//模糊搜尋功能開關，如果筆數為0，改為模糊搜尋
			if (ds.getFuzzySearch() && ds.getFuzzyField() != null && ds.getFuzzyFieldWeight() != null) { 
				if (rsp.getResults().getNumFound() == 0) {;
					multiCoreQ.setQuery( "" )
					.setQueryType("/mlt")
					.setParam("stream.body", qqq)
					.setParam("mlt.fl", ds.getFuzzyField())
					.setParam("mlt.qf", ds.getFuzzyFieldWeight())
					.setParam("mlt.boost", "true")
					.setParam("mlt.mindf", "1")
					.setParam("mlt.mintf", "1");
					
					rsp = mServer.query(multiCoreQ);
				}
			
			}
			
			coreCount.put("" + ds.getId(), rsp.getResults().getNumFound());
			totalCoreCount += rsp.getResults().getNumFound();
			System.out.println("Time Cost [ds" + ds.getId() + "]:" + rsp.getElapsedTime());
		} 
		else {
			// 在找價錢的時候要先把 filter query 加上 Filter Query

			Set<String> set = new LinkedHashSet<String>();
			for (int jjj = 0; filterQuery != null
					&& jjj < filterQuery.length; jjj++) {
				set.add(filterQuery[jjj].trim());
			}
			for(Object s : set){
				multiCoreQ.addFilterQuery((String)s);
			}
			
			for (int jjj = 0; dsFilters != null
					&& jjj < dsFilters.length; jjj++) {
				multiCoreQ.addFilterQuery(dsFilters[jjj].trim());
			}
			
            com.intumit.solr.dataset.DataSet.FacetQuery[] dsFQs = ds.getFQ();
            for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
            	List<String>fqs = dsFQs[jjj].facetQuries();
            	
            	for (String currFQ : fqs) {
            		multiCoreQ.addFacetQuery(currFQ);
            	}
            }
            
            if (eSet != null & eSet.hasFacetQueryFixedAtTop()) {
            	multiCoreQ.setParam("bq", eSet.facetQueryToBoostQuery() + "^100");
            }
            
            if (StringUtils.isNotEmpty(ds.getQueryBoost())) {
				multiCoreQ.setParam("bf", ds.getQueryBoost());
            }
            
			// 只有目前主要的 core 才要查詢維度等資訊
			for (int jjj = 0; jjj < facetField.length; jjj++) {
				multiCoreQ.addFacetField(facetField[jjj]);
			}
			
			facetQuery = StringUtils.trimToEmpty(facetQueryStr).split("\n");
			for (int jjj = 0; jjj < facetQuery.length; jjj++) {
				if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(facetQuery[jjj])))
					multiCoreQ.addFacetQuery(StringUtils.trimToEmpty(facetQuery[jjj].trim()));
			}
			
			multiCoreQ
					.addSortField(sort, sortOrder)
					.setParam("dontHide", true)
					.setParam("enableElevation", true)
					.setParam("forceElevation", true)
					.setParam("hl.fl", highlightField)
					.setParam("hl.usePhraseHighlighter", true)
					.setParam("hl.fragsize",System.getProperty("searchresult_descmax"))
					.setFacetLimit(facetLimit);
			
			mainRsp = mServer.query(multiCoreQ);
			query = multiCoreQ.getCopy();
			
			//模糊搜尋功能開關，如果筆數為0，改為模糊搜尋
			if (ds.getFuzzySearch() && ds.getFuzzyField() != null && ds.getFuzzyFieldWeight() != null) { 
				if (mainRsp.getResults().getNumFound() == 0) {
					multiCoreQ.setQuery( "" )
					.setQueryType("/mlt")
					.setParam("stream.body", qqq)
					.setParam("mlt.fl", ds.getFuzzyField())
					.setParam("mlt.qf", ds.getFuzzyFieldWeight())
					.setParam("mlt.boost", "true")
					.setParam("mlt.mindf", "1")
					.setParam("mlt.mintf", "1");
					
					mainRsp = mServer.query(multiCoreQ);
				}			
			}
			
			request.setAttribute("QTime",(double)mainRsp.getQTime()/(double)1000);
			docs = mainRsp.getResults();
			coreCount.put("" + ds.getId(), mainRsp.getResults().getNumFound());
			totalCoreCount += mainRsp.getResults().getNumFound();
		}
	}
	

	/*
	if (mainRsp.getResults().getNumFound() <= 3) {
		query.setParam("qf", "BODY_mt");
		mainRsp = SearchManager.getServer(targetDs.getCoreName()).query( query );
		docs = mainRsp.getResults();
		coreCount.put("" + targetDs.getId(), mainRsp.getResults().getNumFound());
	}
	 */

	/* 利用 highlight 的字串來多獲取一些 Wiki 關鍵字, 通常適用於內容不混亂的 content （例如新聞）
	 * 如果是用 html content extract 來的，有時候有不少雜訊，效果可能會不好
	 * 但滿多情況下還算不錯
	if (relatedWords.size() <= 1) {
		String hlStr = "";

		// 這一段是拿所有的 title
		// for (Iterator<SolrDocument> iterator = docs.iterator(); iterator.hasNext(); ) {
		//	SolrDocument result = iterator.next();
		//	hlStr += result.getFirstValue("Title_t") + "\n";
		//}

		// 這一段是拿出所有被 highlight 的文字，並且去除 html tag
		hlStr += mainRsp.getHighlighting().toString().replaceAll("<[^>]+>", "");

		// System.out.println(hlStr);

		List<String> recomKeywords2 = WiSeUtils.getWikiWords(hlStr, "5ae3c525-3130-11dc-a600-9b4ef35943ed", stopwords);
		System.out.println(recomKeywords2);

		for (int www = 0; www < recomKeywords2.size(); www++) {
			String word = recomKeywords2.get(www);
			if (word.length() == 1 || word.equalsIgnoreCase(qqq))
				continue;
			query.addFacetQuery("{!key=__" + word + "}" + word);
		}
		mainRsp = SearchManager.getServer(coreId).query( query );
	}

	// 這裡是利用 WikiKeywordTransformer 取得的所有關鍵字，當作 FacetQuery 再來一次
	FacetField kwFF = mainRsp.getFacetField("Keyword_ms");
	if (kwFF != null && kwFF.getValues() != null) {
		for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
				.iterator(); iterator2.hasNext();) {
			FacetField.Count facetCount = iterator2.next();
			String word = facetCount.getName();
			int ccc = (int) facetCount.getCount();

			if (ccc > 0) {
				if (word.length() == 1 || word.equalsIgnoreCase(qqq))
					continue;
				query.addFacetQuery("{!key=__" + word + "}" + word);
			}
		}
		mainRsp = SearchManager.getServer(coreId).query( query );
	}
	 */

	request.setAttribute("q", buildCleanQuery(query, targetDs));
	request.setAttribute("d", dsId);
	if (eSet != null) request.setAttribute("eSet", eSet);
	request.setAttribute("coreCount", coreCount);
	request.setAttribute("facetQueryResults", mainRsp.getFacetQuery());
	request.setAttribute("facetFieldResults", mainRsp.getFacetFields());
	request.setAttribute("highlights", mainRsp.getHighlighting());
	request.setAttribute("results", docs);
	request.getRequestDispatcher(FACET_RESULTS_JSP).forward(request, response);
%>
