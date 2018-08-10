package com.github.jochenw.icm.core.api.plugins;

import java.io.Reader;
import java.util.Iterator;

public interface SqlScriptReader {
	Iterator<String> read(Reader pReader);
}
