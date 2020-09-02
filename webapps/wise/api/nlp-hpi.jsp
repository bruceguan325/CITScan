<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.commons.io.IOUtils"
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
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.util.*"
%><%!
%><%JSONObject resp = new JSONObject();

try {
	Tenant t = null;
	if (request.getParameter("apikey") != null) {
		String apikey = request.getParameter("apikey");
		com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
		
		if (k != null) {
			t = k.getTenant();//Tenant.getTenantByApiKey(apikey);
			k.incCallCounter();
			Apikey.saveOrUpdate(k);
		}
		else {
			t = null;
		}
	}
	
	if (t == null) {
		JSONObject errorOutput = new JSONObject();
		errorOutput.put("errorCode", 400);
		errorOutput.put("errorMessage", "Cann't determine skill type.");
		out.println(errorOutput.toString(2));
		return;
	}
	
	String json = IOUtils.toString(request.getInputStream(), "UTF-8");
	System.out.println("got hpi request: " + json);
	String CTX_PREFIX = "HPI:";
	
	JSONObject input = null;
	try {
		input = new JSONObject(json);
		
		CTX_PREFIX += input.optString("channel", "hpi") + ":";
		CTX_PREFIX += input.optString("platform", "pepper") + ":";
	}
	catch (Exception e) {
		JSONObject errorOutput = new JSONObject();
		errorOutput.put("errorCode", 500);
		errorOutput.put("errorMessage", "Input request error:" + e.getMessage());
		out.println(errorOutput.toString(2));
		return;
	}
	
	Locale locale = Locale.TRADITIONAL_CHINESE;//(Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	if (locale == null) {
		String localeStr = request.getParameter("locale");
		
		if (localeStr != null) {
			locale = new Locale(localeStr);
		}
	}
	
	String hpiSessionId = input.optString("sessionId");
	if (StringUtils.isEmpty(hpiSessionId)) {
		hpiSessionId = java.util.UUID.randomUUID().toString();
	}
	
	String qaId = CTX_PREFIX + hpiSessionId;
	
	
	boolean testMode = false;
	boolean noRecommand = false;
	
	QAContext qaCtx = QAContextManager.lookup(qaId);
	if (qaCtx == null) {
		qaCtx = QAContextManager.create(qaId);
	}
	qaCtx.setClientSupportHtml(false);
	
	if (t != null) {
		qaCtx.setTenant(t);
	}
	// 測試模式不計算 request 量，不然目前排程測試或者相關的自動測試可能都會卡住
	else if (!testMode) {
		if (!qaCtx.acquireRequestPermit() 
			|| !RequestRateLimiter.acquireRequestPermition(
				com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request), 
				qaCtx.getTenant(), "/nlp-hpi.jsp")) {
			JSONObject errorOutput = new JSONObject();
			errorOutput.put("errorCode", 600);
			errorOutput.put("errorMessage", "Cannot acquire request permission, you should not flooding API request.");
		
			out.println(errorOutput.toString(2));
		
			return;
		}
	}
	
	qaCtx.shift(); // 清除 request scope attributes
	qaCtx.setLocale(com.intumit.hithot.HitHotLocale.zh_TW);
	
	String htmlParam = StringUtils.defaultString(request.getParameter("html"), "false");
	String icspid = request.getParameter("ICSPID");
	String qaCategorySelect = request.getParameter("qaCategorySelect");
	Boolean forceToggleContextThisTime = null;
	if (request.getParameter("ftc") != null) {
		if (Boolean.parseBoolean(request.getParameter("ftc")))
		{
			forceToggleContextThisTime = true;
		}else {
			forceToggleContextThisTime = false;
		}
	}
	
	boolean explainQA = qaCtx.getTenant().getEnableQAExplain();
	
	String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(input.getString("question")));
	
	if (explainQA) {
		qaCtx.appendExplain("Enter QA-AJAX", "Got Question [" + com.intumit.solr.servlet.SolrDispatchFilter.getClientIpAddr(request) + "]："
		+ qaCtx.getCurrentQuestion() + "(OriginQ:" + tmpQ + ")");
	}
	
	JSONObject conversation = qaCtx.getConversationsJson();
	
	if (conversation == null) {
		conversation = new JSONObject();
	}
	
	if (!conversation.has("messages")) {
		conversation.put("messages", new JSONArray());
	}
	
	if(StringUtils.isNotBlank(htmlParam)) {
		qaCtx.setClientSupportHtml(Boolean.parseBoolean(htmlParam));
	}
	qaCtx.setForceToggleContextThisTime(forceToggleContextThisTime);
	qaCtx.setIcspid(icspid);
	qaCtx.setRequestAttribute("noRecommand", noRecommand);
	qaCtx.setRequestAttribute("qaCategorySelect", qaCategorySelect);
	
	String qaAnswerType = input.optString("platform", "pepper");
	System.out.println("Channel [platform] = " + qaAnswerType);
	qaCtx.setQaChannel(qaAnswerType);

	String userType = input.optString("userType", "unknown");
	qaCtx.setUserType(userType);
	
	String suggestQ = qaCtx.getTenant().getEnablePhoneticHomonym() 
					? KnowledgePointDictionary.suggestFullQuestion(qaCtx.getTenant().getId(), tmpQ)
					: null;
	
	qaCtx.setOriginalQuestion(tmpQ);
	qaCtx.setCurrentQuestion(suggestQ != null ? suggestQ : tmpQ);
	JSONObject ourResp = null;
	
	if (qaCtx.getCurrentQuestion() != null) {
		QAMatchRuleController c = QAMatchRuleController.getInstance(qaCtx.getQAChannelInstance());
		c.check(qaCtx);
		ourResp = qaCtx.genResponseJSON();
		
		resp.put("answer", ourResp.getString("output"));
		resp.put("type", "1");
		resp.put("url", "");
		resp.put("result", "1");
		resp.put("opendata", JSONObject.NULL);
		
		List<String> omit = Arrays.asList(new String[] {"_bundle", "bundle", "answerType", "confidence", "confidenceFactor", "currentCategory", "currentQaCategory", "kid", "lastQaCategory", "link", "originalQuestion", "output", "questionType", "score"});
		for (Object key: ourResp.keySet()) {
			String keyStr = key.toString();
			
			if (!omit.contains(keyStr)) {
				resp.put(keyStr, ourResp.get(key));
			}
		}
	
		System.out.println(ourResp.toString(4));
		//System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
		//System.out.println(qaCtx.explainPath());
	}

	if (ourResp != null) {
		conversation.getJSONArray("messages").put(ourResp);
	}
	qaCtx.setConversations(conversation.toString());
	//System.out.println(new JSONObject(new JSONSerializer().exclude("answerText", "response", "reqAttr", "explain").deepSerialize(qaCtx)).toString(4));
	QAContextManager.put(qaId, qaCtx);
	

	//System.out.println(qaCtx.explainPath());

	// DO LOG
	ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);
	
	if (log == null) {
		log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(),
		ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
		ServiceLogEntity.setIntoSession(qaId, log);
	}

	log.setConversations(conversation.toString(2));
	log.setLastMessage(qaCtx.getOriginalQuestion());
	// 有限制業務類別就存業務類別，沒有就看看知識主題有沒有業務類別，有的話存第一個
	if (qaCtx.getRestrictToQaCategory() != null) {
		log.setLastQaCategory(qaCtx.getRestrictToQaCategory());
	}
	else if (qaCtx.getCurrentKPs() != null && qaCtx.getCurrentKPs().length > 0) {
		for (DictionaryDatabase dd: qaCtx.getCurrentKPs()) {
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
}
catch (Exception e) {
	resp.put("answer", "");
	resp.put("type", "1");
	resp.put("url", "");
	resp.put("result", "-1");
	resp.put("opendata", JSONObject.NULL);
	resp.put("errorCode", 500);
	resp.put("errorMessage", "Input request error:" + e.getMessage());
}%>
<%= resp %>