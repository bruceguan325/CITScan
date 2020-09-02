<%@ page 
	contentType="text/html;charset=UTF-8" 
    pageEncoding="UTF-8" 
    language="java" 
        
	import="java.util.HashMap"
	import="java.io.File"
	import="java.net.*"
	import="java.sql.*"
	import="java.util.*"
	import="com.intumit.solr.*"
	import="com.intumit.solr.dataimport.*"
	import="com.intumit.solr.util.*"
	import="org.apache.wink.json4j.*"
	import="org.apache.solr.common.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.commons.lang.StringUtils"
	import="org.apache.commons.io.FileUtils"
	import="org.apache.commons.lang.StringEscapeUtils"
	import="java.util.regex.Pattern"
	import="java.util.regex.Matcher" %><%!
	
	Map<String, Object> getExif(String photoId) {
		// http://api.flickr.com/services/rest/?method=flickr.photos.getExif&api_key=420a8f733406d76db842e853962104a8&format=json&nojsoncallback=1&photo_id=8112921034	
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			JSONObject photoData = new JSONObject(WiSeUtils.getDataFromUrl("http://api.flickr.com/services/rest/?method=flickr.photos.getExif&api_key=420a8f733406d76db842e853962104a8&format=json&nojsoncallback=1&photo_id=" + photoId));
			JSONArray exifs = photoData.getJSONObject("photo").getJSONArray("exif");
			
			for (int i=0; i < exifs.length(); i++) {
				JSONObject exifItem = exifs.getJSONObject(i);
				
				String key = exifItem.getString("tag");
				String value = exifItem.getJSONObject("raw").getString("_content");
				
				if (map.containsKey(key)) {
					Object oVal = map.get(key);
					if (oVal instanceof String) {
						List<String> list = new ArrayList<String>();
						list.add((String)oVal);
						list.add(value);
						map.put(key, list);
					}
					else if (oVal instanceof List) {
						((List)oVal).add(value);
					}
				}
				else {
					map.put(key, value);
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}
%>
<%
	String q = StringUtils.trimToNull(request.getParameter("q"));
%>
<form>
<input type="text" name="q">
</form>
<%
if (q != null) {
	SolrServer server = SearchManager.getServer("core0");
	
	SolrQuery sq = new SolrQuery();
	sq.setQuery(q);
	sq.setFields("id", "CreateDate_s");
	sq.setRows(1000000);
	
	java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	SolrDocumentList docList = server.query(sq).getResults();
	
	for (SolrDocument doc: docList) {
		String createDateStr = (String)doc.getFirstValue("CreateDate_s");
		java.util.Date createDate = sdf.parse(createDateStr);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("set", createDate);
		
		SolrInputDocument updateDoc = new SolrInputDocument();
		updateDoc.setField("id", doc.getFieldValue("id"));
		updateDoc.setField("Date_dt", map);
		
		server.add(updateDoc);
	}

	server.commit(true, true, false);
	
	try {
		// wait for softCommit
		Thread.sleep(1000);
	} catch (InterruptedException ignore) {
	} 
}
%>
