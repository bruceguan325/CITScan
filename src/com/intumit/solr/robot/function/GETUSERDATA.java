package com.intumit.solr.robot.function;

import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.UserClue;

public class GETUSERDATA extends FunctionBase {

	public GETUSERDATA(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String key = data;
		if (StringUtils.isEmpty(data) && in != null) {
			key = in.getInput();
		}
		
		UserClue uc = ctx.getUserClue();
		if (uc != null && StringUtils.isNotEmpty(key)) {
			if (StringUtils.equalsIgnoreCase(key, "line")) {
				return uc.getLineUserId();
			}
			else if (StringUtils.equalsIgnoreCase(key, "admin")) {
				return uc.getAdminUserId();
			}
			else if (StringUtils.equalsIgnoreCase(key, "cookie")) {
				return uc.getCookieUserId();
			}
			else if (StringUtils.equalsIgnoreCase(key, "facebook")) {
				return uc.getFbPsId();
			}
			else if (StringUtils.equalsIgnoreCase(key, "email")) {
				return uc.getEmail();
			}
			else if (StringUtils.equalsIgnoreCase(key, "nickname")) {
				return uc.getNickname();
			}
		}
		
		return null;
	}
}
