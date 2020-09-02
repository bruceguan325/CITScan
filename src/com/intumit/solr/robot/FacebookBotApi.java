package com.intumit.solr.robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;
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

import com.intumit.solr.tenant.Tenant;

public class FacebookBotApi {
	
	public static final String API_VERSION = "v2.10";
	public static final String MESSENGER_SEND_URL = "https://graph.facebook.com/" + API_VERSION + "/me/messages?access_token=";
	public static final String MESSENGER_GET_URL = "https://graph.facebook.com/" + API_VERSION + "/";
	public static final String MESSENGER_PASS_URL = "https://graph.facebook.com/" + API_VERSION + "/me/pass_thread_control?access_token=";
	public static final String MESSENGER_PASSTOPRIMARY_URL = "https://graph.facebook.com/" + API_VERSION + "/me/take_thread_control?access_token=";
	public static final String BUTTONS_TITLE = "請點選下列選項";
	public static final String METADATA = "UserIN";
	public static final String PAGE_INBOX_ID = "263902037430900";
	
	public static JSONObject POSTRequest(CloseableHttpClient httpClient,
		HttpPost request, JSONObject json) throws Exception {
		StringEntity params = new StringEntity(json.toString());
		request.addHeader("content-type", "application/json");
		request.setEntity(params);
		HttpResponse response = httpClient.execute(request);

		if (response != null) {
			System.out.println("StatusCode : " + response.getStatusLine().getStatusCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent()));
			String inputLine;
			StringBuffer resultData = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				resultData.append(inputLine);
			}
			in.close();

			System.out.println(resultData.toString());
			JSONObject result = new JSONObject(resultData.toString());
			return result;
		}
				
		return null;
	}
	
	public static JSONObject GETRequest(CloseableHttpClient httpClient, HttpGet request) throws Exception {
		HttpResponse response = httpClient.execute(request);

		if (response != null) {
			System.out.println("StatusCode : "
					+ response.getStatusLine().getStatusCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			String inputLine;
			StringBuffer resultData = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				resultData.append(inputLine);
			}
			in.close();

			JSONObject result = new JSONObject(resultData.toString());

			return result;
		}
		return null;
	}
	
	public static JSONObject buildTextMessage(String text) {
		JSONObject message = new JSONObject();
		try {
			message.put("text", text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}
	
	public static void main(String[] args) {	
		JSONObject aaa = passControlToPrimary("EAACk9yMUj6cBAKLo139XoCtBZBwjdLMCTcqc7LCdCftaH7amJEggSFooWkpA8jUlulUr2srgpwwZC2axesQIV7cuiCV4WvVOXNCuW4AWZBaBhcfpXhKQtoBQ8V7775pZAWGuj3KHr0frRAJNiZAQE48DlbOFY0ZCTtFHh7JBc7tQZDZD", "2131736046851529");
		System.out.println(checkSuccess(aaa));
	}
	
	// 交接通訊協定(轉換成ROBOT(Primary Receiver))
	@SuppressWarnings("deprecation")
	public static JSONObject passControlToPrimary(String pageToken, String userId) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			JSONObject json = new JSONObject();

			JSONObject recipient = new JSONObject();
			recipient.put("id", Long.valueOf(userId));
			json.put("recipient", recipient);
			json.put("metadata", METADATA);

			HttpPost request = new HttpPost(MESSENGER_PASSTOPRIMARY_URL + pageToken);
			result = POSTRequest(httpClient, request, json);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}
		
	//交接通訊協定(轉換成小編)
	@SuppressWarnings("deprecation")
	public static JSONObject passControl(String pageToken, String userId) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			JSONObject json = new JSONObject();
			
			JSONObject recipient = new JSONObject();
			recipient.put("id", userId);
	
			json.put("recipient", recipient);
			json.put("metadata", METADATA);
			json.put("target_app_id", Long.valueOf(PAGE_INBOX_ID));
			
			HttpPost request = new HttpPost(MESSENGER_PASS_URL + pageToken);
			result = POSTRequest(httpClient, request, json);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}
	
	public static boolean checkSuccess(JSONObject jsonObj) {
		try {
			return jsonObj.getBoolean("success");
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("deprecation")
	public static JSONObject push(String pageToken, String userId, JSONObject message) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			JSONObject json = new JSONObject();
			
			JSONObject recipient = new JSONObject();
			recipient.put("id", userId);
	
			json.put("recipient", recipient);
			json.put("message", message);

			HttpPost request = new HttpPost(MESSENGER_SEND_URL + pageToken);
			result = POSTRequest(httpClient, request, json);
		} catch (Exception ex) {
			// handle exception here
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}
	
	/*
	 * format
	 * {
	 *   "first_name": "Rock",
	 *   "last_name": "Tsai",
	 *   "profile_pic": "https://scontent.xx.fbcdn.net/v/t1.0-1/10891970_10202794776264547_7735284103185449987_n.jpg?oh=f9473778263c392d7a9485afc8a7fa5c&oe=5AA968AF",
	 *   "locale": "zh_TW",
	 *   "timezone": 8,
	 *   "gender": "male",
	 *   "id": "1107289276065123"
	 * }
	 */
	@SuppressWarnings("deprecation")
	public static JSONObject getProfile(String pageToken, String userId) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			HttpGet request = new HttpGet(MESSENGER_GET_URL + userId + "?access_token=" + pageToken);
			result = GETRequest(httpClient, request);
		} catch (Exception ex) {
			// handle exception here
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}
	
	public static String getPageToken(Integer tenantId) {
		try {
			JSONObject botCfg = new JSONObject(Tenant.get(tenantId).getLineBotConfigJson());
			JSONObject fbBotCfg = botCfg.has("messenger") ? botCfg.getJSONObject("messenger") : botCfg;
			String pageToken = fbBotCfg.getString("pageToken");
			return pageToken; 
		} 
		catch (Exception e) { } 
		return null;
	}
	
	public static String getVerifyToken(Integer tenantId) {
		try {
			JSONObject botCfg = new JSONObject(Tenant.get(tenantId).getLineBotConfigJson());
			JSONObject fbBotCfg = botCfg.has("messenger") ? botCfg.getJSONObject("messenger") : botCfg;
			String validationToken = fbBotCfg.getString("verifyToken");
			return validationToken; 
		} 
		catch (Exception e) { } 
		return null;
	}
	
	/*//測試區
	public static void main(String[] args) {
//		String dataStr = "IMG:https://service.sinotrade.com.tw/ir/commons/activities/20171107_PIC1-1040.png@URL:https://goo.gl/vK3FKq";
//		String dataStr = "OPT:[{\"LINK\":\"https://service.sinotrade.com.tw/stg/member/web/info/content?id=00ee98255fd614ec015fd72b089b2ab6\",\"TEXT\":\"《外資》小摩：台股2018年高點1...\"},{\"LINK\":\"https://service.sinotrade.com.tw/stg/member/web/info/content?id=00ee98255fcbc836015fce66ebb319f8\",\"TEXT\":\"《水泥股》年底趕工潮，台泥、環泥出...\"},{\"LINK\":\"https://service.sinotrade.com.tw/stg/member/web/info/content?id=00ee98255fc6a1da015fc95744782f77\",\"TEXT\":\"X 《證交所》今日單一證券鉅額成交...\"},{\"LINK\":\"https://service.sinotrade.com.tw/stg/member/web/info/content?id=00ee98245fc3d937015fc433a37206d4\",\"TEXT\":\"X 《證交所》今日單一證券鉅額成交...\"}]";	
		String dataStr = "Template:{\"type\":\"template\",\"altText\":\"客戶登入\",\"template\":{\"type\":\"buttons\",\"thumbnailImageUrl\":null,\"title\":null,\"text\":\"您好，詢問顧問訊息需進行永豐金證券客戶登入，請點擊下方「客戶登入」，登入完成後可點選「重新發問」，將可幫您重新發問一次！\",\"actions\":[{\"type\":\"uri\",\"label\":\"客戶登入\",\"uri\":\"https://service.sinotrade.com.tw/stg/member/sino?bot=MESSENGER:3:1692084560864645&botType=L\"},{\"type\":\"message\",\"label\":\"重新發問\",\"text\":\"2330分析\"}]}}";
//		String dataStr = "Template:{\"type\":\"template\",\"altText\":\"五大面向\",\"template\":{\"type\":\"carousel\",\"columns\":[{\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/TechnologyOriented.jpg\",\"title\":\"2017年11月17日 時間13:30:00\",\"text\":\"成交價：34.65\r\n成交量：6,276\r\n漲跌：0.35 漲跌幅：1.02%\",\"actions\":[{\"type\":\"message\",\"label\":\"個股報價\",\"text\":\"1101報價\"},{\"type\":\"uri\",\"label\":\"技術分析\",\"uri\":\"https://sodrobot.sinotrade.com.tw/z/zc/zcw/zcw1.djhtm?a=1101&W=1040&H=1040\"},{\"type\":\"message\",\"label\":\"大盤報價\",\"text\":\"大盤走勢\"}]},{\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/NewsOriented.jpg\",\"title\":\"20171117\",\"text\":\"X 《證交所》今日單一證券鉅額成交股票：聯電、鴻海、台積電、南亞科\",\"actions\":[{\"type\":\"uri\",\"label\":\"個股新聞資訊\",\"uri\":\"https://service.sinotrade.com.tw/stg/member/web/quote/detail/1101?category=news\"},{\"type\":\"uri\",\"label\":\"即時新聞\",\"uri\":\"https://service.sinotrade.com.tw/stg/member/web/info?bot=U3dbfb3a811b78799d3d2bb9a1876700e&botType=L\"},{\"type\":\"message\",\"label\":\" \",\"text\":\" \"}]},{\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/ChipsOriented.jpg\",\"title\":\"三大法人合計 -228 張\",\"text\":\"外資 -50 張\r\n投信 80 張\r\n自營商 -258 張\",\"actions\":[{\"type\":\"message\",\"label\":\"三大法人買賣超\",\"text\":\"1101三大法人買賣超\"},{\"type\":\"message\",\"label\":\"主力券商\",\"text\":\"1101主力券商\"},{\"type\":\"message\",\"label\":\"融資融券\",\"text\":\"1101融資融券\"}]},{\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/FinanceOriented.jpg\",\"title\":\"ROE：1.8\",\"text\":\"速動比率：107.63%\r\n長期資金適合率：199.84%\r\n營收成長率：3.17%\",\"actions\":[{\"type\":\"message\",\"label\":\"資產負債表\",\"text\":\"1101資產負債表\"},{\"type\":\"message\",\"label\":\"損益表\",\"text\":\"1101損益表\"},{\"type\":\"uri\",\"label\":\"財務比率\",\"uri\":\"http://stockchannelnew.sinotrade.com.tw/z/zc/zcr/zcr.djhtm?a=1101\"}]},{\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/BasicOriented.jpg\",\"title\":\"EPS:0.53元\",\"text\":\"月營收：8,387百萬\r\n殖利率：4.18% \r\n本益比：18.93\",\"actions\":[{\"type\":\"message\",\"label\":\"基本資料\",\"text\":\"1101基本資料\"},{\"type\":\"message\",\"label\":\"股利政策\",\"text\":\"1101股利政策\"},{\"type\":\"message\",\"label\":\"獲利能力指標\",\"text\":\"1101獲利能力指標\"}]}]}}";
//		String dataStr = "Template:{\"type\":\"template\",\"altText\":\"籌碼面\",\"template\":{\"type\":\"buttons\",\"thumbnailImageUrl\":\"https://service.sinotrade.com.tw/ir/commons/oriented/ChipsOriented.jpg\",\"title\":\"三大法人合計 5,104 張\",\"text\":\"外資 4,661 張\r\n投信 -51 張\r\n自營商 494 張\",\"actions\":[{\"type\":\"message\",\"label\":\"三大法人買賣超\",\"text\":\"2330三大法人買賣超\"},{\"type\":\"message\",\"label\":\"主力券商\",\"text\":\"2330主力券商\"},{\"type\":\"message\",\"label\":\"融資融券\",\"text\":\"2330融資融券\"},{\"type\":\"message\",\"label\":\"董監持股\",\"text\":\"2330董監持股\"}]}}";
//		String dataStr = "IMG:https://service.sinotrade.com.tw/ir/commons/stockAPI/EPS/EPS1101-20171120-299.png";
//		String dataStr = "Template:{\"type\":\"template\",\"altText\":\"請點選下方選擇買超排行或賣超排行資訊\",\"template\":{\"type\":\"buttons\",\"text\":\"請點選\",\"actions\":[{\"type\":\"message\",\"label\":\"三大法人買超排行\",\"text\":\"三大法人買超排行\"},{\"type\":\"message\",\"label\":\"三大法人賣超排行\",\"text\":\"三大法人賣超排行\"}]}}";
		try {
				System.out.println(FacebookBotApi.push("EAAENJ6vRrEoBAJKTYrDoZBx4vreCA2Hzw7gjHKnFfuykIgqwdQZBUhj0zKkod5qEgSsZAFhp2wTlgJQE2QMqInB3Pnl5ciWzjJTwatAkyCx60RB64j3NZArI6JU7DZCmT7pophlVxc9WrsTOZADhDB6lY0scgijksXjcJmxb4TM4K84bZBnzmAw", "1494726903909292", FacebookBotApi.lineToFBJSON(dataStr)).toString(2));
			} catch (JSONException e) {
				e.printStackTrace();
			}
	
	}*/

	public static JSONObject lineToFBJSON(String dataStr) {
		JSONObject json = new JSONObject();
		try {
			if (StringUtils.contains(dataStr, "Template:")) {
				JSONObject jsonLine = new JSONObject(dataStr.replace("Template:", ""));
				JSONObject jsonTemplate = jsonLine.getJSONObject("template");
				
				String lineType = jsonTemplate.getString("type");
				String thumbnailImageUrl = null;
				try {
					thumbnailImageUrl = jsonTemplate.getString("thumbnailImageUrl");
				} catch (Exception e) {
					System.out.println(e);
				}
				
				if (lineType.equals("carousel")) {
					json = convertCarouselMessage(jsonTemplate);
				} else if (lineType.equals("buttons")) {
					if(thumbnailImageUrl != null && thumbnailImageUrl.length() > 0){
						json = convertButtonsMessage(jsonTemplate);
					} else {
						json = convertReAskButtonsMessage(jsonTemplate);
					}
				}
			} else if (StringUtils.contains(dataStr, "IMG:")) {				
				String specialParts = StringUtils.substringAfter(dataStr, "IMG:");
				dataStr = StringUtils.trimToNull(StringUtils.stripEnd(StringUtils.substringBefore(dataStr, "IMG:"), "@"));
				
				String imgUrl = null;
				String link = null;
				
				boolean urlButton = false;
				if (specialParts.contains("@URL")) {
					imgUrl = StringUtils.substringBefore(specialParts, "@URL:");
					link = StringUtils.substringAfter(specialParts, "@URL:");
					urlButton = true;
				}
				else {
					imgUrl = specialParts;
				}
				
	     		if (dataStr != null)
	    	     		json.put( "text", dataStr );
	     		
	     		if(urlButton){
	     			json = convertIMGAndURLMessage(imgUrl, link);
	     		} else {
	     			json = convertIMGMessage(imgUrl);
	     		}
	     	} else if (StringUtils.contains(dataStr, "TEXT:")) {	
	     		json = buildTextMessage(dataStr.replace("TEXT:", ""));
	     	} else if (StringUtils.contains(dataStr, "OPT:")) {	
	     		json = convertOPTMessage(new JSONArray(dataStr.replace("OPT:", "")));
	     	}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONObject convertIMGAndURLMessage(String imgUrl, String link) {
		JSONObject json = new JSONObject();
		JSONObject jsonFBAttachmentObject = new JSONObject();
		JSONObject jsonFBPayloadObject = new JSONObject();
		JSONArray jsonFBElementsArray = new JSONArray();
		try {

			JSONObject jsonFBObject = new JSONObject();
			JSONObject jsonFBButtonsObject = new JSONObject();
			JSONObject jsonFBButtonsObject_1 = new JSONObject();
			jsonFBObject.put("contentType", "application/vnd.microsoft.card.hero");
			JSONObject jsonLINEContentObject = new JSONObject();

			jsonLINEContentObject.put("title", BUTTONS_TITLE);
			jsonLINEContentObject.put("image_url", imgUrl);

			JSONArray jsonFBButtonsArray = new JSONArray();
			
			jsonFBButtonsObject.put("title", "請點此觀看完整圖片");
			jsonFBButtonsObject.put("url", imgUrl);
			jsonFBButtonsObject.put("type", "web_url");
			
			jsonFBButtonsArray.put(jsonFBButtonsObject);
			
			jsonFBButtonsObject_1.put("title", "更多資訊請點此");
			jsonFBButtonsObject_1.put("url", link);
			jsonFBButtonsObject_1.put("type", "web_url");
			jsonFBButtonsArray.put(jsonFBButtonsObject_1);
			
			jsonLINEContentObject.put("buttons", jsonFBButtonsArray);
			jsonFBElementsArray.put(jsonLINEContentObject);

			jsonFBPayloadObject.put("template_type", "generic");
			jsonFBPayloadObject.put("elements", jsonFBElementsArray);

			jsonFBAttachmentObject.put("payload", jsonFBPayloadObject);
			jsonFBAttachmentObject.put("type", "template");
			json.put("attachment", jsonFBAttachmentObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONObject convertIMGMessage(String imgUrl) {
		JSONObject json = new JSONObject();
		JSONObject jsonFBAttachmentObject = new JSONObject();
		JSONObject jsonFBPayloadObject = new JSONObject();
		JSONArray jsonFBElementsArray = new JSONArray();
		try {

			JSONObject jsonFBObject = new JSONObject();
			JSONObject jsonFBButtonsObject = new JSONObject();
			jsonFBObject.put("contentType", "application/vnd.microsoft.card.hero");
			JSONObject jsonLINEContentObject = new JSONObject();

			jsonLINEContentObject.put("title", BUTTONS_TITLE);
			jsonLINEContentObject.put("image_url", imgUrl);

			JSONArray jsonFBButtonsArray = new JSONArray();
			
			jsonFBButtonsObject.put("title", "請點此觀看完整圖片");
			jsonFBButtonsObject.put("url", imgUrl);
			jsonFBButtonsObject.put("type", "web_url");
			
			jsonFBButtonsArray.put(jsonFBButtonsObject);
			jsonLINEContentObject.put("buttons", jsonFBButtonsArray);
			jsonFBElementsArray.put(jsonLINEContentObject);

			jsonFBPayloadObject.put("template_type", "generic");
			jsonFBPayloadObject.put("elements", jsonFBElementsArray);

			jsonFBAttachmentObject.put("payload", jsonFBPayloadObject);
			jsonFBAttachmentObject.put("type", "template");
			json.put("attachment", jsonFBAttachmentObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONObject convertReAskButtonsMessage(JSONObject jsonTemplate) {
		JSONObject json = new JSONObject();
		JSONObject jsonFBAttachmentObject = new JSONObject();
		JSONObject jsonFBPayloadObject = new JSONObject();
	
		try {
			String header = jsonTemplate.getString("text");

			JSONArray jsonLINEActionArray = jsonTemplate.getJSONArray("actions");
			JSONArray jsonFBButtonsArray = new JSONArray();
			for (int rr = 0; rr < jsonLINEActionArray.size(); rr++) {
				JSONObject jsonFBButtonsObject = new JSONObject();
				JSONObject jsonLINEActionObject = jsonLINEActionArray.getJSONObject(rr);
				String type = jsonLINEActionObject.get("type").toString();
				String label = jsonLINEActionObject.get("label").toString();

				if (label.equals(" ") || label.equals(" ") || label == null) {
					continue;
				}

				if (type.contains("message")) {
					if(label.equals("重新發問")){
						String texts = jsonLINEActionObject.get("text").toString();
						jsonFBButtonsObject.put("title", texts);
						jsonFBButtonsObject.put("payload", texts);
						jsonFBButtonsObject.put("type", "postback");
						header = header.replace("「重新發問」", "「" + texts + "」");
					} else {
						String texts = jsonLINEActionObject.get("text").toString();
						jsonFBButtonsObject.put("title", texts);
						jsonFBButtonsObject.put("payload", texts);
						jsonFBButtonsObject.put("type", "postback");
					}
				} else if (type.contains("uri")) {
					jsonFBButtonsObject.put("title", label);
					String uri = jsonLINEActionObject.get("uri").toString();
					jsonFBButtonsObject.put("url", uri);
					jsonFBButtonsObject.put("type", "web_url");
				}
				jsonFBButtonsArray.put(jsonFBButtonsObject);
			}

			jsonFBPayloadObject.put("template_type", "button");
			jsonFBPayloadObject.put("text", header);
			jsonFBPayloadObject.put("buttons", jsonFBButtonsArray);

			jsonFBAttachmentObject.put("payload", jsonFBPayloadObject);
			jsonFBAttachmentObject.put("type", "template");
			json.put("attachment", jsonFBAttachmentObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONObject convertButtonsMessage(JSONObject jsonTemplate) {
		JSONObject json = new JSONObject();
		JSONObject jsonFBAttachmentObject = new JSONObject();
		JSONObject jsonFBPayloadObject = new JSONObject();
		JSONArray jsonFBElementsArray = new JSONArray();
		try {

			JSONObject jsonFBObject = new JSONObject();
			jsonFBObject.put("contentType", "application/vnd.microsoft.card.hero");
			JSONObject jsonLINEContentObject = new JSONObject();

			jsonLINEContentObject.put("title", jsonTemplate.get("title"));
			jsonLINEContentObject.put("subtitle", jsonTemplate.get("text").toString());

			String imgUrl = findImageForFacebookSpec(jsonTemplate.getString("thumbnailImageUrl"));

			jsonLINEContentObject.put("image_url", imgUrl);

			JSONArray jsonLINEActionArray = jsonTemplate.getJSONArray("actions");
			JSONArray jsonFBButtonsArray = new JSONArray();
			int buttonCount = 0;
			for (int rr = 0; rr < jsonLINEActionArray.size(); rr++) {
				JSONObject jsonFBButtonsObject = new JSONObject();
				JSONObject jsonLINEActionObject = jsonLINEActionArray.getJSONObject(rr);
				String type = jsonLINEActionObject.get("type").toString();
				String label = jsonLINEActionObject.get("label").toString();

				if (label.equals(" ") || label.equals(" ") || label == null) {
					continue;
				}

				jsonFBButtonsObject.put("title", label);
				if (type.contains("message")) {
					String texts = jsonLINEActionObject.get("text").toString();
					jsonFBButtonsObject.put("payload", texts);
					jsonFBButtonsObject.put("type", "postback");
				} else if (type.contains("uri")) {
					String uri = jsonLINEActionObject.get("uri").toString();
					jsonFBButtonsObject.put("url", uri);
					jsonFBButtonsObject.put("type", "web_url");
				}

				jsonFBButtonsArray.put(jsonFBButtonsObject);
				// FB Buttons最多三個
				buttonCount++;
				if (buttonCount == 3) {
					break;
				}
			}
			jsonLINEContentObject.put("buttons", jsonFBButtonsArray);
			jsonFBElementsArray.put(jsonLINEContentObject);

			jsonFBPayloadObject.put("template_type", "generic");
			jsonFBPayloadObject.put("elements", jsonFBElementsArray);

			jsonFBAttachmentObject.put("payload", jsonFBPayloadObject);
			jsonFBAttachmentObject.put("type", "template");
			json.put("attachment", jsonFBAttachmentObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public static JSONObject convertCarouselMessage(JSONObject jsonTemplate) {
		JSONObject json = new JSONObject();
		JSONObject jsonFBAttachmentObject = new JSONObject();
		JSONObject jsonFBPayloadObject = new JSONObject();
		JSONArray jsonFBElementsArray = new JSONArray();
		try {
			JSONArray jsonLINEArray = jsonTemplate.getJSONArray("columns");

			for (int qq = 0; qq < jsonLINEArray.size(); qq++) {
				JSONObject jsonFBObject = new JSONObject();
				jsonFBObject.put("contentType", "application/vnd.microsoft.card.hero");
				JSONObject jsonLINEContentObject = new JSONObject();
				JSONObject jsonLINEObject = jsonLINEArray.getJSONObject(qq);

				jsonLINEContentObject.put("title", jsonLINEObject.get("title"));
				jsonLINEContentObject.put("subtitle", jsonLINEObject.get("text").toString());

				String imgUrl = findImageForFacebookSpec(jsonLINEObject.getString("thumbnailImageUrl"));
				
				//因艾斯還無法串FB，故FB問進來先不顯示消息面
				if(imgUrl.indexOf("NewsOriented") >= 0){
					continue;
				}
				jsonLINEContentObject.put("image_url", imgUrl);

				JSONArray jsonLINEActionArray = jsonLINEObject.getJSONArray("actions");
				JSONArray jsonFBButtonsArray = new JSONArray();
				int buttonCount = 0;
				for (int rr = 0; rr < jsonLINEActionArray.size(); rr++) {
					JSONObject jsonFBButtonsObject = new JSONObject();
					JSONObject jsonLINEActionObject = jsonLINEActionArray.getJSONObject(rr);
					String type = jsonLINEActionObject.get("type").toString();
					String label = jsonLINEActionObject.get("label").toString();

					if (label.equals(" ") || label.equals(" ") || label == null) {
						continue;
					}

					jsonFBButtonsObject.put("title", label);
					if (type.contains("message")) {
						String texts = jsonLINEActionObject.get("text").toString();
						jsonFBButtonsObject.put("payload", texts);
						jsonFBButtonsObject.put("type", "postback");
					} else if (type.contains("uri")) {
						String uri = jsonLINEActionObject.get("uri").toString();
						jsonFBButtonsObject.put("url", uri);
						jsonFBButtonsObject.put("type", "web_url");
					}
					
					jsonFBButtonsArray.put(jsonFBButtonsObject);
					//FB Buttons最多三個
					buttonCount ++;
					if(buttonCount == 3){
						break;
					}
				}
				jsonLINEContentObject.put("buttons", jsonFBButtonsArray);
				jsonFBElementsArray.put(jsonLINEContentObject);

				jsonFBPayloadObject.put("template_type", "generic");
				jsonFBPayloadObject.put("elements", jsonFBElementsArray);
			}
			jsonFBAttachmentObject.put("payload", jsonFBPayloadObject);
			jsonFBAttachmentObject.put("type", "template");
			json.put("attachment", jsonFBAttachmentObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		System.out.println(json);
		return json;
	}
	
	public static JSONObject convertOPTMessage(JSONArray options) {
		JSONObject json = new JSONObject();
		JSONObject attachs = new JSONObject();
		JSONObject payloadObj = new JSONObject();
		JSONArray buttonsArr = new JSONArray();

		try {
			String header = "請點擊選項，了解詳細內容";
	
			for (int m = 0, n = options.length(); m < n && m < 3; m++) {
				JSONObject option = options.getJSONObject(m);
				JSONObject buttonsObj = new JSONObject();
				try {
					buttonsObj.put("type", "web_url");
					buttonsObj.put("url", option.getString("LINK").trim());
					buttonsObj.put("title", option.getString("TEXT"));
					buttonsArr.put(buttonsObj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			payloadObj.put("template_type", "button");

			payloadObj.put("text", header);
			payloadObj.put("buttons", buttonsArr);

			attachs.put("type", "template");
			attachs.put("payload", payloadObj);

			json.put("attachment", attachs);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public static String findImageForFacebookSpec(String imgUrl) {
		if (StringUtils.trimToNull(imgUrl) == null)
			return imgUrl;
		
		String imgFn = "/commons" + StringUtils.substringAfterLast(imgUrl, "/commons");
		
		//System.out.println(imgUrl);
		//System.out.println(imgFn);
		if (!StringUtils.endsWith(StringUtils.substringBeforeLast(imgFn, "."), "_FB")) {
			imgFn = StringUtils.substringBeforeLast(imgFn, ".") + "_FB." + StringUtils.substringAfterLast(imgFn, ".");
			
			if (new File("../webapps/wise" + imgFn).exists()) {
				imgUrl = StringUtils.substringBeforeLast(imgUrl, "/commons") + imgFn;
			}
		}
		
		return imgUrl;
	}
}
