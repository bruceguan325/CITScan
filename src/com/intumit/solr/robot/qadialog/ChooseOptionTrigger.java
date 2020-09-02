package com.intumit.solr.robot.qadialog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;

import com.intumit.solr.robot.QAContext;

public class ChooseOptionTrigger extends NormalTrigger {
	public ChooseOptionTrigger() {
		super();
	}
	
	public ChooseOptionTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
	}

	Pattern numberPattern = Pattern.compile("第?(?<choosedNumber>\\d{1})個?");
	
	@Override
	public boolean isTrigger(QAContext ctx) {
		Matcher m = numberPattern.matcher(ctx.getCurrentQuestion());
		
		if (m.matches()) {
			int choosedNumber = Integer.parseInt(m.group("choosedNumber")) - 1;
			
			for (String assignExpr: contents) {
				JexlContext jctx = new MapContext();
				jctx.set("ctx", ctx);
				jctx.set("choosedNumber", choosedNumber);
				Number result = (Number)JexlUtil.runScript(jctx, assignExpr);
				
				if (result != null)
					return true;
			}
			return false;
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
