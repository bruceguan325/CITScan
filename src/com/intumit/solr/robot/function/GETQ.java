package com.intumit.solr.robot.function;

import com.intumit.solr.robot.QAContext;

public class GETQ extends FunctionBase {

	public GETQ(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		return ctx.getCurrentQuestion();
	}
}
