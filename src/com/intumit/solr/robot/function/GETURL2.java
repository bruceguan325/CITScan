package com.intumit.solr.robot.function;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class GETURL2 extends FunctionBase {

	public GETURL2(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String url = null;
		if (in != null) {
			url = in.getInput();
		}
		else {
			url = data;
		}
		
		String content = null;
		try {
			JSONObject ctxJson = new JSONObject(ctx.getCtxAttr());
			JSONObject reqJson = new JSONObject(ctx.getRequestAttributes());
			
			// 移除掉 "_" 開頭的，為內部參數不傳給外面
			for (String key: ctx.getCtxAttr().keySet()) {
				if (StringUtils.startsWith(key, "_")) {
					ctxJson.remove(key);
				}
			}
			for (String key: ctx.getRequestAttributes().keySet()) {
				if (StringUtils.startsWith(key, "_")) {
					reqJson.remove(key);
				}
			}
			
			JSONObject payload = new JSONObject();
			payload.put("ctx", ctxJson);
			payload.put("req", reqJson);
			content = WiSeUtils.postWithPayload(url, null, payload.toString().getBytes(), "UTF-8");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return content;
	}
}
