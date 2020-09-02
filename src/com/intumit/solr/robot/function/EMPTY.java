package com.intumit.solr.robot.function;

import com.intumit.solr.robot.QAContext;

/**
 * 直接回傳空白（便於處理某些 function call 不想要有輸出的）
 * 
 * @author herb
 */
public class EMPTY extends FunctionBase {

	public EMPTY(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		return "";
	}
}
