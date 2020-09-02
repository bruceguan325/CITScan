package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.intumit.android.search.util.ChineseHelper;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.blackKeywords.BlackKeyword;
import com.intumit.solr.blackKeywords.BlackKeywordFacade;
import com.intumit.solr.dataset.DataSet;
import com.intumit.solr.dataset.DataSet.FacetQuery;
import com.intumit.solr.dataset.DataSetFacade;
import com.intumit.solr.dataset.ElevatorSet;
import com.intumit.solr.dataset.ElevatorSetFacade;
import com.intumit.solr.qparser.ExtendedDismaxQParserPlugin;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.util.StringUtil;


public class GroupQueryServlet extends QueryServlet {

    @Override
    String getQueryType() {
    	return "/browse";
    }
    

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException {

        try {
            boolean usejson = "json".equalsIgnoreCase(req.getParameter("format"));

            String[] finq = req.getParameterValues("finq"); // 在結果中查詢
            String[] foutq = req.getParameterValues("foutq"); // 在結果中排除
            
            // 是否強迫啟動（或強迫關閉）模糊搜尋
            Boolean forceEnableOrDisableFuzzy = req.getParameter("fuzzy") == null ? null : new Boolean((String) req.getParameter("fuzzy"));
            Boolean forceEnableOrDisableSyn = req.getParameter("syn") == null ? null : new Boolean((String) req.getParameter("syn"));
            
            String qf = req.getParameter("qf");
            String groupBy = req.getParameter("groupBy");
            int groupSize = Integer.parseInt(StringUtils.defaultString(req.getParameter("groupSize"), "10"));
            boolean forceQueryField = qf != null;
            String[] fields = req.getParameter("fl") != null ? req.getParameter("fl").split(",") : null;
            String qqq = StringUtils.trimToEmpty(req.getParameter("q"));
            
            /*List<String> fuzzyTerms = getFuzzyTerms(qqq);
            if (fuzzyTerms.size() > 0) {
            	qqq = StringUtils.join(fuzzyTerms, " ");
            	System.out.println("New Query: " + qqq);
            }*/
            
            // 判斷參數是否非數字
            int start = 0;
            String preStart = StringUtils.defaultString(req.getParameter("start"), "0");
            if (isNumeric(preStart)) {
                start = Math.max(0, Integer.parseInt(preStart));
            }

            int facetLimit = Integer.parseInt(StringUtils.defaultString(req.getParameter("facet.limit"), "20"));
            
            String[] filterQuery = req.getParameterValues("fq");

            // 打開這裡啟動延伸搜尋 (關掉改成 = new ArrayList<String>(); )
            List<String> relatedWords = new ArrayList<String>();
                /*WiSeUtils.getRelateWikiWords(
                qqq,
                "5ae3c525-3130-11dc-a600-9b4ef35943ed",
                40,
                null);*/

            QueryResponse rsp = null;
            SolrQuery query = null;
            List<DataSet> dsList = DataSetFacade.getInstance().listAllEnabled();
            if (dsList.size() == 0) {
                // out.println("請先設定資料組");
                return;
            }
            

            // 資料組
            int targetDsId = Integer.parseInt(StringUtils.defaultString(req.getParameter("d"), "0"));
            if (targetDsId == 0) {
                // 如果 targetDsId == 0, 代表目前沒有指定 ds，那麼就以 dsList 中第一個當作預設 ds

                for (DataSet ds : dsList) {
                	if (!Boolean.FALSE.equals(ds.getVisible())) {
                		targetDsId = ds.getId();
                		break;
                	}
                }
                
                if (targetDsId == 0)
                	targetDsId = dsList.get(0).getId();
            }
    		ElevatorSet eSet = ElevatorSetFacade.getInstance().get("" + targetDsId, qqq);

            Document xmlDocument = DocumentHelper.createDocument();
            Element root = xmlDocument.addElement("page");
            JSONObject jsonDoc = new JSONObject();
            JSONArray jsonRoot = new JSONArray();
            jsonDoc.put("page", jsonRoot);
            
            // 依照資料組來搜尋
            for (DataSet ds : dsList) {
                // if not target dataset && is invisible, skip it.
                if (Boolean.FALSE.equals(ds.getVisible()) && ds.getId() != targetDsId) {
                    continue;
                }

                // 使用者可以在前端自行輸入一頁顯示筆數，這邊判斷如果跟系統預設不一樣就以使用者輸入為主
                int rows = req.getParameter("rows") == null
                    ? Integer.parseInt(ds.getDisplayRows())
                    : Integer.parseInt(req.getParameter("rows"));

                String[] facetField = StringUtils.isEmpty(ds.getFacets())
                    ? new String[0]
                    : ds.getFacets().split(",");

                String[] dsFilters = StringUtils.isEmpty(ds.getFilters())
                    ? new String[0]
                    : ds.getFilters().split(",");

                // if (StringUtils.isNotEmpty(ds.getFieldWeight()))
                // qf = ds.getFieldWeight();

                // if (StringUtils.isNotEmpty(ds.getFieldHighlight()))
                // highlightField = ds.getFieldHighlight();

                String priceFaceQuery = "";

                String[] facetQuery = new String[0];

                // 這段是屬於延伸搜尋的 code，利用透過 SmartWiki 取得的相關關鍵字。加入 facetQuery 當中，
                // 用來產生延伸搜尋的效果 (前面的 WiSeUtils.getRelateWikiWords() 要打開）
                String facetQueryStr = "";

                for (int www = 0; www < relatedWords.size(); www++) {
                    String word = relatedWords.get(www);
                    if (word.length() <= 1)
                        continue;
                    facetQueryStr += "{!key=__" + word + "}" + word + "\n";
                }
                // System.out.println("**[" + relatedWords + "]**");

                SolrQuery multiCoreQ = new SolrQuery();
                HashSet<String>blackList = loadBlackList();
                boolean isBlackKeyword = blackList.contains(qqq);
                
                if (!isBlackKeyword) { 
	                multiCoreQ.setQuery(qqq)
	                    .setRequestHandler(getQueryType())
	                    .setParam("d", "" + ds.getId())
	                    .setParam("qf", forceQueryField ? qf : ds.getFieldWeight())
	                    .setParam("mm", "1")
	                    .setFacet(true)
	                    .setFacetMinCount(1)
	    				.setParam("enableElevation", false)
	    				.setParam("forceElevation", false)
	                    .setHighlight(true)
	                    .setRows(rows)
	                    .setStart(start);
                }
                else {
	                multiCoreQ.setQuery("AKEYWORDNEVERGOTBEFOUNDINDOCUMENT")
	                    .setRequestHandler(getQueryType())
	                    .setParam("d", "" + ds.getId())
	                    .setParam("qf", forceQueryField ? qf : ds.getFieldWeight())
	                    .setFacet(true)
	                    .setFacetMinCount(1)
	    				.setParam("enableElevation", false)
	    				.setParam("forceElevation", false)
	                    .setHighlight(true)
	                    .setRows(rows)
	                    .setStart(start);
                }

                // 過濾重複名稱的 fq
                Set<String> filterQuerySet = new LinkedHashSet<String>();
                if (filterQuery != null) {
                    filterQuerySet.addAll(Arrays.asList(filterQuery));
                }

                // 在結果中查詢
                if (finq != null) {
                	for (int i=0; i < finq.length; i++) {
	                	if (StringUtils.indexOf(finq[i], ":") != -1)
	                		filterQuerySet.add("(" + finq[i] + ")");
	                	else
	                		filterQuerySet.add("(FilterResult_mt:" + finq[i] + ")");
                	}
                }
                // 在結果中排除
                if (foutq != null) {
                	for (int i=0; i < foutq.length; i++) {
	                	if (StringUtils.indexOf(foutq[i], ":") != -1)
	                		filterQuerySet.add("(NOT " + foutq[i] + ")");
	                	else
	                		filterQuerySet.add("(NOT FilterResult_mt:" + foutq[i] + ")");
	            	}
                }
                if (forceEnableOrDisableFuzzy != null) {
                	multiCoreQ.setParam(ExtendedDismaxQParserPlugin.SEGMENT, forceEnableOrDisableFuzzy ? true : false);
                }
                if (forceEnableOrDisableSyn != null) {
                	multiCoreQ.setParam(ExtendedDismaxQParserPlugin.SYNONYM, forceEnableOrDisableSyn ? true : false);
                }

                // 設定 fq
                for (String filter : filterQuerySet) {
                    multiCoreQ.addFilterQuery(filter);
                }

                // core 設定的過濾條件
                for (int jjj = 0; dsFilters != null && jjj < dsFilters.length; jjj++) {
                    multiCoreQ.addFilterQuery("{!tag=DF}" + dsFilters[jjj].trim());
                }

                SolrServer mServer = SearchManager.getServer(ds.getCoreName());

                // 不是主要 core 只需要筆數
                if (ds.getId() != targetDsId) {
                    multiCoreQ.setRows(1);
                }
                else {
                	// 只有目前主要的 core 才要查詢維度等資訊
                    for (int jjj = 0; jjj < facetField.length; jjj++) {
                        multiCoreQ.addFacetField(facetField[jjj]);
                    }
        			facetQuery = StringUtils.trimToEmpty(facetQueryStr).split("\n");
        			for (int jjj = 0; jjj < facetQuery.length; jjj++) {
        				if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(facetQuery[jjj])))
        					multiCoreQ.addFacetQuery(StringUtils.trimToEmpty(facetQuery[jjj].trim()));
        			}
                    FacetQuery[] dsFQs = ds.getFQ();
                    for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
                    	List<String>fqs = dsFQs[jjj].facetQuries();
                    	
                    	for (String currFQ : fqs) {
                    		multiCoreQ.addFacetQuery(currFQ);
                    	}
                    }

                    if (StringUtils.isNotEmpty(ds.getQueryBoost())) {
        				multiCoreQ.setParam("bf", ds.getQueryBoost());
                    }

                    // 處理 SORT 
                    String sortStr = req.getParameter("sort") == null
                        ? "score desc"
                        : req.getParameter("sort");

                    StringTokenizer st = new StringTokenizer(sortStr, ",");
                    while (st.hasMoreTokens()) {
                    	String sort = st.nextToken();
                        SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;
                    	
	                    if (sort.indexOf(" ") != -1) {
	                        String sortDir = sort.substring(sort.lastIndexOf(" "))
	                            .trim();
	                        sort = sort.substring(0, sort.lastIndexOf(" "));
	                        sortOrder = SolrQuery.ORDER.valueOf(sortDir);
	                    }
	                    
	                    multiCoreQ.addSortField(sort, sortOrder);
                    }

                	String qqq2 = StringUtil.trim(qqq, '"');
                    if (eSet != null && eSet.hasFacetQueryFixedAtTop()) {
                    	multiCoreQ.setParam("bq", eSet.facetQueryToBoostQuery() + "^100 "
                    			+ "\"" + qqq2 + "\"^3");
                    }
                    else {
                        multiCoreQ.setParam("bq", "\"" + qqq2 + "\"^3");
                    }
                    
                    if (fields != null) {
                    	multiCoreQ.setFields(fields);
                    }
            		
                    multiCoreQ
						.setParam("enableElevation", true)
						.setParam("forceElevation", true)
						.setParam("group", true)
						.setParam("group.field", groupBy)
						.setParam("group.limit", "" + groupSize)
						.setParam("pf", ds.getFieldWeight())
						.setParam("pf2", ds.getFieldWeight())
						.setParam("qs", "2")
                        .setParam("hl.fl", ds.getFieldHighlight())
                        .setParam("hl.usePhraseHighlighter", true)
                        .setParam(
                            "hl.fragsize",
                            System.getProperty("searchresult_descmax"))
                        .setFacetLimit(facetLimit);
                }

                // 執行 query
                query = multiCoreQ;
                rsp = mServer.query(multiCoreQ);

				// 如果筆數為0，改為模糊搜尋
				/*if (rsp.getResults().getNumFound() == 0 && !isBlackKeyword) {
					if ((fuzzy == null && ds.getFuzzySearch() != null && ds.getFuzzySearch())
							|| new Boolean(fuzzy)) { //模糊搜尋功能開關
						SolrQuery mltQuery = multiCoreQ.getCopy();
						
    					mltQuery.setQuery( "" )
	    					.setRequestHandler("/mlt")
	    					.setParam("stream.body", qqq)
	    					.setParam("mlt.fl", ds.getFuzzyField())
	    					.setParam("mlt.qf", ds.getFuzzyFieldWeight())
	    					.setParam("mlt.boost", "true")
	    					.setParam("mlt.mindf", "1")
	    					.setParam("mlt.mintf", "1");
    					
    					rsp = mServer.query(mltQuery);
    				}
    			}*/
				
                System.out.println("Time Cost [ds" + ds.getId() + "]:" + rsp.getElapsedTime() + " [Q=" + multiCoreQ.getQuery() + "]");

                // 建立 xml
                if (usejson) {
                	JSONObject jsonCore = new JSONObject();
                	jsonRoot.put(jsonCore);
                	jsonCore.put("id", String.valueOf(ds.getId()));
                	jsonCore.put("name", ds.getName());

	                // 如果是主要 core 才需要造出詳細的資訊
	                if (ds.getId() == targetDsId) {
	                	jsonCore.put("selected", true);
	                	
	                    makeCoreBodyJson(rsp, query, jsonCore, ds, priceFaceQuery,req);
	                } // ds.getId() == targetDsId
	                else {
	                	jsonCore.put("selected", false);
                		jsonCore.put("numFound", rsp.getResults().getNumFound());

	                    SolrQuery cleanQ = buildCleanQuery(multiCoreQ);

	                    cleanQ.remove("rows");
	                    cleanQ.remove("start");
	                	jsonCore.put("link", cleanQ.toString() + "&d=" + ds.getId());
	                }
                }
                else {
	                Element xmlCore = root.addElement("core");
	                xmlCore.addAttribute("id", String.valueOf(ds.getId()));
	                xmlCore.addAttribute("name", ds.getName());
	
	                // 如果是主要 core 才需要造出詳細的資訊
	                if (ds.getId() == targetDsId) {
	                    xmlCore.addAttribute("selected", "true");
	                    makeCoreBodyXml(rsp, query, xmlCore, ds, priceFaceQuery,req);
	                } // ds.getId() == targetDsId
	                else {
	                    xmlCore.addAttribute("selected", "false");
	                    
	                    SolrQuery cleanQ = buildCleanQuery(multiCoreQ);

	                    cleanQ.remove("rows");
	                    cleanQ.remove("start");
	                    xmlCore.addAttribute("link", cleanQ.toString() + "&d=" + ds.getId());
	                }
                }
            } // for ds

            if (usejson) {
	            resp.setContentType("application/json;charset=UTF-8");
	            resp.getWriter().write(jsonDoc.toString());
            }
            else {
	            resp.setContentType("application/xml;charset=UTF-8");
	            PrintWriter out = resp.getWriter();
	            try {
	                XMLWriter output = new XMLWriter(out);
	                output.write(xmlDocument);
	                output.close();
	            }
	            catch (IOException e) {
	                System.out.println(e.getMessage());
	            }
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    @Override
    protected void makeCoreBodyXml(
        QueryResponse rsp,
        SolrQuery query,
        Element root,
        DataSet ds,
        String priceFaceQuery
        ,HttpServletRequest request) {
        SolrDocumentList docs;
        root.addAttribute("qTime", String.valueOf((double) rsp.getQTime()
            / (double) 1000));

        // query 物件
        SolrQuery liteQuery = buildCleanQuery(query);
        Element xmlQuery = root.addElement("query");

        xmlQuery.addAttribute("rows", query.getRows().toString()); // 一頁幾筆
        xmlQuery.addAttribute("start", query.getStart().toString()); // 開始筆數
        xmlQuery.addElement("fullLink").addCDATA(liteQuery.toString() + "&d=" + ds.getId());
        liteQuery.remove("start");
        liteQuery.remove("rows");
        xmlQuery.addElement("link").addCDATA(liteQuery.toString() + "&d=" + ds.getId());

        // 排序資訊
        String sortables = "score"
            + (ds.getSortables() != null ? "," + ds.getSortables() : "");
        Element xmlSortables = root.addElement("sortables");
        for (String sortable : sortables.split(",")) {
            Element xmlSortable = xmlSortables.addElement("data");
            xmlSortable.addAttribute("name", translate(sortable)).addAttribute(
                "key",
                sortable);

            for (String sortField : liteQuery.getSortFields()) {
                if (sortable.equals(sortField.split(" ")[0])) {
                    String order = sortField.split(" ")[1];
                    xmlSortable.addAttribute("order", order);
                }
            }
            SolrQuery sortQuery = liteQuery.getCopy();
            sortQuery.setSortField(sortable, SolrQuery.ORDER.asc);
            xmlSortable.addElement("ascLink").addCDATA(
                sortQuery.toString() + "&d=" + ds.getId());
            sortQuery.setSortField(sortable, SolrQuery.ORDER.desc);
            xmlSortable.addElement("descLink").addCDATA(
                sortQuery.toString() + "&d=" + ds.getId());

        }

        // 麵包屑
        if (liteQuery.getFilterQueries() != null) {
            Element xmlQueryHistory = root.addElement("queryHistory");
            Pattern p = Pattern.compile("\\(.*\\)");
            Pattern p2 = Pattern.compile("\\[.*\\]");

            SolrQuery queryHistoryQuery = liteQuery.getCopy();
            queryHistoryQuery.setFilterQueries(null);
            queryHistoryQuery.setStart(0);

            for (String eachString : liteQuery.getFilterQueries()) {
                Matcher m = p.matcher(eachString);
                String s = null;
                if (m.find()) {
                    s = m.group();

                    if (s != null) {
                        s = s.replace("(", "");
                        s = s.replace(")", "");
                    }
                }
                else {
                    m = p2.matcher(eachString);
                    if (m.find()) {
                        s = eachString;
                    }
                }

                if (s != null) {
                    queryHistoryQuery.addFilterQuery(eachString); // 標準麵包屑
                    // shellQuery.removeFilterQuery(eachString); //
                    // 單獨去掉一個 fq
                    s = s.replaceAll("\"", "");
                    s = translate(s);
                    if (s.contains(":")) {
                        String s1 = s;
                        s = s.substring(s.indexOf(":") + 1, s.length());

                        if (s1.contains("NOT"))
                            s = MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "search.exclude")+"(" + s + ")";

                    }
                    Element queryHistoryData = xmlQueryHistory.addElement("data");
                    queryHistoryData.addAttribute("name", s);
                    queryHistoryData.addElement("link").addCDATA(
                        queryHistoryQuery.toString() + "&d=" + ds.getId());
                }
            }
        } // if filter query != null

        // 維度
        Element faceList = root.addElement("facets");
        Map<String, Integer> facetQuerys = (Map<String, Integer>) rsp.getFacetQuery();
        if (facetQuerys != null) {
            makeCloudFace(faceList, facetQuerys, ds.getId(),request);
            makeReSearchFace(faceList, facetQuerys, liteQuery, ds.getId(),request);
            // 範圍型維度
            FacetQuery[] dsFQs = ds.getFQ();
            
            for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
            	makeFacetQueryElement(
                    dsFQs[jjj],
                    faceList,
                    facetQuerys,
                    liteQuery,
                    ds.getId());
            }
            makeFace(rsp.getFacetFields(), liteQuery, faceList, ds.getId());

        }

        // 文件
        Element xmlGroups = root.addElement("groups");
    	GroupResponse gr = rsp.getGroupResponse();
        	
    	for (GroupCommand gc : gr.getValues()) {
    		root.addAttribute("numFound", String.valueOf(gc.getMatches())); // 總筆數
        		
    		for (Group grp : gc.getValues()) {
    			Element xmlDocs = xmlGroups.addElement("docs");
                xmlDocs.addAttribute("numFound", String.valueOf(grp.getResult().getNumFound())); // 總筆數
                xmlDocs.addAttribute("name", grp.getGroupValue()); // 總筆數
                xmlDocs.addAttribute("groupBy", gc.getName()); // 總筆數

                int i = 0;
                Map<String, Map<String, List<String>>> highlights = rsp.getHighlighting();
                docs = grp.getResult();

                for (Iterator<SolrDocument> iterator = docs.iterator(); iterator.hasNext(); i++) {
                    SolrDocument doc = iterator.next();
                    Collection<String> fieldNames = doc.getFieldNames();

                    Element xmlDoc = xmlDocs.addElement("doc").addAttribute(
                        "id",
                        doc.getFieldValue("id").toString());

                    for (String fieldName : fieldNames) {
                        Element xmlField = xmlDoc.addElement("data").addAttribute(
                            "name",
                            fieldName);

                        Collection<Object> values = doc.getFieldValues(fieldName);
                        for (Object value : values) {
                            String data = getHighlightString(
                                highlights,
                                fieldName,
                                doc.getFieldValue("id").toString(),
                                value.toString());

                            xmlField.addElement("value").addCDATA(data);
                        }
                    }
                }
    		}
    	}
    }
    
    @Override
    protected void makeCoreBodyJson(
        QueryResponse rsp,
        SolrQuery query,
        JSONObject root,
        DataSet ds,
        String priceFaceQuery,
        HttpServletRequest request) {
    	
        try {
			SolrDocumentList docs;
			root.put("qTime", String.valueOf((double) rsp.getQTime() / (double) 1000));

			// query 物件
			SolrQuery liteQuery = buildCleanQuery(query);
			JSONObject jsonQuery = new JSONObject();
			root.put("query", jsonQuery);

			jsonQuery.put("rows", query.getRows().toString()); // 一頁幾筆
			jsonQuery.put("start", query.getStart().toString()); // 開始筆數
			jsonQuery.put("fullLink", liteQuery.toString() + "&d=" + ds.getId());
			liteQuery.remove("start");
			liteQuery.remove("rows");
			jsonQuery.put("link", liteQuery.toString() + "&d=" + ds.getId());

			// 排序資訊
			String sortables = "score"
			    + (ds.getSortables() != null ? "," + ds.getSortables() : "");

			JSONArray jsonSortables = new JSONArray();
			root.put("sortables", jsonSortables);
			
			for (String sortable : sortables.split(",")) {
				JSONObject jsonSortable = new JSONObject();
				jsonSortables.put(jsonSortable);
				
				jsonSortable.put("name", translate(sortable)).put("key", sortable);

			    for (String sortField : liteQuery.getSortFields()) {
			        if (sortable.equals(sortField.split(" ")[0])) {
			            String order = sortField.split(" ")[1];
			            jsonSortable.put("order", order);
			        }
			    }
			    SolrQuery sortQuery = liteQuery.getCopy();
			    sortQuery.setSortField(sortable, SolrQuery.ORDER.asc);
			    jsonSortable.put("ascLink", sortQuery.toString() + "&d=" + ds.getId());
			    sortQuery.setSortField(sortable, SolrQuery.ORDER.desc);
			    jsonSortable.put("descLink", sortQuery.toString() + "&d=" + ds.getId());

			}

			// 麵包屑
			if (liteQuery.getFilterQueries() != null) {
			    JSONArray jsonQueryHistories = new JSONArray();
			    root.put("queryHistories", jsonQueryHistories);
			    
			    Pattern p = Pattern.compile("\\(.*\\)");
			    Pattern p2 = Pattern.compile("\\[.*\\]");

			    SolrQuery queryHistoryQuery = liteQuery.getCopy();
			    queryHistoryQuery.setFilterQueries(null);
			    queryHistoryQuery.setStart(0);

			    for (String eachString : liteQuery.getFilterQueries()) {
			        Matcher m = p.matcher(eachString);
			        String s = null;
			        if (m.find()) {
			            s = m.group();

			            if (s != null) {
			                s = s.replace("(", "");
			                s = s.replace(")", "");
			            }
			        }
			        else {
			            m = p2.matcher(eachString);
			            if (m.find()) {
			                s = eachString;
			            }
			        }

			        if (s != null) {
			            queryHistoryQuery.addFilterQuery(eachString); // 標準麵包屑
			            // shellQuery.removeFilterQuery(eachString); //
			            // 單獨去掉一個 fq
			            s = s.replaceAll("\"", "");
			            s = translate(s);
			            if (s.contains(":")) {
			                String s1 = s;
			                s = s.substring(s.indexOf(":") + 1, s.length());

			                if (s1.contains("NOT"))
			                    s = MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "search.exclude")+"(" + s + ")";

			            }
			            try {
							JSONObject jsonQueryHistory = new JSONObject();
							jsonQueryHistories.put(jsonQueryHistory);
							jsonQueryHistory.put("name", s);
							jsonQueryHistory.put("link", queryHistoryQuery.toString() + "&d=" + ds.getId());
						} catch (JSONException e) {
							e.printStackTrace();
						}
			        }
			    }
			} // if filter query != null

			// 維度
			JSONArray jsonFacets = new JSONArray();
			root.put("facets", jsonFacets);
			
			Map<String, Integer> facetQuerys = (Map<String, Integer>) rsp.getFacetQuery();
			if (facetQuerys != null) {
				JSONObject obj = null;
				
				obj = makeCloudFacetJson(facetQuerys, ds.getId(),request);
				if (obj != null) {
					jsonFacets.put(obj);
				}
				
				obj = makeReSearchFacetJson(facetQuerys, liteQuery, ds.getId(),request);
				if (obj != null) {
					jsonFacets.put(obj);
				}
			    // 範圍型維度
			    FacetQuery[] dsFQs = ds.getFQ();
			    
			    for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {
			    	jsonFacets.put(makeFacetQueryJson(
			            dsFQs[jjj],
			            facetQuerys,
			            liteQuery,
			            ds.getId()));
			    }
			    makeFacetJson(rsp.getFacetFields(), liteQuery, jsonFacets, ds.getId());

			}

			// 文件
        	JSONArray jsonGroups = new JSONArray();
        	root.put("groups", jsonGroups);
        	GroupResponse gr = rsp.getGroupResponse();
        	
        	for (GroupCommand gc : gr.getValues()) {
        		root.put("numFound", gc.getMatches());
        		
        		for (Group grp : gc.getValues()) {
	        		JSONObject jsonGroup = new JSONObject();
	        		jsonGroups.put(jsonGroup);
	        		jsonGroup.put("numFound", grp.getResult().getNumFound());
	        		jsonGroup.put("name", grp.getGroupValue());
	        		jsonGroup.put("groupBy", gc.getName()); // 總筆數
	    			docs = grp.getResult();
	
	    			if (grp.getResult().getNumFound() > 0) {
	    				JSONArray jsonDocs = new JSONArray();
	    				jsonGroup.put("docs", jsonDocs);
	    			    
	    			    int i = 0;
	    			    Map<String, Map<String, List<String>>> highlights = rsp.getHighlighting();
	    				SimpleDateFormat stdDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
	
	    			    for (Iterator<SolrDocument> iterator = docs.iterator(); iterator.hasNext(); i++) {
	    			        SolrDocument doc = iterator.next();
	    			        Collection<String> fieldNames = doc.getFieldNames();
	
	    			        try {
	    						JSONObject jsonDoc = new JSONObject().put("id", doc.getFieldValue("id").toString());
	    						//JSONArray dataArr = new JSONArray();
	    						//jsonDoc.put("data", dataArr);
	    						jsonDocs.put(jsonDoc);
	
	    						for (String fieldName : fieldNames) {                    
	    						    //JSONObject jsonField = new JSONObject().put("name", fieldName);
	    						    JSONArray valuesArr = new JSONArray();
	    						    Collection<Object> values = doc.getFieldValues(fieldName);
	    						    
	    						    if (values != null) {
	    						    	// If multivalue and actually has more than 1 value, use json array
	    						    	if (values.size() > 1) {
	    								    for (Object value : values) {
	    								    	String strVal = null;
	    								    	//System.out.println(value.getClass().getName());
	    								    	if (value instanceof Date) {
	    								    		strVal = stdDF.format((Date)value);
	    								    	}
	    								    	else {
	    								    		strVal = value.toString();
	    								    	}
	    								        String data = getHighlightString(
	    								            highlights,
	    								            fieldName,
	    								            doc.getFieldValue("id").toString(),
	    								            strVal);
	    		
	    								        valuesArr.put(data);
	    								    }
	    								    
	    								    jsonDoc.put(fieldName, valuesArr);
	    						    	}
	    						    	else {
	    									String strVal = null;
	    									Object value = values.iterator().next();
	    									if (value instanceof Date) {
	    										strVal = stdDF.format((Date) value);
	    									} else {
	    										strVal = value.toString();
	    									}
	    									String data = getHighlightString(
	    											highlights, fieldName, doc
	    													.getFieldValue("id")
	    													.toString(), strVal);
	    									jsonDoc.put(fieldName, data);
	    						    	}
	    						    }
	    						}
	    					} catch (JSONException e) {
	    						e.printStackTrace();
	    					}
	    			    }
	    			}
        		}
        	}
					
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }
    
}
