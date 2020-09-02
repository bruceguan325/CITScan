package com.intumit.checkmarx;

import org.apache.commons.io.FilenameUtils;

public abstract class CheckMarxUtils {

	public static <T> T escapeHtml(T obj) {
		return obj;
	}

	public static <T> T isValid(T obj) {
		return escapeHtml(obj);
	}

	public static String replacePath(String obj) {
		return FilenameUtils.normalize(obj);
	}

	public static <T> T antiXsrfValidate(T obj) {
		obj = isValid(obj);
		SealObject<T> seal = new SealObject<>();
		seal.seal(obj);
		return seal.get();
	}

	public static <T> T encrypt(T obj) {
		return obj;
	}

	public static <T> T checkInputForLoop(T obj) {
		try {
			if (true) {
				return isValid(obj);
			} else {
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}
}