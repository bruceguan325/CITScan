package com.intumit.smartwiki.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class PageLink implements Serializable{

    private Integer pageLinkFrom;
    private Integer pageLinkNamespace;
    private String pageLinkTitle;
    private Integer pageLinkTo;
	public Integer getPageLinkFrom() {
		return pageLinkFrom;
	}
	public void setPageLinkFrom(Integer pageLinkFrom) {
		this.pageLinkFrom = pageLinkFrom;
	}
	public Integer getPageLinkNamespace() {
		return pageLinkNamespace;
	}
	public void setPageLinkNamespace(Integer pageLinkNamespace) {
		this.pageLinkNamespace = pageLinkNamespace;
	}
	public String getPageLinkTitle() {
		return pageLinkTitle;
	}
	public void setPageLinkTitle(String pageLinkTitle) {
		this.pageLinkTitle = pageLinkTitle;
	}

	public boolean equals(Object object){
		if(!(object instanceof PageLink)) {
			return false;
		}
		PageLink pl = (PageLink)object;
		return new EqualsBuilder()
			.appendSuper(super.equals(object))
			.append(this.pageLinkFrom, pl.pageLinkFrom)
			.append(this.pageLinkNamespace, pl.pageLinkNamespace)
			.append(this.pageLinkTitle, pl.pageLinkTitle)
			.isEquals();
	}

	public int hashCode(){
		return new HashCodeBuilder(-528253723, -475504089)
			.appendSuper(super.hashCode())
			.append(this.pageLinkFrom)
			.append(this.pageLinkNamespace)
			.append(this.pageLinkTitle)
			.toHashCode();
	}
	public Integer getPageLinkTo() {
		return pageLinkTo;
	}
	public void setPageLinkTo(Integer pageLinkTo) {
		this.pageLinkTo = pageLinkTo;
	}

}
