package com.intumit.solr.robot.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DictionaryDatabaseChange4CacheReloadListener implements
DictionaryDatabaseChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);
	
	@Override
	public void update(DictionaryDatabase oldWord, DictionaryDatabase newWord) {
		DictionaryDatabase.clearCache(newWord.getTenantId());
		LOG.info("DictionaryDatabase updated, reload cache [" + oldWord + " -> " + newWord + "]");
	}

	@Override
	public void save(DictionaryDatabase word) {
		DictionaryDatabase.clearCache(word.getTenantId());
		LOG.info("DictionaryDatabase save, reload cache [" + word + "]");
	}

	@Override
	public void delete(DictionaryDatabase word) {
		DictionaryDatabase.clearCache(word.getTenantId());
		LOG.info("DictionaryDatabase delete, reload cache [" + word + "]");
	}

}
