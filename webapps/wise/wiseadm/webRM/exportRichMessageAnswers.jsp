<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="java.io.*"
import="java.nio.file.*"
import="java.util.ArrayList"
import="java.util.Base64"
import="java.util.List"
import="org.apache.commons.io.IOUtils"
import="org.apache.wink.json4j.*"
import="org.supercsv.cellprocessor.FmtBool"
import="org.supercsv.cellprocessor.ift.CellProcessor"
import="org.supercsv.cellprocessor.Optional"
import="org.supercsv.io.*"
import="org.supercsv.prefs.CsvPreference"
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.robot.connector.web.RichMessage"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.WiSeEnv"
import="com.intumit.systemconfig.WiseSystemConfig"
%>
<%!
void exportCSV(PrintWriter out, Tenant tenant, Boolean exportImg, Boolean isTW) {
	List<RichMessage> richMessages = new ArrayList<RichMessage>();
	ICsvBeanWriter beanWriter = null;
	ICsvMapWriter mapWriter = null;
	richMessages = RichMessage.list(tenant.getId(), null);
	  
    try {
        // 寫入UTF8 BOM
    	if (!isTW)
    		   out.print('\uFEFF');
    	if (!exportImg) {
	    	beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);
	    	final String[] header = new String[] {"mkey", "msgName", "msgDesc", "msgType", "msgTemplate"};
	    	final CellProcessor[] processors = new CellProcessor[] { new Optional(), new Optional(), new Optional(), new Optional(), new Optional()};
	    	for (final RichMessage richMessage : richMessages) {
	        	beanWriter.write(richMessage, header, processors);
	    	}
    	} else {
    		mapWriter = new CsvMapWriter(out, CsvPreference.EXCEL_PREFERENCE);
	    	final String[] header = new String[] {"mkey", "msgName", "msgDesc", "msgType", "msgTemplate", "img0", "img1", "img2", "img3", "img4", "img5", "img6", "img7", "img8", "img9"};
	    	final CellProcessor[] processors = new CellProcessor[header.length];
	        for (int i = 0; i < header.length; i++) {
	        	processors[i] = new Optional();
	        }

	        Base64.Encoder encoder = Base64.getEncoder();
	        
	        for (final RichMessage richMessage : richMessages) {
	    		Map<String, Object> docMap = new HashMap<String, Object>();
	    		List<RobotImageFile> imgFiles = RobotImageFile.listDeleteBy(tenant.getId(), RichMessage.class.getName(), richMessage.getMkey(), false);
	    		String[] imgBase64Str = new String[imgFiles.size()];
	    		for (int i = 0; i < header.length; i++) {
	    			switch (i) {
	    			case 0:
	    				docMap.put(header[i], richMessage.getMkey());
	    				break;
	    			case 1:
	    				docMap.put(header[i], richMessage.getMsgName());
	    				break;
	    			case 2:
	    				docMap.put(header[i], richMessage.getMsgDesc());
	    				break;
	    			case 3:
	    				docMap.put(header[i], richMessage.getMsgType());
	    				break;
	    			case 4:
	    				docMap.put(header[i], richMessage.getMsgTemplate());
	    				break;
	    			}
		        }
	    		List<String> thumbnailImageUrls = new ArrayList<String>();
	    		if (richMessage.getMsgType().equals("carousel")) {
		    		JSONObject jo = new JSONObject(richMessage.getMsgTemplate());
		    		JSONObject tpl = jo.optJSONObject("template", new JSONObject());
					JSONArray columns = tpl.optJSONArray("columns", new JSONArray());
					for (int i = 0;i < columns.size(); i++) {
						JSONObject column = (JSONObject) columns.get(i);
						thumbnailImageUrls.add(column.optString("thumbnailImageUrl", ""));
					}
	    		}
	    		for (RobotImageFile imgFile : imgFiles) {
	    			if (richMessage.getMsgType().equals("imagemap") && imgFile.getMkey().equals("1040")) {
		    			InputStream imageInFile = imgFile.getFileBody().getBinaryStream();
		    			byte imageData[] = IOUtils.toByteArray(imageInFile);
		    			String fileBody = "data:image/jpg;base64," + encoder.encodeToString(imageData);
		    			docMap.put(header[5], fileBody);
	    			} else if (richMessage.getMsgType().equals("carousel")) {
	    				for (int j = 0; j < thumbnailImageUrls.size(); j++) {
	    					if (thumbnailImageUrls.get(j).endsWith(imgFile.getMkey())) {
	    						InputStream imageInFile = imgFile.getFileBody().getBinaryStream();
	    						byte imageData[] = IOUtils.toByteArray(imageInFile);
	    						String fileBody = "data:image/jpg;base64," + encoder.encodeToString(imageData);
	    						docMap.put(header[j+5], fileBody);
	    						break;
	    					}
	    				}
	    			}
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
boolean isTW = locale.getCountry().equals("TW");
response.setHeader("Content-Type", "text/csv; charset=" + (isTW ? "BIG5" : "UTF-8"));
response.setHeader("Content-Disposition", "inline; filename=" + new Date().getTime() + "_RichMessage-Web.csv");
Boolean exportImg = true;
if (request.getParameter("exportImg") != null) {
	exportImg = Boolean.valueOf(request.getParameter("exportImg"));
}
if ("export".equalsIgnoreCase(request.getParameter("action"))) {
	exportCSV(response.getWriter(), Tenant.getFromSession(session), exportImg, isTW);	
}
%>