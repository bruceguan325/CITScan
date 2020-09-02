package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.solr.SearchManager;
import com.intumit.solr.searchKeywords.SearchKeywordLog;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.thoughtworks.xstream.XStream;

public class StaticPageToSearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private	static final String STATIC_SEARCH_JSP = "/doStaticSearch.jsp";
    

	/** 
	 * 做一個乾淨的 Query 物件
	 * 目的為
	 * 1. 怕 URL 過長（這樣 Google AdSense 可能會有 Error）
	 * 2. Facet Fileds, Facet Queries 其實不是給 User 自訂的，不需要包含在 URL 裡頭
	 */
	private static SolrQuery buildCleanQuery(SolrQuery query) {
		SolrQuery cleanQuery = new SolrQuery(query.getQuery());
		for (int iii=0; query.getFilterQueries() != null && iii < query.getFilterQueries().length; iii++) {
			cleanQuery.addFilterQuery(query.getFilterQueries()[iii].trim());
		}
		cleanQuery.setParam("sort", query.getSortField());
		cleanQuery.setStart(query.getStart());
		cleanQuery.setRows(query.getRows());
		cleanQuery.setQueryType(query.getQueryType());
		
		return cleanQuery;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try {
			String reqUri = req.getRequestURI();
			String qqq = java.net.URLDecoder.decode(reqUri.substring( reqUri.lastIndexOf("/") + 1 ), "UTF-8");
			
			req.setAttribute("queryText", qqq);
			
			req.getRequestDispatcher(STATIC_SEARCH_JSP).forward(req, resp);
		} catch (RuntimeException e) {
			e.printStackTrace();
		} 
	}
}
