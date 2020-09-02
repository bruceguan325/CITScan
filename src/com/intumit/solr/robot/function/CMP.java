package com.intumit.solr.robot.function;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;

/**
 * {{CMP:eq::[[GETCTX:ABC]]::你好}}
 * 
 * 比對 ctx.ctxAttr.ABC 的值是否為 "你好" 兩個字
 * 
 * @author herb
 *
 */
public class CMP extends FunctionBase {

	public CMP(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		boolean result = false;

		List<String> params = splitData(3);
		String operator = params.get(0);
		String lVal = params.get(1);
		String rVal;
		
		if (in != null) {
			rVal = in.getInput();
		}
		else {
			rVal = params.get(2);
		}
		
		if (operator.equals("eq")) {
			result = StringUtils.equals(lVal, rVal);
		}
		else if (operator.equals("eqi")) {
			result = StringUtils.equalsIgnoreCase(lVal, rVal);
		}
		else if (operator.equals("neq")) {
			result = !StringUtils.equals(lVal, rVal);
		}
		else if (operator.equals("neqi")) {
			result = !StringUtils.equalsIgnoreCase(lVal, rVal);
		}
		else if (operator.equals("eqany")) {
			Set<String> all = new HashSet<>();
			
			if (rVal.contains("::")) {
				all.addAll(Arrays.asList(StringUtils.splitByWholeSeparator(rVal, "::")));
			}
			else {
				all.add(rVal);
			}
			result = all.contains(lVal);
		}
		else if (operator.equals("eqiany")) {
			Set<String> all = new HashSet<>();
			
			if (rVal.contains("::")) {
				all.addAll(Arrays.asList(StringUtils.splitByWholeSeparator(rVal.toLowerCase(), "::")));
			}
			else {
				all.add(rVal.toLowerCase());
			}
			result = all.contains(lVal.toLowerCase());
		}
		else if (operator.equals("neqany")) {
			Set<String> all = new HashSet<>();
			
			if (rVal.contains("::")) {
				all.addAll(Arrays.asList(StringUtils.splitByWholeSeparator(rVal, "::")));
			}
			else {
				all.add(rVal);
			}
			result = !all.contains(lVal);
		}
		else if (operator.equals("neqiany")) {
			Set<String> all = new HashSet<>();
			
			if (rVal.contains("::")) {
				all.addAll(Arrays.asList(StringUtils.splitByWholeSeparator(rVal.toLowerCase(), "::")));
			}
			else {
				all.add(rVal.toLowerCase());
			}
			result = !all.contains(lVal.toLowerCase());
		}
		
		return result;
	}
}
