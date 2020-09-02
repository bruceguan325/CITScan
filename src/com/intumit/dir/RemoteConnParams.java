package com.intumit.dir;

import javax.servlet.http.HttpSession;

public class RemoteConnParams {

	private String host;
	private Authenticator auth;

	public RemoteConnParams(String host, Authenticator auth) {
		this.host = host;
		this.auth = auth;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setAuth(Authenticator auth) {
		this.auth = auth;
	}

	public Authenticator getAuth() {
		return auth;
	}

	public void setToSession(HttpSession session) {
		session.setAttribute(RemoteConnParams.class.getName(), this);
	}

	public static RemoteConnParams readFromSession(HttpSession session) {
		return (RemoteConnParams) session.getAttribute(RemoteConnParams.class
				.getName());
	}

	public static void clearSession(HttpSession session) {
		session.removeAttribute(RemoteConnParams.class.getName());
	}
}
