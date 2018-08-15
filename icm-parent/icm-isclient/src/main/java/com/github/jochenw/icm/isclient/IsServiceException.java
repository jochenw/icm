package com.github.jochenw.icm.isclient;

public class IsServiceException extends RuntimeException {
	private static final long serialVersionUID = -7905196145570315347L;
	private final int statusCode;
	private final String statusMessage;
	private final String remoteErrorMessage;
	private final String remoteErrorType;
	private final String remoteErrorTrace;

	public IsServiceException(int pStatusCode, String pStatusMessage, Throwable pCause) {
		this(pStatusCode, pStatusMessage);
		if (pCause != null) {
			initCause(pCause);
		}
	}

	public IsServiceException(int pStatusCode, String pStatusMessage) {
		this(pStatusCode, pStatusMessage, null, null, null);
	}

	public IsServiceException(int pStatusCode, String pStatusMessage, String pRemoteErrorMessage, String pRemoteErrorType,
			                  String pRemoteErrorTrace) {
		super("Invalid response: " + pStatusCode + ", " + pStatusMessage);
		statusCode = pStatusCode;
		statusMessage = pStatusMessage;
		remoteErrorMessage = pRemoteErrorMessage;
		remoteErrorType = pRemoteErrorType;
		remoteErrorTrace = pRemoteErrorTrace;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public String getRemoteErrorMessage() {
		return remoteErrorMessage;
	}

	public String getRemoteErrorType() {
		return remoteErrorType;
	}

	public String getRemoteErrorTrace() {
		return remoteErrorTrace;
	}
}
