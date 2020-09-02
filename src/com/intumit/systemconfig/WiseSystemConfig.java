package com.intumit.systemconfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Type;

import com.intumit.solr.robot.WSUtil;
import com.intumit.solr.tenant.Tenant;

@Entity
public class WiseSystemConfig {

	static final int FIXED_ID = 0;
	static final int DEFAULT_HOT_KEYWORD_FROM = 7;
	static final int DEFAULT_SNIPPET_SIZE = 100;
	static final int DEFAULT_MAX_CORES = 10;
	static final String DEFAULT_CONTEXT_PATH = "/wise";
	static final Boolean DEFAULT_PROXY_ENABLE = Boolean.TRUE;
	static final Boolean DEFAULT_ENABLE_FRONTEND_USER_LOGIN = Boolean.FALSE;
	static final Boolean DEFAULT_ENABLE_SSL = Boolean.FALSE;
	static final int DEFAULT_TENANT_ID = 1;
	static final boolean DEFAULT_LOAD_BALANCE_MODE_ENABLE = Boolean.TRUE;
	static final String DEFAULT_LOG_PATH = "D://Project_GIT/CIT01P181/SmartSRM_Robot_OLD/SmartSRM_Robot/jetty9/logs";

	@Deprecated
	public static final String DEFAULT_IBANK_LOGIN_URL = "http://dxmybank05/mybank/Home/Login_Test";
	@Deprecated
	public static final String DEFAULT_IBANK_FUNC_BASE_URL = "https://www.mybank.com.tw/MyBank/";
	@Deprecated
	public static final String DEFAULT_APP_URL_TRANSFORM_PREFIX = "mymobibank://smartService?type=web&value=";
	@Deprecated
	public static final String DEFAULT_APP_IBANK_URL_TRANSFORM_PREFIX = "mymobibank://smartService?type=myBank&value=";


	public WiseSystemConfig() {

	}

	@Id
	private int id;
	private String hostname = null; // DEFAULT
	private String localPort = "8080"; // DEFAULT
	private String contextPath = "/wise"; // DEFAULT
	private int coreMax;
	private int descriptionMax;
	private Integer hotKeywordFrom = DEFAULT_HOT_KEYWORD_FROM;

	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	@Deprecated
	private Boolean enableFrontEndUser = false;
	private Integer defaultTenantId;
	
	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean proxyEnable = DEFAULT_PROXY_ENABLE;
	private String proxyHost;
	private Integer proxyPort;
	private String allowedReverseProxies = null;

	private String secretHash = null;
	private String intumitSH = null;

	private String mailUsername;
	private String mailPassword;
	private String mailServerHost;
	private Integer mailServerPort;
	
	private String vLog = "";
	
	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean enableSsl;

	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean lbModeEnable = DEFAULT_LOAD_BALANCE_MODE_ENABLE;
	
	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean lbModeSwitchable = DEFAULT_LOAD_BALANCE_MODE_ENABLE;
	
	private Integer lbErrorCount;

	// customized preferences
	@Deprecated
	private String bancsWsUrl;
	@Deprecated
	private String cardWsUrl;
	@Deprecated
	private String fepWsUrl;
	@Deprecated
	private String iBankLoginUrl; // 網路銀行登入網址
	@Deprecated
	private String iBankFuncBaseUrl; // 網路銀行功能根網址
	@Deprecated
	private String appUrlTransformPrefix; // APP網址轉換前綴
	@Deprecated
	private String appIbankUrlTransformPrefix; // APP網路銀行網址轉換前綴

	@Column(columnDefinition = "TINYINT")
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private Boolean testDummyBooleanColumn;

	@Transient
	private Set<String> cacheAllowedReverseProxies = null;
	
	private String ssoPermissionCheckUrl;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCoreMax() {
		return coreMax;
	}

	public void setCoreMax(int coreMax) {
		this.coreMax = coreMax;
	}

	public int getDescriptionMax() {
		return descriptionMax;
	}

	public void setDescriptionMax(int descriptionMax) {
		this.descriptionMax = descriptionMax;
	}

	public Integer getHotKeywordFrom() {
		return hotKeywordFrom == null ? DEFAULT_HOT_KEYWORD_FROM : hotKeywordFrom;
	}

	public void setHotKeywordFrom(int hotKeywordFrom) {
		this.hotKeywordFrom = hotKeywordFrom;
	}

	@Deprecated
	public Boolean getEnableFrontEndUser() {
		return false; // Deprecated, so always return false until we totally remove this field 
		// return enableFrontEndUser != null ? enableFrontEndUser : DEFAULT_ENABLE_FRONTEND_USER_LOGIN;
	}

	@Deprecated
	public void setEnableFrontEndUser(Boolean enableFrontEndUser) {
		this.enableFrontEndUser = enableFrontEndUser;
	}

	public String getSecretHash() {
		return secretHash;
	}

	public Boolean getLbModeEnable() {
		return DEFAULT_LOAD_BALANCE_MODE_ENABLE;
	}

	public void setLbModeEnable(Boolean lbModeEnable) {
		this.lbModeEnable = lbModeEnable;
	}
	
	public Boolean getLbModeSwitchable() {
		return DEFAULT_LOAD_BALANCE_MODE_ENABLE;
	}

	public void setLbModeSwitchable(Boolean lbModeSwitchable) {
		this.lbModeSwitchable = lbModeSwitchable;
	}

	public String getMailUsername() {
		return mailUsername;
	}

	public void setMailUsername(String mailUsername) {
		this.mailUsername = mailUsername;
	}

	public String getMailPassword() {
		return mailPassword;
	}

	public void setMailPassword(String mailPassword) {
		this.mailPassword = mailPassword;
	}

	public Tenant getDefaultTenant() {
		return Tenant.get(getDefaultTenantId());
	}

	public Integer getDefaultTenantId() {
		return defaultTenantId == null ? this.DEFAULT_TENANT_ID : defaultTenantId;
	}

	public void setDefaultTenantId(Integer defaultTenant) {
		this.defaultTenantId = defaultTenant;
	}

	public void setSecretHash(String secretHash) {
		this.secretHash = secretHash;
	}

	public Boolean getProxyEnable() {
		return proxyEnable != null ? proxyEnable : DEFAULT_PROXY_ENABLE;
	}

	public void setProxyEnable(Boolean proxyEnable) {
		this.proxyEnable = proxyEnable;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public Proxy getProxy() {
		if (this.getProxyEnable()) {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort == null ? 3128 : proxyPort));
		}
		return null;
	}

	public String getAllowedReverseProxies() {
		return allowedReverseProxies;
	}

	public void setAllowedReverseProxies(String allowedReverseProxies) {
		this.allowedReverseProxies = allowedReverseProxies;
	}

	public boolean isAllowedReverseProxy(String ipToCheck) {
		if (cacheAllowedReverseProxies == null) {
			cacheAllowedReverseProxies = new HashSet<String>();

			if (StringUtils.isNotEmpty(allowedReverseProxies)) {
				cacheAllowedReverseProxies.addAll(Arrays.asList(StringUtils.split(allowedReverseProxies)));
			}
		}

		return cacheAllowedReverseProxies.contains(ipToCheck);
	}

	@Deprecated
	public String getBancsWsUrl() {
		return bancsWsUrl == null?WSUtil.DEFAULT_BANCS_ENDPOINT:bancsWsUrl;
	}

	@Deprecated
	public void setBancsWsUrl(String bancsWsUrl) {
		this.bancsWsUrl = bancsWsUrl;
	}

	@Deprecated
	public String getCardWsUrl() {
		return cardWsUrl == null?WSUtil.DEFAULT_CARD_ENDPOINT:cardWsUrl;
	}

	@Deprecated
	public void setCardWsUrl(String cardWsUrl) {
		this.cardWsUrl = cardWsUrl;
	}

	@Deprecated
	public String getFepWsUrl() {
		return fepWsUrl == null?WSUtil.DEFAULT_FEP_ENDPOINT:fepWsUrl;
	}

	@Deprecated
	public void setFepWsUrl(String fepWsUrl) {
		this.fepWsUrl = fepWsUrl;
	}

	@Deprecated
	public String getiBankLoginUrl() {
		return iBankLoginUrl == null?DEFAULT_IBANK_LOGIN_URL:iBankLoginUrl;
	}

	@Deprecated
	public void setiBankLoginUrl(String iBankLoginUrl) {
		this.iBankLoginUrl = iBankLoginUrl;
	}

	@Deprecated
	public String getiBankFuncBaseUrl() {
		return iBankFuncBaseUrl == null?DEFAULT_IBANK_FUNC_BASE_URL:iBankFuncBaseUrl;
	}

	@Deprecated
	public void setiBankFuncBaseUrl(String iBankFuncBaseUrl) {
		this.iBankFuncBaseUrl = iBankFuncBaseUrl;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	/**
	 * get absolute url (include contextPath, strip ending "/")
	 * it use WiseSystemConfig.hosename + WiseSystemConfig.contextPath 
	 * @return
	 */
	public String getFullUrlBase() {
		String baseUrl = StringUtils.stripEnd(StringUtils.trimToEmpty(WiseSystemConfig.get().getHostname()) + getContextPath(), "/");
		
		return baseUrl;
	}

	public String getContextPath() {
		return contextPath == null?DEFAULT_CONTEXT_PATH:contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getAppUrlTransformPrefix() {
		return appUrlTransformPrefix == null?DEFAULT_APP_URL_TRANSFORM_PREFIX:appUrlTransformPrefix;
	}

	public void setAppUrlTransformPrefix(String appUrlTransformPrefix) {
		this.appUrlTransformPrefix = appUrlTransformPrefix;
	}

	public String getAppIbankUrlTransformPrefix() {
		return appIbankUrlTransformPrefix == null?DEFAULT_APP_IBANK_URL_TRANSFORM_PREFIX:appIbankUrlTransformPrefix;
	}

	public void setAppIbankUrlTransformPrefix(String appIbankUrlTransformPrefix) {
		this.appIbankUrlTransformPrefix = appIbankUrlTransformPrefix;
	}

	public static WiseSystemConfig get() {
		return WiseSystemConfigFacade.getInstance().get();
	}

	public Integer getMailServerPort() {
		return mailServerPort;
	}

	public void setMailServerPort(Integer mailServerPort) {
		this.mailServerPort = mailServerPort;
	}

	public String getMailServerHost() {
		return mailServerHost;
	}

	public void setMailServerHost(String mailServerHost) {
		this.mailServerHost = mailServerHost;
	}

	public Boolean isEnableSsl() {
		return enableSsl != null ? enableSsl : DEFAULT_ENABLE_SSL;
	}

	public void setEnableSsl(Boolean enableSsl) {
		this.enableSsl = enableSsl;
	}

	public Boolean isTestDummyBooleanColumn() {
		return testDummyBooleanColumn;
	}

	public void setTestDummyBooleanColumn(Boolean testDummyBooleanColumn) {
		this.testDummyBooleanColumn = testDummyBooleanColumn;
	}
	
	public String getIntumitSH() {
		return intumitSH;
	}

	public void setIntumitSH(String intumitSH) {
		this.intumitSH = intumitSH;
	}

	public String getLocalPort() {
		return localPort == null ? "8080" : localPort;
	}

	public void setLocalPort(String localPort) {
		this.localPort = localPort;
	}

	public Integer getLbErrorCount() {
		return 1;
	}

	public void setLbErrorCount(Integer lbErrorCount) {
		this.lbErrorCount = lbErrorCount;
	}

	public String getvLog() {
		return vLog;
	}

	public void setvLog(String vLog) {
		this.vLog = vLog;
	}

	public String getSsoPermissionCheckUrl() {
		return ssoPermissionCheckUrl;
	}

	public void setSsoPermissionCheckUrl(String ssoPermissionCheckUrl) {
		this.ssoPermissionCheckUrl = ssoPermissionCheckUrl;
	}

}
