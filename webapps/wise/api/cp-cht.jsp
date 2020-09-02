<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
import="com.intumit.hithot.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.servlet.FakeHttpRequest"
	import="com.intumit.solr.util.*"
	import="java.io.*"
	import="java.util.*"
	import="java.lang.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.io.IOUtils"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
	import="com.microsoft.ttshttpoxford.sample.*"
	import="com.intumit.systemconfig.*"
	import="org.apache.solr.common.*"
	import="com.intumit.solr.robot.QAUtil.FormalAnswerReplacer"
	import="com.intumit.solr.robot.dictionary.KnowledgePointDictionary"
	import="com.intumit.solr.robot.connector.line.LINEBotApi"
%><%!
/* Sample Request
{
	"serviceId":"C003",
	"action":"command", "time":"2017-08-28T12:00:00Z", "requestId":"REQ201708280002", "accountName":"{CP 服務帳號}", "accessToken":"{CP 服務的 AccessToken}", 
	"commands":[
		{
		    "sequence":"1",
		    "type":"05",
		    "subType":"0013",
		    "contents":
		    {
		       "key":"value"
			} 
		}
	]
}
*/
%><%

boolean testMode = false;
if (request.getParameter("testMode") != null) {
	if (Boolean.parseBoolean(request.getParameter("testMode")))
	{
		testMode = true;
	}
}

String jsonStr = IOUtils.toString(request.getInputStream(), "UTF-8");
System.out.println("got CHT request: " + jsonStr);

JSONObject json = new JSONObject(jsonStr);
String ch = StringUtils.defaultString(request.getParameter("ch"), QAChannel.DEFAULT_CHANNEL_CODE);

// 語意雲目前沒有訂 user 規格，所以就直接把 session 當 user
String requestId = json.optString("requestId");
String action = json.optString("action", "command");

QAChannel channel = null;
boolean isLINEChannel = false;
JSONArray messages = null;
Tenant t = Tenant.getFromRequest(request, testMode);
//======================= 

if (t != null) {
	channel = QAChannel.get(t.getId(), ch);
	if (channel != null && channel.getType() == QAChannelType.LINE) {
		isLINEChannel = true;
	}
}

String qaId = UUID.randomUUID().toString();
if (StringUtils.isBlank(qaId)) {
	qaId = QAContextManager.generateQaId(t, QAChannel.get(t.getId(), ch), null); //LINE_CTX_PREFIX + t.getId() + ":" + fromUser;
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	qaCtx = QAContextManager.create(qaId);
}
JSONObject newResp = new JSONObject();
////////
Calendar cal = Calendar.getInstance();
cal.setTimeInMillis(qaCtx.getAccessTimestamp());
String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(cal.getTime());
newResp.put("action", action);
newResp.put("time", dateStr);
newResp.put("requestId", requestId);

try {
	JSONObject cmd = json.optJSONArray("commands", new JSONArray().put(new JSONObject().put("contents", new JSONObject()))).getJSONObject(0);

	String cmdType = cmd.optString("type");
	String cmdSubType = cmd.optString("subType");
	Long kid = new Long(cmdSubType);
	QAUtil qu = QAUtil.getInstance(t);
	String q = qu.getStandardQ(kid);
	
	FakeHttpRequest fakeReq = new FakeHttpRequest(request);
	// 最前面有 Sample Request 格式
	fakeReq.setFakeParameter("q", q);
	
	qaCtx.setRequestAttribute("requestId", requestId);
	QAUtil.parseRequest(qaCtx, fakeReq, testMode, true);

	System.out.println(qaCtx.toString());
	
	JSONObject resp = null;
	
	// 這裡開始 
	if ((qaCtx.getEventType() != null && !"message".equalsIgnoreCase(qaCtx.getEventType().getCode())) || qaCtx.getCurrentQuestion() != null) {
		QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
		c.check(qaCtx);
	
		resp = qaCtx.genResponseJSON();
		// line.messages 拿來全用
		if (isLINEChannel) {
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
					messages.add(0, LINEBotApi.buildTextMessage(forLine));
				}
			}
		
			JSONObject lineResult = new JSONObject();
			lineResult.put("messages", messages);
			resp.put("line", lineResult);
		}
		
		JSONObject rspCmd = null;
		
		switch (qaCtx.getQuestionType()) {
			case PROFESSIONAL:
			case INTERCEPTED:
				newResp.put("resultCode", "0");
				newResp.put("resultMessage", "成功");
				rspCmd = new JSONObject().put("type", "CPE").put("content", resp.optString("output"));
			break;
			case ANSWER_OPTION:
			case CASUAL:
			case PREPARE_FORWARD:
			case ANSWER_FORWARD:
			case ANSWER_FORWARD_OPTION:
			case BAD_QUESTION:
			case UNKNOWN:
			default:
				newResp.put("resultCode", "1");
				newResp.put("resultMessage", "失敗");
				rspCmd = new JSONObject().put("type", "NO").put("content", resp.optString("output"));
		}	
		
		
		newResp.put("results", new JSONArray().put(rspCmd));
	}
}
catch (Exception rbtEx) {
	out.println(rbtEx.getMessage());
	newResp.put("resultCode", "1");
	newResp.put("resultMessage", "錯誤：" + rbtEx.getMessage());
	return;
}
//}
%><%= newResp.toString(4) %>
