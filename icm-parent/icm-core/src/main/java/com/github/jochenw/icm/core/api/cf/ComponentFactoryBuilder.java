package com.github.jochenw.icm.core.api.cf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Provider;

import com.github.jochenw.icm.core.api.cf.ComponentFactory.Binding;
import com.github.jochenw.icm.core.api.cf.ComponentFactory.Key;
import com.github.jochenw.icm.core.impl.cf.SimpleComponentFactory;
import com.github.jochenw.icm.core.util.AbstractBuilder;
import com.github.jochenw.icm.core.util.Exceptions;


public class ComponentFactoryBuilder extends AbstractBuilder {
	public abstract class Binder {
		public void bindInstance(Object pSingleton) {
			bindInstance(pSingleton, null);
		}
		public <T> void bindList(List<T> pList, Class<? extends T> pType) {
			final Provider<List<T>> provider = new Provider<List<T>>() {
				@Override
				public List<T> get() {
					final ComponentFactory cf = (ComponentFactory) ComponentFactoryBuilder.this.getInstance();
					for (T t : pList) {
						cf.init(t);
					}
					return pList;
				}
			};
			bindProvider(List.class, provider, pType.getName());
		}
		public void bindInstance(Class<? extends Object> pType, Object pSingleton) {
			bindInstance(pType, pSingleton, null);
		}
		public void bindInstance(Class<? extends Object> pType, Object pSingleton, String pName) {
			@SuppressWarnings("unchecked")
			final Class<Object> type = (Class<Object>) pType;
			final Key key = new Key(type, pName);
			final Provider<Object> provider = new Provider<Object>() {
				@Override
				public Object get() {
					return pSingleton;
				}
			};
			add(key, type, provider);
		}
		public void bindInstance(Object pSingleton, String pName) {
			@SuppressWarnings("unchecked")
			final Class<Object> type = (Class<Object>) pSingleton.getClass();
			bindInstance(type, pSingleton, pName);
		}
		public void bindClass(Class<?> pType, Class<?> pImplementation) {
			bindClass(pType, pImplementation, null);
		}
		public void bindClass(Class<?> pType, Class<?> pImplementation, String pName) {
			final Key key = new Key(pType, pName);
			final Provider<Object> provider = new Provider<Object>() {
				@Override
				public Object get() {
					try {
						return pImplementation.newInstance();
					} catch (Throwable t) { 
						throw Exceptions.show(t);
					}
				}
			};
			@SuppressWarnings("unchecked")
			final Class<Object> type = (Class<Object>) pType;
			add(key, type, provider);
		}
		public void bindProvider(Class<?> pType, Provider<?> pProvider) {
			bindProvider(pType, pProvider, null);
		}
		public void bindProvider(Class<?> pType, Provider<?> pProvider, String pName) {
			final Key key = new Key(pType, pName);
			@SuppressWarnings("unchecked")
			final Provider<Object> provider = (Provider<Object>) pProvider;
			@SuppressWarnings("unchecked")
			final Class<Object> type = (Class<Object>) pType;
			add(key, type, provider);
		}
		
		protected abstract void add(Key pKey, Class<Object> pType, Provider<Object> pProvider);
	}
	public interface Module {
		void bind(Binder pBinder);
	}

	private List<Module> modules = new ArrayList<>();
	private Class<? extends ComponentFactory> componentFactoryClass;

	public ComponentFactoryBuilder module(Module pModule) {
		Objects.requireNonNull(pModule, "Module");
		assertMutable();
		modules.add(pModule);
		return this;
	}

	public ComponentFactoryBuilder modules(Module... pModules) {
		Objects.requireNonNull(pModules, "Modules");
		assertMutable();
		for (Module m : pModules) {
			modules.add(m);
		}
		return this;
	}

	public ComponentFactoryBuilder modules(Iterable<Module> pModules) {
		Objects.requireNonNull(pModules, "Modules");
		assertMutable();
		for (Module m : pModules) {
			modules.add(m);
		}
		return this;
	}

	public List<Module> getModules() {
		return modules;
	}

	public ComponentFactoryBuilder componentFactoryClass(Class<? extends ComponentFactory> pType) {
		assertMutable();
		componentFactoryClass = pType;
		return this;
	}

	public Class<? extends ComponentFactory> getComponentFactoryClass() {
		return componentFactoryClass == null ? SimpleComponentFactory.class : componentFactoryClass;
	}
	
	public ComponentFactory build() {
		return (ComponentFactory) super.build();
	}

	public ComponentFactory newInstance() {
		final ComponentFactory cf = newComponentFactoryInstance();
		cf.setBindings(getBindings(cf));
		return cf;
	}

	protected ComponentFactory newComponentFactoryInstance() {
		if (componentFactoryClass == null) {
			return new SimpleComponentFactory();
		} else {
			try {
				return componentFactoryClass.newInstance();
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}
	
	protected Map<Key,Binding> getBindings(ComponentFactory pComponentFactory) {
		final Map<Key,Binding> bindings = new HashMap<>();
		final Consumer<Object> consumer = new Consumer<Object>() {
			@Override
			public void accept(Object pInstance) {
				pComponentFactory.init(pInstance);
			}
		};
		final Binder binder = new Binder() {
			@Override
			protected void add(Key pKey, Class<Object> pType, Provider<Object> pProvider) {
				final Binding binding = new Binding(pType, pProvider, consumer);
				bindings.put(pKey,binding);
			}
		};
		binder.bindInstance(ComponentFactory.class, pComponentFactory);
		for (Module module : modules) {
			module.bind(binder);
		}
		return bindings;
	}
}
