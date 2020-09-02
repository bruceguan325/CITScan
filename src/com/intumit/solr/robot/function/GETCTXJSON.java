package com.intumit.solr.robot.function;

import java.util.List;

import org.json.JSONObject;

import com.intumit.solr.robot.QAContext;

public class GETCTXJSON extends FunctionBase {

	public GETCTXJSON(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		List<String> params = splitData();
		String val = "";
		try {
			if (params.get(1).equals("COUNTS")) {
				int counts = 0;
				for (String key : ctx.getCtxAttr().keySet()) {
					if (key.contains(params.get(0))) {
						counts++;
					}
				}
				return counts;
			}
			for (String key : ctx.getCtxAttr().keySet()) {
				if (key.contains(params.get(0))) {
					String v = (String) ctx.getCtxAttr().get(key);
					JSONObject o = new JSONObject(v);
					if (val.length() > 0)
						val += ",";
					for (int i = 1; i < params.size(); i++) {
						if (o.has(params.get(i))) {
							val += o.getString(params.get(i));
						} else {
							val += params.get(i);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

		return val;
	}
}
