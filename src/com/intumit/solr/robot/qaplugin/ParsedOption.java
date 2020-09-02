package com.intumit.solr.robot.qaplugin;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionAction;

public class ParsedOption implements Serializable {
	public Long id = null;
	public String title = null;
	public OptionAction optionAction = null;
	public String question = null;
	public String answer = null;
	public JSONArray matchSentences = null;
	public String expiry = null;
	
	/**
	 * pipe 值根據 OptionAction 會有兩種意義
	 * 一個是 redirect 到 question， pipe 放的是 kid
	 * 另一種是 redirect 到其他 option， pipe 放的是其他 option 的 id
	 */
	public String pipe = null;
	public String script = null;
	public JSONObject extraParams = null;
	public MenuSelectionBehavior inputType = null;
	public MenuView menuView = null;
	public JSONArray children = null;
	
	public ParsedOption() {
		super();
	}
	
	public ParsedOption(String title, String question, OptionAction oa,String pipe) {
		super();
		this.title = title;
		this.question = question;
		this.pipe = pipe;
		if (!StringUtils.equals(title, question)) {
			try {
				this.matchSentences = new JSONArray(new String[] {title, question});
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.optionAction = oa;
	}
	
	public ParsedOption(String title, String question, OptionAction oa) {
		super();
		this.title = title;
		this.question = question;
		
		if (!StringUtils.equals(title, question)) {
			try {
				this.matchSentences = new JSONArray(new String[] {title, question});
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.optionAction = oa;
	}

	@Override
	public String toString() {
		return "ParsedOption [id=" + id + ", title=" + title + ", optionAction=" + optionAction + ", question=" + question
				+ ", answer=" + answer + ", expiry=" + expiry + ", pipe=" + pipe + ", script=" + script
				+ ", extraParams=" + extraParams + ", inputType=" + inputType + ", menuView=" + menuView
				+ ", children=" + children + "]";
	}
	
	/**
	 * 用問句是否一樣判斷是否是相同選項
	 */
	@Override
	public int hashCode() {
		return StringUtils.trimToEmpty(this.title).hashCode();
	}
	
	/**
	 * 用問句是否一樣判斷是否是相同選項
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ParsedOption) {
			ParsedOption that = (ParsedOption) o;
			
			if (optionAction != null) {
				switch (optionAction) {
					case INPUT_NUMBER:
					case INPUT_TEXT:
						return StringUtils.equalsIgnoreCase(that.question, this.question);
					case REDIRECT_TO_QUESTION:
					case REDIRECT_TO_OPTION:
						return StringUtils.equalsIgnoreCase(that.pipe, this.pipe);
					case DIRECT_ANSWER:
					case SUB_MENU_ONLY:
						return StringUtils.equalsIgnoreCase(that.title, this.title);
					case PROCESS_BY_QAPLUGIN:
						// 都不相同
				}
			}

			return StringUtils.equalsIgnoreCase(that.question, this.question);
		}
		return false;
	}
}