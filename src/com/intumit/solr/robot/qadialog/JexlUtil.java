package com.intumit.solr.robot.qadialog;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;


public class JexlUtil {
	static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();

	public static Object runExpr(JexlContext ctx, String exprStr) {
		JexlExpression je = jexl.createExpression(exprStr);
		ctx.set("out", System.out);
		
		return je.evaluate(ctx);
	}
	
	public static Object runScript(JexlContext ctx, String scriptStr) {
		JexlScript js = jexl.createScript(scriptStr);
		ctx.set("out", System.out);
		
		return js.execute(ctx);
	}
}
