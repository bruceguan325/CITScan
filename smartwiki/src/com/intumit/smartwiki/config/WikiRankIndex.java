package com.intumit.smartwiki.config;

import java.net.MalformedURLException;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

@Entity
public class WikiRankIndex {
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	/**
	 * 採用 HitHotLocale.name()
	 */
	private String lang;

	@Lob
	private String indexLocation;
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getIndexLocation() {
		return indexLocation;
	}

	public void setIndexLocation(String indexLocation) {
		this.indexLocation = indexLocation;
	}

	public SolrServer getServer() {
		return new HttpSolrServer(indexLocation);
	}
}
