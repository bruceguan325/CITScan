package com.intumit.viewRecord;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Index;

@Entity
public class ViewRecordEntity {

	@Id @GeneratedValue
	private int id;

	@Column(length = 255)	
	@Index(name="keyword")
	private String keyword;
	
	@OneToMany(mappedBy="viewRecordEntity", fetch=FetchType.EAGER)
	@MapKey(name="idRecord")
	private Map<String,KeywordToIdRecord> keywordToIdRecords;
	
	public ViewRecordEntity() {
		if (keywordToIdRecords==null) {
			keywordToIdRecords = new HashMap<String, KeywordToIdRecord>();
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public Map<String, KeywordToIdRecord> getKeywordToIdRecords() {
		return keywordToIdRecords;
	}

	public void setKeywordToIdRecords(
			Map<String, KeywordToIdRecord> keywordToIdRecords) {
		this.keywordToIdRecords = keywordToIdRecords;
	}
	
	//fetch=FetchType.EAGER消耗大改用jdbc寫
	/*public void addIdRecords(String id) {
		KeywordToIdRecord record;

		if (this.keywordToIdRecords.containsKey(id)){
			record = this.keywordToIdRecords.get(id);
		} else {
			record = new KeywordToIdRecord();
			record.setIdRecord(id);
			record.setViewRecordEntity(this);
			record.setViewTime(0);
			this.keywordToIdRecords.put(id, record);
		}
		
		record.setViewTime(record.getViewTime()+1);
		
		record.save();
	}*/
	
}

