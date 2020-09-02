package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.velocity.VelocityContext;

import com.google.common.collect.Lists;
import com.intumit.smartwiki.util.NameValuePair;
import com.intumit.solr.SearchManager;
import com.intumit.solr.qparser.ExtendedDismaxQParserPlugin;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.dictionary.FuzzyCoreUtils;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;

public abstract class QADataAggregator {
	
	private static final String DEFAULT_PACKAGE = "com.intumit.solr.robot.";
	public static final String NO_INSTRUCTION_INDICATOR = "\u0000\u0000";
	private static final String DAMN_HACKING_FOR_CRAPPY_JAVA_REGEX_NAMED_GROUP = "9527";
	
	static Map<String, Class> registeredClass = new HashMap<String, Class>();

	static {
		register(ListDataAggregator.class);
		register(DetailDataAggregator.class);
		register(URLDataAggregator.class);
		register(PeriodDataAggregator.class);
	}

	public static Class findRegisteredClass(String type) {
		if (type.startsWith(DEFAULT_PACKAGE)) {
			type = StringUtils.substringAfter(type, DEFAULT_PACKAGE);
		}
		return registeredClass.get(type);
	}
	
	public static Collection<String> listTypes() {
		return registeredClass.keySet();
	}
	
	public static void register(Class clazz) {
		String clazzName = clazz.getName();
		
		if (clazzName.startsWith(DEFAULT_PACKAGE)) {
			clazzName = StringUtils.substringAfter(clazzName, DEFAULT_PACKAGE);
		}
		registeredClass.put(clazzName, clazz);
	}
	
	public static SolrServer getDataSourceServer(QAPattern.DataSource ds, Tenant t) {
		if (ds == QAPattern.DataSource.LOCAL) {
			return t.getCoreServer();
		}
		else if (ds == QAPattern.DataSource.OPENDATA) {
			String url = t.getOpendataCoreUrl();
			
			if (url == null) {
				throw new RuntimeException("Not allowed to use Opendata");
			}
			
			return SearchManager.getRemoteServer(url);
		}
		
		throw new RuntimeException("Unknown QAPattern.DataSource");
	}
	
	Set<String> currencySet = new HashSet<String>(Lists.newArrayList("美金","美元","usd","us","$","us$","英鎊","gbp","£","港幣","港元","hkd","hk$","澳幣","澳元","aud","澳洲元","aud$","新加坡幣","新加坡元","新幣","新元","星元","星幣","sgd新幣","sgd","s$","瑞士法郎","瑞郎","sf","sfr","chf","加拿大幣","加元","cad","加拿大元","c$","日圓","日幣","日元","jpy","¥","南非幣","瑞典幣","瑞典克朗","sek","kr","泰國銖","泰銖","thb","฿","紐西蘭幣","紐元","nzd","紐西蘭元","nzd$","歐元","eur","€","人民幣","離岸人民幣","cny","cny¥","cnh","rmb","rmb¥","土耳其幣","土耳其里拉","try"));
	Set<String> rangeSet = new HashSet<String>(Lists.newArrayList("以上", "以下", "以內", "超過", "未滿", "不滿"));
	Set<String> periodSet = new HashSet<String>(Lists.newArrayList("月", "天", "年", "季", "旬"));
	public static String replaceCasualText(String r) {
		String tmp = r;
		if(tmp.contains("十個月")){
			tmp = tmp.replace("十個月", "10個月");
		}
		tmp = StringUtils.replaceChars( StringUtils.lowerCase(tmp), "一兩二三四五六七八九十", "12234567891");
		return tmp;
	}
	
	public SolrQuery firstBankSpecial(QAContext qaCtx, SolrQuery sq){
		
		String currencyNmae = "";
		String currentQ = "";
		String range = "";
		boolean specialNext = false;
		int period = 0;

		if (qaCtx.getLastFirstBankSpecialNext().equals("period")) {
			currentQ = qaCtx.getCurrentQuestion() + qaCtx.getLastQuestion();
			if (currentQ.contains("全部")) {
				currentQ = replaceCasualText(currentQ.replace("全部", "1年全部"));
			}
			specialNext = true;
		} else {
			currentQ = replaceCasualText(qaCtx.getCurrentQuestion());
		}
		
		if(qaCtx.getRequestAttribute("INTEREST_RATE") != null){
			currentQ = currentQ.replace(qaCtx.getRequestAttribute("INTEREST_RATE").toString(), "");
		}

		String numS = currentQ.replaceAll("[^0-9]+", "");
		
		if(numS.equals("") && (currentQ.contains("半年") || currentQ.contains("半個月"))) {
			numS = "0.5";
		}else if(numS.equals("")){
			numS = "0";
		}
		
		//find currency and filter
		for (String c : new ArrayList<String>(currencySet)) {
			if (currentQ.contains(c)) {
				if(c.equals("美元") || c.equals("usd") || c.equals("us") || c.equals("$") || c.equals("us$"))
					c = "美金";
				if(c.equals("gbp") || c.equals("£"))
					c = "英鎊";
				if(c.equals("新加坡元") || c.equals("新幣") || c.equals("新元") || c.equals("星元") || c.equals("星幣") || c.equals("sgd新幣") || c.equals("sgd") || c.equals("s$"))
					c = "新加坡幣";
				if(c.equals("港元") || c.equals("hkd") || c.equals("hk$"))
					c = "港幣";
				if(c.equals("澳元") || c.equals("aud") || c.equals("澳洲元") || c.equals("aud$"))
					c = "澳幣";
				if(c.equals("瑞郎") || c.equals("sf") || c.equals("sfr") || c.equals("chf"))
					c = "瑞士法郎";
				if(c.equals("加元") || c.equals("cad") || c.equals("加拿大元") || c.equals("c$"))
					c = "加拿大幣";
				if(c.equals("日幣") || c.equals("日元") || c.equals("jpy") || c.equals("¥"))
					c = "日圓";
				if(c.equals("泰銖") || c.equals("thb") || c.equals("฿"))
					c = "泰國銖";
				if(c.equals("紐元") || c.equals("nzd") || c.equals("紐西蘭元") || c.equals("nzd$"))
					c = "紐西蘭幣";
				if(c.equals("離岸人民幣") || c.equals("cny") || c.equals("cny¥") || c.equals("cnh") || c.equals("rmb") || c.equals("rmb¥"))
					c = "人民幣";
				if(c.equals("瑞典克朗") || c.equals("sek") || c.equals("kr"))
					c = "瑞典幣";
				if(c.equals("eur") || c.equals("€"))
					c = "歐元";
				if(c.equals("土耳其里拉") || c.equals("try"))
					c = "土耳其幣";
				currencyNmae += " " + c;
			}
		}
		for (String c : new ArrayList<String>(rangeSet)) {
			if (currentQ.contains(c)) {
				range = c;
				break;
			}
		}
		if (!currencyNmae.equals("")) {
			sq.addFilterQuery("currency_s:(" + currencyNmae + ")");
		}
		//find range, period and filter
		if (!range.equals("") || specialNext) {
			for (String c : new ArrayList<String>(periodSet)) {
				if (currentQ.contains(c)) {
					Double num = Double.valueOf(numS);
					if (c.equals("年")) {
						period = (int) (num * 365);
					} else if (c.equals("月")) {
						period = (int) (num * 30);
					} else if (c.equals("天")) {
						period = (int) (num * 1);
					} else if (c.equals("季")) {
						period = (int) (num * 90);
					} else if (c.equals("旬")) {
						period = (int) (num * 10);
					}
					break;
				}
			}
		}
		if (period > 0) {
			if (range.equals("以上")) {
				sq.addFilterQuery("day_i:[" + period + " TO 365]");
			} else if (range.equals("超過")) {
				sq.addFilterQuery("day_i:[" + (period + 1) + " TO 365]");
			} else if (range.equals("以下") || range.equals("以內")) {
				sq.addFilterQuery("day_i:[1 TO " + period + "]");
			} else if (range.equals("未滿") || range.equals("不滿")) {
				sq.addFilterQuery("day_i:[1 TO " + (period - 1) + "]");
			}else if(currentQ.contains("1年全部")){
				sq.addFilterQuery("day_i:[1 TO " + period + "]");
			}else{
				sq.addFilterQuery("day_i:[" + period + " TO " + period + "]");
			}
		} else if (period == 0 && !numS.equals("0")) {
			for (String c : new ArrayList<String>(periodSet)) {
				if (currentQ.contains(c)) {
					Double num = Double.valueOf(numS);
					if (c.equals("年")) {
						period = (int) (num * 365);
					} else if (c.equals("月")) {
						period = (int) (num * 30);
					} else if (c.equals("天")) {
						period = (int) (num * 1);
					} else if (c.equals("季")) {
						period = (int) (num * 90);
					} else if (c.equals("旬")) {
						period = (int) (num * 10);
					}
					break;
				}
			}
			sq.addFilterQuery("day_i:[1 TO " + period + "]");
			if(!currentQ.contains("最高") && !currentQ.contains("最好") && !currentQ.contains("最多") &&  !currentQ.contains("最優惠") && !currentQ.contains("最低") && !currentQ.contains("最少")){
				if(currencyNmae.equals("")){
					sq.setRows(15);
				}else{
					sq.setRows(1);
				}
			}
		}
		
		if(qaCtx.getRequestAttribute("INTEREST_RATE") != null){
			String rate = qaCtx.getRequestAttribute("INTEREST_RATE").toString();
			numS = rate;
			Double r = 0.0;
			if(rate.contains("%")){
				r = Double.valueOf(rate.replace("%", ""));
				r = r * 0.01;
			}else{
				r = Double.valueOf(rate);
			}
			sq.addFilterQuery("rate_d:["+r+" TO *]");
		}
		
		if(period == 0 && (currentQ.contains("活期") || currentQ.contains("活存"))){
			sq.addFilterQuery("period_s:活期");
		}

		if (numS.equals("0") && !currentQ.contains("活期") && !currentQ.contains("活存")) {
			qaCtx.setFirstBankSpecialNext("period");
			qaCtx.setAnswerText("請輸入一項您欲查詢的期別，依下列顯示:<br>全部,7天,14天,21天,1個月,3個月,6個月,9個月,1年");
			qaCtx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
		} else {
			qaCtx.setFirstBankSpecialNext("");
		}
		return sq;
	}
	
	public SolrQuery generateQuery(QA customQa, QAPattern qp, QAContext qaCtx, List<CustomData> completePairs, AtomicBoolean autoCompleted) {
		SolrQuery sq = new SolrQuery();
		sq.setRows(qp.getMaxMatched());
		
		if (qp.hasSpecialRestriction()) {
			qp.addSpecialRestriction(customQa, qaCtx, completePairs, sq);
		}
		
		Map<String, Set<NameValuePair>> completeFnToPairMap = new HashMap<String, Set<NameValuePair>>();
		
		for (CustomData cd: completePairs) {
			String key = cd.getName() + ":" + cd.getDataType();
			if (!completeFnToPairMap.containsKey(key)) {
				completeFnToPairMap.put(key, new HashSet<NameValuePair>());
			}
			
			completeFnToPairMap.get(key).add(new NameValuePair(cd.getName(),cd.getValue()));
		}
		
		List<NameValuePair> uncompletePairs = (List<NameValuePair>)qaCtx.getRequestAttribute("uncompleteNVPairs");
		Map<String, Set<NameValuePair>> uncompleteFnToPairMap = new HashMap<String, Set<NameValuePair>>();
		if (uncompletePairs != null) {
			qaCtx.appendExplain(this.getClass().getName(),
					String.format("Found uncompleteNVPairs (%s, %s, %s)",
							qp, qaCtx.getCurrentQuestion(), uncompletePairs));
			System.out.println("uncompleteNVPairs:" + uncompletePairs);
			
			for (NameValuePair nv: uncompletePairs) {
				if (!uncompleteFnToPairMap.containsKey(nv.getName())) {
					uncompleteFnToPairMap.put(nv.getName(), new HashSet<NameValuePair>());
				}
				
				uncompleteFnToPairMap.get(nv.getName()).add(nv);
			}
		}

		String qstr = "";
		String bq = "";
		String fq = "";
		int mm = 0;
		boolean allTermMatch = true;
		
		if (customQa != null) {
			List<String> targetFns = getFieldNamesInQuestion(customQa.getQuestionAltTemplates().toArray(new String[0]));
			List<String> mustFns = getMustHaveFieldNamesInQuestion(customQa.getQuestionAltTemplates().toArray(new String[0]));
	
			for (String fnWithDT: completeFnToPairMap.keySet()) {
				if (mustFns.contains(fnWithDT) || targetFns.contains(fnWithDT)) {
					if (qstr.length() > 0) {
						qstr += " ";
						bq += " ";
					}
					
					StringBuffer valBuf = new StringBuffer();
					
					for (NameValuePair nv: completeFnToPairMap.get(fnWithDT)) {
						
						if (valBuf.length() > 0) {
							valBuf.append(" ");
						}
						valBuf.append(StringUtils.contains(nv.getValue().trim(), " ") ? WiSeUtils.dblQuote(nv.getValue()) : nv.getValue());
					}
					
					mm++;
					qstr += valBuf.toString();
					String fn = fnWithDT.replaceAll("\\:.*$", "");
					bq += fn + "_s^5 " + fn + "_t^0.1 " 
						+ fn + "_ms^5 " + fn + "_mt^0.1";
					
				}
			}
			
			for (String fnWithDT: uncompleteFnToPairMap.keySet()) {
				if (targetFns.contains(fnWithDT)) {
					if (qstr.length() > 0) {
						qstr += " ";
						bq += " ";
					}
					
					StringBuffer valBuf = new StringBuffer();
					
					for (NameValuePair nv: uncompleteFnToPairMap.get(fnWithDT)) {
						if (valBuf.length() > 0) {
							valBuf.append(" ");
						}
						StringBuffer nvResult = new StringBuffer();
						AtomicBoolean autoCompleteThisTime = new AtomicBoolean(false);
						allTermMatch &= processFuzzyQuery(qaCtx.getTenant(), nv, nvResult, autoCompleteThisTime);
						valBuf.append(nvResult.toString());
						autoCompleted.set(autoCompleted.get() || autoCompleteThisTime.get());
					}
					mm++;
					qstr += valBuf.toString();
					String fn = fnWithDT.replaceAll("\\:.*$", "");
					bq += fn + "_s^5 " + fn + "_t^0.1 " 
						+ fn + "_ms^5 " + fn + "_mt^0.1";
					
				}
			}
			
		}
		
		
		if (StringUtils.isNotEmpty(fq)) {
			sq.setFilterQueries(fq);
		}	
		
		if(StringUtils.isNotBlank(qstr)) {
			qstr = "(" + qstr + ")";
		}
		
		if(allTermMatch) {
			mm = StringUtils.split(qstr, " ").length;
		}
		
		if (mm > 0) {
			sq.setQuery(qstr);
			sq.setParam("qf", bq);
			sq.setRequestHandler("/browse")
				.setParam("enableElevation", true)
				.setParam("forceElevation", true)
				.setParam("fuzzy", false)
				.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
				.setParam(ExtendedDismaxQParserPlugin.SYNONYM, true)
				.setParam(ExtendedDismaxQParserPlugin.TENANT_ID, "" + qp.getTenantId())
				.setParam("mm", allTermMatch ? "" + mm : "90%");
			if (customQa.getFieldValues("id").toArray()[0].toString().contains("COMMON_SENSE-999999")) {
				firstBankSpecial(qaCtx, sq);
			}
		}
		else {
			sq.setQuery("*:*")
				.setParam("qf", bq)
				.setRequestHandler("/browse")
				.setParam("enableElevation", true)
				.setParam("forceElevation", true)
				.setParam("fuzzy", false)
				.setParam(ExtendedDismaxQParserPlugin.SEGMENT, false)
				.setParam(ExtendedDismaxQParserPlugin.SYNONYM, false)
				;

			if (customQa.getFieldValues("id").toArray()[0].toString().contains("COMMON_SENSE-999999")) {
				firstBankSpecial(qaCtx, sq);
			}
		}
		
		if (qaCtx.getTenant().getEnableQAExplain()) {
			qaCtx.appendExplain(this.getClass().getName(),
				String.format("QAOutputTemplate.generateQuery(%s, %s, %s, %s)",
						qp, qaCtx.getCurrentQuestion(), completePairs, sq ));
		}

		return sq;
	}
	
	private static boolean processFuzzyQuery(Tenant t, NameValuePair nv, StringBuffer nvResult, AtomicBoolean autoCompleteThisTime) {
		QAUtil qaUtil = QAUtil.getInstance(t.getId());
		String value = preProcess(nv.getValue());
		String afterNlp = QAUtil.simpleNlp(qaUtil, value, false);
		if(StringUtils.isNotBlank(afterNlp)) {
			afterNlp = afterNlp.substring(1, afterNlp.length() - 1);
		}
		boolean hasCombine = false;
		String[] singleTerms = StringUtils.split(afterNlp, ")(");
		List<String> result = new ArrayList<String>();
		for(String term : singleTerms) {
			if(StringUtils.length(term) > 1) {
				if(result.size() > 0 && StringUtils.length(result.get(result.size() - 1)) == 1) {
					// 只發生在第一個詞是單詞時才會有此情形
					result.set(result.size() - 1, result.get(result.size() - 1) + term);
					hasCombine = true;
				}
				result.add(term);
			}
			else {
				String combine = term;
				if(!result.isEmpty()) {
					hasCombine = true;
					combine = result.get(result.size() - 1) + term;
				}
				result.add(combine);
			}
		}
		
		String query = StringUtils.join(result, " ");
		
		if(nv.isEmbeddedFuzzy()) {
			query = FuzzyCoreUtils.getInstance(t.getId()).autoComplete(query, nv.getEmbeddedType(), autoCompleteThisTime);
		}
		nvResult.append(query);
		return !hasCombine;
	}
	
	private static String preProcess(String value) {
		String clone = value;
		clone = StringUtils.replaceChars( StringUtils.lowerCase(value), "123456789臺", "一二三四五六七八九台");
		return clone;
	}

	public static List<String> getFieldNamesInQuestion(String... questionAltTpls) {
		Pattern p = Pattern.compile("\\{\\{\\$\\+?(.*?)\\}\\}");
		List<String> fns = new ArrayList<String>();
		
		for (String questionAltTpl: questionAltTpls) {
			Matcher m = p.matcher(questionAltTpl);
			
			while (m.find()) {
				String fn = m.group(1).replaceAll("[\\s\\$\\+]+", "");
				if (!fns.contains(fn))
					fns.add(fn);
				
				if(StringUtils.countMatches(fn, ":") > 1) {
					String fnNoEmbedded = StringUtils.substringBeforeLast(fn, ":");
					if (!fns.contains(fnNoEmbedded))
						fns.add(fnNoEmbedded);
				}
			}
		}
		return fns;
	}

	public static List<String> getMustHaveFieldNamesInQuestion(String... questionAltTpls) {
		Pattern p = Pattern.compile("\\{\\{\\$\\+(.*?)\\}\\}");
		List<String> fns = new ArrayList<String>();
		
		for (String questionAltTpl: questionAltTpls) {
			Matcher m = p.matcher(questionAltTpl);
			
			while (m.find()) {
				String fn = m.group(1).replaceAll("[\\s\\$\\+]+", "");
				
				if (!fns.contains(fn))
					fns.add(fn);
			}
		}
		return fns;
	}
	
	public static List<String> getEmbeddedFieldNamesInQuestion(String... questionAltTpls) {
		Pattern p = Pattern.compile("\\{\\{\\$\\+(.*?)\\}\\}");
		List<String> fns = new ArrayList<String>();
		
		for (String questionAltTpl: questionAltTpls) {
			Matcher m = p.matcher(questionAltTpl);
			
			while (m.find()) {
				String fn = m.group(1);
				if(StringUtils.countMatches(fn, ":") < 2) continue;
				fn = fn.replaceAll("[\\s\\$\\+]+", "");
				if (!fns.contains(fn))
					fns.add(fn);
			}
		}
		return fns;
	}

	/**
	 * 作法是將完整的 nv pair 直接從 question template 當中還原成真正的 value
	 * 剩下的 %{XXXX} 代表著是沒被比對到的區塊，試著找出使用者問句當中對應的區塊的文字片段，然後放在另一個 nv pair 裡頭回傳
	 * 
	 * P.S. 這裡不會特別排除 %{+XXXX} ，意思是 %{+XXXX} 的還是會被試圖找出來
	 * 
	 * @param question
	 * @param completePairs
	 * @return
	 */
	public static List<NameValuePair> findUncompleteNVPairs(List<String> questionAltTpls, String question, List<CustomData> completePairs) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		
		for (String tpl: questionAltTpls) {
			List<String> fieldNames = getFieldNamesInQuestion(tpl);
			List<String> mustHaveFieldNames = getMustHaveFieldNamesInQuestion(tpl);
			Set<String> replacedRegexPatternFNs = new HashSet<String>();
			
			for (CustomData nv: completePairs) {
				if (fieldNames.remove(nv.getName() + ":" + nv.getDataType())) {
					tpl = replaceCompletePart(tpl, nv, mustHaveFieldNames.contains(nv.getName()));
				}
			}
			
			for (String restOfFn: fieldNames) {
				tpl = replaceWithRegexPattern(tpl, restOfFn, replacedRegexPatternFNs, mustHaveFieldNames.contains(restOfFn));
			}
			
			if(StringUtils.endsWith(tpl, ">.*?)")) tpl += "$";
	
			System.out.println("The Uncomplete Pair Regex Pattern:[" + tpl + "]");
			try {
				Pattern p = Pattern.compile(tpl);
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					for (String restOfFn: fieldNames) {
						if (replacedRegexPatternFNs.contains(restOfFn)) {
							String val = m.group(restOfFn.replaceAll("[_\\:]", DAMN_HACKING_FOR_CRAPPY_JAVA_REGEX_NAMED_GROUP));
							if (StringUtils.trimToNull(val) != null) {
								pairs.add(new NameValuePair(restOfFn, val));
							}
						}
					}
				}
			}
			catch (Exception ignore) {
				System.out.println("Cannot compile Uncomplete pattern, maybe the question already has COMPLETE term...." + ignore.getMessage());
			}
		}
		return pairs;
	}

	/**
	 * 將沒比對到的欄位%{XXXX}，取代成 regex 語法，之後用來查出使用者問題當中該區段的實際文字
	 * 
	 * @param tpl
	 * @param fn
	 * @param  
	 * @return
	 */
	private static String replaceWithRegexPattern(String tpl, String fn, Set<String> replacedFNs, boolean isMustHave) {
		if (isMustHave && tpl.indexOf("{{$+" + fn + "}}") != -1) {
			replacedFNs.add(fn);
			return StringUtils.replace(tpl, "{{$+" + fn + "}}", "(?<" + fn.replaceAll("[\\:_]", DAMN_HACKING_FOR_CRAPPY_JAVA_REGEX_NAMED_GROUP) + ">.*?)");
		}
		
		if (tpl.indexOf("{{$" + fn + "}}") != -1) {
			replacedFNs.add(fn);
			return StringUtils.replace(tpl, "{{$" + fn + "}}", "(?<" + fn.replaceAll("[\\:_]", DAMN_HACKING_FOR_CRAPPY_JAVA_REGEX_NAMED_GROUP) + ">.*?)");
		}
		return tpl;
	}

	private static String replaceCompletePart(String tpl, CustomData cd, boolean isMustHave) {
		if (isMustHave)
			return StringUtils.replace(tpl, "{{$+" + cd.getName() + ":" + cd.getDataType() + "}}", cd.getValue());
		return StringUtils.replace(tpl, "{{$" + cd.getName() + ":" + cd.getDataType()  + "}}", cd.getValue());
	}
	
	abstract public void aggregate(QA customQa, QAContext qaContext, QAPattern qp, List<CustomData> nvPairs, VelocityContext context);
}
