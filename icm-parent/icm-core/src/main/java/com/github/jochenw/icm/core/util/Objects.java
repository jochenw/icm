package com.github.jochenw.icm.core.util;

public class Objects {
	public static <O> O notNull(O pValue, O pDefault) {
		if (pValue == null) {
			return pDefault;
		} else {
			return pValue;
		}
	}

	public static void requireNonNull(Object pValue, String pMessage) {
		if (pValue == null) {
			if (pMessage == null) {
				throw new NullPointerException();
			} else {
				throw new NullPointerException(pMessage);
			}
		}
	}
}
