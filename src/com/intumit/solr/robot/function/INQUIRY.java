package com.intumit.solr.robot.function;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.EventType;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.qarule.QAMatchRuleController;

public class INQUIRY extends FunctionBase {

	public INQUIRY(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		String str = null;
		if (in != null) {
			str = in.getInput();
		} else {
			str = data;
		}
		
		QAContext cloned = ctx.cloneWithoutStateStackAndExplain();
		// 必須將 EventType 設定為 message，否則遇到 postback 這種 Event 無論怎麼 INQUIRY 都無效
		try {
			cloned.setEventType(EventType.matchBestEventType(cloned.getTenant().getId(), null, "message"));
		} catch (Exception ex) { 
			// Move on even got exception
			ex.printStackTrace(); 
		}

		cloned.setCurrentQuestion(str);
		cloned.setOriginalQuestion(str);
		cloned.setAnswerText("");
		QAMatchRuleController c = QAMatchRuleController.getInstance("tenant:" + cloned.getTenant().getId());
		c.check(cloned);
		String answer = "";
		if (cloned.getAnswerText() != null) {
			JSONObject resp = cloned.genResponseJSON();
			answer = resp.optString("output");
		}
		
		return answer;
	}
}
