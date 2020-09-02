package com.intumit.android.search.fuzzy;

import org.apache.lucene.search.ScoreDoc;

public class ScoredDoc extends ScoreDoc implements Cloneable {

	public ScoredDoc(int internalId, float score) {
		super(internalId, score);
	}

	/**
	 * Get Primary Key of Document
	 * @return
	 */
	public int getInternalId() {
		return doc;
	}
	
	public float getScore() {
		return score;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + doc;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScoredDoc other = (ScoredDoc) obj;
		if (doc != other.doc) return false;
		return true;
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		ScoredDoc cloned = (ScoredDoc)super.clone();
		return cloned;
	}
}
