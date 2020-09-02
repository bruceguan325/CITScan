<%@ page pageEncoding="UTF-8" language="java"
import="java.util.ArrayList"
import="java.util.List"
import="java.io.Writer"
import="java.io.IOException"
import="org.supercsv.io.*"
import="org.supercsv.cellprocessor.*"
import="org.supercsv.cellprocessor.ift.*"
import="org.supercsv.cellprocessor.constraint.*"
import="org.supercsv.exception.*"
import="org.supercsv.prefs.*"
import="org.supercsv.util.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.synonymKeywords.SynonymKeyword"
import="com.intumit.solr.synonymKeywords.SynonymKeywordFacade"
import="com.intumit.solr.tenant.Tenant"
%>
<%!
void exportCsv(Writer out, Tenant tenant) {
	List<SynonymKeyword> syns = new ArrayList<SynonymKeyword>();
    ICsvBeanWriter beanWriter = null;
    try {
        beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
        // the header elements are used to map the bean values to each column (names must match)
        final String[] header = new String[] { "keyword", "synonymKeyword", "reverse"};
        final CellProcessor[] processors = new CellProcessor[] {
                        new NotNull(), // keyword
                        new Optional(), // synonymKeyword
                        new FmtBool("true", "false") // reverse
                	};
        // write the header
        //beanWriter.writeHeader(header);
		SynonymKeywordFacade facade = SynonymKeywordFacade.getInstance();
		syns = facade.listAll(tenant.getId());
		
        // write the beans
        for( final SynonymKeyword syn : syns ) {
        	// For export, no auto-generated synonyms included.
        	syn.setSynonymKeyword(syn.getSynonymKeywordForEditingOrReading());
            beanWriter.write(syn, header, processors);
        }

    }
    catch(Exception ignore) {
    	ignore.printStackTrace();
    }
    finally {
        if( beanWriter != null ) {
            try {
				beanWriter.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}
%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	return;
}
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
response.setContentType("text/csv");
response.setCharacterEncoding("UTF-8");
response.addHeader("Content-Disposition", "attachment;filename=synonymKeywords.csv");
exportCsv(response.getWriter(), t);
%>