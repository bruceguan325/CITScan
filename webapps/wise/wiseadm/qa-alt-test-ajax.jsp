<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page pageEncoding="UTF-8" language="java"
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
%><%@ page import="com.intumit.solr.admin.*" %><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	return;
}
%><%!

// 完全複製貼上 (from: qa-nlp-test-ajax.jsp 內的 nlp function)
// 也許未來應該搬到 QAUtil 當中

String nlp(Tenant tenant, QAUtil qautil, String question, Locale locale, boolean replaceSyn) {
	StringBuilder notNM = new StringBuilder();
	
	List<org.ansj.domain.Term> parse = QAUtil.USE_NLPANALYSIS
			? org.ansj.splitWord.analysis.NlpAnalysis.parse(QAUtil.preReconstructClean(" " + question + " ")).getTerms()
			: qautil.getToAnalysis().parseStr(QAUtil.preReconstructClean(" " + question + " ")).getTerms();
	//parse = org.ansj.util.FilterModifWord.modifResult(parse) ;
	boolean allEnglish = true;
	boolean inEnMode = false;
	String englishPhraseStack = "";
	List<String> queue = new ArrayList<String>();

	for (int i=0; i < parse.size(); i++) {
		org.ansj.domain.Term t = parse.get(i);

		System.out.println(t.getName() + ":" + t.getNatureStr());
		
		if ((i == 0 || i+1 == parse.size()) && StringUtils.isBlank(t.getName()))
			continue;
		
		String nature = t.getNatureStr();
		
		if (StringUtils.startsWith(nature, "m") && StringUtils.endsWithAny(t.getRealName(), QAUtil.TEMPORAL_STR_ENDING)) {
			t.setNature(new org.ansj.domain.Nature("t"));
			nature = "t";
		}
		
		if (StringUtils.startsWith(nature, "mt")) {
			// 避免被下一個 elseif 到
		}
		else if (StringUtils.startsWith(nature, "m") && !QAUtil.NOT_A_NUMBER_STR_SET.contains(t.getRealName())) {
			CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER), t.getRealName(), CategorizedKeyValuePair.Type.NUMBER);

			String convertable = CategorizedValueParser.toNumberRangeQuery(kv);

			if (convertable == null) {
				nature = "nm";
			}
		}
		else if (StringUtils.startsWith(nature, "t")) {
			CategorizedKeyValuePair kv = new CategorizedKeyValuePair(CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL), t.getRealName(), CategorizedKeyValuePair.Type.TEMPORAL);
			String convertable = CategorizedValueParser.toDateRangeQuery(kv);

			if (convertable == null) {
				nature = "nt";
			}
		}

		if (StringUtils.equalsIgnoreCase(nature, "en")) {
			if (inEnMode) {
				englishPhraseStack += " " + t.getRealName();
			}
			else {
				inEnMode = true;
				englishPhraseStack = t.getRealName();
			}
			if (i < parse.size() - 1) continue;
		}
		else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"mt"})) {
			t.setRealName("{{$NUM}}|{{$DATE}}|");
		}
		else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"m"})) {
			t.setRealName("{{$NUM}}|");
		}
		else if (replaceSyn && StringUtils.startsWithAny(nature, new String[] {"t"})) {
			t.setRealName("{{$DATE}}|");
		}
		else if (StringUtils.equals(nature, "nr") && " ".equals(t.getRealName())) {
			// ansj 會將空白判斷為 nr（人名），非常詭異，不知道是否是哪裡程式有不小心把空白加入？
			if (i < parse.size() - 1) continue;
		}
		else {
			allEnglish = false;

			if (inEnMode) {
				queue.add(englishPhraseStack);
				inEnMode = false;
			}
		}

		if (!inEnMode && (nature != null && !"null".equals(nature) && StringUtils.startsWithAny(nature, QAUtil.NLP_INCLUDED_NATURES))) {
			String kw = t.getRealName();
	
			try {
				if (i == 0 && kw.matches("^(查詢?|(抱歉)?請?(問|說明)(一下)?)")) {
					kw += "**";
				}
				else if (tenant.isForceIgnoreCharacter(kw)) {
					kw += "**";
				}
			}
			catch (Exception ignore) {}
			
			queue.add(kw);
		}
		else {
			String kw = t.getRealName();
			queue.add(kw + "**");
		}
		if (inEnMode && !(i < parse.size() - 1)) {
			queue.add(englishPhraseStack);
		}

		while (queue.size() > 0) {
			String kw = queue.remove(0);
			if ("nw".equals(nature)) {
				continue;
			}

			boolean replacedBySyn = false;
			if (replaceSyn) {
				if (kw.indexOf("**") != -1) {
					continue;
				}
				
				List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(tenant.getId(), kw);
				
				if (synonymsAll.size() > 0) {
					replacedBySyn = true;
					String newKw = "";
					
					for (SynonymKeyword syn: synonymsAll) {
						if (newKw.length() > 0) {
							newKw += "|";
						}
						
						newKw += syn.getKeyword();
					}
					
					kw = newKw;
				}
			}

			if (notNM.length() > 0) {
				//notNM.append(" / ");
			}

			if (kw.length() > 0) {
				if (replaceSyn) {
					if (kw.length() > 1 || replacedBySyn || StringUtils.startsWith(nature, "#")) {
						notNM.append("(");
						notNM.append(kw);
						notNM.append(")");
					}
					else {
						notNM.append(kw);
					}
				}
				else {
					notNM.append("(");
					notNM.append(kw);// + ":[" + nature + "]");
					notNM.append(")");
				}
			}
		}
	}

	return notNM.toString();
}

%><%
response.addHeader("Cache-Control", "no-cache");
response.addHeader("Expires", "Thu, 01 Jan 1970 00:00:01 GMT");
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

JSONObject obj = new JSONObject();
Long kid = new Long(request.getParameter("kid"));
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
QAUtil qautil = QAUtil.getInstance(t);

String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(request.getParameter("q")));
String suggestQ = t.getEnablePhoneticHomonym() 
					? KnowledgePointDictionary.suggestFullQuestion(t.getId(), tmpQ)
					: null;
String question = suggestQ == null ? tmpQ : suggestQ;


Collection<String> expanded = null;
QA doc = new QA(qautil.getMainQASolrDocument(kid));

boolean replaceSyn = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("replaceSyn"), "false"));

boolean batch = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("batch"), "false"))
				&& (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0);
boolean adv = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("adv"), "false"))
				&& (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0);

QAContext qaCtx = new QAContext();
qaCtx.setCurrentQuestion(StringUtils.lowerCase(StringUtils.trimToEmpty(question)));
qaCtx.setClientSupportHtml(false);
qaCtx.setTenant(t);
List<String> reconstructed = null;

if (!batch) {
	List<CustomData> completePairs = new ArrayList<CustomData>();
	
	reconstructed = QAUtil.reconstructQuestion(question, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, qaCtx, qautil.getToAnalysis());
	List<String> alts = new ArrayList<String>();
	
	for (String tmp: reconstructed) {
		alts.addAll(CustomDataDictionary.expand(t.getId(), tmp.toCharArray(), completePairs));
	}
	
	if(alts.size() > 50){
		System.out.println("results:" + alts.subList(0, 50) + "..., total alts : " + alts.size());
	} else {
		System.out.println("results:" + alts);
	}
	if(completePairs.size() > 50){
		System.out.println(completePairs.subList(0, 50) + "..., total completePairs : " + completePairs.size());
	} else {
		System.out.println(completePairs);
	}

	if (adv) {
	}

	if (StringUtils.isNotEmpty(question)) {
		obj.put("question", question);
		List<String> aaa = new ArrayList<String>();
		
		JSONArray jArr = new JSONArray();
		JSONArray jSimilar = new JSONArray();
		
		// 接著檢查 reconstructed question(s)，看看有沒有整句本身就是可以找到同義詞的
		// 有的話把所有同義詞都加入 reconstructed，因為真正找答案的時候如果完整問句本身就可以查到同義詞，實際上是真的會把所有同義詞拿來 OR
		Set<String> toBeAdd = new HashSet<String>();
		for (String rec: reconstructed) {
			List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listByQuery(t.getId(), rec);
			if (syns != null) {
				for (SynonymKeyword syn: syns) {
					toBeAdd.addAll(syn.getKeywordAndSynonymList());
				}
			}
		}
		reconstructed.addAll(toBeAdd);
		
		// 開始比對每一行問法
		// 這段滿吃效能的，尤其是目標問答是很多種組合的
		// 假設有 N 個組合 + M 個重組的問句 => 最慘的狀況比對次數就是 N*M
		for (String altTpl: doc.getQuestionAltTemplates()) {
			if (StringUtils.isNotEmpty(altTpl)) {
				QAAltBuild b = new QAAltBuild();
				b.setExpandSynonyms(false);
				b.setQAltTpls(altTpl);
				b.setTenantId(t.getId());
				QASaver qs = new QASaver(b, new QA());
				expanded = qs.expandAlt(null);
		
				for (String rec: reconstructed) {
					//rec = rec.replaceAll("(?i)srbtcptemporal", "{{\\$DATE}}");
					//rec = rec.replaceAll("(?i)srbtcpnumber", "{{\\$NUM}}");
					
					aaa.add(rec);
					
					if (expanded != null && expanded.contains(rec)) {
						System.out.println("Expanded Alts contains reconstructed q [" + rec + "], break loop for better performance.");
						
						JSONObject jObj = new JSONObject();
						jObj.put("altTpl", QA.parseAndGetQAAltOnly(altTpl));
						jObj.put("matchedSentence", rec);
						
						jArr.put(jObj);
						break; // 找到一個組合就跳開（為了效能考量）
					}
					else {
						System.out.println("Expanded Alts NOT contains reconstructed q [" + rec + "]");
					}
				}
				
				/* 這邊是用 Levenshtein Distance 找相似句，不過效能更差，只是研究玩玩
				for (String rec: reconstructed) {
					rec = rec.replaceAll("srbtcptemporal", "{{\\$DATE}}");
					rec = rec.replaceAll("srbtcpnumber", "{{\\$NUM}}");
					
					if (expanded != null) {
						for (String exp: expanded) {
							int d = StringUtils.getLevenshteinDistance(exp, rec);
							
							if (d > 0) {
								JSONObject jObj = new JSONObject();
								jObj.put("altTpl", altTpl);
								jObj.put("similarAlt", exp);
								jObj.put("similarSentence", rec);
								jObj.put("distance", d);
								
								jSimilar.put(jObj);
							}
						}
					}
				}
				*/
				
				obj.put("matchedCases", jArr);
				obj.put("similarCases", jSimilar);
				obj.put("altTpls", new JSONArray(doc.getQuestionAltTemplates()));
			}
		}
	
		obj.put(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, new JSONArray(aaa));
	}
}
else {
	List<String> questions = QAUtil.parseMultiValue(question);
	
	JSONArray arr = new JSONArray();
	
	for (String q: questions) {
		arr.put(nlp(t, qautil, q, locale, replaceSyn));
	}
	
	obj.put("nlp", arr);
}
%><%= obj %>
