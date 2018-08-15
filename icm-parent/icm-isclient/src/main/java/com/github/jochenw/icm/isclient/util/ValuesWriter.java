package com.github.jochenw.icm.isclient.util;

import java.io.OutputStream;
import java.util.Map;

public interface ValuesWriter {
	void write(Map<String,Object> pMap, OutputStream pStream);
}
