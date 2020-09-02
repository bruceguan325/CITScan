package com.intumit.solr.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;

import flexjson.JSONSerializer;

@Entity
public class DataSet implements Serializable {
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	private String name;
	private String coreName;
	
	@Column(length = 32)
	private String nameField;
	
	@Column(length = 512)
	private String bodyFields;
	
	@Lob
	private String filters;
	
	@Lob
	private String facetQueries;
	
	@Column(length = 512)
	private String facets;
	
	@Column(length = 32)
	private String defaultSort;
	
	@Column(length = 512)
	private String sortables;
	
	@Column(length = 512)
	private String fieldWeight;
	
	@Column(length = 512)
	private String fieldMltName1;
	
	@Column(length = 512)
	private String fieldMltName2;
	
	@Column(length = 512)
	private String fieldMltName3;
	
	@Column(length = 512)
	private String fieldMltValue1;
	
	@Column(length = 512)
	private String fieldMltValue2;
	
	@Column(length = 512)
	private String fieldMltValue3;
	
	@Column(length = 512)
	private String fieldHighlight;

	@Column(length = 512)
	private String fieldWiki;
	
	private Long fieldWikiCount;

	@Column(length = 512)
	private String fieldMlt;
	
	private Long fieldMltCount;
	
	private Boolean rss;
	
	private Boolean fuzzySearch;

	@Column(length = 512)
	private String fuzzyField;
	
	@Column(length = 512)
	private String fuzzyFieldWeight;
	
	@Column(length = 512)
	private String advancedDateField;
	
	private String displayRows;
	
	private Boolean enable;
	
	private Boolean visible;

	@Column(length = 512)
	private String queryBoost;

	@Column(length = 512)
	private String queryBoostMultiply;
	
	@Transient
	private FacetQuery[] fq = null;
	
	/**
	 * 各資料組呈現順序
	 */
	private Long dsOrder;

	public Long getDsOrder() {
		return dsOrder;
	}

	public void setDsOrder(Long dsOrder) {
		this.dsOrder = dsOrder;
	}

	public String getFieldHighlight() {
		return fieldHighlight;
	}

	public void setFieldHighlight(String fieldHighlight) {
		this.fieldHighlight = fieldHighlight;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCoreName() {
		return coreName;
	}

	public void setCoreName(String coreName) {
		this.coreName = coreName;
	}

	public String getNameField() {
		return nameField;
	}

	public void setNameField(String nameField) {
		this.nameField = nameField;
	}

	public String getBodyFields() {
		return bodyFields;
	}

	public void setBodyFields(String bodyFields) {
		this.bodyFields = bodyFields;
	}

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}

	public String getFacets() {
		return facets;
	}

	public void setFacets(String facets) {
		this.facets = facets;
	}

	public String getDefaultSort() {
		return defaultSort;
	}

	public void setDefaultSort(String defaultSort) {
		this.defaultSort = defaultSort;
	}

	public String getSortables() {
		return sortables;
	}

	public void setSortables(String sortables) {
		this.sortables = sortables;
	}
	
	public String processMlt() {
		String mltString = "";
		if (!this.fieldMltName1.isEmpty()) {
			mltString += this.fieldMltName1;
			if (this.fieldMltValue1.isEmpty()) {
				mltString += "^1.0 ";
			} else {
				mltString += "^" + this.fieldMltValue1 + " ";
			}
		}
		
		if (!this.fieldMltName2.isEmpty()) {
			mltString += this.fieldMltName2;
			if (this.fieldMltValue2.isEmpty()) {
				mltString += "^1.0 ";
			} else {
				mltString += "^" + this.fieldMltValue2 + " ";
			}
		}
		
		if (!this.fieldMltName3.isEmpty()) {
			mltString += this.fieldMltName3;
			if (this.fieldMltValue3.isEmpty()) {
				mltString += "^1.0 ";
			} else {
				mltString += "^" + this.fieldMltValue3 + " ";
			}
		}
		
		return mltString;
	}
	
	public String getFieldWeight() {
		return fieldWeight;
	}

	public void setFieldWeight(String fieldWeight) {
		this.fieldWeight = fieldWeight;
	}

	public String getFieldMltName1() {
		return fieldMltName1;
	}

	public void setFieldMltName1(String fieldMltName1) {
		this.fieldMltName1 = fieldMltName1;
	}

	public String getFieldMltName2() {
		return fieldMltName2;
	}

	public void setFieldMltName2(String fieldMltName2) {
		this.fieldMltName2 = fieldMltName2;
	}

	public String getFieldMltName3() {
		return fieldMltName3;
	}

	public void setFieldMltName3(String fieldMltName3) {
		this.fieldMltName3 = fieldMltName3;
	}

	public String getFieldMltValue1() {
		return fieldMltValue1;
	}

	public void setFieldMltValue1(String fieldMltValue1) {
		this.fieldMltValue1 = fieldMltValue1;
	}

	public String getFieldMltValue2() {
		return fieldMltValue2;
	}

	public void setFieldMltValue2(String fieldMltValue2) {
		this.fieldMltValue2 = fieldMltValue2;
	}

	public String getFieldMltValue3() {
		return fieldMltValue3;
	}

	public void setFieldMltValue3(String fieldMltValue3) {
		this.fieldMltValue3 = fieldMltValue3;
	}
	
	public String getFieldWiki() {
		return fieldWiki;
	}

	public void setFieldWiki(String fieldWiki) {
		this.fieldWiki = fieldWiki;
	}

	public String getFieldMlt() {
		return fieldMlt;
	}

	public void setFieldMlt(String fieldMlt) {
		this.fieldMlt = fieldMlt;
	}
	
	public Long getFieldMltCount() {
		return fieldMltCount;
	}

	public void setFieldMltCount(Long fieldMltCount) {
		this.fieldMltCount = fieldMltCount;
	}

	public Long getFieldWikiCount() {
		return fieldWikiCount;
	}

	public void setFieldWikiCount(Long fieldWikiCount) {
		this.fieldWikiCount = fieldWikiCount;
	}
	
	public Boolean getRss() {
		return rss;
	}

	public void setRss(Boolean rss) {
		this.rss = rss;
	}

	public Boolean getFuzzySearch() {
		return fuzzySearch;
	}

	public void setFuzzySearch(Boolean fuzzySearch) {
		this.fuzzySearch = fuzzySearch;
	}

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public Boolean getVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}


	public String getFuzzyField() {
		return fuzzyField;
	}

	public void setFuzzyField(String fuzzyField) {
		this.fuzzyField = fuzzyField;
	}

	public String getFuzzyFieldWeight() {
		return fuzzyFieldWeight;
	}

	public void setFuzzyFieldWeight(String fuzzyFieldWeight) {
		this.fuzzyFieldWeight = fuzzyFieldWeight;
	}
	
	public String getDisplayRows() {
		return displayRows;
	}

	public void setDisplayRows(String displayRows) {
		this.displayRows = displayRows;
	}
	
	public String getAdvancedDateField() {
		return advancedDateField;
	}

	public void setAdvancedDateField(String advancedDateField) {
		this.advancedDateField = advancedDateField;
	}
	
	/**
	 * 採用特殊語法，用大括弧包起來
	 * {
	 * Date_dt:[NOW/DAY TO NOW/DAY+1DAY]
	 * Date_dt:[NOW/DAY+1DAY-3DAY TO NOW/DAY+1DAY]
	 * Date_dt:[NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY]
	 * }
	 * 
	 * @return
	 */
	public String getFacetQueries() {
		return facetQueries;
	}

	public void setFacetQueries(String facetQueries) {
		this.facetQueries = facetQueries;
	}
	
	/**
	 * @return null if no facet queries.
	 */
	public FacetQuery[] getFQ() {
		if (fq == null && StringUtils.isNotEmpty(facetQueries))
			parseFacetQueries();
		
		return fq;
	}
	
	private void parseFacetQueries() {
		Pattern p = Pattern.compile("(?s)(\\{.*?\\})");
		Matcher m = p.matcher(facetQueries);
		
		ArrayList<FacetQuery> afq = new ArrayList<FacetQuery>();
				
		while (m.find()) {
			FacetQuery cfq = new FacetQuery(m.group(1));
			afq.add(cfq);
		}
		
		if (afq.size() > 0) {
			fq = afq.toArray(new FacetQuery[0]);
		}
		else {
			fq = null;
		}
	}

	public DataSet() {
	}


	static Pattern fqPattern1 = Pattern.compile("(?s)\\{<([^:]+):(\\d+)>\\r?\\n(.*?)\\r?\\n\\}");
	static Pattern fqPattern2 = Pattern.compile("(?m)^<([^>]+)>(.*?)$");
	
	public class FacetQuery {
//		private String fqString = null;
		private List<String> queries = null;
		private String fieldName = null;
		private int offset = -1;
		private Hashtable<String, String> map = new Hashtable<String, String>();

		private FacetQuery(String fqString) {
//			this.fqString = fqString;
			
			Matcher m = fqPattern1.matcher(fqString);
			List<String> fqQueries = null;
			if (m.find()) {
				fieldName  = m.group(1);
				offset = Integer.parseInt(m.group(2));
				fqQueries = Arrays.asList(m.group(3).split("\\r?\\n"));
			}
			
			if (fqQueries != null) {
				queries = new ArrayList<String>();
				
				for (String query : fqQueries) {
					Matcher m2 = fqPattern2.matcher(query);
					
					if (m2.find()) {
						String queryPart = m2.group(2);
						map.put(queryPart, m2.group(1));
						queries.add(queryPart);
					}
					else {
						queries.add(query);
					}
				}
			}
		}

		public String getFieldName() {
			return fieldName;
		}

		public int getOffset() {
			return offset;
		}
		
		public String mappingName(String query) {
			return map.get(query);
		}
		
		public List<String> facetQuries() {
			return queries;
		}
	}
	
	public String getQueryBoost() {
		return queryBoost;
	}

	public void setQueryBoost(String queryBoost) {
		this.queryBoost = queryBoost;
	}
	
	public String getQueryBoostMultiply() {
		return queryBoostMultiply;
	}

	public void setQueryBoostMultiply(String queryBoostMultiply) {
		this.queryBoostMultiply = queryBoostMultiply;
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}
