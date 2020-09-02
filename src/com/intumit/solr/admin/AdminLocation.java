package com.intumit.solr.admin;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import flexjson.JSONSerializer;

/*
 * 
 */
@Entity
public class AdminLocation implements Serializable {
	public static final Boolean DEFAULT_ALLOW_CORE_OPER = false;
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	@Index(name="ipAddress")
	private String ipAddress;
	
	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean noNeedLogin;
	
	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean allowCoreOperation;
	
	@Index(name="loginName")
	private String loginName;
	
	@Index(name="urlRegex")
	private String urlRegex;

	public AdminLocation() {
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Boolean getNoNeedLogin() {
		return noNeedLogin;
	}

	public void setNoNeedLogin(Boolean noNeedLogin) {
		this.noNeedLogin = noNeedLogin;
	}

	public Boolean getAllowCoreOperation() {
		return allowCoreOperation != null ? allowCoreOperation : DEFAULT_ALLOW_CORE_OPER;
	}

	public void setAllowCoreOperation(Boolean allowCoreOperation) {
		this.allowCoreOperation = allowCoreOperation;
	}

	public String getUrlRegex() {
		return urlRegex;
	}

	public void setUrlRegex(String urlRegex) {
		this.urlRegex = urlRegex;
	}

	
	@Override
	public String toString() {
		return "AdminLocation [id=" + id + ", ipAddress=" + ipAddress
				+ ", noNeedLogin=" + noNeedLogin + ", allowCoreOperation="
				+ allowCoreOperation + ", loginName=" + loginName
				+ ", urlRegex=" + urlRegex + "]";
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).append(ipAddress).append(noNeedLogin).append(allowCoreOperation).append(loginName).append(urlRegex).toHashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof AdminLocation) {
			AdminLocation that = (AdminLocation)o;
			return new EqualsBuilder().append(this.id, that.id).append(this.ipAddress, that.ipAddress).append(this.noNeedLogin, that.noNeedLogin).append(this.allowCoreOperation, that.allowCoreOperation).append(this.loginName, that.loginName).append(this.urlRegex, that.urlRegex).isEquals();
		}
		return false;
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}
