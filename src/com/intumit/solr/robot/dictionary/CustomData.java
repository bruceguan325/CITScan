package com.intumit.solr.robot.dictionary;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.entity.QAEntityType;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;

public class CustomData implements Serializable {
	
	private static final long serialVersionUID = -3978155545936029763L;
	
	public final static String FIXED_CUSTOM_QA_FULL_PARAM = "SRBTCQAFLL";
	public final static String FIXED_CUSTOM_QA_PARTIAL_PARAM = "SRBTCQAPTL";
	
	
	String dataType;
	String name;
	String value;
	QAEntity entityRef;
	
	public CustomData(String dataType, String name, String value) {
		super();
		this.dataType = dataType;
		this.name = name;
		this.value = value.trim();
	}
	
	public CustomData(String dataType, String name, String value, QAEntity entityRef) {
		super();
		this.dataType = dataType;
		this.name = name;
		this.value = value.trim();
		this.entityRef = entityRef;
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
	public QAEntity getEntityRef() {
		return entityRef;
	}
	public void setEntityRef(QAEntity entityRef) {
		this.entityRef = entityRef;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((entityRef == null) ? 0 : entityRef.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CustomData other = (CustomData) obj;
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
		if (entityRef == null) {
			if (other.entityRef != null) return false;
		}
		else if(!entityRef.equals(other.entityRef)) return false;
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
		return "CustomData [dataType=" + dataType + ", name=" + name + ", value=" + value + ", entityRef=" + entityRef + "]";
	}
	public synchronized static Set<CustomData> getAll(Tenant t, Set<CustomData> all, SolrServer server) {
		List<QAEntity> ddList = QAEntity.listByTenantId(t.getId());
		
		for (QAEntity e: ddList) {
			if (e.isEnabled()) {
    			if (e.getFromIndex()) {
    				if (e.getEntityType() == QAEntityType.STRING) {
    					all.addAll(getAllTermsFromSpecificDataType(e, server));
    				}
    				else {
    					System.out.println("The QAEntity[" + e + "] is not a STRING type, won't load into CustomDataDictionary.");
    				}
    			}
    			else if (e.getEntityType() == QAEntityType.STRING) {
            		String[] keywords = StringUtils.split(e.getEntityValues(), "|");
            		
            		for (String keyword: keywords) {
            			CustomData cd = new CustomData(e.getCategory(), e.getCode(), keyword, e);
            			all.add(cd);
            		}
        		}
			}
		}
		
		System.out.println("Got all terms (CustomData).size()=" + all.size());
		return all;
	}
	
	/**
	 * 根據 Entity 設定從索引 load terms， Entity.Category 視為 dataType， Entity.Code 視為欄位名稱
	 * @param e
	 * @param server
	 * @return
	 */
	public static Set<CustomData> getAllTermsFromSpecificDataType(QAEntity e, SolrServer server) {
		Set<CustomData> set = new HashSet<>();
		String dataType = e.getCategory();
		String targetFn = e.getCode();
		
		if (QAUtil.DATATYPE_COMMON_SENSE.equals(dataType) || QAUtil.DATATYPE_CASUAL.equals(dataType)) return set;

		System.out.println("Now processing CustomData [ " + targetFn + " of " + dataType + " ]..");

		List<String> allPossibleTerms = null;
		if (e.getFromIndex()) {
			allPossibleTerms = QAUtil.getAllPossibleFacetTerms(server, targetFn, "dataType_s:" + dataType);
		}
		else {
			allPossibleTerms = Arrays.asList(StringUtils.split(e.getEntityValues(), "|"));
		}
		
		for (String possibleTerm: allPossibleTerms) {
			CustomData cd = new CustomData(dataType, targetFn, possibleTerm, e);
			set.add(cd);
		}

		System.out.println("CustomData [ " + targetFn + " of " + dataType + " ]... Finished");
		return set;
	}
	
	public synchronized static Set<CustomData> getAllJson(Set<CustomData> all, String server, Set<String> allowedFieldNames) {
		String respStr = "";
		JSONObject jobj_fn = null;
		try {
			respStr = WiSeUtils.getDataFromUrl(server);
			jobj_fn = new JSONObject(respStr);
			Iterator itr_fn = jobj_fn.keys();

			while (itr_fn.hasNext()) {
				String fn = (String) itr_fn.next();
				Object obj_dataType = jobj_fn.get(fn);

				if (obj_dataType instanceof JSONObject) {
					JSONObject jobj_dateType = (JSONObject) obj_dataType;
					Iterator itr_dateType = jobj_dateType.keys();

					while (itr_dateType.hasNext()) {
						String dataType = (String) itr_dateType.next();
						JSONArray possibleTerms = jobj_dateType.getJSONArray(dataType);

						for (int i = 0; i < possibleTerms.length(); i++) {

							if (possibleTerms.get(i) instanceof String) {
								String possibleTerm = (String) possibleTerms.get(i);
								if ("id".equalsIgnoreCase(fn) || "dataType_s".equalsIgnoreCase(fn))
									continue;
								if (allowedFieldNames != null && !allowedFieldNames.contains(fn))
									continue;
								if (StringUtils.endsWithAny(fn, new String[] { "_s", "_ms", "_t", "_mt" })) {
									String cleanedFn = fn.replaceAll("_m?[tsifldp]$", "");
									CustomData cd = new CustomData(dataType, cleanedFn, possibleTerm);
									all.add(cd);
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Got all terms (CustomData).size()=" + all.size());
		return all;
	}

}
