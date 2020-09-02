package com.intumit.solr.robot.function;

import java.util.List;

import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.UserClueTag;

public class USERTAG extends FunctionBase {

	public USERTAG(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		List<String> params = splitData();
		String subfunc = params.get(0);

		UserClue uc = ctx.getUserClue();
		
		if (uc != null) {
    		if (StringUtils.startsWithIgnoreCase(subfunc, "count")) {
    			String tagName = params.get(1);
    			List<UserClueTag> tags = UserClueTag.findUserClueTags(uc, tagName);
    			int count = tags != null ? tags.size() : 0;
    			
    			if (StringUtils.equalsIgnoreCase(subfunc, "count"))
    				return count;

    			int val = Integer.parseInt(params.get(2));
    			if (StringUtils.equalsIgnoreCase(subfunc, "countEq"))
    				return count == val;
    			else if (StringUtils.equalsIgnoreCase(subfunc, "countNeq"))
    				return count != val;
    			else if (StringUtils.equalsIgnoreCase(subfunc, "countGt"))
    				return count > val;
    			else if (StringUtils.equalsIgnoreCase(subfunc, "countGe"))
    				return count >= val;
    			else if (StringUtils.equalsIgnoreCase(subfunc, "countLt"))
    				return count < val;
    			else if (StringUtils.equalsIgnoreCase(subfunc, "countLe"))
    				return count <= val;
    		}
    		else if (StringUtils.equalsIgnoreCase(subfunc, "has")) {
    			String tagName = params.get(1);
    			List<UserClueTag> tags = UserClueTag.findUserClueTags(uc, tagName);
    			return tags != null && tags.size() > 0;
    		}
    		else if (StringUtils.equalsIgnoreCase(subfunc, "add")) {
    			String tagName = params.get(1);
    			String addInfo = null;
    			
    			if (params.size() > 2) {
    				addInfo = params.get(2);
    			}
    			
    			if (addInfo != null) {
    				UserClueTag.addTagToUser(uc, tagName, addInfo);
    			} else {
    				UserClueTag.addTagToUser(uc, tagName);
    			}
    			List<UserClueTag> tags = UserClueTag.findUserClueTags(uc, tagName);
    			int count = tags != null ? tags.size() : 0;
    			return count;
    		}
    		else if (StringUtils.equalsIgnoreCase(subfunc, "del")) {
    			String tagName = params.get(1);
    			List<UserClueTag> tags = UserClueTag.findUserClueTags(uc, tagName);
    			int count = tags != null ? tags.size() : 0;
    			UserClueTag.clearTagOnUser(uc, tagName);
    			return count;
    		}
		}
		
		return null;
	}
}
