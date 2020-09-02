package org.apache.solr.handler.dataimport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.EntityProcessorBase;

import com.intumit.solr.SearchManager;

public class IndexEntityProcessor extends EntityProcessorBase {
	int start = 0;
	long total = -1;
	int batchSize = 10;
	String queryText = null;
	String targetCore = null;
	int max = 10;

	@Override
	public void init(Context context) {
		super.init(context);
	}

	@Override
	public Map<String, Object> nextRow() {
		if (rowIterator == null) {
			queryText = context.getEntityAttribute("query");
			targetCore = context.getEntityAttribute("targetCore");
			String batchSizeStr = context.getEntityAttribute("batchSize");

			if (batchSizeStr != null) {
				batchSize = Integer.parseInt(batchSizeStr);
			}

			doSearch();
		} else {
			if (rowIterator.hasNext()) {
				return getNext();
			} else {
				doSearch();
			}
		}
		return getNext();
	}

	void doSearch() {
		if (total != -1 && start >= (total - 1))
			return;

		SolrServer server = SearchManager.getServer(targetCore);
		SolrQuery query = new SolrQuery();
		query.setQuery(queryText);
		query.setStart(start);
		query.setRows(batchSize);

		try {
			QueryResponse response = server.query(query);
			SolrDocumentList docList = response.getResults();
			total = docList.getNumFound();
			ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

			for (Iterator<SolrDocument> itr = docList.iterator(); itr.hasNext(); start++) {
				SolrDocument doc = itr.next();
				list.add(doc);
			}
			rowIterator = list.iterator();
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Map<String, Object> nextModifiedRowKey() {
		return nextRow();
	}

	@Override
	public Map<String, Object> nextDeletedRowKey() {
		return null;
	}

	@Override
	public Map<String, Object> nextModifiedParentRowKey() {
		return nextRow();
	}
}
