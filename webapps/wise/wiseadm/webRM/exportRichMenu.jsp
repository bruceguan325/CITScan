<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.io.*"
import="java.util.*"
import="org.apache.commons.codec.binary.Base64"
import="org.apache.commons.io.IOUtils"
import="org.supercsv.cellprocessor.FmtBool"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.io.*"
import="org.supercsv.prefs.CsvPreference"
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.robot.connector.web.RichMenu"
import="com.intumit.solr.robot.RobotImageFile"
%>
<%!
void exportCSV(Writer out, Tenant tenant, Boolean exportImg) {
	List<RichMenu> richMenus = new ArrayList<RichMenu>();
	ICsvBeanWriter beanWriter = null;
	ICsvMapWriter mapWriter = null;
	richMenus = RichMenu.list(tenant.getId());
    try {
    	if (!exportImg) {
	    	beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
	    	final String[] header = new String[] {"mkey", "channelCode", "menuSeq", "msgName", "msgDesc", "msgType", "msgTemplate"};
	    	final CellProcessor[] processors = new CellProcessor[] { new Optional(), new Optional(), new Optional(), new Optional(), new Optional(), new Optional(), new Optional() };
	    	for (final RichMenu RichMenu : richMenus) {
	        	beanWriter.write(RichMenu, header, processors);
	    	}
    	} else {
    		mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);
	    	final String[] header = new String[] {"mkey", "channelCode", "menuSeq", "msgName", "msgDesc", "msgType", "msgTemplate", "img0"};
	    	final CellProcessor[] processors = new CellProcessor[header.length];
	        for (int i = 0; i < header.length; i++) {
	        	processors[i] = new Optional();
	        }
	        Base64 base64 = new Base64();
	        for (final RichMenu richMenu : richMenus) {
	    		Map<String, Object> docMap = new HashMap<String, Object>();
	    		List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(tenant.getId(), RichMenu.class.getName(), richMenu.getMkey(), false);
	    		String[] imgBase64Str = new String[imgFiles.size()];
	    		for (int i = 0; i < header.length; i++) {
	    			switch (i) {
	    			case 0:
	    				docMap.put(header[i], richMenu.getMkey());
	    				break;
	    			case 1:
	    				docMap.put(header[i], richMenu.getChannelCode());
	    				break;
	    			case 2:
	    				docMap.put(header[i], richMenu.getMenuSeq());
	    				break;
	    			case 3:
	    				docMap.put(header[i], richMenu.getMsgName());
	    				break;
	    			case 4:
	    				docMap.put(header[i], richMenu.getMsgDesc());
	    				break;
	    			case 5:
	    				docMap.put(header[i], richMenu.getMsgType());
	    				break;
	    			case 6:
	    				docMap.put(header[i], richMenu.getMsgTemplate());
	    				break;
	    			}
		        }
	    		for (RobotImageFile imgFile : imgFiles) {
	    			InputStream imageInFile = imgFile.getFileBody().getBinaryStream();
		    		byte imageData[] = IOUtils.toByteArray(imageInFile);
		    		String fileBody = "data:image/" + (imgFile.getMkey().contains(".png") ? "png" : "jpeg") + ";base64," + base64.encodeToString(imageData);
		    		docMap.put(header[7], fileBody);
	    		}
	    		mapWriter.write(docMap, header, processors);
	    	}
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
%>
<%
Locale locale = Locale.getDefault();
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
response.setHeader("Content-Type", "text/csv; charset=" + (locale.getCountry().equals("TW") ? "BIG5" : "UTF-8"));
response.setHeader("Content-Disposition", "inline; filename=" + new Date().getTime() + "_RichMenu-WEB.csv");
Boolean exportImg = true;
if (request.getParameter("exportImg") != null) {
	exportImg = Boolean.valueOf(request.getParameter("exportImg"));
}
if ("export".equalsIgnoreCase(request.getParameter("action"))) {
	exportCSV(response.getWriter(), Tenant.getFromSession(session), exportImg);
}
%>