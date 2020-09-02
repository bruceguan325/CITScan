<%@ page 
	pageEncoding="UTF-8"  
	import="java.util.*"
	import="java.util.regex.*" import="org.apache.solr.core.*"
	import="org.apache.solr.servlet.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
	import="org.apache.solr.common.util.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.httpclient.methods.GetMethod"
	import="org.apache.commons.httpclient.HttpClient"
	import="org.apache.commons.httpclient.auth.AuthScope"
	import="org.apache.commons.httpclient.UsernamePasswordCredentials"
	import="org.dom4j.Document" import="org.dom4j.DocumentHelper"
	import="org.dom4j.Element" 
    import="com.intumit.message.MessageUtil"
	import="com.intumit.solr.*"
	import="com.intumit.solr.util.*" import="com.intumit.solr.dataset.*"
	import="com.intumit.solr.dataset.DataSet.*"
	import="com.intumit.solr.SearchManager"
	import="com.intumit.solr.searchKeywords.*"
	import="com.intumit.solr.config.ColumnNameMappingFacade"
	import="com.intumit.solr.config.ColumnNameMapping"
	language="java" %><%!
static final Pattern breadcrumbPattern1 = Pattern.compile("\\(.*\\)");
static final Pattern breadcrumbPattern2 = Pattern.compile("\\[.*\\]");

java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
java.text.SimpleDateFormat sdfToday = new java.text.SimpleDateFormat("HH:mm");
java.text.DecimalFormat df = new java.text.DecimalFormat("$###,###");
String pricePattern = "(.*?):\\[([0-9\\*]+) TO ([0-9\\*]+)\\]";
String datePattern = "(.*?Date.*?):\\[([^\\s\\]]*) TO ([^\\s\\]]*)\\]";
String fgPattern = "(.*?):\\[([^\\s\\]]* TO [^\\s\\]]*)\\]";

HashMap<String, String> translateMap = new HashMap<String, String>();
{
	translateMap.put("", "(N/A)");
	translateMap.put("PROFESSIONAL", "analytics.questionType.professional");
	translateMap.put("UNKNOWN", "analytics.questionType.unknown");
	translateMap.put("CASUAL", "analytics.questionType.casual");
	translateMap.put("REPEAT_TOO_MANY_TIMES", "analytics.questionType.repeatTooManyTimes");
	translateMap.put("INTERCEPTED", "analytics.questionType.intercepted");
	translateMap.put("BAD_QUESTION", "analytics.questionType.badQuestion");
	translateMap.put("ANSWER_OPTION", "analytics.questionType.answerOption");
	translateMap.put("ANSWER_FORWARD", "analytics.questionType.answerForward");
	translateMap.put("ANSWER_FORWARD_OPTION", "analytics.questionType.answerForwardOption");
	
	translateMap.put("NO_ANSWER", "analytics.answerType.noAnswer");
	translateMap.put("QA_PATTERN", "analytics.answerType.qaPattern");
	translateMap.put("MLT_ONLY", "analytics.answerType.mltOnly");
	translateMap.put("SEARCH", "analytics.answerType.search");
	translateMap.put("STAY_WITH_ROBOT", "analytics.answerType.stayWithRobot");
	translateMap.put("FORWARD", "analytics.answerType.forward");
	// translateMap.put("", "");
}


public String translate(Locale locale, AnalyticsUtil au, FacetQuery[] fqs, String org, boolean includeFieldName) {
	String translated = org;

	if (org == null)
		return "";
	
	if (fqs != null)
	for (FacetQuery fq: fqs) {
		String t = fq.mappingName(org);
		if (t != null)
			return t;
	}

	if (org.matches(fgPattern)) {
		if (org.matches(pricePattern)) {
			Matcher m = Pattern.compile(pricePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String priceFrom = m.group(2);
			String priceTo = m.group(3);

			if ("*".equals(priceFrom)) {
				translated = df.format(Integer.parseInt(priceTo)) + " 以下";
			}
			else if ("*".equals(priceTo)) {
				translated = df.format(Integer.parseInt(priceFrom)) + " 以上";
			}
			else {
				translated = df.format(Integer.parseInt(priceFrom)) + " ~ " + df.format(Integer.parseInt(priceTo));
			}
		}
		if (org.matches(datePattern)) {
			Matcher m = Pattern.compile(datePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String dateFrom = m.group(2);
			String dateTo = m.group(3);

			if (translateMap.containsKey(dateFrom + " TO " + dateTo)) {
				translated = translateMap.get(dateFrom + " TO " + dateTo);
			}
			else {
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm");
				try {
					Calendar calFrom = WiSeUtils.fromSolrDateStr(dateFrom).toCalendar(Locale.TAIWAN);
					//fix -8 hours when Solr display
					calFrom.add(Calendar.HOUR, +8);
					dateFrom = sdf.format(calFrom.getTime());
				}
				catch (Exception ignored) {}
				
				try {
					Calendar calTo = WiSeUtils.fromSolrDateStr(dateTo).toCalendar(Locale.TAIWAN);
					//fix -8 hours when Solr display
					calTo.add(Calendar.HOUR, +8);
					dateTo = sdf.format(calTo.getTime());
				}
				catch (Exception ignored) {}
				translated = (includeFieldName ? "日期：" : "") + dateFrom + " 至 " + dateTo;
			}
				
		}
	}
	else {
		if(StringUtils.isNotBlank(ColumnNameMappingFacade.getInstance().getMappingName(org))){
			translated = ColumnNameMappingFacade.getInstance().getMappingName(org);
		}
		if (translateMap.containsKey(org))
			translated = translateMap.get(org);
	}
	
    if (StringUtils.equals(translated, org)) {
        String targetField = null;
        String criteria = null;
        
        if (StringUtils.indexOf(org, ":") != -1) {
            	targetField = StringUtils.substringBefore(org, ":");
            	criteria = StringUtils.substringAfter(org, ":");
            	targetField = targetField.replaceAll("^\\{[^}]+\\}", "");
        }
        else {
	        	criteria = org;
        }
        
		if (criteria != null) {
            Matcher m = breadcrumbPattern1.matcher(criteria);
            
            if (m.find()) {
            		criteria = m.group();

                if (criteria != null) {
                		criteria = criteria.replace("(", "");
                    criteria = criteria.replace(")", "");
                }
            }
		}

        if (criteria != null) {
            // shellQuery.removeFilterQuery(eachString); //
            // 單獨去掉一個 fq
            criteria = StringUtils.strip(criteria, "\"");
            
            if (targetField != null) {
            		if ("Tag_ms".equals(targetField)) {
            			if (criteria.startsWith("1")) {
            				translated = (includeFieldName ? "專案代號：" : "") + StringUtils.substringAfterLast(criteria, "/");;
            			}
            			else if (criteria.startsWith("2")) {
            				translated = (includeFieldName ? "標籤分類：" : "") + StringUtils.substringAfterLast(criteria, "/");
            			}
            			else if (criteria.startsWith("3")) {
            				translated = (includeFieldName ? "標籤：" : "") + StringUtils.substringAfterLast(criteria, "/");;
            			}
            		}
            		else {
            			translated = (includeFieldName ? translate(locale, au, fqs, targetField, true) + "：" : "") + translate(locale, au, fqs, criteria, false);
            		}
            }
            else {
            		translated = criteria;
            }
        }
    }

	return MessageUtil.returnKeyIfNotExist(locale, translated);
}
%>