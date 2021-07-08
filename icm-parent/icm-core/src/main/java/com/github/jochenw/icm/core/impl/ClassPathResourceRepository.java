package com.github.jochenw.icm.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import com.github.jochenw.afw.core.io.AbstractFileVisitor;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Predicates;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger.Level;
import com.github.jochenw.icm.core.util.Exceptions;

public class ClassPathResourceRepository<V> implements IcmChangeRepository {
	public static class FileResource {
		private final Path path;
		private final String uri;
		public FileResource(Path path, String uri) {
			super();
			this.path = path;
			this.uri = uri;
		}
	}
	@InjectLogger private IcmLogger logger;
	private @Inject ClassLoader classLoader;
	private final Predicate<String> filter;
	private final Charset charset;

	public ClassPathResourceRepository(Predicate<String> pFilter, Charset pCharset) {
		filter = Objects.notNull(pFilter, Predicates.alwaysTrue());
		charset = Objects.notNull(pCharset, StandardCharsets.UTF_8);
	}

	@Override
	public void list(Consumer<IcmChangeResource> pConsumer) {
		list(pConsumer, "META-INF/MANIFEST.MF");
	}

	public void list(Consumer<IcmChangeResource> pConsumer, String pPathComponentUri) {
		final Consumer<IcmChangeResource> consumer = newConsumer(pConsumer);
		final Enumeration<URL> urls;
		try {
			urls = classLoader.getResources(pPathComponentUri);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		} 
		while (urls.hasMoreElements()) {
			final URL u = urls.nextElement();
			logger.trace("list: Detected URL: " + u);
			if ("file".equals(u.getProtocol())) {
				findFilesInDirectory(pPathComponentUri, consumer, u);
			} else if ("zip".equals(u.getProtocol())  ||  "jar".equals(u.getProtocol())) {
				findFilesInZipFile(pPathComponentUri, consumer, u);
			}
		}
	}

	protected void findFilesInDirectory(String pPathComponentUri,
			                            Consumer<IcmChangeResource> pConsumer,
			                            URL pUrl) {
		Path dir = new File(pUrl.getFile()).toPath();
		// The path dir is now referring to the file META-INF/MANIFEST.MF in the
		// actual directory. So, let's find the actual directory.
		String uri = pPathComponentUri;
		while (uri.length() > 0) {
			if (uri.startsWith("/")) {
				uri = uri.substring(1);
			} else {
				int offset = uri.indexOf("/");
				if (offset == -1) {
					uri = "";
				} else {
					uri = uri.substring(offset+1);
				}
				dir = dir.getParent();
			}
		}
		// Now the directory is, as it should be.
		logger.info("Looking for classpath entries in directory " + dir);
		final List<FileResource> readables = findFilesInDirectory(dir);
		filter(readables, pConsumer, "directory " + dir,
			   (fr) -> fr.uri + ", " + fr.path,
			   (fr) -> new FileChangeResource(fr, charset),
			   (fr) -> filter.test(fr.uri));
	}

	protected void findFilesInZipFile(String pPathComponentUri,
			                          Consumer<IcmChangeResource> pConsumer,
			                          URL pUrl) {
		final URLConnection uc;
		try {
			uc = pUrl.openConnection();
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
		if (uc instanceof JarURLConnection) {
			final JarURLConnection juc = (JarURLConnection) uc;
			final String jarFilePathStr;
			try (final JarFile jarFile = juc.getJarFile()) {
				jarFilePathStr = jarFile.getName();
			} catch (IOException e) {
				throw Exceptions.show(e);
			}
			final Path zipFilePath = Paths.get(jarFilePathStr);
			if (!Files.isRegularFile(zipFilePath)) {
				throw new IllegalStateException("Jar file not found: " + jarFilePathStr);
			}
			findFilesInZipFile(zipFilePath, pConsumer, pUrl);
		} else {
			throw new IllegalStateException("Invalid type of URLConnection for URL: " + pUrl);
		}
	}

	public static class ZipFileResource {
		private final Path zipFilePath;
		private final String entry;

		public ZipFileResource(Path zipFilePath, String entry) {
			super();
			this.zipFilePath = zipFilePath;
			this.entry = entry;
		}
	}

	protected void findFilesInZipFile(Path pZipFile,
									  Consumer<IcmChangeResource> pConsumer,
									  URL pUrl) {
		// Now the directory is, as it should be.
		logger.info("Looking for classpath entries in zip file " + pZipFile);
		final List<ZipFileResource> readables = new ArrayList<>();
		try (ZipFile zf = new ZipFile(pZipFile.toFile())) {
			for (Enumeration<?> en = zf.entries();  en.hasMoreElements();  ) {
				final ZipEntry ze = (ZipEntry) en.nextElement();
				if (!ze.isDirectory()) {
					final ZipFileResource zfr = new ZipFileResource(pZipFile, ze.getName());
					readables.add(zfr);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		filter(readables, pConsumer, "zip file " + pZipFile,
			   (zfr) -> zfr.entry, (zfr) -> new ZipFileChangeResource(zfr, charset),
			   (zfr) -> filter.test(zfr.entry));
	}

	protected <O> void filter(List<O> pList, Consumer<IcmChangeResource> pConsumer,
			                  String pWhat, Function<O,String> pStringifier,
			                  Function<O,IcmChangeResource> pResourceCreator,
			                  Predicate<O> pFilter) {
		logger.debug("Found " + pList.size() + " files in " + pWhat
				+ ", filtering...");
		if (logger.isEnabledFor(Level.TRACE)) {
			for (O o : pList) {
				logger.trace("File: " + pStringifier.apply(o));
			}
		}
		final long numberOfResources = pList.stream().filter(pFilter).count();
		logger.debug("Found " + numberOfResources + " files in " + pWhat
				+ ", after filtering");
		for (O o : pList) {
			if (pFilter.test(o)) {
				if (logger.isEnabledFor(Level.TRACE)) {
					logger.trace("File: " + pStringifier.apply(o));
				}
				final IcmChangeResource icr = pResourceCreator.apply(o);
				pConsumer.accept(icr);
			} else {
				logger.trace("File filtered out: " + pStringifier.apply(o));
			}
		}	
	}

	protected List<FileResource> findFilesInDirectory(Path pDirectory) {
		final List<FileResource> list = new ArrayList<>();
		final FileVisitor<Path> fv = new AbstractFileVisitor(false) {
			@Override
			public void visitFile(String pPath, Path pFile, BasicFileAttributes pAttrs) throws IOException {
				list.add(new FileResource(pFile, pPath));
			}
		};
		try {
			Files.walkFileTree(pDirectory, fv);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return list;
	}

	public static class ZipFileChangeResource implements IcmChangeResource {
		private final ZipFileResource zfr;
		private final Charset charset;

		public ZipFileChangeResource(ZipFileResource zfr, Charset charset) {
			super();
			this.zfr = zfr;
			this.charset = charset;
		}

		@Override
		public String getName() {
			final String entryName = zfr.entry;
			int offset1 = entryName.lastIndexOf('/');
			int offset2 = entryName.lastIndexOf('\\');
			final int offset = Math.max(offset1, offset2);
			if (offset == -1) {
				return entryName;
			} else {
				return entryName.substring(offset+1);
			}
		}

		@Override
		public String getUri() {
			return zfr.entry;
		}

		@Override
		public Charset getCharset() {
			return charset;
		}
	}

	public static class FileChangeResource implements IcmChangeResource {
		private final FileResource fr;
		private final Charset charset;

		public FileChangeResource(FileResource pFileResource, Charset pCharset) {
			fr = pFileResource;
			charset = pCharset;
		}

		@Override
		public String getUri() {
			return fr.uri;
		}
		
		@Override
		public String getName() {
			return fr.path.getFileName().toString();
		}
		
		@Override
		public Charset getCharset() {
			return charset;
		}
	}

	protected Consumer<IcmChangeResource> newConsumer(Consumer<IcmChangeResource> pConsumer) {
		return (res) -> {
			boolean accept = filter.test(res.getUri());
			if (accept) {
				pConsumer.accept(res);
			}
		};
	}

	@Override
	public InputStream open(IcmChangeResource pResource) throws IOException {
		if (pResource instanceof FileChangeResource) {
			final FileChangeResource fcr = (FileChangeResource) pResource;
			return Files.newInputStream(fcr.fr.path);
		} else {
			throw new IllegalStateException("Invalid resource type: "
						+ pResource.getClass().getName());
		}
	}

}
