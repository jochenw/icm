package com.github.jochenw.icm.core.impl.cf;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;

import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.cf.InjectProperty;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.util.Exceptions;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class GuiceComponentFactory extends ComponentFactory {
	public class LoggerInjectingTypeListener implements TypeListener {
		@Override
		public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
			Class<?> clazz = type.getRawType();
			while (clazz != null  &&  clazz != Object.class) {
				for (Field field : clazz.getDeclaredFields()) {
					final Callable<Object> callable;
		            if (field.getType() == IcmLogger.class) {
		            	final InjectLogger injectLogger = field.getAnnotation(InjectLogger.class);
		            	String id = injectLogger.id();
		            	if (id.length() == 0) {
		            		id = field.getDeclaringClass() + "." + field.getName();
		            	}
		            	final String loggerId = id;
		            	callable = new Callable<Object>() {
							@Override
							public Object call() throws Exception {
								final IcmLoggerFactory loggerFactory = requireInstance(IcmLoggerFactory.class);
								return loggerFactory.getLogger(loggerId);

							}
		            	};
		            } else if (field.getType() == String.class) {
		            	final InjectProperty injectProperty = field.getAnnotation(InjectProperty.class);
		            	String id = injectProperty.id();
		            	if (id.length() == 0) {
		            		id = field.getDeclaringClass().getName();
		            	}
		            	final String propertyKey = id;
		            	callable = new Callable<Object>() {
							@Override
							public Object call() throws Exception {
			            		final IcmPropertyProvider propertyProvider = requireInstance(IcmPropertyProvider.class);
			            		return propertyProvider.getProperty(propertyKey);
							}
		            	};
		            } else {
		            	callable = null;
		            }
		            if (callable != null) {
		            	encounter.register(new MembersInjector<Object>() {
							@Override
							public void injectMembers(Object pInstance) {
								if (!field.isAccessible()) {
									field.setAccessible(true);
								}
								try {
									field.set(pInstance, callable.call());
								} catch (Throwable t) {
									throw Exceptions.show(t);
								}
							}
						});
		            }
				}
				clazz = clazz.getSuperclass();
			}
		}
		
	}
	private Injector injector;

	protected void init(Map<Key,Binding> pBindings) {
		injector = newInjector(pBindings);
	}
	
	@Override
	public void init(Object pInstance) {
		injector.injectMembers(pInstance);
	}

	public <O> O getInstance(Class<O> pType, String pName) {
		final Object obj;
		if (pName == null  ||  pName.length() == 0) {
			obj = injector.getInstance(pType);
		} else {
			@SuppressWarnings("unchecked")
			final Class<Object> cl = (Class<Object>) pType;
			final com.google.inject.Key<Object> key = com.google.inject.Key.get(cl, Names.named(pName));
			obj = injector.getInstance(key);
		}
		if (obj == null  ||  obj == NULL_INSTANCE) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final O o = (O) obj;
		return o;
	}

	protected Injector newInjector(final Map<Key,Binding> pBindings) {
		final Module module = new Module() {
			@Override
			public void configure(Binder pBinder) {
				for (Map.Entry<Key,Binding> en : pBindings.entrySet()) {
					final Key key = en.getKey();
					final Binding b = en.getValue();
					final String name = key.getName();
					if (name.length() == 0) {
						pBinder.bind(b.getType()).toProvider(b.getProvider()).in(Scopes.SINGLETON);
					}
				}
				pBinder.bindListener(Matchers.any(), new LoggerInjectingTypeListener());
			}
		};
		return Guice.createInjector(module);
	}

	public Injector getInjector() {
		return injector;
	}
}
