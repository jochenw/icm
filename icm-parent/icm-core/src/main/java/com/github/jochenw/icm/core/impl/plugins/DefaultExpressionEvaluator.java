package com.github.jochenw.icm.core.impl.plugins;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.plugins.ExpressionEvaluator;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;


public class DefaultExpressionEvaluator implements ExpressionEvaluator {
	@Inject IcmPropertyProvider propertyProvider;

	@Override
	public boolean evaluate(String pExpression) {
		if (pExpression == null) { return false; }
		if (pExpression.startsWith("!${")  &&  pExpression.endsWith("}")) {
			final String key = pExpression.substring("!${".length(), pExpression.length()-"}".length());
			return !Boolean.parseBoolean(propertyProvider.getProperty(key));
		} else if (pExpression.startsWith("${")  &&  pExpression.endsWith("}")) {
			final String key = pExpression.substring("${".length(), pExpression.length()-"}".length());
			return Boolean.parseBoolean(propertyProvider.getProperty(key));
		} else {
			throw new IllegalStateException("Invalid expression: " + pExpression);
		}
	}
}
