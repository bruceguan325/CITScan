package com.intumit.android.search;

import com.intumit.android.search.fuzzy.Output;

public class FuzzyQueryOption {
	final public int allowDistance;
	final public boolean prefixFuzzy;
	final public boolean numberFuzzy;
	final public int maxResult;
	final public boolean stopWhenReachMaxResult;
	final public boolean isMappedQuery;
	final public int mappedCode;

	/**
	 * Query time options
	 * 
	 * @param allowDistance 
	 * @param prefixFuzzy
	 * @param maxResult
	 * @param stopWhenReachMaxResult
	 * @param isMappedQuery
	 * @param mappedCode
	 */
	public FuzzyQueryOption(int allowDistance, boolean prefixFuzzy, int maxResult, boolean stopWhenReachMaxResult, boolean isMappedQuery, int mappedCode) {
		this(allowDistance, prefixFuzzy, true, maxResult, stopWhenReachMaxResult, isMappedQuery, mappedCode);
	}
	
	/**
	 * Query time options
	 * 
	 * @param allowDistance
	 * @param prefixFuzzy
	 * @param numberFuzzy
	 * @param maxResult
	 * @param stopWhenReachMaxResult
	 * @param isMappedQuery
	 * @param mappedCode
	 */
	public FuzzyQueryOption(int allowDistance, boolean prefixFuzzy, boolean numberFuzzy, int maxResult, boolean stopWhenReachMaxResult, boolean isMappedQuery, int mappedCode) {
		super();
		this.allowDistance = allowDistance;
		this.prefixFuzzy = prefixFuzzy;
		this.numberFuzzy = numberFuzzy;
		this.maxResult = maxResult;
		this.stopWhenReachMaxResult = stopWhenReachMaxResult;
		this.isMappedQuery = isMappedQuery;
		this.mappedCode = mappedCode;
	}
	
	@Override
	public String toString() {
		return "FuzzyQueryOption [allowDistance=" + allowDistance
				+ ", prefixFuzzy=" + prefixFuzzy + ", numberFuzzy="
				+ numberFuzzy + ", maxResult=" + maxResult
				+ ", stopWhenReachMaxResult=" + stopWhenReachMaxResult
				+ ", isMappedQuery=" + isMappedQuery + ", mappedCode="
				+ mappedCode + "]";
	}


	public static final FuzzyQueryOption DEFAULT_D0_P = new FuzzyQueryOption(0, true, true, -1, false, false, Output.NOT_SYNONYM);
	public static final FuzzyQueryOption DEFAULT_D0_NP = new FuzzyQueryOption(0, false, true, -1, false, false, Output.NOT_SYNONYM);
	public static final FuzzyQueryOption DEFAULT_D1_P = new FuzzyQueryOption(1, true, true, -1, false, false, Output.NOT_SYNONYM);
	public static final FuzzyQueryOption DEFAULT_D1_NP = new FuzzyQueryOption(1, false, true, -1, false, false, Output.NOT_SYNONYM);
	public static final FuzzyQueryOption DEFAULT_D2_P = new FuzzyQueryOption(2, true, true, -1, false, false, Output.NOT_SYNONYM);
	public static final FuzzyQueryOption DEFAULT_D2_NP = new FuzzyQueryOption(2, false, true, -1, false, false, Output.NOT_SYNONYM);
}
