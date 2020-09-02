package com.intumit.android.search.fuzzy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

import com.intumit.android.search.FuzzyQueryOption;
import com.intumit.android.search.util.Statistics;

public abstract class AhoCorasick {

	public static final byte MAX_DEPTH = 100;
	public static boolean DEBUG = false;

	public abstract void delete(int output);

	public abstract DocSet fuzzySearch(char[] chars, FuzzyQueryOption option);
	
	public abstract DocSet fuzzySearch(DocSet docSet, char[] chars, FuzzyQueryOption option);

	public abstract DocSet search(char[] bytes);

	protected abstract DocSet startFuzzySearch(DocSet docSet, char[] chars, FuzzyQueryOption option);
	
	public abstract void edit();

	public abstract void prepare();

	protected boolean prepared;
	int minCharValue = 32;
	protected int maxCharValue;
	protected boolean buildReverseMap;
	protected Scorer scorer = new Scorer();
	protected BitSet deleteBS = null;
	protected boolean includePathInScoreDoc = false;
	protected boolean enablePhoneticMode = false;
	protected PhoneticMap pmap = new DefaultPhoneticMap();
	State root;
	
	public static void printTrie(State root) {
		System.out.println("***");
		printTrieRecursive(root, 0);
	}

	public boolean isIncludePathInScoreDoc() {
		return includePathInScoreDoc;
	}

	public void setIncludePathInScoreDoc(boolean includePathInScoreDoc) {
		this.includePathInScoreDoc = includePathInScoreDoc;
	}

	public boolean isEnablePhoneticMode() {
		return enablePhoneticMode;
	}

	public void setEnablePhoneticMode(boolean enablePhoneticMode) {
		this.enablePhoneticMode = enablePhoneticMode;
	}
	
	protected void setPhoneticMap(PhoneticMap pmap) {
		this.pmap = pmap;
	}

	public boolean isPrepared() {
		return prepared;
	}

	public void printTrie() {
		System.out.println("*** STATE_ID_SEED:" + State._STATE_ID_SEED + " ***");
		printTrieRecursive(root, 0);
		
	}

	static void printTrieRecursive(State state, int depth) {
		char[] keys = state.keys();

		System.out.print("[" + StringUtils.leftPad("" + depth, 2, '0') + "]");
		if (state.hasOutput()) {
			System.out.println("<" + state.getOutputs() + ">");
		}
		for (int i = 0; i < keys.length; i++) {
			for (int k=0; k < depth; k++) System.out.print(" - ");
			char c = keys[i];
			
			State childState = state.get(c);
			System.out.println(" " + c + " " + "[ID:" + childState.id + "]");
			
			printTrieRecursive(childState, depth + 1);
		}
		
	}

	/**
	 * by Herb
	 * Adds a new keyword with the given output. During search, if the keyword
	 * is matched, output will be one of the yielded elements in SearchResults.getOutputs().
	 * 
	 * This can be used as a reverse index, output can store document ID, then we can use the trie as a simple search engine.
	 */
	protected Hashtable<Output, Object> reverseMap = new Hashtable<Output, Object>();

	/**
	 * Returns the root of the tree. Package protected, since the user probably
	 * shouldn't touch this.
	 */
	protected State getRoot() {
		return this.root;
	}

	/**
	 * Used for dump all node (the ones has output.length > 0)
	 * 
	 * @return
	 */
	public List<SearchResult> dumpAllPaths() {
		List<SearchResult> pool = new ArrayList<SearchResult>();
		fetchAllChildrenLeaf(new char[0], root, pool);
		return pool;
	}

	/**
	 * Traverse and fetch all node contains result (which is output.length > 0)
	 * 
	 * @param path the path from root, represent as a character array
	 * @param startFromHere start from this node
	 * @param pool the result pool, used recursively
	 */
	void fetchAllChildrenLeaf(final char[] path, State startFromHere, List<SearchResult> pool) {
		char[] keys = startFromHere.keys();
		
		for (char key: keys) {
			char[] newPath = Arrays.copyOf(path, path.length + 1);
			newPath[newPath.length - 1] = key;

			State state = startFromHere.get(key);
			if (state.hasOutput()) {
				for (Output output: state.getOutputs()) {
					pool.add(new SearchResult(state, newPath, -1, -1));
				}
			}
			
			fetchAllChildrenLeaf(newPath, state, pool);
		}
	}
	
	protected int maxDocId = 0;
	protected int entryCount = 0;

	protected static String simpleAnalyzer(String keyword) {
		StringBuffer sb = new StringBuffer();
		boolean isLastCharLatin = false;
		char[] chars = keyword.toCharArray();
		for (int i=0; i < chars.length; i++) {
			char c = chars[i];
			Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
			boolean isLatin = 
				(ub != Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				&& ub != Character.UnicodeBlock.CJK_COMPATIBILITY
				&& ub != Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
				&& ub != Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
				&& ub != Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				&& ub != Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				&& ub != Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
						);
			
			if (i != 0 && (isLatin ^ isLastCharLatin)) {
				sb.append("_");
			}
			
			if (isLatin) {
				if (' ' == c) {
					c = '_';
				}
				c = Character.toLowerCase(c);
			}
			sb.append(c);
			isLastCharLatin = isLatin;
		}
		
		return sb.toString();
	}

	public int getMaxDocId() {
		return maxDocId;
	}

	public boolean addString(String keyword, Output output) {
		//String token = keyword.toLowerCase().replaceAll(" ", "_");
		//String token = simpleAnalyzer(keyword);
		
		// Just a small hack, should be done in Analyzer
		/*if (StringUtils.trimToEmpty(keyword).length() < 2)
			return false;*/
		
		long begin = System.currentTimeMillis();
		add(keyword.toCharArray(), output);
		Statistics.AHOCORASIC_ADD_TC += System.currentTimeMillis() - begin;
		
		if (buildReverseMap) {
			if (reverseMap.containsKey(output)) {
				Object obj = reverseMap.get(output);
				if (obj instanceof Set) {
					((Set)obj).add(keyword);
				}
				else if (obj instanceof String) {
					HashSet<String> set = new HashSet<String>();
					set.add((String)obj);
					set.add(keyword);
					reverseMap.put(output, set);
				}
			}
			else {
				reverseMap.put(output, keyword);
			}
		}
		entryCount++;
		return true;
	}

	public boolean addString(String keyword, Set<Output> outputs) {
		String token = keyword.toLowerCase().replaceAll(" ", "_");
		//String token = simpleAnalyzer(keyword);
		
		// Just a small hack, should be done in Analyzer
		/*if (StringUtils.trimToEmpty(token).length() < 2)
			return false;*/
		
		add(token.toCharArray(), outputs);
		
		if (buildReverseMap) {
			for (Output output: outputs) {
				if (reverseMap.containsKey(output)) {
					Object obj = reverseMap.get(output);
					if (obj instanceof Set) {
						((Set)obj).add(keyword);
					}
					else if (obj instanceof String) {
						HashSet<String> set = new HashSet<String>();
						set.add((String)obj);
						set.add(keyword);
						reverseMap.put(output, set);
					}
				}
				else {
					reverseMap.put(output, keyword);
				}
			}
		}
		entryCount++;
		return true;
	}

	public int getEntryCount() {
		return entryCount;
	}

	public Set<String> mapOutput(int output) {
		if (!buildReverseMap) throw new RuntimeException("Build reverse map set to false! No mapped output.");
		
		Object obj = reverseMap.get(output);
		
		if (obj == null)
			return null;
		
		if (obj instanceof Set) {
			return (Set<String>)obj;
		}
		else if (obj instanceof String){
			HashSet<String> set = new HashSet<String>();
			set.add((String)obj);
			return set;
		}
		return null;
	}

	/**
	 * Adds a new keyword with the given output. During search, if the keyword
	 * is matched, output will be one of the yielded elements in
	 * SearchResults.getOutputs().
	 */
	protected void add(char[] keyword, Output output) {
		if (this.prepared)
			throw new IllegalStateException(
					"can't add keywords after prepare() is called");
	
		try {
			long begin = System.currentTimeMillis();
			this.root.extendAllAndAddOutput(keyword, output);
			Statistics.AHOCORASIC_BUILDING_TC += System.currentTimeMillis() - begin;
			
			if (maxDocId < output.getDocId()) {
				maxDocId = output.getDocId();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//this.printTrie();
		}
	}

	protected void add(char[] keyword, Set<Output> outputs) {
		if (this.prepared)
			throw new IllegalStateException(
					"can't add keywords after prepare() is called");
	
		long begin = System.currentTimeMillis();
		this.root.extendAllAndAddAllOutputs(keyword, outputs);
		Statistics.AHOCORASIC_BUILDING_TC += System.currentTimeMillis() - begin;
		
		for (Output output: outputs) {
			maxDocId = Math.max(maxDocId, output.getDocId());
		}
	}

	public AhoCorasick() {
		super();
	}

	public void setScorer(Scorer scorer) {
		this.scorer = scorer;
	}

}
