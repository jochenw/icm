package com.github.jochenw.icm.core.api.cf;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Provider;

public abstract class ComponentFactory {
	public static class Key {
		private final String typeId, name;
		public Key(String pTypeId, String pName) {
			Objects.requireNonNull(pTypeId, "Type Id");
			typeId = pTypeId;
			name = pName == null ? "" : pName;
		}
		public Key(Class<?> pType, String pName) {
			this(pType.getName(), pName);
		}

		public String getTypeId() {
			return typeId;
		}
		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			return 31 * (31 + name.hashCode()) +typeId.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			return name.equals(other.name)  &&  typeId.equals(other.typeId);
		}
	}

	public static class Binding {
		private final Consumer<Object> initializer;
		private final Provider<Object> provider;
		private final Class<Object> type;
		private Object instance;
		private boolean initialized;

		public Binding(Class<Object> pType, Provider<Object> pProvider, Consumer<Object> pInitializer) {
			Objects.requireNonNull(pType, "Type");
			Objects.requireNonNull(pProvider, "Provider");
			provider = pProvider;
			initializer = pInitializer;
			type = pType;
		}

		public Binding(Class<Object> pType, Provider<Object> pProvider) {
			this(pType, pProvider, null);
		}

		public Class<Object> getType() {
			return type;
		}

		public Provider<Object> getProvider() {
			return provider;
		}
		
		public Object getInstance() {
			if (!initialized) {
				instance = provider.get();
				initialized = true;
				if (initializer != null) {
					initializer.accept(instance);
				}
			}
			return instance;
		}
	}

	public static final Object NULL_INSTANCE = new Object();
	public static final Provider<Object> NULL_PROVIDER = new Provider<Object>() {
		@Override
		public Object get() {
			return NULL_INSTANCE;
		}
	};

	private Map<Key,Binding> bindings;
	protected Map<Key,Binding> getBindings() {
		return bindings;
	}
	protected abstract void init(Map<Key,Binding> pBindings);
	void setBindings(Map<Key,Binding> pBindings) {
		if (bindings == null) {
			bindings = pBindings;
			init(pBindings);
		} else {
			throw new IllegalStateException("Bindings already present");
		}
	}

	public abstract void init(Object pInstance);
	public abstract <O> O getInstance(Class<O> pType, String pName);
	public <O> O getInstance(Class<O> pType) {
		return getInstance(pType, null);
	}
	public <O> O requireInstance(Class<O> pType, String pName) throws NoSuchElementException {
		final Object obj = getInstance(pType, pName);
		if (obj == null  || obj == NULL_INSTANCE) {
			throw new NoSuchElementException("No binding registered for " + pType.getName() + ":" + (pName == null ? "" : pName)); 
		}
		@SuppressWarnings("unchecked")
		final O o = (O) obj;
		return o;
	}
	public <O> O requireInstance(Class<O> pType) throws NoSuchElementException {
		return requireInstance(pType, null);
	}
	public <O> List<O> getList(Class<O> pType) {
		@SuppressWarnings("unchecked")
		final List<O> list = getInstance(List.class, pType.getName());
		return list;
	}
}
