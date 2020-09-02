package com.intumit.solr.robot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.connector.web.RichMessage;
import com.intumit.solr.robot.connector.web.RichMessageServlet;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.util.WiSeUtils;

public class WebRichMessageAnswerTransformer {

	public WebRichMessageAnswerTransformer() {
		super();
	}

	protected boolean generateRichMessage(QAContext ctx, JSONArray currentMessages, boolean useHtmlNewLine) throws JSONException {
    	boolean hasRichMessage = false;
    	if (ctx.getCurrentQA() != null) {
	    	QA currentQA = ctx.getCurrentQA();
			Long kid = (Long) currentQA.getFieldValue("kid_l");
			
			String type = StringUtils.lowerCase(ctx.getQAChannelInstance().getType().name());
			MultiChannelAnswer mca = null;
			
			if (kid != null) {
				mca = MultiChannelAnswer.findNonEmptyAnswer(ctx, currentQA.getId(), ctx.getQAChannelInstance(), ctx.getUserType());
			}
			
	    	if (mca != null && StringUtils.isNotBlank(mca.getRichMessageMKey())) {
				RichMessage rm = RichMessage.getByMKey(ctx.getTenant().getId(), mca.getRichMessageMKey());
				com.intumit.solr.robot.connector.webline.RichMessage lineRm = com.intumit.solr.robot.connector.webline.RichMessage.getByMKey(ctx.getTenant().getId(), mca.getRichMessageMKey());
				
				JSONObject princessNotBr = new JSONObject(lineRm.getMsgTemplate(ctx));
				boolean raichu =  princessNotBr.getJSONObject("template").has("stretchText") == true? true: false;
				String newlineReplaceBr = "";
				
				if(raichu) {
					newlineReplaceBr = princessNotBr.getJSONObject("template").get("stretchText")+"";
				}
				
				if(rm != null && ctx.getQaChannel().equals("web")) {
					hasRichMessage = true;
					if (princessNotBr.getJSONObject("template").has("stretchText")) {
						newlineReplaceBr = newlineReplaceBr.replaceAll("\n","<br>");
						princessNotBr.getJSONObject("template").put("stretchText",newlineReplaceBr);
						currentMessages.add(0, princessNotBr);
					}else {
						currentMessages.add(0, new JSONObject(lineRm.getMsgTemplate(ctx)));
					}	
				}else if (lineRm != null && ctx.getQaChannel().equals("webline")) {
					hasRichMessage = true;
					if (princessNotBr.getJSONObject("template").has("stretchText")) {
						newlineReplaceBr = newlineReplaceBr.replaceAll("\n","<br>");
						princessNotBr.getJSONObject("template").put("stretchText",newlineReplaceBr);
						currentMessages.add(0, princessNotBr);
					}else {
						currentMessages.add(0, new JSONObject(lineRm.getMsgTemplate(ctx)));
					}					
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
		    				com.intumit.solr.robot.connector.webline.RichMessage lineRm = com.intumit.solr.robot.connector.webline.RichMessage.getByMKey(ctx.getTenant().getId(), msgCfg.optString("mkey"));
		    				if (rm != null && ctx.getQaChannel().equals("web")) {
			    				JSONObject rmj = new JSONObject(rm.getMsgTemplate(ctx));
			    				String es = currentQA != null ? currentQA.getId() : ("_ANSTYPE_" + ctx.getAnswerType());
			    				RichMessageServlet.addEventSourceAndEncryptPostback(rmj, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
		    					currentMessages.add(offset++, rmj);
		    				} else if (lineRm != null && ctx.getQaChannel().equals("webline")) {
			    				JSONObject rmj = new JSONObject(lineRm.getMsgTemplate(ctx));
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
    
	protected void processOptionMenus(QAContext ctx, JSONArray currentMessages, boolean useHtmlNewLine) throws JSONException {
	    boolean usePostbackForTemplateAction = false;
	    boolean addDisplayTextWhenUsingPostback = true;
	    
		String NL = useHtmlNewLine ? "<br>" : "\n";
		
		FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
		//int opNum = 1;
		for (String key : ctx.showedOptionMenuKeys) {
			String qaChannel = ctx.getQaChannel();
			String menuHeader = "";
			if (currentMessages.length() >= 5) break;
			
			OptionMenu menu = ctx.optionMenuMap.get(key);
			if (menu.view == MenuView.HIDDEN) continue;
			
			if (qaChannel.equals("webline")) {
				menuHeader = StringUtils.defaultString(StringUtils.trimToNull(stripBr(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far))), TemplateUtil.processTwiceAndClean(ctx,"{{F:LADDER_GUIDE_BY_WEBLINE}}",far));
			}else {
				menuHeader = StringUtils.defaultString(StringUtils.trimToNull(stripBr(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far))), TemplateUtil.processTwiceAndClean(ctx,"{{F:LADDER_GUIDE}}",far));
			}

			String menuFooter = StringUtils.defaultString(StringUtils.trimToNull(stripBr(TemplateUtil.processTwiceAndClean(ctx, menu.menuFooter, far))), "");
			List<String> preExecutedOptionNames = new ArrayList<String>();
			
			// 先把 title 們跑過 FunctionUtil.collectExecAndReplace()，不然只要有用到 inline function 幾乎一定超過長度
			for (Option option: menu.options) {
				String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
				opName = TemplateUtil.processTwiceAndClean(ctx, opName, far);
				preExecutedOptionNames.add(opName);
			}
			
			System.out.println("-----preExecutedOptionNames:-----\n" + preExecutedOptionNames + "\n-----END-----");			
	        
			JSONObject message = new JSONObject();
			message.put("type", "template");
			message.put("altText", menuHeader);
			currentMessages.add(message);
			
			JSONObject template = new JSONObject();
			
			if (qaChannel.equals("webline")) {
				template.put("type", "quickReplies");
				template.put("title", StringUtils.left(menuHeader, 100));
				template.put("text", StringUtils.defaultIfEmpty(StringUtils.left(menuFooter, 160), "　"));
				message.put("template", template);
			}else {
				template.put("type", "buttons");
				template.put("title", StringUtils.left(menuHeader, 100));
				template.put("text", StringUtils.defaultIfEmpty(StringUtils.left(menuFooter, 160), "　"));
				message.put("template", template);
			}
			
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
						action.put("label", StringUtils.left(label, 100));
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
						action.put("label", StringUtils.left(label, 100));
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
			
			System.out.println("-----actions:-----\n" + actions + "\n-----END-----");
			List<String> checkLabel = new ArrayList<String>();
			for (int i = 0;i < actions.size();i++) {
				JSONObject temp = actions.getJSONObject(i);
				String label = temp.optString("label");
				if (checkLabel.contains(label)) {
					actions.remove(i);
				} else {
					checkLabel.add(label);
				}
			}
			
			template.put("actions", actions);
		}
	}
	
	static String stripBr(String withBr) {
		String noTrailingBr = withBr.replaceAll("(\\s*<[Bb][Rr]\\s*/?>)+\\s*$", "");
		String noBr = noTrailingBr.replaceAll("^(\\s*<[Bb][Rr]\\s*/?>)+", "");
		return noBr;
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
