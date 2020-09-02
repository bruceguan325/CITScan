<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.util.*"
import="com.intumit.message.MessageUtil"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}

%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

JSONObject schema = null;

String type = request.getParameter("type");
//
Locale defaultLocale = Locale.getDefault();
String country = defaultLocale.getCountry();
String language = defaultLocale.getLanguage();
//
if (type.equals("qa-extra")) {
	Locale locale =  (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
    if(locale.equals(Locale.SIMPLIFIED_CHINESE)){
    	schema = new JSONObject(QA.class.getResourceAsStream("qa-extra-schema.CN.json"));
    }
    else if(locale.equals(Locale.JAPAN)) {
    	schema = new JSONObject(QA.class.getResourceAsStream("qa-extra-schema.JA.json"));
    }
    else{
    	schema = new JSONObject(QA.class.getResourceAsStream("qa-extra-schema.TW.json"));      
    }
}
else if (type.equals("line-messages")) {
	schema = new JSONObject(QA.class.getResourceAsStream("line-messages-schema.json"));   
}
else if (type.equals("line-template")) {
	schema = new JSONObject(QA.class.getResourceAsStream("line-template-schema.json"));   
}
else if (type.equals("line-carousel-column")) {
	schema = new JSONObject(QA.class.getResourceAsStream("line-carousel-column-schema.json"));   
}
else if (type.equals("line-action")) {
	schema = new JSONObject(QA.class.getResourceAsStream("line-action-schema.json"));   
}
else if (type.equals("qa-h-extra")) {
    	schema = new JSONObject(QA.class.getResourceAsStream("qa-hierarchial-extra-schema.json"));      
}
else if (type.equals("qa-dlg-perceptions")) {
	schema = new JSONObject(QA.class.getResourceAsStream("qa-dialog-perceptions-schema.json"));
	Locale locale =  (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	schema.put("title", MessageUtil.getMessage(locale, "scenario.awareness.condition.setting.area"));
	schema.put("description", MessageUtil.getMessage(locale, "scenario.multiple.sensing.conditions"));
	JSONObject v = schema.getJSONObject("items").getJSONObject("properties").getJSONObject("label");
	v.put("title", MessageUtil.getMessage(locale, "scenario.name.suggestion"));
	JSONObject j = schema.getJSONObject("items").getJSONObject("properties").getJSONObject("type");
	j.put("title",  MessageUtil.getMessage(locale, "scenario.awareness.type"));
	JSONArray datalist=j.getJSONArray("enumSource");
	for(Object projectObj:datalist.toArray()){
		JSONObject project = (JSONObject)projectObj;
		JSONArray sourcelist=(JSONArray) project.get("source");
		for(Object sourceObj:sourcelist){
			JSONObject x = (JSONObject) sourceObj;
			String u = x.get("title").toString();			
			if(u.equalsIgnoreCase("正規表示法")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.representation"));
			}else if(u.equalsIgnoreCase("確認是或否")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.confirmation.yesno"));
			}else if(u.equalsIgnoreCase("數字及數字範圍")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.number.range"));
			}else if(u.equalsIgnoreCase("字串比對")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.string.comparison"));
			}else if(u.equalsIgnoreCase("日期及時間比對")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.datetime.comparison"));
			}else if(u.equalsIgnoreCase("地址比對")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.address.comparison"));
			}else if(u.equalsIgnoreCase("記憶、意圖與實體")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.memory.intentions.entities"));
			}else if(u.equalsIgnoreCase("無條件進入")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.unconditional.entry"));
			}else if(u.equalsIgnoreCase("身份證字號")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.ID"));
			}else if(u.equalsIgnoreCase("記憶、意圖與實體比對")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.memory.intention.entity.comparison"));
			}else if(u.equalsIgnoreCase("一般問答句型比對")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.general.question.sentence.comparison"));
			}else if(u.equalsIgnoreCase("內建函數呼叫")){
				x.put("title", MessageUtil.getMessage(locale, "Domestic.function"));
			}else{
				System.out.println(u);
			}
			
		}
	}
	
	JSONObject w=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("checkval");
	w.put("title", MessageUtil.getMessage(locale, "scenario.variable.replacement"));
	w.put("description", MessageUtil.getMessage(locale, "scenario.variable.replacement.description"));
	JSONObject y=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("criteria");
	y.put("title", MessageUtil.getMessage(locale, "scenario.awareness.condition"));
	y.put("description", MessageUtil.getMessage(locale, "scenario.awareness.condition.description"));
	JSONObject z=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("enable");
	z.put("title", MessageUtil.getMessage(locale, "is.enabled"));
	//System.out.println(schema);
	//schema.replace("title", schema.getString("title"), MessageUtil.getMessage(request.getLocale(), "scenario.awareness.condition.setting.area"));
	//schema.replace("description", schema.get("description"), MessageUtil.getMessage(request.getLocale(), "scenario.multiple.sensing.conditions"));
	//schema.replace("items", schema.getString("items"), MessageUtil.getMessage(request.getLocale(), "scenario.multiple.sensing.conditions"));
// 	Iterator<?> irt = schema.keys();
// 	while(irt.hasNext()){
// 		System.out.println("irt="+irt.next());
// 	}
}
else if (type.equals("qa-dlg-reactions")) {
	schema = new JSONObject(QA.class.getResourceAsStream("qa-dialog-reactions-schema.json")); 
	Locale locale =  (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	schema.put("title", MessageUtil.getMessage(locale, "scenario.response.action.setting.area"));
	schema.put("description", MessageUtil.getMessage(locale, "scenario.response.action.setting.area.description"));
	JSONObject v = schema.getJSONObject("items").getJSONObject("properties").getJSONObject("label");
	v.put("title", MessageUtil.getMessage(locale, "scenario.name.suggestion"));
	JSONObject j = schema.getJSONObject("items").getJSONObject("properties").getJSONObject("when");
	j.put("title", MessageUtil.getMessage(locale, "scenario.response.timing"));
	JSONArray datalist=j.getJSONArray("enumSource");
	for(Object projectObj:datalist.toArray()){
		JSONObject project = (JSONObject)projectObj;
		JSONArray sourcelist=(JSONArray) project.get("source");
		for(Object sourceObj:sourcelist){
			JSONObject x = (JSONObject) sourceObj;
			String u = x.get("title").toString();			
			if(u.equalsIgnoreCase("剛進入節點")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.just.entered.node"));
			}else if(u.equalsIgnoreCase("等待使用者輸入")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.waiting.for.input"));
			}else if(u.equalsIgnoreCase("數字及數字範圍")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.number.range1"));
			}else if(u.equalsIgnoreCase("接收到使用者輸入")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.receives.user.input1"));
			}else if(u.equalsIgnoreCase("準備離開節點")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.ready.leave.node1"));
			}else if(u.equalsIgnoreCase("節點進入休眠")){
				x.put("title", MessageUtil.getMessage(locale, "scenario.node.goes.sleep1"));
			}else{
				System.out.println(u);
			}
			
		}
	}
	JSONObject j1 = schema.getJSONObject("items").getJSONObject("properties").getJSONObject("type");
	j1.put("title", MessageUtil.getMessage(locale, "scenario.response.type"));
	JSONArray datalist1=j1.getJSONArray("enumSource");
	for(Object projectObj1:datalist1.toArray()){
		JSONObject project1 = (JSONObject)projectObj1;
		JSONArray sourcelist1=(JSONArray) project1.get("source");
		for(Object sourceObj1:sourcelist1){
			JSONObject x1 = (JSONObject) sourceObj1;
			String u = x1.get("title").toString();			
			if(u.equalsIgnoreCase("覆蓋答案")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.coverage.answer"));
			}else if(u.equalsIgnoreCase("附加回覆")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.additional.reply"));
			}else if(u.equalsIgnoreCase("設定選單")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.setting.menu"));
			}else if(u.equalsIgnoreCase("附加選項")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.additional.options"));
			}else if(u.equalsIgnoreCase("附加LINE圖文")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.attached.LINE.graphic"));
			}else if(u.equalsIgnoreCase("轉送到別題")){
				x1.put("title", MessageUtil.getMessage(locale, "hierarchical.redirect.other.question"));
			}else if(u.equalsIgnoreCase("轉送到別個節點")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.transferred.another.node"));
			}else if(u.equalsIgnoreCase("轉送回上個節點（從那裡來，就回那裡去）")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.transferred.back.previous.node"));
			}else if(u.equalsIgnoreCase("設定情境變數（情境結束就忘記）")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.setting.context.variable"));
			}else if(u.equalsIgnoreCase("設定長期變數（直到使用者離開）")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.setting.long.term.variables"));
			}else if(u.equalsIgnoreCase("設定長期變數(搜尋特殊問答)")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.search.special.question.answer"));
			}else if(u.equalsIgnoreCase("設定一次性變數")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.setting.onetime.variables"));
			}else if(u.equalsIgnoreCase("清空情境變數")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.clearing.situation.variables"));
			}else if(u.equalsIgnoreCase("清空長期變數")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.clearing.longterm.variables"));
			}else if(u.equalsIgnoreCase("清空一次性變數")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.clear.onetime.variables"));
			}else if(u.equalsIgnoreCase("結束情境")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.END"));
			}else if(u.equalsIgnoreCase("根據長期變數附加回覆")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.based.longterm.variables.attached.reply"));
			}else if(u.equalsIgnoreCase("執行 Script")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.execution.script"));
			}else if(u.equalsIgnoreCase("設定長期變數(取得日期)")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.acquisition.date"));
			}else if(u.equalsIgnoreCase("設定反應變數")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.setting.response.variable"));
			}else if(u.equalsIgnoreCase("貼使用者標籤")){
				x1.put("title", MessageUtil.getMessage(locale, "scenario.sticker.user.label"));
			}else{
				System.out.println(u);
			}
			
		}
	}
	JSONObject w1=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("contents");
	w1.put("title", MessageUtil.getMessage(locale, "Context.parameter.array"));
	w1.put("description", MessageUtil.getMessage(locale, "Context.parameter.array.description"));
	JSONObject w2=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("contents").getJSONObject("items");
	w2.put("title", MessageUtil.getMessage(locale, "Context.parameter"));
	w2.put("description", MessageUtil.getMessage(locale, "scenario.parameter.value"));
	JSONObject y1=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("enable");
	y1.put("title", MessageUtil.getMessage(locale, "is.enabled"));
	JSONObject z1=schema.getJSONObject("items").getJSONObject("properties").getJSONObject("detail");
	z1.put("title", MessageUtil.getMessage(locale, "scenario.remark.area"));
	z1.put("description", MessageUtil.getMessage(locale, "scenario.remark.area.description"));
}
else if (type.equals("rule-perceptions")) {
	schema = new JSONObject(QA.class.getResourceAsStream("qa-dialog-perceptions-schema.json"));
	
}
else if (type.equals("rule-reactions")) {
	schema = new JSONObject(QA.class.getResourceAsStream("qa-dialog-reactions-schema.json"));      
}
else if (type.equals("dialog")) {
//	schema = new JSONObject(QADialogRule.class.getResourceAsStream("dialog-schema.json"));
}
%><%= schema %>
