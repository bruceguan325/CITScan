package com.intumit.ldap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

import com.intumit.util.XMLUtil;

public class LdapConfig implements Serializable {

	private String ldapName;
	private String serverAddr;
	private Integer serverPort = 389;
	private String apUserDn;
	private String apPassword;	
	private String loginBaseDn;
	private String userNameField;
	private String userAccountField;
	private String groupNameField;
	private String groupDescField;
	private String emailField;
	private String defaultGroupName;
	private String userFilter;
	private String groupFilter;	
	private long lastModified = 0;

	public static LdapConfig getCfgFromString(String str) {
		LdapConfig cfg = new LdapConfig();
		
		try {
			Element ldapConfigs = readXml(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))).getRootElement();
			if (ldapConfigs != null) {
				cfg.initLdapParams(ldapConfigs.getChild("global"));					
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return cfg;
	}

	public static LdapConfig getCfgFromFile(File serverConfigFile) {
		LdapConfig cfg = new LdapConfig();
		
		try {
			if (serverConfigFile.lastModified() > cfg.lastModified) {
				try {
					Element ldapConfigs = readXml(new FileInputStream(serverConfigFile)).getRootElement();
					if (ldapConfigs != null) {
						cfg.initLdapParams(ldapConfigs.getChild("global"));					
					}
					cfg.lastModified = serverConfigFile.lastModified();
				} catch (Exception e) {
					throw new LdapLoginException("讀取 " + serverConfigFile.getName() + " 發生錯誤!", e);
				}
			}
		}
		catch (LdapLoginException e) {
			e.printStackTrace();
		}
		
		return cfg;
	}

	LdapConfig() {
		super();
	}
	
	private void initLdapParams(Element ldap) {
		ldapName = StringUtils.trimToEmpty(ldap.getAttributeValue("name"));
        serverAddr = ldap.getChildTextTrim("serverAddr");
        if (StringUtils.isNotBlank(ldap.getChildTextTrim("serverPort"))) {
            serverPort = Integer.parseInt(ldap.getChildTextTrim("serverPort"));
        }
        apUserDn = ldap.getChildTextTrim("apUserDn");
        apPassword = ldap.getChildTextTrim("apPassword");
        loginBaseDn = ldap.getChildTextTrim("loginBaseDn");        
        userFilter = ldap.getChildTextTrim("userFilter");
        groupFilter = ldap.getChildTextTrim("groupFilter");
        userNameField = ldap.getChildTextTrim("userNameField");
        userAccountField = ldap.getChildTextTrim("userAccountField");
        groupNameField = ldap.getChildTextTrim("groupNameField");
        emailField = ldap.getChildTextTrim("emailField");
        groupDescField = ldap.getChildTextTrim("groupDescriptionField");
        defaultGroupName = ldap.getChildTextTrim("defaultGroupName");        
    }

	private static org.jdom.Document readXml(InputStream xmlInputStream) throws IOException, JDOMException {
		return XMLUtil.build(xmlInputStream).getDocument();
	}

	public String getLoginBaseDn() {
		return loginBaseDn;
	}

	public void setLoginBaseDn(String loginBaseDn) {
		this.loginBaseDn = StringUtils.lowerCase(loginBaseDn);
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String ip) {
		serverAddr = ip;
	}

	public String getApPassword() {
		return apPassword;
	}

	public void setApPassword(String password) {
		apPassword = password;
	}

	public Integer getServerPort() {
		return serverPort;
	}

	public void setServerPort(Integer port) {
		serverPort = port;
	}

	public String getApUserDn() {
		return apUserDn;
	}

	public void setApUserDn(String userDn) {
		apUserDn = userDn;
	}

	public String getUserFilter() {
		return userFilter;
	}

	public void setUserFilter(String userFilter) {
		this.userFilter = userFilter;
	}

	public String getGroupFilter() {
		return groupFilter;
	}

	public void setGroupFilter(String groupFilter) {
		this.groupFilter = groupFilter;
	}

	public String getUserNameField() {
		return userNameField;
	}

	public void setUserNameField(String userNameField) {
		this.userNameField = userNameField;
	}

	public String getUserAccountField() {
		return userAccountField;
	}

	public void setUserAccountField(String userAccountField) {
		this.userAccountField = userAccountField;
	}

	public String getGroupNameField() {
		return groupNameField;
	}

	public void setGroupNameField(String groupNameField) {
		this.groupNameField = groupNameField;
	}

	public String getEmailField() {
		return emailField;
	}

	public void setEmailField(String emailField) {
		this.emailField = emailField;
	}

	public String getGroupDescField() {
		return groupDescField;
	}

	public void setGroupDescField(String groupDescField) {
		this.groupDescField = groupDescField;
	}

	public boolean isSyncGroup() {
		return StringUtils.isNotBlank(groupFilter);
	}

	public String getDefaultGroupName() {
		return defaultGroupName;
	}

	public void setDefaultGroupName(String defaultGroupName) {
		this.defaultGroupName = defaultGroupName;
	}

	public boolean isAnonymousBind() {
		return StringUtils.isBlank(apUserDn);
	}

	public String getLdapName() {
		return ldapName;
	}

	public void setLdapName(String ldapName) {
		this.ldapName = ldapName;
	}
}
