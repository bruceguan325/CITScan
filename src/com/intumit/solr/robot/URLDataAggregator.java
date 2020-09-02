package com.intumit.solr.robot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.VelocityContext;

import com.intumit.solr.robot.dictionary.CustomData;

public class URLDataAggregator extends QADataAggregator {

	@Override
	public void aggregate(QA customQa, QAContext qaCtx, QAPattern qp, List<CustomData> nvPairs, VelocityContext context) {
		
		try {
			AtomicBoolean autoCompleted = new AtomicBoolean(false);
			SolrQuery thisQuery = this.generateQuery(customQa, qp, qaCtx, nvPairs, autoCompleted);
			QueryResponse res = getDataSourceServer(qp.getDataSource(), qaCtx.getTenant()).query(thisQuery);
			SolrDocumentList docs = res.getResults();

			context.put("docs", docs);
			context.put("autoCompleted", autoCompleted);
		}
		catch (SolrServerException e) {
			e.printStackTrace();
		}
	}

}
