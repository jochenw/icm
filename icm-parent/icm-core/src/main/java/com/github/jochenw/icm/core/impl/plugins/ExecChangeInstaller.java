package com.github.jochenw.icm.core.impl.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.xml.sax.InputSource;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.AbstractChangeInstaller;
import com.github.jochenw.icm.core.api.plugins.CommandExecutor;
import com.github.jochenw.icm.core.api.plugins.CommandExecutor.ExecutionBuilder;
import com.github.jochenw.icm.core.api.plugins.ResourceInstaller;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.api.prop.Interpolator;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.JaxbUtils;
import com.github.jochenw.icm.core.util.Streams;
import com.github.namespaces.jochenw.icm.core.schema.exec._1_0.Commands;
import com.github.namespaces.jochenw.icm.core.schema.exec._1_0.TCommand;
import com.github.namespaces.jochenw.icm.core.schema.exec._1_0.TCommand.Arg;
import com.github.namespaces.jochenw.icm.core.schema.exec._1_0.TCommand.Env;

@ResourceInstaller
public class ExecChangeInstaller extends AbstractChangeInstaller {
	public static final String TYPE_ID = "exec";

	@InjectLogger private IcmLogger logger;
	@Inject private Interpolator interpolator;
	@Inject private IcmPropertyProvider propertyProvider;
	@Inject private CommandExecutor commandExecutor;
	@Inject ClassLoader classLoader;

	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		return TYPE_ID.equals(pInfo.getType());
	}

	@Override
	public void install(Context pContext) {
		try (InputStream in = pContext.open();
			 InputStream in2 = interpolator.filter(in, (s) -> propertyProvider.getProperty(s),
					                                        pContext.getResource().getCharset())) {
			final InputSource isource = new InputSource(in2);
			isource.setSystemId(pContext.getResource().getUri());
			final Commands commands = (Commands) JaxbUtils.parse(isource, Commands.class);
			for (TCommand command : commands.getCommand()) {
				execute(commands, command);
			}
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}

	protected void execute(Commands pCommands, TCommand pCommand) {
		final ExecutionBuilder eb = commandExecutor.builder();
		final List<Path> temporaryFiles = new ArrayList<>();
		final List<Path> temporaryDirectories = new ArrayList<>();
		final String executable = pCommand.getExecutable();
		try {
			if (executable == null  || executable.length() == 0) {
				throw new IllegalStateException("Missing, or empty attribute for exec command: executable");
			}
			eb.executable(executable);
			String extension = pCommand.getExtension();
			if (extension == null) {
				extension = pCommands.getExtension();
			}
			if (extension != null) {
				eb.extension(extension);
			}
			String oFile = pCommand.getOutputFile();
			if (oFile == null) {
				oFile = pCommands.getOutputFile();
			}
			if (oFile != null) {
				eb.outputFile(Paths.get(oFile));
				eb.outputFileAppending(pCommand.isOutputFileAppend()  ||  pCommands.isOutputFileAppend());
			}
			String eoFile = pCommand.getErrorOutputFile();
			if (eoFile == null) {
				eoFile = pCommands.getErrorOutputFile();
			}
			if (eoFile != null) {
				eb.setErrorOutputFile(Paths.get(eoFile));
				eb.errorOutputFileAppending(pCommand.isErrorOutputFileAppend()
						||  pCommands.isErrorOutputFileAppend());
			}
			String dir = pCommand.getDir();
			if (dir == null) {
				dir = pCommands.getDir();
			}
			if (dir != null) {
				eb.directory(Paths.get(dir));
			}
			String ifile = pCommand.getInputFile();
			if (ifile == null) {
				ifile = pCommands.getInputFile();
			}
			if (ifile != null) {
				eb.inputFile(Paths.get(ifile));
				String icharset = pCommand.getInputCharset();
				if (icharset == null) {
					icharset = pCommands.getInputCharset();
				}
				final Charset cs;
				if (icharset != null) {
					try {
						cs = Charset.forName(icharset);
					} catch (IllegalCharsetNameException e) {
						throw new IllegalStateException("Invalid charset name: " + icharset, e);
					} catch (UnsupportedCharsetException e) {
						throw new IllegalStateException("Unsupported charset name: " + icharset, e);
					}
				} else {
					cs = StandardCharsets.UTF_8;
				}
				eb.inputCharset(cs);
			}
			for (Object o : pCommand.getArgOrEnv()) {
				if (o instanceof Arg) {
					Arg arg = (Arg) o;
					String value = null;
					int numValues = 0;
					if (arg.getLocation() != null) {
						value = Paths.get(arg.getLocation()).toAbsolutePath().toString();
						++numValues;
					}
					if (arg.getValue() != null) {
						value = arg.getValue();
						++numValues;
					}
					if (arg.getTempFile() != null) {
						final Path path = createTempFile(temporaryDirectories, arg.getTempFile(), arg.getTempFileName());
						temporaryFiles.add(path);
						value = path.toString();
						++numValues;
					}
					if (numValues == 0) {
						throw new IllegalStateException("Either of the following attributes is required for commands/command/arg: @value, @location, or @url");
					} else if (numValues > 1) {
						throw new IllegalStateException("The following attributes are mutually exclusive for commands/command/arg: @value, @location, and @url");
					}
					eb.arg(value);
				} else if (o instanceof Env) {
					Env env = (Env) o;
					final String name = env.getName();
					if (name == null  ||  name.length() == 0) {
						throw new IllegalStateException("Missing, or empty attribute: commands/command/env/@name");
					}
					String value = null;
					int numValues = 0;
					if (env.getLocation() != null) {
						value = Paths.get(env.getLocation()).toAbsolutePath().toString();
						++numValues;
					}
					if (env.getValue() != null) {
						value = env.getValue();
						++numValues;
					}
					if (env.getTempFile() != null) {
						final Path path = createTempFile(temporaryDirectories, env.getTempFile(), env.getTempFileName());
						temporaryFiles.add(path);
						value = path.toString();
						++numValues;
					}
					if (numValues == 0) {
						throw new IllegalStateException("Either of the following attributes is required for commands/command/arg: @value, @location, or @url");
					} else if (numValues > 1) {
						throw new IllegalStateException("The following attributes are mutually exclusive for commands/command/arg: @value, @location, and @url");
					}
					eb.env(name, value);
				} else {
					throw new IllegalStateException("Invalid object type: " + o.getClass().getName());
				}
			}
			eb.run();
		} finally {
			cleanTempFiles(temporaryFiles, temporaryDirectories);
		}
	}

	protected void cleanTempFiles(List<Path> pTempFiles, List<Path> pTempDirs) {
		IOException ex = null;
		for (Path tempFile : pTempFiles) {
			try {
				logger.debug("Deleting temporary file: " + tempFile);
				Files.delete(tempFile);
			} catch (IOException e) {
				logger.warn("Failed to delete temporary file " + tempFile + ": " + e.getMessage());
				logger.warn(e);
				if (ex == null) {
					ex = e;
				}
			}
		}
		pTempFiles.clear();
		for (Path tempDir : pTempDirs) {
			try {
				logger.debug("Deleting temporary directory: " + tempDir);
				com.github.jochenw.icm.core.util.Files.deleteDirectory(tempDir);
			} catch (IOException e) {
				logger.warn("Failed to delete temporary directory " + tempDir + ": " + e.getMessage());
				logger.warn(e);
				if (ex == null) {
					ex = e;
				}
			}
		}
		pTempDirs.clear();
		if (ex != null) {
			throw new UncheckedIOException(ex);
		}
	}
	
	protected Path createTempFile(List<Path> pTempDirs, String pUrl, String pFileName) {
		URL url;
		if (pUrl.startsWith("resource:")) {
			final String uri = pUrl.substring("resource:".length());
			url = classLoader.getResource(uri);
			if (url == null) {
				final String msg = "Unable to locate resource: " + uri;
				logger.error("createTempFile: " + msg);
				throw new IllegalStateException(msg);
			}
		} else {
			try {
				url = new URL(pUrl);
			} catch (MalformedURLException e) {
				final String msg = "Invalid URL for temporary File: " + pUrl;
				logger.error("createTempFile: " + msg);
				throw new IllegalStateException(msg);
			}
		}
		final Path path;
		final String tmpDirStr = propertyProvider.getProperty("tmp.dir");
		if (tmpDirStr != null) {
			Path tmpDir = Paths.get(tmpDirStr);
			try {
				Files.createDirectories(tmpDir);
			} catch (IOException e) {
				throw new UncheckedIOException("Unable to create temporary directory: " + tmpDir.toAbsolutePath(), e);
			}
			if (pFileName == null) {
				try {
					path = Files.createTempFile(tmpDir, "icm-tempfile", ".bin");
				} catch (IOException e) {
					throw new UncheckedIOException("Unable to create temporary file in directory: " + tmpDir.toAbsolutePath(), e);
				}
			} else {
				try {
					final Path tempDir = Files.createTempDirectory(tmpDir, "icm-tempdir");
					pTempDirs.add(tempDir);
					path = tempDir.resolve(pFileName);
				} catch (IOException e) {
					throw new UncheckedIOException("Unable to create temporary file in directory: " + tmpDir.toAbsolutePath(), e);
				}
			}
		} else {
			if (pFileName == null) {
				try {
					path = Files.createTempFile("icm-tempfile", ".bin");
				} catch (IOException e) {
					throw new UncheckedIOException("Unable to create temporary file: " + e.getMessage(), e);
				}
			} else {
				try {
					final Path tempDir = Files.createTempDirectory("icm-tempdir");
					pTempDirs.add(tempDir);
					path = tempDir.resolve(pFileName);
				} catch (IOException e) {
					throw new UncheckedIOException("Unable to create temporary file: " + e.getMessage(), e);
				}
			}
		}
		try (InputStream in = url.openStream();
			 OutputStream out = Files.newOutputStream(path)) {
			Streams.copy(in, out);
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to copy create temporary file: " + e.getMessage(), e);
		}
		return path;
	}
}
