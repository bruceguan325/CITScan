package com.intumit.solr.robot.qaplugin;

public class ImportHierarchicalQA {

	private String qaid = null;
	private String optionAction = null;
	private String pipe = null;
	private String question = null;
	private String answer = null;
	private String matchSentences = null;
	private String script = null;
	private String extraParams = null;
	private String expiry = null;
	private String menuView = null;
	private String inputType = null;
	private String text = null;
	
	private String hqaClass = null;
	/**
	 * 當這個不為 null 時，將會使用 QAUtil.mergeQA 來將指定的 QA 合併進此問答
	 * 並且將其答案轉化為選項，但目前僅支援 default 頻道的答案
	 */
	private Long mergeKidAsSingleOption = null;
	
	public Long getMergeKidAsSingleOption() {
		return mergeKidAsSingleOption;
	}

	public void setMergeKidAsSingleOption(Long mergeKidAsSingleOption) {
		this.mergeKidAsSingleOption = mergeKidAsSingleOption;
	}

	public String getQaid() {
		return qaid;
	}

	public void setQaid(String qaid) {
		this.qaid = qaid;
	}

	public String getOptionAction() {
		return optionAction;
	}

	public void setOptionAction(String optionAction) {
		this.optionAction = optionAction;
	}

	public String getPipe() {
		return pipe;
	}

	public void setPipe(String pipe) {
		this.pipe = pipe;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getMatchSentences() {
		return matchSentences;
	}

	public void setMatchSentences(String matchSentences) {
		this.matchSentences = matchSentences;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getExtraParams() {
		return extraParams;
	}

	public void setExtraParams(String extraParams) {
		this.extraParams = extraParams;
	}

	public String getExpiry() {
		return expiry;
	}

	public void setExpiry(String expiry) {
		this.expiry = expiry;
	}

	public String getMenuView() {
		return menuView;
	}

	public void setMenuView(String menuView) {
		this.menuView = menuView;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

	public String getHqaClass() {
		return hqaClass;
	}

	public void setHqaClass(String hqaClass) {
		this.hqaClass = hqaClass;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "ImportHierarchicalQA [" + (qaid != null ? "qaid=" + qaid + ", " : "") + (optionAction != null ? "optionAction=" + optionAction + ", " : "")
				+ (pipe != null ? "pipe=" + pipe + ", " : "") + (question != null ? "question=" + question + ", " : "")
				+ (answer != null ? "answer=" + answer + ", " : "") + (matchSentences != null ? "matchSentences=" + matchSentences + ", " : "")
				+ (script != null ? "script=" + script + ", " : "") + (extraParams != null ? "extraParams=" + extraParams + ", " : "")
				+ (expiry != null ? "expiry=" + expiry + ", " : "") + (menuView != null ? "menuView=" + menuView + ", " : "")
				+ (inputType != null ? "inputType=" + inputType + ", " : "") + (text != null ? "text=" + text + ", " : "")
				+ (hqaClass != null ? "hqaClass=" + hqaClass : "") + "]";
	}

}
