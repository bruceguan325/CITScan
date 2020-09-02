package com.intumit.solr.robot.function;

import java.util.List;

import com.intumit.solr.robot.QAContext;

public class SETREQ extends FunctionBase {

	public SETREQ(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		List<String> params = splitData(2);
		String key = params.get(0);
		String val = null;
		if (in != null) {
			val = in.getInput();
		}
		else {
			val = params.get(1);
		}

		ctx.setRequestAttribute(key, val);
		
		return val;
	}
}
