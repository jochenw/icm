package com.github.jochenw.icm.core.impl.cf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.cf.InjectProperty;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.util.Exceptions;

public class SimpleComponentFactory extends ComponentFactory {
	@Override
	protected void init(Map<Key, Binding> pBindings) {
		// Nothing to do
	}

	@Override
	public void init(Object pInstance) {
		@SuppressWarnings("unchecked")
		final Class<Object> cl = (Class<Object>) pInstance.getClass();
		final Consumer<Object> configurator = getConfigurator(cl);
		configurator.accept(pInstance);
	}

	@Override
	public <O> O getInstance(Class<O> pType, String pName) {
		final Key key = new Key(pType, pName);
		final Binding binding = getBindings().get(key);
		if (binding == null) {
			return null;
		} else {
			final Object obj = binding.getInstance();
			if (obj == null  ||  obj == NULL_INSTANCE) {
				return null;
			}
			@SuppressWarnings("unchecked")
			final O o = (O) obj;
			return o;
		}
	}

	protected Consumer<Object> getConfigurator(Class<Object> pType) {
		final List<Consumer<Object>> configurators = new ArrayList<>();
		findConfigurators(configurators, pType);
		return new Consumer<Object>() {
			@Override
			public void accept(Object pInstance) {
				for (Consumer<Object> c : configurators) {
					c.accept(pInstance);
				}
			}
		};
	}

	protected void findConfigurators(List<Consumer<Object>> pList, Class<Object> pType) {
		findFieldConfigurators(pList, pType);
		findMethodConfigurators(pList, pType);
		final Class<Object> parentClass = pType.getSuperclass();
		if (parentClass != null  &&  parentClass != Object.class) {
			findConfigurators(pList, parentClass);
		}
	}

	protected void findMethodConfigurators(List<Consumer<Object>> pList, Class<Object> pType) {
		for (Method m : pType.getDeclaredMethods()) {
			final Inject inject = m.getAnnotation(Inject.class);
			if (inject != null) {
				final Class<?>[] types = m.getParameterTypes();
				final Key[] keys = new Key[types.length];
				final Binding[] bindings = new Binding[keys.length];
				for (int i = 0;  i < keys.length;  i++) {
					final Annotation[] annotations = m.getParameterAnnotations()[i];
					Named named = null;
					for (Annotation ann : annotations) {
						if (ann instanceof Named) {
							named = (Named) ann;
							break;
						}
					}
					if (named == null) {
						keys[i] = new Key(types[i], null);
					} else {
						keys[i] = new Key(types[i], named.value());
					}
					bindings[i] = getBindings().get(keys[i]);
					if (bindings[i] == null) {
						throw new IllegalStateException("No binding available for parameter " + i
								                        + " in method " + m.getDeclaringClass().getName() + "." + m.getName());
					}
				}
				final Consumer<Object> consumer = new Consumer<Object>() {
					@Override
					public void accept(Object pInstance) {
						final Object[] values = new Object[bindings.length];
						for (int i = 0; i < bindings.length;  i++) {
							Object obj = bindings[i].getInstance();
							if (obj == NULL_INSTANCE) {
								obj = null;
							}
							values[i] = obj;
						}
						if (!m.isAccessible()) {
							m.setAccessible(true);
						}
						try {
							m.invoke(pInstance, values);
						} catch (Throwable t) {
							throw Exceptions.show(t);
						}
					}
				};
				pList.add(consumer);
			}
		}
	}

	protected void findFieldConfigurators(List<Consumer<Object>> pList, Class<Object> pType) {
		for (Field f : pType.getDeclaredFields()) {
			Binding binding = null;
			final Inject inject = f.getAnnotation(Inject.class);
			if (inject != null) {
				final Named named = f.getAnnotation(Named.class);
				final Key key;
				if (named == null) {
					key = new Key(f.getType(), null);
				} else {
					key = new Key(f.getType(), named.value());
				}
				binding = getBindings().get(key);
				if (binding == null) {
					throw new IllegalStateException("No binding available for field " + f.getDeclaringClass().getName() + "." + f.getName());
				} else {
					final Binding b = binding;
					final Consumer<Object> configurator = new Consumer<Object>() {
						@Override
						public void accept(Object pInstance) {
							Object value = b.getInstance();
							if (value == NULL_INSTANCE) {
								value = null;
							}
							setFieldValue(f, pInstance, value);
						}
					};
					pList.add(configurator);
				}
			} else {
				final InjectLogger injectLogger = f.getAnnotation(InjectLogger.class);
				if (injectLogger != null) {
					if (!IcmLogger.class.isAssignableFrom(f.getType())) {
						throw new IllegalStateException("The field " + f.getDeclaringClass().getName() + "." + f.getName()
						                                + " must have the type " + IcmLogger.class.getName());
					}
					String id = injectLogger.id();
					if (id.length() == 0) {
						id = f.getDeclaringClass().getName();
					}
					final Key key = new Key(IcmLoggerFactory.class, null);
					binding = getBindings().get(key);
					if (binding == null) {
						throw new IllegalStateException("No binding available for field " + f.getDeclaringClass().getName() + "." + f.getName());
					} else {
						final String loggerId = id;
						final Binding b = binding;
						final Consumer<Object> configurator = new Consumer<Object>() {
							@Override
							public void accept(Object pInstance) {
								final IcmLoggerFactory loggerFactory = (IcmLoggerFactory) b.getInstance();
								if (loggerFactory == null) {
									throw new IllegalStateException("IcmLoggerFactory not available");
								}
								final IcmLogger logger = loggerFactory.getLogger(loggerId);
								setFieldValue(f, pInstance, logger);
							}
						};
						pList.add(configurator);
					}
				} else {
					final InjectProperty injectProperty = f.getAnnotation(InjectProperty.class);
					if (injectProperty != null) {
						if (!String.class.isAssignableFrom(f.getType())) {
							throw new IllegalStateException("The field " + f.getDeclaringClass().getName() + "." + f.getName()
                            + " must have the type " + String.class.getName());
						}
						String id = injectProperty.id();
						if (id.length() == 0) {
							id = f.getDeclaringClass().getName() + "." + f.getName();
						}
						final Key key = new Key(IcmPropertyProvider.class, null);
						binding = getBindings().get(key);
						if (binding == null) {
							throw new IllegalStateException("No binding available for field " + f.getDeclaringClass().getName() + "." + f.getName());
						} else {
							final String propertyKey = id;
							final Binding b = binding;
							final Consumer<Object> configurator = new Consumer<Object>() {
								@Override
								public void accept(Object pInstance) {
									final IcmPropertyProvider propertyProvider = (IcmPropertyProvider) b.getInstance();
									if (propertyProvider == null) {
										throw new IllegalStateException("IcmPropertyProvider not available");
									}
									final String value = propertyProvider.getProperty(propertyKey);
									setFieldValue(f, pInstance, value);
								}
							};
							pList.add(configurator);
						}
					}
				}
			}
		}
	}

	protected void setFieldValue(Field f, Object pInstance, Object value) {
		if (!f.isAccessible()) {
			f.setAccessible(true);
		}
		try {
			f.set(pInstance, value);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
