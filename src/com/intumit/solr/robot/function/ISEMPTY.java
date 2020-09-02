package com.intumit.solr.robot.function;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;

public class ISEMPTY extends FunctionBase {

	public ISEMPTY(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String str = null;
		if (in != null) {
			str = in.getInput();
		}
		else {
			str = data;
		}
		
		if (StringUtils.trimToNull(str) == null) {
			return Boolean.TRUE;
		}
		
		return Boolean.FALSE;
	}
}
