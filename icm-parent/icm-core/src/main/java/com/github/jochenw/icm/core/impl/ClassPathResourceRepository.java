package com.github.jochenw.icm.core.impl;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.impl.AsmClassInfoProvider.ClassInfo;
import com.github.jochenw.icm.core.impl.DirectoryRepository.FileResource;
import com.github.jochenw.icm.core.util.Exceptions;

public class ClassPathResourceRepository<V> implements IcmChangeRepository {
	public class ZipFileResource implements IcmChangeResource {
		private final String zipFile;
		private final String name, entryName, resourceName;
		private final String uri;

		public ZipFileResource(String pZipFile, String pEntryName) {
			zipFile = pZipFile;
			name = getName(pEntryName);
			resourceName = null;
			entryName = pEntryName;
			uri = "zip:file:" + pZipFile + "!" + pEntryName;
		}
		public ZipFileResource(String pZipFile, String pResourceName, String pEntryName) {
			zipFile = pZipFile;
			name = getName(pEntryName);
			resourceName = pResourceName;
			entryName = pEntryName;
			uri = "zip:file:" + pZipFile + "!" + pResourceName + "!" + pEntryName;
		}

		private String getName(String pEntryName) {
			final int slashOffset = pEntryName.lastIndexOf('/');
			final int backSlashOffset = pEntryName.lastIndexOf('\\');
			if (slashOffset == -1  &&  backSlashOffset == -1) {
				return pEntryName;
			} else {
				return pEntryName.substring(Math.max(slashOffset, backSlashOffset)+1);
			}
		}
		
		public String getZipFile() {
			return zipFile;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getUri() {
			return uri;
		}

		public String getEntryName() {
			return entryName;
		}

		public String getResourceName() {
			return resourceName;
		}

		@Override
		public Charset getCharset() {
			return charset;
		}
	}
	@InjectLogger private IcmLogger logger;
	private @Inject ComponentFactory componentFactory;
	private @Inject IcmChangeNumberHandler<V> versionProvider;
	private final Predicate<String> filter;
	private final Charset charset;
	@Inject private ClassLoader classLoader;

	public ClassPathResourceRepository(Predicate<String> pFilter, Charset pCharset) {
		filter = pFilter;
		charset = pCharset;
	}

	public ClassLoader getClassLoader() {
		return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
	}
	
	@Override
	public void list(Consumer<IcmChangeResource> pConsumer) {
		list(pConsumer, "META-INF/MANIFEST.MF");
	}

	public void list(Consumer<IcmChangeResource> pConsumer, String pPathComponentUri) {
		final Consumer<IcmChangeResource> consumer = newConsumer(pConsumer);
		final Enumeration<URL> urls;
		try {
			urls = getClassLoader().getResources(pPathComponentUri);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		} 
		while (urls.hasMoreElements()) {
			final URL u = urls.nextElement();
			logger.trace("list: Detected URL: " + u);
			if ("file".equals(u.getProtocol())) {
				File dir = new File(u.getFile());
				String uri = pPathComponentUri;
				while (uri.length() > 0) {
					if (uri.startsWith("/")) {
						uri = uri.substring(1);
					} else {
						int offset = uri.indexOf('/');
						if (offset == -1) {
							uri = "";
							dir = dir.getParentFile();
						} else {
							uri = uri.substring(offset+1);
							dir = dir.getParentFile();
						}
					}
				}
				logger.debug("Looking for classpath entries in directory " + dir);
				final DirectoryRepository dirRepository = new DirectoryRepository(dir, charset);
				componentFactory.init(dirRepository);
				dirRepository.list(consumer);
			} else if ("zip".equals(u.getProtocol())  ||  "jar".equals(u.getProtocol())) {
				String zipFilePath = u.getFile();
				if (zipFilePath.startsWith("file:")) {
					zipFilePath = zipFilePath.substring("file:".length());
				}
				final int offset = zipFilePath.indexOf('!');
				if (offset == -1) {
					throw new IllegalStateException("Unable to parse zip entry reference: " + u.getFile());
				}
				final String path = zipFilePath.substring(0, offset);
				final String entry = zipFilePath.substring(offset+1);
				final int offset2 = entry.indexOf('!');
				if (offset2 >= 0) {
					String resourcePath = entry.substring(0, offset2);
					try (ZipFile zf = new ZipFile(path)) {
						logger.debug("Looking for classpath entries in zip file " + path + ", resource entry " + resourcePath);
						ZipEntry ze = zf.getEntry(resourcePath);
						if (ze == null) {
							if (resourcePath.startsWith("/")) {
								resourcePath = resourcePath.substring(1);
								ze = zf.getEntry(resourcePath);
								if (ze == null) {
									throw new IllegalStateException("Zip resource " + resourcePath + " not found in zip file: " + path);
								}
							}
						}
						try (InputStream is = zf.getInputStream(ze);
							 ZipInputStream zis = new ZipInputStream(is)) {
							for (;;) {
								final ZipEntry ze2 = zis.getNextEntry();
								if (ze2 == null) {
									break;
								}
								if (!ze2.isDirectory()) {
									final ZipFileResource zfr = new ZipFileResource(path, ze.getName(), ze2.getName());
									accept(consumer, zfr);
								}
							}
							
						}
 					} catch (IOException e) {
 						throw Exceptions.show(e);
 					}
				} else {
					try (ZipFile zf = new ZipFile(path)) {
						logger.debug("Looking for classpath entries in zip file " + path);
						final Enumeration<? extends ZipEntry> en = zf.entries();
						while (en.hasMoreElements()) {
							final ZipEntry ze = en.nextElement();
							if (!ze.isDirectory()) {
								final ZipFileResource zfr = new ZipFileResource(path, ze.getName());
								accept(consumer, zfr);
							}
						}
					} catch (Throwable t) {
						throw Exceptions.show(t);
					}
				}
			}
		}
	}

	protected Consumer<IcmChangeResource> newConsumer(Consumer<IcmChangeResource> pConsumer) {
		final Consumer<IcmChangeResource> consumer = new Consumer<IcmChangeResource>() {
			@Override
			public void accept(IcmChangeResource pResource) {
				boolean accept;
				if (pResource instanceof ClassPathResourceRepository.ZipFileResource) {
					@SuppressWarnings("rawtypes")
					final ClassPathResourceRepository.ZipFileResource zfr = (ClassPathResourceRepository.ZipFileResource) pResource;
					accept = filter.test(zfr.getEntryName());
				} else {
					accept = filter.test(pResource.getUri());
				}
				if (accept) {
					pConsumer.accept(pResource);
				}
			}
		};
		return consumer;
	}

	@Override
	public InputStream open(IcmChangeResource pResource) throws IOException {
		if (pResource instanceof ClassPathResourceRepository.ZipFileResource) {
			@SuppressWarnings("unchecked")
			final ClassPathResourceRepository<V>.ZipFileResource zfr = (ClassPathResourceRepository<V>.ZipFileResource) pResource;
			return open(zfr);
		} else if (pResource instanceof Resource) {
			@SuppressWarnings("unchecked")
			final Resource<V> resource = (Resource<V>) pResource;
			final IcmChangeResource rr = resource.getResource();
			return open(rr);
		} else {
			return DirectoryRepository.open((FileResource) pResource);
		}
	}

	protected InputStream open(ZipFileResource pResource) throws IOException {
		final ZipFile zf = new ZipFile(pResource.getZipFile());
		if (pResource.getResourceName() == null) {
			final ZipEntry ze = zf.getEntry(pResource.getEntryName());
			if (ze == null) {
				throw new IllegalStateException("Entry " + pResource.name
						+ " not found in zip file: " + pResource.zipFile);
			}
			return new FilterInputStream(zf.getInputStream(ze)) {
				@Override
				public void close() throws IOException {
					super.close();
					zf.close();
				}
			};
		} else {
			final ZipEntry ze = zf.getEntry(pResource.getResourceName());
			if (ze == null) {
				throw new IllegalStateException("Entry " + pResource.getResourceName()
				        + " not found in zip file: " + pResource.getZipFile());
			}
			Throwable th = null;
			InputStream is = null;
			ZipInputStream zis = null;
			InputStream result = null;
			try {
				is = zf.getInputStream(ze);
			    zis = new ZipInputStream(is);
				for (;;) {
					final ZipEntry ze2 = zis.getNextEntry();
					if (ze2 == null) {
						break;
					}
					if (!ze2.isDirectory()  &&  ze2.getName().equals(pResource.getEntryName())) {
						result = new FilterInputStream(zis) {
							@Override
							public void close() throws IOException {
								super.close();
								zf.close();
							}
						};
						zis = null;
						is = null;
						break;
					}
				}
			} catch (Throwable t) {
				th = t;
			} finally {
				if (zis != null) { try { zis.close(); } catch (Throwable t) { if (th == null) { th = t; } } }
				if (is != null) { try { is.close(); } catch (Throwable t) { if (th == null) { th = t; } } }
			}
			if (th != null) {
				throw Exceptions.show(th);
			}
			if (result == null) {
				throw new IllegalStateException("Entry " + pResource.getResourceName() + "!" + pResource.getEntryName()
				+ " not found in zip file: " + pResource.getZipFile());
			} else {
				return result;
			}
		}
	}
	
	protected void accept(Consumer<IcmChangeResource> pConsumer, final IcmChangeResource pResource) {
		if (pResource.getName().endsWith(".class")) {
			final AsmClassInfoProvider acip = new AsmClassInfoProvider();
			try (InputStream in = open(pResource)) {
				final ClassInfo ci = acip.getClassInfo(open(pResource));
				if (ci != null  &&
					(ci.getResourceName() != null 
					  ||  ci.getResourceVersion() != null)) {
					if (ci.getResourceName() == null) {
						logger.error("Resource name missing: " + pResource.getUri());
					} else if (ci.getResourceType() == null) {
						logger.error("Resource type missing: " + pResource.getUri());
					} else if (ci.getResourceVersion() == null) {
						logger.error("Resource version missing: " + pResource.getUri());
					} else {
						final V v;
						try {
							v = versionProvider.asVersion(ci.getResourceVersion());
						} catch (Throwable t) {
							logger.error("Invalid version " + ci.getResourceVersion()
										 + " for resource " + pResource.getUri());
							return;
						}
						pConsumer.accept(new Resource<V>(pResource, ci, v));
					}
				}
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		} else {
			pConsumer.accept(pResource);
		}
	}

	public class Resource<RV> implements IcmChangeResource, IcmChangeInfo<RV> {
		private final IcmChangeResource resource;
		private final ClassInfo classInfo;
		private final RV version;
		public Resource(IcmChangeResource pResource, ClassInfo pClassInfo, RV pVersion) {
			resource = pResource;
			classInfo = pClassInfo;
			version = pVersion;
		}
		@Override
		public RV getVersion() {
			return version;
		}
		@Override
		public String getType() {
			return classInfo.getResourceType();
		}
		@Override
		public String getAttribute(String pKey) {
			return classInfo.getAttributes().get(pKey);
		}
		@Override
		public String getName() {
			return resource.getName();
		}
		@Override
		public String getTitle() {
			return classInfo.getResourceName();
		}
		@Override
		public String getUri() {
			return resource.getUri();
		}
		@Override
		public Charset getCharset() {
			return resource.getCharset();
		}
		public IcmChangeResource getResource() {
			return resource;
		}
	}
}
