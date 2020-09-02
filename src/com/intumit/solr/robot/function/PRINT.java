package com.intumit.solr.robot.function;

import com.intumit.solr.robot.QAContext;

/**
 * PRINT 用來明確表明要把傳入的資料插入目前的字串當中
 * 
 * @author herb
 */
public class PRINT extends FunctionBase {

	public PRINT(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String printText = null;
		if (in != null) {
			printText = in.getInput();
		}
		else {
			printText = data;
		}
		
		return printText;
	}
}
