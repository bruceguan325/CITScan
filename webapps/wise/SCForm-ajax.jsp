<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" trimDirectiveWhitespaces="true"
import="java.util.*"
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.form.Form"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.push.*"
import="com.intumit.solr.robot.UserClue"
import="org.apache.commons.lang.StringUtils"

%>
<%
Tenant t = null;
if (StringUtils.isNotBlank(request.getParameter("apikey"))) {
  Apikey apikey = Apikey.getByApiKey(request.getParameter("apikey"));
  if(apikey != null) {
    t = apikey.getTenant();
  }
}

if (t == null) {
  JSONObject errorOutput = new JSONObject();
  errorOutput.put("errorCode", 400);
  errorOutput.put("errorMessage", "Cann't determine skill type.");
  out.println(errorOutput.toString(2));
  return;
}

String action = request.getParameter("action");
String id = request.getParameter("id"); 
String uid = request.getParameter("uid");
String formContent = request.getParameter("formContent");
boolean draft = Boolean.parseBoolean(request.getParameter("draft")); 

JSONObject resp = new JSONObject();
Form form = null;

if (StringUtils.equalsIgnoreCase(action, "save")) {
  form = new Form();
  form.setUid(uid);
  form.setFormContent(formContent);
  if(StringUtils.isNotBlank(id)) {
    form.setId(Long.parseLong(id));
  }
  form.setStatus((draft ? Form.DRAFT : Form.SUBMIT));
  form.setTenantId(t.getId());
  Form.saveOrUpdate(form);
  resp.put("success", true);
  UserClue uc = UserClue.getByLineUserId(t.getId(), uid);
  QAChannel ch = QAChannel.get(t.getId(), "line");
  
  if (uc != null) {
	  java.util.Date now = new java.util.Date();
	  PushData pd = new PushData();
	  pd.setContentType(PushData.ContentType.QA);
	  pd.setCreatedTime(now);
	  pd.setUpdateTime(now);
	  pd.setTargetChannelsType(PushData.TargetChannelsType.byConfig);
	  pd.setTargetChannels("line");
	  pd.setTenantId(t.getId());
	  
	  if (draft) {
		  pd.setPushContent("800"); // 800 是暫存後提醒訊息
	  }
	  else {
		  pd.setPushContent("801"); // 801 是完成送出後提醒訊息
	  }
	  new PushService(t).syncPush(pd, uc);
	  
	  // 開始處理定時通知
	  if (draft) {
		 String ns = "sc:draft:daily:alert";
		 
		 // 先停掉目前的定時
		 List<SingleUserPushTrigger> existTriggers = SingleUserPushTrigger.getByStatus(t.getId(), ns, UserPushTrigger.Status.inqueue);
		 for (SingleUserPushTrigger trigger: existTriggers) {
			 trigger.setPushStatus(UserPushTrigger.Status.suspended);
			 SingleUserPushTrigger.saveOrUpdate(trigger);
		 }
		 
		 // 新增定時
		 pd.setPushContent("802");	// 每天早上提醒的訊息（三次結束） 
		 PushData.saveOrUpdate(pd);
		 
		 Calendar today8am = Calendar.getInstance();
		 today8am.set(Calendar.HOUR_OF_DAY, 8); 
		 today8am.set(Calendar.MINUTE, 0); 
		 today8am.set(Calendar.SECOND, 0);
		 Calendar next8am = today8am;
		 
		 // 找前一個8am，假設現在早於8am，就是昨天8am，假設晚於8am，就是今天 8am。後面 loop 會 +1 天
		 if (Calendar.getInstance().after(today8am)) {
			 next8am.add(Calendar.DATE, -1);
		 }
		 
		 for (int i=0; i < 3; i++) {
			 //next8am.add(Calendar.HOUR, 1); // 先做連三小時通知
			 next8am.add(Calendar.DATE, 1); 
			 
			 SingleUserPushTrigger pt = new SingleUserPushTrigger();
			 pt.setCreatedTime(now);
			 pt.setUpdateTime(now);
			 pt.setCriteriaType(UserPushTrigger.CriteriaType.datetime);
			 pt.setPushCriteria("" + (next8am.getTime().getTime() / 1000)); // Unixtime
			 pt.setNamespace(ns);
			 pt.setPushDataId(pd.getId().intValue());
			 pt.setPushStatus(UserPushTrigger.Status.inqueue);
			 pt.setStopCriteriaType(UserPushTrigger.StopCriteriaType.maxTimes);
			 pt.setStopCriteria("1"); // 1次
			 pt.setTriggeredTimes(0); // 一開始當然是 0
			 pt.setTenantId(t.getId());
			 pt.setUserClueId(uc.getId());
			 
			 SingleUserPushTrigger.saveOrUpdate(pt); // 打完收工
		 }
		  // 替用戶上標籤
		  UserClueTag.addTagToUser(uc, "SC_LOAN_APPLY_DRAFT_SAVED");
	  }
	  else {
		  // 替用戶上標籤
		  UserClueTag.clearTagOnUser(uc, "SC_LOAN_APPLY_DRAFT_SAVED");
		  UserClueTag.addTagToUser(uc, "SC_LOAN_APPLY_SENT");
	  }
  }
}
else if(StringUtils.equalsIgnoreCase(action, "del")) {
  if(StringUtils.isNotBlank(id)) {
	 Form.deleteById(Long.parseLong(id));
  }
  else if(StringUtils.isNotBlank(uid)) {
     Form.deleteByUid(t.getId(), uid);
  }
  else {
     Form.deleteTenantForms(t.getId()); 
  }
}
else {
  form = Form.get(t.getId(), uid);
  if(form == null) {
    resp.put("hasOld", false);
  }
  else {
    resp.put("hasOld", true);
    resp.put("id", form.getId());
    resp.put("old", new JSONObject(form.getFormContent()));
  }
}

%>
<%=resp.toString(2) %>
