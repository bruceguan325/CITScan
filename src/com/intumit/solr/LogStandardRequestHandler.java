package com.intumit.solr;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.StandardRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;

public class LogStandardRequestHandler extends StandardRequestHandler {
	@Override
	public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
		// 略過暖機
		if (!req.getClass().getName().equals("org.apache.solr.core.QuerySenderListener$1")
			&& !"/admin/ping".equalsIgnoreCase(req.getContext().get("path").toString())
				) {
			SolrParams params = req.getParams();
			String q = params.get(CommonParams.Q);
			if (!q.matches("(?is).*?(solr|[:\\*\\[\\]]|(\\sAND\\s)|(\\sOR\\s)).*?"))
				SearchKeywordLogFacade.getInstance().log(q, SearchKeywordLogFacade.DEFAULT_FUNC);
		}
		super.handleRequest(req, rsp);
	}
}
