package com.github.jochenw.icm.core.impl.prop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.cf.InjectProperty;
import com.github.jochenw.icm.core.api.prop.Interpolator;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.util.InterpolatingInputStream;
import com.github.jochenw.icm.core.util.Objects;

public class DefaultInterpolator implements Interpolator {
	@InjectProperty(id="interpolator.startToken") private String startToken;
	@InjectProperty(id="interpolator.endToken") private String endToken;
	@Inject IcmPropertyProvider propertyProvider;
	private final Properties properties;

	public DefaultInterpolator() {
		properties = null;
	}

	public DefaultInterpolator(Properties pProps) {
		Objects.requireNonNull(pProps, "Properties");
		properties = pProps;
	}
	
	public String getStartToken() {
		if (startToken == null) {
			return "${";
		} else {
			return startToken;
		}
	}

	public String getEndToken() {
		if (endToken == null) {
			return "}";
		} else {
			return endToken;
		}
	}
	
	@Override
	public boolean isInterpolatable(String pValue) {
		Objects.requireNonNull(pValue, "Value");
		final String strtToken = getStartToken();
		int startOffset = pValue.indexOf(strtToken);
		if (startOffset == -1) {
			return false;
		} else {
			final int endOffset = pValue.indexOf(getEndToken(), startOffset+strtToken.length());
			return endOffset != -1;
		}
	}

	@Override
	public String interpolate(String pValue) {
		Objects.requireNonNull(pValue, "Value");
		final String strtToken = getStartToken();
		final String ndToken = getEndToken();
		String value = pValue;
		boolean finished = false;
		while (!finished) {
			finished = true;
			final int startOffset = value.lastIndexOf(strtToken);
			if (startOffset != -1) {
				final int endOffset = value.indexOf(ndToken, startOffset+strtToken.length());
				if (endOffset != -1) {
					final String key = value.substring(startOffset+strtToken.length(), endOffset);
					value = value.substring(0, startOffset) + getPropertyValue(key) + value.substring(endOffset+ndToken.length());
					finished = false;
				}
			}
		}
		return value;
	}

	@Override
	public void interpolate(StringSet pValues) {
		boolean finished = false;
		while (!finished) {
			finished = true;
			final Iterator<Map.Entry<String,String>> iter = pValues.getValues();
			while (iter.hasNext()) {
				final Map.Entry<String,String> en = iter.next();
				final String value = en.getValue();
				if (isInterpolatable(value)) {
					en.setValue(interpolate(value));
					finished = false;
				}
			}
		}
	}

	protected String getPropertyValue(String pKey) {
		if (properties == null) {
			return propertyProvider.getProperty(pKey);
		} else {
			return properties.getProperty(pKey);
		}
	}

	@Override
	public InputStream filter(InputStream pIn, Charset pCharset) throws IOException {
		final Function<String,String> propertyInterpolator = (s) -> propertyProvider.getProperty(s);
		return filter(pIn, propertyInterpolator, pCharset);
	}

	@Override
	public InputStream filter(InputStream pIn, Function<String,String> pPropertyProvider, Charset pCharset) throws IOException {
		return new InterpolatingInputStream(pIn, pPropertyProvider, pCharset, startToken, endToken);		
	}
}
