package com.github.jochenw.rcm.isclient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.ws.commons.util.Base64;

public class PackageListReader {
	public void run() throws Exception {
		final URL url = new URL("http://127.0.0.1:5555/invoke/wm.server.packages/packageList");
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "text/xml");
		conn.setRequestProperty("User-Agent", "webMethods");
		final String auth = "Administrator:manage";
		byte[] bytes = auth.getBytes("UTF-8");
		final String authEncoded = Base64.encode(bytes, 0, bytes.length, 0, null);
		conn.setRequestProperty("Authorization", "Basic " + authEncoded);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		OutputStream os = conn.getOutputStream();
		os.write("<?xml version='1.0' encoding='UTF-8'?><Values version='2.0'\r\n\r\n".getBytes("UTF-8"));
		os.close();
		final Map<String,List<String>> headers = conn.getHeaderFields();
		for (String key : headers.keySet()) {
			final List<String> values = headers.get(key);
			for (String value : values) {
				System.out.println(key + "=" + value);
			}
		}
		final int statusCode = conn.getResponseCode();
		System.out.println("Statuscode = " + statusCode);
		final byte[] buffer = new byte[8192];
		final InputStream is = conn.getInputStream();
		for (;;) {
			final int res = is.read(buffer);
			if (res == -1) {
				break;
			} else if (res > 0) {
				System.out.write(buffer, 0, res);
			}
		}
		is.close();
		conn.disconnect();
	}

	public static void main(String[] pArgs) throws Exception {
		new PackageListReader().run();
	}
}
