package com.intumit.solr.wikiindex;

import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.solr.SearchManager;

/**
 * A handy tool Use sum="true" to enable
 */
public class CloneRedirectContentTransformer extends Transformer {

	@Override
	public Object transformRow(Map<String, Object> row, Context context) {

		String targetCore = context.getEntityAttribute("targetCore");
		SolrServer server = SearchManager.getServer(targetCore);

		try {

			String id = (String) row.get("id");

			SolrQuery pagesQ = new SolrQuery().setQuery(
					"Name_s" + ":\"" + (String) row.get("Redirect_s") + "\"")
					.setRows(1);

			SolrDocumentList results = server.query(pagesQ).getResults();
			if (results.getNumFound() > 0) {
				SolrDocument doc = results.get(0);
				doc.put("id", id);
				doc.put("Name_s", row.get("Name_s"));

				row.putAll(doc);
				// SolrInputDocument inputDoc = new SolrInputDocument();
				//
				// for (Entry<String, Object> entry : doc) {
				// inputDoc.addField(entry.getKey(), entry.getValue());
				// }
				//
				// server.add(inputDoc);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return row;
	}
}
