package com.intumit.solr.synonymKeywords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynonymChange4CacheReloadListener implements
		SynonymKeywordChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);
	
	@Override
	public void update(SynonymKeyword oldSyn, SynonymKeyword newSyn) {
		SynonymKeywordFacade.getInstance().reloadCache(newSyn.getTenantId());
		LOG.info("SynonymKeyword updated, reload cache [" + oldSyn + " -> " + newSyn + "]");
	}

	@Override
	public void save(SynonymKeyword syn) {
		SynonymKeywordFacade.getInstance().reloadCache(syn.getTenantId());
		LOG.info("SynonymKeyword save, reload cache [" + syn + "]");
	}

	@Override
	public void delete(SynonymKeyword syn) {
		SynonymKeywordFacade.getInstance().reloadCache(syn.getTenantId());
		LOG.info("SynonymKeyword delete, reload cache [" + syn + "]");
	}

}
