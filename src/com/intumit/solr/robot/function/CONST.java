package com.intumit.solr.robot.function;

import com.intumit.solr.robot.QAContext;

public class CONST extends FunctionBase {

	public CONST(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String constant = data;
		return constant;
	}
}
