package com.github.jochenw.icm.isclient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUrlConnector {
	public HttpURLConnection connect(URL pUrl) {
		try {
			return (HttpURLConnection) pUrl.openConnection();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
