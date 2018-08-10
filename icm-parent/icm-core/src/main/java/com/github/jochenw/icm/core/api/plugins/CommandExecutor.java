package com.github.jochenw.icm.core.api.plugins;

import java.nio.charset.Charset;
import java.nio.file.Path;

public interface CommandExecutor {
	public interface ExecutionBuilder {
		public ExecutionBuilder outputFile(Path pPath);
		public ExecutionBuilder setErrorOutputFile(Path pPath);
		public ExecutionBuilder outputFileAppending();
		public ExecutionBuilder outputFileAppending(boolean pAppending);
		public ExecutionBuilder errorOutputFileAppending();
		public ExecutionBuilder errorOutputFileAppending(boolean pAppending);
		public ExecutionBuilder executable(String pExecutable);
		public ExecutionBuilder directory(Path path);
		public ExecutionBuilder inputFile(Path path);
		public ExecutionBuilder extension(String pExtension);
		public ExecutionBuilder inputCharset(Charset pCharset);
		public default ExecutionBuilder arg(String pValue) { return arg(pValue, null); }
		public ExecutionBuilder arg(String pValue, String pIf);
		public default ExecutionBuilder env(String pName, String pValue) { return env(pName, pValue, null); }
		public ExecutionBuilder env(String pName, String pValue, String pIf);
		public void run();
	}

	public ExecutionBuilder builder();
}
