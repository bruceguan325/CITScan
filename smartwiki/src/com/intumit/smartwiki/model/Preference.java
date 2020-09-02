package com.intumit.smartwiki.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.intumit.smartwiki.enums.FORMAT;

@Entity
public class Preference {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	private String uuid = null;

	private String preferLang;

	private String textColor;

	private String backgroundColor;

	private int textSize;

	@Column(length = 512)
	private FORMAT format;

	private String formatStr;

	private String remarks;

	private String category;

	private String projectNumber; // 專案代碼：[自填]

	private String companyTaxId; // 統一編號：[自填]

	private String authorizedProduct;// 授權產品：[自填] (預設值: WiSe)

	private String authorizedSite; // 網站網址：[自填]

	private String authorizedType; // 授權方式：買斷、租用、合作

	private String authorizedArea; // 授權地域：[自填] (預設值: 台灣)

	private String adSource; // 廣告類型

	private Date mouDeadLine; // MOU期限：指定日期或無限

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	private boolean isVIP;

	public Preference() {
	}

	public Preference(String uuid) {
		this.uuid = uuid;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public String getPreferLang() {
		return preferLang;
	}

	public void setPreferLang(String preferLang) {
		this.preferLang = preferLang;
	}

	public String getTextColor() {
		return textColor;
	}

	public void setTextColor(String textColor) {
		this.textColor = textColor;
	}

	public int getTextSize() {
		return textSize;
	}

	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public FORMAT getFormat() {
		return format;
	}

	public void setFormat(FORMAT format) {
		this.format = format;

		switch (format) {
		case HTML:
			setFormatStr("HTML");
			break;
		case MHTML:
			setFormatStr("MHTML");
			break;
		case XML:
			setFormatStr("XML");
			break;
		case TEXT:
			setFormatStr("TEXT");
			break;
		}
	}

	public String getFormatStr() {
		return formatStr;
	}

	public void setFormatStr(String formatStr) {
		this.formatStr = formatStr;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(uuid).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if ((other instanceof Preference) == false) {
			return false;
		}
		if (this == other) {
			return true;
		}

		Preference obj = (Preference) other;
		return new EqualsBuilder().append(uuid, obj.uuid).isEquals();
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public boolean getIsVIP() {
		return isVIP;
	}

	public void setIsVIP(boolean isVIP) {
		this.isVIP = isVIP;
	}

	public String getProjectNumber() {
		return projectNumber;
	}

	public void setProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
	}

	public String getCompanyTaxId() {
		return companyTaxId;
	}

	public void setCompanyTaxId(String companyTaxId) {
		this.companyTaxId = companyTaxId;
	}

	public String getAuthorizedProduct() {
		return authorizedProduct;
	}

	public void setAuthorizedProduct(String authorizedProduct) {
		this.authorizedProduct = authorizedProduct;
	}

	public String getAuthorizedSite() {
		return authorizedSite;
	}

	public void setAuthorizedSite(String authorizedSite) {
		this.authorizedSite = authorizedSite;
	}

	public String getAuthorizedType() {
		return authorizedType;
	}

	public void setAuthorizedType(String authorizedType) {
		this.authorizedType = authorizedType;
	}

	public String getAuthorizedArea() {
		return authorizedArea;
	}

	public void setAuthorizedArea(String authorizedArea) {
		this.authorizedArea = authorizedArea;
	}

	public String getAdSource() {
		return adSource;
	}

	public void setAdSource(String adSource) {
		this.adSource = adSource;
	}

	public Date getMouDeadLine() {
		return mouDeadLine;
	}

	public void setMouDeadLine(Date mouDeadLine) {
		this.mouDeadLine = mouDeadLine;
	}

	public long getDeadLine() {
		if (mouDeadLine != null)
			return mouDeadLine.getTime();
		else
			return 0;
	}
}
