package com.github.jochenw.icm.core.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

public class Exceptions {
	public static RuntimeException show(Throwable pTh) {
		Objects.requireNonNull(pTh, "Throwable");
		if (pTh instanceof RuntimeException) {
			throw (RuntimeException) pTh;
		} else if (pTh instanceof Error) {
			throw (Error) pTh;
		} else if (pTh instanceof IOException) {
			throw new UncheckedIOException((IOException) pTh);
		} else {
			throw new UndeclaredThrowableException(pTh);
		}
	}

	public static <T extends Throwable> RuntimeException show(Throwable pTh, Class<T> pType) throws T {
		Objects.requireNonNull(pTh, "Throwable");
		if (pType.isAssignableFrom(pTh.getClass())) {
			throw pType.cast(pTh);
		} else if (pTh instanceof RuntimeException) {
			throw (RuntimeException) pTh;
		} else if (pTh instanceof Error) {
			throw (Error) pTh;
		} else if (pTh instanceof IOException) {
			throw new UncheckedIOException((IOException) pTh);
		} else {
			throw new UndeclaredThrowableException(pTh);
		}
	}
}
