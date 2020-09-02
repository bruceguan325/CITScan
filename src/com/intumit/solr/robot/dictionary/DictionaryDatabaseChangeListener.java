package com.intumit.solr.robot.dictionary;

public interface DictionaryDatabaseChangeListener {
	public void update(DictionaryDatabase oldSyn, DictionaryDatabase newSyn);
	public void save(DictionaryDatabase syn);
	public void delete(DictionaryDatabase syn);
}
