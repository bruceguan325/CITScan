package com.intumit.solr.robot.qadialog;

import java.util.Collection;
import java.util.List;

import org.apache.wink.json4j.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qarule.PrepareForQAMatchingRule;
import com.intumit.solr.tenant.Tenant;

import flexjson.JSONDeserializer;

public class LUISRule extends Rule {
	List<LUISCriteria> match;

	public LUISRule() {
	}
	
	public LUISRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
	}

	public List<LUISCriteria> getMatch() {
		return match;
	}

	public void setMatch(List<LUISCriteria> match) {
		this.match = match;
	}

	@Override
	public void init() {
	}

	@Override
	public String toString() {
		return "LUISRule [match=" + match + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		// { "op": "AND", "intents": ["WHEN", "BEFORE"], "entities": ["PAY"] }
		
		Collection<QAIntent> intentDbs = (Collection<QAIntent>)ctx.getRequestAttribute("intents");
		Collection<QAEntity> entityDbs = (Collection<QAEntity>)ctx.getRequestAttribute("entities");
		
		for (LUISCriteria c: match) {
			if (c.isMatch(intentDbs, entityDbs))
				return new RuleCheckResult(RuleCheckResultStatus.MATCH, "", this);
		}
		return RuleCheckResult.NO_MATCH;
	}
	
	public static void main(String[] args) {
		HibernateUtil.init();
		MessageUtil.initialize();
		//QAUtil.getInstance(4);
		String question = "上次是何時繳費的";
		QAContext ctx = new QAContext();
		ctx.setTenant(Tenant.get(4));
		ctx.setCurrentQuestion(question);
		
		new PrepareForQAMatchingRule().checkRule(ctx);
		String json = "{ \"class\": \"com.intumit.solr.robot.qadialog.LUISRule\", \"match\": [ { \"op\": \"AND\", \"intents\": [\"WHEN\", \"BEFORE\"], \"entities\": [\"PAY\"], \"class\": \"com.intumit.solr.robot.qadialog.LUISCriteria\"}, ], } ";
		
		LUISRule r = (LUISRule)new JSONDeserializer().deserialize(json);
		
		System.out.println(r);
		System.out.println(r.check(ctx));
		
		HibernateUtil.shutdown();
	}
}
