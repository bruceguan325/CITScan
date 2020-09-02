package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intumit.android.search.Constants;

public class DetailScoredDoc extends ScoredDoc implements Cloneable {
/*	ArrayList<char[]> paths;
	TByteArrayList highlightPoint;*/
	
	HashMap<Integer, TCustomHashMap<char[], Byte>> highlightIndexMap = new HashMap<Integer, TCustomHashMap<char[], Byte>>();
	HashMap<Integer, TCustomHashMap<char[], Integer>> synonymCodeMap = new HashMap<Integer, TCustomHashMap<char[], Integer>>();
	
			//new TCustomHashMap<char[], Byte>(new CharArrayStrategy());

	public DetailScoredDoc(int doc, float score, char[] path, byte highlightPoint, Output output) {
		super(doc, score);
		/*this.paths = new ArrayList<char[]>();
		this.highlightPoint = new TByteArrayList();
		paths.add(path);
		this.highlightPoint.add(highlightPoint);
		*/
		
		TCustomHashMap<char[], Byte> cbMap = new TCustomHashMap<char[], Byte>(new CharArrayStrategy());
		cbMap.put(path, highlightPoint);
		highlightIndexMap.put(output.getFieldCode(), cbMap);
		
		if (output.synonymCode != Output.NOT_SYNONYM) {
			TCustomHashMap<char[], Integer> ciMap = new TCustomHashMap<char[], Integer>(new CharArrayStrategy());
			ciMap.put(path, output.synonymCode);
			synonymCodeMap.put(output.getFieldCode(), ciMap);
		}
		
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
		DetailScoredDoc other = (DetailScoredDoc) obj;
		if (doc != other.doc) return false;
		return true;
	}
	
	void addPath(char[] path, byte highlightIndex, Output output) {
		/*paths.add(path);
		highlightPoint.add(hps);*/
		
		if (highlightIndexMap.containsKey(output.getFieldCode())) {
			TCustomHashMap<char[], Byte> cbMap = highlightIndexMap.get(output.getFieldCode());
			
			if (cbMap.contains(path)) {
				byte inMap = cbMap.get(path);
				
				if (inMap < highlightIndex) {
					cbMap.put(path, highlightIndex);
				}
			}
			else {
				cbMap.put(path, highlightIndex);
			}
		}
		else {
			TCustomHashMap<char[], Byte> ibMap = new TCustomHashMap<char[], Byte>(new CharArrayStrategy());
			ibMap.put(path, highlightIndex);
			highlightIndexMap.put(output.getFieldCode(), ibMap);
		}
		
		if (output.synonymCode != Output.NOT_SYNONYM) {
			if (synonymCodeMap.containsKey(output.getFieldCode())) {
				TCustomHashMap<char[], Integer> ciMap = synonymCodeMap.get(output.getFieldCode());
				
				if (ciMap.contains(path)) {
					if (Constants.DEBUG)
						System.out.println("The same path in the same field: [" + output.getFieldCode() + "] path [" + new String(path) + "] mappingCode [" + output.synonymCode);
					
					Integer inMap = ciMap.get(path);
					
					if (inMap != output.synonymCode) {
						ciMap.put(path, output.synonymCode);
					}
				}
				else {
					ciMap.put(path, output.synonymCode);
				}
			}
			else {
				TCustomHashMap<char[], Integer> ciMap = new TCustomHashMap<char[], Integer>(new CharArrayStrategy());
				ciMap.put(path, output.synonymCode);
				synonymCodeMap.put(output.getFieldCode(), ciMap);
			}
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DetailScoredDoc cloned = (DetailScoredDoc)super.clone();
		cloned.highlightIndexMap.putAll(highlightIndexMap);
		cloned.synonymCodeMap.putAll(synonymCodeMap);
		return cloned;
	}
	
	/**
	 * Get suggested keyword for specific field
	 * 
	 * @param fieldCode
	 * @return
	 */
	public List<String> getSuggest(int fieldCode) {
		List<String> suggested = new ArrayList<String>();
		
		if (highlightIndexMap.containsKey(fieldCode)) {
			TCustomHashMap<char[], Byte> ibMap = highlightIndexMap.get(fieldCode);
			
			for (char[] path: ibMap.keySet()) {
				suggested.add(new String(path));
			}
		}
		
		return suggested;
	}
	
	
	TCustomHashSet<char[]> getPaths() {
		TCustomHashSet<char[]> set = new TCustomHashSet<char[]>(new CharArrayStrategy());
		
		for (Map.Entry<Integer, TCustomHashMap<char[], Byte>> ibMap: highlightIndexMap.entrySet()) {
			set.addAll(ibMap.getValue().keySet());
		}
		
		return set;
	}
	
	class CharArrayStrategy implements HashingStrategy {
        public int computeHashCode(Object o) {
            char[] c = (char[])o;
            // use the shift-add-xor class of string hashing functions
            // cf. Ramakrishna and Zobel,
            //     "Performance in Practice of String Hashing Functions"
            int h = 31; // seed chosen at random
            for (int i = 0; i < c.length; i++) { // could skip invariants
                // L=5, R=2 works well for ASCII input
                h = h ^ ((h << 5) + (h >> 2) + c[i]);
            }
            return h;
        }

        public boolean equals(Object o1, Object o2) {
            char[] c1 = (char[])o1;
            char[] c2 = (char[])o2;
            // could drop this check for fixed-length keys
            if (c1.length != c2.length) {
                return false;
            }
            // could skip invariants
            for (int i = 0, len = c1.length; i < len; i++) {
                if (c1[i] != c2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

	HashMap<Integer, TCustomHashMap<char[], Byte>> getPathAndHighlightPoints() {
		return highlightIndexMap;
	}

	HashMap<Integer, TCustomHashMap<char[], Integer>> getPathAndSynonymCodes() {
		return synonymCodeMap;
	}
	
	private String mapToString(Map<Integer, TCustomHashMap<char[], Integer>> map) {
		StringBuilder sb = new StringBuilder();
		
		for (Integer key: map.keySet()) {
			sb.append("[key=" + key + ",\n");
			TCustomHashMap<char[], Integer> subMap = map.get(key);
			for (char[] path: subMap.keySet()) {
				sb.append("   path=" + new String(path) + ":" + subMap.get(path) + "\n");
			}
		}
		
		sb.append("]\n");
		
		return sb.toString();
	}
	
	private String mapToString2(Map<Integer, TCustomHashMap<char[], Byte>> map) {
		StringBuilder sb = new StringBuilder();
		
		for (Integer key: map.keySet()) {
			sb.append("  [key=" + key + ",\n");
			TCustomHashMap<char[], Byte> subMap = map.get(key);
			for (char[] path: subMap.keySet()) {
				sb.append("   path=" + new String(path) + ":" + subMap.get(path) + "\n");
			}
		}
		
		sb.append("  ]\n");
		
		return sb.toString();
	}

	@Override
	public String toString() {
		return "TrieScoreDocWithPath [\n highlightIndexMap=\n" + mapToString2(highlightIndexMap)
				+ "\n mappingCodeMap=\n" + mapToString(synonymCodeMap) + "\n score=" + score
				+ ", doc=" + doc + ", shardIndex=" + shardIndex + "]";
	}
	
	
}
