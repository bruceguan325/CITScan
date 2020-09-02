package com.intumit.android.search.fuzzy;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;
import com.google.common.base.Stopwatch;
import com.intumit.android.search.Constants;
import com.intumit.android.search.FuzzyQueryOption;
import com.intumit.android.search.fuzzy.ByteBufferState.RootByteBufferState;
import com.intumit.android.search.util.ByteBufferUtils;

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
public class ByteBufferAhoCorasick extends AhoCorasick {
	protected ByteBufferAhoCorasick(int maxCharValue) {
		this(maxCharValue, false);
	}

	protected ByteBufferAhoCorasick(int maxCharValue, boolean buildReverseMap) {
		this.root = new State();
		this.maxCharValue = maxCharValue;
		this.prepared = false;
		this.buildReverseMap = buildReverseMap;
		init();
	}

	public void setRoot(ByteBufferState.RootByteBufferState bbRoot) {
		bbRoot.root = bbRoot;
		this.root = bbRoot;
	}

	ByteBufferState.RootByteBufferState getBBRoot() {
		return (ByteBufferState.RootByteBufferState)root;
	}

	public void init() {
		if (Constants.DEBUG) {
			System.out.println("Prepare an whole new and empty bytebuffer ahocorasick.");
		}
		ByteBuffer bb = ByteBuffer.allocateDirect(ByteBufferUtils.DEFAULT_BYTE_BUFFER_SIZE);

		bb.position(0);
		bb.putInt(0, 1);
		bb.putInt(4, 1);

		int rootStateId = State._STATE_ID_SEED + 1;
		bb.putInt(8, rootStateId); // The "+1" is bcz we new RootByteBufferState later, that will increase ID_SEED

		int rootPos = ByteBufferState.SIZE_OF_INDEX_HEADER;
		bb.putInt(rootPos, rootStateId);
		bb.put(rootPos + ByteBufferState.OFFSET_TO_DEPTH, (byte)1);
		bb.putInt(rootPos + ByteBufferState.OFFSET_TO_PREALLOCATED_KEY_SIZE, ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[0]);
		bb.putInt(rootPos + ByteBufferState.OFFSET_TO_KEY_SIZE, 0); // No key now
		bb.putInt(rootPos + ByteBufferState.OFFSET_TO_KEYS
				+ ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[0] * ByteBufferState.SIZE_OF_EACH_KEY, 0); // No output now
		int endOfBuffer = rootPos + ByteBufferState.OFFSET_TO_KEYS
				+ ByteBufferState.PREALLOCATE_NUMBER_OF_KEYS_BY_DEPTH[0] * ByteBufferState.SIZE_OF_EACH_KEY
				+ ByteBufferState.BYTES_OF_INTEGER;

		bb.putInt(0 + ByteBufferState.OFFSET_TO_VAR_END_OF_BUFFER, endOfBuffer);

		ByteBufferState.RootByteBufferState root = new ByteBufferState.RootByteBufferState(rootStateId, bb, rootPos, endOfBuffer);
		setRoot(root);
	}

	/**
	 * Prepares the automaton for searching. This must be called before any
	 * searching().
	 */
	@Override
	public void prepare() {
		//this.prepareFailTransitions();
		if (!(this.root instanceof ByteBufferState.RootByteBufferState)) {
			init();
		}

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
		if (!(this.root instanceof ByteBufferState.RootByteBufferState)) {
			init();
		}

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
		return this.startFuzzySearch(DocSet.create(maxDocId + 1), bytes, option);
	}

	/**
	 * Starts a new search, and returns an Iterator of SearchResults.
	 *
	 * @param prefixFuzzy enable fuzzy the prefix of word, ex. "jsm" fuzzy edit distance 1
	 * 		  => can match "james" bcz only check the edit ditance of prefix of "james" => "jam" (distance = 1 from "jsm")
	 */
	@Override
	public DocSet fuzzySearch(DocSet docSet, char[] bytes, FuzzyQueryOption option) {
		/*if (Constants.DEBUG) {
			System.out.println("Query:" + new String(bytes) + " with distance:" + allowDistance);
		}*/
		return this.startFuzzySearch(docSet, bytes, option);
	}


	/**
	 * Begins a new search using the raw interface. Package protected.
	 */
	@Override
	protected DocSet startFuzzySearch(DocSet docSet, char[] chars, FuzzyQueryOption option) {
		if (!this.prepared)
			throw new IllegalStateException(
					"can't start search until prepare()");

		if (DEBUG)
			System.out.println("startFuzzySearch:" + new String(chars) + "/" + option.allowDistance + "/" + option.prefixFuzzy);

		//loopCount = 0;
		//outputCount = 0;
		//docSetOpSW.reset();
		//scoringSW.reset();
		continueFuzzySearch(docSet, ((ByteBufferState)this.root).root, ((ByteBufferState)this.root).root.getStartPosition(), 0, chars, new char[0], option.allowDistance, true, option);
		//System.out.println("\nLoop count of fuzzy search on [" + new String(chars) + "]:" + loopCount + " times / " + docSet.size() + " docs / " + outputCount + " outputs");
		//System.out.println("docSet operation TC:" + docSetOpSW.elapsed(TimeUnit.MICROSECONDS) + " microsec / scoring TC: " + scoringSW.elapsed(TimeUnit.MICROSECONDS) + " microsec");

		if (deleteBS != null) {
			docSet.bitSet.andNot(deleteBS);
		}

		return docSet;
	}

	long loopCount = 0;
	long outputCount = 0;
	Stopwatch scoringSW = Stopwatch.createUnstarted();
	Stopwatch docSetOpSW = Stopwatch.createUnstarted();
	/**
	 * Continues the search, given the initial state described by the
	 * lastResult. Package protected.
	 *
	 * @param lastIndex this is used for the char[] chars (chars represent the QUERY string), not for State
	 */
	DocSet continueFuzzySearch(final DocSet docSet, final ByteBufferState.RootByteBufferState root, final int lastMatchedStatePos, final int lastIndex, final char[] chars, final char[] path, final int allowDistance, final boolean parentCheckIns, final FuzzyQueryOption option) {
		loopCount++;
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
				char[] fuzzyChars = ByteBufferState.keys(root, lastMatchedStatePos);
				int[] childrenPositions = ByteBufferState.childrenPositions(root, lastMatchedStatePos);

				/*boolean hasExactlyMatch = false;
				for (int j = 0; j < fuzzyChars.length; j++) {
					if (fuzzyChars[j] == b || (isEnablePhoneticMode() && pmap.isPhoneticTheSame(fuzzyChars[j], b))) {
						hasExactlyMatch = true;
						break;
					}
				}*/

				for (int j = 0; j < fuzzyChars.length; j++) {
					char fuzzyChar = fuzzyChars[j];
					int childStatePos = childrenPositions[j];

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
						System.out.print( di > 0 ? "+" : "~");
						for (int k=0; k < path.length; k++) System.out.print(" " + path[k] + " ");
						if (isThisCharFuzzy) System.out.println("(" + fuzzyChar + ")");
						else System.out.println(">" + fuzzyChar + "<");
					}

					char[] newPath = Arrays.copyOf(path, path.length + 1);
					newPath[newPath.length - 1] = fuzzyChar;
					// if the number of characters left to be checked are greater than or equal to allowDistance,
					// current index position is a acceptable node for search result.
					if (lastIndex+adjustedAllowDistance+(isThisCharFuzzy ? 0 : 1) >= chars.length) {

						if (ByteBufferState.hasOutput(root, childStatePos)) {
							if (lastIndex + 1 < chars.length && isEnablePhoneticMode()) {
								boolean allRemainedCharsIsAllowInsertion = true;
								for (int reverseCheckIdx = lastIndex + di + 1; reverseCheckIdx < chars.length; reverseCheckIdx++) {
									if (!pmap.isPhoneticAllowInsertion(chars[reverseCheckIdx])) {
										allRemainedCharsIsAllowInsertion = false;
										break;
									}
								}

								if (allRemainedCharsIsAllowInsertion) {
									for (Output output: ByteBufferState.getOutputs(root, childStatePos)) {
										//outputCount++;
										float score = scorer.score(output, chars, newPath, option.allowDistance - adjustedAllowDistance + (isThisCharFuzzy ? 1 : 0), option);
										docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)));
									}
								}
							}
							else {
								if ((!isThisCharFuzzy || allowSubstitution) && ByteBufferState.hasOutput(root, childStatePos)) {
									if (DEBUG) for (Output output: ByteBufferState.getOutputs(root, childStatePos)) {
										System.out.println("*>" + new String(newPath));
									}

									for (Output output: ByteBufferState.getOutputs(root, childStatePos)) {
										//outputCount++;
										float score = scorer.score(output, chars, newPath, option.allowDistance - adjustedAllowDistance + (isThisCharFuzzy ? 1 : 0), option);
										docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)));
									}
									//results.add(new SearchResult(currState, newPath, lastIndex+1, isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance, (1f + 0.1f * (isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance))));
								}
								else {
									//System.out.println("Test");
								}
							}
						}

						if (option.prefixFuzzy) {
							fetchAllChildrenDocs(docSet, lastIndex+di+1, (byte)(newPath.length - (isThisCharFuzzy ? 2 : 1)), chars, newPath, root, childStatePos, option.allowDistance - (isThisCharFuzzy ? adjustedAllowDistance+1 : adjustedAllowDistance), option);
						}
						else {
							// This is MATCH or SUBSTITUTION, just same code as several lines below.
							if (true) {//!isThisCharFuzzy || allowSubstitution) {
								int calculatedDistance = isThisCharFuzzy ? adjustedAllowDistance-1 : adjustedAllowDistance;
								continueFuzzySearch(docSet, root, childStatePos, lastIndex+di+1, chars, newPath, calculatedDistance, true, option);
							}

							if (di == 0 && (!isThisCharFuzzy || allowDeletion)) {
								continueFuzzySearch(docSet, root, childStatePos, lastIndex+di, chars, newPath, adjustedAllowDistance-1, false, option);
							}
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
							continueFuzzySearch(docSet, root, childStatePos, lastIndex+di+1, chars, newPath, calculatedDistance, true, option);
						}
						else {
							//System.out.println("Test");
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
							continueFuzzySearch(docSet, root, childStatePos, lastIndex+di, chars, newPath, adjustedAllowDistance-1, false, option);
						}
						else {
							//System.out.println("Test");
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
				int statePos = lastMatchedStatePos;

				for (int i = lastIndex+di; i < chars.length; i++) {
					char b = chars[i];
					int childIndexOfB = -1;

					char[] charsOfState = ByteBufferState.keys(root, statePos);
					boolean hasExactlyMatch = false;
					for (int j = 0; j < charsOfState.length; j++) {
						if (charsOfState[j] == b || (isEnablePhoneticMode() && pmap.isPhoneticTheSame(charsOfState[j], b))) {
							hasExactlyMatch = true;
							childIndexOfB = j;
							break;
						}
					}

					if (DEBUG) {
						System.out.print("*");
						for (int k=0; k < i; k++) System.out.print(" - ");
					}
					if (hasExactlyMatch) {
						newPathForExactMatch = Arrays.copyOf(newPathForExactMatch, newPathForExactMatch.length + 1);
						newPathForExactMatch[ newPathForExactMatch.length - 1 ] = b;

						statePos = ByteBufferState.getChildStatePosition(root, statePos, childIndexOfB);
						if (DEBUG) System.out.println(" " + b + " ");

						if ((i+1) == chars.length && ByteBufferState.hasOutput(root, statePos)) {

							if (DEBUG) {
								for (Output output: ByteBufferState.getOutputs(root, statePos)) {
									System.out.println("*<" + new String(newPathForExactMatch));
								}
							}

							for (Output output: ByteBufferState.getOutputs(root, statePos)) {

								//outputCount++;
								//scoringSW.start();
								float score = scorer.score(output, chars, newPathForExactMatch, option.allowDistance, option);
								//scoringSW.stop();

								//docSetOpSW.start();
								docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPathForExactMatch : null, (byte)(newPathForExactMatch.length - 1));
								//docSetOpSW.stop();
							}
							//results.add(new SearchResult(state, newPathForExactMatch, i + 1, allowDistance));
							if (option.prefixFuzzy) fetchAllChildrenDocs(docSet, i, (byte)(newPathForExactMatch.length - 1), chars, newPathForExactMatch, root, statePos, option.allowDistance, option);
						}
						else if ((i+1) == chars.length && option.prefixFuzzy) {
							fetchAllChildrenDocs(docSet, i, (byte)(newPathForExactMatch.length - 1), chars, newPathForExactMatch, root, statePos, option.allowDistance, option);
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
	void fetchAllChildrenDocs(final DocSet docSet, int index, byte hp, final char[] query, final char[] path, ByteBufferState.RootByteBufferState root, int startFromHere, final int calculatedDistance, FuzzyQueryOption option) {
		loopCount++;
		char[] keys = ByteBufferState.keys(root, startFromHere);

		for (int i=0; i < keys.length; i++) {
			char key = keys[i];
			char[] newPath = Arrays.copyOf(path, path.length + 1);
			newPath[newPath.length - 1] = key;
			int adjustDistance = calculatedDistance;
			if (index < query.length && calculatedDistance < option.allowDistance) {
				if (query[index] != key)
					adjustDistance++;
				else
					hp++;
			}

			int childStatePos = ByteBufferState.getChildStatePosition(root, startFromHere, i);
			if (ByteBufferState.hasOutput(root, childStatePos)) {
				for (Output output: ByteBufferState.getOutputs(root, childStatePos)) {

					//outputCount++;
					//TrieScoreDoc doc = new TrieScoreDoc(output, baseScore, newPath);
					//don't new object until need
					//scoringSW.start();
					float score = scorer.score(output, query, newPath, adjustDistance, option);
					//scoringSW.stop();

					//docSetOpSW.start();
					if (docSet.setAndMaxScoreIfExists(output, score, includePathInScoreDoc ? newPath : null, hp) && option.stopWhenReachMaxResult) {
						if (docSet.size() > option.maxResult) {
							//docSetOpSW.stop();
							return;
						}
					}
					//docSetOpSW.stop();
				}
			}

			fetchAllChildrenDocs(docSet, index+1, hp, query, newPath, root, childStatePos, adjustDistance, option);
		}
	}

	public static void main(String[] args) {
		System.out.println(simpleAnalyzer("92式手槍"));
	}

	@Override
	public void delete(int output) {
		deleteBS.set(output);
	}


	public static void dumpByteBuffer(ByteBuffer bb, int endOfBuffer) {
		TIntObjectHashMap<int[]> pendingRefPos = new TIntObjectHashMap<int[]>();
		//bb = channel.map(MapMode.READ_ONLY, 0, 10*1024*1024);

		int cursor = 0;
		System.out.println("ByteBuffer Capacity / Limit:" + bb.capacity() + " / " + bb.limit());
		System.out.println("Entry Count:" + bb.getInt(cursor));
		System.out.println("MAX DOC ID:" + bb.getInt(cursor+4));
		System.out.println("STATE ID SEED:" + bb.getInt(cursor+8));

		cursor += ByteBufferState.SIZE_OF_INDEX_HEADER;

		while (bb.hasRemaining() && bb.position() < endOfBuffer) {
			State state = ByteBufferState.readStateFrom(bb, cursor, pendingRefPos, true);

			System.out.println("Got state: " + state.id + " (offset: " + cursor + "->" + bb.position() + ")");
			System.out.print("Child Keys:[ ");
			for (char k: state.keys()) {
				System.out.print(k + " ");
			}
			System.out.println("]");
			System.out.println("Outputs: " + state.getOutputs());
			cursor = bb.position();
		}
	}
}

