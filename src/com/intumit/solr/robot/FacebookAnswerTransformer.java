package com.intumit.solr.robot;

import java.util.ArrayList;
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
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.util.WiSeUtils;

public class FacebookAnswerTransformer implements AnswerTransformer {
	
	public static final int BUTTONS_DIALOG_LENGTH = 20;
	public static final int TEMPLATE_BUTTON_LIMIT = 3;

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		
		if (answerText == null)
			answerText = new StringBuilder();
		
		try {
			JSONObject botCfg = ctx.getTenant().getLineBotConfigJson() != null ? new JSONObject(ctx.getTenant().getLineBotConfigJson()) : new JSONObject("{}");
			JSONObject fbBotCfg = botCfg.optJSONObject("messenger", new JSONObject());
			boolean enableAnswer2QucikReply = fbBotCfg.optJSONObject("answerToQuickReply", new JSONObject()).optBoolean("enable", false);
			boolean enableMenu2QucikReply = fbBotCfg.optJSONObject("menuToQuickReply", new JSONObject()).optBoolean("enable", false);
			
			JSONArray messages = (JSONArray)ctx.getResponseAttribute("messages");
			if (messages == null) messages = new JSONArray();
			
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			String output = answerText.toString();
			
			int questionCount = 0;
			if (ctx.getJSONArrayQ() != null) {
				questionCount = ctx.getJSONArrayQ().size();
			}
			
			int linkCount = 0;
			if (ctx.getJSONArrayLink() != null) {
				linkCount = ctx.getJSONArrayLink().size();
			}
			
			ArrayList<JSONObject> allOptions = new ArrayList<JSONObject>();
			allOptions = getAnswerOptions(allOptions, ctx.getJSONArrayQ());
			allOptions = getAnswerOptions(allOptions, ctx.getJSONArrayLink());
			
			// 若答案中有{{Q:TITLE:TEXT}} or {{L:TITLE:LINK}}
			if (allOptions.size() > 0) {
				ArrayList<Map<String, Object>> imgs = ctx.getJSONArrayIMG();
				int imgCount = imgs != null ? imgs.size() : 0;
				String menuHeader = ctx.getMessage("global.pleaseSelectOrEnterNumber.toGetDetailInfo"); //"請輸入數字或點擊選項，了解詳細內容";
				
				// 如果答案中有 制式貼圖 或 {{L:TITLE:LINK}} 或是 不啟用QuickReply
				if (linkCount > 0 || imgCount > 0 || !enableAnswer2QucikReply) {
					// 選項少於4用按鈕範本
					if (imgCount > 0) {
						messages.add(buildGenericTemplate(imgs, allOptions, menuHeader));
					}
					else if (questionCount + linkCount < 4) {
						messages.add(buildButtonTemplate(allOptions, menuHeader));
					} 
					// 一般範本
					else {
						messages.add(buildGenericTemplate(imgs, allOptions, menuHeader));
					}
				} else {
					// 用Quick Reply(因為link不支援) 
					if (questionCount <= 11) {
						messages.add(buildQuickReply(allOptions, menuHeader));
					} 
					// 一般範本
					else {
						messages.add(buildGenericTemplate(null, allOptions, menuHeader));
					}
				}
			}

			if (ctx.getCurrentQA() != null) {
				QA currentQA = ctx.getCurrentQA();
				Long kid = (Long) currentQA.getFieldValue("kid_l");
				MultiChannelAnswer mca = null;

				if (kid != null) {
					mca = MultiChannelAnswer.findNonEmptyAnswer(ctx, currentQA.getId(), ctx.getQAChannelInstance(), ctx.getUserType());
				}

				JSONObject extraParams = currentQA.getExtraParams();
				if (extraParams != null) {
					try {
						// 除非我們設計出共通的規格，否則以後應該禁止直接用 messages
						if (extraParams.has("messages")) {
							JSONArray arr = extraParams.optJSONArray("messages", new JSONArray());
							for (int i=0; i < arr.length(); i++) {
								messages.add(arr.get(i));
							}
						}
						// 應該用 extraParams.messenger.messages[0..N] 這樣的格式
						if (extraParams.has(ctx.getQaChannel())) {
							JSONArray arr = extraParams.optJSONObject(ctx.getQaChannel(), new JSONObject()).optJSONArray("messages", new JSONArray());
							
							for (int i=0; i < arr.length(); i++) {
								messages.add(arr.get(i));
							}
						}
					}
					catch (Exception ignoreIt) {}
				}

				// 或者直接將 extraParameters 放在對應的 multiChannelAnswer.extraParams 當中，就不用上面那樣的格式
				if (mca != null && StringUtils.isNotEmpty(mca.getExtraParameters())) {
					try {
						JSONObject ep = new JSONObject(mca.getExtraParameters());

						if (ep.has("messages")) {
							JSONArray arr = ep.optJSONArray("messages", new JSONArray());
							for (int i=0; i < arr.length(); i++) {
								messages.add(arr.get(i));
							}
						}
					}
					catch (Exception ignoreIt) {}
				}
			}

			output = WiSeUtils.br2nl(output);
			output = FunctionUtil.collectExecAndReplace(output, ctx);
			
			FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
			output = TemplateUtil.processTwice(ctx, output, far);

			FormalAnswerStickerReplacer farSticker = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerStickerReplacer(ctx);
			output = TemplateUtil.process(ctx, output, farSticker);
			
			ctx.setAnswerText(output);

			int opNum = 1;
			for (String key : ctx.showedOptionMenuKeys) {
				OptionMenu menu = ctx.optionMenuMap.get(key);
				if (menu.view == MenuView.HIDDEN) continue;
				String menuHeader = StringUtils.defaultString(StringUtils.trimToNull(
					TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far)), ctx.getMessage("global.pleaseSelect"));
				
				// 一般選單(階層附加參數)
				if (isGoodForCarouselMode(menu.options) && menu.optionsSize() <= 10) {
					messages.put(buildCarouselMenu(ctx, far, menu, opNum));
				}
				// 一般選單(階層&推薦)
				else if (!enableMenu2QucikReply) {
					messages.put(buildGenericTemplate(ctx, far, menu, menuHeader));
				}
				// 快速回覆(階層&推薦)
				else if (menu.optionsSize() <= 11 && enableMenu2QucikReply) { // 沒有超過11個選項的話可以用qucik reply
					messages.put(buildQuickReply(ctx, far, menu));
				}
				else {
					StringBuffer buf = new StringBuffer();
					buf.append(menuHeader);
					buf.append(NL);
					
					for (Option option : menu.options) {
						int currentOpNum = opNum++;
						buf.append(NL + menu.indentOrSplitString + TemplateUtil.processTwiceAndClean(ctx, "{{F:ENTER}}" + (currentOpNum) + "." + option.getName(), far));
					}
					
					JSONObject message = new JSONObject();
					message.put("type", "text");
					message.put("text", buf.toString());
					messages.add(message);
				}
			}
			
			System.out.println("==== Append FB addtional messages ====");
			try {
				System.out.println(new org.json.JSONArray(messages.toString(2)).toString(2));
			}
			catch (org.json.JSONException ignore) {
				ignore.printStackTrace();
			}
			
			// conn-messenger.jsp 會處理這個資料
			ctx.response.put("messages", messages);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return ctx;
	}
	
	private boolean isGoodForCarouselMode(List<Option> options) {
		Boolean tCheck = null;
		int naCheck = -1;
		for (Option option: options) {
			ParsedOption po = option.getValue();
			boolean hasImgUrl = false;
			int numberOfActions = 1; // 如果 extraParams.actions 是空的，預設就是一個選項，就是這個 option 自己
			
			if (po != null && po.extraParams != null) {
				if (po.extraParams.has("image_url")) {
					hasImgUrl = true;
				}
				else  {
					hasImgUrl = false;
				}
				
				if (po.extraParams.has("buttons")) {
					try {
						numberOfActions = po.extraParams.getJSONArray("buttons").length();
					}
					catch (Exception e) {e.printStackTrace();}
				}
			}
			else {
				hasImgUrl = false;
			}
			
			if (tCheck == null || naCheck == -1) {
				tCheck = hasImgUrl;
				naCheck = numberOfActions;
			}
			
			if (numberOfActions > 3 || !hasImgUrl) 
				return false;
		
		}
		
		return true;
	}
	
	private JSONObject buildCarouselMenu(QAContext ctx, FormalAnswerReplacer far,
		OptionMenu menu, int opNum) throws Exception {
		List<Option> options = menu.getOptions();
		JSONArray elements = new JSONArray();
		for (Option option : options) {
			int currentOpNum = opNum++;
			ParsedOption po = option.getValue();
			String imgUrl  = null;
			String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
			String text = TemplateUtil.processTwiceAndClean(ctx, opName, far);
			String payload = TemplateUtil.processTwiceAndClean(ctx, opName, far);
			JSONObject element = new JSONObject();
			JSONArray buttons = new JSONArray();
			
			if (po != null && po.extraParams != null) {
				imgUrl = po.extraParams.optString("image_url");
				String title = po.extraParams.optString("title");
				String subTitle = po.extraParams.optString("subTitle");
				
				if (StringUtils.trimToNull(text) == null) continue;
				
				if (StringUtils.isNotEmpty(title)) {
					text = title;
				}
				if (StringUtils.isNotEmpty(subTitle)) {
					payload = subTitle;
				}
				buttons = po.extraParams.optJSONArray("buttons");
			}
			
			JSONObject button = new JSONObject();
			button.put("type", "postback");
			button.put("title", StringUtils.left(text, 20));	// 按鈕限制20字
			//button.put("payload", "" + currentOpNum);
			button.put("payload", "action=_message&message=" + currentOpNum + "&est=" + QAContext.EVENT_SOURCE_TYPE_BUTTON + "&es=" + menu.getCreateFrom());
			buttons.add(button);
			
			if (imgUrl != null) {
				element.put("image_url", imgUrl);
			}
			if (StringUtils.isNotEmpty(text)) {
				element.put("title", StringUtils.left(text, 80));
			}
			element.put("subTitle", StringUtils.left(payload, 80));	// 必要
			
			element.put("buttons", buttons);
			elements.add(element);
		}
		JSONObject payload = new JSONObject();
		payload.put("template_type", "generic");
		payload.put("elements", elements);
		
		JSONObject attachment = new JSONObject();
		attachment.put("type", "template");
		attachment.put("payload", payload);
		
		JSONObject message = new JSONObject();
		message.put("attachment", attachment);
		return message;
	}
	
	private JSONObject buildQuickReply(QAContext ctx, FormalAnswerReplacer far, OptionMenu menu) throws Exception {
		List<Option> options = menu.getOptions();
		List<String> menus = new ArrayList<String>();

		JSONObject message = new JSONObject();
		JSONArray quickReplies = new JSONArray();
		
		for (Option option : options) {
			ParsedOption po = option.getValue();
			String opName = FunctionUtil.collectExecAndReplace(po.title, ctx);
			String label = TemplateUtil.processTwiceAndClean(ctx, opName, far);
			menus.add(label);
			JSONObject quickReply = new JSONObject();
			quickReply.put("content_type", "text");
			quickReply.put("title", label); 
			quickReply.put("payload", "action=_message&message=" + label + "&est=" + QAContext.EVENT_SOURCE_TYPE_BUTTON + "&es=" + menu.getCreateFrom());
			quickReplies.add(quickReply);
		}
		message.put("text", RobotFormalAnswers.giveMeRandomAnswer(ctx, "YOU_MAY_WANT_TO_KNOW", "參考以下問題，看是否有可以幫助您的："));
		message.put("quick_replies", quickReplies);
		
		return message;
	}
	
	private JSONObject buildGenericTemplate(QAContext ctx, FormalAnswerReplacer far,
		OptionMenu menu, String title) throws Exception {
		List<Option> options = menu.getOptions();
		JSONObject message = new JSONObject();
		JSONObject attachment = new JSONObject();
		JSONObject payload = new JSONObject();
		JSONArray elements = new JSONArray();
		
		// 總共要產生幾個旋轉選單
		int extra = options.size() % TEMPLATE_BUTTON_LIMIT;
		int genericCount = options.size() / TEMPLATE_BUTTON_LIMIT + (extra > 0 ? 1 : 0);
		for (int i = 0; i < genericCount; i++) {
			JSONObject element = new JSONObject();
			JSONArray buttons = new JSONArray();
			for (int j = 0; j < (i == genericCount - 1 ? (extra == 0 ? TEMPLATE_BUTTON_LIMIT : extra) : TEMPLATE_BUTTON_LIMIT); j++) {
				ParsedOption po = options.get(j + i * TEMPLATE_BUTTON_LIMIT).getValue();
				String opName = FunctionUtil.collectExecAndReplace(po.title, ctx);
				String label = TemplateUtil.processTwiceAndClean(ctx, opName, far);
				JSONObject button = new JSONObject();
				button.put("type", "postback");
				button.put("title", label);
				//button.put("payload", label);
				button.put("payload", "action=_message&message=" + label + "&est=" + QAContext.EVENT_SOURCE_TYPE_BUTTON + "&es=" + menu.getCreateFrom());
				buttons.add(button);
			}
			element.put("buttons", buttons);
			element.put("title", title);
			elements.add(element);
		}
		payload.put("elements", elements);
		payload.put("template_type", "generic");
		attachment.put("type", "template");
		attachment.put("payload", payload);
		message.put("attachment", attachment);
		
		return message;
	}
	
	// 取得答案中包含{{Q:TITLE:TEXT}} & {{L:TITLE:LINK}}的選項
	private ArrayList<JSONObject> getAnswerOptions(ArrayList<JSONObject> options, ArrayList<Map<String, Object>> option) {
		try {
			int limit = BUTTONS_DIALOG_LENGTH;
			for (int i = 0; option != null && i < option.size(); i++) {
				JSONObject jo = new JSONObject(option.get(i));
				if (jo.getString("title").length() <= limit + 6) {
					options.add(new JSONObject(option.get(i)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return options;
	}
	
	private JSONObject buildButtonTemplate(ArrayList<JSONObject> options, String title) throws JSONException {
		JSONObject message = new JSONObject();
		JSONObject attachment = new JSONObject();
		JSONObject payload = new JSONObject();
		JSONArray buttons = new JSONArray();
		for (JSONObject option : options) {
			JSONObject button = new JSONObject();
			if (option.getString("type").equals("uri")) {
				button.put("type", "web_url");
				button.put("url", option.get("link"));
				button.put("title", option.get("title"));
			} else {
				button.put("type", "postback");
				button.put("payload", option.get("link"));
				button.put("title", option.get("title"));
			}
			buttons.add(button);
		}
		payload.put("template_type", "button");
		payload.put("text", title);
		payload.put("buttons", buttons);
		attachment.put("payload", payload);
		attachment.put("type", "template");
		message.put("attachment", attachment);
		return message;
	}
	
	private JSONObject buildGenericTemplate(List<Map<String, Object>> imgs, ArrayList<JSONObject> options, String title) throws JSONException {
		JSONObject message = new JSONObject();
		JSONObject attachment = new JSONObject();
		JSONObject payload = new JSONObject();
		JSONArray elements = new JSONArray();
		
		// 總共要產生幾個旋轉選單
		int extra = options.size() % TEMPLATE_BUTTON_LIMIT;
		int elementCount = options.size() / TEMPLATE_BUTTON_LIMIT + (extra > 0 ? 1 : 0);
		for (int i = 0; i < elementCount; i++) {
			JSONObject element = new JSONObject();
			JSONArray buttons = new JSONArray();
			for (int j = 0; j < (i == elementCount - 1 ? (extra == 0 ? TEMPLATE_BUTTON_LIMIT : extra) : TEMPLATE_BUTTON_LIMIT); j++) {
				JSONObject option = options.get(j + i * TEMPLATE_BUTTON_LIMIT);
				JSONObject button = new JSONObject();
				button.put("type", "postback");
				button.put("title", option.get("title"));
				button.put("payload", option.get("link"));
				buttons.add(button);
			}
			element.put("buttons", buttons);
			element.put("title", title);
			
			// 這裡判斷要不要塞 img
			// 1. 如果 imgs.size() == elementCount，則一個 element 對應一張 img
			// 2. 否則就是固定用 imgs.get(0) 當作 img
			if (imgs != null) {
				Map<String, Object> img = null;
				if (imgs.size() == elementCount) {
					img = imgs.get(i);
				}
				else {
					img = imgs.get(0);
				}
				
				if (img != null) {
					element.put("image_url", img.get("link"));
				}
			}
			
			elements.add(element);
		}
		payload.put("elements", elements);
		payload.put("template_type", "generic");
		attachment.put("type", "template");
		attachment.put("payload", payload);
		message.put("attachment", attachment);
		
		return message;
	}
	
	private JSONObject buildQuickReply(ArrayList<JSONObject> options, String title) throws JSONException {
		JSONObject message = new JSONObject();
		JSONArray quickReplies = new JSONArray();
		for (JSONObject option : options) {
			JSONObject quickReply = new JSONObject();
			quickReply.put("content_type", "text");
			quickReply.put("title", option.get("title"));
			quickReply.put("payload", option.get("link"));
			quickReplies.add(quickReply);
		}
		message.put("text", title);
		message.put("quick_replies", quickReplies);
		return message;
	}
}
