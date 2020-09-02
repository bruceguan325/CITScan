package com.intumit.solr.robot.function;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;

public class REPLACE2 extends FunctionBase {

	public REPLACE2(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		List<String> params = splitData(3);
		String str = params.get(0);
		String regex = params.get(2);
		String replacement = null;
		
		if (in != null) {
			replacement = in.getInput();
		}
		else {
			replacement = params.get(1);
		}
		
		return str.replaceAll(regex, replacement);
	}
}
