package com.intumit.solr.robot.function;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class GETCTX extends FunctionBase {

	public GETCTX(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String key = data;
		if (StringUtils.isEmpty(data) && in != null) {
			key = in.getInput();
		}
		
		return ctx.getCtxAttr(key);
	}
}
