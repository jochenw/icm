package com.github.jochenw.icm.core.impl.plugins;

import java.io.InputStream;

import javax.inject.Inject;

import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeInfoProvider;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.ResourceInfoProvider;
import com.github.jochenw.icm.core.impl.AsmClassInfoProvider;
import com.github.jochenw.icm.core.impl.AsmClassInfoProvider.ClassInfo;

@ResourceInfoProvider
public class AnnotatedClassResourceInfoProvider implements IcmChangeInfoProvider {
	private final AsmClassInfoProvider asmClassInfoProvider = new AsmClassInfoProvider();
	private @InjectLogger IcmLogger logger;
	@SuppressWarnings("rawtypes")
	private @Inject IcmChangeNumberHandler versionProvider;

	@Override
	public <T> IcmChangeInfo<T> getInfo(IcmChangeResource pResource, IcmChangeRepository pRepository) {
		if (pResource.getName().endsWith(".class")) {
			final ClassInfo classInfo;
			try (InputStream in = pRepository.open(pResource)) {
				classInfo = asmClassInfoProvider.getClassInfo(in);
			} catch (Throwable t) {
				logger.error("Failed to parse .class file " + pResource.getUri() + ": " + t.getMessage(), t);
				return null;
			}
			if (Strings.isEmpty(classInfo.getResourceVersion())) {
				logger.debug("No resource version found for .class file " + pResource.getUri() + ", ignoring this file.");
				return null;
			}
			@SuppressWarnings("unchecked")
			final IcmChangeNumberHandler<T> vp = (IcmChangeNumberHandler<T>) versionProvider;
			final T v;
			try {
				v = vp.asVersion(classInfo.getResourceVersion());
			} catch (IllegalArgumentException e) {
				final String msg = "Invalid resource version provided for .class file " + pResource.getUri() + ": " + classInfo.getResourceVersion();
				logger.error(msg);
				throw new IllegalArgumentException(msg, e);
			}
			if (Strings.isEmpty(classInfo.getResourceType())) {
				logger.debug("No resource type found for .class file " + pResource.getUri() + ", ignoring this file.");
				return null;
			}
			if (Strings.isEmpty(classInfo.getResourceName())) {
				logger.debug("No resource name found for .class file " + pResource.getUri() + ", ignoring this file.");
				return null;
			}
			return new IcmChangeInfo<T>() {
				@Override
				public String getTitle() {
					return classInfo.getResourceName();
				}

				@Override
				public T getVersion() {
					return v;
				}

				@Override
				public String getType() {
					return classInfo.getResourceType();
				}

				@Override
				public String getAttribute(String pKey) {
					return classInfo.getAttributes().get(pKey);
				}
			};
		} else {
			return null;
		}
	}
}
