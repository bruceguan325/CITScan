package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.wink.json4j.JSONArray;

import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.util.WiSeUtils;

public class RobotAnswerTransformer implements AnswerTransformer {

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		
		if (answerText == null) {
			answerText = new StringBuilder();
		}

		if (ctx.getShowedOptionMenuSize() > 0) {
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";

			int opNum = 1;
			
			ArrayList<Map<String, Object>> optionDataList = new ArrayList<Map<String, Object>>();

			for (String key : ctx.showedOptionMenuKeys) {
				OptionMenu menu = ctx.optionMenuMap.get(key);
				if (ch.getAppendOptionToOutput()) {
					if (answerText.length() > 0) {
						answerText.append(NL);
					}
					answerText.append(menu.menuHeader);
				}

				if (menu.view != MenuView.HIDDEN) {
	    				for (Option option : menu.options) {
	    					int currentOpNum = opNum++;
	    					
	    					if (ch.getReturnOptionInJson()) {
	        					Map<String, Object> optionData = new HashMap<String, Object>();
	        					if (option.value != null && option.value.question != null) // 有 question 用 question
	        						optionData.put( "option", option.value.question );
	        					else 
	        						optionData.put( "option", ""+currentOpNum ); // 沒 question 用編號
	        					optionData.put( "title", option.getName() );
            					optionData.put( "eventSource", menu.getCreateFrom() );
            					optionData.put( "eventSourceType", QAContext.EVENT_SOURCE_TYPE_BUTTON );
	        					
	        					optionDataList.add(optionData);
	        					continue;
	    					}

        					if (ch.getAppendOptionToOutput()) {
		    					if (ctx.isClientSupportHtml()) {
		    						answerText
		    								.append(menu.indentOrSplitString
		    										+ "{{F:ENTER}}<span class='question-cmd'>"
		    										+ (currentOpNum)
		    										+ "</span>.<span class='question-clickable other-questions-li'"
            										+ " data-es='" + menu.getCreateFrom() + "'"
            										+ " data-est='" + QAContext.EVENT_SOURCE_TYPE_BUTTON + "'"
		    										+ " data-opnum='" + option.getName() + "'>"
		    										+ option.getName() + "</span>");
		    
		    						if (ctx.getTenant().getEnableDebug()) {
		    							Long kid = option.getRefToKid();
		    
		    							if (kid != null) {
		    								String href = QAUtil.getContextPath()
		    										+ "/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-"
		    										+ kid;
		    								String a = "&nbsp;&nbsp;<a href='" + href
		    										+ "' target='"
		    										+ QAUtil.ANSWER_LINK_TARGET
		    										+ "'>(<bean:message key='num'/>" + kid
		    										+ ")</a>";
		    								answerText.append(a);
		    							}
		    						}
		    
		    						answerText.append(NL);
		    					} else {
		    						answerText.append(NL + menu.indentOrSplitString
		    								+ "{{F:ENTER}}" + (currentOpNum) + "."
		    								+ option.getName());
		    					}
        					}
       				}
				}
				answerText.append(menu.menuFooter);
				answerText = QAUtil.replaceDoubleWraps(answerText.toString(), NL+NL, NL);
			}
			
			if (ch.getReturnOptionInJson() && !optionDataList.isEmpty()) {
				try {
					answerText = QAUtil.replaceDoubleWraps(answerText.toString(), NL+NL, NL);
					if (ctx.getResponseAttribute("options") != null) {
						JSONArray jarray = (JSONArray) ctx.getResponseAttribute("options");
						jarray.addAll(optionDataList);
						ctx.setResponseAttribute("options", jarray);
					} else {
						ctx.setResponseAttribute("options", new JSONArray(optionDataList));
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
		
		FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);

		String output = answerText.toString();
		output = FunctionUtil.collectExecAndReplace(output, ctx);
		output = TemplateUtil.processTwice(ctx, output, far);
		FormalAnswerStickerReplacer farSticker = QAUtil.getInstance(ctx.getTenant()) .getFormalAnswerStickerReplacer(ctx);
		output = TemplateUtil.process(ctx, output, farSticker);
		output = TemplateUtil.process(ctx, output, QAUtil.getCleaner());
		
		output = ctx.isClientSupportHtml() ? WiSeUtils.nl2br(output) : output;
		ctx.setAnswerText(output);
		
		if (ctx.getCurrentQA() != null) {
			QA currentQA = ctx.getCurrentQA();
			MultiChannelAnswer mca = MultiChannelAnswer.findNonEmptyAnswer(ctx, (String) currentQA.getFieldValue("id"),
					ctx.getQAChannelInstance(), ctx.getUserType());
			boolean formalAnswer = false;
			if (mca != null && mca.getAnswerType() != null
					&& mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)) {
				if (mca.getAnswerVoice() != null && !mca.getAnswerVoice().isEmpty()) {
					ctx.setResponseAttribute("outputVoice", TemplateUtil.processTwice(ctx, mca.getAnswerVoice(), far));
				}
				else {
					formalAnswer = true;
				}
				ctx.setResponseAttribute("outputMood", mca.getAnswerMood());
			}
			else if (mca != null && mca.getAnswerType() != null
					&& mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_GENERAL)) {
				formalAnswer = true;
				ctx.setResponseAttribute("outputMood", MultiChannelAnswer.ROBOT_MOOD_CURIOUS);
			}
		}
		else if (ctx.hasAnswerText() && ANSWER_TYPE.CASUAL == ctx.getAnswerType()) {
			ctx.setResponseAttribute("outputVoice", TemplateUtil.processTwice(ctx, ctx.getAnswerText().toString(), far));
		}
		else if (ctx.hasAnswerText() && (ANSWER_TYPE.NO_ANSWER == ctx.getAnswerType() || ANSWER_TYPE.PROFESSIONAL_PLUGIN == ctx.getAnswerType())) {
			ctx.setResponseAttribute("outputVoice", TemplateUtil.processTwice(ctx, ctx.getAnswerText().toString(), far));
		}
		
		if (ctx.getResponseAttribute("outputVoice") == null) {
			ctx.setResponseAttribute("outputVoice", TemplateUtil.processTwice(ctx, "{{F:DEFAULT_ROBOT_VOICE_ANSWER}}", far));
		}
		if (ctx.getResponseAttribute("outputMood") == null) {
			ctx.setResponseAttribute("outputMood", MultiChannelAnswer.ROBOT_MOOD_CURIOUS);
		}
		
		return ctx;
	}

}
