package com.intumit.solr.robot.qadialog;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import morfologik.util.FileUtils;

import org.elasticsearch.common.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.util.WiSeUtils;

public class QADialogTestUtil {
	
	public static String getRemoteAnswer(String host, int port, String apikey, String qaId, String question) {
		String output = null;
		try {
			String url = "http://" + host + ":" + port + "/wise/qa-ajax.jsp?html=false&apikey=" + apikey + "&testMode=true&id=" + qaId + "&q=" + URLEncoder.encode(question, "UTF-8");
			JSONObject res = new JSONObject(WiSeUtils.getDataFromUrl(url));
			output = res.getString("output");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return output;
	}

	public static void main(String args[]) throws Exception {
		HibernateUtil.init();
		
		String host = System.getProperty("host", "127.0.0.1");
		int port = Integer.parseInt(System.getProperty("port", "8080"));
		String apikey = System.getProperty("apikey");
		
		String testCases = System.getProperty("testCases");
		String tagStr = System.getProperty("tags");
		
		Set<String> targetTags = tagStr != null ? new HashSet<String>(Arrays.asList(StringUtils.split(tagStr, ","))) : new HashSet<String>();
		
		String testCasesContent = new String(FileUtils.readFully(QADialogTestUtil.class.getResourceAsStream(testCases)), "UTF-8");
		JSONObject allTc = new JSONObject(testCasesContent);
		JSONArray testGrps = allTc.getJSONArray("testGroups");
		
		for (int g=0; g < testGrps.length(); g++) {
			JSONObject testGrp = testGrps.getJSONObject(g);
			
			boolean enable = testGrp.optBoolean("enabled", Boolean.TRUE);
			Set<String> thisTags = new HashSet<String>();
			JSONArray tags = testGrp.optJSONArray("tags");
			if (tags != null) {
				for (int j=0; j < tags.length(); j++) {
					thisTags.add(tags.getString(j));
				}
			}
			thisTags.retainAll(targetTags);
			if (enable && (targetTags.size() == 0 || thisTags.size() > 0)) {
				System.out.println("=== Start test group [" + testGrp.optString("desc") + "] ===");
				
				JSONArray tests = testGrp.getJSONArray("tests");
				String qaId = ("" + System.currentTimeMillis()) 
								+ Math.round(Math.random() * 100);
				
				for (int i=0; i < tests.length(); i++) {
					JSONObject tc = tests.getJSONObject(i);
					String q = tc.getString("question");
					String output = tc.getString("answer");
					String assertType = StringUtils.trimToNull(tc.optString("assertType"));
					
					String result = getRemoteAnswer(host, port, apikey, qaId, q);
					boolean pass = false;
					output = normalizeText(output);
					result = normalizeText(result);
					
					if (assertType == null || assertType.equalsIgnoreCase("equals")) {
						pass = StringUtils.equals(output, result);
					}
					else if (assertType.equalsIgnoreCase("startsWith")) {
						pass = StringUtils.startsWithIgnoreCase(result, output);
					}
					else {
						System.out.println("Unknown asswertType [" + assertType + "]");
					}
					
					
					
					System.out.print(pass ? "\t[O] " : "\t[X] ");
					
					if (pass) {
						System.out.println(String.format("[%s]=>[%s]", q, result));
					}
					else {
						System.out.println(String.format("Q[%s], expect result [%s] but got [%s]", q, output, result));
/*						System.out.println("================");
						System.out.println(output.replaceAll("[\\s\\n\\r]+", " "));
						System.out.println(result.replaceAll("[\\s\\n\\r]+", " "));
						System.out.println("================");
*/					}
				}
				System.out.println("=== End test group [" + testGrp.optString("desc") + "] ===");
			}
		}
		
		HibernateUtil.shutdown();
	}
	
	static String normalizeText(String orig) {
		return orig.replaceAll("[\\s\\n\\r]+", " ");
	}
}
