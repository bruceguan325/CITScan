package com.intumit.solr.robot.function;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.TemplateUtil.Collector;
import com.intumit.solr.robot.function.*;

public class FunctionCollector implements FunctionUtil.Collector {
	public FunctionCollector(QAContext ctx) {
	}

	@Override
	public Map<String, Object> call(String name, String val, String originalString, int start, int end) {
		Map<String, Object> map = new HashMap<String, Object>(5);
		RobotFunction func = null;
		//String originalString = "{{" + name + (val != null ? ":" + val : "") + "}}";

		if (StringUtils.equals(name, "APPENDOPTION") || StringUtils.equals(name, "OPTIONS")) {
			func = new APPENDOPTION(originalString, val);
		}
		else if (StringUtils.equals(name, "CONST")) {
			func = new CONST(originalString, val);
		}
		else if (StringUtils.equals(name, "DATECALCULATE")) {
			func = new DATECALCULATE(originalString, val);
		}
		else if (StringUtils.equals(name, "EMPTY")) {
			func = new EMPTY(originalString, val);
		}
		else if (StringUtils.equals(name, "ISEMPTY")) {
			func = new ISEMPTY(originalString, val);
		}
		else if (StringUtils.equals(name, "INQUIRY")) {
			func = new INQUIRY(originalString, val);
		}
		else if (StringUtils.equals(name, "GETQ")) {
			func = new GETQ(originalString, val);
		}
		else if (StringUtils.equals(name, "GETCHOOSEDOPT")) {
			func = new SETREQ(originalString, val);
		}
		else if (StringUtils.equals(name, "GETURL1")) {
			func = new GETURL1(originalString, val);
		}
		else if (StringUtils.equals(name, "GETURL2")) {
			func = new GETURL2(originalString, val);
		}
		else if (StringUtils.equals(name, "GETCTX")) {
			func = new GETCTX(originalString, val);
		}
		else if (StringUtils.equals(name, "GETDLG")) {
			func = new GETDLG(originalString, val);
		}
		else if (StringUtils.equals(name, "GETUSERDATA")) {
			func = new GETUSERDATA(originalString, val);
		}
		else if (StringUtils.equals(name, "GETREQ")) {
			func = new GETREQ(originalString, val);
		}
		else if (StringUtils.equals(name, "GETJSONVAL") || StringUtils.equals(name, "JSONPATH")) {
			func = new GETJSONVAL(originalString, val);
		}
		else if (StringUtils.equals(name, "PIPE")) {
			func = new PIPE(originalString, val);
		}
		else if (StringUtils.equals(name, "PRINT")) {
			func = new PRINT(originalString, val);
		}
		else if (StringUtils.equals(name, "REGEX")) {
			func = new REGEX(originalString, val);
		}
		else if (StringUtils.equals(name, "REPLACE1")) {
			func = new REPLACE1(originalString, val);
		}
		else if (StringUtils.equals(name, "REPLACE2")) {
			func = new REPLACE2(originalString, val);
		}
		else if (StringUtils.equals(name, "SETCTX")) {
			func = new SETCTX(originalString, val);
		}
		else if (StringUtils.equals(name, "SETREQ")) {
			func = new SETREQ(originalString, val);
		}
		/*else if (StringUtils.equals(name, "GETINTENT")) {
			func = new GETINTENT(originalString, val);
		}
		else if (StringUtils.equals(name, "SETINTENT")) {
			func = new SETINTENT(originalString, val);
		}*/
		else if (StringUtils.equals(name, "GETENTITY")) {
			func = new GETENTITY(originalString, val);
		}
		else if (StringUtils.equals(name, "SETENTITY")) {
			func = new SETENTITY(originalString, val);
		}
		else if (StringUtils.equals(name, "SETCTXJSON")) {
			func = new SETCTXJSON(originalString, val);
		}
		else if (StringUtils.equals(name, "GETCTXJSON")) {
			func = new GETCTXJSON(originalString, val);
		}
		else if (StringUtils.equals(name, "CALCULATE")) {
			func = new CALCULATE(originalString, val);
		}
		else if (StringUtils.equals(name, "CMP")) {
			func = new CMP(originalString, val);
		}
		else if (StringUtils.equals(name, "NCMP")) {
			func = new NCMP(originalString, val);
		}
		else if (StringUtils.equals(name, "EXPR")) {
			func = new EXPR(originalString, val);
		}
		else if (StringUtils.equals(name, "MATCHEDENTITYVAL")) {
			func = new MATCHEDENTITYVAL(originalString, val);
		}
		else if (StringUtils.equals(name, "USERTAG")) {
			func = new USERTAG(originalString, val);
		}

		if (func != null) {
			map.put("funcName", name);
			map.put("func", func);
			map.put("originalString", originalString);
			map.put("start", start);
			map.put("end", end);
			return map;
		}
		return null;
	}
}