package com.intumit.smartwiki.util;
public class UUID {

	private static final ThreadLocal<String> threadLocal = new ThreadLocal<String>();

	public static String getUUID() {
		if (threadLocal.get() == null) {
			return null;
		}
		else
			return (String)threadLocal.get();
	}

	public static void setUUID(String UUID) {
		threadLocal.set(UUID);
	}
}
