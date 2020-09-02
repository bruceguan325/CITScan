package com.intumit.solr.robot.qadialog;

import java.util.List;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import com.intumit.solr.robot.QAContext;

public class ExpressionTrigger extends NormalTrigger {
	
	public ExpressionTrigger() {
		super();
	}
	
	public ExpressionTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
	}

	@Override
	public boolean isTrigger(QAContext ctx) {
		for (String criteria: contents) {
			JexlContext jctx = new MapContext();
			jctx.set("ctx", ctx);
			Boolean result = (Boolean)JexlUtil.runExpr(jctx, criteria);
			
			if (result != null && result) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		JexlContext jctx = new MapContext();
		jctx.set("n1", "5024");
		jctx.set("n2", "1024000");

		Boolean result = (Boolean)JexlUtil.runExpr(jctx, "new('java.lang.Double', n1) > new('java.lang.Double', n2)");
		System.out.println(result);
	}
}
