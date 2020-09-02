package com.intumit.solr.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang.StringUtils;

import flexjson.JSONSerializer;

@Entity
public class ElevatorSet {
	public static final String FACET_QUERY_SPLITTER = ",,,,,";
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(length = 512)
	private String dsId;

	@Column(length = 512)
	private String query;
	
	@Lob
	private String fixedAtTop;
	
	@Lob
	private String facetQueryFixedAtTop;
	
	@Lob
	private String hidden;
	
	@Lob
	private String addedAfterFixed;

	@Column(length = 64)
	private String creator;

	private Date timestamp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDsId() {
		return dsId;
	}

	public void setDsId(String dsId) {
		this.dsId = dsId;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getFixedAtTop() {
		return fixedAtTop;
	}

	public List<String> getFixedAtTopAsList() {
		if (StringUtils.isEmpty(fixedAtTop)) return new ArrayList<String>();
		return Arrays.asList(StringUtils.trimToEmpty(fixedAtTop).split(","));
	}

	public void setFixedAtTop(String fixedAtTop) {
		this.fixedAtTop = fixedAtTop;
	}

	public String getHidden() {
		return hidden;
	}

	public List<String> getHiddenList() {
		if (StringUtils.isEmpty(hidden)) return new ArrayList<String>();
		return Arrays.asList(StringUtils.trimToEmpty(hidden).split(","));
	}

	public void setHidden(String hidden) {
		this.hidden = hidden;
	}

	public String getFacetQueryFixedAtTop() {
		return facetQueryFixedAtTop;
	}

	public List<String> getFacetQueryFixedAtTopAsList() {
		if (StringUtils.isEmpty(facetQueryFixedAtTop)) return new ArrayList<String>();
		return Arrays.asList(StringUtils.trimToEmpty(facetQueryFixedAtTop).split(FACET_QUERY_SPLITTER));
	}

	public void setFacetQueryFixedAtTop(String facetQueryFixedAtTop) {
		this.facetQueryFixedAtTop = facetQueryFixedAtTop;
	}

	public void addFacetQueryFixedAtTop(String toBeAdd) {
		if (StringUtils.isEmpty(facetQueryFixedAtTop)) {
			facetQueryFixedAtTop = toBeAdd;
		}
		else {
			facetQueryFixedAtTop += FACET_QUERY_SPLITTER + toBeAdd;
		}
	}

	public boolean removeFacetQueryFixedAtTop(String toBeRemove) {
		List<String> list = new ArrayList<String>(this.getFacetQueryFixedAtTopAsList());
		if (list.contains(toBeRemove)) {
			list.remove(toBeRemove);
			
			setFacetQueryFixedAtTop(StringUtils.join(list, FACET_QUERY_SPLITTER));
			return true;
		}
		
		return false;
	}

	public boolean hasFacetQueryFixedAtTop() {
		return StringUtils.isNotEmpty(facetQueryFixedAtTop);
	}
	
	public String facetQueryToBoostQuery() {
    	StringBuffer bf = new StringBuffer();
        for (String bffq: getFacetQueryFixedAtTopAsList()) {
        	if (bf.length() == 0) {
        		bf.append(bffq);
        	}
        	else {
        		bf.append(" OR " + bffq);
        	}
        }
        
        return bf.toString();
	}

	public String getAddedAfterFixed() {
		return addedAfterFixed;
	}

	public List<String> getAddedAfterFixedList() {
		if (StringUtils.isEmpty(addedAfterFixed)) return new ArrayList<String>();
		return Arrays.asList(StringUtils.trimToEmpty(addedAfterFixed).split(","));
	}

	public void setAddedAfterFixed(String addedAfterFixed) {
		this.addedAfterFixed = addedAfterFixed;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}
