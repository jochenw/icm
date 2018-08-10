package com.github.jochenw.icm.core.util;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Utility class for working with streams.
 */
public class Streams {
	public static InputStream uncloseable(InputStream pIn) {
		return new FilterInputStream(pIn) {
			@Override
			public void close() {}
		};
	}
	public static OutputStream uncloseable(OutputStream pOut) {
		return new FilterOutputStream(pOut) {
			@Override
			public void close() {}
		};
	}
	public static void copy(InputStream pIn, OutputStream pOut) throws IOException {
		final byte[] buffer = new byte[8192];
		for (;;) {
			final int res = pIn.read(buffer);
			if (res == -1) {
				return;
			} else if (res > 0) {
				pOut.write(buffer, 0, res);
			}
		}
	}
}
