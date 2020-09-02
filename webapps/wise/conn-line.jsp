<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json" pageEncoding="UTF-8" 
import="com.intumit.solr.admin.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.util.*"
import="com.intumit.solr.robot.connector.line.*"
import="com.intumit.systemconfig.*"
import="com.intumit.android.search.util.TaiwanAddressNormalizeUtil"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.io.IOUtils"
import="org.apache.wink.json4j.*"
import="com.fasterxml.jackson.databind.*"

import="org.apache.http.impl.client.CloseableHttpClient"
import="org.apache.http.impl.client.HttpClientBuilder"
import="org.apache.http.client.methods.HttpPost"
import="org.apache.http.entity.StringEntity"
import="org.apache.http.HttpResponse"

import="org.apache.http.client.methods.HttpGet"
import="java.util.regex.Matcher"
import="java.util.regex.Pattern"

import="com.microsoft.ttshttpoxford.sample.*"
%>
<%!
/**
 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
 */
void replyText(String accessToken, String replyToken, String messages) {
	
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
 
JSONObject buildReplyTextMessage(String msg) {
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
void reply(String accessToken, String replyToken, JSONArray messages) {
 	
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
%>
<%!/**
 * @param accessToken 存在 Tenant.lineBotConfigJson 欄位當中，格式為 {"accessToken": "XXXXXXXXXXX" }
 */
String saveContent(String accessToken, String messageId) {
	
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
	    // handle exception here
		System.out.println(ex.toString());
		return null;
	} finally {
	    httpClient.getConnectionManager().shutdown();
	}
	return filename;
}
 
JSONObject getUserProfile(String accessToken, String uid) {
 	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
 	
	try {
		HttpGet request = new HttpGet("https://api.line.me/v2/bot/profile/" + uid);
        request.addHeader("content-type", "application/json");
        request.addHeader("Authorization", "Bearer " + accessToken);
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
             
             System.out.println("Line User Profile:" + resultData.toString());
             
             return new JSONObject(resultData.toString());
        }
	} catch (Exception ex) {
     	System.out.println(ex.toString());
	} finally {
         httpClient.getConnectionManager().shutdown();
	}
	
	return null;
}

/* 這個臨時的綁定機制應該要退休了 
void userEvaluation(String accessToken, String replyToken, String userId, String postbackData, QAContext qaCtx, UserClue clue){
	try {
		String message = "";
		if(clue.getLineUserId().equals(userId)){
			String[] postbackDatas = postbackData.split("_");
			EvaluationLogEntity log = EvaluationLogEntity.get(Integer.valueOf(postbackDatas[1]));
			log.setFlag(Integer.valueOf(postbackDatas[2]));
			EvaluationLogEntity.update(log);
			if(Integer.valueOf(postbackDatas[2]) == 2){
				message = qaCtx.getMessage("question.postback.good");
			} else {
				message = qaCtx.getMessage("question.postback.bad");
			}
		} else {
			message = "Different User!";
		}
		
		JSONObject m = new JSONObject();
		m.put("type", "text");
		m.put("text", message);
		reply(accessToken, replyToken, new JSONArray().put(m));
    } 
    catch (Exception e) {}
	
}
*/

JSONArray lineConfirmTemplate(String altText, String showText, JSONArray actions){
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
}%>
<%
String fromGateway = request.getParameter("fromGateway");

//validate signature
String signature = request.getHeader("X-Line-Signature");
if (fromGateway == null) {
	if (signature == null || signature.length() == 0) {
		System.out.println("Missing 'X-Line-Signature' header");
	    return;
	}
}

String json = IOUtils.toString(request.getInputStream(), "UTF-8");
System.out.println("got: " + json);

Tenant t = Tenant.getFromRequest(request, false);
String ch = request.getParameter("ch") != null ? request.getParameter("ch") : "line";

//這裡開始 check DemoFlow
DemoFlow df = DemoFlow.getInstance(t.getId());

// for gateway
JSONObject lineResp = new JSONObject();
JSONArray preMessages = new JSONArray();

JSONObject jsonData = new JSONObject(json);
ArrayList<Map<String, Object>> events = (ArrayList<Map<String, Object>>) jsonData.get("events");
if (events == null ) {
    System.out.println("Result shouldn't be null");
    return ;
}

JSONObject botCfg = new JSONObject(StringUtils.isNotEmpty(t.getLineBotConfigJson()) ? t.getLineBotConfigJson() : "{}");
JSONObject lineBotCfg = botCfg.has("line") ? botCfg.getJSONObject("line") : botCfg; // Hardcode for old json format, should be removed someday.
String accessToken = lineBotCfg.optString("accessToken");
String channelSecret = lineBotCfg.optString("channelSecret");
String baseUrl = lineBotCfg.optString("baseUrl", StringUtils.trimToEmpty(WiseSystemConfig.get().getHostname()) + request.getContextPath());
boolean enableUserBinding = lineBotCfg.optBoolean("enableUserBinding", false);
boolean enableAudioSTT = lineBotCfg.optBoolean("enableAudioSTT", false);
boolean enableAnswerTTS = lineBotCfg.optBoolean("enableAnswerTTS", false);

for (Map<String, Object> event : events) {
    String fromUser = null;
    String tmpQ = null;//StringUtils.lowerCase(StringUtils.trimToEmpty(req.optString("text")));

    Map<String, Object> sourceData = (Map<String, Object>) event.get("source");
    if (sourceData == null ) {
        System.out.println("sourceData shouldn't be null");
        continue;
    }
    String etCode = (String)event.get("type");
    boolean isMessage = (etCode != null && etCode.equals("message"));
    
	fromUser = (String) sourceData.get("userId");
    
    if (fromUser == null || t == null)
    	continue;
    
    if(fromGateway == null && !NaverLineAnswerTransformer.compareSignature(signature, json, channelSecret)){
    	System.out.println("not match 'X-Line-Signature' header with request 'body'");
        return;
    }
    
    String qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), ch), fromUser); //LINE_CTX_PREFIX + t.getId() + ":" + fromUser;

    boolean testMode = false;
    boolean noRecommand = false;

    QAContext qaCtx = QAContextManager.lookup(qaId);
    if (qaCtx == null) {
    	qaCtx = QAContextManager.create(qaId);
    }

	try {
		QAUtil.parseRequest(qaCtx, request, testMode, true);
	}
	catch (RobotException rbtEx) {
		out.println(rbtEx.getError().toString(2));
		return;
	}
	qaCtx.setCtxAttr("LINEID", fromUser);

	UserClue clue = qaCtx.getUserClue();
	if (clue == null) {
		clue = UserClue.getByLineUserId(t.getId(), fromUser);
		
		if (clue == null) {
			clue = new UserClue();
			clue.setTenantId(t.getId());
			clue.setLineUserId(fromUser);
			UserClue.saveOrUpdate(clue);
		}
		else {
			UserClue.saveOrUpdate(clue);	// 單純刷新 updateTime
		}
	}
	
	if (qaCtx.getCtxAttr("NICKNAME") == null || clue.getNickname() == null) {
		JSONObject profile = null;
		if (fromGateway == null) {
			profile = getUserProfile(accessToken, fromUser);
		} else {
			// 從Gateway來的
			Map<String, Object> profileData = (Map<String, Object>)event.get("profile");
			if (profileData != null) {
				profile = new JSONObject(profileData.toString());
			}
		}
		
		if (profile != null) {
			String displayName = profile.optString("displayName");
			
			if (displayName != null) {
				qaCtx.setCtxAttr("NICKNAME", displayName);
				
				if (!StringUtils.equals(displayName, clue.getNickname())) {
					clue.setTenantId(t.getId());		// 解決版本差異
					clue.setNickname(displayName);
					UserClue.saveOrUpdate(clue);
				}
			}
		}
	}

	qaCtx.setUserClue(clue);
	qaCtx.setQaChannel(ch);
	qaCtx.setEservice(ch);
	qaCtx.setClientSupportHtml(false); // 透過 apikey 的預設 html=false，若有傳遞 html 後面會再覆蓋此值
	EventType et = EventType.matchBestEventType(t.getId(), qaCtx.getQaChannel(), etCode);
	if (et == null) {
		System.out.println("Unknown EventType [" + etCode + "]...");
	}
	qaCtx.setEventType(et);
    
	/* 這個臨時的綁定機制應該要退休了 
    if (enableUserBinding) {
    	UserClue clue = UserClue.getByLineUserId(t.getId(), fromUser);
    		
    	if (clue == null) {
    		// 寫死的一個登入訊息
	        reply(accessToken, (String) event.get("replyToken"), 
	        	new JSONArray("[{ \"type\": \"template\", \"altText\": \"需登入才能繼續對話\", \"template\": { \"type\": \"confirm\", \"text\": \"本帳號尚未進行綁定，需要綁定才能繼續對話，是否要進行綁定?\", \"actions\": [ "
	        	+ "{ \"type\": \"uri\", \"label\": \"好，我要綁定\", \"uri\": \"" + WiseSystemConfig.get().getHostname() + "/wise/bind-adm-user.jsp?from=line&tid=" + t.getId() + "&userId=" + fromUser + "\" }, " 
	        	+ "{ \"type\": \"message\", \"label\": \"不要\", \"text\": \"我會想念你的\" } ] } } ]"));
	        	
	        return ;
    	} else { 
    		JSONObject lineUserProfile = new JSONObject();
    		lineUserProfile.put("lineUserId", fromUser);
    		lineUserProfile.put("adminUserId", clue.getAdminUserId());
    		qaCtx.setLineUserProfile(lineUserProfile);
    			
        	if ((etCode != null && etCode.equals("postback"))) {
	        	String postbackData = new JSONObject(event.get("postback")).get("data").toString(); 
	        	if(postbackData.startsWith("EVALUATION_") && postbackData.split("_").length == 3){
	        	    userEvaluation(accessToken, (String) event.get("replyToken"), fromUser, postbackData, qaCtx, clue);
	        	}
	        	continue;
        	}
    	}
    }*/

	boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
	
    JSONObject resp = null;
	JSONArray messages = new JSONArray();
	Map<String, Object> eventData = null;
	
	if (etCode != null && event.containsKey(etCode)) {
		eventData = (Map<String, Object>) event.get(etCode);
		
		if (eventData != null) {
			// eventData 有可能在 EventTypeRule 被使用。也會被放在 ServiceLogEntity 當中
			qaCtx.setRequestAttribute("eventData", eventData);
		}
	}
    
    if (isMessage) {
        if (eventData == null) {
            System.out.println("messageData shouldn't be null");
            continue;
        }
    	
    	String theQ = null;

     	if ( eventData.get("type").toString().equals("text") ) {
    	    theQ = StringUtils.lowerCase(StringUtils.trimToEmpty( eventData.get("text").toString() ));
     	}
	   	else if (enableAudioSTT && eventData.get("type").toString().equals("audio") && fromGateway == null) {
	        
	    	try {
	            String path = saveContent(accessToken, eventData.get("id").toString());
	    	
	            if (path == null ) {
	                System.out.println("saveContent fail");
	                continue;
	            }
	            
	            String wavFn = WiSeEnv.getHomePath() + "/cache/line_audio/" + StringUtils.substringBeforeLast(path, ".") + ".wav";
	            // 執行 m4a to wav 工作，這裡是透過 Linux 的 faad2 函式來轉檔
	            String[] cmd = { WiSeEnv.getHomePath() + "/utils/convertM4a2Wav.sh", WiSeEnv.getHomePath() + "/cache/line_audio/" + path, wavFn };
	            Process p = Runtime.getRuntime().exec(cmd);
	            
	            String line = "";
	            BufferedReader p_in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            while((line = p_in.readLine()) != null){
	                System.out.println("**** Converting Line Audio **** "+line);
	            }
	            p_in.close();
	            
	            String recognized = STTService.getBestResult(STTService.ToByteArray(wavFn), "json", 8000, "zh-TW");
	            
	            if (StringUtils.isNotEmpty(recognized)) {
	            	theQ = recognized;
	            		
	    		    // 測試功能
	    		    if (qaCtx.getTenant().getEnableDebug())
		        		messages.add(0, buildReplyTextMessage("[DEBUG] 語音辨識結果：" + StringUtils.replaceChars(recognized, " ", "")));
	            	}
	    		}
		    catch (Exception e) {
		    	e.printStackTrace();  
		    }
	    }
	    else if (eventData.get("type").toString().equals("location") ) {
	    	String addr = eventData.get("address").toString();
	    	theQ = addr;
	    }
    		
    	// 有 Q 就跑答案
	    if ( theQ != null ) {
	        tmpQ = theQ;
	
			if (explainQA) {
				qaCtx.appendExplain("Enter QA-AJAX", "Got Question from LINE [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
				+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")" + (fromUser != null ? "FROM (" + fromUser + ")" : ""));
			}
	        
	        String suggestQ = qaCtx.getTenant().getEnablePhoneticHomonym() 
	        	? KnowledgePointDictionary.suggestFullQuestion(qaCtx.getTenant().getId(), tmpQ)
	        	: null;
	
	        qaCtx.setOriginalQuestion(tmpQ);
	        qaCtx.setCurrentQuestion(suggestQ != null ? suggestQ : tmpQ);
	        
	        if (t.getEnableDebug() && df.isEnable() && df.isOverwriteMode()) {
		        if (df.getCurrentOffset() != -1 && df.getCurrentOffset() < df.getQuestions().size()) {
		        	String dfQ = df.getQuestions().get(df.getCurrentOffset());
		        		
		        	if (df.isAutopilotMode()) {
		        		df.setCurrentOffset(df.getCurrentOffset() + 1);
		        	}
		        		
		        	if (df.isSelfRepair()) { // 目前暫時拿來作為覆寫模式每次覆寫完就停止覆寫的啟用或關閉
		        		df.setOverwriteMode(false);
		        	}
		        	qaCtx.setOriginalQuestion(dfQ);
		        	qaCtx.setCurrentQuestion(dfQ);
		        }
	        }

			if (qaCtx.getCurrentQuestion() != null) {
		        QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
		        c.check(qaCtx);
		        resp = qaCtx.genResponseJSON();

		        // line.messages 拿來全用
		        if (resp.containsKey("line")) {
		        	JSONObject lo = resp.optJSONObject("line", new JSONObject());
		        	if (lo.has("messages")) {
						messages.addAll(lo.optJSONArray("messages", new JSONArray()));
		        	}
		        }
		        else if (resp.has("messages")) {
		        	// 理論上應該不會來到這裡
					messages.addAll(resp.optJSONArray("messages", new JSONArray()));

			        String forLine = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
		        	if (StringUtils.trimToNull(forLine) != null) {
		        		messages.add(0, buildReplyTextMessage(forLine));
		        	}
		        }
			}
	    }
	    
	    // 最後再來附加 TTS
	    if (enableAudioSTT && enableAnswerTTS && "audio".equals((String)eventData.get("type"))) {
	    	try {
	    		if (enableAnswerTTS) {
		    		String forTTS = com.intumit.solr.servlet.TextToSpeechServlet.ttsClean(resp.optString("output"), false, false);

		    	    if (StringUtils.isNotEmpty(forTTS)) {
		    	        String[] ttsSetting = TTSService.getMicrosoftTtsSetting(t.getLocale());
						String outputFormat = AudioOutputFormat.Riff16Khz16BitMonoPcm;
				        String deviceLanguage = ttsSetting != null ? ttsSetting[0] : "zh-TW";
				        String genderName = ttsSetting != null ? ttsSetting[1] : Gender.Female;
				        String voiceName = ttsSetting != null ? ttsSetting[2] : "Microsoft Server Speech Text to Speech Voice (zh-TW, Yating, Apollo)";
						byte[] ttsBytes = TTSService.Synthesize(forTTS, outputFormat, deviceLanguage, genderName, voiceName);
						String FS = File.separator;
						String baseDir = WiSeEnv.getHomePath() + FS + ".." +  FS + "webapps" + FS + "wise" + FS + "commons" + FS;
						String key = TTSService.getCacheFileName(forTTS, deviceLanguage, genderName);
						
						if (ttsBytes != null && ttsBytes.length > 0) {
							try {
					            String ttsWavFn = baseDir + key + ".wav";
					            String ttsW4aFn = baseDir + key + ".m4a";
					            
					            if (!new File(ttsW4aFn).exists()) {
						            // 執行 m4a to wav 工作，這裡是透過 Linux 的 faad2 函式來轉檔
						            String[] cmd2 = { WiSeEnv.getHomePath() + "/utils/convertWav2M4a.sh", ttsWavFn, ttsW4aFn };
						            
						            System.out.println("Exec:" + Arrays.asList(cmd2));
						            Process p2 = Runtime.getRuntime().exec(cmd2);
						            
						            String line = null;
						            BufferedReader p2_in = new BufferedReader(new InputStreamReader(p2.getInputStream()));
						            while((line = p2_in.readLine()) != null){
						                System.out.println("**** Converting Output Line Audio **** "+line);
						            }
						            p2_in.close();
					            }

					            if (new File(ttsW4aFn).exists()) {
									JSONObject audioMsg = new JSONObject();
									audioMsg.put("type", "audio");
									audioMsg.put("originalContentUrl", baseUrl + "/commons/" + key + ".m4a");
									audioMsg.put("duration", 10000);
									
									System.out.println("TTS結果檔：" + audioMsg);
									
									messages.add(audioMsg);
					            }
							}
							catch (Exception ttsEx) {
								ttsEx.printStackTrace();
							}
						}
		    	    }
	    		}
	        } catch (Exception e) {
	        		e.printStackTrace();  
	        }
	    }
    }
    else {
        QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
        c.check(qaCtx);
        resp = qaCtx.genResponseJSON();

        if (resp.containsKey("line")) {
        	JSONObject lo = resp.optJSONObject("line", new JSONObject());
        	if (lo.has("messages")) {
				messages.addAll(lo.optJSONArray("messages", new JSONArray()));
        	}
        }
        else if (resp.has("messages")) {
        	// 理論上應該不會來到這裡
			messages.addAll(resp.optJSONArray("messages", new JSONArray()));

	        String forLine = StringUtils.trimToEmpty(resp.optString("output")).replaceAll("<[^>]+>", " ");
        	if (StringUtils.trimToNull(forLine) != null) {
        		messages.add(0, buildReplyTextMessage(forLine));
        	}
        }
	}
    
    if (messages.size() > 0) {
    	if (fromGateway == null) {
			reply(accessToken, (String) event.get("replyToken"), messages);
    	} else {
    		
    		JSONObject line = new JSONObject();
    		line.put("messages", messages);
    		line.put("to", fromUser);
    		
    		JSONObject msg = new JSONObject();
    		msg.put("line", line);
    		
       		if (qaCtx.getResponseAttribute("attentionKeywords") != null) {
       			msg.put("attentionKeywords", qaCtx.getResponseAttribute("attentionKeywords"));
       		}
       		
       		if (qaCtx.getResponseAttribute("output") != null) {
       			msg.put("output", qaCtx.getResponseAttribute("output"));
       		}
       		
       		preMessages.put(msg);
    	}
	}
    
    if (resp != null) {
    	JSONObject conversation = qaCtx.getConversationsJson();
	    conversation.getJSONArray("messages").put(resp);
	    qaCtx.setConversations(conversation.toString());
	    	
	    //System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
	    QAContextManager.put(qaId, qaCtx);
	    	
	    // DO LOG
	    ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);
	    	
	    if (testMode) {
	    	log = (ServiceLogEntity) session.getAttribute("TestModeServiceLogEntity");
	    	session.setAttribute("TestModeServiceLogEntity", log);
	    	//System.out.println(qaCtx.getExplain());
	    	//System.out.println("TestModeServiceLogEntity: " + log.getConversations());
	    }
	    else {
	    	if (log == null) {
			    log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", fromUser,
			    ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
			    ServiceLogEntity.setIntoSession(qaId, log);
	    	}
	
	    	log.setConversations(conversation.toString(2));
	    	log.setChannel(qaCtx.getQaChannel());
			log.setLastMessage(qaCtx.getOriginalQuestion());
	    	// 有限制業務類別就存業務類別，沒有就看看知識主題有沒有業務類別，有的話存第一個
	    	if (qaCtx.getRestrictToQaCategory() != null) {
	    		log.setLastQaCategory(qaCtx.getRestrictToQaCategory());
	    	}
	    	else if (qaCtx.getCurrentQA() != null && qaCtx.getCurrentQA().getQaCategory() != null) {
	    		log.setLastQaCategory(qaCtx.getCurrentQA().getQaCategory());
	    	}
	    	else if (qaCtx.getCurrentKPs() != null && qaCtx.getCurrentKPs().length > 0) {
	    		for (DictionaryDatabase dd: qaCtx.getCurrentKPs()) {
	    			if (StringUtils.isNotEmpty(dd.getCategory())) {
	    				log.setLastQaCategory(dd.getCategory());
	    				break;
	    			}
	    		}
	    	}
	    	else if (qaCtx.getLastKPs() != null && qaCtx.getLastKPs().length > 0) {
	    		for (DictionaryDatabase dd: qaCtx.getLastKPs()) {
	    			if (StringUtils.isNotEmpty(dd.getCategory())) {
	    				log.setLastQaCategory(dd.getCategory());
	    				break;
	    			}
	    		}
	    	}
	    	log.setStatMsgCountFromUser(log.getStatMsgCountFromUser() + 1);
	    	log.setStatMsgCountFromRobot(log.getStatMsgCountFromRobot() + 1);
	    	log.setStatMsgCountTotal(log.getStatMsgCountTotal() + 2);
	    		
	    	if (qaCtx.getAnswerType() == QAContext.ANSWER_TYPE.FORWARD) {
	    		log.setStatForward(log.getStatForward() | 1);
	    	}
	    		
	    	if (AttentionKeywordRule.hasNegativeKeywords(qaCtx)) {
	    		log.setStatForward(log.getStatForward() | 2);
	    	}
	    		
	    	if (qaCtx.getAnswerType() == QAContext.ANSWER_TYPE.NO_ANSWER) {
	    		log.setStatMsgCountNoAnswer(log.getStatMsgCountNoAnswer() + 1);
	    	}
	    	else {
	    		log.setStatMsgCountHasAnswer(log.getStatMsgCountHasAnswer() + 1);
	    	}
	
	    	ServiceLogEntity.save(log);
	    		
	    	if (qaCtx.getTenant() != null && qaCtx.getLineUserProfile() != null && fromGateway == null) {
		    	EvaluationLogEntity evaLog = EvaluationLogEntity.log(request, QAUtil.CURRENT_EVALUATION_LOG_NS, resp.toString(), qaCtx);
		    		
		    	JSONArray actions = new JSONArray();
		    	JSONObject action = null;
		    	action = new JSONObject().put("type", "postback").put("label", qaCtx.getMessage("question.feedback.good")).put("data", "EVALUATION_"+evaLog.getId()+"_2");
		    	actions.add(action);
		    	action = new JSONObject().put("type", "uri").put("label", qaCtx.getMessage("question.feedback.bad")).put("uri", WiseSystemConfig.get().getHostname() + "/wise/qa-eval-log-line.jsp?from=line&logId="+evaLog.getId()+"&flag=5&qaId="+qaCtx.getContextId());
		    	actions.add(action);
		    		
		    	NaverLineAnswerTransformer.push(accessToken, fromUser, lineConfirmTemplate("測試紀錄回饋按鈕", qaCtx.getMessage("question.feedback"), actions));
	    	}
	    }
    }
}

lineResp.put("response", preMessages);

%><%= (fromGateway == null ? "{}" : lineResp.toString(4)) %>