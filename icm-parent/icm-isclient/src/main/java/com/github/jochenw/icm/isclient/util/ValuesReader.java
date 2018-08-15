package com.github.jochenw.icm.isclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import org.xml.sax.InputSource;

public interface ValuesReader {
	public <O> O read(InputSource pSource);

	public default <O> O read(Reader pReader) {
		final InputSource isource = new InputSource(pReader);
		return read(isource);
	}

	public default <O> O read(InputStream pStream) {
		final InputSource isource = new InputSource(pStream);
		return read(isource);
	}

	public default <O> O read(File pFile) {
		InputStream istream = null;
		Throwable th = null;
		O result = null;
		try {
			istream = new FileInputStream(pFile);
			result = read(istream);
			istream.close();
			istream = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (istream != null) {
				try {
					istream.close();
				} catch (Throwable t) {
					if (th == null) { th = t; }
				}
			}
		}
		if (th != null) {
			if (th instanceof RuntimeException) { throw (RuntimeException) th; }
			if (th instanceof Error) { throw (Error) th; }
			throw new UndeclaredThrowableException(th);
		}
		return result;
	}

	public default <O> O read(URL pUrl) {
		InputStream istream = null;
		Throwable th = null;
		O result = null;
		try {
			istream = pUrl.openStream();
			result = read(istream);
			istream.close();
			istream = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (istream != null) {
				try {
					istream.close();
				} catch (Throwable t) {
					if (th == null) { th = t; }
				}
			}
		}
		if (th != null) {
			if (th instanceof RuntimeException) { throw (RuntimeException) th; }
			if (th instanceof Error) { throw (Error) th; }
			throw new UndeclaredThrowableException(th);
		}
		return result;
	}


}
