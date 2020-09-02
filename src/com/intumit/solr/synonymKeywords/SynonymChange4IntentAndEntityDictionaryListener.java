package com.intumit.solr.synonymKeywords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.entity.QAEntityDictionary;
import com.intumit.solr.robot.intent.QAIntentDictionary;

public class SynonymChange4IntentAndEntityDictionaryListener implements
		SynonymKeywordChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);
	
	@Override
	public void update(SynonymKeyword oldSyn, SynonymKeyword newSyn) {
		QAIntentDictionary.clear(newSyn.getTenantId());
		QAEntityDictionary.clear(newSyn.getTenantId());
		LOG.info("SynonymKeyword updated, reload intent&entity dictionary [" + oldSyn + " -> " + newSyn + "]");
	}

	@Override
	public void save(SynonymKeyword syn) {
		QAIntentDictionary.clear(syn.getTenantId());
		QAEntityDictionary.clear(syn.getTenantId());
		LOG.info("SynonymKeyword save, reload intent&entity dictionary [" + syn + "]");
	}

	@Override
	public void delete(SynonymKeyword syn) {
		QAIntentDictionary.clear(syn.getTenantId());
		QAEntityDictionary.clear(syn.getTenantId());
		LOG.info("SynonymKeyword delete, reload intent&entity dictionary [" + syn + "]");
	}

}
