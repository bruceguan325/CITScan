<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page pageEncoding="UTF-8" language="java" contentType="application/json"
import="flexjson.*"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
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
import="org.supercsv.io.*"
import="org.supercsv.cellprocessor.*"
import="org.supercsv.cellprocessor.ift.*"
import="org.supercsv.cellprocessor.constraint.*"
import="org.supercsv.exception.*"
import="org.supercsv.prefs.*"
import="org.supercsv.util.*"
import="com.intumit.smartwiki.util.NameValuePair"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.tenant.*"
import="com.intumit.solr.synonymKeywords.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.entity.*"
import="com.intumit.solr.robot.intent.*"
import="com.intumit.solr.robot.qarule.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	return;
}
%><%
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
QAUtil qautil = QAUtil.getInstance(t);

response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");

JSONObject obj = new JSONObject();
String question = StringUtils.lowerCase(StringUtils.trimToEmpty(request.getParameter("q")));
String qaCategory = request.getParameter("qaCategory");
String qaChannel = QAUtil.getStringParameterDefaultIfEmpty(request, "ch", "web");
String altTpls = request.getParameter("altTpls");
Boolean showColor = false;
if(request.getParameter("showColor") != null && !request.getParameter("showColor").equals("")){
	showColor = Boolean.valueOf(request.getParameter("showColor"));
}

Collection<String> expanded = null;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

String checkAndAddToKidStr = request.getParameter("checkAndAddToKid");
String userNlp = request.getParameter("userNlp");
boolean replaceSyn = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("replaceSyn"), "false"));

boolean batch = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("batch"), "false"))
		&& ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A3) > 0);
boolean adv = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("adv"), "false"))
		&& ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A3) > 0);


QAContext qaCtx = new QAContext();
qaCtx.setCurrentQuestion(StringUtils.lowerCase(StringUtils.trimToEmpty(question)));
qaCtx.setClientSupportHtml(false);
qaCtx.setTenant(t);
qaCtx.setQaChannel(qaChannel);
List<String> reconstructed = null;

String cleanedQ = QAUtil.removeSymbols(question, t.getLocale());
String wivo = qautil.doWiVoByRuleConfig(cleanedQ, qaCtx.getQAChannelInstance().getCode());

if (wivo != null) {
	cleanedQ = wivo;
	obj.put("wivo", wivo);
}

if (!batch) {
	List<CustomData> completePairs = new ArrayList<CustomData>();
	
	reconstructed = QAUtil.reconstructQuestion(cleanedQ, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, qautil.getToAnalysis());
	List<String> alts = new ArrayList<String>();
	alts.add(cleanedQ);
	
	for (String tmp: reconstructed) {
		alts.addAll(CustomDataDictionary.expand(t.getId(), tmp.toCharArray(), completePairs));
	}
	
	if (alts.size() > 50) {
		System.out.println("results:" + alts.subList(0, 50) + "..., total alts : " + alts.size());
	} 
	else {
		System.out.println("results:" + alts);
	}
	if (completePairs.size() > 50) {
		System.out.println(completePairs.subList(0, 50) + "..., total completePairs : " + completePairs.size());
	} 
	else {
		System.out.println(completePairs);
	}

	qaCtx.setRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, alts);
	com.intumit.solr.robot.qadialog.QADialog.findTrainedUtterance(qaCtx, null);
	Set<QAIntent> intents = qaCtx.getIntents();
	if (intents == null) intents = new HashSet<QAIntent>();
	Set<QAEntity> entities = qaCtx.getEntities();
	if (entities == null) entities = new HashSet<QAEntity>();

	com.google.common.base.Stopwatch sw = com.google.common.base.Stopwatch.createStarted();
	for (String alt: alts) {
		Set<QAIntent>[] intentsRes = QAIntentDictionary.search(qaCtx.getTenant().getId(), alt.toCharArray(), null);
		Set<QAEntity>[] entitiesRes = QAEntityDictionary.search(qaCtx.getTenant().getId(), alt, null);
		
		for (Set<QAIntent> d: intentsRes) {
			intents.addAll(d);
		}
		for (Set<QAEntity> d: entitiesRes) {
			entities.addAll(d);
		}
	}
	long tc = sw.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS);
	System.out.println("TimeCost for Search Intent / Entity: " + tc + " MILLISECONDS.");
	
	Map<String, QAEntity> map = QAEntity.collToMap(entities, null);
	Set<QAEntity> toBeRemove = new HashSet<QAEntity>();
	
	for (QAEntity ed: entities) {
		if (StringUtils.isNotEmpty(ed.getSubEntities())) {
			for (String subTag: StringUtils.split(ed.getSubEntities(), ",")) {
				if (map.containsKey(subTag)) {
					toBeRemove.add(ed);
				}
			}
		}
	}
	
	for (QAIntent i: intents) {
		if (i.isSystemBuiltIn()) {
			i.setKeywords("");
		}
	}
	
	entities.removeAll(toBeRemove);
	obj.put("intents", new JSONArray(new JSONSerializer().serialize(intents)));
	obj.put("entities", new JSONArray(new JSONSerializer().serialize(entities)));


	/*for (String alt: alts) {
		System.out.println("Test Alt:[" + alt + "]");
		try {
			List<PercolationResult> results = QAUtil.getInstance(t).searchPercolators(alt);
			for (PercolationResult result: results) {
				List<String> tpls = new ArrayList<String>(Arrays.asList(new String[] {result.getOriginalAlt()}));
				List<NameValuePair> uncompleteNVPairs = QADataAggregator.findUncompleteNVPairs(tpls, question, completePairs);
		
				System.out.println(result);
				System.out.println(uncompleteNVPairs);
				System.out.println("****");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	if (adv) {
		if (StringUtils.isNotEmpty(altTpls)) {
			QAAltBuild b = new QAAltBuild();
			b.setExpandSynonyms(false);
			b.setQAltTpls(altTpls);
			b.setTenantId(t.getId());
			QASaver qs = new QASaver(b, new QA());
			expanded = qs.expandAlt(null);
	
			obj.put("altTpls", b.getQAltTpls());
			obj.put("expandAlt", new JSONArray(expanded));
			obj.put("expandES", new JSONArray(qs.expandEssentialKeywords()));
		}
	}

	NlpResult nlpResult = QAUtil.nlp(t, qautil, cleanedQ, locale, 
			replaceSyn ? ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM
					: ReconstructQuestionParameter.DEFAULT_CASUAL_USER_INPUT_PARAM, 
		            TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING,replaceSyn); //.nlpWithColor(qautil, question, locale); //    
	obj.put("nlp", nlpResult.getSegResult());
	
	if (nlpResult.hasEks() && t.getEnableEssentialKeywordMode()) {
		obj.put("eks", new JSONArray(nlpResult.getEks()));
	}		
			
	String nlpColor = "";
	if(showColor){
		nlpColor = QAUtil.nlpWithColor(qautil, question, locale);
	}
	obj.put("nlpColor", nlpColor);

	DictionaryDatabase[] kps = KnowledgePointDictionary.search(t.getId(), cleanedQ.toCharArray(), qaCategory);
	
	if (kps.length > 0) {
		JSONArray kpsArr = new JSONArray(kps);
		obj.put("kps", kpsArr);
	}
	
	if (replaceSyn && checkAndAddToKidStr != null) {
		JSONObject caResult = new JSONObject();
		caResult.put("originalQuestion", question);
		
		Long kid = new Long(checkAndAddToKidStr);
		SolrDocument doc = qautil.getMainQASolrDocument(kid, true);
		SolrDocument auditDoc = qautil.getFilterQASolrDocument(kid, true, QAUtil.DATATYPE_QA_AUDIT, false);
		SolrDocumentList docs = qautil.lowLevelSearchDirectAnswer(question, alts, qaCtx);
		docs.add(doc);
		
		if (doc != null && auditDoc == null) {
			boolean isContains = false;
			Long foundInKid = null;
			String matchedAlt = null;
			QA qa = new QA(doc);
			
			for (SolrDocument chkDoc: docs) {
				QA chkQa = new QA(chkDoc);
				List<String> thisAltTpls = chkQa.getQuestionAltTemplates();
				
				if (userNlp != null && !userNlp.equals("")){ // 強制替換為傳入的 userNlp 當作斷詞結果
					nlpResult.setSegResult(userNlp);
				}
				Sentence sa = new Sentence(nlpResult.getSegResult());
				
				for (String altTpl: thisAltTpls) {
					Sentence sb = new Sentence(QA.parseAndGetQAAltOnly(altTpl));
					
					if (sb.contains(sa)) {
							foundInKid = chkQa.getKid();
						matchedAlt = altTpl;
						isContains = true;
						break;
					}
				}
			}
			
			if (!isContains) {
				List<String> thisAltTpls = qa.getQuestionAltTemplates();
				
				JSONObject append = new JSONObject();
				append.put("testCase", question);
				append.put("testCase.editor", user.getId());
				append.put("alt.editor", user.getId());
				thisAltTpls.add(0, nlpResult + "\t// " + append.toString());
				Date now = new Date();
				qa.setUpdateInfo(now, user);
				qa.setQuestionAltTemplates(thisAltTpls);

				SolrServer server = t.getCoreServer4Write();
				server.add(qa);
				server.commit(true, true, false);
				
				try {
					// wait for softCommit
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				} 
				
				QAAltBuildQueue.add(t.getId(), (String)qa.getFieldValue("id"), kid, qa.getQuestionAltTemplates(), user.getLoginName());

				caResult.put("status", "success");
			}
			else {
				caResult.put("status", "error");
				caResult.put("foundInKid", foundInKid);
				caResult.put("matchedAlt", matchedAlt);
				String altOnly = QA.parseAndGetQAAltOnly(matchedAlt);
				caResult.put("errorMsg", MessageUtil.getMessage(locale, "already.exist") + altOnly);
			}
		}
		else {
			caResult.put("status", "error");
			caResult.put("errorMsg", "Question Not Exist Error or Auditing...");
		}
		
		obj.put("checkAndAddResult", caResult);
	}
	else if (StringUtils.isNotEmpty(question)) {
		obj.put("question", question);
		List<String> aaa = new ArrayList<String>();
	
		if (replaceSyn) {
			reconstructed = new ArrayList<String>();
			reconstructed.addAll(QASaver.quickExpand(nlpResult.getSegResult()));
		}
	
		for (String rec: reconstructed) {
			if (expanded != null && expanded.contains(rec)) {
				System.out.println("Expanded Alts contains reconstructed q [" + rec + "]");
			}
			else {
				System.out.println("Expanded Alts NOT contains reconstructed q [" + rec + "]");
			}
			rec = rec.replaceAll("srbtcptemporal", "{{\\$DATE}}");
			rec = rec.replaceAll("srbtcpnumber", "{{\\$NUM}}");
	
			aaa.add(rec);
			
			if (aaa.size() > 10000) {
				System.out.println("測試「斷1」時產生太多排列組合，僅跑完前10000筆便跳過...問句為「" + question + "」");
				break;
			}
		}
		obj.put(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, new JSONArray(aaa));
	}
}
else {
	List<String> questions = QAUtil.parseMultiValue(cleanedQ);
	
	JSONArray arr = new JSONArray();
	
	for (String q: questions) {
		arr.put(QAUtil.nlp(t, qautil, q, locale, 
				replaceSyn ? ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM
						: ReconstructQuestionParameter.DEFAULT_CASUAL_USER_INPUT_PARAM, 
				TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING,replaceSyn).getSegResult());
	}
	
	obj.put("nlp", arr);
}
%><%= obj %>
