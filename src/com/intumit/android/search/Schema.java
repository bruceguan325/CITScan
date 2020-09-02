package com.intumit.android.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Schema implements Serializable {
	public static final Field _ID = new Field("internal_id_dont_try_to_modified", 65535, false);

	Field pk = null;
	List<Field> fields = null;
	boolean storeData = false;
	boolean compactMode = false;
	boolean privateMode = false;
	boolean pkAsId = false;
	
	transient BiMap<String, Integer> fieldCode = null;
	transient BiMap<String, Field> name2field = null;

	public List<Field> getFields() {
		return fields;
	}
	public Field getField(String fieldName) {
		if (name2field == null) {
			name2field = HashBiMap.create();
			
			for (Field f: fields) {
				name2field.put(f.getFieldName(), f);
			}
		}
		
		return name2field.get(fieldName);
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
		if (!fields.contains(_ID)) {
			fields.add(_ID);
		}
		fieldCode = null;
	}
	
	public boolean addField(Field field) {
		if (fields == null) {
			fields = new ArrayList<Field>();
			fields.add(_ID);
		}
		
		if (fields.contains(field)) 
			return false;
		
		fields.add(field);
		
		if (fieldCode == null) {
			fieldCode = HashBiMap.create();
			
			for (Field f: fields) {
				fieldCode.put(f.getFieldName(), f.getFieldCode());
			}
		}
		else {
			fieldCode.put(field.getFieldName(), field.getFieldCode());
		}
		
		return true;
	}

	public Field getPrimaryKey() {
		return pk;
	}

	public void setPrimaryKey(Field primaryKey) {
		this.pk = primaryKey;
	}
	
	protected String getDefaultFieldName() {
		return "fulltext";
	}
	
	public BiMap<String, Integer> getFieldCode() {
		if (fieldCode == null) {
			fieldCode = HashBiMap.create();
			
			for (Field f: fields) {
				fieldCode.put(f.getFieldName(), f.getFieldCode());
			}
		}
		
		return fieldCode;
	}

	public boolean isStoreData() {
		return storeData;
	}

	public void setStoreData(boolean storeData) {
		this.storeData = storeData;
	}
	
	public boolean isCompactMode() {
		return compactMode;
	}
	
	public void setCompactMode(boolean compactMode) {
		this.compactMode = compactMode;
	}
	
	boolean isPrivateMode() {
		return privateMode;
	}
	void setPrivateMode(boolean privateMode) {
		this.privateMode = privateMode;
	}
	public boolean isPkAsId() {
		return pkAsId;
	}
	public void setPkAsId(boolean pkAsId) {
		this.pkAsId = pkAsId;
	}
}
