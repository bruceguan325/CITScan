package com.intumit.solr.robot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.connector.web.RichMessage;
import com.intumit.solr.robot.connector.web.RichMessageServlet;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.util.WiSeUtils;

public class PlainTextAnswerTransformer implements AnswerTransformer {

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
		JSONArray messages = new JSONArray();
		int opNum = 1;
		ArrayList<Map<String, Object>> optionDataList = new ArrayList<Map<String, Object>>();
		
		if (answerText == null)
			answerText = new StringBuilder();

		try {
			if (!ch.getSupportMultiRichMessages()) {
				if (ctx.getShowedOptionMenuSize() > 0) {
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
				}
			}
			else {
				// 支援多圖文，除了原始內容之外，選單的東西全部都轉成圖文
				generateRichMessage(ctx, messages, ch.getUseHtmlNewline());
				processOptionMenus(ctx, messages, ch.getUseHtmlNewline());
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
		catch (JSONException e) {
			e.printStackTrace();
		}

		String output = answerText.toString();
		output = FunctionUtil.collectExecAndReplace(output, ctx);

		FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
		output = TemplateUtil.processTwice(ctx, output, far);

		FormalAnswerStickerReplacer farSticker = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerStickerReplacer(ctx);
		output = TemplateUtil.process(ctx, output, farSticker);
		output = TemplateUtil.process(ctx, output, QAUtil.getCleaner());
		
		output = ctx.isClientSupportHtml() ? WiSeUtils.nl2br(output) : output;
		ctx.setAnswerText(output);
		
		try {
			if (ch.getSupportMultiRichMessages()) {
				
				// 這裡直接把 answerText 塞在 messages 第一個，因此前端呈現的時候，應該在 webRM 存在時，忽略 output
				if (output.length() > 0) {
					JSONObject msgObj = new JSONObject();
					try {
						msgObj.put("type", "text");
						msgObj.put("text", output);
						
						messages.add(0, msgObj);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			if (messages.length() > 0) {
				ctx.response.put("webRM", new JSONObject().put("messages", messages));
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return ctx;
	}


    private boolean generateRichMessage(QAContext ctx, JSONArray currentMessages, boolean useHtmlNewLine) throws JSONException {
    	boolean hasRichMessage = false;
    	if (ctx.getCurrentQA() != null) {
	    	QA currentQA = ctx.getCurrentQA();
			Long kid = (Long) currentQA.getFieldValue("kid_l");
			
			String type = StringUtils.lowerCase(ctx.getQAChannelInstance().getType().name());
			String typeSuffix = "_" + type + "type";
			MultiChannelAnswer mca = null;
	
			if (kid != null) {
				mca = MultiChannelAnswer.findNonEmptyAnswer(ctx, currentQA.getId(), ctx.getQAChannelInstance(), ctx.getUserType());
			}
			
	    	if (mca != null && StringUtils.isNotBlank(mca.getRichMessageMKey())) {
				RichMessage rm = RichMessage.getByMKey(ctx.getTenant().getId(), mca.getRichMessageMKey());
				if(rm != null) {
					hasRichMessage = true;
					currentMessages.add(0, new JSONObject(rm.getMsgTemplate(ctx)));
				}
			}

	    	if (mca != null && StringUtils.isNotBlank(mca.getChannelTypeConfig())) {
	    		JSONObject msgsCfg = new JSONObject(mca.getChannelTypeConfig());
	    		
	    		if (msgsCfg.has("messages")) {
	    			JSONArray msgsArr = msgsCfg.getJSONArray("messages");
	    			int offset = 0;
	    			
	    			for (int i=0; i < msgsArr.length(); i++) {
	    				JSONObject msgCfg = msgsArr.getJSONObject(i);
	    				String msgType = msgCfg.optString("type", "");
	    				
	    				if (msgType.equals("")) {
	    					continue;
	    				}
	    				else if (msgType.equals(type + "_answer_text")) {
	    					String answer =  msgCfg.optString("text");
	    					answer = WiSeUtils.br2nl(answer).replaceAll("<[^>]+>", " ");
	    					answer = FunctionUtil.collectExecAndReplace(answer, ctx);

	    					TagLGQReplacer tagLGQ = new TagLGQReplacer(ctx);
	    					answer = TemplateUtil.process(ctx, answer, tagLGQ);
	    					
	    					FormalAnswerReplacer far = ctx.getQAUtil().getFormalAnswerReplacer(ctx);
	    					answer = TemplateUtil.processTwice(ctx, answer, far);
	    					answer = TemplateUtil.process(ctx, answer, tagLGQ);
	    					currentMessages.add(offset++, new JSONObject().put("type", "text").put("text", answer));
	    				}
	    				else {
		    				RichMessage rm = RichMessage.getByMKey(ctx.getTenant().getId(), msgCfg.optString("mkey"));
		    				
		    				if (rm != null) {
			    				JSONObject rmj = new JSONObject(rm.getMsgTemplate(ctx));
			    				String es = currentQA != null ? currentQA.getId() : ("_ANSTYPE_" + ctx.getAnswerType());
			    				RichMessageServlet.addEventSourceAndEncryptPostback(rmj, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
		    					currentMessages.add(offset++, rmj);
		    				}
	    				}
	    			}
	    			
	    			if (offset > 0) {
						hasRichMessage = true;
	    			}
	    		}
			}
    	}
    	return hasRichMessage;
	}
    
	private void processOptionMenus(QAContext ctx, JSONArray currentMessages, boolean useHtmlNewLine) throws JSONException {
	    boolean usePostbackForTemplateAction = false;
	    boolean addDisplayTextWhenUsingPostback = true;
	    
		String NL = useHtmlNewLine ? "<br>" : "\n";
		
		FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
		int opNum = 1;
		for (String key : ctx.showedOptionMenuKeys) {
			if (currentMessages.length() >= 5) break;
			
			OptionMenu menu = ctx.optionMenuMap.get(key);
			if (menu.view == MenuView.HIDDEN) continue;
			String menuHeader = StringUtils.defaultString(StringUtils.trimToNull(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far)), ctx.getMessage("global.pleaseSelect"));
			String menuFooter = StringUtils.defaultString(StringUtils.trimToNull(TemplateUtil.processTwiceAndClean(ctx, menu.menuFooter, far)), "");
			List<String> preExecutedOptionNames = new ArrayList<String>();
			
			// 先把 title 們跑過 FunctionUtil.collectExecAndReplace()，不然只要有用到 inline function 幾乎一定超過長度
			for (Option option: menu.options) {
				String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
				opName = TemplateUtil.processTwiceAndClean(ctx, opName, far);
				preExecutedOptionNames.add(opName);
			}

			JSONObject message = new JSONObject();
			message.put("type", "template");
			message.put("altText", menuHeader);
			currentMessages.add(message);
			
			JSONObject template = new JSONObject();

			template.put("type", "buttons");
			template.put("title", StringUtils.left(menuHeader, 40));
			template.put("text", StringUtils.defaultIfEmpty(StringUtils.left(menuFooter, 160), "　"));
			message.put("template", template);
			
			JSONArray actions = new JSONArray();
			
			for (int i=0; i <  menu.options.size(); i++) {
				Option option = menu.options.get(i);
				//int currentOpNum = opNum++;
				String opName = preExecutedOptionNames.get(i);
				String label = opName;
				String text = opName;
				
				if (StringUtils.trimToNull(text) == null) continue;
				
				try {
					JSONObject action = new JSONObject();
					ParsedOption po = option.getValue();
					
					if (usePostbackForTemplateAction) {
						action.put("type", "postback");
						action.put("label", StringUtils.left(label, 20));
						if (addDisplayTextWhenUsingPostback) {
							action.put("displayText", label);
						}
						QA currentQA = ctx.getCurrentQA();
						
						if (po != null && po.id != null) {
							String fallback = "";
							String pipeId = menu.getKey() + "::" + po.id;
							
							if (currentQA != null) {
								fallback = "&fallback=" + currentQA.getKid();
							}
							action.put("data", "action=_option&option=" + pipeId + fallback);
						}
						else {
							action.put("data", "action=_message&message=" + URLEncoder.encode(text, "UTF-8"));
						}
					}
					else {
						action.put("type", "message");
						action.put("label", StringUtils.left(label, 20));
						action.put("text", label);
					}
					
					actions.add(action);
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

	    	QA currentQA = ctx.getCurrentQA();
			String es = currentQA != null ? currentQA.getId() : ("_ANSTYPE_" + ctx.getAnswerType());
			RichMessageServlet.addEventSourceAndEncryptPostback(actions, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
			
			template.put("actions", actions);
		}
	}
	
	/**
	 * @author herb
	 */
	public class TagLGQReplacer implements Replacer {

		public TagLGQReplacer(QAContext ctx) {
		}

		@Override
		public String call(String name, String val) {
			return call(null, name, val);
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			if (ctx != null) {
    			if (StringUtils.equals(name, "G")) {
    				String stickerKey = StringUtils.substringBefore(val, ":");
    				String stickerAttr = StringUtils.substringAfter(val, ":");
    				
    				RobotFormalAnswersSticker sticker = RobotFormalAnswersSticker.giveMeAnswer(ctx.getTenant().getId(), stickerKey);
    				
    				if (sticker != null) {
    					int h = sticker.getHeight();
    					int w = sticker.getWidth();
    					boolean enclosedByCenterTag = false;
    					
    					if (StringUtils.isNotEmpty(stickerAttr)) {
    						try {
    							Map<String, List<String>> attrs = WiSeUtils.splitUrlQueryParams(stickerAttr);
    							if (attrs.containsKey("width")) {
    								w = Integer.parseInt(attrs.get("width").get(0));
    							}
    							if (attrs.containsKey("height")) {
    								h = Integer.parseInt(attrs.get("height").get(0));
    							}
    							if (attrs.containsKey("enclosedByCenterTag")) {
    								enclosedByCenterTag = Boolean.parseBoolean(attrs.get("enclosedByCenterTag").get(0));
    							}
    						}
    						catch (Exception e) {
    							e.printStackTrace();
    						}
    					}
    					
    					return " " + QAUtil.getFullUrlBase() + "/img/sticker/" + sticker.getAnswers() + " ";
    				}
    				return " " + QAUtil.getFullUrlBase() +"/img/sticker/" + stickerKey + " ";
    			}
    			else if (StringUtils.equals(name, "L")) {
    				//String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");
    				String link = StringUtils.substringAfter(val, ":");

    				return " " + link + " ";
    			}
    			else if (StringUtils.equals(name, "Q")) {
					String qName = StringUtils.substringBefore(val, ":");
//					String q = StringUtils.substringAfter(val, ":");
					return qName;
				}
			}
			return null;
	
		}
	}
}
