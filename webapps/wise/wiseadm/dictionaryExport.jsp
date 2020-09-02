<%@ page pageEncoding="UTF-8" language="java"
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
import="com.intumit.solr.admin.AdminGroup"
import="com.intumit.solr.admin.AdminGroupFacade"
import="com.intumit.solr.robot.dictionary.DictionaryDatabase"
import="com.intumit.solr.tenant.Tenant"
%>
<%!
void exportCSV(Writer out, Tenant tenant) {
    List<DictionaryDatabase> dics = new ArrayList<DictionaryDatabase>();
    ICsvBeanWriter beanWriter = null;
    try {
        beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
        final String[] header = new String[] {"keyword", "purposes", "enabled", "category", "enableQaScopeRestriction"};
        final CellProcessor[] processors = new CellProcessor[] {new Optional(), new Optional(), new FmtBool("true", "false"), new Optional(), new FmtBool("true", "false")};
        dics = DictionaryDatabase.listByTenantId(tenant.getId());
        for (final DictionaryDatabase dic : dics) {
            beanWriter.write(dic, header, processors);
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
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
    return;
}
response.setContentType("text/csv");
response.setCharacterEncoding("UTF-8");
response.addHeader("Content-Disposition", "attachment;filename=DictionaryExport.csv");
exportCSV(response.getWriter(), Tenant.getFromSession(session));
%>