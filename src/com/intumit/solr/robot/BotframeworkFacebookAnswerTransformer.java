package com.intumit.solr.robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfig;
import com.runawaybits.html2markdown.HTML2Md;

/**
 * 
 * 
 * @author herb
 */
public class BotframeworkFacebookAnswerTransformer implements AnswerTransformer {

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		
		if (answerText == null)
			answerText = new StringBuilder();

		try {
		    JSONObject botCfg = new JSONObject(ctx.getTenant().getLineBotConfigJson());
		    JSONObject bfcBotCfg = botCfg.optJSONObject("botframework", new JSONObject());
		    boolean enableMenu2Image = bfcBotCfg.optJSONObject("menuToImage", new JSONObject()).optBoolean("enable", false); // check botCfg.line.menuToImage.enable?
		    String baseUrl = bfcBotCfg.optString("baseUrl", StringUtils.trimToEmpty((String)ctx.getCtxAttr("baseUrl")));
		    
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			String output = answerText.toString();
			
			output = FunctionUtil.collectExecAndReplace(output, ctx);

			FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant())
					.getFormalAnswerReplacer(ctx);
			output = TemplateUtil.processTwice(ctx, output, far);

			FormalAnswerStickerReplacer farSticker = QAUtil.getInstance(ctx.getTenant())
					.getFormalAnswerStickerReplacer(ctx);
			output = TemplateUtil.process(ctx, output, farSticker);
			
			ctx.setAnswerText(output);

			int opNum = 1;
			JSONArray messages = (JSONArray)ctx.getResponseAttribute("messages");
			if (messages == null) messages = new JSONArray();

			for (String key : ctx.showedOptionMenuKeys) {
				OptionMenu menu = ctx.optionMenuMap.get(key);
				if (menu.view == MenuView.HIDDEN) continue;
				
				if (isGoodForCarouselMode(menu.options) && menu.optionsSize() <= 10) {
					String menuHeader = StringUtils.trimToNull(WiSeUtils.html2md(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far)));
					if (menuHeader == null)
						menuHeader = ctx.getMessage("global.pleaseSelect");

					JSONObject message = new JSONObject();
					messages.add(message);
					JSONObject channelData = new JSONObject();
					message.put("channelData", channelData);
					
					JSONObject attachment = new JSONObject();
					attachment.put("type", "template");
					attachment.put("altText", menuHeader);
					channelData.put("attachment", attachment);
					
					JSONObject payload = new JSONObject();
					attachment.put("payload", payload);
					payload.put("template_type", "generic");
					
					JSONArray columns = new JSONArray();
					for (Option option : menu.options) {
						int currentOpNum = opNum++;
						ParsedOption po = option.getValue();
						String thumbnail = null;
						String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
						String label = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						String text = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						JSONObject column = new JSONObject();
						JSONArray actions = null;
						
						if (po != null && po.extraParams != null) {
							thumbnail = po.extraParams.optString("thumbnailImageUrl");
							String tmpTitle = po.extraParams.optString("title");
							String tmpText = po.extraParams.optString("text");
							
							if (StringUtils.isNotEmpty(tmpTitle)) {
								label = tmpTitle;
							}
							if (StringUtils.isNotEmpty(tmpText)) {
								text = tmpText;
							}
							//actions = po.extraParams.optJSONArray("actions");
						}
						
						if (actions == null) {
							actions = new JSONArray();
							
							JSONObject action = new JSONObject();
							action.put("type", "postback");
							action.put("title", StringUtils.left(label, 20));
							action.put("payload", text);
							
							actions.add(action);
						}
						
						if (thumbnail != null) {
							column.put("image_url", thumbnail);
						}
						
						column.put("title", StringUtils.left(label, 80));// title & text 長度限制也是 FB 的限制
						column.put("subtitle", StringUtils.left(text, 80));
						
						column.put("buttons", actions);
						columns.add(column);
					}
					
					payload.put("elements", columns);
				}
				else if (menu.optionsSize() < 4) { // 沒有超過四個選項的話可以用 template
					String menuHeader = StringUtils.trimToNull(WiSeUtils.html2md(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far)));
					if (menuHeader == null)
						menuHeader = ctx.getMessage("global.pleaseSelect");

					JSONObject message = new JSONObject();
					messages.add(message);
					JSONObject channelData = new JSONObject();
					message.put("channelData", channelData);
					
					JSONObject attachment = new JSONObject();
					attachment.put("type", "template");
					channelData.put("attachment", attachment);
					
					JSONObject payload = new JSONObject();
					payload.put("template_type", "button");
					payload.put("text", WiSeUtils.html2md(StringUtils.left(menuHeader, 640)));// FB 限制 640 個字
					attachment.put("payload", payload);
					
					JSONArray actions = new JSONArray();
					
					for (Option option : menu.options) {
						int currentOpNum = opNum++;
						String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
						String label = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						String text = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						
						if (StringUtils.trimToNull(text) == null) continue;
						
						JSONObject action = new JSONObject();
						action.put("type", "postback");
						action.put("title", WiSeUtils.html2md(StringUtils.left(label, 20)));
						action.put("payload", "" + currentOpNum);
						
						actions.add(action);
					}
					payload.put("buttons", actions);
				}
				else if (isGoodForSuggestActions(menu.options)) { 
					JSONObject suggestedActions = new JSONObject();
					JSONArray actions = new JSONArray();
					
					for (Option option : menu.options) {
						int currentOpNum = opNum++;
						String opName = FunctionUtil.collectExecAndReplace(option.getName(), ctx);
						String label = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						String text = TemplateUtil.processTwiceAndClean(ctx, opName, far);
						
						if (StringUtils.trimToNull(text) == null) continue;
						
						JSONObject action = new JSONObject();
						action.put("type", "imBack");
						action.put("title", StringUtils.left(label, 20));
						action.put("value", text);
						
						actions.add(action);
					}
					suggestedActions.put("actions", actions);
					ctx.response.put("bfc_suggestedActions", suggestedActions);
				}
				else {
					String menuHeader = StringUtils.trimToNull(WiSeUtils.html2md(TemplateUtil.processTwiceAndClean(ctx, menu.menuHeader, far)));
					StringBuffer buf = new StringBuffer();
					buf.append(menuHeader != null ? menuHeader : ctx.getMessage("global.pleaseSelect"));
					buf.append(NL);
					
					for (Option option : menu.options) {
						int currentOpNum = opNum++;
						buf.append(NL + menu.indentOrSplitString + "{{F:ENTER}}" + (currentOpNum) + "." + option.getName());
					}
					
					JSONObject message = new JSONObject();

					String text = FunctionUtil.collectExecAndReplace(buf.toString(), ctx);
					message.put("text", WiSeUtils.html2md(TemplateUtil.processTwiceAndClean(ctx, text, far)));
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
			
			// ms-bfc.jsp 會處理這個資料
			ctx.response.put("bfc_additional_pushs", messages);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//ctx.setAnswerText(output);
		return ctx;
	}

	/**
	 * LINE 的 Carousel 訊息的規定是 如果有縮圖就每個選項都要有縮圖，如果有 Actions，就每個選項的 Actions 數量要一樣！
	 * 
	 * 
	 * @param options
	 * @return
	 */
	private boolean isGoodForCarouselMode(List<Option> options) {
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
			
			if (numberOfActions > 3 || !hasThumbnail) 
				return false;
		
		}
		
		return true;
	}

	private boolean isGoodForSuggestActions(List<Option> options) {
		for (Option option: options) {
			ParsedOption po = option.getValue();
			if (po.title.length() > 10)
				return false;
		}
		
		return true;
	}
	
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
}
