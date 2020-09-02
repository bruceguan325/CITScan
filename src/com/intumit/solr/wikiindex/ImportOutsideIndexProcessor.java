package com.intumit.solr.wikiindex;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.handler.dataimport.Context;

public class ImportOutsideIndexProcessor extends BatchIndexProcessor {

	String url;

	@Override
	public void init(Context context) {
		super.init(context);
		url = context.getEntityAttribute("url");
	}

	@Override
	protected SolrServer newSolrServer() throws Exception {
		return new HttpSolrServer(url);
	}
}
