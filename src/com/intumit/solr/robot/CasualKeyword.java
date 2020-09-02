package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.intumit.solr.util.WiSeUtils;

public class CasualKeyword implements Serializable {
	public final static String FIXED_CUSTOM_QA_FULL_PARAM = "SRBTCQAFLL";
	public final static String FIXED_CUSTOM_QA_PARTIAL_PARAM = "SRBTCQAPTL";
	
	
	String dataType;
	String name;
	String value;
	public CasualKeyword(String dataType, String name, String value) {
		super();
		this.dataType = dataType;
		this.name = name;
		this.value = value.trim();
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CasualKeyword other = (CasualKeyword) obj;
		if (dataType == null) {
			if (other.dataType != null) return false;
		}
		else if (!dataType.equals(other.dataType)) return false;
		if (name == null) {
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (value == null) {
			if (other.value != null) return false;
		}
		else if (!value.equals(other.value)) return false;
		return true;
	}
	public String toInlineKey() {
		return FIXED_CUSTOM_QA_FULL_PARAM
				+ StringUtils.upperCase(getName().replaceAll("[_/\\s\\$\\+]+", ""))
				+ StringUtils.upperCase(getDataType().replaceAll("[_/\\s\\$\\+]+", ""));
	}
	public static String toInlineKey(String name, String dataType) {
		return FIXED_CUSTOM_QA_FULL_PARAM
				+ StringUtils.upperCase(name.replaceAll("[_/\\s\\$\\+]+", ""))
				+ StringUtils.upperCase(dataType.replaceAll("[_/\\s\\$\\+]+", ""));
	}
	public String toPartialInlineKey() {
		return FIXED_CUSTOM_QA_PARTIAL_PARAM
				+ StringUtils.upperCase(getName().replaceAll("[_/\\s\\$\\+]+", ""))
				+ StringUtils.upperCase(getDataType().replaceAll("[_/\\s\\$\\+]+", ""));
	}
	public static String toPartialInlineKey(String name, String dataType) {
		return FIXED_CUSTOM_QA_PARTIAL_PARAM
				+ StringUtils.upperCase(name.replaceAll("[_/\\s\\$\\+]+", ""))
				+ StringUtils.upperCase(dataType.replaceAll("[_/\\s\\$\\+]+", ""));
	}
	@Override
	public String toString() {
		return "CustomData [dataType=" + dataType + ", name=" + name + ", value=" + value + "]";
	}
	
	static Pattern cleanStart = Pattern.compile("(?im)^(你會|你會不會|你先|你也|你很|你挺|你太|你一點都|你要|你需要|你想要|你覺得|你認為|你想|你為什麼|你能不能|你能|你可以|你可以不可以|你是個|你是不是個)");
	static Pattern cleanAll = Pattern.compile("(?is)(是不是|現在|老是|總是)");
	
	public synchronized static Set<CasualKeyword> getAll(Set<CasualKeyword> all, SolrServer server) {
		int start = 0;
		int rows = 100;
		int count = 0;
		String dataType = QAUtil.DATATYPE_CASUAL;
		
		System.out.println("Now processing CasualKeyword[" + dataType + "]..");
		Set<String> scannedFNs = new HashSet<String>();
		SolrQuery q = new SolrQuery();
		q.setQuery("*:*");
		q.addFilterQuery("dataType_s:" + dataType);
//		q.addFilterQuery("kid_l:[210000 TO 219999]");
		q.setRows(rows).setStart(start);
		
		Set<String> allowedFieldNames = new HashSet<>(Arrays.asList(new String[] {"QUESTION_s", "QUESTION_ALT_ms"}));
		
		try {
			QueryResponse result = server.query(q);
			SolrDocumentList docs = result.getResults();
			
			for (SolrDocument doc: docs) {
				Collection<String> fns = doc.getFieldNames();
				String dt = (String)doc.getFirstValue("dataType_s");
				
				if (dt == null) continue;
				
				for (String fn: fns) {
					if ("id".equalsIgnoreCase(fn) || "dataType_s".equalsIgnoreCase(fn))
						continue;
					
					if (allowedFieldNames != null && !allowedFieldNames.contains(fn))
						continue;
					
					if (StringUtils.endsWithAny(fn, new String[] {"_s", "_ms", "_t", "_mt"})) {
						if (scannedFNs.contains(fn)) continue;
						scannedFNs.add(fn);

						String cleanedFn = fn.replaceAll("_m?[tsifldp]$", "");
						List<String> allPossibleTerms = QAUtil.getAllPossibleFacetTerms(server, cleanedFn, "dataType_s:" + dataType);
						
						for (String possibleTerm: allPossibleTerms) {
							Matcher m = cleanStart.matcher(possibleTerm);
							String cleaned = m.replaceAll("");
							
							if (cleaned.length() <= 1) {
								cleaned = possibleTerm;
							}
							
							possibleTerm = cleaned;
							
							m = cleanAll.matcher(cleaned);
							cleaned = m.replaceAll("");
							
							if (cleaned.length() <= 1) {
								cleaned = possibleTerm;
							}
							
							CasualKeyword cd = new CasualKeyword(dataType, cleanedFn, cleaned);
							all.add(cd);
						}
					}
				}
			}
		}
		catch (SolrServerException e) {
			e.printStackTrace();
		}

		System.out.println("CasualKeyword[" + dataType + "]... Finished");
			
		System.out.println("Got all terms (CasualKeyword).size()=" + all.size());
		return all;
	}
}
