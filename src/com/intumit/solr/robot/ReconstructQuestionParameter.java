package com.intumit.solr.robot;

public class ReconstructQuestionParameter {
	public static final ReconstructQuestionParameter DEFAULT_USER_INPUT_PARAM = 
			new ReconstructQuestionParameter(QAUtil.ENABLE_NLP, true, true, true, true, true, true, false);
	public static final ReconstructQuestionParameter LONG_LENGTH_USER_INPUT_PARAM = 
			new ReconstructQuestionParameter(QAUtil.ENABLE_NLP, true, false, true, true, true, true, true);
	public static final ReconstructQuestionParameter DEFAULT_CASUAL_USER_INPUT_PARAM = 
			new ReconstructQuestionParameter(QAUtil.ENABLE_NLP, false, true, true, true, true, false, false);
	public static final ReconstructQuestionParameter DEFAULT_QA_ALT_PARAM = 
			new ReconstructQuestionParameter(QAUtil.ENABLE_NLP, false, true, true, true, false, QAUtil.QA_SAVER_DEBUG, false);
	
	public final boolean nlp;
	public final boolean normalizeSynonym;
	public final boolean checkDup;
	public final boolean simpleClean;
	public final boolean pChaosMode;
	public final boolean allowAutoDisableChaosMode;
	public final boolean debug;
	public final boolean normalizeSynonymWithHighestPriority;
	public final boolean insertStopwordBetweenKeywords;

	/**
	 * 
	 * 
	 * 用於 User Input 一般是： nlp=true, normalizeSyn=true, checkDups=false, simpleClean=true
	 * 用於 Alt Expansion 一般是： nlp=true, normalizeSyn=false, checkDups=true, simpleClean=true
	 * 
	 * @param question
	 * @param nlp Use NLP to remove some "介係詞", "助詞"
	 * @param normalizeSynonym 是否要將同義詞全部轉為「關鍵詞」
	 * @param checkDup 檢測 reconstructed questions 是否有重複（不檢測重複通常是為了重複的問題在 solr query 可以提供權重）
	 * @param simpleClean 簡單的 reqular expression 清除標點符號等stop words，一般應該沒有不使用這個的，不用會很容易有各種問題，留此選項只為了 debug 用
	 * @param debug 是否print debug訊息
	 * @param normalizeSynonymWithHighestPriority 是否在轉換同義詞時，若該同義詞有多個「關鍵詞」，只挑選 priority 最高的
	 * @return
	 */
	public ReconstructQuestionParameter(boolean nlp, boolean normalizeSynonym,
			boolean checkDup, boolean simpleClean, boolean pChaosMode, boolean allowAutoDisableChaosMode,
			boolean debug, boolean normalizeSynonymWithHighestPriority) {
		this.nlp = nlp;
		this.normalizeSynonym = normalizeSynonym;
		this.checkDup = checkDup;
		this.simpleClean = simpleClean;
		this.pChaosMode = pChaosMode;
		this.allowAutoDisableChaosMode = allowAutoDisableChaosMode;
		this.debug = debug;
		this.normalizeSynonymWithHighestPriority = normalizeSynonymWithHighestPriority; 
		this.insertStopwordBetweenKeywords = false;
	}
}