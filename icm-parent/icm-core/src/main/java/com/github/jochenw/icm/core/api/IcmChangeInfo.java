package com.github.jochenw.icm.core.api;

public interface IcmChangeInfo<V extends Object> {
    String getTitle();
	V getVersion();
	String getType();
	String getAttribute(String pKey);
}
