package com.github.jochenw.icm.core.util;

public class Strings {
	public static String replace(String pValue, String pFrom, String pTo) {
		int offset = pValue.indexOf(pFrom);
		if (offset == -1) {
			return pValue;
		} else {
			final StringBuilder sb = new StringBuilder();
			sb.append(pValue.subSequence(0, offset));
			sb.append(pTo);
			sb.append(pValue.subSequence(offset + pFrom.length(), pValue.length()));
			return sb.toString();
		} 
	}
}
