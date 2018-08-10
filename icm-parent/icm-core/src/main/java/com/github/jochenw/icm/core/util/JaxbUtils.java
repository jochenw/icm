package com.github.jochenw.icm.core.util;

import javax.xml.bind.JAXBContext;


import org.xml.sax.InputSource;

public class JaxbUtils {
	public static Object parse(InputSource pSource, Class<?> pSchemaClass) {
		try {
			final JAXBContext ctx = JAXBContext.newInstance(new Class[] {pSchemaClass});
			return ctx.createUnmarshaller().unmarshal(pSource);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}

