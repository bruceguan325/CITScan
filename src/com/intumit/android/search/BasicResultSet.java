package com.intumit.android.search;

import java.util.List;
import java.util.Map;

class BasicResultSet {
	List<Map<String, String>> docs;
	long timeCost;
	boolean hasMore;
	public BasicResultSet() {
		hasMore = false;
		timeCost = 0;
		docs = null;
	}
	public BasicResultSet(List<Map<String, String>> docs, long timeCost, boolean hasMore) {
		super();
		this.docs = docs;
		this.timeCost = timeCost;
		this.hasMore = hasMore;
	}
	public List<Map<String, String>> getDocs() {
		return docs;
	}
	public long getTimeCost() {
		return timeCost;
	}
	public boolean isHasMore() {
		return hasMore;
	}
	public void setDocs(List<Map<String, String>> docs) {
		this.docs = docs;
	}
	public void setTimeCost(long timeCost) {
		this.timeCost = timeCost;
	}
	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}
	
}
