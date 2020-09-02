package com.intumit.android.search.fuzzy;

import java.util.BitSet;
import java.util.Hashtable;

public class DocSetIterator {
	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;
	private Hashtable<Integer, ScoredDoc> table;
	private BitSet bitSet;
	private int docId;

	protected DocSetIterator(BitSet bitSet, Hashtable<Integer, ScoredDoc> table) {
		this.bitSet = bitSet;
		this.table = table;
		this.docId = -1;
	}

	public ScoredDoc nextDoc() {
		// (docId + 1) on next line requires -1 initial value for docNr:
		if (docId == NO_MORE_DOCS)
			return null;
		
		int d = bitSet.nextSetBit(docId + 1);
		// -1 returned by BitSet.nextSetBit() when exhausted
		docId = d == -1 ? NO_MORE_DOCS : d;
		return table.get(docId);
	}
}
