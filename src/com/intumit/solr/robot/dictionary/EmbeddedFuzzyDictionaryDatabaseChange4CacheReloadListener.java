package com.intumit.solr.robot.dictionary;

public class EmbeddedFuzzyDictionaryDatabaseChange4CacheReloadListener
		implements EmbeddedFuzzyDictionaryDatabaseChangeListener {

	@Override
	public void save(EmbeddedFuzzyDictionaryDatabase efd) {
		EmbeddedFuzzyDictionaryDatabase.clearCache(efd.getTenantId());
	}

	@Override
	public void delete(EmbeddedFuzzyDictionaryDatabase efd) {
		EmbeddedFuzzyDictionaryDatabase.clearCache(efd.getTenantId());
	}

	@Override
	public void update(EmbeddedFuzzyDictionaryDatabase efd, EmbeddedFuzzyDictionaryDatabase oldEfd) {
		EmbeddedFuzzyDictionaryDatabase.clearCache(efd.getTenantId());
	}

}
