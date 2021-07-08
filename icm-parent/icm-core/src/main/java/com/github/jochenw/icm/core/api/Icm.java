package com.github.jochenw.icm.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Binder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.api.IcmPluginContext.Committable;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.api.plugins.LifeCycleAware;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.Objects;


public class Icm<V extends Object> implements Runnable {
	public static class DetectedResource {
		private final IcmChangeResource resource;
		private IcmChangeInfo<?> info;
		private IcmChangeInstaller installer;
		private final IcmChangeRepository repository;

		public DetectedResource(IcmChangeResource pResource, IcmChangeRepository pRepository) {
			resource = pResource;
			repository = pRepository;
		}

		public IcmChangeResource getResource() { return resource; }
		public <T> IcmChangeInfo<T> getInfo() {
			@SuppressWarnings("unchecked")
			final IcmChangeInfo<T> res = (IcmChangeInfo<T>) info;
			return res;
		}
		public IcmChangeRepository getRepository() { return repository; }
		public IcmChangeInstaller getInstaller() { return installer; }
		public void setInfo(IcmChangeInfo<?> pInfo) {
			info = pInfo;
		}
		public void setInstaller(IcmChangeInstaller pInstaller) {
			installer = pInstaller;
		}
	}

	@Inject private IComponentFactory componentFactory;
	@Inject private IcmLoggerFactory loggerFactory;
	@Inject private List<IcmChangeRepository> repositories;
	@Inject private List<IcmChangeInfoProvider> infoProviders;
	@Inject private List<IcmContextProvider> contextProviders;
	@SuppressWarnings("rawtypes")
	@Inject private IcmInstallationTarget installationTarget;
	@Inject private List<IcmChangeInstaller> deployers;
	@InjectLogger private IcmLogger logger; 
	@SuppressWarnings("rawtypes")
	@Inject private IcmChangeNumberHandler versionProvider;

	@Override
	public void run() {
		logger.debug("run: ->");
		final ContextImpl context = new ContextImpl();
		final List<DetectedResource> resources = findResources(context);
		final List<DetectedResource> resourcesWithInfo = sort(findResourceInfo(context, resources));
		install(context, resourcesWithInfo);
		logger.debug("run: <-");
	}

	private static class ResourceKey {
		private final String name;
		private final String type;
		private final String version;

		ResourceKey(String pName, String pType, String pVersion) {
			Objects.requireNonNull(pName, "Name");
			Objects.requireNonNull(pType, "Type");
			Objects.requireNonNull(pVersion, "Version");
			name = pName;
			type = pType;
			version = pVersion;
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(name, type, version);
		}

		@Override
		public boolean equals(Object pOther) {
			if (pOther == null) {
				return false;
			}
			if (pOther == this) {
				return true;
			}
			if (pOther.getClass() != getClass()) {
				return false;
			}
			final ResourceKey other = (ResourceKey) pOther;
			return name.equals(other.name)  &&  type.equals(other.type)  && version.equals(other.version);
		}

		@Override
		public String toString() {
			return "name=" + name + ", type=" + type + ", version=" + version;
		}
	}
	
	protected List<DetectedResource> findResources(ContextImpl pContext) {
		final List<DetectedResource> list = new ArrayList<>();
		for (IcmChangeRepository repository : repositories) {
			final Consumer<IcmChangeResource> consumer = (r) -> list.add(new DetectedResource(r, repository));
			repository.list(consumer);
		}
		return list;
	}

	protected List<DetectedResource> findResourceInfo(ContextImpl pContext, List<DetectedResource> pResources) {
		final Set<ResourceKey> keys = new HashSet<ResourceKey>();
		for (ListIterator<DetectedResource> it = pResources.listIterator();  it.hasNext(); ) {
			final DetectedResource dr = it.next();
			IcmChangeInfo<V> info = null;
			if (dr.getResource() instanceof IcmChangeInfo) {
				@SuppressWarnings("unchecked")
				final IcmChangeInfo<V> inf = (IcmChangeInfo<V>) dr.getResource();
				info = inf;
			} else {
				for (IcmChangeInfoProvider rrip : infoProviders) {
					info = rrip.getInfo(dr.getResource(), dr.getRepository());
					if (info != null) {
						break;
					}
				}
			}
			boolean remove = true;
			if (info != null) {
				@SuppressWarnings("unchecked")
				final IcmChangeNumberHandler<V> versionProv = (IcmChangeNumberHandler<V>) versionProvider;
				final ResourceKey key = new ResourceKey(info.getTitle(), info.getType(),
						versionProv.asString(info.getVersion()));
				if (keys.contains(key)) {
					logger.warn("Ignoring duplicate resource key: name=" + info.getTitle()
					            + ", type=" + info.getType() + ", version="
					            + key.version);
				} else {
					IcmChangeInstaller installer = null;
					for (IcmChangeInstaller inst : deployers) {
						if (inst.isInstallable(info)) {
							installer = inst;
							break;
						}
					}
					if (installer == null) {
						throw new IllegalStateException("No installer available for resource: name=" + info.getTitle()
						+ ", version=" + info.getVersion() + ", type=" + info.getType()
						+ ", uri=" + dr.getResource().getUri());
					}
					if (installer != null) {
						if (getInstallationTarget().isInstalled(pContext, info)) {
							logger.debug("Ignoring already installed resource: " + key);
						} else {
							logger.debug("Using installer " + installer.getClass().getName() + " for resource: " + key);
						}
						dr.setInfo(info);
						dr.setInstaller(installer);
						remove = false;
					}
				}
			}
			if (remove) {
				it.remove();
			}
		}
		return pResources;
	}

	protected List<DetectedResource> sort(List<DetectedResource> pResources) {
		@SuppressWarnings("unchecked")
		final Comparator<V> versionComparator = componentFactory.requireInstance(IcmChangeNumberHandler.class).getComparator();
		final Comparator<DetectedResource> comparator = (dr1, dr2) -> {
			@SuppressWarnings("unchecked")
			final V v1 = (V) dr1.getInfo().getVersion();
			@SuppressWarnings("unchecked")
			final V v2 = (V) dr2.getInfo().getVersion();
			return versionComparator.compare(v1, v2);
		};
		Collections.sort(pResources, comparator);
		return pResources;
	}

	protected class ContextImpl implements Context {
		private List<Committable> transactions = new ArrayList<>();
		private List<LifeCycleAware> lifeCycleParticipants = new ArrayList<LifeCycleAware>();
		private IcmChangeResource resource;
		private IcmChangeRepository repository;
		private IcmChangeInfo<?> info;

		@Override
		public IcmChangeResource getResource() {
			return resource;
		}

		@Override
		public IcmChangeRepository getRepository() {
			return repository;
		}

		@Override
		public IcmChangeInfo<?> getInfo() {
			return info;
		}

		public void setResource(IcmChangeResource pResource) {
			resource = pResource;
		}
		public void setRepository(IcmChangeRepository pRepository) {
			repository = pRepository;
		}
		public void setInfo(IcmChangeInfo<?> pInfo) {
			info = pInfo;
		}

		@Override
		public InputStream open() throws IOException {
			return getRepository().open(getResource());
		}

		@Override
		public Reader openText() throws IOException {
			return new InputStreamReader(open(), getResource().getCharset());
		}

		@Override
		public void add(Committable pCommittable) {
			transactions.add(pCommittable);
		}

		@Override
		public List<Committable> getTransactions() {
			return transactions;
		}

		@Override
		public <C extends LifeCycleAware> C getContextFor(String pId) {
			for (IcmContextProvider rcp : contextProviders) {
				if (rcp.isContextProviderFor(pId)) {
					final C c = rcp.getContextFor(this, pId);
					lifeCycleParticipants.add(c);
					return c;
				}
			}
			throw new IllegalStateException("No context provider available for id: " + pId);
		}
	
		public void shutdown() {
			Throwable th = null;
			for (LifeCycleAware lca : lifeCycleParticipants) {
				try {
					lca.shutdown();
				} catch (Throwable t) {
					if (th == null) {
						th = t;
					}
				}
			}
			if (th != null) {
				throw Exceptions.show(th);
			}
		}
		
	}
	
	protected void install (ContextImpl context, List<DetectedResource> pResources) {
		logger.debug("install: -> " + pResources.size());
		Throwable th = null;
		for (DetectedResource resource : pResources) {
			logger.debug("install: Installing resource, name=" + resource.getInfo().getTitle() + ", type=" + resource.getInfo().getType()
					     + ", version=" + resource.getInfo().getVersion() + ", uri=" + resource.getResource().getUri());
			try {
				final IcmChangeInfo<V> info = resource.getInfo();
				context.setInfo(info);
				context.setResource(resource.getResource());
				context.setRepository(resource.getRepository());
				final IcmChangeInstaller installer = resource.getInstaller();
				installer.install(context);
				getInstallationTarget().add(context, info);
			} catch (Throwable t) {
				th = t;
				break;
			}
		}
		final List<Committable> transactions = context.getTransactions();
		if (th == null) {
			for (Committable committable : transactions) {
				try {
					committable.commit();
				} catch (Throwable t) {
					th = t;
					break;
				}
			}
		}
		if (th != null) {
			for (int i = transactions.size()-1;  i >= 0;  i--) {
				try {
					final Committable committable = transactions.get(i);
					committable.rollback();
				} catch (Throwable t) {
					// Ignore this, we'll be throwing th
				}
			}
			context.shutdown();
			throw Exceptions.show(th);
		}
		context.shutdown();
		logger.debug("install: <-");
	}

	public static <V> IcmBuilder<IcmChangeNumber> builder() {
		return builder(IcmChangeNumber.class, new DefaultIcmChangeNumberHandler());
	}

	public static <V> IcmBuilder<V> builder(Class<V> pVersionClass, IcmChangeNumberHandler<V> pVersionProvider) {
		final IcmBuilder<V> builder = new IcmBuilder<V>(pVersionClass, pVersionProvider);
		final Module module = new Module() {
			@Override
			public void configure(Binder pBinder) {
				pBinder.bind(IcmChangeNumberHandler.class).toInstance(pVersionProvider);
			}
		};
		builder.getComponentFactoryBuilder().module(module);
		return builder;

	}

	void setResourceInfoProviders(List<IcmChangeInfoProvider> pResourceInfoProviders) {
		infoProviders = pResourceInfoProviders;
	}
	public List<IcmChangeInfoProvider> getResourceInfoProviders() {
		return infoProviders;
	}
	void setInstallationTarget(IcmInstallationTarget<V> pTarget) {
		installationTarget = pTarget;
	}
	public IcmInstallationTarget<V> getInstallationTarget() {
		@SuppressWarnings("unchecked")
		final IcmInstallationTarget<V> iit = (IcmInstallationTarget<V>) installationTarget;
		return iit;
	}
	void setResourceRepositories(List<IcmChangeRepository> pRepositories) {
		repositories = pRepositories;
	}
	public List<IcmChangeRepository> getResourceRepositories() {
		return repositories;
	}
	void setComponentFactory(IComponentFactory pFactory) {
		componentFactory = pFactory;
	}
	public IComponentFactory getComponentFactory() {
		return componentFactory;
	}
	void setLoggerFactory(IcmLoggerFactory pLoggerFactory) {
		loggerFactory = pLoggerFactory;
	}
	public IcmLoggerFactory getLoggerFactory() {
		return loggerFactory;
	}
}
