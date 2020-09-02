package com.intumit.solr.wikiindex;

import java.util.ArrayList;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.EntityProcessorBase;

import com.intumit.solr.SearchManager;

public class BatchIndexProcessor extends EntityProcessorBase {

	protected int start = 0;
	protected long total = -1;
	protected int batchSize = 10;
	protected String queryText;
	protected String targetCore;
	protected int limit = -1;
	protected String[] fields;

	protected int cursor = 0;
	protected SolrServer server;

	@Override
	public void init(Context context) {
		super.init(context);
		queryText = context.getEntityAttribute("query");
		targetCore = context.getEntityAttribute("targetCore");
		String field = context.getEntityAttribute("fields");
		if (field != null)
			fields = field.split(",");

		String batchSizeStr = context.getEntityAttribute("batchSize");
		if (batchSizeStr != null) {
			batchSize = Integer.parseInt(batchSizeStr);
		}

		String startStr = context.getEntityAttribute("start");
		if (startStr != null) {
			start = Integer.parseInt(startStr);
			cursor = start;
		}

		String limitStr = context.getEntityAttribute("limit");
		if (limitStr != null) {
			limit = Integer.parseInt(limitStr);
		}

	}

	@Override
	public Map<String, Object> nextRow() {

		if (limit > -1 && cursor >= start + limit) {
			return null;
		}
		if (total > -1 && cursor >= total)
			return null;

		if (rowIterator == null || !rowIterator.hasNext()) {
			doSearch();
		}

		cursor++;

		return getNext();
	}

	protected SolrServer newSolrServer() throws Exception {
		return SearchManager.getServer(targetCore);
	}

	void doSearch() {
		try {
			if (server == null)
				server = newSolrServer();

			SolrQuery query = new SolrQuery();
			query.setQuery(queryText);
			query.setStart(cursor);
			query.setRows(batchSize);

			if (fields != null)
				query.setFields(fields);

			QueryResponse response = server.query(query);
			SolrDocumentList docList = response.getResults();

			if (total <= -1)
				total = docList.getNumFound();

			ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			list.addAll(docList);

			rowIterator = list.iterator();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
