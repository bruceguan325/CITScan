package com.intumit.android.search.fuzzy;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;
import com.intumit.android.search.Constants;
import com.intumit.android.search.FuzzyQueryOption;

/**
 * <p>
 * An implementation of the Aho-Corasick string searching automaton. This
 * implementation of the <a
 * href="http://portal.acm.org/citation.cfm?id=360855&dl=ACM&coll=GUIDE"
 * target="_blank">Aho-Corasick</a> algorithm is optimized to work with bytes.
 * </p>
 * 
 * <p>
 * Example usage: <code><pre>
       AhoCorasick tree = new AhoCorasick();
       tree.add("hello".getBytes(), "hello");
       tree.add("world".getBytes(), "world");
       tree.prepare();

       Iterator searcher = tree.search("hello world".getBytes());
       while (searcher.hasNext()) {
           SearchResult result = searcher.next();
           System.out.println(result.getOutputs());
           System.out.println("Found at index: " + result.getLastIndex());
       }
   </pre></code>
 * </p>
 * 
 * <h2>Recent changes</h2>
 * <ul>
 * 
 * <li>Per user request from Carsten Kruege, I've changed the signature of
 * State.getOutputs() and SearchResults.getOutputs() to Sets rather than Lists.</li>
 * 
 * </ul>
 */
public class StateAhoCorasick extends AhoCorasick {
	
	protected StateAhoCorasick(int maxCharValue) {
		this(maxCharValue, false);
	}

	protected StateAhoCorasick(int maxCharValue, boolean buildReverseMap) {
		this.root = new State(); 
		this.maxCharValue = maxCharValue;
		this.prepared = false;
		this.buildReverseMap = buildReverseMap;
	}
	
	/**
	 * Prepares the automaton for searching. This must be called before any
	 * searching().
	 */
	@Override
	public void prepare() {
		//this.prepareFailTransitions();
		this.prepared = true;
		if (deleteBS != null) {
			BitSet newOne = new BitSet(maxDocId);
			newOne.or(deleteBS);
			deleteBS = newOne;
		}
		else {
			deleteBS = new BitSet(maxDocId);
		}
	}
	
	@Override
	public void edit() {
		this.prepared = false;
	}
	
	/**
	 * Starts a new search, and returns an Iterator of SearchResults.
	 */
	@Override
	public DocSet search(char[] bytes) {
		return this.startFuzzySearch(DocSet.create(maxDocId + 1), bytes, FuzzyQueryOption.DEFAULT_D0_NP);
	}
	

	/**
	 * DANGER DANGER: dense algorithm code ahead. Very order dependent.
	 * Initializes the fail transitions of all states except for the root.
	private void prepareFailTransitions() {
		Queue q = new Queue();
		for (int i = minCharValue; i < maxCharValue ; i++) {
			if (this.root.get((char) i) != null) {
				this.root.get((char) i).setFail(this.root);
				q.add(this.root.get((char) i));
			}
		}
		
		this.prepareRoot();
		
		while (!q.isEmpty()) {
			State state = q.pop();
			char[] keys = state.keys();
			
			for (int i = 0; i < keys.length; i++) {
				State r = state;
				char a = keys[i];
				
				State s = r.get(a);
				q.add(s);
				r = r.getFail();
				
				while (r.get(a) == null) {
					r = r.getFail();
				}
				s.setFail(r.get(a));

				// This will add all possible suffix words into this node.
				//s.addAllOutputs(r.get(a));
			}
		}
	}
	 */

	/**
	 * Sets all the out transitions of the root to itself, if no transition yet
	 * exists at this point.
	 */
	/*
	private void prepareRoot() {
		for (int i = minCharValue; i < maxCharValue ; i++)
			if (this.root.get((char) i) == null)
				this.root.put((char) i, this.root);
	}
	 */

	/**
	 * Begins a new search using the raw interface. Package protected.
	 */
	@Override
	protected DocSet startFuzzySearch(DocSet docSet, char[] chars, FuzzyQueryOption option) {
		if (!this.prepared)
			throw new IllegalStateException(
					"can't start search until prepare()");
		
		if (DEBUG) {
			System.out.println("startFuzzySearch:" + new String(chars) + "/" + option.allowDistance + "/" + option.prefixFuzzy);
		}
		
		continueFuzzySearch(docSet, this.root, 0, chars, new char[0], option.allowDistance, true, option);
		
		if (deleteBS != null) {
			docSet.bitSet.andNot(deleteBS);
		}
		
		return docSet;
	}

	
	/**
	 * Starts a new search, and returns an Iterator of SearchResults.
	 * 
	 * @param prefixFuzzy enable fuzzy the prefix of word, ex. "jsm" fuzzy edit distance 1 
	 * 		  => can match "james" bcz only check the edit ditance of prefix of "james" => "jam" (distance = 1 from "jsm")
	 */
	@Override
	public DocSet fuzzySearch(char[] bytes, FuzzyQueryOption option) {
		/*if (Constants.DEBUG) {
			System.out.println("Query:" + new String(bytes) + " with distance:" + allowDistance);
		}*/
		if (DEBUG) {
			System.out.println("Annotations:");
			System.out.println("Begining  *< : Has Ouput at this node");
			System.out.println("Begining  ~  : No local di offset now");
			System.out.println("Begining  +  : Has local di offset now");
			System.out.println("Begining  *  : No distance left, just normal ahocorasick checking");
			System.out.println("Ending  >X< : Current checked character not fuzzy");
			System.out.println("Ending  (X) : Current checked character is fuzzy");
			System.out.println("Ending     *: Allow substitude");
			System.out.println("Ending  !!X : Not match and dead ended (No distance left)");
		}
		return this.startFuzzySearch(DocSet.create(maxDocId + 1), bytes, option);
	}
	
	@Override
	public DocSet fuzzySearch(DocSet docSet, char[] chars, FuzzyQueryOption option) {
		return this.startFuzzySearch(docSet, chars, option);
	}
	
	/**
	 * Continues the search, given the initial state described by the
	 * lastResult. Package protected.
	 * 
	 * @param lastIndex this is used for the char[] chars (chars represent the QUERY string), not for State
	 */
	DocSet continueFuzzySearch(final DocSet docSet, final State lastMatchedState, final int lastIndex, final char[] chars, final char[] path, final int allowDistance, final boolean parentCheckIns, FuzzyQueryOption option) {
		long totalFuzzyTC = 0;
		boolean checkInsertion = parentCheckIns;
		
		/**
		 * The di move the index of "QUERY", not state itself.
		 * Query: jjjeffrey
		 *        ^    di=0
		 *         ^   di=1
		 *          ^  di=2
		 * Loop di=0~N to check current state have wanted letter, if not, can still go deeper if adjustedAllowDistance still > 0
		 */
		for (int di = 0; (di == 0 || checkInsertion) && (di <= allowDistance) && (lastIndex + di) < chars.length; di++) {
			int adjustedAllowDistance = (allowDistance-di);  // when di > 0, this is for checking insertion. Yes, it's for INSERTION!
			//System.out.println("aad=" + adjustedAllowDistance + ", di=" + di);
			
			if (adjustedAllowDistance > 0) {
				char b = chars[lastIndex+di];  // when di > 0, this is for checking insertion. Yes, it's for INSERTION!
				long beginOfDepth = System.currentTimeMillis();
				char[] fuzzyChars = lastMatchedState.keys();
								
				for (int j = 0; j < fuzzyChars.length; j++) {
					char fuzzyChar = fuzzyChars[j];
					State currState = lastMatchedState.get(fuzzyChar);
					
					boolean isThisCharFuzzy = !(b == fuzzyChar || (isEnablePhoneticMode() && pmap.isPhoneticTheSame(fuzzyChar, b)));
					boolean allowSubstitution = true;
					boolean allowDeletion = true;
					boolean allowInsertion = true;
					
					if (isThisCharFuzzy && isEnablePhoneticMode()) {
						allowSubstitution = pmap.isPhoneticSimilar(b, fuzzyChar);
						allowDeletion = pmap.isPhoneticAllowDeletion(fuzzyChar);
						allowInsertion = pmap.isPhoneticAllowInsertion(b);
					}
					checkInsertion = allowInsertion && parentCheckIns;
					
					if (DEBUG) {
						System.out.print("[" + StringUtils.leftPad("" + path.length, 2, '0') + "]");
						System.out.print( di > 0 ? "+" : "~");
						for (int k=0; k < path.length; k++) System.out.print(" " + path[k] + " ");
						if (isThisCharFuzzy) System.out.println("(" + fuzzyChar + ")" + (allowSubstitution ? "" : "*"));
						else System.out.println(">" + fuzzyChar + "<" + (allowSubstitution ? "" : "*"));
					}
					

					char[] newPath = Arrays.copyOf(path, path.length + 1);
					newPath[newPath.length - 1] = fuzzyChar;
					// if the number of characters left to be checked are greater than or equal to allowDistance, 
					// current index position is a acceptable node for search result. 
					if (lastIndex+adjustedAllowDistance+(isThisCharFuzzy ? 0 : 1) >= chars.length) {
						if (currState.hasOutput()) {
							if (DEBUG) for (Output output: currState.getOutputs()) {
								System.out.print("[" + StringUtils.leftPad("" + path.length, 2, '0') + "]");
								System.out.println("*>" + new String(newPath));
							}
							/*
							for (Output output: currState.getOutputs()) {
								float score = scorer.score(output, chars, newPath, option.allowDistance - (isThisCharFuzzy ? adjustedAllowDistance+1 : adjustedAllowDistance), option);
								docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)));
							}*/
							
							
							if (lastIndex + 1 < chars.length && isEnablePhoneticMode()) {
								boolean allRemainedCharsIsAllowInsertion = true;
								for (int reverseCheckIdx = lastIndex + 1; reverseCheckIdx < chars.length; reverseCheckIdx++) {
									if (!pmap.isPhoneticAllowInsertion(chars[reverseCheckIdx])) {
										allRemainedCharsIsAllowInsertion = false;
										break;
									}
								}
								
								if (allRemainedCharsIsAllowInsertion) {
									for (Output output: currState.getOutputs()) {
										//outputCount++;
										float score = scorer.score(output, chars, newPath, option.allowDistance - (isThisCharFuzzy ? adjustedAllowDistance+1 : adjustedAllowDistance), option);
										docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)));
									}
								}
							}
							else {
								if (!isThisCharFuzzy || allowSubstitution) {
									if (DEBUG) for (Output output: currState.getOutputs()) {
										System.out.print("[" + StringUtils.leftPad("" + path.length, 2, '0') + "]");
										System.out.println("*>" + new String(newPath));
									}
									
									for (Output output: currState.getOutputs()) {
										//outputCount++;
										float score = scorer.score(output, chars, newPath, option.allowDistance - (isThisCharFuzzy ? adjustedAllowDistance+1 : adjustedAllowDistance), option);
										docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)));
									}
									//results.add(new SearchResult(currState, newPath, lastIndex+1, isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance, (1f + 0.1f * (isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance))));
								}
								else {
									//System.out.println("Test");
								}
							}
							//results.add(new SearchResult(currState, newPath, lastIndex+1, isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance, (1f + 0.1f * (isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance))));
						}
						
						if (option.prefixFuzzy) {
							fetchAllChildrenDocs(docSet, lastIndex+di+1, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)), chars, newPath, currState, option.allowDistance - (isThisCharFuzzy ? adjustedAllowDistance+1 : adjustedAllowDistance), option);
						}
						else {
							// This is MATCH or SUBSTITUTION, just same code as several lines below.
							int calculatedDistance = isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance;
							continueFuzzySearch(docSet, currState, lastIndex+di+1, chars, newPath, calculatedDistance, true, option);
						}
					}
					else {
						// Go matching next index position with (allowDistance-1) if current index is substitution, or keep the same allowDistance if current index is exact match.
						// In short: MATCH or SUBSTITUION
						// Example of SUBSTITION
						// Query: jsmws
						//         ^ lastIndex + 1, isThisCharFuzyy == true, allowDistance-1, then going to check "m"
						//         v 
						// STATE: james
						//System.out.println("call continueFuzzySearch('" + fuzzyChar + "', " + (lastIndex+1) + ", >" + new String(newPath) + "<, " + (isThisCharFuzzy ? allowDistance-1 : allowDistance) + ") for substitution");
						if (!isThisCharFuzzy || allowSubstitution) {
							int calculatedDistance = isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance;
							continueFuzzySearch(docSet, currState, lastIndex+di+1, chars, newPath, calculatedDistance, true, option);
						}
						
						// Assume the letter at current index is missing, so call continueFuzzySearch with the same "i", not "i+1" 
						// IN short: DELETION
						// Example of DELETION
						// Query: jfrey
						//         ^ lastIndex not increase to hold on this letter, but the STATE go deeper, and of course the allowDistance must decrease by 1.
						//          v (use currState, it is next level)
						// STATE: jeffrey
						// Query: jsmws
						//        ^ lastIndex not increase to hold on this letter, but the STATE go deeper, and of course the allowDistance must decrease by 1.
						//         v (use currState, it is next level)
						// STATE: wills
						//System.out.println("call continueFuzzySearch('" + fuzzyChar + "', " + (lastIndex) + ", >" + new String(newPath) + "<, " + (allowDistance-1) + ") for deletion");
						if (di == 0 && (!isThisCharFuzzy || allowDeletion)) {
							continueFuzzySearch(docSet, currState, lastIndex+di, chars, newPath, adjustedAllowDistance-1, false, option);
						}
					}
				}

				if (false && DEBUG) {
					totalFuzzyTC += (System.currentTimeMillis() - beginOfDepth);
					if (lastIndex == 0) System.out.println("\ntotalFuzzyTC (aad=" + adjustedAllowDistance + "):" + totalFuzzyTC + "ms");
				}
			}
			else {
				
				/**
				 * When there is no "allowDistance" for fuzzy, just using a FOR LOOP on rest letters of query to find potential keywords. 
				 */
				char[] newPathForExactMatch = Arrays.copyOf(path, path.length);
				State state = lastMatchedState;
				
				for (int i = lastIndex+di; i < chars.length; i++) {
					char b = chars[i];
					boolean hasExactlyMatch = state.get(b) != null;
					if (DEBUG) {
						System.out.print("[" + StringUtils.leftPad("" + path.length, 2, '0') + "]");
						System.out.print("*");
						for (int k=0; k < i; k++) System.out.print(" - ");
					}
					if (hasExactlyMatch) {
						newPathForExactMatch = Arrays.copyOf(newPathForExactMatch, newPathForExactMatch.length + 1);
						newPathForExactMatch[ newPathForExactMatch.length - 1 ] = b;
						
						state = state.get(b);
						if (DEBUG) System.out.println(" " + b + " ");
						
						if ((i+1) == chars.length && state.hasOutput()) {
							
							if (DEBUG) {
								for (Output output: state.getOutputs()) {
									System.out.println("*<" + new String(newPathForExactMatch));
								}
							}

							for (Output output: state.getOutputs()) {
								float score = scorer.score(output, chars, newPathForExactMatch, option.allowDistance, option);
								docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPathForExactMatch : null, (byte)(newPathForExactMatch.length - 1));
							}
							//results.add(new SearchResult(state, newPathForExactMatch, i + 1, allowDistance));
							if (option.prefixFuzzy) fetchAllChildrenDocs(docSet, i, (byte)(newPathForExactMatch.length - 1), chars, newPathForExactMatch, state, option.allowDistance, option); 
						}
						else if ((i+1) == chars.length && option.prefixFuzzy) {
							fetchAllChildrenDocs(docSet, i, (byte)(newPathForExactMatch.length - 1), chars, newPathForExactMatch, state, option.allowDistance, option);
						}
					}
					else {
						if (DEBUG) System.out.println("!!" + b);
						break;
					}
				}
			}
		}

		return docSet;
	}

	/**
	 * Traverse and fetch all node contains result (which is output.length > 0)
	 * 
	 * @param path the path from root, represent as a character array
	 * @param startFromHere start from this node
	 * @param pool the result pool, used recursively
	 */
	void fetchAllChildrenDocs(final DocSet docSet, int index, byte hp, final char[] query, final char[] path, State startFromHere, final int calculatedDistance, FuzzyQueryOption option) {
		char[] keys = startFromHere.keys();
		
		for (char key: keys) {
			char[] newPath = Arrays.copyOf(path, path.length + 1);
			newPath[newPath.length - 1] = key;
			int adjustDistance = calculatedDistance;
			if (index < query.length && calculatedDistance < option.allowDistance) {
				if (query[index] != key)
					adjustDistance++;
				else
					hp++;
			}

			State state = startFromHere.get(key);
			if (state.hasOutput()) {
				for (Output output: state.getOutputs()) {
					//TrieScoreDoc doc = new TrieScoreDoc(output, baseScore, newPath);
					//don't new object until need
					float score = scorer.score(output, query, newPath, adjustDistance, option);
					
					if (docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, hp) && option.stopWhenReachMaxResult) {
						if (docSet.size() > option.maxResult)
							return;
					}
				}
			}
			
			fetchAllChildrenDocs(docSet, index+1, hp, query, newPath, state, adjustDistance, option);
		}
	}
	
	public static void main(String[] args) {
		System.out.println(simpleAnalyzer("92式手槍"));
	}

	@Override
	public void delete(int output) {
		deleteBS.set(output);
	}
	
}
