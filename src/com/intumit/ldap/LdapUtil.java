package com.intumit.ldap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

public class LdapUtil {

	public static final void closeQuietly(LdapConnection conn, SearchCursor cursor) {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		} catch (Exception e) {
			// log.warn("close failed", e);
		}
	}

	public static final String getAttr(Dn dn, String attrName, LdapConnection conn) throws LdapException {
		Entry entry = conn.lookup(dn, attrName);
		Attribute attr = entry.get(attrName);
		return attr == null ? null : StringUtils.trimToNull(attr.getString());
	}

	public static final LdapConnection getNewConnAp(LdapConfig cfg) throws Exception {

		LdapConnection connAp = LdapUtil.getNewConn(cfg);
		try {
			if (cfg.isAnonymousBind()) {
				connAp.anonymousBind();
			} else {
				connAp.bind(cfg.getApUserDn(), cfg.getApPassword());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("AP Ldap connection failed");
		}
		return connAp;
	}

	public static final LdapConnection getNewConn(LdapConfig cfg) {
		LdapConnection connAp = new LdapNetworkConnection(cfg.getServerAddr(), cfg.getServerPort());
		return connAp;
	}

	public static SearchRequest getSearchRequest(String baseDn, String filter, SearchScope scope) throws LdapException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn(baseDn));
		searchRequest.setFilter(filter);
		searchRequest.setScope(scope);
		return searchRequest;
	}

	public static List<Dn> search(LdapConnection conn, Dn baseDn, String filter, SearchScope scope)
			throws LdapException {
		List<Dn> result = new ArrayList<Dn>();
		SearchCursor cursor = null;
		try {
			SearchRequest searchRequest = new SearchRequestImpl();
			searchRequest.setBase(baseDn);
			searchRequest.setFilter(filter);
			searchRequest.setScope(scope);
			cursor = conn.search(searchRequest);
			while (cursor.next() && cursor.isEntry()) {
				result.add(cursor.getEntry().getDn());
			}
		} catch (CursorException e) {
			throw new LdapException(e);
		} finally {
			closeQuietly(null, cursor);
		}
		return result;
	}

}
