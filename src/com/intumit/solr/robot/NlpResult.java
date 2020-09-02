package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class NlpResult implements Serializable {
	String originSentence = null;
	String originSentenceSeg = null;
	String segResult = null;
	List<TreeSet<String>> eks = null;

	public NlpResult() {
		super();
	}
	
	public NlpResult(String originSentence) {
		super();
		this.originSentence = originSentence;
	}
	
	public NlpResult(String originSentence, String originSentenceSeg, String segResult, List<TreeSet<String>> eks) {
		super();
		this.originSentence = originSentence;
		this.originSentenceSeg = originSentenceSeg;
		this.segResult = segResult;
		this.eks = eks;
	}
	
	public NlpResult(String originSentence, String originSentenceSeg, String segResult, TreeSet<String>... eksArr) {
		super();
		this.segResult = segResult;
		this.originSentence = originSentence;
		this.originSentenceSeg = originSentenceSeg;
		
		if (eksArr != null) {
			if (eks == null) {
				eks = new ArrayList<TreeSet<String>>();
			}
			eks.addAll(Arrays.asList(eksArr));
		}
	}

	/**
	 * 會併行增加同階層的 ez keywords
	 * 例如 ek = [C, D]
	 * 1. 假設原本的 eks 為空，則最後 eks = [[C], [D]]
	 * 2. 假設原本的 eks 為 [[A,X], [B,Y]]，則最後為 [[A,X,C], [A,X,D], [B,Y,C], [B,Y,D]]
	 * @param ek
	 */
	public void addEssentialKeyword(String... ek) {
		if (eks == null) {
			eks = new ArrayList<TreeSet<String>>();
		}
		
		List<TreeSet<String>> temp = new ArrayList<TreeSet<String>>();
		
		for (int i=ek.length - 1; i >= 0; i--) {
			List<TreeSet<String>> cloned = i == 0 ? eks : cloneList(eks);
			
			if (cloned.size() > 0) {
				for (TreeSet<String> ts: cloned) {
					ts.add(ek[i]);
				}
			}
			else {
				TreeSet<String> ts = new TreeSet<String>();
				ts.add(ek[i]);
				cloned.add(ts);
			}
			
			if (i > 0) temp.addAll(cloned);
		}
		
		eks.addAll(temp);
	}
	
	List<TreeSet<String>> cloneList(List<TreeSet<String>> toBeClone) {
		List<TreeSet<String>> cloned = new ArrayList<TreeSet<String>>();
		for (TreeSet<String> s: toBeClone) {
			TreeSet<String> ns = new TreeSet<String>();
			ns.addAll(s);
			cloned.add(ns);
		}
		
		return cloned;
	}
	
	@Override
	public String toString() {
		return "NlpResult [" + (originSentence != null ? "originSentence=" + originSentence + ", " : "")
				+ (originSentenceSeg != null ? "originSentenceSeg=" + originSentenceSeg + ", " : "")
				+ (segResult != null ? "segResult=" + segResult + ", " : "") + (eks != null ? "eks=" + eks : "") + "]";
	}

	public String getOriginSentence() {
		return originSentence;
	}

	public void setOriginSentence(String originSentence) {
		this.originSentence = originSentence;
	}

	public String getOriginSentenceSeg() {
		return originSentenceSeg;
	}

	public void setOriginSentenceSeg(String originSentenceSeg) {
		this.originSentenceSeg = originSentenceSeg;
	}

	public String setSegResult(String segResult) {
		this.segResult = segResult;
		return this.segResult;
	}

	public String getSegResult() {
		return getSegResult(false);
	}

	/**
	 * 加入 stopword（實際就是句號），可以影響 Solr 產生的查詢條件
	 * 
	 * @param insertStopwordBetweenKeyword 沒事請用 false
	 * @return
	 */
	public String getSegResult(boolean insertStopwordBetweenKeyword) {
		if (!insertStopwordBetweenKeyword) {
			return segResult;
		}
		
		String newSegRes = segResult.replaceAll("\\)", ")."); // 加個句號，世界變的不一樣了
		return newSegRes;
	}

	public List<TreeSet<String>> getEks() {
		return eks;
	}
	
	public boolean hasEks() {
		return eks != null && eks.size() > 0;
	}
}
