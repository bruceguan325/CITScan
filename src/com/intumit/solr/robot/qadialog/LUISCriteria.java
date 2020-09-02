package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;

public class LUISCriteria implements Serializable {
	Operator op;
	List<String> intents;
	List<String> entities;
	List<String> restrictToEntityCategory = null;
	
	public Operator getOp() {
		return op;
	}
	public void setOp(Operator op) {
		this.op = op;
	}
	public List<String> getIntents() {
		return intents;
	}
	public void setIntents(List<String> intents) {
		this.intents = intents;
	}
	public List<String> getEntities() {
		return entities;
	}
	public void setEntities(List<String> entities) {
		this.entities = entities;
	}
	public List<String> getRestrictToEntityCategory() {
		return restrictToEntityCategory;
	}
	public void setRestrictToEntityCategory(List<String> restrictToEntityCategory) {
		this.restrictToEntityCategory = restrictToEntityCategory;
	}
	
	@Override
	public String toString() {
		return "LUISCriteria [op=" + op + ", intents=" + intents
				+ ", entities=" + entities + "]";
	}

	public boolean isMatch(Collection<QAIntent> intentDbs, Collection<QAEntity> entityDbs) {
		Map<String, QAIntent> iMap = QAIntent.collToMap(intentDbs);
		Map<String, QAEntity> eMap = QAEntity.collToMap(entityDbs, restrictToEntityCategory);
		
		boolean result = false;
		
		if (op == Operator.AND || op == Operator.NOT)
			result = true;
		
		if (intents != null) {
			boolean iMapAny = false;
			
			for (String im: intents) {
				if (StringUtils.equalsIgnoreCase(im, ".*")) {
					iMapAny = true;
					break;
				}
			}
			
			for (String in: intents) {
				boolean contains = (iMapAny && iMap.size() > 0) || iMap.containsKey(in);
				if (op == Operator.OR) {
					result |= contains;
				}
				else if (op == Operator.AND) {
					result &= contains;
				}
				else if (op == Operator.NOT) {
					result &= !contains;
				}
			}
		}
		
		if (entities != null) {
			boolean eMapAny = false;
			
			for (String em: entities) {
				if (StringUtils.equalsIgnoreCase(em, ".*")) {
					eMapAny = true;
					break;
				}
			}
			
			for (String en: entities) {
				boolean contains = (eMapAny && eMap.size() > 0) || eMap.containsKey(en);
				if (op == Operator.OR) {
					result |= contains;
				}
				else if (op == Operator.AND) {
					result &= contains;
				}
				else if (op == Operator.NOT) {
					result &= !contains;
				}
			}
		}
		
		return result;
	}


	public static enum Operator {
		OR, AND, NOT,
	}
}