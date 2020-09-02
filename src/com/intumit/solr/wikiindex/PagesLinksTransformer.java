package com.intumit.solr.wikiindex;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class PagesLinksTransformer extends Transformer {

	@SuppressWarnings("unchecked")
	@Override
	public Object transformRow(Map<String, Object> row, Context context) {

		// SolrServer設定查詢的core代號
		String targetCore = context.getEntityAttribute("targetCore");
		SolrServer server = SearchManager.getServer(targetCore);

		try {

			String title = (String) row.get("Name_s");

			// row.put("id", row.get("id"));
			// row.put("Name_s", title);
			// PAGES
			SolrQuery pagesQ = new SolrQuery().setQuery(
					"Description_mt" + ":\"" + title + "\"").setRows(0);
			long pagesi = server.query(pagesQ).getResults().getNumFound();
			row.put("Pages_i", pagesi);
			// END

			// LINKS
			String lq = "LinkTo_ms" + ":\"" + title + "\"";
			SolrQuery linkQ = new SolrQuery().setQuery(lq).setFields("Name_s")
					.setRows(500);
			SolrDocumentList results = server.query(linkQ).getResults();
			long linki = results.getNumFound();
			row.put("LinkFrom_i", linki);
			// END

			// LINK_FROM_MS
			Set<String> linkfrom_ms = new HashSet<String>();
			for (SolrDocument doc : results) {
				linkfrom_ms.add((String) doc.getFieldValue("Name_s"));
			}
			row.put("LinkFrom_ms", linkfrom_ms);
			// END

			// LINK_TO_MS
			Set<String> linkto_ms = new HashSet<String>();
			Object lms = row.get("LinkTo_ms");
			if (lms instanceof String) {
				linkto_ms.add((String) lms);
			} else if (lms instanceof List) {
				linkto_ms.addAll((List) lms);
			}

			row.put("LinkTo_ms", linkto_ms);
			row.put("LinkTo_i", linkto_ms.size());
			// END

			// CO_LINK_MS no usage
			// Collection intersection =
			// CollectionUtils.intersection(linkfrom_ms,
			// linkto_ms);
			// row.put("CoLink_ms", intersection);
			// row.put("CoLink_i", intersection.size());
			// END

			// ALL_LINK_MS
			Set<String> all = new HashSet<String>(linkfrom_ms);
			all.addAll(linkto_ms);
			row.put("AllLink_ms", all);
			row.put("AllLink_i", all.size());
			// END

			// Score
			double numerator = (linki)
			// * Math.sqrt(1)
			;
			double denominator = Math.pow(pagesi, 1.5) + 1;
			double adjustScore = numerator / denominator;
			row.put("Score_i", (int) (adjustScore * 10000));
			// END

		} catch (Exception e) {
			e.printStackTrace();
		}
		return row;
	}
}
