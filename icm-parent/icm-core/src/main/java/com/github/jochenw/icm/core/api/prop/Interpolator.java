package com.github.jochenw.icm.core.api.prop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;


public interface Interpolator {
	public interface StringSet {
		public Iterator<Map.Entry<String,String>> getValues();
		public String getValue(String pKey);
		public void setValue(String pKey, String pValue);
	}
	public boolean isInterpolatable(String pValue);
	public String interpolate(String pValue);
	public void interpolate(StringSet pValues);
	public default void interpolate(final Map<Object,Object> pProperties) {
		final StringSet stringSet = new StringSet() {
			@Override
			public Iterator<Entry<String, String>> getValues() {
				final Iterator<Entry<Object,Object>> iter = pProperties.entrySet().iterator();
				return new Iterator<Entry<String,String>>(){
					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public Entry<String, String> next() {
						final Entry<Object,Object> entry = iter.next();
						return new Entry<String,String>(){
							@Override
							public String getKey() {
								return (String) entry.getKey();
							}

							@Override
							public String getValue() {
								return (String) entry.getValue();
							}

							@Override
							public String setValue(String pValue) {
								final String value = getValue();
								entry.setValue(pValue);
								return value;
							}
						};
					}
					
				};
			}

			@Override
			public String getValue(String pKey) {
				return (String) pProperties.get(pKey);
			}

			@Override
			public void setValue(String pKey, String pValue) {
				pProperties.put(pKey, pValue);
			}
		};
		interpolate(stringSet);
	}

	public default Properties filter(final Properties pProperties) {
		final Properties interpolatedProperties = new Properties();
		interpolatedProperties.putAll(pProperties);
		final Map<Object,Object> map = interpolatedProperties;
		interpolate(map);
		return interpolatedProperties;
	}

	public InputStream filter(InputStream pIn, Charset pCharset) throws IOException; 
	public InputStream filter(InputStream pIn, Function<String,String> pPropertyProvider, Charset pCharset) throws IOException; 
}
