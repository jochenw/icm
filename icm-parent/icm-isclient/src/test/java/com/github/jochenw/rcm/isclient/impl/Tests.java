package com.github.jochenw.rcm.isclient.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.junit.Assert;
import org.junit.AssumptionViolatedException;

import com.github.jochenw.rcm.isclient.RcmIsClient;

public class Tests {
	private static Properties props;
	private static RcmIsClient client;

	public static Properties getTestProperties() {
		if (props == null) {
			URL url = Tests.class.getResource("/rcm-isclient-test.properties");
			Assert.assertNotNull(url);
			try (InputStream istream = url.openStream()) {
				final Properties p = new Properties();
				p.load(istream);
				props = p;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return props;
	}
	
	public static RcmIsClient newRcmIsClient() throws MalformedURLException {
		if (client == null) {
			final Properties p = getTestProperties();
			final String url = p.getProperty("wmis.remote.url");
			final String userName = p.getProperty("wmis.remote.userName");
			final String password = p.getProperty("wmis.remote.password");
			if (url == null  ||  url.length() == 0  ||  "${wmis.remote.url}".equals(url)) {
				throw new AssumptionViolatedException("No IS Server available");
			} else {
				final URL u = new URL(url);
				client = new RcmIsClient(u, userName, password);
			}
		}
		return client;
	}

	public static boolean isTrue(Object pObject) {
		if (pObject == null) {
			return false;
		} else if (pObject instanceof Boolean) {
			return ((Boolean) pObject).booleanValue();
		} else if (pObject instanceof String) {
			return Boolean.parseBoolean((String) pObject);
		} else {
			throw new IllegalArgumentException("Invalid data type: " + pObject.getClass().getName());
		}
	}
}
