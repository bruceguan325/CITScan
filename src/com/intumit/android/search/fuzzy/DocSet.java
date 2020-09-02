package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TCustomHashMap;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class DocSet implements Cloneable {

	public static DocSet create(int maxDoc) {
		return new DocSet(maxDoc);
	}

	BitSet bitSet;
	private Hashtable<Integer, ScoredDoc> table;
	
	public DocSet(int maxDoc) {
		bitSet = new BitSet(maxDoc);
		table = new Hashtable<Integer, ScoredDoc>();
	}
	
	/**
	 * Set doc into this docset, overwrite if the docid already existed
	 * @param doc
	 */
	public void set(ScoredDoc doc) {
		bitSet.set(doc.doc);
		table.put(doc.doc, doc);
	}
	
	/**
	 * Set doc into this docset, and if doc is already exists, add doc.score to existed one.
	 * @param doc
	 * @return true if already exists, false if not found.
	 */
	public boolean setAndAddScoreIfExists(ScoredDoc doc) {
		if (bitSet.get(doc.doc)) {
			ScoredDoc exist = table.get(doc.doc);
			exist.score += doc.score;
			return true;
		}
		else {
			set(doc);
			return false;
		}
	}

	public boolean setAndMaxScoreIfExists(Output output, float score, char[] path, byte highlightPoint) {
		if (bitSet.get(output.getDocId())) {
			ScoredDoc exist = table.get(output.getDocId());
			if (score > exist.score)
				exist.score = score;
			
			if (exist instanceof DetailScoredDoc) {
				((DetailScoredDoc) exist).addPath(path, highlightPoint, output);
			}
			return true;
		}
		else {
			set( path == null ? new ScoredDoc(output.getDocId(), score) : new DetailScoredDoc(output.getDocId(), score, path, highlightPoint, output));
			return false;
		}
	}

	public boolean setAndAddScoreIfExists(Output output, float score, char[] path, byte highlightPoint) {
		if (bitSet.get(output.getDocId())) {
			ScoredDoc exist = table.get(output.getDocId());
			exist.score += score;
			
			if (exist instanceof DetailScoredDoc) {
				((DetailScoredDoc) exist).addPath(path, highlightPoint, output);
			}
			return true;
		}
		else {
			set( path == null ? new ScoredDoc(output.getDocId(), score) : new DetailScoredDoc(output.getDocId(), score, path, highlightPoint, output));
			return false;
		}
	}

	public Collection<ScoredDoc> docSet() {
		return table.values();
	}
	
	public DocSetIterator iterator() {
		return new DocSetIterator(bitSet, table);
	}

	public int size() {
		return bitSet.cardinality();
	}
	
	public void andOperation(DocSet anotherSet) {
		bitSet.and(anotherSet.bitSet);
		
		int index = -1;
		while ( (index=bitSet.nextSetBit(index+1)) != -1 ) {
			ScoredDoc thisDoc = table.get(index);
			ScoredDoc anotherDoc = anotherSet.table.get(index);
			thisDoc.score += anotherDoc.score;
			
			if (thisDoc instanceof DetailScoredDoc && anotherDoc instanceof DetailScoredDoc) {

				{
				HashMap<Integer, TCustomHashMap<char[], Byte>> toCodeMap = ((DetailScoredDoc)thisDoc).highlightIndexMap;
				HashMap<Integer, TCustomHashMap<char[], Byte>> fromCodeMap = ((DetailScoredDoc)anotherDoc).highlightIndexMap;
				
				for (Integer fromKey: fromCodeMap.keySet()) {
					if (toCodeMap.containsKey(fromKey)) {
						toCodeMap.get(fromKey).putAll(fromCodeMap.get(fromKey));
					}
					else {
						toCodeMap.put(fromKey, fromCodeMap.get(fromKey));
					}
				}
				}
				

				{
				HashMap<Integer, TCustomHashMap<char[], Integer>> toCodeMap = ((DetailScoredDoc)thisDoc).synonymCodeMap;
				HashMap<Integer, TCustomHashMap<char[], Integer>> fromCodeMap = ((DetailScoredDoc)anotherDoc).synonymCodeMap;
				
				for (Integer fromKey: fromCodeMap.keySet()) {
					if (toCodeMap.containsKey(fromKey)) {
						toCodeMap.get(fromKey).putAll(fromCodeMap.get(fromKey));
					}
					else {
						toCodeMap.put(fromKey, fromCodeMap.get(fromKey));
					}
				}
				}
			}
			
		}
	}
	
	public void orOperation(DocSet anotherSet) {
		bitSet.or(anotherSet.bitSet);
		
		int index = -1;
		while ( (index=anotherSet.bitSet.nextSetBit(index+1)) != -1 ) {
			ScoredDoc thisDoc = table.get(index);
			ScoredDoc anotherDoc = anotherSet.table.get(index);
			
			if (thisDoc == null) {
				try {
					table.put(index, (ScoredDoc)anotherDoc.clone());
				} catch (CloneNotSupportedException e) {
					throw new RuntimeException("Fatal error when cloning ScoredDoc:" + e.getMessage());
				}
			}
			else {
				thisDoc.score += anotherDoc.score;
				
				if (thisDoc instanceof DetailScoredDoc && anotherDoc instanceof DetailScoredDoc) {
					{
					HashMap<Integer, TCustomHashMap<char[], Byte>> toCodeMap = ((DetailScoredDoc)thisDoc).highlightIndexMap;
					HashMap<Integer, TCustomHashMap<char[], Byte>> fromCodeMap = ((DetailScoredDoc)anotherDoc).highlightIndexMap;
					
					for (Integer fromKey: fromCodeMap.keySet()) {
						if (toCodeMap.containsKey(fromKey)) {
							toCodeMap.get(fromKey).putAll(fromCodeMap.get(fromKey));
						}
						else {
							toCodeMap.put(fromKey, fromCodeMap.get(fromKey));
						}
					}
					}
					
	
					{
					HashMap<Integer, TCustomHashMap<char[], Integer>> toCodeMap = ((DetailScoredDoc)thisDoc).synonymCodeMap;
					HashMap<Integer, TCustomHashMap<char[], Integer>> fromCodeMap = ((DetailScoredDoc)anotherDoc).synonymCodeMap;
					
					for (Integer fromKey: fromCodeMap.keySet()) {
						if (toCodeMap.containsKey(fromKey)) {
							toCodeMap.get(fromKey).putAll(fromCodeMap.get(fromKey));
						}
						else {
							toCodeMap.put(fromKey, fromCodeMap.get(fromKey));
						}
					}
					}
				}
			}
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DocSet cloned = (DocSet)super.clone();
		cloned.bitSet = (BitSet)bitSet.clone();
		cloned.table = new Hashtable<Integer, ScoredDoc>();
		for (Map.Entry<Integer, ScoredDoc> entry: table.entrySet()) {
			cloned.table.put(entry.getKey(), (ScoredDoc)entry.getValue().clone());
		}
		return cloned;
	}
}
