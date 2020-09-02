package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.elasticsearch.common.lang3.StringUtils;

import com.intumit.solr.robot.QAContext;

public class Filter implements Serializable {
	String name;
	String showName;
	String type;
	String defaultQuestion;
	String targetIndexFieldName;
	int require;
	Boolean locked; // Locked 的話就不再 check rule，也不能改變 value 
	Boolean hidden;
	Boolean noContext;
	boolean multivalue;
	Boolean doubleConfirm;
	String doubleConfirmText;
	List<Rule> rules;
	List<Map<String, Object>> validators;
	List<String> entities;
	String currentValue;
	RuleCheckResult currentResult;
	String outputExpr;
	
	public Filter() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Filter other = (Filter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public void init() {
		if (rules != null && rules.size() > 0) {
			for (Rule r: rules) {
				r.init();
			}
		}
	}
	
	public RuleCheckResult checkRules(QAContext ctx) {
		RuleCheckResult result = null;
		
		if (rules != null && rules.size() > 0) {
			for (Rule r: rules) {
				result = r.check(ctx);
				
				if (result.isMatch()) {
					currentValue = QADialog.replaceVariables(ctx, result.getValue());
					return result;
				}
				else if (result.getStatus() == RuleCheckResultStatus.CALL_DIALOG) {
					return result;
				}
			}
		}
		
		if (result == null) {
			return RuleCheckResult.NO_MATCH;
		}
		
		return result;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShowName() {
		return showName;
	}

	public void setShowName(String showName) {
		this.showName = showName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDefaultQuestion() {
		return defaultQuestion;
	}

	public void setDefaultQuestion(String defaultQuestion) {
		this.defaultQuestion = defaultQuestion;
	}

	public String getTargetIndexFieldName() {
		return targetIndexFieldName;
	}

	public void setTargetIndexFieldName(String targetIndexFieldName) {
		this.targetIndexFieldName = targetIndexFieldName;
	}

	public int getRequire() {
		return require;
	}

	public void setRequire(int require) {
		this.require = require;
	}

	public Boolean getLocked() {
		return locked != null ? locked : Boolean.FALSE;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Boolean isHidden() {
		return hidden != null ? hidden : Boolean.FALSE;
	}

	public Boolean getHidden() {
		return hidden != null ? hidden : Boolean.FALSE;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getNoContext() {
		return noContext != null ? noContext : Boolean.FALSE;
	}

	public void setNoContext(Boolean noContext) {
		this.noContext = noContext;
	}

	public boolean isMultivalue() {
		return multivalue;
	}

	public void setMultivalue(boolean multivalue) {
		this.multivalue = multivalue;
	}

	public Boolean getDoubleConfirm() {
		return doubleConfirm;
	}

	public void setDoubleConfirm(Boolean doubleConfirm) {
		this.doubleConfirm = doubleConfirm;
	}

	public String getDoubleConfirmText() {
		return doubleConfirmText;
	}

	public void setDoubleConfirmText(String doubleConfirmText) {
		this.doubleConfirmText = doubleConfirmText;
	}

	public String getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}

	public String getCurrentValueForShow() {
		if (currentResult != null) {
			return currentResult.getValueForShow();
		}
		return currentValue;
	}

	public RuleCheckResult getCurrentResult() {
		return currentResult;
	}

	public void setCurrentResult(RuleCheckResult currentResult) {
		this.currentResult = currentResult;
	}

	public String getOutputExpr() {
		return outputExpr;
	}

	public void setOutputExpr(String outputExpr) {
		this.outputExpr = outputExpr;
	}

	public List<String> getEntities() {
		return entities;
	}

	public void setEntities(List<String> entities) {
		this.entities = entities;
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public List<Map<String, Object>> getValidators() {
		return validators;
	}

	public void setValidators(List<Map<String, Object>> validators) {
		this.validators = validators;
	}
	
	public Map<String, Object> validate(QAContext ctx, QADialog dlg) {
		if (validators != null && validators.size() > 0) {
			for (Map<String, Object> v: validators) {
				String expr = (String)v.get("expr");
				String errorText = (String)v.get("errorText");
				
				JexlContext jctx = new MapContext();
				jctx.set("ctx", ctx);
				jctx.set("dlg", dlg);
				jctx.set("field", this);
				jctx.set("currentField", this);
				Boolean result = (Boolean)JexlUtil.runExpr(jctx, expr);
				
				if (result != null && !result)
					return v;
			}
		}
		
		return null;
	}

	@Override
	public String toString() {
		return "Filter [name=" + name + ", showName=" + showName + ", type="
				+ type + ", defaultQuestion=" + defaultQuestion
				+ ", targetIndexFieldName=" + targetIndexFieldName
				+ ", require=" + require + ", locked=" + locked + ", hidden="
				+ hidden + ", noContext=" + noContext + ", multivalue="
				+ multivalue + ", doubleConfirm=" + doubleConfirm
				+ ", doubleConfirmText=" + doubleConfirmText + ", rules="
				+ rules + ", validators=" + validators + ", entities="
				+ entities + ", currentValue=" + currentValue
				+ ", currentResult=" + currentResult + ", outputExpr="
				+ outputExpr + "]";
	}

	public Integer getIntValue() {
		return new Integer(currentValue);
	}

	public Double getDoubleValue() {
		return new Double(currentValue);
	}
}