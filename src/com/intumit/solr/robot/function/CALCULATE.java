package com.intumit.solr.robot.function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;

public class CALCULATE extends FunctionBase {

	public CALCULATE(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		String result = "";
		Pattern pp = Pattern.compile("(.*)(\\+|\\-|\\*|\\/|\\>|\\<)(.*)");
		Matcher mm = pp.matcher(data);
		float le = 0f;
		float ri = 0f;
		while (mm.find()) {
			String left = mm.group(1);
			String condition = mm.group(2);
			String right = mm.group(3);
			try {
				le = Float.valueOf(left);
				ri = Float.valueOf(right);
			} catch (Exception e) {
				System.out.println(e);
			}
			switch (condition) {
			case "+":
				result = String.valueOf((int) (le + ri));
				break;
			case "-":
				result = String.valueOf((int) (le - ri));
				break;
			case "*":
				result = String.valueOf((float) (le * ri));
				break;
			case "/":
				result = String.valueOf((float) (le / ri));
				break;
			case ">":
				result = String.valueOf((boolean) (le > ri));
				if(result.equals("false"))
					result = "";
				break;
			case "<":
				result = String.valueOf((boolean) (le < ri));
				if(result.equals("false"))
					result = "";
				break;
			}

		}
		return result;
	}
}
