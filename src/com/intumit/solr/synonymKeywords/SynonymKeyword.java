package com.intumit.solr.synonymKeywords;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hibernate.annotations.Index;

import com.intumit.solr.SearchManager;

@Entity
public class SynonymKeyword implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	@Index(name="tenantIdIdx")
	private Integer tenantId;
	@Index(name="keyword")
	private String keyword;
	@Index(name="synonymKeyword")
	private String synonymKeyword;
	private boolean reverse;
	@Column(length=32)
	private String nature;
	
	/**
	 * 當問句是長詞時，用來判定多用途的詞該直接選擇哪一個比較好
	 */
	@Transient
	private int priority = -1;
	
	@Transient
	private boolean inAudit;
	
	@Transient
	private String passDate;

	public boolean isInAudit() {
		return inAudit;
	}
	
	public void setInAudit(boolean inAudit) {
		this.inAudit = inAudit;
	}
	
	public String getPassDate() {
		return passDate;
	}
	
	public void setPassDate(String passDate) {
		this.passDate = passDate;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = StringUtils.trim(keyword);
	}
	
	@Transient
	public int getPriority() {
		if (priority < 0) {
			// 整個系統只有一份 keywordPriorityMap，裡頭的 Priority 值也是參考全系統的 core-log...
			if (SynonymKeywordFacade.keywordPriorityMap.containsKey(getKeyword())) {
				priority = SynonymKeywordFacade.keywordPriorityMap.get(getKeyword()).intValue();
			}
			else {
				QueryResponse mainRsp = null;
				SolrServer mServer = SearchManager.getServer("core-log");
				SolrQuery multiCoreQ = new SolrQuery();
				multiCoreQ.setQuery(getKeyword());
				multiCoreQ.setFacet(false);
				multiCoreQ.setRows(0);
				multiCoreQ.setRequestHandler("browse");
				multiCoreQ.set("qf", "Name_t^1");
				multiCoreQ.set("fl", "*,score");
				
				try {
					System.out.println(multiCoreQ.toString());
					mainRsp = mServer.query(multiCoreQ);
					priority = (int)mainRsp.getResults().getNumFound();
					SynonymKeywordFacade.keywordPriorityMap.put(getKeyword(), new Long(priority));
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return priority;
	}
	@Transient
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public String getSynonymKeyword() {
		return "," + StringUtils.strip(synonymKeyword, ",") + ",";
	}
	public String getSynonymKeywordForEditingOrReading() {
		// 同義詞當中, 四個逗號 ",,,," 之後的是系統自動產生的同義詞（例如為了日文的平假名，系統可以自動產生）
		return "," + StringUtils.strip(StringUtils.substringBefore(synonymKeyword, ",,,,"), ",") + ","; 
	}
	public List<String> getSynonymList() {
		return new ArrayList<>(Arrays.asList(StringUtils.split(getSynonymKeyword(), ",")));
	}
	public List<String> getKeywordAndSynonymList() {
		List<String> l = new ArrayList<>(Arrays.asList(StringUtils.split(getSynonymKeyword(), ",")));
		l.add(0, getKeyword());
		return l;
	}
	public void setSynonymKeyword(String synonymKeyword) {
		this.synonymKeyword = synonymKeyword;
	}
	public boolean isReverse() {
		return reverse;
	}
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
	public String getNature() {
		return StringUtils.trimToNull(nature);
	}
	public void setNature(String nature) {
		this.nature = nature;
	}
	@Override
	public String toString() {
		return "SynonymKeyword [tenantId="+tenantId+", id=" + id + ", keyword=" + keyword
				+ ", synonymKeyword=" + synonymKeyword + ", reverse=" + reverse + ", nature=" + nature
				+ "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
		result = prime * result
				+ ((tenantId == null) ? 0 : tenantId.hashCode());
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
		SynonymKeyword other = (SynonymKeyword) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (keyword == null) {
			if (other.keyword != null)
				return false;
		} else if (!keyword.equals(other.keyword))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		return true;
	}

}
