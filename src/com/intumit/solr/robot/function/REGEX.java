package com.intumit.solr.robot.function;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;

public class REGEX extends FunctionBase {

	public REGEX(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		List<String> params = splitData(3);
		String regex = params.get(1);
		String str = null;
		if (in != null) {
			str = in.getInput();
		} else {
			str = params.get(0);
		}

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);

		if (m.find()) {
			return m.group(1);
		}

		return null;
	}
}
