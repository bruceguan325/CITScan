package com.intumit.smartwiki;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

@Entity
public class WikiWord {
	private String pageTitle; // page title
	@Transient
	private int firstIndex; // 文章中出現位置
	@Transient
	private int frequency; // 文章中出現的次數
	@Transient
	private double relationWordScore; // 關聯詞數
	@Transient
	private double devoteScore; // 貢獻分數
	@Transient
	private double longTermScore; // 長詞分數
	@Transient
	private double totalScore; // 總分
	@Transient
	// @CollectionOfElements(targetElement = Integer.class)
	private Set<Integer> allChildSet; // 所有關聯詞
	@Transient
	// @CollectionOfElements(targetElement = Integer.class)
	private Set<Integer> relationChildSet; // 出現在文章中的關聯詞
	@Transient
	private int pageDF; // TF
	@Transient
	private int links; // 在 Wikipeida 出現的連結數
	@Transient
	private double tfidfValue; // TF-IDF 總分
	@Transient
	private String recommand; // 建議
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int keywordId;
	@Transient
	private int redirectId;
	@Transient
	private int ambigId; // 同義字列表頁的 Page id
	@Transient
	private String synset;
	
	private String nature; // 詞性
	
	@Lob
	private String pageDescription;

	public String getPageDescription() {
		return pageDescription;
	}

	public void setPageDescription(String pageDescription) {
		this.pageDescription = pageDescription;
	}

	public WikiWord() {
	}

	public WikiWord(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getNature() {
		return nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}

	public Set<Integer> getAllChildSet() {
		return allChildSet;
	}

	public void setAllChildSet(Set<Integer> allChildSet) {
		this.allChildSet = allChildSet;
	}

	public double getDevoteScore() {
		return devoteScore;
	}

	public void setDevoteScore(double devoteScore) {
		this.devoteScore = devoteScore;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getFirstIndex() {
		return this.firstIndex;
	}

	public void setFirstIndex(int index) {
		this.firstIndex = index;
	}

	public double getLongTermScore() {
		return longTermScore;
	}

	public void setLongTermScore(double longTermScore) {
		this.longTermScore = longTermScore;
	}

	public int getPageDF() {
		return pageDF;
	}

	public void setPageDF(int pageDF) {
		this.pageDF = pageDF;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getRecommand() {
		return recommand;
	}

	public void setRecommand(String recommand) {
		this.recommand = recommand;
	}

	public Set<Integer> getRelationChildSet() {
		return relationChildSet;
	}

	public void setRelationChildSet(Set<Integer> relationChildSet) {
		this.relationChildSet = relationChildSet;
	}

	public double getRelationWordScore() {
		return relationWordScore;
	}

	public void setRelationWordScore(double relationWordScore) {
		this.relationWordScore = relationWordScore;
	}

	public double getTfidfValue() {
		return tfidfValue;
	}

	public void setTfidfValue(double tfidfValue) {
		this.tfidfValue = tfidfValue;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}

	public int getKeywordId() {
		return keywordId;
	}

	public void setKeywordId(int keywordId) {
		this.keywordId = keywordId;
	}

	public boolean isMoreImportant(WikiWord wiki) {
		int len = this.getPageTitle().split("_").length;
		int wikiLen = wiki.getPageTitle().split("_").length;
		if (wiki.getPageTitle().split("_").length == 1)
			return true;
		else if (this.getFirstIndex() == wiki.getFirstIndex()
				&& this.getPageTitle().length() > wiki.getPageTitle().length())
			return true; // new wiki word is the prefix
		else if (this.getFirstIndex() + len == wiki.getFirstIndex() + wikiLen
				&& this.getFirstIndex() < wiki.getFirstIndex())
			return true; // new wiki word is the suffix
		else if (len > wikiLen)
			return true;
		else if (len == wikiLen
				&& (this.getPageTitle().startsWith("the_") || this
						.getPageTitle().startsWith("The_")))
			return false;
		else if (len == wikiLen
				&& (wiki.getPageTitle().startsWith("the_") || wiki
						.getPageTitle().startsWith("The_")))
			return true;
		else
			return false;
	}

	public int getRedirectId() {
		return redirectId;
	}

	public void setRedirectId(int redirectId) {
		this.redirectId = redirectId;
	}

	public int getAmbigId() {
		return ambigId;
	}

	public void setAmbigId(int ambigId) {
		this.ambigId = ambigId;
	}

	public String getSynset() {
		return synset;
	}

	public void setSynset(String synset) {
		this.synset = synset;
	}

	public int getLinks() {
		return links;
	}

	public void setLinks(int links) {
		this.links = links;
	}

	public int getWordLen() {
		int result = 0;
		result = this.getPageTitle().length();

		return result;
	}

}
