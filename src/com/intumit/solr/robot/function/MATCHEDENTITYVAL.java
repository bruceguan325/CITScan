package com.intumit.solr.robot.function;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.entity.QAEntity;

public class MATCHEDENTITYVAL extends FunctionBase {

	public MATCHEDENTITYVAL(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		List<String> params = splitData();
		String entityCode = params.get(0);
		
		if (StringUtils.isEmpty(entityCode) && in != null) {
			entityCode = in.getInput();
		}
		
		int offset = 1;
		if (params.size() > 1) {
			offset = Integer.parseInt(params.get(1));
		}
		
		Set<QAEntity> entities = ctx.getEntities();
		
		if (entities != null && entities.size() >= offset) {
			for (QAEntity e: entities) {
				if (StringUtils.equalsIgnoreCase(entityCode, e.getCode()) && offset <= 1) {
					return e.getEntityValues();
				}
				offset--;
			}
		}
		
		return null;
	}
}
