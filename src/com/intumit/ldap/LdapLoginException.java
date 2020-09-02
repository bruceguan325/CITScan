package com.intumit.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapLoginException extends Exception {

	private static final long serialVersionUID = 1L;
	
	Logger logger = LoggerFactory.getLogger(LdapLoginException.class);
	
	public LdapLoginException() {
		super();
	}

	public LdapLoginException(String message) {
		super(message);
		logger.error(message);
	}
	
	public LdapLoginException(Throwable cause) {
		super(cause);
		logger.error("LdapLoginException", cause);
	}
	
	public LdapLoginException(String message, Throwable cause) {
		super(message, cause);
		logger.error(message, cause);
	}	
}
