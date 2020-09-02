package com.intumit.solr.robot.qarule;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.NO_ANSWER_OPTION;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.util.WiSeUtils;

public class ForwardToCrmRule implements PreQAMatchRule {

	public static final String REQ_ATTR_FORWARD_OPTIONS = "offerForwardOptions";
	public static final String REQ_ATTR_CRM_FORWARD_TO = "forward";
	public static final String REQ_ATTR_FORCE_CRM_FORWARD = "forceCrmForward";
	public static final String REQ_ATTR_ASK_USER_REDIRECT_TO_CRM = "askUserRedirectToCRM";
	public static final String REQ_ATTR_HAS_MARKETING_KEYWORDS = "marketingKeywords";
	public static final String REQ_ATTR_HAS_NEGATIVE_KEYWORDS = "negativeKeywords";
	
	boolean forceChoose = false;

	public ForwardToCrmRule() {
		super();
	}

	public ForwardToCrmRule(Map<String, Object> cfg) {
		init(cfg);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("forceChoose")) {
			forceChoose = ((Boolean)configs.get("forceChoose")).booleanValue();
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		// State 1，要提供轉文字客服了，但需要根據文字客服端不同的狀態提供不同的選項
		if ((Boolean)ctx.getLastRequestAttribute(REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, Boolean.FALSE)) {
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		}
		// State 2，根據 State1 提供的選項來作對應的 Forward
		else if ((Boolean)ctx.getLastRequestAttribute("listenOffHourCmd", Boolean.FALSE)
				|| (Boolean)ctx.getLastRequestAttribute("listenWaitingCmd", Boolean.FALSE)) {
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if ((Boolean)ctx.getLastRequestAttribute(REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, Boolean.FALSE)) {
			ctx.setQuestionType(QAContext.QUESTION_TYPE.ANSWER_FORWARD);
			
			boolean explainQA = ctx.getTenant().getEnableQAExplain();
			List<Map<String, Object>> options = (List<Map<String, Object>>)ctx.getLastRequestAttribute(REQ_ATTR_FORWARD_OPTIONS);

			if (options == null) {
				if (QAUtil.isConfirmWithYes(ctx.getCurrentQuestion())) {
					CrmRedirectInfo rInfo = redirectToCRM(ctx);
					if (explainQA) ctx.appendExplain("User confirmed to redirect to CRM, CrmRedirectInfo", rInfo);

					if (!rInfo.forwardDirectly) {
						ctx.setRequestAttribute("listenOffHourCmd", !rInfo.isDepartmentOnline);
						ctx.setRequestAttribute("listenWaitingCmd", rInfo.isDepartmentOnline);
					}
					else {
						ctx.setResponseAttribute(REQ_ATTR_CRM_FORWARD_TO, Forward.PERSONNEL.name());
					}
					String output = rInfo.output;
					ctx.setAnswerText(output);
					ctx.setAnswerType(QAContext.ANSWER_TYPE.FORWARD);
					ctx.setForwardDep(rInfo.forwardDep);
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else if (QAUtil.isConfirmWithNo(ctx.getCurrentQuestion())) {
					String output = "";
					output = "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}";
					ctx.setAnswerText(output);
					ctx.setAnswerType(QAContext.ANSWER_TYPE.STAY_WITH_ROBOT);
					ctx.setNoResultCount(0);
					if (explainQA) ctx.appendExplain("User doesn't want to redirect to CRM, CrmRedirectInfo", output);
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			else {
				Integer userInputNumber = null;
				
				if (StringUtils.isNumeric(ctx.getCurrentQuestion())) {
					try {
						userInputNumber = new Integer(ctx.getCurrentQuestion());
					}
					catch (Exception ignore) {}
				}
				
				if (userInputNumber != null && userInputNumber > 0 && userInputNumber <= options.size()) {
					Map<String, Object> option = options.get(userInputNumber - 1);
					NO_ANSWER_OPTION nao = (NO_ANSWER_OPTION) option.get("NO_ANSWER_OPTION");
					if (nao == NO_ANSWER_OPTION.CONTINUE) {
						if (option.containsKey("question")) {
							ctx.setNoResultCount(0);
							String q = (String) option.get("question");
							ctx.setCurrentQuestion(q);
							if (explainQA) ctx.appendExplain("User doesn't want to redirect to CRM, and predefined question", q);
							return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
						}
						else {
							String output = "";
							output = "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}";
							ctx.setAnswerText(output);
							ctx.setAnswerType(QAContext.ANSWER_TYPE.STAY_WITH_ROBOT);
							ctx.setNoResultCount(0);
							if (explainQA) ctx.appendExplain("User doesn't want to redirect to CRM, CrmRedirectInfo", output);
							return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
						}
					}
					else if (nao == NO_ANSWER_OPTION.WEB_CHAT) {
						CrmRedirectInfo rInfo = redirectToCRM(ctx);
						if (explainQA) ctx.appendExplain("User confirmed to redirect to CRM, CrmRedirectInfo", rInfo);

						if (!rInfo.forwardDirectly) {
							ctx.setRequestAttribute("listenOffHourCmd", !rInfo.isDepartmentOnline);
							ctx.setRequestAttribute("listenWaitingCmd", rInfo.isDepartmentOnline);
						}
						else {
							ctx.setResponseAttribute(REQ_ATTR_CRM_FORWARD_TO, Forward.PERSONNEL.name());
						}
						String output = rInfo.output;
						ctx.setAnswerText(output);
						ctx.setAnswerType(QAContext.ANSWER_TYPE.FORWARD);
						ctx.setForwardDep(rInfo.forwardDep);
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					else if (nao == NO_ANSWER_OPTION.MESSAGE_BOARD) {
						String output = "{{F:CHOOSED_FORWARD_OPTION_MESSAGE_BOARD}}";
						ctx.setResponseAttribute(REQ_ATTR_CRM_FORWARD_TO, Forward.MESSAGE_BOARD.name());
						ctx.setAnswerText(output);
						ctx.setAnswerType(QAContext.ANSWER_TYPE.FORWARD);
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
				}
				
				if (forceChoose) {
					ctx.setAnswerText(ctx.getLastAnswerText());
					ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
					ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORWARD_OPTIONS, options);
					ctx.setAnswerType(QAContext.ANSWER_TYPE.PREPARE_FORWARD);
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				
				return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
			}
		}
		
		// 轉真人文字客服後的選項（忙線或者下班時間）
		Cmd cmd = null;
		
		if ((Boolean)ctx.getLastRequestAttribute("listenOffHourCmd", Boolean.FALSE)) {
			cmd = getCmdFromInput(OffHourCmd.class, ctx.getCurrentQuestion());
		}
		else if ((Boolean)ctx.getLastRequestAttribute("listenWaitingCmd", Boolean.FALSE)) {
			cmd = getCmdFromInput(WaitingCmd.class, ctx.getCurrentQuestion());
		}

		if (cmd != null) {
			if (ctx.getTenant().getEnableQAExplain()) ctx.appendExplain("Got Cmd", cmd);

			String cmdText = cmd.getText();
			ctx.setCurrentQuestion(cmdText);
			ctx.setResponseAttribute(REQ_ATTR_CRM_FORWARD_TO, cmd.getForward().name());

			ctx.setAnswerText(cmdText);
			ctx.setQuestionType(QAContext.QUESTION_TYPE.ANSWER_FORWARD_OPTION);
			ctx.setAnswerType(QAContext.ANSWER_TYPE.FORWARD);
			ctx.setNoResultCount(0);
			
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		else if (forceChoose) {
			ctx.setAnswerText(ctx.getLastAnswerText());
			ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
			ctx.setRequestAttribute("listenOffHourCmd", (Boolean)ctx.getLastRequestAttribute("listenOffHourCmd", Boolean.FALSE));
			ctx.setRequestAttribute("listenWaitingCmd", (Boolean)ctx.getLastRequestAttribute("listenWaitingCmd", Boolean.FALSE));
			ctx.setAnswerType(QAContext.ANSWER_TYPE.PREPARE_FORWARD);
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

	public static class CrmRedirectInfo {
		public boolean forwardDirectly;
		public String output;
		public boolean isDepartmentOnline;
		public int pendingCount;
		public int forwardDep = 0;
	}
	
	public static CrmRedirectInfo redirectToCRM(QAContext ctx) {
		int depId = 0;
		String baseUrl = ctx.getTenant().getCrmApiBaseUrl();
		CrmRedirectInfo info = new CrmRedirectInfo();
		boolean specificEservice = (ctx.getEservice() != null && !ctx.getEservice().equals("") && ctx.getTenant().getSpecificEservice().contains(ctx.getEservice()));
		//eservice為某特定來源，以首次進線的知識主題為主
		if(specificEservice){
			try {
				String data = WiSeUtils.getDataFromUrlIgnoringStatusCode(baseUrl
						+ "index.php/site_admin/chatapi/qacategorydepartment/"
						+ URLEncoder.encode(ctx.getRequestAttribute("qaCategorySelect").toString(),"utf-8"));
				depId = new JSONObject(data).getInt("dep_id");
				info.forwardDep = depId;
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		//eservice非特定來源，以上一次的QA知識主題為主
		else if(!specificEservice && ctx.getLastQaCategory() != null){
			try {
				String data = WiSeUtils.getDataFromUrlIgnoringStatusCode(baseUrl
						+ "index.php/site_admin/chatapi/qacategorydepartment/"
						+ URLEncoder.encode(ctx.getLastQaCategory(),"utf-8"));
				depId = new JSONObject(data).getInt("dep_id");
				info.forwardDep = depId;
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		//直接擷取預設部門
		else{
			depId = ctx.getTenant().getDefaultTextCrmDepartmentId();
		}
		System.out.println("depId:" + depId + ",forwardDep:" + info.forwardDep);
		String output = "";
		info.isDepartmentOnline = false;
		info.forwardDirectly = false;

		try {
			String data = WiSeUtils.getDataFromUrlIgnoringStatusCode(baseUrl
					+ "index.php/site_admin/chatapi/isonlinedepartment/"
					+ depId);
			info.isDepartmentOnline = new JSONObject(data)
					.getBoolean("isonline");
		} catch (Exception e) {
			System.out.println(e);
		}
		String NL = ctx.isClientSupportHtml() ? "<br/>" : "\\n";
		

		List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
		
		if (info.isDepartmentOnline) {
			info.pendingCount = 0;
			try {
				String data = WiSeUtils
						.getDataFromUrlIgnoringStatusCode(baseUrl
								+ "index.php/site_admin/chatapi/pending/"+depId);
				info.pendingCount = Integer.parseInt(new JSONObject(data).get(
						"count").toString());
				System.out.println("pendingCount:"+info.pendingCount);
			} catch (Throwable t) {
			}
			if (info.pendingCount > 0) {
				options = new ArrayList<Map<String, Object>>();
				Map<String, Object> opt1 = new HashMap<String, Object>();
				opt1.put("title", "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}");
				opt1.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.CONTINUE);
				Map<String, Object> opt2 = new HashMap<String, Object>();
				opt2.put("title", "{{F:FORWARD_OPTION_KEEP_WAITING_WEBCHAT}}");
				opt2.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.WEB_CHAT);
				
				options.add(opt1);
				options.add(opt2);

				output += "{{F:FORWARD_HEADER_WEBCHAT_BUSY}}";
			} else {
				output = "{{F:FORWARD_OPTION_KEEP_WAITING_WEBCHAT}}";
				info.forwardDirectly = true;
			}
		} else {
			options = new ArrayList<Map<String, Object>>();
			Map<String, Object> opt1 = new HashMap<String, Object>();
			opt1.put("title", "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}");
			opt1.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.CONTINUE);
//			Map<String, Object> opt2 = new HashMap<String, Object>();
//			opt2.put("title", "{{F:FORWARD_OPTION_MESSAGE_BOARD}}");
//			opt2.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.MESSAGE_BOARD);
			
			options.add(opt1);
//			options.add(opt2);
			
			output += "{{F:FORWARD_HEADER_WEBCHAT_OFFLINE}}";
		}
		

		int counter = 1;
		
		for (Map<String, Object> option: options) {
			String title = (String)option.get("title");
			NO_ANSWER_OPTION nao = (NO_ANSWER_OPTION)option.get("NO_ANSWER_OPTION");
			
			int no = counter; counter++;
			output +=  NL + "{{F:ENTER}}" + (no) + ". " + QAUtil.toClickableHtml("" + no, title, ctx.isClientSupportHtml());
		}
		//	+ NL + "{{F:ENTER}}1." + QAUtil.toClickableHtml("1", "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}", ctx.isClientSupportHtml())
		//	+ NL + "{{F:ENTER}}2." + QAUtil.toClickableHtml("2", "{{F:FORWARD_OPTION_MESSAGE_BOARD}}", ctx.isClientSupportHtml())
		output += NL + "{{F:FORWARD_FOOTER_GENERAL}}";
		
		if (options.size() > 0)
			ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORWARD_OPTIONS, options);
		
		info.output = output;
		return info;
	}

	public interface Cmd {
		String getText();

		Forward getForward();
	}

	public static enum Status {
		ENTERING, ACTIVE, OFFLINE, CLOSED, ;
	}

	public enum Forward {

		PERSONNEL, // 轉入文字客服
		MESSAGE, // 轉入文字客服留言功能
		SURVEY, // 轉入文字客服問卷(滿意度調查)功能
		CONTINUE, // 繼續跟機器人對話
		NETWORK_BANK, //
		MESSAGE_BOARD // 轉入外部留言板（非文字客服）

	}

	public enum OffHourCmd implements Cmd {
		
		_1(Forward.CONTINUE, "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}");

		final String text;
		final Forward forward;

		OffHourCmd(Forward forward, String text) {
			this.forward = forward;
			this.text = text;
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public Forward getForward() {
			return forward;
		}

	}

	public enum WaitingCmd implements Cmd {

		_1(Forward.CONTINUE, "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}"),
		_2(Forward.PERSONNEL, "{{F:FORWARD_OPTION_KEEP_WAITING_WEBCHAT}}");

		public final String text;
		public final Forward forward;

		WaitingCmd(Forward forward, String text) {
			this.forward = forward;
			this.text = text;
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public Forward getForward() {
			return forward;
		}

	}

	static <T extends Enum<T> & Cmd> String getCmdInput(T cmd) {
		return StringUtils.substringAfter(cmd.name(), "_");
	}

	public static <T extends Enum<T> & Cmd> Cmd getCmdFromInput(Class<T> type, String input) {
		T cmd = null;
		for (T c : EnumSet.allOf(type)) {
			if (getCmdInput(c).equals(input)) {
				cmd = c;
				break;
			}
		}
		return cmd;
	}

}
