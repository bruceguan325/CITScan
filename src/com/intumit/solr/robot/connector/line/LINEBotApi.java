package com.intumit.solr.robot.connector.line;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.intumit.solr.robot.EvaluationLogEntity;
import com.intumit.solr.robot.EvaluationLogEntityUpdateLog;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapAction;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;

public class LINEBotApi {
	@SuppressWarnings("unused")
	private static String welcomeMessageKey = "NEW_USER_FOLLOW";
	
	public static String getAccessToken(Integer tenantId) {
		try {
			JSONObject botCfg = new JSONObject(Tenant.get(tenantId).getLineBotConfigJson());
			JSONObject lineBotCfg = botCfg.has("line") ? botCfg.getJSONObject("line") : botCfg;
			String accessToken = lineBotCfg.getString("accessToken");
			return accessToken;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static JSONObject buildTextMessage(String msg) {
		JSONObject msgObj = new JSONObject();
		try {
			msgObj.put("type", "text");
			msgObj.put("text", msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return msgObj;
	}

	public static JSONObject POSTRequest(CloseableHttpClient httpClient, HttpPost request, String accessToken,
			JSONObject json) throws Exception {
		StringEntity params = new StringEntity(json.toString());
		request.addHeader("content-type", "application/json");
		request.addHeader("Authorization", "Bearer " + accessToken);
		request.setEntity(params);
		HttpResponse response = httpClient.execute(request);

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			System.out.println("StatusCode : " + statusCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String inputLine;
			StringBuffer resultData = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				resultData.append(inputLine);
			}
			in.close();

			System.out.println(resultData.toString());
			if(!resultData.toString().equals("{}")){
				JSONObject result = new JSONObject(resultData.toString());
				return result;
			} else {
				JSONObject result = new JSONObject("{ \"StatusCode\" : " + statusCode + "}");
				return result;
			}
		}

		return null;
	}
	
	public static JSONObject GETRequest(CloseableHttpClient httpClient, HttpGet request, String accessToken)
			throws Exception {
		request.addHeader("Authorization", "Bearer " + accessToken);
		HttpResponse response = httpClient.execute(request);

		if (response != null) {
			System.out.println("StatusCode : " + response.getStatusLine().getStatusCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			String inputLine;
			StringBuffer resultData = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				resultData.append(inputLine);
			}
			in.close();

			JSONObject result = new JSONObject(resultData.toString());

			// if (profile.containsKey("displayName"))
			// qaCtx.setLineUserProfile(profile);

			// System.out.println(profile.toString());
			return result;
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject push(String accessToken, String userId, JSONArray messages) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			JSONObject json = new JSONObject();
			json.put("to", userId);
			json.put("messages", messages);

			HttpPost request = new HttpPost("https://api.line.me/v2/bot/message/push");
			result = POSTRequest(httpClient, request, accessToken, json);
		} catch (Exception ex) {
			// handle exception here
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject multicast(String accessToken, String[] userIds, JSONArray messages) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			JSONObject json = new JSONObject();
			json.put("to", userIds);
			json.put("messages", messages);

			HttpPost request = new HttpPost("https://api.line.me/v2/bot/message/multicast");
			result = POSTRequest(httpClient, request, accessToken, json);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject profile(String accessToken, String userId) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			HttpGet request = new HttpGet("https://api.line.me/v2/bot/profile/" + userId);
			result = GETRequest(httpClient, request, accessToken);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject content(String accessToken, String messageId) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JSONObject result = null;
		try {
			HttpGet request = new HttpGet("https://api.line.me/v2/bot/message/" + messageId + "/content");
			result = GETRequest(httpClient, request, accessToken);
		} catch (Exception e) {

		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return result;
	}

	public static String imagemap(String link, int width, int height) {
		List<ImagemapAction> actions = new ArrayList<ImagemapAction>();
		ImagemapMessage im = new ImagemapMessage(link, "imagemap", new ImagemapBaseSize(height, width), actions);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonString = "";
		try {
			jsonString = ow.writeValueAsString(im);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return jsonString;
	}

	public static String imagemap(String link, int width, int height, String menuHeader) {
		List<ImagemapAction> actions = new ArrayList<ImagemapAction>();
		ImagemapMessage im = new ImagemapMessage(link, menuHeader, new ImagemapBaseSize(height, width), actions);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonString = "";
		try {
			jsonString = ow.writeValueAsString(im);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return jsonString;
	}
	
	public static JSONArray convertLink2Image(String output) {
		JSONArray jsonArray = null;
		try {
			jsonArray = new JSONArray();
			String[] dataList = output.split("@");
			
			for (int i = 0; i < dataList.length; i++) {
				String dataStr = dataList[i].trim();
				if (dataStr.indexOf("TEXT:") >= 0) {
					String textStr = dataStr.replace("TEXT:", "");
					jsonArray.add(i, buildReplyTextMessage(textStr));
				} else if (dataStr.indexOf("IMG:") >= 0) {
					String IMGUrl = dataStr.replace("IMG:", "").replace(" ", "");
					if (output.indexOf("URL:") >= 0){
						String IMGLink = dataList[i+1].replace("URL:", "");
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(convertImagemap(IMGUrl.trim(), 1040, height, IMGLink.trim())));
					} else{
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(imagemap(IMGUrl.trim(), 1040, height)));
					}
				} else if (dataStr.indexOf("OPT:") >= 0) {
					String optionStr = dataStr.replace("OPT:", "");
					jsonArray.add(new JSONObject(convertTemplateMessage(new JSONArray(optionStr))));
                } else if (dataStr.indexOf("MA:") >= 0) {
                    String optionStr = dataStr.replace("MA:", "");
                    jsonArray.add(new JSONObject(convertTemplateMessageForMessageAction(new JSONArray(optionStr))));
                } else if (dataStr.indexOf("Template:") >= 0) {
                    String optionStr = dataStr.replace("Template:", "");
                    jsonArray.add(new JSONObject(optionStr));
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArray;
	}

	public static JSONArray convertLink2ImageAndLineID(String output, String fromUser) {
		JSONArray jsonArray = null;
		try {
			jsonArray = new JSONArray();
			String[] dataList = output.split("@");
			
			for (int i = 0; i < dataList.length; i++) {
				String dataStr = dataList[i].trim();
				if (dataStr.indexOf("TEXT:") >= 0) {
					String textStr = dataStr.replace("TEXT:", "");
					jsonArray.add(i, buildReplyTextMessage(textStr));
				} else if (dataStr.indexOf("IMG:") >= 0) {
					String IMGUrl = dataStr.replace("IMG:", "").replace(" ", "");
					if (output.indexOf("URL:") >= 0){
						String IMGLink = dataList[i+1].replace("URL:", "");
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(convertImagemap(IMGUrl.trim(), 1040, height, IMGLink.trim())));
					} else{
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(imagemap(IMGUrl.trim(), 1040, height)));
					}
				} else if (dataStr.indexOf("OPT:") >= 0) {
					String optionStr = dataStr.replace("OPT:", "");
					jsonArray.add(new JSONObject(convertTemplateMessage(new JSONArray(optionStr))));
                } else if (dataStr.indexOf("MA:") >= 0) {
                    String optionStr = dataStr.replace("MA:", "");
                    jsonArray.add(new JSONObject(convertTemplateMessageForMessageAction(new JSONArray(optionStr))));
                } else if (dataStr.indexOf("Template:") >= 0) {
                    String optionStr = dataStr.replace("Template:", "").replace("{LINEID}", fromUser);
                    jsonArray.add(new JSONObject(optionStr));
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArray;
	}
	
	public static JSONArray convertLink2Image(String output, String menuHeader) {
		JSONArray jsonArray = null;
		try {
			jsonArray = new JSONArray();
			String[] dataList = output.split("@");
			
			for (int i = 0; i < dataList.length; i++) {
				String dataStr = dataList[i].trim();
				if (dataStr.indexOf("TEXT:") >= 0) {
					String textStr = dataStr.replace("TEXT:", "");
					jsonArray.add(i, buildReplyTextMessage(textStr));
				} else if (dataStr.indexOf("IMG:") >= 0) {
					String IMGUrl = dataStr.replace("IMG:", "").replace(" ", "");
					if (output.indexOf("URL:") >= 0){
						String IMGLink = dataList[i+1].replace("URL:", "");
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(convertImagemap(IMGUrl.trim(), 1040, height, IMGLink.trim(), menuHeader)));
					} else{
						int height = Integer.parseInt(dataStr.substring(dataStr.lastIndexOf("-") + 1, dataStr.lastIndexOf(".")));
						jsonArray.add(i, new JSONObject(imagemap(IMGUrl.trim(), 1040, height, menuHeader)));
					}
				} else if (dataStr.indexOf("OPT:") >= 0) {
					String optionStr = dataStr.replace("OPT:", "");
					jsonArray.add(new JSONObject(convertTemplateMessage(new JSONArray(optionStr))));
                } else if (dataStr.indexOf("MA:") >= 0) {
                    String optionStr = dataStr.replace("MA:", "");
                    jsonArray.add(new JSONObject(convertTemplateMessageForMessageAction(new JSONArray(optionStr))));
                }

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArray;
	}
	
	public static JSONArray convertLink2ImageAndText(String output, String url) {
		JSONArray jsonArray = null;
		try {
			jsonArray = new JSONArray();
			String[] images = output.split(":IMG:");
			for (int i = 1, j = images.length; i < j; i++) {
				String image = images[i].trim();
				int height = Integer.parseInt(image.substring(image.lastIndexOf("-") + 1, image.lastIndexOf(".")));
				jsonArray.add(i - 1, buildReplyTextMessage("------"));
				jsonArray.add(i, new JSONObject(convertImagemap(images[i].trim(), 1040, height, url)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArray;
	}

	public static String convertImagemap(String link, int width, int height, String url) {

		List<ImagemapAction> actions = new ArrayList<ImagemapAction>();
		if (StringUtils.isNotBlank(url)) {
			actions.add(new URIImagemapAction(url, new ImagemapArea(0, 0, width, height)));
		}
		ImagemapMessage im = new ImagemapMessage(link, "imagemap", new ImagemapBaseSize(height, width), actions);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonString = "";
		try {
			jsonString = ow.writeValueAsString(im);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonString;
	}
	
	public static String convertImagemap(String link, int width, int height, String url, String menuHeader) {

		List<ImagemapAction> actions = new ArrayList<ImagemapAction>();
		if (StringUtils.isNotBlank(url)) {
			actions.add(new URIImagemapAction(url, new ImagemapArea(0, 0, width, height)));
		}
		ImagemapMessage im = new ImagemapMessage(link, menuHeader, new ImagemapBaseSize(height, width), actions);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonString = "";
		try {
			jsonString = ow.writeValueAsString(im);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonString;
	}
	
	public static String convertTemplateMessageForMessageAction(JSONArray options) {
		String header = "請點擊選項，獲得更多詳細內容";
		List<Action> buttonsActions = new ArrayList<Action>();
		
		for (int m = 0, n = options.length(); m < n; m++) {
			JSONObject option;
			try {
				option = options.getJSONObject(m);
				buttonsActions.add(new MessageAction(option.getString("TEXT"), option.getString("PUSHTEXT").trim()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		ButtonsTemplate template = new ButtonsTemplate(null, null, header, buttonsActions);
		TemplateMessage message = new TemplateMessage(header, template);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String result = "";
		try {
			result = ow.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static String convertTemplateMessage(JSONArray options) {
		String header = "請點擊選項，了解詳細內容";
		List<Action> buttonsActions = new ArrayList<Action>();
		
		for (int m = 0, n = options.length(); m < n; m++) {
			JSONObject option;
			try {
				option = options.getJSONObject(m);
				buttonsActions.add(new URIAction(option.getString("TEXT"), option.getString("LINK").trim()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		ButtonsTemplate template = new ButtonsTemplate(null, null, header, buttonsActions);
		TemplateMessage message = new TemplateMessage(header, template);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String result = "";
		try {
			result = ow.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static JSONObject buildReplyTextMessage(String msg) {
		JSONObject msgObj = new JSONObject();
		try {
			msgObj.put("type", "text");
			msgObj.put("text", msg);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return msgObj;
	}

	/**
	 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
	 */
	@SuppressWarnings("deprecation")
	public static String saveContent(String accessToken, String messageId) {
		
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		String filename = null;
		try {
			
			HttpGet httpGet  = new HttpGet("https://api.line.me/v2/bot/message/"+messageId+"/content");
			httpGet.addHeader("Authorization", "Bearer " + accessToken);
		    HttpResponse httpResponse = httpClient.execute(httpGet);
		    
		    if (httpResponse != null) {
			    	System.out.println( "StatusCode : "+httpResponse.getStatusLine().getStatusCode());
			    	System.out.println("All Headers: " + Arrays.asList(httpResponse.getAllHeaders()));
				filename = System.currentTimeMillis() + messageId;
		    		Pattern templatePattern = Pattern.compile("=\"(.*)\"");
		        Matcher matcher = templatePattern.matcher("Content_Disposition");
				while (matcher.find()) {
					//System.out.println("aaaaaaaaaaaaaaaaaaaaaa");
					filename = matcher.group(1);
					System.out.println("name :" + filename);
				}
				if (filename == null) {
					return null;
				}
			    	InputStream is = httpResponse.getEntity().getContent();
			    	System.out.println("path :" + WiSeEnv.getHomePath()+ "/cache/line_audio/");
		        FileOutputStream fos = new FileOutputStream(new File(WiSeEnv.getHomePath()+ "/cache/line_audio/", filename));

		        int read = 0;
		        byte[] buffer = new byte[32768];
		        while( (read = is.read(buffer)) > 0) {
		        		fos.write(buffer, 0, read);
				}
		
		        fos.close();
		        is.close();
		    }
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {
		    httpClient.getConnectionManager().shutdown();
		}
		return filename;
	}
	
	/**
	 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void replyText(String accessToken, String replyToken, String messages) {
		
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try {
	    	JSONArray messagesJsArray = new JSONArray();
	    	messagesJsArray.add(buildReplyTextMessage(messages));
	    	Map<String, Object> lineData = new HashMap<String, Object>();
	    	lineData.put( "replyToken", replyToken );
	    	lineData.put( "messages", messagesJsArray );
	    	JSONObject json = new JSONObject();
	    	json.putAll( lineData );
	    	
	    	HttpPost request = new HttpPost("https://api.line.me/v2/bot/message/reply");
	        StringEntity params = new StringEntity(json.toString());
	        request.addHeader("content-type", "application/json");
	        request.addHeader("Authorization", "Bearer " + accessToken);
	        request.setEntity(params);
	        HttpResponse response = httpClient.execute(request);
	        
	        if (response != null) {
	        	System.out.println( "StatusCode : "+response.getStatusLine().getStatusCode());
	        	
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
	 
	/**
	 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
	 */
	@SuppressWarnings("deprecation")
	public static void reply(String accessToken, String replyToken, JSONArray messages) {
	 	
	 	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try {
	     	JSONObject json = new JSONObject();
	     	json.put( "replyToken", replyToken );
	     	json.put( "messages", messages );
	     	
			HttpPost request = new HttpPost("https://api.line.me/v2/bot/message/reply");
	        StringEntity params = new StringEntity(json.toString());
	        request.addHeader("content-type", "application/json");
	        request.addHeader("Authorization", "Bearer " + accessToken);
	        request.setEntity(params);
	        HttpResponse response = httpClient.execute(request);
	         
	        if (response != null) {
	         	System.out.println( "StatusCode : "+response.getStatusLine().getStatusCode());
	         	
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
	
	public static JSONArray lineConfirmTemplate(String altText, String showText, JSONArray actions){
		JSONArray confirmTemplate = new JSONArray();
		try {
			JSONObject template = new JSONObject();
			template.put("type", "confirm");
			template.put("text", showText);
			template.put("actions", actions);
			JSONObject confirm = new JSONObject();
			confirm.put("type", "template");
			confirm.put("altText", altText);
			confirm.put("template", template);
			confirmTemplate.add(confirm);
			System.out.println(confirmTemplate);
	    } 
	    catch (Exception e) {}
		return confirmTemplate;
	}
	
}