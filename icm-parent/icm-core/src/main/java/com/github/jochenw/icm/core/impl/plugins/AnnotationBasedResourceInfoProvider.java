package com.github.jochenw.icm.core.impl.plugins;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeInfoProvider;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.ResourceInfoProvider;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.FileAnnotationScanner;
import com.github.jochenw.icm.core.util.FileAnnotationScanner.Annotation;

@ResourceInfoProvider
public class AnnotationBasedResourceInfoProvider implements IcmChangeInfoProvider {
	@InjectLogger private IcmLogger logger;
	@SuppressWarnings("rawtypes")
	@Inject private IcmChangeNumberHandler versionProvider;


	@Override
	public <T> IcmChangeInfo<T> getInfo(IcmChangeResource pResource, IcmChangeRepository pRepository) {
		try (Reader reader = new InputStreamReader(pRepository.open(pResource), pResource.getCharset())) {
			final List<Annotation> annotations = FileAnnotationScanner.parse(reader);
			final Map<String,String> attributes = new HashMap<>();
			if (annotations != null) {
				for (Annotation annotation : annotations) {
					if ("Attribute".equals(annotation.getName())) {
						final String name = annotation.getAttribute("name");
						final String value = annotation.getAttribute("value");
						if (name == null  ||  value == null) {
							logger.warn(pResource.getUri() + ": An Attribute declaration must have the attributes 'name', and 'value'.");
						} else {
							attributes.put(name, value);
						}
					}
				}
				for (Annotation annotation : annotations) {
					if ("IcmChange".equals(annotation.getName())) {
						final String name = annotation.getAttribute("name");
						final String type = annotation.getAttribute("type");
						final String version = annotation.getAttribute("version");
						if (name == null  ||  name.length() == 0) {
							logger.error(pResource.getUri() + ": An IcmChange declaration must have a non-empty 'name' attribute.");
						} else if (type == null  ||  type.length() == 0) {
							logger.error(pResource.getUri() + ": An IcmChange declaration must have a non-empty 'type' attribute.");
						} else if (version == null  ||  version.length() == 0) {
							logger.error(pResource.getUri() + ": An IcmChange declaration must have a non-empty 'version' attribute.");
						} else {
							try {
								@SuppressWarnings("unchecked")
								final IcmChangeNumberHandler<T> vp = (IcmChangeNumberHandler<T>) versionProvider;
								final T v = vp.asVersion(version);
								return new IcmChangeInfo<T>() {
									@Override
									public String getTitle() {
										return name;
									}

									@Override
									public T getVersion() {
										return v;
									}

									@Override
									public String getType() {
										return type;
									}

									@Override
									public String getAttribute(String pKey) {
										return attributes.get(pKey);
									}
								};
							} catch (Throwable t) {
								t.printStackTrace();
								logger.error(pResource.getUri() + ": Invalid 'version' attribute: " + version);
							}
							
						}
					}
				}
			}
			return null;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

}
