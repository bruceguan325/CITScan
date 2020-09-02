<%@ page pageEncoding="UTF-8" language="java"
import="java.io.IOException"
import="java.io.Writer"
import="java.text.SimpleDateFormat"
import="java.util.ArrayList"
import="java.util.Date"
import="java.util.HashMap"
import="java.util.LinkedHashMap"
import="java.util.List"
import="java.util.Locale"
import="java.util.Map"
import="java.util.TreeSet"
import="java.util.Set"
import="org.apache.wink.json4j.*"
import="org.apache.solr.client.solrj.SolrServer"
import="org.apache.solr.client.solrj.SolrQuery"
import="org.apache.solr.common.SolrDocument"
import="org.apache.solr.common.SolrDocumentList"
import="org.apache.commons.lang.StringUtils"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.io.CsvMapWriter"
import="org.supercsv.io.ICsvMapWriter"
import="org.supercsv.prefs.CsvPreference"
import="com.intumit.message.MessageUtil"
import="com.intumit.solr.robot.qaplugin.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.tenant.Tenant"	
import="com.intumit.solr.admin.*"
%>
<%!
public static final String FORMAT = "yyyy/MM/dd HH:mm:ss";

String[] headers = new String[] {"id", "題目", "進入點", "選項編號", "選後行為", "QAID", "問題", "答案", "例句", 
								"腳本", "extraParams", "期限", "下層顯示模式", "下層輸入模式",
								"選項標題 (Lv1)", "選項標題 (Lv2)", "選項標題 (Lv3)", "選項標題 (Lv4)", "選項標題 (Lv5)", "選項標題 (Lv6)", "選項標題 (Lv7)", "選項標題 (Lv8)", "選項標題 (Lv9)", "選項標題 (Lv10)",
								"選項標題 (Lv11)", "選項標題 (Lv12)", "選項標題 (Lv13)", "選項標題 (Lv14)", "選項標題 (Lv15)", "選項標題 (Lv16)", "選項標題 (Lv17)", "選項標題 (Lv18)", "選項標題 (Lv19)", "選項標題 (Lv20)",
								};

public SolrQuery getQuery() {
    SolrQuery sqry = new SolrQuery();
    sqry.setQuery("*:*");
    sqry.setParam("fl", "id,kid_l,QUESTION_s,HIERARCHICAL_QA_s");
    sqry.addFilterQuery("-isPart_i:[2 TO *]");
    sqry.addFilterQuery("ANSWER_PLUGIN_ID_s:3");
    sqry.addFilterQuery("dataType_s:COMMON_SENSE");
    sqry.addSort("kid_l", SolrQuery.ORDER.asc);
    sqry.setRows(3000);
    
    return sqry;
}

public List<Map<String, Object>> toDocMapList(int lv, List<Map<String, Object>> list, JSONObject data, Map<String, Object> docMapBase) {
	list.add(toDocMap(lv, data, docMapBase));
	
	try {
		if (data.has("children")) {
			JSONArray children = data.getJSONArray("children");
			
			for (int i=0; i < children.length(); i++) {
				toDocMapList(lv + 1, list, children.getJSONObject(i), docMapBase);
			}
		}
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	return list;
}

public Map<String, Object> toDocMap(int lv, JSONObject data, Map<String, Object> docMapBase) {
	Map<String, Object> docMap = new HashMap<String, Object>();
	docMap.putAll(docMapBase);
	ParsedOption po = HierarchicalQA.parseOption(data);
	docMap.put("選項編號", po.id);
	docMap.put("選後行為", po.optionAction);
	docMap.put("QAID", po.pipe);
	docMap.put("答案", po.answer);
	docMap.put("問題", po.question);
	docMap.put("例句", po.matchSentences);
	docMap.put("腳本", po.script);
	docMap.put("extraParams", po.extraParams);
	docMap.put("期限", po.expiry);
	docMap.put("下層顯示模式", po.menuView);
	docMap.put("下層輸入模式", po.inputType);
	docMap.put("選項標題 (Lv" + lv + ")", po.title);
	
	return docMap;
}

public void exportHQA(Writer out, Tenant tenant) {
	
	ICsvMapWriter mapWriter = null;
  
	try {
        mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);
        mapWriter.writeHeader(headers);
        
        int headerLength = headers.length;
        CellProcessor[] processor = new CellProcessor[headerLength];
        for (int i = 0; i < headerLength; i++) {
        	processor[i] = new Optional();
        }
        
	    SolrQuery q = getQuery();
	    SolrServer server = tenant.getCoreServer4Write();
	    SolrDocumentList result = server.query(q).getResults();
      
	    SimpleDateFormat sdFormat = new SimpleDateFormat(FORMAT);
	    JSONArray altDataTableArray = null;
	    
	    for (int i = 0; i < result.size(); i++) {
	    	SolrDocument doc = result.get(i);
	    	QA qa = new QA(doc);
		    JSONObject data = HierarchicalQA.getDataFromDocument(doc);
	    	Map<String, Object> docMapBase = new HashMap<String, Object>();
		    int lv = 0;
		    boolean firstDumped = false;
		    String entryPoint = data.optString("entryPoint");
		    docMapBase.put("id", qa.getKid());
		    docMapBase.put("題目", qa.getQuestion());
		    docMapBase.put("進入點", entryPoint);
		    
		    if (data.has("children")) {
		    	JSONArray children = data.getJSONArray("children");
		    	
		    	for (int j=0; j < children.length(); j++) {
		    		JSONObject child = children.getJSONObject(j);
		    		List<Map<String, Object>> docs = toDocMapList(lv + 1, new ArrayList<Map<String, Object>>(), child, new HashMap<String, Object>());
		    		
		    		for (Map<String, Object> docMap: docs) {
		    			if (!firstDumped) {
		    				docMap.putAll(docMapBase);
		    				firstDumped = true;
		    			}
		    	    	mapWriter.write(docMap, headers, processor);
		    		}
		    	}
		    }
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
response.setContentType("text/csv");
response.setCharacterEncoding("UTF-8");
response.addHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode("智能客服階層式問答匯入檔-" + t.getName(), "UTF-8") + "-" + new java.text.SimpleDateFormat("yyyy-MM-DD-HH-mm").format(new java.util.Date()) + ".csv");
exportHQA(response.getWriter(), t);
%>