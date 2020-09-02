package com.intumit.solr.robot.function;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class GETURL1 extends FunctionBase {

	public GETURL1(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String url = null;
		if (in != null) {
			url = in.getInput();
		}
		else {
			url = data;
		}
		
		String content = null;
		try {
			content = WiSeUtils.getDataFromUrl(url);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return content;
	}
}
