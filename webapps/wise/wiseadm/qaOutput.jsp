
<%@page import="java.io.File"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page import="java.util.zip.ZipOutputStream"%>
<%@page import="java.io.FileWriter"%>
<%@page import="java.io.Writer"%>
<%@page import="java.io.BufferedWriter"%>
<%@page import="java.io.OutputStreamWriter"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="org.supercsv.io.CsvBeanWriter"%>
<%@page import="org.supercsv.io.ICsvBeanWriter"%>
<%@page import="org.supercsv.io.CsvMapWriter"%>
<%@page import="org.supercsv.io.ICsvMapWriter"%>
<%@page import="org.supercsv.prefs.CsvPreference"%>
<%@page import="org.supercsv.cellprocessor.ift.CellProcessor"%>
<%@page import="org.supercsv.cellprocessor.FmtDate"%>
<%@page import="org.supercsv.cellprocessor.Optional"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.io.IOException"%>
<%@page import="com.intumit.quartz.AutoEvaluationJob"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.apache.solr.client.solrj.*"%>
<%@page import="org.apache.solr.client.solrj.response.*"%>
<%@page import="org.apache.solr.common.*"%>
<%@page import="com.intumit.solr.robot.*"%>
<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.intumit.solr.robot.MultiChannelAnswer"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="com.intumit.solr.robot.QAZipFileSetting"%>

<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>



<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>

<%@page import="java.util.Locale"%>
<%@ page import="com.intumit.solr.admin.*" %>

<%
Locale locale= (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) {
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}
%>

<%
Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
//File tmpZipFile = File.createTempFile("zip" + System.currentTimeMillis(), ".zip");
//FileOutputStream tmpZipFileOs = new FileOutputStream(tmpZipFile);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ZipOutputStream zos = new ZipOutputStream(baos);

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
if (t == null || !user.getTenantIdSet().contains(t.getId())) {
	out.println("<h1>550 Permission Denied</h1>");
	return ;
}

SolrServer server = t.getCoreServer4Write();
	
try {
	File qaFile = File.createTempFile("qa_" + System.currentTimeMillis(), ".csv");
	//FileWriter tmpFileWriter = new FileWriter(tmpFile);
	Writer qaFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaFile), "UTF-8"));
	Map<String, Object> docData = qaToCsv(qaFileWriter, server);
	qaFileWriter.close();
	
	File qaCategoryFile = File.createTempFile("qaCategory_" + System.currentTimeMillis(), ".csv");
	Writer qaCategoryFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaCategoryFile), "UTF-8"));
	qaListToCsv(qaCategoryFileWriter, docData, "category", "CATEGORY_ms");
	qaCategoryFileWriter.close();
	
	File qaQuestionAltFile = File.createTempFile("qaQuestionAlt_" + System.currentTimeMillis(), ".csv");
	Writer qaQuestionAltWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaQuestionAltFile), "UTF-8"));
	qaListToCsv(qaQuestionAltWriter, docData, "question_alt", "QUESTION_ALT_TPL_ms");
	qaQuestionAltWriter.close();
	
	File qaBinderFile = File.createTempFile("qaBinderId_" + System.currentTimeMillis(), ".csv");
	Writer qaBinderFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaBinderFile), "UTF-8"));
	qaListToCsv(qaBinderFileWriter, docData, "binderId", "BINDER_ID_ml");
	qaBinderFileWriter.close();
	
	//QAPlugins
	/*
	File qaNonQAPluginsFile = File.createTempFile("qaNonQAPlugins_" + System.currentTimeMillis(), ".csv");
	Writer qaNonQAPluginsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaNonQAPluginsFile), "UTF-8"));
	//qaListToCsv(qaNonQAPluginsWriter, docData, "binderId", "BINDER_ID_ml");
	qaNonQAPluginsWriter.close();
	
	File qaAnswerLinkFile = File.createTempFile("qaAnswerLink_" + System.currentTimeMillis(), ".csv");
	Writer qaAnswerLinkWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaAnswerLinkFile), "UTF-8"));
	//qaListToCsv(qaAnswerLinkWriter, docData, "binderId", "BINDER_ID_ml");
	qaAnswerLinkWriter.close();
	
	File qaAnswerRichtextFile = File.createTempFile("qaAnswerRichtext_" + System.currentTimeMillis(), ".csv");
	Writer qaAnswerRichtextWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaAnswerRichtextFile), "UTF-8"));
	//qaListToCsv(qaAnswerRichtextWriter, docData, "binderId", "BINDER_ID_ml");
	qaAnswerRichtextWriter.close();
	
	File qaHierarchicalFile = File.createTempFile("qaHierarchical_" + System.currentTimeMillis(), ".csv");
	Writer qaHierarchicalWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qaHierarchicalFile), "UTF-8"));
	//qaListToCsv(qaHierarchicalWriter, docData, "binderId", "BINDER_ID_ml");
	qaHierarchicalWriter.close();
	*/
	
	//MultiChannelAnswer
	
	QAChannel.checkData(t.getId());
    QAUserType.checkData(t.getId());
	
	File nonMultiChFile = File.createTempFile("nonMultiCh_" + System.currentTimeMillis(), ".csv");
	Writer nonMultiChWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(nonMultiChFile), "UTF-8"));
	
	File multiChFile = File.createTempFile("multiCh_" + System.currentTimeMillis(), ".csv");
	Writer multiChWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(multiChFile), "UTF-8"));
	multiChToCsv(multiChWriter, nonMultiChWriter, t, docData);
	multiChWriter.close();
	nonMultiChWriter.close();
	
	//String qafileName = "qaOutPut.csv";
	AutoEvaluationJob.addToZipFile(qaFile, QAZipFileSetting.QA_OUTPUT_FILENAME, zos);
	
	//String qaCategoryfileName = "qaCategoryOutPut.csv";
	AutoEvaluationJob.addToZipFile(qaCategoryFile, QAZipFileSetting.QA_Category_OUTPUT_FILENAME, zos);
	
	//String qaQuestionAltfileName = "qaQuestionAltOutPut.csv";
	AutoEvaluationJob.addToZipFile(qaQuestionAltFile, QAZipFileSetting.QA_Question_Alt_OUTPUT_FILENAME, zos);
	
	//String qaBinderfileName = "qaBinderOutPut.csv";
	AutoEvaluationJob.addToZipFile(qaBinderFile, QAZipFileSetting.QA_Binder_OUTPUT_FILENAME, zos);
	
	//String multiChfileName = "multiChOutPut.csv";
	AutoEvaluationJob.addToZipFile(multiChFile, QAZipFileSetting.QA_MultiChannel_OUTPUT_FILENAME, zos);
	
	//String multiChfileName = "nonMultiChOutPut.csv";
	AutoEvaluationJob.addToZipFile(nonMultiChFile, QAZipFileSetting.QA_NonMultiChannel_OUTPUT_FILENAME, zos);
	
}
catch (Exception e) {
	e.printStackTrace();
	LOG.info("qaOutput Exception : " + e.toString() );
}

zos.close();
Calendar cal = Calendar.getInstance();
String date = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

String zipName = "問答管理匯出_" + date + ".zip";
response.setHeader("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(zipName,"UTF-8") + "\"");
response.setContentType("application/zip; charset=UTF-8");
//response.getOutputStream().write(baos.toByteArray());
//response.flushBuffer();
OutputStream output = response.getOutputStream();
output.write(baos.toByteArray(), 0, baos.toByteArray().length);
output.close();
baos.close();
%>

<%!
public Map<String, Object> qaToCsv(Writer out, SolrServer server) throws SolrServerException {
	
	String DATATYPE = QAUtil.DATATYPE_COMMON_SENSE;
	int start = 0;
	int row = 50;
	Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
	Map<String, Object> docData = new HashMap<String, Object>();

	//List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
    //ICsvBeanWriter beanWriter = null;
    ICsvMapWriter mapWriter = null;
    try {
        //beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
        mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);

        // the header elements are used to map the bean values to each column (names must match)
        final String[] header = 
        	new String[] {
        		"id", "kid", "dataType", 
        		"question", "answer", "answerPhone",
        		"extraParams","isBanned","isNotForMLT",
        		"IsNotForSearch","notAppendMLT","expire",
        		"expireDays","enableDt","enableDays",
        		"qa_category","created_day",
        		"createdAdminUserId","updated_day","updatedAdminUserId",
        		//"question_alt","binderId",
        	};
        
        final String[] solrValue = 
           	new String[] {
           		"id", "kid_l", "dataType_s", 
           		"QUESTION_s", "ANSWER_s", "ANSWERPHONE_s",
           		"EXTRA_PARAM_s","IsBanned_b","IsNotForMlt_b",
           		"IsNotForSearch_b","IsNotAppendMlt_b","EXPIRE_dt",
           		"EXPIRE_DAYS_i","ENABLE_dt","ENABLE_DAYS_i",
           		"QA_CATEGORY_s","created_dt",
        		"createdAdminUserId_i","updated_dt","updatedAdminUserId_i",
        		//"QUESTION_ALT_TPL_ms","BINDER_ID_ml",
           	};

        final CellProcessor[] processors =
			new CellProcessor[] {
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),
                   new Optional(),new Optional(),new Optional(),
                   //new Optional(),new Optional(),
           	};
        
        SimpleDateFormat sdf = new SimpleDateFormat(QAUtil.DATE_FORMAT);
        
        // write the header
        mapWriter.writeHeader(header);
        
        while (true) {
	        //LOG.info("beanWriter aaaaaaaaaaaaa" );
	        //System.out.println("beanWriter aaaaaaaaaaaaa");
	    	SolrQuery sqry = new SolrQuery();
	    	sqry.setRequestHandler("/browse");
	    	sqry.setParam("qf", "QUESTION_t^1000000 QUESTION_ALT_mt^0.01");
	    	sqry.setParam("mm", "1");
	    	sqry.setParam("fuzzy", false);
	    	sqry.addFilterQuery("{!tag=DF}dataType_s:" + DATATYPE);
	    	sqry.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]");
	    	sqry.setRows(row);
	    	sqry.setStart(start);
	    	QueryResponse resp = server.query(sqry);
	    	SolrDocumentList docList = resp.getResults();
	    	
	    	LOG.info("qaCsvObject getNumFound : " + docList.getNumFound() );
	    	for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext();) {
	    		SolrDocument doc = docItr.next();
	    		docData.put((String) doc.getFieldValue("id"), doc);
	    		Map<String, Object> docMap = new HashMap<String, Object>();
	    		int vauleCount=0;
	    		for (String keyHeader: header) {
	    			Object value = doc.getFieldValue(solrValue[vauleCount]);
	    			if (value!=null && (solrValue[vauleCount].equals("EXPIRE_dt")||solrValue[vauleCount].equals("ENABLE_dt")) ){
	    				value = sdf.format((Date) value);
	    			}
	    			/*if (value!=null)
	    				LOG.info("solr value : "+solrValue[vauleCount]+" value class name : " + value.getClass().getName() );
	    			if (value!=null && value.getClass().getName().equals("String")) { 
	    				value = URLEncoder.encode((String) value,"UTF-8");
	    				LOG.info("URLEncoder");
	    			}else if (value!=null && value.getClass().getName().equals("java.util.Date")){
	    				value = sdf.format((Date) value);
	    				LOG.info("Date format");
	    			}*/
	    			docMap.put(keyHeader, value);
	    			vauleCount++;
	    		}
	    		mapWriter.write(docMap, header, processors);
	    		//QACsvObject qaCsvObject = new QACsvObject(doc);
	    		
	    		//LOG.info("qaCsvObject Id : " + qaCsvObject.getId() );
	    		//LOG.info("qaCsvObject answer : " + qaCsvObject.getAnswer() );
	    		
	    		//beanWriter.write(qaCsvObject, header, processors);
	    	}
	    	start += row;
	    	if (start>docList.getNumFound()) break;
        }
        
    }
    catch(Exception ignore) {
    	ignore.printStackTrace();
    	LOG.info("qaToCsv ignore : " + ignore.toString() );
    }
    finally {
        if( mapWriter != null ) {
            try {
            	mapWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    return docData;
}
%>
<%!
public void qaListToCsv(Writer out, Map<String, Object> docData, String headName, String solrName) throws SolrServerException {
	Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
	ICsvMapWriter mapWriter = null;
    try {
        //beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
        mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);

        // the header elements are used to map the bean values to each column (names must match)
        final String[] header = 
        	new String[] {
        		"id", "question", headName,
        	};
        
        final CellProcessor[] processors =
			new CellProcessor[] {
                   new Optional(),new Optional(),new Optional(),
           	};
        
        // write the header
        mapWriter.writeHeader(header);
        
        for (Object data : docData.values()) {
        	Map<String, Object> docMap = new HashMap<String, Object>();
        	SolrDocument doc = (SolrDocument) data;
        	
        	ArrayList<String> listData = (ArrayList<String>) doc.getFieldValue(solrName);
        	if (listData==null) continue;
        	
        	docMap.put("id", (String) doc.getFieldValue("id"));
        	String question = (String) doc.getFieldValue("QUESTION_s");
        	//if (question!=null)
        	//	question = URLEncoder.encode(question, "UTF-8");
        	docMap.put("question", question);
        	docMap.put(headName, StringUtils.join(listData, ","));
        	
        	mapWriter.write(docMap, header, processors);
    	}
        
    }
    catch(Exception ignore) {
    	ignore.printStackTrace();
    	LOG.info("qaListToCsv headName : "+ headName+ " ignore : " + ignore.toString() );
    }
    finally {
        if( mapWriter != null ) {
            try {
            	mapWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}
%>
<%!
public void multiChToCsv(Writer multiChOut, Writer nonMultiChOut, com.intumit.solr.tenant.Tenant t, Map<String, Object> docData) throws SolrServerException {
	Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
    ICsvMapWriter multiChMapWriter = null;
    ICsvMapWriter nonMultiChMapWriter = null;
    QAUtil qu = QAUtil.getInstance(t);
    try {
    	multiChMapWriter = new CsvMapWriter(multiChOut, CsvPreference.EXCEL_PREFERENCE);
    	nonMultiChMapWriter = new CsvMapWriter(nonMultiChOut, CsvPreference.EXCEL_PREFERENCE);
    	
        final String[] multiChHeader = 
        	new String[] {
        		"qaId", "qa", "channel", 
        		"userType", "answer",
        	};
        
        final CellProcessor[] multiChProcessors =
			new CellProcessor[] {
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),new Optional(),
           	};
        
        // write the header
        multiChMapWriter.writeHeader(multiChHeader);
        
        final String[] nonMultiChHeader = 
            	new String[] {
            		"qaId", "qa", "channel", 
            		"userType",
            	};
        
        final CellProcessor[] nonMultiChProcessors =
   			new CellProcessor[] {
                   new Optional(),new Optional(),new Optional(),
                   new Optional(),
           	};
        
        // write the header
        nonMultiChMapWriter.writeHeader(nonMultiChHeader);
        
        List<QAChannel> channels = QAChannel.list(t.getId());
        List<QAUserType> userTypes = QAUserType.list(t.getId());
        
        for (Object data : docData.values()) {
        	SolrDocument doc = (SolrDocument) data;
        	
        	
        	String qaId = (String) doc.getFieldValue("id");
        	String qa = (String) doc.getFieldValue("QUESTION_s");
        	
        	for (QAChannel c: channels) {
        		for (QAUserType ut: userTypes) {
        			String answer = MultiChannelAnswer.channelToAnswer(t.getId(), qaId, c.getCode(), ut.getCode());
        			Map<String, Object> multiChMap = new HashMap<String, Object>();
        			
        			if( StringUtils.trimToNull(answer) == null ) {
        	    		multiChMap.put("qaId", qaId);
        	    		multiChMap.put("qa", qa);
        	    		multiChMap.put("channel", c.getCode());
        	    		multiChMap.put("userType", ut.getCode());
        	    		
        	    		nonMultiChMapWriter.write(multiChMap, nonMultiChHeader, nonMultiChProcessors);
        			} else {
        				multiChMap.put("qaId", qaId);
        	    		multiChMap.put("qa", qa);
        	    		multiChMap.put("channel", c.getCode());
        	    		multiChMap.put("userType", ut.getCode());
        	    		multiChMap.put("answer", answer);
        	    		
        	    		multiChMapWriter.write(multiChMap, multiChHeader, multiChProcessors);
        			}
        		}
        	}
    	}
        
        /*List<MultiChannelAnswer> datalist = MultiChannelAnswer.csvOutPutlist(t.getId());
        
        for (MultiChannelAnswer multiObject: datalist) {
        	String qaId = multiObject.getQaId();
        	//Long kid = QAUtil.id2Kid(qaId);
    		//SolrDocument origDoc = qu.getMainQASolrDocument(kid, true);
    		SolrDocument origDoc = (SolrDocument) docData.get(qaId);
    		if (origDoc==null) continue;
    		String qa = (String)origDoc.getFieldValue("QUESTION_s");
    		
    		Map<String, Object> multiChMap = new HashMap<String, Object>();
    		multiChMap.put("qaId", qaId);
    		multiChMap.put("qa", qa);
    		multiChMap.put("channel", multiObject.getChannel());
    		multiChMap.put("userType", multiObject.getUserType());
    		multiChMap.put("answer", multiObject.getAnswer());
    		
    		mapWriter.write(multiChMap, header, processors);
        }*/
    }
    catch(Exception ignore) {
    	ignore.printStackTrace();
    	LOG.info("multiChToCsv ignore : " + ignore.toString() );
    }
    finally {
        if( multiChMapWriter != null ) {
            try {
            	multiChMapWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if( nonMultiChMapWriter != null ) {
            try {
            	nonMultiChMapWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}
%>
