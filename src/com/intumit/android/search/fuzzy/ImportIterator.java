package com.intumit.android.search.fuzzy;

interface ImportIterator {

	/**
	 * for iterate all data for import
	 * @return null if end of data
	 */
	public String next();
	
	
	public void close() throws Exception;
}