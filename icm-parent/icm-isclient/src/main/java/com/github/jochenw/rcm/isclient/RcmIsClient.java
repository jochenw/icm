package com.github.jochenw.rcm.isclient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.ws.commons.util.Base64;

import com.github.jochenw.rcm.isclient.util.Exceptions;
import com.github.jochenw.rcm.isclient.util.ValuesReader;
import com.github.jochenw.rcm.isclient.util.ValuesWriter;
import com.github.jochenw.rcm.isclient.util.DefaultValuesReader;
import com.github.jochenw.rcm.isclient.util.DefaultValuesWriter;

public class RcmIsClient implements ServiceInvoker {
	private final HttpUrlConnector connector;
	private final String hostName;
	private final int port;
	private final boolean usingSsl;
	private final String userName;
	private final String password;
	private ValuesReader valuesReader = new DefaultValuesReader();
	private ValuesWriter valuesWriter = new DefaultValuesWriter();

	public RcmIsClient(String pHostName, int pPort, boolean pUsingSsl, String pUserName, String pPassword) {
		this(new HttpUrlConnector(), pHostName, pPort, pUsingSsl, pUserName, pPassword);
	}

	public RcmIsClient(URL pUrl, String pUserName, String pPassword) {
		this(new HttpUrlConnector(), pUrl, pUserName, pPassword);
	}

	public RcmIsClient(HttpUrlConnector pConnector, String pHostName, int pPort, boolean pUsingSsl, String pUserName, String pPassword) {
		connector = pConnector;
		hostName = pHostName;
		port = pPort;
		usingSsl = pUsingSsl;
		userName = pUserName;
		password = pPassword;
	}

	public RcmIsClient(HttpUrlConnector pConnector, URL pUrl, String pUserName, String pPassword) {
		connector = pConnector;
		switch (pUrl.getProtocol()) {
		case "http":
			hostName = pUrl.getHost();
			port = (pUrl.getPort() == -1) ? 5555 : pUrl.getPort();
			usingSsl = false;
			userName = pUserName;
			password = pPassword;
			break;
		case "https":
			hostName = pUrl.getHost();
			port = (pUrl.getPort() == -1) ? 443 : pUrl.getPort();
			usingSsl = true;
			userName = pUserName;
			password = pPassword;
			break;
		default:
			throw new IllegalStateException("Invalid protocol: " + pUrl.getProtocol() + " (Expected http, or https)");
		}
	}

	public HttpUrlConnector getConnector() {
		return connector;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}

	public boolean isUsingSsl() {
		return usingSsl;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public Map<String, Object> invoke(String pFullyQualifiedService, Map<String, Object> pInput) {
		HttpURLConnection conn = null;
		Throwable th = null;
		Map<String,Object> output = null;
		try {
			final URL url = getUrl(pFullyQualifiedService);
			conn = connector.connect(url);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "text/xml");
			conn.setRequestProperty("Content-Type", "text/xml");
			if (userName != null  &&  userName.length() > 0) {
				final String auth = "Administrator:manage";
				byte[] bytes = auth.getBytes("UTF-8");
				final String authEncoded = Base64.encode(bytes, 0, bytes.length, 0, null);
				conn.setRequestProperty("Authorization", "Basic " + authEncoded);
			}
			conn.setDoOutput(true);
			conn.setDoInput(true);
			try (OutputStream os = conn.getOutputStream()) {
				valuesWriter.write(pInput, os);
			}
			final int statusCode = conn.getResponseCode();
			if (statusCode >= 200  &&  statusCode < 300) {
				try (InputStream is = conn.getInputStream()) {
					output = valuesReader.read(is);
				}
			} else {
				try {
					final String msg = conn.getResponseMessage();
					try {
						try (InputStream es = conn.getErrorStream()) {
							final Map<String,Object> error = valuesReader.read(es);
							@SuppressWarnings("unchecked")
							final Map<String,Object> errorInfo = (Map<String,Object>) error.get("$errorInfo");
							if (errorInfo != null) {
								final String remoteErrorMessage = (String) errorInfo.get("$error");
								final String remoteErrorType = (String) errorInfo.get("$errorType");
								final String remoteErrorTrace = (String) errorInfo.get("$errorDump");
								throw new IsServiceException(statusCode, msg, remoteErrorMessage, remoteErrorType, remoteErrorTrace);
							}
						}
					} catch (Throwable t) {
						System.err.println(t);
						throw new IsServiceException(statusCode, msg);
					}
				} catch (Throwable t) {
					System.err.println(t);
					throw new IsServiceException(statusCode, "Unknown error");
				}
			}
			conn.disconnect();
			conn = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Throwable t) {
					if (th == null) {
						th = t;
					}
				}
			}
		}
		if (th != null) {
			throw Exceptions.show(th);
		}
		return output;
	}

	protected URL getUrl(String pFullyQualifiedService) throws MalformedURLException {
		final StringBuilder urlSb = new StringBuilder();
		if (usingSsl) {
			urlSb.append("https");
		} else {
			urlSb.append("http");
		}
		urlSb.append("://");
		urlSb.append(hostName);
		urlSb.append(':');
		urlSb.append(port);
		urlSb.append("/invoke/");
		final int offset = pFullyQualifiedService.indexOf(':');
		if (offset == -1) {
			throw new IllegalStateException("Unable to parse fully qualified service name: " + pFullyQualifiedService
					                        + " (Expected ':')");
		}
		final String namespace = pFullyQualifiedService.substring(0, offset);
		urlSb.append(namespace);
		final String service = pFullyQualifiedService.substring(offset+1);
		urlSb.append('/');
		urlSb.append(service);
		return new URL(urlSb.toString());
	}

	public ValuesReader getValuesReader() {
		return valuesReader;
	}

	public void setValuesReader(ValuesReader valuesReader) {
		this.valuesReader = valuesReader;
	}

	public ValuesWriter getValuesWriter() {
		return valuesWriter;
	}

	public void setValuesWriter(ValuesWriter valuesWriter) {
		this.valuesWriter = valuesWriter;
	}
}
