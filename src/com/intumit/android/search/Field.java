package com.intumit.android.search;

import java.io.Serializable;

public class Field implements Serializable {

	String fieldName;
	int fieldCode;
	boolean searchable;
	boolean sortable;


	public Field(String fieldName, int fieldCode) {
		this(fieldName, fieldCode, true, false);
	}
	public Field(String fieldName, int fieldCode, boolean sortable) {
		this(fieldName, fieldCode, true, sortable);
	}
	public Field(String fieldName, int fieldCode, boolean searchable, boolean sortable) {
		super();
		this.fieldName = fieldName;
		this.fieldCode = fieldCode;
		this.searchable = searchable;
		this.sortable = sortable;
	}

	public String getFieldName() {
		return fieldName;
	}

	public int getFieldCode() {
		return fieldCode;
	}

	public boolean isSortable() {
		return sortable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fieldCode;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + (searchable ? 1231 : 1237);
		result = prime * result + (sortable ? 1231 : 1237);
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
		Field other = (Field) obj;
		if (fieldCode != other.fieldCode)
			return false;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (searchable != other.searchable)
			return false;
		if (sortable != other.sortable)
			return false;
		return true;
	}

	
}
