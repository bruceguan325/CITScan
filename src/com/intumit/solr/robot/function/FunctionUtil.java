package com.intumit.solr.robot.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.UserClueTag;
import com.intumit.solr.tenant.Tenant;

public class FunctionUtil {
	
	public static List<RobotFunction> mapToRobotFunction(ArrayList<Map<String, Object>> funcMap) {
		List<RobotFunction> funcs = new ArrayList<RobotFunction>();

		if (funcMap != null)
		for (Map<String, Object> funcEntry: funcMap) {
			try {
				RobotFunction f = (RobotFunction)funcEntry.get("func");
				if (f != null)
					funcs.add(f);
			}
			catch (Exception regEx) {
				regEx.printStackTrace();
			}
		}
		
		return funcs;
	}

	public static Object collectAndExec(String str, QAContext ctx) {
		ArrayList<Map<String, Object>> funcMap = collect(str, new FunctionCollector(ctx));
		List<RobotFunction> funcs = mapToRobotFunction(funcMap);
		
		UserInput in = null;
		Object lastRet = null;
		
		if (funcs != null && funcs.size() > 0)
		for (RobotFunction func: funcs) {
			//System.out.print("Exec func[" + func.getClass() + "] with input[" + in + "]:");
			try {
				if (func instanceof PIPE) {
					//System.out.println("piped!");
					if (lastRet != null) {
						in = new UserInput(lastRet.toString());
					}
				}
				else {
					lastRet = func.exec(ctx, in);
					in = null;
					//System.out.println(" ret:[" + lastRet + "]");
				}
			}
			catch (Exception funcEx) {
				funcEx.printStackTrace();
			}
		}
		
		return lastRet;
	}

	static final Pattern templatePattern = Pattern.compile("\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)(:.+?)?\\}\\}");

	interface Collector {
		/** 收集字串沒有回傳null */
		Map<String, Object> call(String name, String val, String originalStr, int start, int end);
	}
	
	static ArrayList<Map<String, Object>> collect(String tpl, Collector c){
		ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		if(tpl != null){
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				String originalStr = matcher.group();
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));

				Map<String, Object> collected = c.call(name, val, originalStr, start, end);
				if (collected != null){
					//System.out.println( "TemplateUtil.collect : " + new JSONObject(collected).toString() );
					out.add(collected);
				}
			}
		}
		if (out.size()<1)
			out = null;
		return out;
	}
	
	/**
	 * 執行字串內的 functions，並且替換調 function 文字
	 * @param str
	 * @param ctx
	 * @return
	 */
	public static String collectExecAndReplace(String str, QAContext ctx) {
		ArrayList<Map<String, Object>> funcMap = collect(str, new FunctionCollector(ctx));
		List<RobotFunction> funcs = mapToRobotFunction(funcMap);
		
		UserInput in = null;
		Object lastRet = null;
		StringBuffer buf = new StringBuffer();
		
		int pos = 0;
		int funcPointer = 1;
		
		if (funcs != null && funcs.size() > 0)
		for (RobotFunction func: funcs) {
			int newPos = str.indexOf(func.getOriginalText(), pos);
			//System.out.print("Exec and replace func (" + funcPointer + ") [" + func.getOriginalText() + "] with input[" + in + "] piece [" + pos + ", " + newPos + "]:");
			//System.out.println(buf.toString());
			buf.append(str.substring(pos, newPos));
			pos = newPos;
			
			try {
				if (func instanceof PIPE) {
					//System.out.println("piped!");
					if (lastRet != null) {
						in = new UserInput(lastRet.toString());
					}
				}
				else if (func instanceof PRINT || funcPointer == funcs.size()) {
					//System.out.println("print!");
					lastRet = func.exec(ctx, in);
					if (lastRet != null) {
						String p = lastRet.toString();
						buf.append(p);
					}
					in = null;
				}
				else if (funcPointer < funcs.size() && !(funcs.get(funcPointer) instanceof PIPE)) {
					//System.out.println("print!");
					lastRet = func.exec(ctx, in);
					if (lastRet != null) {
						String p = lastRet.toString();
						buf.append(p);
					}
					in = null;
				}
				else {
					lastRet = func.exec(ctx, in);
					/*if (lastRet != null) {
						String p = lastRet.toString();
						buf.append(p);
					}*/
					in = null;
					//System.out.println(" ret:[" + lastRet + "]");
				}
				pos += func.getOriginalText().length();
			}
			catch (Exception funcEx) {
				funcEx.printStackTrace();
			}
			finally {
				funcPointer++;
			}
		}
		
		//System.out.println("Final [" + str.substring(pos) + "]");
		buf.append(str.substring(pos));
		
		return buf.toString();
	}
	
	/**
	 * Split by separator, but take the Inline Function syntax into consideration.
	 * For ex. "{{ABC:DEF}}:{{OOO:XXX}}", split by ":", the result should be "{{ABC:DEF}}" and "{{OOO:XXX}}", NOT "{{ABC", "DEF}}", "{{OOO", "XXX}}"
	 * @param str
	 * @return
	 */
	public static List<String> split(String str, String separator) {
		Matcher matcher = templatePattern.matcher(str);
		List<String> res = new ArrayList<>();
		
		int lastCutPnt = 0;
		int cursor = 0;
		boolean[] mask = new boolean[str.length()];
		Arrays.fill(mask, false);
		
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			
			Arrays.fill(mask, start, end, true);
		}
		
		int idx = 0;
		while ((idx = StringUtils.indexOf(str, separator, cursor)) != -1) {
			if (!mask[idx]) {
				res.add(StringUtils.substring(str, lastCutPnt, idx));
				lastCutPnt = idx + 1;
			}
			
			cursor = idx + 1;
		}
		
		if (idx == -1) {
			res.add(StringUtils.substring(str, lastCutPnt, str.length()));
		}
		
		return res;
	}
}
