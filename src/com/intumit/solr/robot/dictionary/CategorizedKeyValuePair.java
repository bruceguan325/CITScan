package com.intumit.solr.robot.dictionary;

import java.io.Serializable;
import org.apache.commons.lang.StringUtils;

public class CategorizedKeyValuePair implements Serializable {
	public static final String CATEGORIZED_PREFIX = "SRBTCP";
	public static enum Type {
		ROBOTNAME,
		NUMBER,
		TEMPORAL,
		CUSTOM,
	}

	String key;
	Object value;
	Type type;
	public CategorizedKeyValuePair(String key, Object value, Type type) {
		super();
		this.key = key;
		this.value = value;
		this.type = type;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CategorizedKeyValuePair other = (CategorizedKeyValuePair) obj;
		if (key == null) {
			if (other.key != null) return false;
		}
		else if (!key.equals(other.key)) return false;
		if (type != other.type) return false;
		if (value == null) {
			if (other.value != null) return false;
		}
		else if (!value.equals(other.value)) return false;
		return true;
	}
	@Override
	public String toString() {
		return "CategorizedKeyValuePair [key=" + key + ", value=" + value + ", type=" + type + "]";
	}

	public String toInlineKey() {
		return CATEGORIZED_PREFIX + StringUtils.upperCase(getType().name());
	}

	public static String toInlineKey(Type type) {
		return CATEGORIZED_PREFIX + StringUtils.upperCase(type.name());
	}
	
}
