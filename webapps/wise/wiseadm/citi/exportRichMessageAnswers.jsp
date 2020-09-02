<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.Date"
import="java.util.List"
import="java.util.Map"
import="java.util.Locale"
import="java.io.IOException"
import="java.io.Writer"
import="java.util.ArrayList"
import="java.util.List"
import="org.supercsv.cellprocessor.FmtBool"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.io.CsvBeanWriter"
import="org.supercsv.io.ICsvBeanWriter"
import="org.supercsv.prefs.CsvPreference"
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.robot.connector.citi.RichMessage"
import="com.intumit.citi.CitiDeep"
import="org.supercsv.cellprocessor.StrReplace"
%>
<%!
void exportCSV(Writer out, Tenant tenant) {
	List<RichMessage> richMessages = new ArrayList<RichMessage>();
    ICsvBeanWriter beanWriter = null;
    try {
    	beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
    	final String[] header = new String[] {"priority", "cardType", "title", "reward", "imageUrl", "offerName1", 
              "offerText1", "offerName2", "offerText2", "offerName3", "offerText3", "knowMore", "applyNow", "id"};
    	final CellProcessor[] processors = new CellProcessor[] { new Optional(), new StrReplace(";","\n"), new Optional(), new Optional(), new Optional(),
    	                                                         new Optional(), new Optional(), new Optional(), new Optional(), new Optional(),
    	                                                         new Optional(), new Optional(), new Optional(), new Optional() };
    	richMessages = RichMessage.list(tenant.getId());
    	for (final CitiDeep richMessage : richMessages) {
        	beanWriter.write(richMessage, header, processors);
    	}
    }
    catch(Exception ignore) {
        ignore.printStackTrace();
    }
    finally {
        if(beanWriter != null) {
            try {
                beanWriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
%>
<%
Locale locale = Locale.getDefault();
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
response.setHeader("Content-Type", "text/csv; charset=UTF-8");
response.setHeader("Content-Disposition", "inline; filename=" + new Date().getTime() + "_RichMessage.csv");
exportCSV(response.getWriter(), Tenant.getFromSession(session));
%>