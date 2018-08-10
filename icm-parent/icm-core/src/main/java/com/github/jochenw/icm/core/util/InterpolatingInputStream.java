package com.github.jochenw.icm.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.function.Function;

public class InterpolatingInputStream extends FilterInputStream {
	private final Function<String,String> interpolator;
	private final Charset charset;
	private InputStream contents;
	private final String startToken, endToken;

	public InterpolatingInputStream(InputStream pIn, Function<String, String> pInterpolator, Charset pCharset, int pBufferSize) {
		this(pIn, pInterpolator, pCharset, pBufferSize, null, null);
	}
	public InterpolatingInputStream(InputStream pIn, Function<String, String> pInterpolator, Charset pCharset, int pBufferSize, String pStartToken, String pEndToken) {
		super(pIn);
		this.interpolator = pInterpolator;
		this.charset = pCharset;
		startToken = pStartToken == null ? "${" : pStartToken;
		endToken = pEndToken == null ? "}" : pEndToken;
	}

	public InterpolatingInputStream(InputStream pIn, Function<String, String> pInterpolator, Charset pCharset, String pStartToken, String pEndToken) {
		this(pIn, pInterpolator, pCharset, 16284, pStartToken, pEndToken);
	}
	public InterpolatingInputStream(InputStream pIn, Function<String, String> pInterpolator, Charset pCharset) {
		this(pIn, pInterpolator, pCharset, 16384);
	}

	protected void assertBufferFilled() throws IOException {
		if (contents == null) {
			final InputStream in = Streams.uncloseable(super.in);
			final StringBuilder sb = new StringBuilder();
			try (Reader r = new InputStreamReader(in, charset);
				 BufferedReader br = new BufferedReader(r)) {
				final char[] buffer = new char[8192];
				for (;;) {
					final int res = br.read(buffer);
					if (res == -1) {
						break;
					} else if (res > 0) {
						sb.append(buffer, 0, res);
					}
				}
			}
			contents = new ByteArrayInputStream(interpolate(sb).getBytes(charset));
		}
	}

	public String interpolate(StringBuilder pSb) {
		boolean finished = false;
		while (!finished) {
			finished = true;
			final int startOffset = pSb.lastIndexOf(startToken);
			if (startOffset != -1) {
				final int endOffset = pSb.indexOf(endToken, startOffset+startToken.length());
				if (endOffset != -1) {
					final String key = pSb.substring(startOffset+startToken.length(), endOffset);
					final String value = interpolator.apply(key);
					if (value == null) {
						throw new NullPointerException("Missing property: " + key);
					}
					pSb.replace(startOffset, endOffset+endToken.length(), value);
					finished = false;
				}
			}
		}
		return pSb.toString();
	}

	
	@Override
	public int read() throws IOException {
		assertBufferFilled();
		return contents.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		assertBufferFilled();
		return contents.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public boolean markSupported() {
		return false;
	}
}
