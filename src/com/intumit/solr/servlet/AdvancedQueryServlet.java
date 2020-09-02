package com.intumit.solr.servlet;


public class AdvancedQueryServlet extends QueryServlet {

    @Override
    String getQueryType() {
    	return "/select";
    }
    
    @Override
    void doSearchLog(int totalCoreCount, String query, int dsId) {
    	// No search log in advance mode
    }
}
