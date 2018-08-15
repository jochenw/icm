package com.github.jochenw.icm.core.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;

import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder.Binder;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder.Module;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.api.log.IcmLogger.Level;
import com.github.jochenw.icm.core.api.plugins.CommandExecutor;
import com.github.jochenw.icm.core.api.plugins.ExpressionEvaluator;
import com.github.jochenw.icm.core.api.plugins.JdbcConnectionProvider;
import com.github.jochenw.icm.core.api.plugins.PluginRepository;
import com.github.jochenw.icm.core.api.plugins.SqlAdapter;
import com.github.jochenw.icm.core.api.plugins.SqlScriptReader;
import com.github.jochenw.icm.core.api.plugins.SqlStatementExecutor;
import com.github.jochenw.icm.core.api.plugins.TextFileEditor;
import com.github.jochenw.icm.core.api.plugins.XmlPluginRepository;
import com.github.jochenw.icm.core.api.prop.Interpolator;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.impl.ClassPathResourceRepository;
import com.github.jochenw.icm.core.impl.DirectoryRepository;
import com.github.jochenw.icm.core.impl.log.SimpleLoggerFactory;
import com.github.jochenw.icm.core.impl.plugins.DefaultCommandExecutor;
import com.github.jochenw.icm.core.impl.plugins.DefaultExpressionEvaluator;
import com.github.jochenw.icm.core.impl.plugins.DefaultJdbcConnectionProvider;
import com.github.jochenw.icm.core.impl.plugins.DefaultSqlAdapter;
import com.github.jochenw.icm.core.impl.plugins.DefaultSqlScriptReader;
import com.github.jochenw.icm.core.impl.plugins.DefaultSqlStatementExecutor;
import com.github.jochenw.icm.core.impl.prop.DefaultInterpolator;
import com.github.jochenw.icm.core.impl.prop.DefaultIcmPropertyProvider;
import com.github.jochenw.icm.core.util.AbstractBuilder;
import com.github.jochenw.icm.core.util.Exceptions;


public class IcmBuilder<V extends Object> extends AbstractBuilder {
	private final Class<V> versionClass;
	private final IcmChangeNumberHandler<V> versionProvider;
	private final ComponentFactoryBuilder componentFactoryBuilder = new ComponentFactoryBuilder();

	private ClassLoader classLoader;
	private PluginRepository pluginRepository;
	private List<IcmChangeRepository> repositories = new ArrayList<>();
	private List<IcmChangeInfoProvider> resourceInfoProviders = new ArrayList<>();
	private List<IcmContextProvider> contextProviders = new ArrayList<>();
	private List<Properties> properties = new ArrayList<>();
	private List<Module> modules = new ArrayList<>();
	private IcmPropertyProvider propertyProvider;
	private IcmInstallationTarget<V> target;
	private List<IcmChangeInstaller> resourceInstallers = new ArrayList<>();
	private IcmLoggerFactory loggerFactory;
	private IcmLogger logger;
	
	IcmBuilder(Class<V> pVersionClass, IcmChangeNumberHandler<V> pVersionProvider) {
		versionClass = pVersionClass;
		versionProvider = pVersionProvider;
	}
	public Icm<V> build() {
		@SuppressWarnings("unchecked")
		final Icm<V> icm = (Icm<V>) super.build();
		return icm;
	}

	protected Icm<V> newInstance() {
		if (classLoader == null) {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		if (target == null) {
			throw new IllegalStateException("No IcmInstallationTarget has been configured.");
		}
		if (loggerFactory == null) {
			loggerFactory = newLoggerFactory();
			logger = loggerFactory.getLogger(IcmBuilder.class);
			logger.info("Logging initialized");
		}
		if (propertyProvider == null) {
			propertyProvider = newPropertyProvider();
		}
		if (repositories == null  ||  repositories.isEmpty()) {
			throw new IllegalStateException("No IcmChangeRepository has been configured.");
		}
		if (resourceInfoProviders.isEmpty()) {
			defaultResourceInfoProviders();
		}
		if (resourceInstallers.isEmpty()) {
			defaultResourceInstallers();
		}
		if (contextProviders.isEmpty()) {
			defaultContextProviders();
		}
		final Module module = new Module() {
			@Override
			public void bind(Binder pBinder) {
				pBinder.bindClass(Icm.class, Icm.class);
				pBinder.bindInstance(IcmLoggerFactory.class, loggerFactory);
				pBinder.bindInstance(IcmPropertyProvider.class, propertyProvider);
				pBinder.bindInstance(IcmInstallationTarget.class, target);
				pBinder.bindInstance(ClassLoader.class, classLoader);
				pBinder.bindClass(TextFileEditor.class, TextFileEditor.class);
				pBinder.bindList(repositories, IcmChangeRepository.class);
				pBinder.bindList(resourceInfoProviders, IcmChangeInfoProvider.class);
				pBinder.bindList(resourceInstallers, IcmChangeInstaller.class);
				pBinder.bindList(contextProviders, IcmContextProvider.class);
				pBinder.bindClass(SqlScriptReader.class, DefaultSqlScriptReader.class);
				pBinder.bindClass(SqlStatementExecutor.class, DefaultSqlStatementExecutor.class);
				pBinder.bindClass(JdbcConnectionProvider.class, DefaultJdbcConnectionProvider.class);
				pBinder.bindClass(CommandExecutor.class, DefaultCommandExecutor.class);
				pBinder.bindClass(ExpressionEvaluator.class, DefaultExpressionEvaluator.class);
				pBinder.bindClass(Interpolator.class, DefaultInterpolator.class);
				pBinder.bindInstance(Properties.class, getProperties());
				pBinder.bindInstance(IcmLoggerFactory.class, getLoggerFactory());
				pBinder.bindInstance(SqlAdapter.class, DefaultSqlAdapter.class);
			}
		};
		final ComponentFactory componentFactory = newComponentFactory(module);
		@SuppressWarnings("unchecked")
		final Icm<V> icm = (Icm<V>) componentFactory.requireInstance(Icm.class);
		return icm;
	}

	public IcmBuilder<V> classLoader(ClassLoader pClassLoader) {
		assertMutable();
		classLoader = pClassLoader;
		return this;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
	protected IcmLoggerFactory newLoggerFactory() {
		return new SimpleLoggerFactory(System.out, Level.DEBUG);
	}
	
	protected ComponentFactory newComponentFactory(Module pModule) {
		return getComponentFactoryBuilder().module(pModule).modules(modules).build();
	}
	
	public Class<V> getVersionClass() { return versionClass; }
	public IcmChangeNumberHandler<V> getVersionProvider() { return versionProvider; }
	public ComponentFactoryBuilder getComponentFactoryBuilder() { return componentFactoryBuilder; }

	public IcmBuilder<V> module(Module pModule) {
		Objects.requireNonNull(pModule, "Module");
		assertMutable();
		modules.add(pModule);
		return this;
	}
	
	public IcmBuilder<V> propertyProvider(IcmPropertyProvider pProvider) {
		assertMutable();
		propertyProvider = pProvider;
		return this;
	}
	public List<IcmChangeRepository> getRepositories() { return repositories; }
	public IcmBuilder<V> resourceRepository(IcmChangeRepository pRepository) {
		assertMutable();
		repositories.add(pRepository);
		return this;
	}
	public IcmBuilder<V> resourceDirectory(File pDirectory, Charset pCharset) {
		if (!pDirectory.isDirectory()) {
			throw new IllegalArgumentException("Directory does not exist, or is not a directory: " + pDirectory.getAbsolutePath());
		}
		return resourceRepository(new DirectoryRepository(pDirectory, pCharset));
	}
	public IcmBuilder<V> resourceDirectory(String pDirectory) {
		return resourceDirectory(new File(pDirectory), StandardCharsets.UTF_8);
	}
	public IcmBuilder<V> resourceDirectory(File pDirectory) {
		return resourceDirectory(pDirectory, StandardCharsets.UTF_8);
	}
	public IcmBuilder<V> classPathResourceRepository(String pPrefix, Charset pCharset) {
		final String prefix = pPrefix.replace('.', '/');
		final Predicate<String> filter = new Predicate<String>() {
			@Override
			public boolean test(String t) {
				if (!t.startsWith(prefix)) {
					return false;
				}
				return true;
			}
		};
		final ClassPathResourceRepository<V> repository = new ClassPathResourceRepository<V>(filter, pCharset);
		return resourceRepository(repository);
	}
	public IcmBuilder<V> classPathResourceRepository(String pPrefix) {
		return classPathResourceRepository(pPrefix, StandardCharsets.UTF_8);
	}
	public IcmBuilder<V> pluginRepository(PluginRepository pRepository) {
		assertMutable();
		pluginRepository = pRepository;
		return this;
	}
	public PluginRepository getPluginRepository() {
		if (pluginRepository == null) {
			pluginRepository = newPluginRepository();
		}
		return pluginRepository;
	}
	public IcmBuilder<V> contextProvider(IcmContextProvider pProvider) {
		Objects.requireNonNull(pProvider, "ContextProvider");
		assertMutable();
		contextProviders.add(pProvider);
		return this;
	}
	public IcmBuilder<V> defaultContextProviders() {
		assertMutable();
		final List<IcmContextProvider> ctxProviders = getPluginRepository().getContextProviders();
		if (ctxProviders == null  ||  ctxProviders.isEmpty()) {
			throw new IllegalStateException("No context providers available");
		}
		contextProviders.addAll(ctxProviders);
		return this;
	}
	public List<IcmContextProvider> getContextProviders() {
		return contextProviders;
	}
	protected PluginRepository newPluginRepository() {
		final XmlPluginRepository repo = new XmlPluginRepository();
		repo.setLoggerFactory(getLoggerFactory());
		return repo;
	}
	public IcmBuilder<V> properties(Properties pProperties) {
		assertMutable();
		properties.add(pProperties);
		return this;
	}
	public IcmBuilder<V> properties(String pPropertyFile) {
		return properties(new File(pPropertyFile));
	}
	public IcmBuilder<V> properties(File pPropertyFile) {
		final Properties props = new Properties();
		try (InputStream istream = new FileInputStream(pPropertyFile)) {
			props.load(istream);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		return properties(props);
	}
	public IcmBuilder<V> properties(URL pPropertyUrl) {
		final Properties props = new Properties();
		try (InputStream istream = pPropertyUrl.openStream()) {
			props.load(istream);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		return properties(props);
	}
	public IcmBuilder<V> resourceInfoProvider(IcmChangeInfoProvider pInfoProvider) {
		assertMutable();
		resourceInfoProviders.add(pInfoProvider);
		return this;
	}
	public IcmBuilder<V> defaultResourceInfoProviders() {
		assertMutable();
		final List<IcmChangeInfoProvider> infoProviders = getPluginRepository().getResourceInfoProviders();
		if (infoProviders == null  ||  infoProviders.isEmpty()) {
			throw new IllegalStateException("No IcmChangeInfoProviders have been found.");
		}
		resourceInfoProviders.addAll(infoProviders);
		return this;
	}
	public List<IcmChangeInfoProvider> getResourceInfoProviders() {
		return resourceInfoProviders;
	}
	public IcmBuilder<V> installationTarget(IcmInstallationTarget<V> pTarget) {
		assertMutable();
		target = pTarget;
		return this;
	}
	public IcmInstallationTarget<V> getInstallationTarget() {
		return target;
	}
	public IcmBuilder<V> resourceInstaller(IcmChangeInstaller pInstaller) {
		assertMutable();
		resourceInstallers.add(pInstaller);
		return this;
	}
	public IcmBuilder<V> defaultResourceInstallers() {
		assertMutable();
		final List<IcmChangeInstaller> installers = getPluginRepository().getResourceInstallers();
		if (installers == null  ||  installers.isEmpty()) {
			throw new IllegalStateException("No IcmChangeInstallers have been found.");
		}
		resourceInstallers.addAll(installers);
		return this;
	}
	public IcmBuilder<V> loggerFactory(IcmLoggerFactory pFactory) {
		assertMutable();
		loggerFactory = pFactory;
		return this;
	}
	public IcmLoggerFactory getLoggerFactory() {
		return loggerFactory;
	}
	public Properties getProperties() {
		final Properties props = newBuiltinProperties();
		for (Properties p : properties) {
			props.putAll(p);
		}
		final Interpolator interpolator = new DefaultInterpolator(props);
		return interpolator.filter(props);
	}
	protected IcmPropertyProvider newPropertyProvider() {
		if (properties == null  ||  properties.isEmpty()) {
			throw new IllegalStateException("No properties have been configured.");
		}
		return new DefaultIcmPropertyProvider(getProperties());
	}
	protected Properties newBuiltinProperties() {
		final Properties props = new Properties(System.getProperties());
		for (Map.Entry<String,String> en : System.getenv().entrySet()) {
			if (en.getValue() != null) {
				props.put("env." + en.getKey(), en.getValue());
			}
		}
		final String osName = props.getProperty("os.name").toLowerCase();
		if (osName.indexOf("win") >= 0) {
			props.put("os.windows", "true");
		} else if (osName.indexOf("mac") >= 0) {
			props.put("os.mac", "true");
		} else if (osName.indexOf("linux") >= 0) {
			props.put("os.linux", "true");
		} else if (osName.indexOf("nix") >= 0  ||  osName.indexOf("aix") >= 0  ||  osName.indexOf("sunos") >= 0) {
			props.put("os.unix", "true");
		} else {
			props.put("os.unknown", "true");
		}
		return props;
	}
}
