package com.github.jochenw.icm.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;

public class DirectoryRepository implements IcmChangeRepository {
	public class FileResource implements IcmChangeResource {
		private final File file;
		private final String uri;

		public FileResource(File pFile, String pUri) {
			file = pFile;
			uri = pUri;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public String getUri() {
			return uri;
		}

		@Override
		public Charset getCharset() {
			return charset;
		}

		public File getFile() {
			return file;
		}
	}

	@InjectLogger IcmLogger logger;
	private final File directory;
	private final Charset charset;

	public DirectoryRepository(File pDirectory, Charset pCharset) {
		directory = pDirectory;
		charset = pCharset == null ? StandardCharsets.UTF_8 : pCharset;
	}

	@Override
	public void list(Consumer<IcmChangeResource> pConsumer) {
		logger.trace("list: -> " + directory.getAbsolutePath() + ", " + pConsumer);
		final StringBuilder sb = new StringBuilder();
		findFiles(directory, sb, pConsumer);
		logger.trace("list: <-");
	}

	protected void findFiles(File pDir, StringBuilder pPrefix, Consumer<IcmChangeResource> pConsumer) {
		final int len = pPrefix.length();
		for (File f : pDir.listFiles()) {
			if (len > 0) {
				pPrefix.append('/');
			}
			pPrefix.append(f.getName());
			if (f.isFile()) {
				logger.trace("findFiles: " + f.getPath());
				final FileResource fr = new FileResource(f, pPrefix.toString());
				pConsumer.accept(fr);
			} else if (f.isDirectory()) {
				logger.trace("findFiles: Subdirectory" + f.getPath());
				findFiles(f, pPrefix, pConsumer);
			}
			pPrefix.setLength(len);
		}
	}
	
	@Override
	public InputStream open(IcmChangeResource pResource) throws IOException {
		return open((FileResource) pResource);
	}

	public static InputStream open(FileResource pResource) throws IOException {
		return new FileInputStream(pResource.getFile());
	}
}
