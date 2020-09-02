package com.intumit.android.search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Value object for each field
 * 
 * @author Intumit Inc.
 */
public class FieldValue implements Serializable {

	Field field;
	String orginal;
	Map<String, Set<String>> synonyms = null;
	List<String> multiValues = null;
	private boolean isMultiValue = false;
	
	public FieldValue(Field field) {
		super();
		this.field = field;
	}

	public FieldValue(Field field, String orginal) {
		super();
		this.field = field;
		this.orginal = orginal;
	}

	/**
	 * 
	 * @param fieldName field name 
	 * @param orginal the original data of this field
	 * @param synonyms all synonyms of original data, including pinyin.
	 */
	public FieldValue(Field field, String orginal,
			Map<String, Set<String>> synonyms) {
		super();
		this.field = field;
		this.orginal = orginal;
		this.synonyms = synonyms;
	}
	
	public Field getField() {
		return field;
	}
	public String getFieldName() {
		return field.getFieldName();
	}

	public String getOrginal() {
		return orginal;
	}

	public void setOrginal(String orginal) {
		this.orginal = orginal;
	}
	
	public void setMultiValues(List<String> values) {
		this.multiValues = values;
		isMultiValue = true;
	}
	
	public List<String> getMultiValues() {
		return multiValues;
	}

	public boolean isMultiValue() {
		return isMultiValue;
	}

	public Map<String, Set<String>> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(Map<String, Set<String>> synonyms) {
		this.synonyms = synonyms;
	}
	
	public void addSynonym(String part, String synonym) {
		if (synonyms == null) {
			synonyms = new HashMap<String, Set<String>>();
		}
		
		if (!synonyms.containsKey(part)) {
			synonyms.put(part, new HashSet<String>());
		}
		
		synonyms.get(part).add(synonym);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((orginal == null) ? 0 : orginal.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldValue other = (FieldValue) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (orginal == null) {
			if (other.orginal != null)
				return false;
		} else if (!orginal.equals(other.orginal))
			return false;
		return true;
	}
}
