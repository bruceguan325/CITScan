package com.intumit.solr.robot.qadialog;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.CallDialogRuleCheckResult.RewriteType;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;

public class AddressRule extends Rule {

	Map<String, Object> administrativeArea;
	Map<String, Object> locality;
	Map<String, Object> sublocality;
	Map<String, Object> streetAndNumber;
	
	public AddressRule() {
	}
	
	public AddressRule(JSONObject cfg) {
		super(cfg);
	}
	
	public Map<String, Object> getAdministrativeArea() {
		return administrativeArea;
	}

	public void setAdministrativeArea(Map<String, Object> administrativeArea) {
		this.administrativeArea = administrativeArea;
	}

	public Map<String, Object> getLocality() {
		return locality;
	}

	public void setLocality(Map<String, Object> locality) {
		this.locality = locality;
	}

	public Map<String, Object> getSublocality() {
		return sublocality;
	}

	public void setSublocality(Map<String, Object> sublocality) {
		this.sublocality = sublocality;
	}

	public Map<String, Object> getStreetAndNumber() {
		return streetAndNumber;
	}

	public void setStreetAndNumber(Map<String, Object> streetAndNumber) {
		this.streetAndNumber = streetAndNumber;
	}

	@Override
	public void loadConfig(JSONObject cfg) {
	}

	@Override
	public void init() {
	}

	@Override
	public String toString() {
		return "AddressRule [testCases=" + testCases + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		List<String> questions = null;
		if (isReconstructQuestion()) {
			questions = (List<String>)ctx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			if (!questions.contains(ctx.getCurrentQuestion())) {
				questions.add(0, ctx.getCurrentQuestion());
			}
		}
		else {
			questions = Arrays.asList(new String[] {ctx.getCurrentQuestion()});
		}
		
		for (String question: questions) {
			TaiwanAddressNormalizeUtil anu = AVMQADialog.getAddressNormalizeUtil(StringUtils.upperCase(question));
			
			if (StringUtils.trimToNull(anu.getFormatAddress()) != null) {
				if ((Boolean)administrativeArea.get("require") && anu.getAdministrative_area() == null) {
					return new CallDialogRuleCheckResult(RuleCheckResultStatus.CALL_DIALOG, anu.getFormatAddress(), this, (String)administrativeArea.get("targetDialog"), RewriteType.PREPEND, anu.getFormatAddress());
				}
				else if ((Boolean)locality.get("require") && anu.getLocality() == null) {
					return new CallDialogRuleCheckResult(RuleCheckResultStatus.CALL_DIALOG, anu.getFormatAddress(), this, (String)locality.get("targetDialog"), RewriteType.APPEND, anu.getFormatAddress());
				}
				else if ((Boolean)sublocality.get("require") && anu.getSublocality() == null) {
					return new CallDialogRuleCheckResult(RuleCheckResultStatus.CALL_DIALOG, anu.getFormatAddress(), this, (String)sublocality.get("targetDialog"), RewriteType.APPEND, anu.getFormatAddress());
				}
				else if ((Boolean)streetAndNumber.get("require") && anu.getStreet_number() == null) {
					return new CallDialogRuleCheckResult(RuleCheckResultStatus.CALL_DIALOG, anu.getFormatAddress(), this, (String)streetAndNumber.get("targetDialog"), RewriteType.APPEND, anu.getFormatAddress());
				}
				return new AddressRuleCheckResult(RuleCheckResultStatus.MATCH, anu.getFormatAddress(), this, anu, null);
			}
		}
		return RuleCheckResult.NO_MATCH;
	}
}
