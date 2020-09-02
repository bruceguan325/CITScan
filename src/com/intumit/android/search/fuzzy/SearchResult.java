package com.intumit.android.search.fuzzy;

import java.util.Arrays;
import java.util.Set;

/**
 * <p>
 * Holds the result of the search so far. Includes the outputs where the search
 * finished as well as the last index of the matching.
 * </p>
 * 
 * <p>
 * (Internally, it also holds enough state to continue a running search, though
 * this is not exposed for public use.)
 * </p>
 */
public class SearchResult {
	State lastMatchedState;
	char[] chars;
	int lastIndex;
	protected int allowDistance;

	SearchResult(State s, char[] bs, int i, int d) {
		this.lastMatchedState = s;
		this.chars = bs;
		this.lastIndex = i;
		this.allowDistance = d;
	}

	/**
	 * Returns a list of the outputs of this match.
	 */
	public Set<Output> getOutputs() {
		return lastMatchedState.getOutputs();
	}

	/**
	 * Returns the index where the search terminates. Note that this is one byte
	 * after the last matching character.
	 */
	public int getLastIndex() {
		return lastIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(chars);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SearchResult other = (SearchResult) obj;
		if (!Arrays.equals(chars, other.chars)) return false;
		return true;
	}

	public int getAllowDistance() {
		return allowDistance;
	}

	public String pathToString() {
		return new String(chars);
	}
}
