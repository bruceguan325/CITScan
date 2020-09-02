package com.intumit.solr.robot;

import java.io.Serializable;

public class PercolationResult implements Serializable {

	String esId;
	String originalAlt;
	Long kid;
	String prefix;
	String postfix;
	
	public PercolationResult(String esId, String originalAlt, Long kid, String prefix, String postfix) {
		super();
		this.esId = esId;
		this.originalAlt = originalAlt;
		this.kid = kid;
		this.prefix = prefix;
		this.postfix = postfix;
	}
	public String getEsId() {
		return esId;
	}
	public void setEsId(String esId) {
		this.esId = esId;
	}
	public String getOriginalAlt() {
		return originalAlt;
	}
	public void setOriginalAlt(String originalAlt) {
		this.originalAlt = originalAlt;
	}
	public Long getKid() {
		return kid;
	}
	public void setKid(Long kid) {
		this.kid = kid;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((originalAlt == null) ? 0 : originalAlt.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PercolationResult other = (PercolationResult) obj;
		if (originalAlt == null) {
			if (other.originalAlt != null) return false;
		}
		else if (!originalAlt.equals(other.originalAlt)) return false;
		return true;
	}
	@Override
	public String toString() {
		return "PercolationResult [esId=" + esId + ", originalAlt=" + originalAlt + ", kid=" + kid + ", prefix=" + prefix + ", postfix=" + postfix + "]";
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getPostfix() {
		return postfix;
	}
	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}
}
