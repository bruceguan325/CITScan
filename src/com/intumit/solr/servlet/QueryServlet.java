package com.intumit.solr.servlet;

import it.exprivia.cnos.opencloud.Cloud;
import it.exprivia.cnos.opencloud.Tag;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.dataset.DataSet;
import com.intumit.solr.dataset.DataSet.FacetQuery;
import com.intumit.solr.dataset.DataSetFacade;
import com.intumit.solr.dataset.ElevatorSet;
import com.intumit.solr.dataset.ElevatorSetFacade;
import com.intumit.solr.qparser.ExtendedDismaxQParserPlugin;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.util.StringUtil;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class QueryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private static final java.text.DecimalFormat df = new java.text.DecimalFormat(
        "###,###元");
    private static final String pricePattern = "(.*?):\\[([0-9\\*]+) TO ([0-9\\*]+)\\]";
    private static final String datePattern = "(.*?_dt.*?):\\[(.*? TO .*?)\\]";
    private static final String fgPattern = "(.*?):\\[(.*? TO .*?)\\]";
    

    private static final Pattern breadcrumbPattern1 = Pattern.compile("\\(.*\\)");
    private static final Pattern breadcrumbPattern2 = Pattern.compile("\\[.*\\]");

    protected static final HashMap<String, String> translateMap = new HashMap<String, String>();
	protected static long lastLoadTimestamp = -1;
	protected static long CHECK_PERIOD = 5 * 60 * 1000;
	protected static HashSet<String> blackList = null;

    {
/*        translateMap.put("NOW/DAY TO NOW/DAY+1DAY", "近一日");
        translateMap.put("NOW/DAY+1DAY-3DAY TO NOW/DAY+1DAY", "近三日");
        translateMap.put("NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY", "近一個月");
        translateMap.put("NOW/DAY+1DAY-3MONTH TO NOW/DAY+1DAY", "近三個月");
        translateMap.put("NOW/DAY+1DAY-6MONTH TO NOW/DAY+1DAY", "近半年");
        translateMap.put("NOW/DAY+1DAY-1YEAR TO NOW/DAY+1DAY", "近一年");
        translateMap.put("NOW/DAY+1DAY-3YEAR TO NOW/DAY+1DAY", "近三年");
        translateMap.put("* TO NOW/DAY-3YEAR", "超過三年以前");*/
        translateMap.put("", "(N/A)");
        translateMap.put("score", MessageUtil.getMessage(Locale.getDefault(), "search.score"));
    }

    String getQueryType() {
    	return "/browse";
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException {

        try {
            
            boolean usejson = "json".equalsIgnoreCase(req.getParameter("format"));

            String[] finq = req.getParameterValues("finq"); // 在結果中查詢
            String[] foutq = req.getParameterValues("foutq"); // 在結果中排除
            
            // 是否強迫啟動（或強迫關閉）模糊搜尋
            // "fuzzy" 已經 deprecated, 這邊只是為了相容性
            Boolean forceEnableOrDisableFuzzy = 
            		req.getParameter(ExtendedDismaxQParserPlugin.SEGMENT) == null 
            			? (req.getParameter("fuzzy") == null 
            				? null
            				: new Boolean((String) req.getParameter("fuzzy")))
            			: new Boolean((String) req.getParameter(ExtendedDismaxQParserPlugin.SEGMENT));
            Boolean forceEnableOrDisableSyn = req.getParameter(ExtendedDismaxQParserPlugin.SYNONYM) == null ? null : new Boolean((String) req.getParameter(ExtendedDismaxQParserPlugin.SYNONYM));
            
            String qf = req.getParameter("qf");
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

            int totalCoreCount = 0;

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
	                    .setParam("mm", "0")
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
                            
                            String mappedName = dsFQs[jjj].mappingName(currFQ);
                            if (!translateMap.containsKey(currFQ) && StringUtils.isNotEmpty(mappedName)) {
                                translateMap.put(currFQ, mappedName);
                            }
                    	}
                    }

                    if (StringUtils.isNotEmpty(ds.getQueryBoost())) {
        				multiCoreQ.setParam("bf", ds.getQueryBoost());
                    }

                    if (StringUtils.isNotEmpty(ds.getQueryBoostMultiply())) {
        				multiCoreQ.setParam("boost", ds.getQueryBoostMultiply());
                    }

                    // 處理 SORT 
                    String sortStr = req.getParameter("sort") == null
                        ? StringUtils.defaultIfEmpty(ds.getDefaultSort(), "score desc")
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
                    	multiCoreQ.setParam("bq", eSet.facetQueryToBoostQuery() + "^100 " + "\"" + qqq2 + "\"^5 (" + qqq2 + ")^5");
                    }
                    else {
                        multiCoreQ.setParam("bq", "\"" + qqq2 + "\"^5 (" + qqq2 + ")^5");
                    }
                    
                    if (fields == null) {
                    	List<String> fldArr = new ArrayList<>();
                        if (StringUtils.isNotEmpty(ds.getNameField())) {
                        	fldArr.add("Name_t:" + ds.getNameField());
                        }
                        if (StringUtils.isNotEmpty(ds.getBodyFields())) {
                        	fldArr.addAll(Arrays.asList(StringUtils.split(ds.getBodyFields(), ",")));
                        }
                        
                        
                        if (fldArr.size() > 0) {
                        	fldArr.add("id");
                        	
                        	fields = fldArr.toArray(new String[0]);
                        }
                    }
                    
                    if (fields != null) {
                    	multiCoreQ.setFields(fields);
                    }
            		
                    multiCoreQ
						.setParam("enableElevation", true)
						.setParam("forceElevation", true)
						.setParam("pf", ds.getFieldWeight())
						.setParam("pf2", ds.getFieldWeight())
						.setParam("qs", "2")
                        .setParam("hl.fl", ds.getFieldHighlight())
                        .setParam("hl.usePhraseHighlighter", true)
                        .setParam(
                            "hl.fragsize",
                            "" + WiseSystemConfigFacade.getInstance().get().getDescriptionMax())
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
				
                totalCoreCount += rsp.getResults().getNumFound();
                System.out.println("Time Cost [ds" + ds.getId() + "]:" + rsp.getElapsedTime() + "ms [Q=" + multiCoreQ.getQuery() + "] [S=" + req.getRemoteHost() + "]");

                // 建立 xml
                if (usejson) {
                	JSONObject jsonCore = new JSONObject();
                	jsonRoot.put(jsonCore);
                	jsonCore.put("id", String.valueOf(ds.getId()));
                	jsonCore.put("name", ds.getName());
                	jsonCore.put("nameField", StringUtils.defaultString(StringUtils.trimToNull(ds.getNameField()), "Name_t"));
                	jsonCore.put("bodyFields", 
                			new JSONArray(
                					StringUtils.split(
                							StringUtils.defaultString(
                									StringUtils.trimToNull(ds.getBodyFields()), "Description_mt"), ",")));
                	jsonCore.put("numFound", rsp.getResults().getNumFound());

	                // 如果是主要 core 才需要造出詳細的資訊
	                if (ds.getId() == targetDsId) {
	                	jsonCore.put("selected", true);
	                    makeCoreBodyJson(rsp, query, jsonCore, ds, priceFaceQuery,req);
	                } // ds.getId() == targetDsId
	                else {
	                	jsonCore.put("selected", false);

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
	                xmlCore.addAttribute(
	                    "numFound",
	                    String.valueOf(rsp.getResults().getNumFound()));
	
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

            if (HibernateUtil.isInitialized())
            	doSearchLog(totalCoreCount, (String) req.getParameter("q"), targetDsId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    static int termId = 0;
    static Map<Integer, String> keyMap = new HashMap<Integer, String>();

	protected HashSet<String> loadBlackList() {
		if (lastLoadTimestamp + CHECK_PERIOD < System.currentTimeMillis()) {
			List<BlackKeyword> list = BlackKeywordFacade.getInstance().listBlackKeywords();
			
			blackList = new HashSet<String>();
			for (BlackKeyword kw: list) {
				blackList.add(kw.getKeyword());
			}
			lastLoadTimestamp = System.currentTimeMillis();
		}
		return blackList;
	}

	void doSearchLog(int totalCoreCount, String query, int dsId) {
    	if (totalCoreCount != 0) {
            SearchKeywordLogFacade.getInstance().log(query, "" + dsId);
        }
        else {
            SearchKeywordLogFacade.getInstance().log(query, SearchKeywordLogFacade.NO_RESULT_FUNC);
        }
    }

    protected void makeCoreBodyXml(
        QueryResponse rsp,
        SolrQuery query,
        Element root,
        DataSet ds,
        String priceFaceQuery,HttpServletRequest request) {
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
            
            for (String eachString : liteQuery.getFilterQueries()) {
                Matcher m = breadcrumbPattern1.matcher(eachString);
                String s = null;
                if (m.find()) {
                    s = m.group();

                    if (s != null) {
                        s = s.replace("(", "");
                        s = s.replace(")", "");
                    }
                }
                else {
                    m = breadcrumbPattern2.matcher(eachString);
                    if (m.find()) {
                        s = eachString;
                    }
                }

                if (s != null) {
                	SolrQuery queryHistoryQuery = liteQuery.getCopy();
				    queryHistoryQuery.setStart(0);
                    queryHistoryQuery.removeFilterQuery(eachString); // 標準麵包屑
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
        docs = rsp.getResults();

        if (rsp.getResults().getNumFound() > 0) {
            Element xmlDocs = root.addElement("docs");
            xmlDocs.addAttribute("numFound", String.valueOf(rsp.getResults()
                .getNumFound())); // 總筆數

            int i = 0;
            Map<String, Map<String, List<String>>> highlights = rsp.getHighlighting();

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
    

    protected void makeCoreBodyJson(
        QueryResponse rsp,
        SolrQuery query,
        JSONObject root,
        DataSet ds,
        String priceFaceQuery
        ,HttpServletRequest request) {
    	
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

			    for (String eachString : liteQuery.getFilterQueries()) {
			        Matcher m = breadcrumbPattern1.matcher(eachString);
			        String s = null;
			        if (m.find()) {
			            s = m.group();

			            if (s != null) {
			                s = s.replace("(", "");
			                s = s.replace(")", "");
			            }
			        }
			        else {
			            m = breadcrumbPattern2.matcher(eachString);
			            if (m.find()) {
			                s = eachString;
			            }
			        }

			        if (s != null) {
					    SolrQuery queryHistoryQuery = liteQuery.getCopy();
					    queryHistoryQuery.setStart(0);
			            queryHistoryQuery.removeFilterQuery(eachString); // 標準麵包屑
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
			docs = rsp.getResults();

			if (rsp.getResults().getNumFound() > 0) {
				JSONArray jsonDocs = new JSONArray();
			    root.put("docs", jsonDocs);
			    
			    int i = 0;
			    Map<String, Map<String, List<String>>> highlights = rsp.getHighlighting();
				//SimpleDateFormat stdDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
				DateTimeFormatter stdDF = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");

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
								    		strVal = new DateTime(value).toString(stdDF);
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
							    		strVal = new DateTime(value).toString(stdDF);
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
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }

    /*
    private String getPriceFaceQuery(SolrQuery multiCoreQ, SolrServer mServer)
        throws SolrServerException {

        String priceFaceQuery = "";
        int lowPricei = 0, highPricei = 0;

        try {
            // 組 query 找最低價錢
            SolrQuery priceCoreQ = multiCoreQ.getCopy();
            priceCoreQ.setRows(1).setStart(0).setSortField(
                PRICE_FIELD_NAME,
                SolrQuery.ORDER.asc);

            QueryResponse rsp = mServer.query(priceCoreQ);

            if (rsp.getResults().size() > 0)
                lowPricei = (Integer) rsp.getResults().get(0).getFieldValue(
                    PRICE_FIELD_NAME);

            // 組 query 找最高價錢
            priceCoreQ = multiCoreQ.getCopy();
            priceCoreQ.setRows(1).setStart(0).setSortField(
                PRICE_FIELD_NAME,
                SolrQuery.ORDER.desc);

            rsp = mServer.query(priceCoreQ);

            if (rsp.getResults().size() > 0)
                highPricei = (Integer) rsp.getResults().get(0).getFieldValue(
                    PRICE_FIELD_NAME);

            // 找出最高最低後開始組動態 face query
            BigDecimal lowPrice = new BigDecimal(lowPricei);
            BigDecimal highPrice = new BigDecimal(highPricei);

            int precision = highPrice.precision() - 1; // 最高價錢的前一位數如 90000
                                                       // 的話，就是以萬
            double base = Math.pow(10, precision);

            boolean startFlag = false;
            boolean endFlag = false;
            for (int i = 1; i <= 10; i++) {
                BigDecimal maxPrice = new BigDecimal(i * base - 1);
                BigDecimal minPrice = new BigDecimal((i - 1) * base);

                if (!startFlag && (maxPrice.intValue() > lowPrice.intValue())) {
                    priceFaceQuery += PRICE_FIELD_NAME
                        + ":["
                        + minPrice.intValue()
                        + " TO "
                        + maxPrice.intValue()
                        + "]\n";
                    startFlag = true;
                    continue;
                }

                if (!endFlag && (maxPrice.intValue() >= highPrice.intValue())) {
                    priceFaceQuery += PRICE_FIELD_NAME
                        + ":["
                        + minPrice.intValue()
                        + " TO "
                        + maxPrice.intValue()
                        + "]\n";
                    endFlag = true;
                    break;
                }

                if (startFlag && !endFlag) {
                    priceFaceQuery += PRICE_FIELD_NAME
                        + ":["
                        + minPrice.intValue()
                        + " TO "
                        + maxPrice.intValue()
                        + "]\n";
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return priceFaceQuery;
    }
    */

    /**
     * 製作FacetQuery維度節點
     *
     * @param faceList
     * @param facetQuerys
     * @param dataFaceQuery
     * @param query
     */
    protected void makeFacetQueryElement (
        FacetQuery fq,
        Element faceListElement,
        Map<String, Integer> facetQueryResults,
        SolrQuery query,
        int dsId) {

        Element faceElement = faceListElement.addElement("facet").addAttribute(
            "name",
            fq.getFieldName());

        boolean has = false;
        int prevCcc = 0;
        for (String key: fq.facetQuries()) {
            if (facetQueryResults.containsKey(key)) {
                int ccc = facetQueryResults.get(key).intValue();

                if (ccc > 0) { // && ccc != prevCcc) { 
                	// 本來 prevCcc 的功用是如果這個 facetQuery 的 count 跟前一個相同，那就不顯示，不過有些情境看起來會像 bug...
                	// 例如 價格區間，如果客戶是使用那種條件是互相包覆的（例如 $200以上，$300以上....），那當條件是範圍比較小的時候就會導致數量相同出現
                    prevCcc = ccc;
                    SolrQuery newQuery = query.getCopy();
                    newQuery.setStart(0);
                    newQuery.addFilterQuery("(" + key + ")");
                    
                    String name = fq.mappingName(key);

                    if (name == null) {
                    	name = translate(key);
                    }
                    else {
                    	translateMap.put(key, name);
                    }

                    Element elm = faceElement.addElement("data");
                    elm.addAttribute("name", name)
                    	.addAttribute("query", "fq=" + key)
                    	.addAttribute("count", String.valueOf(ccc))
                    		.addElement("link").addCDATA(newQuery.toString() + "&d=" + dsId);

                    has = true;
                }
            }
        }

        if (!has)
            faceListElement.remove(faceElement);
    }

    /**
     * 製作FacetQuery維度節點
     *
     * @param faceList
     * @param facetQuerys
     * @param dataFaceQuery
     * @param query
     */
    protected JSONObject makeFacetQueryJson (
        FacetQuery fq,
        Map<String, Integer> facetQueryResults,
        SolrQuery query,
        int dsId) {
        
		JSONArray dataArr = new JSONArray();

        boolean has = false;
        int prevCcc = 0;
        for (String key: fq.facetQuries()) {
            if (facetQueryResults.containsKey(key)) {
                int ccc = facetQueryResults.get(key).intValue();

                if (ccc > 0) { // && ccc != prevCcc) { 
                	// 本來 prevCcc 的功用是如果這個 facetQuery 的 count 跟前一個相同，那就不顯示，不過有些情境看起來會像 bug...
                	// 例如 價格區間，如果客戶是使用那種條件是互相包覆的（例如 $200以上，$300以上....），那當條件是範圍比較小的時候就會導致數量相同出現
                    prevCcc = ccc;
                    SolrQuery newQuery = query.getCopy();
                    newQuery.setStart(0);
                    newQuery.addFilterQuery("(" + key + ")");
                    
                    String name = fq.mappingName(key);
                    
                    if (name == null) {
                    	name = translate(key);
                    }
                    else {
                    	translateMap.put(key, name);
                    }
                    
                    try {
						dataArr.put(
								new JSONObject().put("name", name)
					        		.put("query", "fq=" + key)
						        	.put("count", ccc)
						        	.put("link", newQuery.toString() + "&d=" + dsId)
							);
					} catch (JSONException e) {
						e.printStackTrace();
					}

                    has = true;
                }
            }
        }

        if (has) {
            try {
				return new JSONObject().put("name", translate(fq.getFieldName())).put("data", dataArr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        return null;
    }

    /**
     * 取得所以設定的維度，不包含日期、價錢、再搜尋、延伸搜尋等。
     *
     * @param faceList
     * @param facetQuerys
     * @param dataFaceQuery
     * @param query
     */
    protected void makeFace(
        List<FacetField> facetFields,
        SolrQuery query,
        Element faceList,
        int dsId) {
        for (Iterator<FacetField> iterator = facetFields.iterator(); iterator.hasNext();) {
            FacetField facetEnt = iterator.next();

            if (facetEnt.getValues() != null && facetEnt.getValues().size() > 0) {

                Element ele = faceList.addElement("facet").addAttribute(
                    "name",
                    translate(facetEnt.getName()));

                for (Iterator<FacetField.Count> iterator2 = facetEnt.getValues()
                    .iterator(); iterator2.hasNext();) {
                    FacetField.Count facetCount = iterator2.next();

                    if (facetCount.getCount() == 0)
                        continue;

                    String fq = "(" + facetEnt.getName() + ":\"" + facetCount.getName() + "\")";
                    SolrQuery newQuery = query.getCopy();
                    newQuery.setStart(0);
                    newQuery.addFilterQuery(fq);
                    ele.addElement("data")
                        .addAttribute("name", translate(facetCount.getName()))
                        .addAttribute("query", "fq=" + fq)
                        .addAttribute("count", String.valueOf(facetCount.getCount()))
                        .addElement("link").addCDATA(newQuery.toString() + "&d=" + dsId);
                } // end for
            }
        }
    }

    /**
     * 取得所以設定的維度，不包含日期、價錢、再搜尋、延伸搜尋等。
     *
     * @param faceList
     * @param facetQuerys
     * @param dataFaceQuery
     * @param query
     */
    protected void makeFacetJson(
        List<FacetField> facetFields,
        SolrQuery query,
        JSONArray jsonFacets,
        int dsId) {
    	
        for (Iterator<FacetField> iterator = facetFields.iterator(); iterator.hasNext();) {
            FacetField facetEnt = iterator.next();

            if (facetEnt.getValues() != null && facetEnt.getValues().size() > 0) {
				JSONArray dataArr = new JSONArray();

                for (Iterator<FacetField.Count> iterator2 = facetEnt.getValues()
                    .iterator(); iterator2.hasNext();) {
                    FacetField.Count facetCount = iterator2.next();

                    if (facetCount.getCount() == 0)
                        continue;

                    String fq = "(" + facetEnt.getName() + ":\"" + facetCount.getName() + "\")";
                    SolrQuery newQuery = query.getCopy();
                    newQuery.setStart(0);
                    newQuery.addFilterQuery(fq);
                    
                    try {
                        dataArr.put(
                            new JSONObject()
                                .put("name", translate(facetCount.getName()))
                                .put("count", facetCount.getCount())
                                .put("query", "fq=" + fq)
                                .put("link", newQuery.toString() + "&d=" + dsId)
                            );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } // end for
                
                try {
					jsonFacets.put(new JSONObject().put("name", translate(facetEnt.getName())).put("data", dataArr));
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
        }
    }

    /**
     * 取得再搜尋維度
     *
     * @param facetQuerys
     * @param query
     * @return
     */
    protected void makeReSearchFace(
        Element faceList,
        Map<String, Integer> facetQuerys,
        SolrQuery query,
        int dsId,HttpServletRequest request) {

        boolean has = false;
        TreeSet<Map.Entry<String, Integer>> sorted = WiSeUtils.getSortedTreeSet();

        for (Iterator<Map.Entry<String, Integer>> iter = facetQuerys.entrySet()
            .iterator(); iter.hasNext();) {
            Map.Entry<String, Integer> entry = iter.next();

            if (entry.getKey().startsWith("__")) {
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;

                has = true;
                sorted.add(entry);
            }
        }
        if (has) {
            Element ele = faceList.addElement("facet").addAttribute(
                "name",
                MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "again.search"));
            int showCount = 0;
            for (Iterator<Map.Entry<String, Integer>> iter = sorted.iterator(); iter.hasNext();) {
                Map.Entry<String, Integer> entry = iter.next();
                String val = entry.getKey().substring(2);
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;
                if (++showCount > 8)
                    break;
                SolrQuery newQuery = query.getCopy();
                newQuery.setStart(0);
                newQuery.addFilterQuery("(" + val + ")");

                ele.addElement("data").addAttribute("name", val).addAttribute(
                    "count",
                    String.valueOf(ccc)).addElement("link").addCDATA(
                    newQuery.toString() + "&d=" + dsId);
            }
        }
    }

    /**
     * 取得再搜尋維度
     *
     * @param facetQuerys
     * @param query
     * @return
     */
    protected JSONObject makeReSearchFacetJson(
        Map<String, Integer> facetQuerys,
        SolrQuery query,
        int dsId,HttpServletRequest request) {

        boolean has = false;
        TreeSet<Map.Entry<String, Integer>> sorted = WiSeUtils.getSortedTreeSet();

        for (Iterator<Map.Entry<String, Integer>> iter = facetQuerys.entrySet()
            .iterator(); iter.hasNext();) {
            Map.Entry<String, Integer> entry = iter.next();

            if (entry.getKey().startsWith("__")) {
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;

                has = true;
                sorted.add(entry);
            }
        }
        if (has) {
            int showCount = 0;
			JSONArray dataArr = new JSONArray();
            
            for (Iterator<Map.Entry<String, Integer>> iter = sorted.iterator(); iter.hasNext();) {
                Map.Entry<String, Integer> entry = iter.next();
                String val = entry.getKey().substring(2);
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;
                if (++showCount > 8)
                    break;
                SolrQuery newQuery = query.getCopy();
                newQuery.setStart(0);
                newQuery.addFilterQuery("(" + val + ")");

                try {
					JSONObject json = new JSONObject().put("name", val)
						.put("count", ccc)
						.put("link", newQuery.toString() + "&d=" + dsId);
					
					dataArr.put(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
            

			try {
				return new JSONObject().put("name", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "again.search")).put("data", dataArr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        
        return null;
    }
    
    /**
     * 取得延伸搜尋維度 Tag Cloud
     *
     * @param facetQuerys
     * @return
     */
    protected void makeCloudFace(
        Element faceList,
        Map<String, Integer> facetQuerys,
        int dsId,HttpServletRequest request) {

        // Tag Cloud 功能 (Begin) 延伸搜尋
        //Cloud cloud = new Cloud(java.util.Locale.TAIWAN);
    	Cloud cloud = new Cloud((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"));
        cloud.setMaxTagsToDisplay(30);
        cloud.setMaxWeight(10);
        cloud.setMinWeight(1);
        cloud.setDefaultLink("q=%s");
        // Tag Cloud 功能 (End)
        for (Iterator<Map.Entry<String, Integer>> iter = facetQuerys.entrySet()
            .iterator(); iter.hasNext();) {
            Map.Entry<String, Integer> entry = iter.next();

            if (entry.getKey().startsWith("__")) {
                String val = entry.getKey().substring(2);
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;

                // 有使用 Tag Cloud 就要加上此行
                try {
                    cloud.addTag(new Tag(val, "q="
                        + URLEncoder.encode(val, "utf-8"), ccc));
                }
                catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (cloud.tags().size() > 0) {
            Element ele = faceList.addElement("facet").addAttribute(
                "name",
                MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "extended.search"));

            for (Tag tag : cloud.tags()) {
                ele.addElement("data")
                    .addAttribute("name", tag.getName())
                    .addAttribute("count", String.valueOf(tag.getWeightInt()))
                    .addElement("link")
                    .addCDATA(tag.getLink() + "&d=" + dsId);
            }
        }
    }

    /**
     * 取得延伸搜尋維度 Tag Cloud
     *
     * @param facetQuerys
     * @return
     */
    protected JSONObject makeCloudFacetJson(
        Map<String, Integer> facetQuerys,
        int dsId,HttpServletRequest request) {

        // Tag Cloud 功能 (Begin) 延伸搜尋
       // Cloud cloud = new Cloud(java.util.Locale.TAIWAN);
    	Cloud cloud = new Cloud((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"));
        cloud.setMaxTagsToDisplay(30);
        cloud.setMaxWeight(10);
        cloud.setMinWeight(1);
        cloud.setDefaultLink("q=%s");
        // Tag Cloud 功能 (End)
        for (Iterator<Map.Entry<String, Integer>> iter = facetQuerys.entrySet()
            .iterator(); iter.hasNext();) {
            Map.Entry<String, Integer> entry = iter.next();

            if (entry.getKey().startsWith("__")) {
                String val = entry.getKey().substring(2);
                int ccc = entry.getValue();
                if (ccc == 0)
                    continue;

                // 有使用 Tag Cloud 就要加上此行
                try {
                    cloud.addTag(new Tag(val, "q="
                        + URLEncoder.encode(val, "utf-8"), ccc));
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        if (cloud.tags().size() > 0) {
        	try {
				JSONArray dataArr = new JSONArray();

				for (Tag tag : cloud.tags()) {
					JSONObject json = new JSONObject().put("name", tag.getName())
				        .put("count", tag.getWeightInt())
				        .put("link", tag.getLink() + "&d=" + dsId);
					
					dataArr.put(json);
				}
				
				return new JSONObject().put("name", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "extended.search")).put("data", dataArr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        
        return null;
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 做一個乾淨的 Query 物件 目的為 1. 怕 URL 過長（這樣 Google AdSense 可能會有 Error） 2. Facet
     * Fileds, Facet Queries 其實不是給 User 自訂的，不需要包含在 URL 裡頭
     */
    protected static SolrQuery buildCleanQuery(SolrQuery query) {
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

    public String getHighlightString(
        Map<String, Map<String, List<String>>> highlights,
        String field,
        String id,
        String def) {
        try {
            List<String> hlList = highlights.get(id).get(field);
            if (hlList != null) {
                for (String hl : hlList) {
                    return hl;
                }
            }

            return def;
        }
        catch (Exception e) {
            return def;
        }
    }

    public String translate(String org) {
        if (org == null)
            return "";

        if (org.matches(fgPattern)) {
            if (org.matches(pricePattern)) {
                Matcher m = Pattern.compile(pricePattern).matcher(org);
                m.find();
                String fieldName = m.group(1);
                String priceFrom = m.group(2);
                String priceTo = m.group(3);
                
                if ("*".equals(priceFrom) && "*".equals(priceTo))
                    return org;

                if ("*".equals(priceFrom)) {
                    return df.format(Integer.parseInt(priceTo)) + " 以下";
                }
                if ("*".equals(priceTo)) {
                    return df.format(Integer.parseInt(priceFrom)) + " 以上";
                }

                return df.format(Integer.parseInt(priceFrom))
                    + " ~ "
                    + df.format(Integer.parseInt(priceTo));
            }
            /*if (org.matches(datePattern)) {
                Matcher m = Pattern.compile(datePattern).matcher(org);
                m.find();
                String fieldName = m.group(1);
                String dateRange = m.group(2);

                if (translateMap.containsKey(dateRange))
                    return translateMap.get(dateRange);
                else
                    return dateRange;
            }*/
        }
        if (StringUtils.isNotBlank(ColumnNameMappingFacade.getInstance().getMappingName(org))) {
            return ColumnNameMappingFacade.getInstance().getMappingName(org);
        }
        if (translateMap.containsKey(org))
            return translateMap.get(org);
        else
            return org;
    }
}
