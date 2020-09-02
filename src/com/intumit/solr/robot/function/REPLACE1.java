package com.intumit.solr.robot.function;

import java.util.List;

import com.intumit.solr.robot.QAContext;

public class REPLACE1 extends FunctionBase {

	public REPLACE1(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		List<String> params = splitData(3);
		String replacement = params.get(1);
		String regex = params.get(2);
		String str = null;
		if (in != null) {
			str = in.getInput();
		}
		else {
			str = params.get(0);
		}
		
		return str.replaceAll(regex, replacement);
	}
}
