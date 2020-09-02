package com.intumit.solr.robot.dictionary;

public interface EmbeddedFuzzyDictionaryDatabaseChangeListener {
	public void save(EmbeddedFuzzyDictionaryDatabase efd);
	public void delete(EmbeddedFuzzyDictionaryDatabase efd);
	public void update(EmbeddedFuzzyDictionaryDatabase efd, EmbeddedFuzzyDictionaryDatabase oldEfd);
}
