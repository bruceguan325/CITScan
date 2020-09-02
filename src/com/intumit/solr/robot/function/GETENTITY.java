package com.intumit.solr.robot.function;

import java.util.List;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.entity.QAEntity;

public class GETENTITY extends FunctionBase {

	public GETENTITY(String originalText, String data) {
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
		QAEntity entity = null;
		if (params.size() == 3) {
			entity = QAEntity.get(ctx.getTenant().getId(), params.get(1), params.get(0), null);
			if (entity == null)
				return val;
			if (params.get(2).equals("ALL")) {
				ctx.setRequestAttribute("QAENTITY", entity);
				val = entity.getEntityValues();
			} else if (params.get(2).equals("EACH")) {
				String[] vals = entity.getEntityValues().split("\\|");
				val = vals[0];
			}
		}
		if (params.size() == 4) {
			entity = QAEntity.get(ctx.getTenant().getId(), params.get(1), params.get(0), params.get(2));
			if (entity == null)
				return val;
			if (params.get(3).equals("ALL")) {
				ctx.setRequestAttribute("QAENTITY", entity);
				val = entity.getEntityValues();
			} else if (params.get(3).equals("EACH")) {
				String[] vals = entity.getEntityValues().split("\\|");
				val = vals[0];
			}
		}
		return val;
	}
}
