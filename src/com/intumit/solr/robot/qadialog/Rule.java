package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

public abstract class Rule implements Serializable {
	boolean reconstructQuestion = Boolean.TRUE;
	List<Map<String, String>> testCases;

	public Rule() {}
	
	public Rule(JSONObject cfg) {
		super();
		loadConfig(cfg);
	}
	
	public abstract void loadConfig(JSONObject cfg);
	public abstract void init();

	public abstract RuleCheckResult check(QAContext ctx);

	public List<Map<String, String>> getTestCases() {
		return testCases;
	}

	public void setTestCases(List<Map<String, String>> testCases) {
		this.testCases = testCases;
	}
	
	public boolean isReconstructQuestion() {
		return reconstructQuestion;
	}

	public void setReconstructQuestion(boolean reconstructQuestion) {
		this.reconstructQuestion = reconstructQuestion;
	}

	public boolean runTest() {
		
		if (testCases != null) {
			try {
				for (Map<String, String> testCase: testCases) {
					QAContext tmp = new QAContext();
					tmp.setCurrentQuestion(testCase.get("input"));
					
					RuleCheckResult result = check(tmp);
					System.out.println("Running testCase of rule[" + toString() + "]");
					
					if (testCase.containsKey("assertEquals")) {
						if (!result.isMatch() || !StringUtils.equals(result.getValue(), testCase.get("assertEquals"))) {
							System.out.print("[Failed!]... ");
							System.out.println(String.format("Rule testCase (input[%s], %s[%s != %s]) match[%s]", tmp.getCurrentQuestion(), "assertEquals", result.getValue(), testCase.get("assertEquals"), "" + result.isMatch()));
							return false;
						}
						else {
							System.out.print("[Pass!]... ");
						}
						System.out.println(String.format("Rule testCase (input[%s], %s[%s]) match[%s]", tmp.getCurrentQuestion(), "assertEquals", result.getValue(), "" + result.isMatch()));
					}
					else if (testCase.containsKey("assertNotMatch")) {
						if (result.isMatch()) {
							System.out.print("[Failed!]... ");
							System.out.println(String.format("Rule testCase (input[%s], %s[%s]) match[%s]", tmp.getCurrentQuestion(), "assertNotMatch", result.getValue(), "" + result.isMatch()));
							return false;
						}
						else {
							System.out.print("[Pass!]... ");
						}
						System.out.println(String.format("Rule testCase (input[%s], %s[%s]) match[%s]", tmp.getCurrentQuestion(), "assertNotMatch", result.getValue(), "" + result.isMatch()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		else {
			System.out.println("This rule has no testCase[" + toString() + "]");
		}
		
		return true;
	}
}
