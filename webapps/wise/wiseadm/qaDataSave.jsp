<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="org.apache.commons.collections.CollectionUtils"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugin"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="com.intumit.solr.robot.QAAltBuildQueue"%>
<%@page import="com.intumit.solr.robot.ExpireDuration"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="com.intumit.solr.robot.QA"%>
<%@page import="com.intumit.solr.robot.ProcessQADataServlet"%>
<%@page import="com.intumit.solr.admin.*" %>
<%@page contentType="application/json" pageEncoding="UTF-8" language="java"
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
import="org.json.*"
import="com.intumit.message.MessageUtil"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.dto.*"

import="com.intumit.solr.util.XssHttpServletRequestWrapper"
import="com.intumit.syslog.OperationLogEntity"

%><%!
static List<String> getMultiValueParameters(HttpServletRequest req, String name){
	String param = req.getParameter(name);
	return QAUtil.parseMultiValue(param);
}

String qaToString(QA qa) {
	return "QA [getQuestion()=" + qa.getQuestion() 
			+ ", getBrieflyQuestion()=" + qa.getBrieflyQuestion() 
			+ ", getExtraParams()=" + qa.getExtraParams() 
			+ ", getQuestionAltTemplates()=" + qa.getQuestionAltTemplates() 
			+ ", getIsBanned()=" + qa.getIsBanned() 
			+ ", getIsNotForMLT()=" + qa.getIsNotForMLT() 
			+ ", getIsNotForSearch()=" + qa.getIsNotForSearch()
			+ ", getNotAppendMLT()=" + qa.getNotAppendMLT() 
			+ ", getKid()=" + qa.getKid() 
			+ ", getTags()=" + qa.getTags() 
			+ ", getBinderIds()=" + qa.getBinderIds() 
			+ ", getFieldNames()=" + qa.getFieldNames()
			+ ", getDocumentBoost()=" + qa.getDocumentBoost()
			+ ", entrySet()=" + qa.entrySet()
			+ ", isEmpty()=" + qa.isEmpty() 
			+ ", keySet()=" + qa.keySet()
			+ ", size()=" + qa.size() 
			+ ", values()=" + qa.values()
			+ ", getChildDocuments()=" + qa.getChildDocuments()
			+ ", hasChildDocuments()=" + qa.hasChildDocuments()
			+ ", getClass()=" + qa.getClass() 
			+ ", hashCode()=" + qa.hashCode()
			+ "]";
}
%><%
JSONObject result = new JSONObject();

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	result.put("status", "denied");

	out.println(result);
	return;
}
%><%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
try {

	if (log == null) {
		result.put("status", "error");
		result.put("errorMsg", "OperationLogEntity Missing");
		out.println(result);
		return;
	}

	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	QAUtil qu = QAUtil.getInstance(t);
	Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	String id = StringUtils.trimToNull(request.getParameter("id"));
	
    boolean batchNlpReplaceSyn = Boolean.parseBoolean(request.getParameter("batchNlpReplaceSyn"));
    
	// 要合併的知識點
	String mergeId = StringUtils.trimToNull(request.getParameter("mergeId"));
	QA mergeQA = (mergeId != null && !StringUtils.equals(id, mergeId)) ? new QA(qu.getMainQASolrDocument(new Integer(mergeId))) : null;
	
	Long kid = null;
	QA qa = null;
	SolrServer server = t.getCoreServer4Write();
	String qaAudit = request.getParameter("qaAudit");
	String qaStatus = request.getParameter("qaStatus");
	String regex="[`~!#$%^&*()+=\"|{}':;'\\[\\]<>~！#￥%……&*（）——+|{}【】‘；：”“’、]";
    Pattern pat = Pattern.compile(regex);
    Matcher mat = pat.matcher(request.getParameter("question"));
    String lastString = (String)request.getParameter("question");
    lastString = lastString.substring(lastString.length() -1);
    if(mat.find() || " ".equals(lastString)){
        throw new Exception("The question is not valid");
    }
	if (id != null) {
		kid = QAUtil.id2Kid(id);
		SolrDocument origDoc = null;
		if (qaAudit == null || qaAudit.equals("auditPass")) {
			origDoc = qu.getMainQASolrDocument(kid, true);
		} else if (t.getEnableQaAudit()) {
			origDoc = qu.getFilterQASolrDocument(kid, true, QAUtil.DATATYPE_QA_AUDIT, false);
		}

		if (origDoc != null) {
			qa = new QA(origDoc);
		}
		else {
			qa = new QA();
		}
	}
	else {
		qa = new QA();

		while (true) {
			kid = (System.currentTimeMillis() / 1000 / 60) * 10 + new Random().nextInt(10);

			if (qu.getMainQASolrDocument(kid, true) == null)
				break;
		}

		id = QAUtil.DATATYPE_COMMON_SENSE + "-" + kid;
	}
	
	if (qaAudit == null || qaAudit.equals("auditPass")) {
		id = id.replace(QAUtil.DATATYPE_QA_AUDIT, QAUtil.DATATYPE_COMMON_SENSE);
	} else if (t.getEnableQaAudit()) {
		id = id.replace(QAUtil.DATATYPE_COMMON_SENSE, QAUtil.DATATYPE_QA_AUDIT);
	}

	qa.setId(id);
	qa.setKid(kid);
	qa.setQuestion(request.getParameter("question"));
	qa.setBrieflyQuestion(request.getParameter("briefly_question"));
	qa.setAnswer(request.getParameter("answer_web_unknown"));
	qa.setAnswerPhone(request.getParameter("answer_app_unknown"));
	qa.setExtraParams(request.getParameter("extraParams"));
	qa.setCustomScript(request.getParameter("customScript"));
	
	String[] choosedEventTypes = request.getParameterValues("choosedEventTypes");
	
	if (choosedEventTypes != null) {
		qa.setTriggeredByEventTypes(Arrays.asList(choosedEventTypes));
	}
	else {
		qa.setTriggeredByEventTypes(null);
	}

	if (StringUtils.isNotEmpty(request.getParameter("locales"))) {
		qa.setLocales(Arrays.asList(StringUtils.split(request.getParameter("locales"), ",")));
	}
	
	if (qaAudit == null || qaAudit.equals("auditPass")) {
		qa.setDataType(QAUtil.DATATYPE_COMMON_SENSE);
	} else if (t.getEnableQaAudit()) {
		qa.setDataType(QAUtil.DATATYPE_QA_AUDIT);
		qa.setAndSaveAudit(qaAudit);
		qa.setStatus(qaStatus);
		
		if (qaAudit.equals("auditReject")) {
			qa.setStatus(qaStatus);
			qa.setAndSaveAudit("auditEdit");
			qa.setReject("Y");
		}else {
			qa.setReject("N");
		}
	}


	
	String forMlt = request.getParameter("forMlt");
	if (forMlt == null) {
		qa.setIsNotForMLT(true);
	}
	else {
		qa.setIsNotForMLT(false);
	}
	String forSearch = request.getParameter("forSearch");
	if (forSearch == null) {
		qa.setIsNotForSearch(true);
	}
	else {
		qa.setIsNotForSearch(false);
	}
	String appendMlt = request.getParameter("appendMlt");
	if (appendMlt == null) {
		qa.setNotAppendMLT(true);
	}
	else {
		qa.setNotAppendMLT(false);
	}

	String kmsRelateExpiredMemo = request.getParameter("kmsRelateExpiredMemo");
	if (kmsRelateExpiredMemo == null) {
		qa.setIsKmsRelateExpiredMemo(false);
	}
	else {
		qa.setIsKmsRelateExpiredMemo(true);
	}
	
	List<String> ihAltTplMkeys = new ArrayList<String>();
	List<String> ihAltTpls = new ArrayList<String>();
	
	Map<String, String[]> pmap = request.getParameterMap();
	for (String key: pmap.keySet()) {
		if (StringUtils.startsWith(key, "ihTplKeywords")) {
			String mkey = StringUtils.substringAfter(key, "ihTplKeywords");
			String keywords = StringUtils.trimToEmpty(request.getParameter(key));
			//QAAltTemplate tpl = QAAltTemplate.getByKey(t.getId(), mkey);
			
			if (request.getParameter("ihToggleTpl" + mkey) != null) {
				ihAltTplMkeys.add(mkey);
				ihAltTpls.add(mkey + ":" + keywords);
				System.out.println("Inheritant: (" + mkey + "):" + keywords );
			}
		}
	}
	qa.setOrUpdateField(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS, ihAltTplMkeys.size() > 0 ? ihAltTplMkeys : null);
	qa.setOrUpdateField(QA.FN_INHERITANT_ALT_TEMPLATES, ihAltTpls.size() > 0 ? ihAltTpls : null);

	String choosedQAPlugIn = StringUtils.trimToNull(request.getParameter("choosedQAPlugIn"));
	qa.setAndSavePlugin(choosedQAPlugIn, request);

	String expireRadio = request.getParameter("expireRadio");
	qa.setAndSaveExpire(expireRadio, request);

	qa.setAndSaveEnable(request);

	String qaCategoryRadio = request.getParameter("qaCategoryRadio");
	qa.setAndSaveCategory(qaCategoryRadio, request);

	qa.setTags(getMultiValueParameters(request, "category"));

	Date now = new Date();
	qa.setCreateInfo(now, user);
	qa.setUpdateInfo(now, user);

	List<String> qAlts = getMultiValueParameters(request, "question_alt");
	String batchNlp = StringUtils.trimToNull(request.getParameter("for_batch_nlp"));
  
	if ((qAlts == null || qAlts.isEmpty()) && ihAltTplMkeys.size() == 0) {
	    if (batchNlp == null) {
		    	System.out.println(String.format("qaAltsEmptyLog QAID[%s] adminName[%s]", id, user.getLoginName()));
				throw new IllegalArgumentException(MessageUtil.getMessage(locale, "qa.alts.blank.error"));
	    }
	}
	else {
		qa.setQuestionAltTemplates(qAlts);
	}

	// 這裡是提供「批次匯入例句」的功能，實際上就是批次斷2，檢查有沒有重複，然後就放入各種問法
	if (batchNlp != null) {
		List<String> forBatchNlp = getMultiValueParameters(request, "for_batch_nlp");
		List<String> dupCheck = new ArrayList<String>();
		List<String> currentAlts = qa.getQuestionAltTemplates();
		for (String altTplLine: currentAlts) {
			dupCheck.add(QA.parseAndGetQAAltOnly(altTplLine));
		}
		boolean dirty = false;

		for (String s: forBatchNlp) {
			String wivo = qu.doWiVoByRuleConfig(s, null);  // 批次匯入例句，先跑看看 WiVo，但不分 channel
			String newS = StringUtils.defaultString(wivo, s);
			String cleanedQ = QAUtil.removeSymbols(newS, t.getLocale());
			//String altTpl = QAUtil.nlp(t, qu, newS, locale, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING).getSegResult();
            String altTpl = qu.nlp(cleanedQ, qu, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING,batchNlpReplaceSyn).getSegResult();

			
			if (!dupCheck.contains(altTpl)) {
				dirty = true;
				dupCheck.add(altTpl + "");

				JSONObject obj = new JSONObject();
				obj.put("alt.editor", user.getId());
				obj.put("testCase", newS);
				obj.put("testCase.editor", user.getId());

				String altTplLine = altTpl + " // " + obj.toString();
				currentAlts.add(altTplLine);
			}
		}

		if (dirty)
			qa.setQuestionAltTemplates( currentAlts );
	}

	Set<Long> binderIds = null;
	List<String> binderIdStrs = getMultiValueParameters(request, "binderId");
	if(CollectionUtils.isNotEmpty(binderIdStrs)){
		binderIds = new HashSet<Long>();
		for(String _id : binderIdStrs){
			binderIds.add(Long.valueOf(_id));
		}
	}
	qa.setBinderIds(binderIds);
	
	String notBanned = request.getParameter("notBanned");
	if (notBanned == null) {
		qa.setBinderIds(null);
		qa.setIsBanned(true);
	}
	else {
		qa.setIsBanned(false);
	}
	
	if (StringUtils.trimToNull(request.getParameter("exAltTpls")) != null) {
		qa.setExcludeQuestionAltTemplates(Arrays.asList(request.getParameter("exAltTpls").split("\r?\n")));
	}
	
	// 合併 QA
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) { 
		if (mergeQA != null) {
			qu.mergeQA(qa, mergeQA, true, user);
			server.add(mergeQA);
		}
	}

	List<QAChannel> channels = QAChannel.list(t.getId());
	List<QAUserType> userTypes = QAUserType.list(t.getId());
	List<String> hasRichMessages = new ArrayList<String>();

	for (QAChannel c: channels) {
		for (QAUserType ut: userTypes) {
			String concatCode = "answer_" + c.getCode() + "_" + ut.getCode();	// ex. answer_DEFAULT_UNKNOWN  or answer_RICHART_VIP
			String answer = request.getParameter(concatCode);
			String ep = request.getParameter(concatCode + "_extraParams");

			QAChannelType cType = c.getType();
	    	String type = StringUtils.lowerCase(cType.name());  // Channel type key
	    	String typeSuffix = "_" + type + "type";			// ex. _linetype or _plain_texttype, etc...
			String mType = request.getParameter(concatCode + typeSuffix);  // Message type key, ex. answer_DEFAULT_UNKNOWN_linetype
			
			MultiChannelAnswer mca = MultiChannelAnswer.get(t.getId(), id, c.getCode(), ut.getCode());

			String rmMKey = request.getParameter(concatCode + "_mkey");
			String rmMultiRichMessages = request.getParameter(concatCode + "_multiple_richmessages");
			
			if (c.getSupportMultiRichMessages()) {
				if ((type + "_answer_text").equals(mType)) {
					rmMKey = null;
					rmMultiRichMessages = null;
				}
				else if ((type + "_answer_rich_message").equals(mType)) {
					// 套用圖文，答案必須是 {{EMPTY}}，不能是空字串，不然會拉 default 的來用
					// 雖然最終文字不會顯示，但可能會受 INLINE_FUNCTION 影響（如 {{OPTIONS}} ）
					answer = "{{EMPTY}}";
					rmMultiRichMessages = null;
					if (!hasRichMessages.contains(type + " RichMessage")) {
						hasRichMessages.add(type + " RichMessage");
					}
				}
				else {
					// 套用多圖文，答案必須是 {{EMPTY}}，不能是空字串，不然會拉 default 的來用
					// 雖然最終文字不會顯示，但可能會受 INLINE_FUNCTION 影響（如 {{OPTIONS}} ）
					answer = "{{EMPTY}}";
					rmMKey = null;
					if (!hasRichMessages.contains(type + " Multi RichMessages")) {
						hasRichMessages.add(type + " Multi RichMessages");
					}
				}
			}

			if (mca == null) {
				if (QAChannelType.ROBOT == cType) {		
					if (mType.equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)){
						mca = MultiChannelAnswer.save(t.getId(), id, c.getCode(), ut.getCode(), answer, MultiChannelAnswer.ROBOT_ANSWER_ADVANCE, request.getParameter(concatCode + "_voice"), request.getParameter(concatCode + "_mood"));
					} else {
						mca = MultiChannelAnswer.save(t.getId(), id, c.getCode(), ut.getCode(), answer, MultiChannelAnswer.ROBOT_ANSWER_GENERAL, null, null);
					}
				} else if (QAChannelType.PLAIN_TEXT_WITH_VOICE == cType) {
					mca = MultiChannelAnswer.save(t.getId(), id, c.getCode(), ut.getCode(), answer, null, request.getParameter(concatCode + "_voice"), null);
				}
				else if (c.getSupportMultiRichMessages()) {
					MultiChannelAnswerDto dto = new MultiChannelAnswerDtoBuilder().setTenantId(t.getId()).setQaId(id).setChannel(c.getCode()).setUserType(ut.getCode()).setAnswer(answer).setChannelTypeConfig(rmMultiRichMessages).setLineMKey(rmMKey).get();
					mca = MultiChannelAnswer.save(dto);
				}
				else {
					mca = MultiChannelAnswer.save(t.getId(), id, c.getCode(), ut.getCode(), answer);
				}
			}
			else {
				if (QAChannelType.ROBOT == cType) {
					if (mType.equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)){
						mca.setAnswerType( MultiChannelAnswer.ROBOT_ANSWER_ADVANCE);
						mca.setAnswerVoice(request.getParameter(concatCode + "_voice"));
						mca.setAnswerMood(request.getParameter(concatCode + "_mood"));
						mca.setAnswer(answer);
					} else {
						mca.setAnswerType( MultiChannelAnswer.ROBOT_ANSWER_GENERAL);
						mca.setAnswerVoice(null);
						mca.setAnswerMood(null);
						mca.setAnswer(answer);
					}
				} else if (QAChannelType.PLAIN_TEXT_WITH_VOICE == cType) {
					mca.setAnswerVoice(request.getParameter(concatCode + "_voice"));
					mca.setAnswer(answer);
				}
				else if (c.getSupportMultiRichMessages()) {
					mca.setAnswer(answer);
					mca.setRichMessageMKey(rmMKey);
					mca.setChannelTypeConfig(rmMultiRichMessages);
				}
				else {
					mca.setAnswer(answer);
				}
			}
			
			if (QAChannelType.VELOCITY_TEMPLATE == cType) {
				//
			}
			
			if (mca != null) {
				mca.setExtraParameters(ep);
				MultiChannelAnswer.saveOrUpdate(mca);
			}
		}
	}
	
	qa.setOrUpdateField(QA.FN_RICH_MESSAGE_TYPE, hasRichMessages); // 存有哪些問答用到單圖文、多圖文

	if (t.getEnableDebug())
		System.out.println(String.format("%s %d(%s) save QA[KID=%d] data [%s]", now.toString(), user.getId(), user.getLoginName(), qa.getKid(), qaToString(qa)));

	server.add(qa);
	server.commit(true, true, false);

	Thread.sleep(1000); // wait for softCommit
	QAAltBuildQueue.add(t.getId(), id, kid, qa.getQuestionAltTemplates(), user.getLoginName());
			
	if (t.getEnableQaAudit() && qaAudit != null && qaAudit.equals("auditPass")) {
		out.println("deleting [" + QAUtil.DATATYPE_QA_AUDIT + "-" + kid + "].....<BR>");
		server.deleteByQuery("id:" + QAUtil.DATATYPE_QA_AUDIT + "-" + kid);
	}

	result.put("status", "success");
	result.put("kid", "" + kid);
	//response.sendRedirect("qaDataEditor.jsp?id=" + id + "&hideNavBar=" + Boolean.parseBoolean(request.getParameter("hideNavBar")));
	log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
}
catch (Exception e) {
	result.put("status", "error");
	result.put("errorMsg", e.getMessage());
	e.printStackTrace();
	log.setStatusMessage(OperationLogEntity.Status.FAILED);
	log.appendToMoreDetails(e.toString());
}
log.update();
%><%= result %>