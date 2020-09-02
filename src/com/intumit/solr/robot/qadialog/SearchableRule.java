package com.intumit.solr.robot.qadialog;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;

public interface SearchableRule {
	public boolean applySearchCriteria(RuleCheckResult result, SolrQuery query);
	public boolean applySearchCriteria(RuleCheckResult result, SolrDocumentList docs);
}
