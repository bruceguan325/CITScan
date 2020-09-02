package com.intumit.smartwiki.recommend.renew;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.model.WikiWordtable;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.util.TextUtil;

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
public class AhoCorasick {
	private State root;
	private boolean prepared;
	HitHotLocale locale;

	protected AhoCorasick(HitHotLocale locale) {
		this.root = new State();
		this.prepared = false;
		this.locale = locale;
	}
	
	/**
	 * Adds a new keyword with the given output. During search, if the keyword
	 * is matched, output will be one of the yielded elements in
	 * SearchResults.getOutputs().
	 */
	private void add(char[] keyword, int output) {
		if (this.prepared)
			throw new IllegalStateException(
					"can't add keywords after prepare() is called");
		
		/*String kw = new String(keyword);
		if (kw.startsWith("92式"))
			System.out.println("**************" + kw);*/
		State lastState = this.root.extendAll(keyword);
		lastState.addOutput(output);
	}

	/**
	 * Prepares the automaton for searching. This must be called before any
	 * searching().
	 */
	public void prepare() {
		this.prepareFailTransitions();
		this.prepared = true;
	}
	
	public boolean isPrepared() {
		return this.prepared;
	}

	/**
	 * Starts a new search, and returns an Iterator of SearchResults.
	 */
	public Iterator<SearchResult> search(char[] bytes) {
		return new Searcher(this, this.startSearch(bytes));
	}

	/**
	 * DANGER DANGER: dense algorithm code ahead. Very order dependent.
	 * Initializes the fail transitions of all states except for the root.
	 */
	private void prepareFailTransitions() {
		Queue q = new Queue();
		for (int i = 0; i < Character.MAX_VALUE ; i++)
			if (this.root.get((char) i) != null) {
				this.root.get((char) i).setFail(this.root);
				q.add(this.root.get((char) i));
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
				while (r.get(a) == null)
					r = r.getFail();
				s.setFail(r.get(a));

				s.addAllOutputs(r.get(a));

			}
		}
	}

	/**
	 * Sets all the out transitions of the root to itself, if no transition yet
	 * exists at this point.
	 */
	private void prepareRoot() {
		for (int i = 0; i < Character.MAX_VALUE ; i++)
			if (this.root.get((char) i) == null)
				this.root.put((char) i, this.root);
	}

	/**
	 * Returns the root of the tree. Package protected, since the user probably
	 * shouldn't touch this.
	 */
	State getRoot() {
		return this.root;
	}

	/**
	 * Begins a new search using the raw interface. Package protected.
	 */
	SearchResult startSearch(char[] chars) {
		if (!this.prepared)
			throw new IllegalStateException(
					"can't start search until prepare()");
		return continueSearch(new SearchResult(this.root, chars, 0));
	}

	/**
	 * Continues the search, given the initial state described by the
	 * lastResult. Package protected.
	 */
	SearchResult continueSearch(SearchResult lastResult) {
		char[] chars = lastResult.chars;
		State state = lastResult.lastMatchedState;
		for (int i = lastResult.lastIndex; i < chars.length; i++) {
			char b = chars[i];
			while (state.get(b) == null)
				state = state.getFail();
			state = state.get(b);
			if (state.getOutputs().length > 0)
				return new SearchResult(state, chars, i + 1);
		}
		return null;
	}

	/**
	 * by Herb
	 * Adds a new keyword with the given output. During search, if the keyword
	 * is matched, output will be one of the yielded elements in
	 * SearchResults.getOutputs().
	 */
	private Hashtable<Integer, String> reverseMap = new Hashtable<Integer, String>();
	private Hashtable<Integer, Double> initScoreMap = new Hashtable<Integer, Double>();
	private int entryCount = 0;
	
	public void addString(String keyword, int output) {
		addString(keyword, output, 0);
	}
	public boolean addString(String keyword, int output, double score) {
		//String token = keyword.toLowerCase().replaceAll(" ", "_");
		String token = TextUtil.preprocessText(keyword);
		//String token = simpleAnalyzer(keyword);
		
		// Just a small hack, should be done in Analyzer
		if (StringUtils.trimToEmpty(token).length() < 2)
			return false;
		add(token.toCharArray(), output);
		reverseMap.put(output, keyword);
		initScoreMap.put(output, score);
		entryCount++;
		return true;
	}
	
	public int getEntryCount() {
		return entryCount;
	}

	public List<WikiWord> getWikiWord(String content) {
		content = content.toLowerCase().replaceAll(" ", "_");
		WikiWordtable matchedPatterns = new WikiWordtable(locale);
		List<WikiWord> list = new ArrayList<WikiWord>();
		Iterator<SearchResult> searcher = search(content.toCharArray());
		while (searcher.hasNext()) {
			SearchResult result = searcher.next();
			int[] outputs = result.getOutputs();
			
			for (int output: outputs) {
				String pt = reverseMap.get(output);
				
				if (!matchedPatterns.containsKey(pt)) {
					double score = initScoreMap.get(output);
					WikiWord w = new WikiWord();
					w.setPageTitle(pt);
					w.setKeywordId(output);
					w.setInitScore(score);
					w.setFirstIndex(result.getLastIndex() - pt.length());
					w.setFrequency(1);
					
					// 如果不是 CJK，那不是 match完整的字直接跳過
					int firstIndex = w.getFirstIndex();
					int lastIndex = result.getLastIndex();
					if (!locale.isCjk() || w.getIsBasicLatin()) {
						if (lastIndex < content.length() && content.charAt(lastIndex) != '_') {
							//System.out.println("skip this pt:" + pt + " ==> at(" + (lastIndex) + ")=" + content.charAt(lastIndex));
							continue;
						}
						if (firstIndex - 1 >= 0 && content.charAt(firstIndex-1) != '_') {
							//System.out.println("skip this pt:" + pt + " ==> at(" + (firstIndex-1) + ")=" + content.charAt(firstIndex-1));
							continue;
						}
					}

					//System.out.println("Found [" + pt + "] at index: " + w.getFirstIndex());
					// 如果有兩個 match wikiword 再同一個區域，那麼用 isMoreImport 決定誰留下來
					WikiWord overlap = matchedPatterns.getOverLap(w);
					if (overlap != null) {
						if (isMoreImportant(w, overlap, locale.isCjk()))
							matchedPatterns.replaceOverLap(overlap, w);
					}
					else {
						matchedPatterns.put(pt, w);
					}
				}
				else {
					WikiWord w = (WikiWord)matchedPatterns.get(pt);
					w.setFrequency(w.getFrequency() + 1);
					//System.out.println("Found [" + pt + "]" + w.getFrequency() + " times at index: " + w.getFirstIndex());
				}
			}
		}
		
		Enumeration<String> enumeration = matchedPatterns.keys();
		while (enumeration.hasMoreElements()) {
			WikiWord word = new WikiWord();
			String pageTitle = enumeration.nextElement();
			/* 也許當初是為了避免 memory leak? 或者無法 gc? 不然這樣寫的原因實在很難理解
			 * 反正直接就把 object 加入新的 list 比較乾脆，不然有一些值如果沒有設定到就很難除錯
			word.setPageTitle(pageTitle);
			word.setFrequency(((WikiWord) matchedPatterns.get(pageTitle))
					.getFrequency());
			word.setFirstIndex(((WikiWord) matchedPatterns.get(pageTitle))
					.getFirstIndex());
			word.setKeywordId(((WikiWord) matchedPatterns.get(pageTitle))
					.getKeywordId());
			word.setAllChildSet(((WikiWord) matchedPatterns.get(pageTitle))
					.getAllChildSet());*/
			list.add((WikiWord)matchedPatterns.get(pageTitle));
		}
		
		return list;
	}
	

	public boolean isMoreImportant(WikiWord wiki1, WikiWord wiki2, boolean cjkMode) {
		// TODO
		// 這裡還有一些問題，即便是 cjk，也有可能是英文字，所以這個判斷遠比我想像的複雜，例如「無線HDMI」應該算三個字才對，也許用 StandardAnalyzer 比較簡單
		int wiki1Len = cjkMode ? wiki1.getPageTitle().length() : wiki1.getPageTitle().split("_").length;
		int wiki2Len = cjkMode ? wiki2.getPageTitle().length() : wiki2.getPageTitle().split("_").length;
		if (wiki2Len == 1)
			return true;
		else if (wiki1.getFirstIndex() == wiki2.getFirstIndex()
				&& wiki1Len > wiki2Len)
			return true; // new wiki word is the prefix
		else if (wiki1.getFirstIndex() + wiki1.getPageTitle().length() == wiki2.getFirstIndex() + wiki2.getPageTitle().length()
				&& wiki1.getFirstIndex() < wiki2.getFirstIndex())
			return true; // new wiki word is the suffix
		else if (wiki1Len > wiki2Len)
			return true;
		else if (wiki1Len == wiki2Len
				&& (wiki1.getPageTitle().startsWith("the_") || wiki1
						.getPageTitle().startsWith("The_")))
			return false;
		else if (wiki1Len == wiki2Len
				&& (wiki2.getPageTitle().startsWith("the_") || wiki2
						.getPageTitle().startsWith("The_")))
			return true;
		else
			return false;
	}
	
	private static String simpleAnalyzer(String keyword) {
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
	
	public static void main(String[] args) {
		System.out.println(simpleAnalyzer("92式手槍"));
		System.out.println(TextUtil.preprocessText("92式手槍"));
	}
	
}
