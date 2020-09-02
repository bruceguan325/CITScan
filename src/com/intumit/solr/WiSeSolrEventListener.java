package com.intumit.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class WiSeSolrEventListener implements SolrEventListener {
	private static final Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
	
	public WiSeSolrEventListener() {
		super();
	}

	@Override
	public void init(NamedList arg0) {
		LOG.info("Fire SolrEventListener.init() in ClusterMembershipListener.");
		ClusterMembershipListener.getInstance().checkAndSwitchMode();
	}


	@Override
	public void newSearcher(SolrIndexSearcher arg0, SolrIndexSearcher arg1) {
		LOG.info("Fire SolrEventListener.newSearcher() in ClusterMembershipListener.");
		ClusterMembershipListener.getInstance().checkAndSwitchMode();
	}


	@Override
	public void postCommit() {
		LOG.debug("Fire SolrEventListener.postCommit() in ClusterMembershipListener.");
	}


	@Override
	public void postSoftCommit() {
		LOG.debug("Fire SolrEventListener.postSoftCommit() in ClusterMembershipListener.");
	}

	/*public String getReplicationMasterUrl() {
		System.out.println("solrconfig [" + shardCoreName + "] / :"
				+ core.getRequestHandlers().get("/replication"));
		org.apache.solr.handler.ReplicationHandler rh = (org.apache.solr.handler.ReplicationHandler) core
				.getRequestHandlers().get("/replication");
		System.out.println(rh.getInitArgs());
		try {
			System.out.println(((org.apache.solr.common.util.NamedList) rh
					.getInitArgs().get("slave")).get("masterUrl"));
		} catch (Exception ignore) {
		}
	}*/
}
