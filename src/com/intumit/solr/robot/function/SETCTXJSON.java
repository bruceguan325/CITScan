package com.intumit.solr.robot.function;

import java.util.List;

import org.json.JSONObject;

import com.intumit.solr.robot.QAContext;

public class SETCTXJSON extends FunctionBase {

	public SETCTXJSON(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		List<String> params = splitData();
		JSONObject o = new JSONObject();
		try {
			String val1 = params.get(1);

			if (val1.equals("ALL")) {
				for (String key : ctx.getCtxAttr().keySet()) {
					if (key.contains(params.get(0))) {
						String val = (String) ctx.getCtxAttr().get(key);
						o = new JSONObject(val);
						setObjectToCtx(params, o, ctx, key);
					}
				}
				return "";
			} else if (ctx.getCtxAttr().containsKey(params.get(0) + val1)) {
				String val = (String) ctx.getCtxAttr().get(params.get(0) + val1);
				o = new JSONObject(val);
			}
			// 變數一個一個加
			setObjectToCtx(params, o, ctx, params.get(0) + val1);
		} catch (Exception e) {
			System.out.println(e);
		}
		return "";
	}

	// 變數一個一個加
	private void setObjectToCtx(List<String> params, JSONObject o, QAContext ctx, String key) {
		try {
			if (params.size() % 2 == 0) {
				for (int i = 2; i < params.size(); i = i + 2) {
					o.put(params.get(i), params.get(i + 1));
				}
				ctx.setCtxAttr(key, o.toString());
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}
}
