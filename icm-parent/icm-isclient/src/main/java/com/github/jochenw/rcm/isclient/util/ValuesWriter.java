package com.github.jochenw.rcm.isclient.util;

import java.io.OutputStream;
import java.util.Map;

public interface ValuesWriter {
	void write(Map<String,Object> pMap, OutputStream pStream);
}
