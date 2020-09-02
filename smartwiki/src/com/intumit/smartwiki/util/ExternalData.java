package com.intumit.smartwiki.util;

import java.io.Serializable;

public class ExternalData  implements Serializable {
	private static final long serialVersionUID = 1L;

	private String pageTitle;                      
	private int pageId;			           
	private String externalTitle;        
	private String externalLink;

	public ExternalData() {
		super();
	}

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getExternalTitle() {
        return externalTitle;
    }

    public void setExternalTitle(String externalTitle) {
        this.externalTitle = externalTitle;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }
}
