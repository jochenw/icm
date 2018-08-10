package com.github.jochenw.icm.core.impl.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.CommandExecutor;
import com.github.jochenw.icm.core.api.plugins.ExpressionEvaluator;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.Streams;

public class DefaultCommandExecutor implements CommandExecutor {
	public static class Arg {
		private final String value;
		private final String ifExpression;
		public Arg(String pValue, String pIfExpression) {
			value = pValue;
			ifExpression = pIfExpression;
		}
		public String getValue() {
			return value;
		}
		public String getIfExpression() {
			return ifExpression;
		}
	}
	public static class Env {
		private final String name;
		private final String value;
		private final String ifExpression;
		public Env(String pName, String pValue, String pIfExpression) {
			name = pName;
			value = pValue;
			ifExpression = pIfExpression;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
		public String getIfExpression() {
			return ifExpression;
		}
	}
	public class MyExecution implements CommandExecutor.ExecutionBuilder {
		private Path directory;
		private String executable, extension;
		private Path outputFile, errorOutputFile, inputFile;
		private boolean outputFileAppending, errorOutputFileAppending;
		private List<Arg> args = new ArrayList<>();
		private Map<String,Env> env = new HashMap<>();

		@Override
		public ExecutionBuilder outputFile(Path pPath) {
			outputFile = pPath;
			return this;
		}

		@Override
		public ExecutionBuilder setErrorOutputFile(Path pPath) {
			errorOutputFile = pPath;
			return this;
		}

		@Override
		public ExecutionBuilder outputFileAppending() {
			return outputFileAppending(true);
		}

		@Override
		public ExecutionBuilder outputFileAppending(boolean pAppending) {
			outputFileAppending = pAppending;
			return this;
		}

		@Override
		public ExecutionBuilder errorOutputFileAppending() {
			return errorOutputFileAppending(true);
		}

		@Override
		public ExecutionBuilder errorOutputFileAppending(boolean pAppending) {
			errorOutputFileAppending = pAppending;
			return this;
		}

		@Override
		public ExecutionBuilder executable(String pExecutable) {
			executable = pExecutable;
			return this;
		}

		@Override
		public ExecutionBuilder extension(String pExtension) {
			extension = pExtension;
			return this;
		}

		@Override
		public ExecutionBuilder directory(Path pDirectory) {
			directory = pDirectory;
			return this;
		}

		@Override
		public ExecutionBuilder inputFile(Path pPath) {
			inputFile = pPath;
			return this;
		}

		@Override
		public ExecutionBuilder inputCharset(Charset pCharset) {
			// Do nothing
			return this;
		}

		@Override
		public ExecutionBuilder arg(String pValue, String pIfExpression) {
			args.add(new Arg(pValue, pIfExpression));
			return this;
		}

		@Override
		public ExecutionBuilder env(String pName, String pValue, String pIfExpression) {
			env.put(pName, new Env(pName, pValue, pIfExpression));
			return this;
		}

		@Override
		public void run() {
			DefaultCommandExecutor.this.run(this);
		}

		public String getExecutable() {
			return executable;
		}
	}

	@InjectLogger private IcmLogger logger;
	@Inject private ExpressionEvaluator expressionEvaluator;
	
	@Override
	public ExecutionBuilder builder() {
		return new MyExecution();
	}

	protected void run(MyExecution pExecution) {
		final Runtime rt = Runtime.getRuntime();
		final String[] env;
		if (pExecution.env.isEmpty()) {
			env = null;
		} else {
			final List<String> envList = new ArrayList<String>(pExecution.env.size());
			for (Env en : pExecution.env.values()) {
				if (en.getIfExpression() == null || expressionEvaluator.evaluate(en.ifExpression)) {
					envList.add(en.getName() + "=" + en.getValue());
				}
			}
			env = envList.toArray(new String[envList.size()]);
		}
		final List<String> cmd = new ArrayList<String>(pExecution.args.size()+1);
		cmd.add(getExecutable(pExecution).toString());
		for (Arg arg : pExecution.args) {
			if (arg.getIfExpression() == null || expressionEvaluator.evaluate(arg.ifExpression)) {
				cmd.add(arg.getValue());
			}
		}
		final String[] args = cmd.toArray(new String[cmd.size()]);
		final StringBuilder commandSb = new StringBuilder();
		for (int i = 0;  i < args.length;  i++) {
			if (i > 0) {
				commandSb.append(' ');
			}
			commandSb.append(args[i]);
		}
		final String cmdDescription = commandSb.toString();
		logger.debug("Running command: " + cmdDescription);
		final Process process;
		try {
			if (pExecution.directory == null) {
				process = rt.exec(args, env);
			} else {
				process = rt.exec(args, env, pExecution.directory.toFile());
			}
		} catch (IOException e) {
			logger.error("Error while invoking command: " + cmdDescription, e);
			throw Exceptions.show(e);
		}
		if (pExecution.inputFile != null) {
			logger.debug("Reading input from: " + pExecution.inputFile.toAbsolutePath());
			try (InputStream in = Files.newInputStream(pExecution.inputFile)) {
				Streams.copy(in, process.getOutputStream());
			} catch (IOException e) {
				logger.error("Error while reading input file: " + e.getMessage(), e);
				throw Exceptions.show(e);
			}
		}
		try (InputStream pout = process.getInputStream();
			 InputStream perr = process.getErrorStream()) {
			handleOutput(pExecution.outputFile, pout, pExecution.outputFileAppending, "output");
			handleOutput(pExecution.errorOutputFile, pout, pExecution.errorOutputFileAppending, "error output");
		} catch (IOException e) {
			final String msg = "Error hile invoking command (" + cmdDescription + "): " + e.getMessage();
			logger.error(msg, e);
			throw Exceptions.show(e);
		}
	}

	protected void handleOutput(Path pFile, InputStream pIn, boolean pAppend, String pDescription) {
		if (pFile != null) {
			logger.debug("Writing " + pDescription + " to: " + pFile.toAbsolutePath());
			try (OutputStream out = open(pFile, pAppend)) {
				Streams.copy(pIn, out);
				pIn.close();
			} catch (IOException e) {
				logger.error("Error while writing " + pDescription + " file: " + e.getMessage(), e);
				throw Exceptions.show(e);
			}
		} else {
			final String description = Character.toUpperCase(pDescription.charAt(0)) + pDescription.substring(1);
			try (final BufferedInputStream bis = new BufferedInputStream(pIn);
				 final InputStreamReader isr = new InputStreamReader(bis);
				 final BufferedReader br = new BufferedReader(isr)) {
				for (;;) {
					String line;
					try {
						line = br.readLine();
					} catch (IOException e) {
						if (e.getMessage().indexOf("Stream closed") != -1) {
							line = null;
						} else {
							throw e;
						}
					}
					if (line == null) {
						break;
					} else {
						logger.debug(description + " line: " + line);
					}
				}
			} catch (IOException e) {
				logger.error("Error while reading " + pDescription + ": " + e.getMessage(), e);
				throw Exceptions.show(e);
			}
		}
	}
	
	protected OutputStream open(Path pPath, boolean pAppend) throws IOException {
		final Path dir = pPath.getParent();
		if (dir != null) {
			Files.createDirectories(dir);
		}
		if (pAppend) {
			return Files.newOutputStream(pPath, StandardOpenOption.APPEND);
		} else {
			return Files.newOutputStream(pPath);
		}
	}

	protected Path getExecutable(MyExecution pExecution) {
		final List<String> paths = new ArrayList<String>();
		String path = pExecution.executable;
		if (path.endsWith(".")) {
			path = path.substring(0, path.length()-1);
		}
		String extension = pExecution.extension;
		if ("auto".equals(extension)) {
			paths.add(path + ".sh");
			paths.add(path + ".cmd");
			paths.add(path + ".bat");
		} else if (extension != null) {
			if (extension.startsWith(".")) {
				paths.add(path + extension);
			} else {
				paths.add(path + "." + extension);
			}
		}
		for (String s : paths) {
			final Path p = Paths.get(s);
			if (Files.isExecutable(p)) {
				return p;
			}
		}
		return Paths.get(pExecution.executable);
	}
}
