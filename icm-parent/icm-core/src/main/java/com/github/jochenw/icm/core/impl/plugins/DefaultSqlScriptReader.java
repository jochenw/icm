package com.github.jochenw.icm.core.impl.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import com.github.jochenw.icm.core.api.plugins.SqlScriptReader;
import com.github.jochenw.icm.core.util.Exceptions;

public class DefaultSqlScriptReader implements SqlScriptReader {
	@Override
	public Iterator<String> read(final Reader pReader) {
		final BufferedReader br = new BufferedReader(pReader);
		final StringBuilder sb = new StringBuilder();
		return new Iterator<String>() {
			boolean eofSeen;
			private String line;

			@Override
			public boolean hasNext() {
				if (eofSeen) {
					return false;
				} else if (line != null) {
					return true;
				} else {
					try {
						line = getLine(br, sb);
						if (line == null) {
							br.close();
							eofSeen = true;
						}
						return line != null;
					} catch (IOException e) {
						throw Exceptions.show(e);
					}
				}
			}

			@Override
			public String next() {
				final String s = line;
				line = null;
				if (s == null) {
					throw new IllegalStateException("No more lines available. Did you call hasNext()?");
				}
				return s;
			}
		};
	}

	protected String getLine(BufferedReader pReader, StringBuilder pSb) throws IOException {
		pSb.setLength(0);
		for (;;) {
			final String line = pReader.readLine();
			if (line == null) {
				return pSb.length() == 0 ? null : pSb.toString();
			} else {
				String l = line;
				final int offset = l.indexOf("--");
				if (offset != -1) {
					l = l.substring(0, offset);
				}
				if (pSb.length() > 0) {
					pSb.append('\n');
				}
				pSb.append(l);
				if (l.endsWith(";")) {
					pSb.setLength(pSb.length()-1);
					return pSb.toString();
				}
			}
		}
	}
}
