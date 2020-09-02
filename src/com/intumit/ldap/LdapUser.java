package com.intumit.ldap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.util.WiSeEnv;

public class LdapUser implements Serializable {
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		
		LdapUser adUser = new LdapUser(
				LdapConfig.getCfgFromFile(new File(WiSeEnv.getHomePath() + "/../sample/ldap-cfg-sample.xml")), 
				"AdUser",
				"AdPasswd",
				"127.0.0.1"
				);
		System.out.println(adUser.isLogin());
	}
	
	Logger logger = LoggerFactory.getLogger(LdapUser.class);

	public static final String SESSION_KEY = LdapUser.class.getName();
	public static final String LDAP_ENTRY_ATTRIBUTE_MEMBEROF = "memberOf";

	private LdapConfig cfg;
	private String account;
	private String accountId;
	private String password;
	private String username;
	private String email;
	private String ip;
	private List<String> memberOfPathList = new ArrayList<String>();
	
	public LdapUser(LdapConfig cfg, String account, String password, String ip, List<String> memberOfPathList) {
		this.cfg = cfg;
		this.account = account;
		this.password = password;
		this.ip = ip;
		this.memberOfPathList = memberOfPathList;
	}

	public LdapUser(LdapConfig cfg, String account, String password, String ip) {
		this.cfg = cfg;
		this.account = account;
		this.password = password;
		this.ip = ip;
	}

	public boolean isLogin() {
		boolean isLogin = true;

		try {
			logger.info("Login check starts...");
			doLoginAndSetMemberOfList();
			logger.info("Login check ends.");
		} catch (LdapLoginException e) {
			isLogin = false;
		}

		return isLogin;
	}

	private void doLoginAndSetMemberOfList() throws LdapLoginException {
		LdapConnection connAp = null;
		SearchCursor cursor = null;
		Dn userDn = null;

		if (cfg != null) {
			try {
				logger.info("Connect ldap...");
				connAp = LdapUtil.getNewConnAp(cfg);
				SearchRequest searchRequest = LdapUtil.getSearchRequest(cfg.getLoginBaseDn(),
						"(&" + cfg.getUserFilter() + "(" + cfg.getUserAccountField() + "=" + account + "))",
						SearchScope.SUBTREE);
				cursor = connAp.search(searchRequest);
				List<Entry> entrys = new ArrayList<Entry>();
				while (cursor.next() && cursor.isEntry()) {
					entrys.add(cursor.getEntry());
				}
				
				if (entrys.size() == 1) {
					Entry entry = entrys.get(0);
					userDn = entry.getDn();
					String name = entry.get(cfg.getUserNameField()).getString();
					this.setUsername(name);
					String accountId = entry.get(cfg.getUserAccountField()).getString();
					this.setAccountId(accountId);
					this.setEmail(accountId+"@");
					logger.info(String.format("Ldap-name:%s accountID:%s", name, accountId));
					
					checkUserAllowedLogin(entrys.get(0).get(LDAP_ENTRY_ATTRIBUTE_MEMBEROF), connAp);
				} else if (entrys.size() > 1) {
					throw new LdapLoginException("使用者不只一個:" + account);
				} else if (entrys.isEmpty()) {
					throw new LdapLoginException("找不到使用者:" + account);
				}
			} catch (Exception e) {
				throw new LdapLoginException("LdapException!!", e);
			} finally {
				LdapUtil.closeQuietly(connAp, cursor);
			}

			if (StringUtils.isBlank(password)) {
				throw new LdapLoginException("使用者登入失敗： " + account + "，密碼為空！");
			}

			LdapConnection connUser = LdapUtil.getNewConn(cfg);

			try {
				logger.info("Check account/password...");
				connUser.bind(userDn, password);
				if (!connUser.isAuthenticated()) {
					throw new LdapLoginException("使用者登入失敗: " + account);
				}
			} catch (LdapException e) {
				throw new LdapLoginException("LdapException!!", e);
			} finally {
				LdapUtil.closeQuietly(connUser, null);
			}
		}
	}

	private String convertToPath(String dnFormat) {
		String tempPath = dnFormat.substring(3);
		// ex:
		// CN=wise-all,OU=ma_getac_group_test,OU=ma_getac,OU=ma_tw,OU=ma,DC=getacad,DC=com
		tempPath = tempPath.replace(",OU=", "/");
		tempPath = tempPath.substring(0, tempPath.indexOf(",DC=")) + "/"
				+ tempPath.substring(tempPath.indexOf(",DC=") + 4);
		tempPath = tempPath.replace(",DC=", ".");

		return tempPath;
	}

	private String reversePath(String path) {
		String reversePath = "";

		if (path != null && !path.trim().equals("")) {
			String[] names = path.split("/");
			for (int i = 0; i < names.length; i++) {
				if (i == 0) {
					reversePath = names[i];
				} else {
					reversePath = names[i] + "/" + reversePath;
				}
			}
		}

		return reversePath;
	}

	private void checkUserAllowedLogin(Attribute memberOfs, LdapConnection connAp) throws LdapLoginException {

		try {
			logger.info("Search memberOf...");
			while (memberOfs != null && memberOfs.size() > 0) {
				String memberOf = memberOfs.getString();
				String searchFilter = cfg.getGroupFilter();
				SearchRequest searchRequest = LdapUtil.getSearchRequest(memberOf, searchFilter, SearchScope.SUBTREE);
				SearchCursor cursor = connAp.search(searchRequest);
				while (cursor.next() && cursor.isEntry()) {
					Attribute tempAttr = cursor.getEntry().get(LDAP_ENTRY_ATTRIBUTE_MEMBEROF);
					if (tempAttr != null && !memberOfs.contains(tempAttr.getString())) {
						memberOfs.add(cursor.getEntry().get(LDAP_ENTRY_ATTRIBUTE_MEMBEROF).getString());
					}
				}

				String tempPath = convertToPath(memberOf);
				tempPath = reversePath(tempPath);

				logger.info("memberOf: " + tempPath);

				memberOfs.remove(memberOf);
			}
		} catch (Exception e) {
			throw new LdapLoginException("Check memberOf has Exception!!", e);
		}
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}		

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<String> getMemberOfPath() {
		return memberOfPathList;
	}

	public void setMemberOfPath(List<String> memberOfPathList) {
		this.memberOfPathList = memberOfPathList;
	}

	public static LdapUser getFromSession(HttpSession sess) {
		return (LdapUser) sess.getAttribute(SESSION_KEY);
	}

	public static void setToSession(HttpSession sess, LdapUser ldapUser) {
		sess.setAttribute(SESSION_KEY, ldapUser);
	}

	public static void removeFromSession(HttpSession sess) {
		sess.removeAttribute(SESSION_KEY);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
}

