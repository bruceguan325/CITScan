<%@ page pageEncoding="UTF-8" language="java" import="java.util.HashMap"
import="java.util.Map"
import="java.util.ArrayList"
import="java.util.LinkedHashMap"
import="java.util.List"
import="java.util.Locale"
import="java.util.Date"
import="java.io.Writer"
import="java.io.IOException"
import="com.intumit.solr.tenant.Tenant"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.io.CsvMapWriter"
import="org.supercsv.io.ICsvMapWriter"
import="org.supercsv.prefs.CsvPreference"
import="org.apache.solr.common.SolrDocument"
import="org.apache.solr.common.SolrDocumentList"
import="org.apache.solr.client.solrj.SolrServer"
import="org.apache.solr.client.solrj.SolrQuery"
import="com.intumit.solr.admin.*"
import="com.intumit.message.MessageUtil"
import="com.intumit.solr.robot.QAChannel"
import="com.intumit.solr.robot.QAUtil"
import="com.intumit.solr.robot.QAUserType"
import="com.intumit.solr.robot.MultiChannelAnswer"
%>
<%!
private LinkedHashMap<String, String[]> headerMap = null;
private String[] header = null;
private String[] headerKey = null;
private String[] field = null;
private List<QAUserType> userTypes = null;

public void getHeaderMap(Tenant tenant) {
	Locale locale = Locale.getDefault();
  	headerMap = new LinkedHashMap<String, String[]>(); 
  	headerMap.put("id", new String[]{"id", "id"});
  	//知識編號(kid_l)
  	headerMap.put("kid", new String[]{MessageUtil.getMessage(locale, "knowledge.num"), "kid_l"});
  	headerMap.put("question", new String[]{MessageUtil.getMessage(locale, "standard.problem"), "QUESTION_s"});
  	getValue(tenant.getId());
}

public void getValue(int tenantId) {  
	int i = 0;
	int length = headerMap.size();
	String[][] headerValue = new String[length][2];
    
    headerKey = new String[length];
    for (String key : headerMap.keySet()) {
        headerKey[i] = key;
        headerValue[i] = headerMap.get(key);
        i++;
    }
    
    header = new String[length];
    field = new String[length];
    for (int j = 0; j < length; j++) {
    	header[j] = headerValue[j][0];
    	field[j] = headerValue[j][1];
    }
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
    sqry.setRows(3000);
    
    return sqry;
}

void exportQS(Writer out, Tenant tenant) {
	ICsvMapWriter mapWriter = null;
    try {
	    	getHeaderMap(tenant);
	    	com.intumit.solr.robot.QAContext qaCtx = new com.intumit.solr.robot.QAContext();
	    	qaCtx.setClientSupportHtml(false);
		qaCtx.setTenant(tenant);
	    	mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);
	    	String[] header = new String[] { "Id", "Question","Similar_Id","Similar_Question"};
	    	int headerLength = header.length;
        CellProcessor[] processor = new CellProcessor[]{
        		new Optional(), 
                new Optional(),
                new Optional(), 
                new Optional() 
        };
    		mapWriter.writeHeader(header);
    	
        
        SolrQuery q = getQuery();
        SolrServer server = tenant.getCoreServer4Write();
        SolrDocumentList result = server.query(q).getResults();
        int tmp = -1;
        
	    	for (int i = 0, j = result.size(); i < j; i++) {
	    		tmp = i;
	    		Map<String, Object> similarQs = new HashMap<String, Object>();
	    		SolrDocument doc = result.get(i);
	    		Date lastUpdated = null;
	    		String question = (String) doc.getFieldValue("question");
	    		SolrDocumentList docs = QAUtil.getInstance(tenant).mltCommonSense(question, qaCtx, 5);
			if (docs != null) {
				for (SolrDocument doc2: docs) {
					String dId = (String) doc2.getFieldValue("id");
					float score = (Float) doc2.getFieldValue("score");
					if(!dId.equals((String) doc.getFieldValue("id")) && score > 10 ){
						if(tmp==i){
							similarQs.put("Id", doc.getFieldValue("kid"));
							similarQs.put("Question", question);
							tmp=-1;
						}
						else {
							similarQs.put("Id", "");
							similarQs.put("Question", "");
						}
						similarQs.put("Similar_Id", doc2.getFieldValue("kid_l"));
						similarQs.put("Similar_Question", (String) doc2.getFieldValue("QUESTION_s"));
						mapWriter.write(similarQs, header, processor);
					}
				}
			}
	    	}
    }
    catch(Exception ignore) {
	    	ignore.printStackTrace();
    }
    finally {
        if ( mapWriter != null ) {
            try {
	            	mapWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}%>
<%
	if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
			&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName())
					&& 1 == AdminUserFacade.getInstance().getFromSession(session).getId())) {
		out.println("Permission Denied.");
		return;
	}
%>
<%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	response.setContentType("text/csv");
	response.setCharacterEncoding("UTF-8");
	response.addHeader("Content-Disposition", "attachment;filename=similar_question.csv");
	exportQS(response.getWriter(), t);
%>