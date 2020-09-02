package com.intumit.solr.robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.connector.line.RichMessage;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.util.StringUtil;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapAction;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;

/**
 * 這裡只做部分格式轉換，最終轉成 LINE API 要的格式，會在 conn-line.jsp 當中完成
 * 目前分工沒有很清楚。也許之後應該統一在同一個地方完成
 * 
 * 目前邏輯比較容易搞錯的地方是，這裡基本上如果沒有必要不處理「主要回答內容文字」的部分
 * conn-line.jsp 會把 AnswerText 轉成 LINE 需要的 message。
 * 但因為 LINE 的回覆支援一次 reply 可以給一個 JSON array 包含最多五個 message
 * 所以這些額外的 message 就利用這裡來處理。
 * 
 * 要多個 message 的原因是為了儘可能使用 LINE 的一些豐富的回應格式，例如「圖卡」、「多按鈕選單」...
 * 
 * @author herb
 */
public class NaverLineAnswerTransformer implements AnswerTransformer {
	
	public static final int IMAGEMAP_MODE_LENGTH = 8;
	public static final int CONFIRM_MODE_LENGTH = 4;
	public static final int IMAGEMAP_DIALOG_LENGTH = 10;
	public static final int BUTTONS_DIALOG_LENGTH = 20;
	public static final String HASH_ALGORITHM = "HmacSHA256";

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		
		if (answerText == null)
			answerText = new StringBuilder();

		try {
		    JSONObject botCfg = new JSONObject(StringUtils.defaultIfEmpty(ctx.getTenant().getLineBotConfigJson(), "{}"));
			
			JSONObject line = new JSONObject();
			JSONArray messages = new JSONArray();
			if (ctx.response.containsKey("line")) {
				line = (JSONObject)ctx.response.get("line");
			}
			
			if (line.containsKey("messages")) {
				messages = line.getJSONArray("messages");
			}
			
			//processLinksAndImages(ctx, messages, botCfg);
			
			if(!generateLineRichMessage(ctx, botCfg, messages, ch.getUseHtmlNewline())) {
				generateLineResponseFromAnswerText(ctx, botCfg, messages, answerText, ch.getUseHtmlNewline());
			}
			
			processOptionMenus(ctx, messages, botCfg, ch.getUseHtmlNewline());
			
			while (messages.length() > 5) {
				// 只能活下5個
				messages.remove(messages.length() - 1);
			}
			
			line.put("messages", messages);
			
			System.out.println("==== Final LINE messages ====");
			try {
				System.out.println(new org.json.JSONArray(messages.toString(2)).toString(2));
			}
			catch (org.json.JSONException ignore) {
				ignore.printStackTrace();
			}
			
			ctx.response.put("line", line);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		//ctx.setAnswerText(output);
		return ctx;
	}
	
    private boolean generateLineRichMessage(QAContext ctx, JSONObject botCfg, JSONArray currentMessages, boolean useHtmlNewLine) throws JSONException {
    	boolean hasRichMessage = false;
    	if (ctx.getCurrentQA() != null) {
	    	QA currentQA = ctx.getCurrentQA();
			Long kid = (Long) currentQA.getFieldValue("kid_l");
			MultiChannelAnswer mca = null;
	
			if (kid != null) {
				mca = MultiChannelAnswer.findNonEmptyAnswer(ctx, currentQA.getId(), ctx.getQAChannelInstance(), ctx.getUserType());
			}
			
	    	if (mca != null && StringUtils.isNotBlank(mca.getLineMKey())) {
				RichMessage rm = RichMessage.getByMKey(ctx.getTenant().getId(), mca.getLineMKey());
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
	    				String type = msgCfg.optString("type", "");
	    				
	    				if (type.equals("")) {
	    					continue;
	    				}
	    				else if (type.equals(MultiChannelAnswer.LINE_ANSWER_TEXT)) {
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
			    				addEventSource(rmj, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
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
	
    /**
     * Traverse actions and append event source / event source type information to the postback.data
     * @param rmj
     * @param ctx
     */
	public static void addEventSource(JSONObject rmj, String eventSource, String eventSourceType) {
		try {
			if (rmj.has("actions")) {
				addEventSource(rmj.getJSONArray("actions"), eventSource, eventSourceType);
			}
			if (rmj.has("template")) {
				JSONObject tpl = rmj.optJSONObject("template");
				addEventSource(tpl, eventSource, eventSourceType);
			}
			if (rmj.has("columns")) {
				JSONArray cols = rmj.optJSONArray("columns");
				
				for (int i=0; i < cols.length(); i++) {
					JSONObject col = cols.getJSONObject(i);
					addEventSource(col, eventSource, eventSourceType);
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    /**
     * Traverse actions and append event source / event source type information to the postback.data
     * @param rmj
     * @param ctx
     */
	public static void addEventSource(JSONArray actions, String eventSource, String eventSourceType) {
		for (int i=0; i < actions.length(); i++) {
			try {
				JSONObject a = actions.getJSONObject(i);
				
				// 這是 for quick reply 的結構是 items[i].["aciton"] （判斷依據是 items[i] 發現 type == "action"，就再 getJSONObject("action") 才做後面比對
				if ("action".equals(a.optString("type"))) {
					a = a.optJSONObject("action");
				}
				
				switch (a.optString("type", "")) {
					case "postback":
						if (a.has("data")) {
							String data = a.optString("data", "");
							data += "&est=" + eventSourceType;
							data += "&es=" + eventSource;
							a.put("data", data);
						}
						
						break;
					default:
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void generateLineResponseFromAnswerText(QAContext ctx, JSONObject botCfg, JSONArray currentMessages, StringBuilder answerText, boolean useHtmlNewLine) throws JSONException {
		
		String output = answerText.toString();
		
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
							currentMessages.add(arr.get(i));
						}
					}
					// 應該用 extraParams.line.messages[0..N] 這樣的格式
					if (extraParams.has(ctx.getQaChannel())) {
						JSONArray arr = extraParams.optJSONObject(ctx.getQaChannel(), new JSONObject()).optJSONArray("messages", new JSONArray());
						
						for (int i=0; i < arr.length(); i++) {
							currentMessages.add(arr.get(i));
						}
					}
				}
				catch (Exception ignoreIt) {}
			}

			// 應該用 extraParams.messenger.messages[0..N] 這樣的格式
			if (mca != null && StringUtils.isNotEmpty(mca.getExtraParameters())) {
				try {
					JSONObject ep = new JSONObject(mca.getExtraParameters());

					if (ep.has("messages")) {
						JSONArray arr = ep.optJSONArray("messages", new JSONArray());
						for (int i=0; i < arr.length(); i++) {
							currentMessages.add(arr.get(i));
						}
					}
				}
				catch (Exception ignoreIt) {}
			}
		}
		
/*			if (output.length() == 0 && messages.length() == 0) {
			output = StringUtils.trimToEmpty("{{F:NO_ANSWER}}");
		}*/

		output = WiSeUtils.br2nl(output).replaceAll("<[^>]+>", " ");
		output = FunctionUtil.collectExecAndReplace(output, ctx);

		TagLGQReplacer tagLGQ = new TagLGQReplacer(ctx);
		output = TemplateUtil.process(ctx, output, tagLGQ);
		
		FormalAnswerReplacer far = ctx.getQAUtil().getFormalAnswerReplacer(ctx);
		output = TemplateUtil.processTwice(ctx, output, far);
		output = TemplateUtil.process(ctx, output, tagLGQ);
		
		ctx.setAnswerText(output);
		
		// 這裡直接把 answerText 塞在 messages 第一個，不再等到 conn-line.jsp 當中才去 insert
		if (output.length() > 0) {
			JSONObject msgObj = new JSONObject();
			try {
				msgObj.put("type", "text");
				msgObj.put("text", output);
				
				currentMessages.add(0, msgObj);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * 
	 * @param ctx
	 * @param currentMessages
	 * @param botCfg
	 * @throws JSONException
	 */
	/*private void processLinksAndImages(QAContext ctx, JSONArray currentMessages, JSONObject botCfg) throws JSONException {
		
		JSONObject lineBotCfg = botCfg.has("line") ? botCfg.getJSONObject("line") : botCfg; // Hardcode for old json format, should be removed someday.
	    String baseUrl = lineBotCfg.optString("baseUrl", StringUtils.trimToEmpty((String)ctx.getCtxAttr("baseUrl")));
		
		ArrayList<JSONObject> allImages = new ArrayList<JSONObject>();
		allImages = isGoodForDialogMode(allImages, ctx.getJSONArrayIMG());
		
		ArrayList<JSONObject> allLinks = new ArrayList<JSONObject>();
		allLinks = isGoodForDialogMode(allLinks, ctx.getJSONArrayLink());
		allLinks = isGoodForDialogMode(allLinks, ctx.getJSONArrayQ());
		
		if (allImages.size() > 0) {
			for (JSONObject allImage : allImages) {
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				ImagemapArea area = new ImagemapArea(0, 0, 1040, 1040);
				List<ImagemapAction> imagemapActions = new ArrayList<ImagemapAction>();
				imagemapActions.add(new URIImagemapAction(allImage.getString("link"), area));
				ImagemapMessage imagemap = new ImagemapMessage(allImage.getString("link"),
						allImage.getString("title"), new ImagemapBaseSize(1040, 1040), imagemapActions);
				String json = null;
				try {
					json = ow.writeValueAsString(imagemap);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				currentMessages.add(new JSONObject(json));
			}
		}
		
		//以下將{{L}}、{{Q}}轉成imagemap
		if (allLinks.size() > 0) {
			String menuHeader = ctx.getMessage("global.pleaseSelectOrEnterNumber.toGetDetailInfo"); //"請輸入數字或點擊選項，了解詳細內容";
			List<String> menus = new ArrayList<>();
			JSONObject m2iCfg = lineBotCfg.getJSONObject("menuToImage");
			for (JSONObject allLink : allLinks) {
				menus.add(allLink.getString("title"));
			}
			org.json.JSONObject obj = MenuImageCreator.create(menus, "jpg", 
					// 詳情請參考 tenant.lineBotConfigJson 欄位設定
					m2iCfg.optString("bgColor", "#FFFFFF"),
					m2iCfg.optString("fontColor", "#3B5998"),
					m2iCfg.optString("boxBgColor", "#FFFFFF"),
					m2iCfg.optString("boxBorderColor", "#3B5998"),
					m2iCfg.optInt("borderStroke", 5)
					);

			// 用來將物件parse成JSON string
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			
			// 超過四個以上使用imagemap, 四個以下使用Buttons Template
			try {
				int i = 0;
				org.json.JSONArray map = obj.getJSONArray("map");
				List<ImagemapAction> imagemapActions = new ArrayList<ImagemapAction>();
				List<Action> buttonsActions = new ArrayList<Action>();
				for (JSONObject allLink : allLinks) {
					org.json.JSONObject mapEntry = map.getJSONObject(i);
					ImagemapArea area = new ImagemapArea(mapEntry.getInt("x"), 
					mapEntry.getInt("y"), mapEntry.getInt("width"), mapEntry.getInt("height"));
					if (allLink.getString("type").equals("uri")) {
						imagemapActions.add(new URIImagemapAction(allLink.getString("link"), area));
						buttonsActions.add(new URIAction(allLink.getString("title"), allLink.getString("link")));
					} else {
						imagemapActions.add(new MessageImagemapAction(allLink.getString("title"), area));
						buttonsActions.add(new MessageAction(allLink.getString("title"), allLink.getString("link")));
					}
					i++;
				}
				
				if (allLinks.size() > 4) {
					ImagemapMessage imagemap = new ImagemapMessage(
						baseUrl + "/commons/imagemaps/" + obj.getString("imageFilename"), menuHeader,
						new ImagemapBaseSize(Integer.parseInt(obj.getString("height")), 1040), imagemapActions);
					String json = ow.writeValueAsString(imagemap);
					currentMessages.add(new JSONObject(json));
				} else {
					ButtonsTemplate template = new ButtonsTemplate(null, null, menuHeader, buttonsActions);
					TemplateMessage message = new TemplateMessage(menuHeader, template);
					String json = ow.writeValueAsString(message);
					currentMessages.add(new JSONObject(json));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// 將answer中選項的字處理掉
			//int length = TemplateUtil.getCollectLength(output);
			//output = output.substring(0, output.length() - length);
			
		}
	}*/
	
	private void processOptionMenus(QAContext ctx, JSONArray currentMessages, JSONObject botCfg, boolean useHtmlNewLine) throws JSONException {
		JSONObject lineBotCfg = botCfg.has("line") ? botCfg.getJSONObject("line") : botCfg; // Hardcode for old json format, should be removed someday.
	    boolean enableMenu2Image = lineBotCfg.optJSONObject("menuToImage", new JSONObject()).optBoolean("enable", false); // check botCfg.line.menuToImage.enable?
	    boolean enableMenu2QuickReply = lineBotCfg.optBoolean("enableMenuToQuickReply", false);  
	    boolean usePostbackForTemplateAction = lineBotCfg.optBoolean("usePostbackForTemplateAction", false); 
	    boolean addDisplayTextWhenUsingPostback = lineBotCfg.optBoolean("addDisplayTextWhenUsingPostback", false); // Only works when usePostbackForTemplateAction=true
	    //String baseUrl = lineBotCfg.optString("baseUrl", StringUtils.trimToEmpty((String)ctx.getCtxAttr("baseUrl")));
	    
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
			
			if (isGoodForButtonsTemplateMode(menuHeader, menuFooter, preExecutedOptionNames)) { // LINE 的限制，沒有超過四個選項的話可以用 template
				JSONObject message = new JSONObject();
				message.put("type", "template");
				message.put("altText", menuHeader);
				currentMessages.add(message);
				
				JSONObject template = new JSONObject();
				
				// 兩個選項就用 confirm
				if (isGoodForConfirmMode(menuHeader, menuFooter, preExecutedOptionNames)) {
					template.put("type", "confirm");
					template.put("text", StringUtils.defaultIfEmpty(menuHeader, StringUtils.left(menuFooter, 160)));
				}
				else {
					template.put("type", "buttons");
					template.put("title", StringUtils.left(menuHeader, 40));
					template.put("text", StringUtils.defaultIfEmpty(StringUtils.left(menuFooter, 160), "　"));
				}
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
				addEventSource(actions, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
				
				template.put("actions", actions);
			}
			else if (enableMenu2QuickReply && isGoodForQuickReplyMode(menuHeader, menuFooter, preExecutedOptionNames)) { // LINE 的限制，沒有超過四個選項的話可以用 template
				JSONObject message = new JSONObject();
				message.put("type", "text");
				message.put("text", menuHeader + StringUtils.trimToEmpty("\n" + menuFooter));
				currentMessages.add(message);
				
				JSONObject quickReplyRoot = new JSONObject();
				
				// 兩個選項就用 confirm
				if (isGoodForConfirmMode(menuHeader, menuFooter, preExecutedOptionNames)) {
					quickReplyRoot.put("type", "confirm");
					quickReplyRoot.put("text", StringUtils.defaultIfEmpty(menuHeader, StringUtils.left(menuFooter, 160)));
				}
				else {
					quickReplyRoot.put("type", "buttons");
					quickReplyRoot.put("title", StringUtils.left(menuHeader, 40));
					quickReplyRoot.put("text", StringUtils.defaultIfEmpty(StringUtils.left(menuFooter, 160), "　"));
				}
				message.put("quickReply", quickReplyRoot);
				JSONArray items = new JSONArray();
				
				for (int i=0; i <  menu.options.size(); i++) {
					JSONObject actionRoot = new JSONObject();
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

						actionRoot.put("type", "action").put("action", action);
						items.add(actionRoot);
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

		    	QA currentQA = ctx.getCurrentQA();
				String es = currentQA != null ? currentQA.getId() : ("_ANSTYPE_" + ctx.getAnswerType());
				addEventSource(items, es, QAContext.EVENT_SOURCE_TYPE_BUTTON);
				quickReplyRoot.put("items", items);
			}
			/*else if (enableMenu2Image && isGoodForImageMapMode(preExecutedOptionNames)) {
			}*/
			else {
				StringBuffer buf = new StringBuffer();
				buf.append(menuHeader); buf.append(NL); 
				
				if (StringUtils.isNotEmpty(menuFooter)) {
					buf.append(menuFooter); buf.append(NL);
				}
				
				for (Option option : menu.options) {
					int currentOpNum = opNum++;
					buf.append(NL + menu.indentOrSplitString + TemplateUtil.processTwiceAndClean(ctx, "{{F:ENTER}}" + (currentOpNum) + "." + option.getName(), far));
				}

				String text = TemplateUtil.processTwiceAndClean(ctx, buf.toString(), far);
				JSONObject message = new JSONObject();
				message.put("type", "text");
				message.put("text", text);
				currentMessages.add(message);
			}
		}
	}

	/**
	 * LINE 的 Quick reply 按鈕數不能超過 13 個，每個按鈕最多 20 個字。
	 * 
	 * @param options
	 * @return
	 */
	private boolean isGoodForQuickReplyMode(String menuHeader, String menuFooter, List<String> preExecutedOptionNames) {
		if (preExecutedOptionNames.size() > 13)
			return false;
		
		for (String optionName: preExecutedOptionNames) {
			if (StringUtils.length(optionName) > 20) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * LINE 的 Buttons template 按鈕數不能超過 4 個，每個按鈕最多 20 個字。
	 * Title 不能超過 40 個字。
	 * Text 不能超過 160 字。
	 * 
	 * @param options
	 * @return
	 */
	private boolean isGoodForButtonsTemplateMode(String menuHeader, String menuFooter, List<String> preExecutedOptionNames) {
		if (preExecutedOptionNames.size() > 4)
			return false;
		
		if (StringUtils.length(menuHeader) > 40 || StringUtils.length(menuFooter) > 160) {
			return false;
		}
		
		for (String optionName: preExecutedOptionNames) {
			if (StringUtils.length(optionName) > 20) {
				return false;
			}
		}
		
		return true;
	}

	private boolean isGoodForConfirmMode(String menuHeader, String menuFooter, List<String> preExecutedOptionNames) {
		if (StringUtils.isNotEmpty(menuHeader) && StringUtils.isNotEmpty(menuFooter))
			return false;
		
		if (preExecutedOptionNames.size() == 2) {
			for (String optionName: preExecutedOptionNames) {
				if (optionName.length() > CONFIRM_MODE_LENGTH)
					return false;
			}
		}
		else {
			return false;
		}
		
		return true;
	}
	
	/**
	 * LINE 的 Carousel 訊息的規定是 如果有縮圖就每個選項都要有縮圖，如果有 Actions，就每個選項的 Actions 數量要一樣！
	 * 
	 * 
	 * @param options
	 * @return
	 */
	/*private boolean isGoodForCarouselMode(List<Option> options) {
		Boolean tCheck = null;
		int naCheck = -1;
		
		for (Option option: options) {
			ParsedOption po = option.getValue();
			boolean hasThumbnail = false;
			int numberOfActions = 1; // 如果 extraParams.actions 是空的，預設就是一個選項，就是這個 option 自己
			
			if (po != null && po.extraParams != null) {
				if (po.extraParams.has("thumbnailImageUrl")) {
					hasThumbnail = true;
				}
				else  {
					hasThumbnail = false;
				}
				
				if (po.extraParams.has("actions")) {
					try {
						numberOfActions = po.extraParams.getJSONArray("actions").length();
					}
					catch (Exception e) {e.printStackTrace();}
				}
			}
			else {
				hasThumbnail = false;
			}
			
			if (tCheck == null || naCheck == -1) {
				tCheck = hasThumbnail;
				naCheck = numberOfActions;
			}
			else {
				if (!hasThumbnail || numberOfActions != naCheck)
					return false;
			}
		}
		
		return true;
	}*/

	private boolean isGoodForImageMapMode(List<String> preExecutedOptionNames) {
		for (String optionName: preExecutedOptionNames) {
			if (optionName.length() > IMAGEMAP_MODE_LENGTH)
				return false;
		}
		
		return true;
	}

	
	/*private ArrayList<JSONObject> isGoodForDialogMode(ArrayList<JSONObject> allLinks, ArrayList<Map<String, Object>> link) {
		try {
			int limit = BUTTONS_DIALOG_LENGTH;
            if (link.size() > 4) 
            	limit = IMAGEMAP_DIALOG_LENGTH;
			for (int i = 0; link != null && i < link.size(); i++) {
				JSONObject jo = new JSONObject(link.get(i));
                if (jo.getString("title").length() <= limit + 6)	// 6 => {{Q:}}
					allLinks.add(new JSONObject(link.get(i)));
			}
		} catch (Exception e) {
		}

		return allLinks;
	}*/
	
	/**
	 * @deprecated
	 * 
	 * @param message
	 * @param okLabel
	 * @param okText
	 * @param cancelLabel
	 * @param cancelText
	 * @return
	 */
	public static JSONObject confirmMessage(String message, String okLabel, String okText, String cancelLabel, String cancelText) {
		JSONObject confirmTemplate = new JSONObject();
		try {
    		JSONArray actions = new JSONArray();
    		JSONObject action = null;
    		action = new JSONObject().put("type", "message").put("label", okLabel).put("text", okText);
    		actions.add(action);
    		action = new JSONObject().put("type", "message").put("label", cancelLabel).put("text", cancelText);
    		actions.add(action);
        		
			JSONObject template = new JSONObject();
			template.put("type", "confirm");
			template.put("text", message);
			template.put("actions", actions);
			
			confirmTemplate.put("type", "template");
			confirmTemplate.put("altText", message);
			confirmTemplate.put("template", template);
	    } 
	    catch (Exception e) {}
		return confirmTemplate;
	}


	/**
	 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
	 */
	public static JSONObject getUserProfile(String userId, String accessToken, QAContext qaCtx) {

		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try {
			HttpGet request = new HttpGet("https://api.line.me/v2/bot/profile/" + userId);
			System.out.println(accessToken);
			request.addHeader("Authorization", "Bearer " + accessToken);
			HttpResponse response = httpClient.execute(request);

			if (response != null) {
				System.out.println("StatusCode : " + response.getStatusLine().getStatusCode());

				BufferedReader in = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				String inputLine;
				StringBuffer resultData = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					resultData.append(inputLine);
				}
				in.close();

				JSONObject profile = new JSONObject(resultData.toString());
				return profile;
			}
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
		finally {
			httpClient.getConnectionManager().shutdown();
		}
		
		return null;
	}
	

    /**
     * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
     */
    public static void push(String accessToken, String userId, JSONArray messages) {
     	
     	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        	try {
			JSONObject json = new JSONObject();
			json.put("to", userId);
			json.put("messages", messages);

			HttpPost request = new HttpPost("https://api.line.me/v2/bot/message/push");
			StringEntity params = new StringEntity(json.toString());
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Bearer " + accessToken);
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);

			if (response != null) {
				System.out.println("StatusCode : " + response.getStatusLine().getStatusCode());

				BufferedReader in = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));
				String inputLine;
				StringBuffer resultData = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					resultData.append(inputLine);
				}
				in.close();

				System.out.println(resultData.toString());
			}
        	} catch (Exception ex) {
                 // handle exception here
             	System.out.println(ex.toString());
        	} finally {
                 httpClient.getConnectionManager().shutdown();
        	}
    }
    
    
	public static boolean compareSignature(String signature, String body, String channelSecret) throws Exception {
		try {
			SecretKeySpec key = new SecretKeySpec(channelSecret.getBytes(), HASH_ALGORITHM);
			Mac mac = Mac.getInstance(HASH_ALGORITHM);
			mac.init(key);
			byte[] source = body.getBytes(StandardCharsets.UTF_8);
			String createdSignature = Base64.encodeBase64String(mac.doFinal(source));
			return createdSignature.equals(signature);
		} catch (Exception e) {
		}
		return false;
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
