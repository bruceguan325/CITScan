package com.intumit.android.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.intumit.android.search.fuzzy.ScoredDoc;

public class ResultSet extends BasicResultSet {
	Collection<ScoredDoc> docs;
	public ResultSet() {
		hasMore = false;
		timeCost = 0;
		docs = null;
	}
	ResultSet(Collection<ScoredDoc> docs, long timeCost, boolean hasMore) {
		super();
		this.docs = docs;
		this.timeCost = timeCost;
		this.hasMore = hasMore;
	}
	
	/**
	 * 取得所有符合的ScoredDoc
	 * 
	 * @return
	 */
	public Collection<ScoredDoc> getScoredDocs() {
		return docs;
	}
	protected void setScoredDocs(Collection<ScoredDoc> docs) {
		this.docs = docs;
	}
	
}
