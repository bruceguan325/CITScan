package com.intumit.solr.util;

public class GetUrlContentException extends RuntimeException {
	int errorCode = -1;
	String errorStreamContent = null;

	public GetUrlContentException() {
		// TODO Auto-generated constructor stub
	}

	public GetUrlContentException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public GetUrlContentException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public GetUrlContentException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public GetUrlContentException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorStreamContent() {
		return errorStreamContent;
	}

	public void setErrorStreamContent(String errorStreamContent) {
		this.errorStreamContent = errorStreamContent;
	}
}
