package com.intumit.solr.synonymKeywords;

public interface SynonymKeywordChangeListener {
	public void update(SynonymKeyword oldSyn, SynonymKeyword newSyn);
	public void save(SynonymKeyword syn);
	public void delete(SynonymKeyword syn);
}
