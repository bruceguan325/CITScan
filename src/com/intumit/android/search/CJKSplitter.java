package com.intumit.android.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource.State;
import org.apache.lucene.util.Version;

public class CJKSplitter {
	//final static Analyzer ANALYZER = new MySimpleAnalyzer(Version.LUCENE_CURRENT);
	//final static Analyzer ANALYZER_NO_LOWER_CASE = new MySimpleAnalyzer(Version.LUCENE_CURRENT, false);
    final static Set<String> CJK_TYPES = new HashSet<String>(Arrays.asList(new String[] {"<IDEOGRAPHIC>", "<HIRAGANA>", "<KATAKANA>", "<HANGUL>"}));
    final static String NUM_TYPE = "<NUM>";

	private TokenStream ts;
	CharTermAttribute termAtt = null;
    TypeAttribute typeAtt = null;
    OffsetAttribute offsetAtt = null;
    
    State lastState = null;
    private String token = null;
	private Boolean isCJK = null;
	private boolean isNUM;
	private boolean concateCJK;
	private boolean concateCJKEvenCrossStopWords = false;
	private boolean enableLowerCase = true;
	private int startPos = -1;
	private int endPos = -1;
	private boolean dupStateCheck = false;
	private int dupStateLastStartPos = -1;
	private int dupStateLastEndPos = -1;
	
	public CJKSplitter() {
		ts = null;
	}
    
	public CJKSplitter(String query, boolean concateCJK) throws IOException {
		setQuery(query);
		setConcateCJK(concateCJK);
	}
	
	public void setQuery(String query) throws IOException {
		ts = enableLowerCase ? new MySimpleAnalyzer(Version.LUCENE_CURRENT).tokenStream("", new StringReader(query)) : new MySimpleAnalyzer(Version.LUCENE_CURRENT, false).tokenStream("", new StringReader(query));
		termAtt = ts.getAttribute(CharTermAttribute.class);
		typeAtt = ts.getAttribute(TypeAttribute.class);
		offsetAtt = ts.getAttribute(OffsetAttribute.class);
		ts.reset();
		dupStateCheck = false;
		dupStateLastStartPos = -1;
		dupStateLastEndPos = -1;
	}
	
	public void setConcateCJK(boolean concateCJK) {
		this.concateCJK = concateCJK;
	}
	
	public boolean isConcateCJKEvenCrossStopWords() {
		return concateCJKEvenCrossStopWords;
	}

	public void setConcateCJKEvenCrossStopWords(boolean concateCJKEvenCrossStopWords) {
		this.concateCJKEvenCrossStopWords = concateCJKEvenCrossStopWords;
	}

	public void setEnableLowerCase(boolean elc) {
		this.enableLowerCase = elc;
	}
	
	public boolean isEnableLowerCase() {
		return enableLowerCase;
	}
	
	/*void printState(String bp) {
		System.out.println(bp + "[" + (ts != null ? "ts=" + ts + ", " : "") + (termAtt != null ? "termAtt=" + termAtt + ", " : "")
				+ (typeAtt != null ? "typeAtt=" + typeAtt + ", " : "") + (offsetAtt != null ? "offsetAtt=" + offsetAtt + ", " : "")
				+ (lastState != null ? "lastState=" + lastState + ", " : "") + (token != null ? "token=" + token + ", " : ""));
	}*/

	public boolean next() {
		String term = "";
		
		try {
			if (!concateCJK) {
				if (ts.incrementToken()) {
					do {
						String type = typeAtt.type();
						isNUM = NUM_TYPE.equals(type);
						isCJK = CJK_TYPES.contains(type);
						token = termAtt.toString();
						startPos  = offsetAtt.startOffset();
						endPos   = offsetAtt.endOffset();
						
						if ( !(dupStateCheck && dupStateLastStartPos == startPos && dupStateLastEndPos == endPos)) {
							dupStateCheck = true;
							dupStateLastStartPos = startPos;
							dupStateLastEndPos = endPos;
							break;
						}
					} while (ts.incrementToken());
					return true;
				}
				return false;
			}

			int lastStartPos = -1;
			int lastEndPos = -1;
			boolean hasLastState = false;
			
			if (lastState != null) {
				ts.restoreState(lastState);
				hasLastState = true;
				lastStartPos = offsetAtt.startOffset();
				lastEndPos = offsetAtt.startOffset();
				lastState = null;
			}
			
			Boolean cjkMode = null;
			
			while (hasLastState || ts.incrementToken()) {
				hasLastState = false;
				String type = typeAtt.type();
				boolean isCJKOfThisTerm = CJK_TYPES.contains(type);
				boolean currentIsNUM = NUM_TYPE.equals(type);
				
				if (cjkMode != null 
						&& 
						(cjkMode != isCJKOfThisTerm
							|| (cjkMode && !concateCJKEvenCrossStopWords && (lastEndPos != -1 && lastEndPos != offsetAtt.startOffset()))
						) 
					)
					{
					lastState = ts.captureState();
					isCJK = cjkMode;
					token = term;
					startPos  = lastStartPos == -1 ? offsetAtt.startOffset() : lastStartPos;
					endPos   = lastEndPos == -1 ? offsetAtt.endOffset() : lastEndPos;
					return true;
				}

				term += termAtt.toString();
				isNUM = currentIsNUM;
				cjkMode = isCJKOfThisTerm;
				if (!isCJKOfThisTerm) {
					isCJK = false;
					token = term;
					startPos  = offsetAtt.startOffset();
					endPos   = offsetAtt.endOffset();
					return true;
				}

				if (lastStartPos == -1)
					lastStartPos = offsetAtt.startOffset();
	 			lastEndPos = offsetAtt.endOffset();
				ts.clearAttributes();
			}
			
			if (cjkMode != null && cjkMode) {
				token = term;
				isCJK = true;
				startPos  = lastStartPos == -1 ? offsetAtt.startOffset() : lastStartPos;
				endPos   = lastEndPos == -1 ? offsetAtt.endOffset() : lastEndPos;
				return true;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String getToken() {
		return token;
	}

	public boolean isCJK() {
		return isCJK;
	}

	public boolean isNUM() {
		return isNUM;
	}
	
	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}

	public void close() {
		try {
			ts.end();
			ts.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
