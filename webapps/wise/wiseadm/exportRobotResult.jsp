<%@ include file="/commons/taglib.jsp"%>
<%@ page pageEncoding="UTF-8" language="java" 
import="java.text.*"
import="java.util.*"
import="com.intumit.message.*"
import="com.intumit.solr.robot.*"
import="org.apache.commons.lang.StringUtils"
import="org.json.*"
import="org.jsoup.Jsoup"
import="io.netty.util.internal.StringUtil"
%><%@ page import="com.intumit.solr.admin.*" %><%!
String handleOtherChannelOutput(Integer tenantId, String output, String eservice, JSONObject msg) throws JSONException {
	// 其他特殊Channel的答案不會在output顯示的都在這邊處理(LINE、Messenger etc..)
    QAChannel channel = QAChannel.get(tenantId, eservice);
    if (channel != null) {
    	if (channel.getType() == QAChannelType.LINE) {
        	if (msg.has("line")) {
        		JSONObject line = msg.getJSONObject("line");
        		JSONArray messages = line.optJSONArray("messages");
        		int messagesCount = messages.length();
        		for (int i = 0; messages != null && i < messagesCount; i++) {
        			// 先判斷是一般文字還是template
        			JSONObject message = messages.getJSONObject(i);
        			if ("text".equals(message.optString("type"))) {
        				output += message.getString("text");
        			} else if ("template".equals(message.optString("type"))) {
        				JSONObject template = message.getJSONObject("template");
        				// 雖然carousel與buttons template可以不放圖片
        				// 但基本上目前客戶有這個需求都會放圖，所以一些例外狀況就不特別處理
        				if ("buttons".equals(template.optString("type"))) {
        					// 避免階層、推薦影響
        					if (template.optString("thumbnailImageUrl") != null) {
        						output += message.optString("altText") + "[圖卡(單張)]";
        					}
        				} else if ("carousel".equals(template.optString("type"))) {
        					output += message.optString("altText") + "[圖卡(多張)]";
        				}
        			} else if ("imagemap".equals(message.optString("type"))) {
        				output += message.optString("altText") + "[大圖]";
        			}
        				
        			// 最後一個之前都用斷行將答案分開
        			if (i < messagesCount - 1) 
        				output += "\n";
        		}
        	}
       	} else if (channel.getType() == QAChannelType.FACEBOOK_MESSENGER) {
       			
       	}
    }
	return output;
}
%><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String start = request.getParameter("start").replace("T", " ") + ":00";
System.out.println(start);
String end = request.getParameter("end").replace("T", " ") + ":59";
System.out.println(end);
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

// 這裡應該要再用 listBy 的 start + rows 的分頁機制做 while loop
// 避免日期範圍很大的時候這裡會 OOM
List<ServiceLogEntity> logEntitys = ServiceLogEntity.listByDate(t.getId(), "robot:cathay:chat", sdf.parse(start), sdf.parse(end));

response.setHeader("Content-Type","text/csv; charset=" + (com.intumit.hithot.HitHotLocale.zh_TW == t.getLocale() ? "BIG5" : "UTF-8"));
response.setHeader("Content-Disposition","inline; filename="+new Date().getTime()+"_Log_Entity.csv");
String outputData = "";
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String title = MessageUtil.getMessage(locale, "session.number")+","+MessageUtil.getMessage(locale, "end.record.time")+","+MessageUtil.getMessage(locale, "statistics.msgCount.fromRobot")
				+","+MessageUtil.getMessage(locale, "statistics.msgCount.fromUser")+","+MessageUtil.getMessage(locale, "statistics.msgCount.hasAnswer")+","+MessageUtil.getMessage(locale, "statistics.msgCount.noAnswer")
				+","+MessageUtil.getMessage(locale, "statistics.msgCount.total")+","+MessageUtil.getMessage(locale, "global.eservice")+","+MessageUtil.getMessage(locale, "enter.time.record")+","+MessageUtil.getMessage(locale, "test.time")
				+",User Type,"+MessageUtil.getMessage(locale, "category")+","+MessageUtil.getMessage(locale, "global.label")+ "," + MessageUtil.getMessage(locale, "customer.feedback")+","+MessageUtil.getMessage(locale, "test.question")
				+","+MessageUtil.getMessage(locale, "robot.answer")+","+MessageUtil.getMessage(locale, "global.confidence.level")+","+MessageUtil.getMessage(locale, "global.action")+","+MessageUtil.getMessage(locale, "robot.answer.number")
				+","+MessageUtil.getMessage(locale, "last.edit") + "\n";
out.clear();
out.write(title);

for(ServiceLogEntity logEntity : logEntitys){
	out.write("\"" + logEntity.getQaId() + "\",");
	out.write("\"" + logEntity.getTimestamp() + "\",");
	out.write("\"" + logEntity.getStatMsgCountFromRobot() + "\",");
	out.write("\"" + logEntity.getStatMsgCountFromUser() + "\",");
	out.write("\"" + logEntity.getStatMsgCountHasAnswer() + "\",");
	out.write("\"" + logEntity.getStatMsgCountNoAnswer() + "\",");
	out.write("\"" + logEntity.getStatMsgCountTotal() + "\",");
	out.write("\"" + (StringUtil.isNullOrEmpty(logEntity.getChannel()) ? "" : logEntity.getChannel()) + "\"\n");
	JSONObject conversation = new JSONObject(logEntity.getConversations());
	if (conversation.has("messages")) {
        JSONArray messages = new JSONArray();
        try {
            messages = conversation.getJSONArray("messages");
        }
        catch (Exception ex) {}
            
        for (int i=0; i < messages.length(); i++) {
        	Object obj = messages.get(i);
        	if (obj instanceof JSONObject) {
        		JSONObject msg = (JSONObject)obj;
            
        		String enterTime = msg.has("originalQuestionTime") ? msg.getString("originalQuestionTime") : "";
	        	String dt = msg.has("datetime") ? msg.getString("datetime") : "";
	        	String eservice = msg.has("eservice") ? msg.getString("eservice") : "";
	        	String userType = msg.has("userType") ? msg.getString("userType") : "";
	        	String currentQaCategory = msg.has("currentQaCategory") ? msg.getString("currentQaCategory") : "";
	        	String currentCategory = msg.has("currentCategory") ? msg.getString("currentCategory") : "";
	        	String feedback = msg.has("feedback") ? msg.getString("feedback") : "0";
	        	String question = msg.has("originalQuestion") ? msg.getString("originalQuestion") : "N/A";
	        	String output = msg.has("output") ? msg.getString("output") : "N/A";
	        	String confidence = msg.has("confidence") ? msg.getString("confidence") : "";
	        	String currentQaUse = msg.has("currentQaUse") ? msg.getString("currentQaUse") : "";
                String kid = msg.has("kid") ? msg.getString("kid") : "";
	        	String action = msg.has("action") ? (msg.getString("action").equals("auto") ? MessageUtil.getMessage(locale, "global.click") : MessageUtil.getMessage(locale, "global.manual")) : "";
	        	
	        	if (msg.has("line") || msg.has("messenger")) {
	        		output = "";
	        		output = handleOtherChannelOutput(t.getId(), output, eservice, msg);
	        	}
	        	
	        	out.write("-,-,-,-,-,-,-,");
	        	out.write("\"" + eservice + "\",");
	        	out.write("\"" + enterTime + "\",");
	        	out.write("\"" + dt + "\",");
	        	out.write("\"" + userType + "\",");
	        	out.write("\"" + currentQaCategory + "\",");
	        	out.write("\"" + currentCategory + "\",");
	        	out.write("\"" + feedback + "\",");
	        	out.write("\"" + Jsoup.parse(StringUtils.replaceChars(question, ",\"\\", " ")).text() + "\",");
	        	out.write("\"" + Jsoup.parse(StringUtils.replaceChars(output, ",\"\\", " ")).text() + "\",");
	        	out.write("\"" + confidence + "\",");
	        	out.write("\"" + action + "\",");
                out.write("\"" + kid + "\",");
	        	out.write("\"" + currentQaUse + "\"\n");
        	
        	}
        }
    }
}

out.flush();
%>
