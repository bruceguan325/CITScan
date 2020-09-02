package com.intumit.smartwiki.util;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class NameValuePair implements Comparable, Serializable {

	private static final long serialVersionUID = 4122537104933342877L;

	private String name;

    private String value;
    
    public NameValuePair() {
        super();
    }

    public NameValuePair(String name, String value) {
        super();
        this.name = StringUtils.trim(name);
        this.value = StringUtils.trim(value);
    }

    public NameValuePair(String name, int value) {
        super();
        this.name = StringUtils.trim(name);
        this.value = String.valueOf(value);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtils.trim(name);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = StringUtils.trim(value);
    }

    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj != null && obj instanceof NameValuePair) {
            NameValuePair o = (NameValuePair) obj;
            equals = new EqualsBuilder().append(this.getName(), o.getName())
                .append(this.getValue(), o.getValue()).isEquals();
        }
        return equals;
    }

    public int hashCode() {
        return new HashCodeBuilder(5, 17).append(this.getName()).append(
            this.getValue()).toHashCode();
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", this.getName()).append(
            "value", this.getValue()).toString();
    }

    public int compareTo(Object o) {
        NameValuePair obj = (NameValuePair) o;
        if(this.getName().length() == obj.getName().length())
            return this.getName().compareTo(obj.getName());
        else
            return this.getName().length() - obj.getName().length();
    }
    
    public boolean isEmbeddedFuzzy() {
    	return StringUtils.countMatches(name, ":") > 1;
    }
    
    public String getEmbeddedType() {
    	return isEmbeddedFuzzy() ? StringUtils.split(name, ":")[2] : "XXXXX";
    }

}
