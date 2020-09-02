<%@page import="com.intumit.solr.robot.QAChannelType"%>
<%@page import="com.intumit.solr.robot.QA"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.Set"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="java.util.ArrayList"%>
<%@ page pageEncoding="UTF-8" language="java"
import="org.json.*"
import="java.io.IOException"
import="java.io.Writer"
import="java.text.SimpleDateFormat"
import="java.util.Date"
import="java.util.HashMap"
import="java.util.LinkedHashMap"
import="java.util.List"
import="java.util.Locale"
import="java.util.Map"
import="java.util.regex.*"
import="org.apache.solr.client.solrj.SolrServer"
import="org.apache.solr.client.solrj.SolrQuery"
import="org.apache.solr.common.SolrDocument"
import="org.apache.solr.common.SolrDocumentList"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.io.CsvMapWriter"
import="org.supercsv.io.ICsvMapWriter"
import="org.supercsv.prefs.CsvPreference"
import="com.intumit.message.MessageUtil"
import="com.intumit.solr.robot.MultiChannelAnswer"
import="com.intumit.solr.robot.QAChannel"
import="com.intumit.solr.robot.QAUserType"
import="com.intumit.solr.tenant.Tenant"	
import="com.intumit.solr.admin.*"
import="com.intumit.solr.util.WiSeUtils"
%>
<%!
public static final String FORMAT = "yyyy/MM/dd HH:mm:ss";

private LinkedHashMap<String, String[]> headerMap = null;
private String[] header = null;
private String[] headerKey = null;
private String[] field = null;
private String[] multiCode = null;
private List<QAChannel> channels = null;
private List<QAUserType> userTypes = null;

public void getHeaderMap(Tenant tenant, boolean hasQuestionAlt) {
  
	Locale locale = Locale.getDefault();
  	headerMap = new LinkedHashMap<String, String[]>();
    
  	headerMap.put("id", new String[]{"id", "id"});
  	//知識編號(kid_l)
  	headerMap.put("kid", new String[]{MessageUtil.getMessage(locale, "knowledge.num"), "kid_l"});
  	//分類(QA_CATEGORY_s)
  	headerMap.put("category", new String[]{MessageUtil.getMessage(locale, "category"), "QA_CATEGORY_s"});
  	//標準問題(QUESTION_s)
  	headerMap.put("question", new String[]{MessageUtil.getMessage(locale, "standard.problem"), "QUESTION_s"});
  	//繼承範本
  	headerMap.put("inheritance_alt_template", new String[]{MessageUtil.getMessage(locale, "qa.inheritance"), "InheritantAltTemplate_ms"});
  	//問法(QUESTION_ALT_TPL_ms)
  	if(hasQuestionAlt){
  		headerMap.put("question_alt", new String[]{MessageUtil.getMessage(locale, "qa.alts.template"), "QUESTION_ALT_TPL_ms"});
  	}
  	//例句(QUESTION_ALT_TPL_ms)
  	headerMap.put("question_test_case", new String[]{MessageUtil.getMessage(locale, "qa.alts.testCase"), "QUESTION_ALT_TPL_ms"});
  	//回答(ANSWER_s)
  	headerMap.put("answer", new String[]{MessageUtil.getMessage(locale, "global.answer"), "ANSWER_s"});
  	//外部連結(ANSWER_LINK_s)
  	//headerMap.put("answer_link", new String[]{MessageUtil.getMessage(locale, "external.link"), "ANSWER_LINK_s"});
    //格式文字(ANSWER_RICHTEXT_s)
  	//headerMap.put("answer_richtext", new String[]{MessageUtil.getMessage(locale, "format.text"), "ANSWER_RICHTEXT_s"});
    //階層式問答(HIERARCHICAL_QA_s)
  	//headerMap.put("hierarchical_qa", new String[]{MessageUtil.getMessage(locale, "hierarchical.qa"), "HIERARCHICAL_QA_s"});
    //特殊答案(CUSTOM_QA_ID_s)
  	//headerMap.put("custom_qa_id", new String[]{MessageUtil.getMessage(locale, "custom.qa"), "CUSTOM_QA_ID_s"});
    //建議問法(CUSTOM_QA_SUGGEST_s)
    //headerMap.put("custom_qa_suggest", new String[]{MessageUtil.getMessage(locale, "custom.qa.suggest"), "CUSTOM_QA_SUGGEST_s"});
    //附加參數(EXTRA_PARAM_s)
    //headerMap.put("extra_param", new String[]{MessageUtil.getMessage(locale, "qa.extra.params"), "EXTRA_PARAM_s"});
    //Custom Script(CUSTOM_SCRIPT_s)
  	//headerMap.put("coustom_script", new String[]{"Custom Script", "CUSTOM_SCRIPT_s"});
  	//上架日期(ENABLE_dt)
  	//headerMap.put("enable_time", new String[]{MessageUtil.getMessage(locale, "enable.time"), "ENABLE_dt"});
  	//標籤(CATEGORY_ms)
  	//headerMap.put("label", new String[]{MessageUtil.getMessage(locale, "global.label"), "CATEGORY_ms"});
  	//保存期限(EXPIRE_dt)
  	//headerMap.put("expire", new String[]{MessageUtil.getMessage(locale, "save.time"), "EXPIRE_dt"});
  	//可被推薦(IsNotForMlt_b)
  	//headerMap.put("not_for_mlt", new String[]{MessageUtil.getMessage(locale, "can.recommended"), "IsNotForMlt_b"});
  	//可被搜索(IsNotForSearch_b)
  	//headerMap.put("not_for_search", new String[]{MessageUtil.getMessage(locale, "can.selected"), "IsNotForSearch_b"});
  	//附加推薦題目(IsNotAppendMlt_b)
  	//headerMap.put("not_append_mlt", new String[]{MessageUtil.getMessage(locale, "additional.recommendations"), "IsNotAppendMlt_b"});
  	//KM關聯文件過期註記(IsKmsRelateExpiredMemo_b)
  	//headerMap.put("kms_relate_expired", new String[]{MessageUtil.getMessage(locale, "kms.relate.expired.memo"), "IsKmsRelateExpiredMemo_b"});
  	//啟用(IsBanned_b)
  	//headerMap.put("banned", new String[]{MessageUtil.getMessage(locale, "global.able"), "IsBanned_b"});
  	//最後編輯(updated_dt)
  	headerMap.put("update", new String[]{MessageUtil.getMessage(locale, "last.edit"), "updated_dt"});
  	//被推薦的顯示問題
  	headerMap.put("briefly_question", new String[]{MessageUtil.getMessage(locale, "recommended.display.problem"), QA.FN_BRIEFLY_QUESTION});
  	if (tenant.getEnableMultiLocale()) {
  		//問答語系(QA_LOCALE_ms)
  	    headerMap.put("locales", new String[]{MessageUtil.getMessage(locale, "qa.locales"), "QA_LOCALE_ms"});
  	}
    
  	getValue(tenant.getId());
}

public void getValue(int tenantId) {
	
	channels = QAChannel.list(tenantId);
	userTypes = QAUserType.list(tenantId);
  
	int i = 0;
	int length = headerMap.size();
	int totalLength = length + channels.size() * userTypes.size() + 1 + getAdditionLength(channels);
	String[][] headerValue = new String[length][2];
    
    headerKey = new String[length];
    for (String key : headerMap.keySet()) {
        headerKey[i] = key;
        headerValue[i] = headerMap.get(key);
        i++;
    }
    
    header = new String[totalLength];
    field = new String[length];
    for (int j = 0; j < length; j++) {
    	header[j] = headerValue[j][0];
    	field[j] = headerValue[j][1];
    }
    
    addMultiChannelHeader(length, totalLength);
}

public int getAdditionLength(List<QAChannel> channels){
	int countRobotType = 0;
	
	for (int i = 0; i < channels.size(); i++){
		QAChannel ch = channels.get(i);
		if (QAChannelType.ROBOT == ch.getType()) {
			countRobotType = countRobotType + 2;
		}
	}
	
	return countRobotType;
}

public void addMultiChannelHeader(int length, int totalLength) {
    
    multiCode = new String[totalLength];
    
    for (int i = 0, j = channels.size(); i < j; i++) {
        for (int m = 0, n = userTypes.size(); m < n; m++) {
        	QAChannel ch = channels.get(i);
        	QAUserType ut = userTypes.get(m);
        	header[length] = ch.getName() + "-" + ut.getName();
        	multiCode[length] = ch.getCode() + "-" + ut.getCode();
        	if (QAChannelType.ROBOT == ch.getType()){
        		length++;
        		header[length] = ch.getName() + "-" + ut.getName() + ".voice";
        		multiCode[length] = ch.getCode() + "-" + ut.getCode() + ".voice";
         	}
        	length++;
        }
    }
    
    header[totalLength - 1] = MessageUtil.getMessage(Locale.getDefault(), "multiChannelAnswer.lastUpdated");   //MultiChannelAnswer更新時間
}

public Map<String, MultiChannelAnswer> getMultiChannelAnswer(int tenantId) {
  
    List<MultiChannelAnswer> multiChannelList = MultiChannelAnswer.list(tenantId);
    Map<String, MultiChannelAnswer> multiChannelMap = new HashMap<String, MultiChannelAnswer>();
  
    for (MultiChannelAnswer tmp : multiChannelList) {
    	StringBuilder key = new StringBuilder();
    	key.append(tmp.getChannel()).append("-").append(tmp.getUserType()).append("-").append(tmp.getQaId());
    	multiChannelMap.put(key.toString(), tmp);
    }
    return multiChannelMap;
}

public SolrQuery getQuery() {
	
    StringBuilder fl = new StringBuilder();
	for (int i = 0, j = headerKey.length; i < j; i++) {
	    fl.append(headerKey[i]).append(":").append(field[i]);
	    if (i != j - 1) {
	        fl.append(",");
	    }
    }
    SolrQuery sqry = new SolrQuery();
    sqry.setQuery("*:*");
    sqry.setParam("fl", fl.toString());
    sqry.setParam("wt", "csv");
    sqry.addFilterQuery("-isPart_i:[2 TO *]");
    sqry.addFilterQuery("dataType_s:COMMON_SENSE");
    sqry.addSort("kid_l", SolrQuery.ORDER.asc);
    sqry.setRows(6000);
    
    return sqry;
}

public void exportQA(Writer out, Tenant tenant, boolean hasQuestionAlt) {
	
	ICsvMapWriter mapWriter = null;
  
	try {
        mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);
        getHeaderMap(tenant, hasQuestionAlt);
        mapWriter.writeHeader(header);
        
        int headerLength = header.length;
        CellProcessor[] processor = new CellProcessor[headerLength];
        for (int i = 0; i < headerLength; i++) {
        	processor[i] = new Optional();
        }
        
	    SolrQuery q = getQuery();
	    SolrServer server = tenant.getCoreServer4Write();
	    SolrDocumentList result = server.query(q).getResults();
      
	    System.out.println("####export Size: "+result.size());
	    Map<String, MultiChannelAnswer> multiChannelMap = getMultiChannelAnswer(tenant.getId());
      
	    SimpleDateFormat sdFormat = new SimpleDateFormat(FORMAT);
	    JSONArray altDataTableArray = null;
	    Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
	    for (int i = 0, j = result.size(); i < j; i++) {
	    	Map<String, Object> docMap = new HashMap<String, Object>();
	    	SolrDocument doc = result.get(i);
	        for (int m = 0, n = headerKey.length; m < n; m++) {
	        	// 問法 - 例句(testCase)
	        	if ("question_test_case".equals(headerKey[m])) {
	        		String testCases = "";
	        		ArrayList<String> questionAlt = (ArrayList<String>)doc.getFieldValue(headerKey[m]);
	        		if (questionAlt != null) {
	        			List<String> altTpls = null;
	        			for (int k = 0; k < questionAlt.size(); k++) {
	        				altTpls = QAUtil.parseMultiValue(StringUtils.trimToEmpty(questionAlt.get(k)));
			        		for (String alt: altTpls) {
			        			Map<String, String> altData = QA.parseQAAlt(alt);
			        			String tc = StringUtils.trimToEmpty(altData.get("testCase"));
			        			testCases += tc + "\n";
			        		}
		        			docMap.put(header[m], testCases);
	        			}
	        		}
	        	}
	        	else if ("question_alt".equals(headerKey[m])) { //匯出的時候有些中文會秀出unicode格式，故轉成中文
	        		String altCases = "";
	        		ArrayList<String> questionAlt = (ArrayList<String>)doc.getFieldValue(headerKey[m]);
	        		if (questionAlt != null) {
	        			for (String ac: questionAlt) {
	        				Matcher matcher = pattern.matcher(ac);
	        		        char ch;
	        		        while (matcher.find()) {
	        		            ch = (char) Integer.parseInt(matcher.group(2), 16);
	        		            ac = ac.replace(matcher.group(1), ch+"" );
	        		        }
	        				altCases += ac + "\n";
	        			}
	        		}
	        		docMap.put(header[m], altCases);
	        	}
	        	else if ("answer".equals(headerKey[m])) {
	        		String answer = StringUtils.defaultString((String) doc.getFieldValue(headerKey[m]), "");
	        		answer = WiSeUtils.br2nl(answer).replace("<br />","").replace("&nbsp;"," ").replace("&hearts;","❤");
	        		docMap.put(header[m], answer);
	        	}
	        	//保存期限、上架日期或最後編輯
                else if ("expire".equals(headerKey[m]) || "enable_time".equals(headerKey[m]) || "update".equals(headerKey[m])) {
	        		String dateStr = "";
	        		Date date = (Date) doc.getFieldValue(headerKey[m]);
	        		if (date != null) {
	        			dateStr = sdFormat.format(date);
	        		}
	        		docMap.put(header[m], dateStr);
	        	} else {
	        		docMap.put(header[m], doc.getFieldValue(headerKey[m]));
	        	}
	        }
          
	        Date lastUpdated = null;
          
            for (int m = headerKey.length, n = headerLength - 1; m < n; m++) {
                String key = multiCode[m] + "-" + (String) doc.getFieldValue("id");
                MultiChannelAnswer mca = multiChannelMap.get(key);
                
                if (mca != null) {
                    docMap.put(header[m], mca.getAnswer());
                    if (StringUtils.equalsIgnoreCase(mca.getAnswerType(), MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)){
                    	docMap.put(header[m] + ".voice", mca.getAnswerVoice());
                    }
                
                    if (lastUpdated == null) {
                    	Date tmpUpdated = mca.getLastUpdated();
                    	lastUpdated = (tmpUpdated != null) ? tmpUpdated : new Date();
                    }
                }
            }
            
            if (lastUpdated != null) {
                docMap.put(header[headerLength - 1], sdFormat.format(lastUpdated));
            }
            
	    	mapWriter.write(docMap, header, processor);
	    }
    }
	catch(Exception ignore) {
        ignore.printStackTrace();
    }
    finally {
        if (mapWriter != null) {
            try {
            	mapWriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
%><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E5) == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	out.println("Permission Denied.");
	return;
}
%><%
Tenant t = Tenant.getFromSession(session);
boolean hasQuestionAlt = Boolean.parseBoolean(request.getParameter("qaAlt"));
response.setContentType("text/csv");
response.setCharacterEncoding("UTF-8");
response.addHeader("Content-Disposition", "attachment;filename=KnowledgeExport-" + new java.text.SimpleDateFormat("yyyy-MM-DD-HH-mm").format(new java.util.Date()) + ".csv");
exportQA(response.getWriter(), t, hasQuestionAlt);
%>