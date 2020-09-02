package com.intumit.message;

import java.util.Locale;

import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;

public class MessageUtil {
	private static MessageResources messageResources = null;
	public static final String KEY_MSG = "msg";
	public static final String KEY_RESET = "reset";
	public static void initialize() {
		MessageUtil.messageResources = MessageResourcesFactory.createFactory()
				.createResources("/default/messages");
	}

	public static MessageResources getMessageResources() {
		return messageResources;
	}

	public static String getMessage(Locale locale, String key, String[] args) {
		String msg = getMessageResources().getMessage(locale, key, args);
		return msg;
	}

	public static String getMessage(Locale locale, String key) {
		String msg = getMessageResources().getMessage(locale, key);
		return msg;
	}
	
	public static String returnKeyIfNotExist(Locale locale, String key) {
		MessageResources mr = getMessageResources();
		if(mr.isPresent(locale, key)) {
			return mr.getMessage(locale, key);
		}
		return key;
	}

}
