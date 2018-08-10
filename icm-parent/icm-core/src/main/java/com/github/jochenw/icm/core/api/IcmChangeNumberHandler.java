package com.github.jochenw.icm.core.api;

import java.util.Comparator;

public interface IcmChangeNumberHandler<V extends Object> {
	V asVersion(String pVersion) throws NullPointerException, IllegalArgumentException;
	Comparator<V> getComparator();
	String asString(V version);
}
