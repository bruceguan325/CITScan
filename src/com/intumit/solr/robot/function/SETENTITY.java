package com.intumit.solr.robot.function;

import java.util.List;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.entity.QAEntityType;

public class SETENTITY extends FunctionBase {

	public SETENTITY(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		List<String> params = splitData();
		QAEntity entity = null;
		if (params.size() == 3) {
			entity = QAEntity.get(ctx.getTenant().getId(), params.get(1), params.get(0), null);
		}
		if (params.size() == 4) {
			entity = QAEntity.get(ctx.getTenant().getId(), params.get(1), params.get(0), params.get(2));
		}
		if (entity == null) {
			if (params.size() == 3) {
				String val = params.get(2);
				if(val.equals("DELETE")){
					return "";
				}
				val = valReplaceEntityNotExist(val);
				QAEntity.save(ctx.getTenant().getId(), params.get(0), params.get(1), "", "", QAEntityType.STRING, val, false, true);
			}
			if (params.size() == 4) {
				String val = params.get(3);
				if(val.equals("DELETE")){
					return "";
				}
				val = valReplaceEntityNotExist(val);
				QAEntity.save(ctx.getTenant().getId(), params.get(0), params.get(1), params.get(2), "", QAEntityType.STRING, val, false, true);
			}
		} else if (entity != null) {
			if (params.size() == 3) {
				String val = params.get(2);
				if(val.equals("DELETE")){
					QAEntity.delete(entity);
					return "";
				}
				entity = valReplaceEntityExist(val, entity);
				if(entity.getEntityValues().length() > 0)
					QAEntity.saveOrUpdate(entity);
				else
					QAEntity.delete(entity);
			}
			if (params.size() == 4) {
				String val = params.get(3);
				if(val.equals("DELETE")){
					QAEntity.delete(entity);
					return "";
				}
				entity = valReplaceEntityExist(val, entity);
				if(entity.getEntityValues().length() > 0)
					QAEntity.saveOrUpdate(entity);
				else
					QAEntity.delete(entity);
			}
		}
		return "";
	}

	private String valReplaceEntityNotExist(String val) {
		if (val.startsWith("+")) {
			val = val.substring(1);
		}
		return val;
	}

	private QAEntity valReplaceEntityExist(String val, QAEntity entity) {
		if (val.startsWith("+")) {
			val = val.substring(1);
			if (!entity.getEntityValues().contains(val)) {
				entity.setEntityValues(entity.getEntityValues() + "|" + val);
			}
		} else if ((val.startsWith("-"))) {
			val = val.substring(1);
			if (entity.getEntityValues().contains(val + "|")) {
				entity.setEntityValues(entity.getEntityValues().replace(val + "|", ""));
			} else if (entity.getEntityValues().contains("|" + val)) {
				entity.setEntityValues(entity.getEntityValues().replace("|" + val, ""));
			} else {
				entity.setEntityValues(entity.getEntityValues().replace(val, ""));
			}
		}
		return entity;
	}
}
