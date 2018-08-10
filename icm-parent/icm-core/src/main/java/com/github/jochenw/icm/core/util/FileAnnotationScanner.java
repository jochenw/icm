package com.github.jochenw.icm.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches for annotations in a text string. An annotation is something like
 * <pre>@SuppressWarnings(user="jwi", reason="Not Given", rules="rule0, rule1, ...")</pre>
 * The annotations name ("SuppressWarnings" in the example), and the attribute
 * names ("user", "reason", and "rules" in the example) must be valid Java
 * identifiers. The attribute values must be valid Java strings. In particular,
 * the following are supported: \", \\, \n, \r, \\uABCD (with ABCD being
 * up to four hexadecimal character).
 */
public class FileAnnotationScanner {
	public static class Annotation implements Serializable {
		private static final long serialVersionUID = 2761700259919020204L;
		private final String name;
		private final Map<String,String> attributes;

		public Annotation(String pName, Map<String,String> pAttributes) {
			name = pName;
			attributes = Collections.unmodifiableMap(pAttributes);
		}

		public String getName() {
			return name;
		}

		public Map<String,String> getAttributes() {
			return attributes;
		}

		public String getAttribute(String pAttrName) {
			return attributes.get(pAttrName);
		}
	}

	public static List<Annotation> parse(String pText) {
		StringReader sr = null;
		List<Annotation> annotations = null;
		Throwable th = null;
		try {
			sr = new StringReader(pText);
			annotations = parse(sr);
			sr.close();
			sr = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (sr != null) {
				try {
					sr.close();
				} catch (Throwable t) {
					if (th == null) {
						th = t;
					}
				}
			}
		}
		if (th != null) {
			if (th instanceof RuntimeException) {
				throw (RuntimeException) th;
			}
			if (th instanceof Error) {
				throw (Error) th;
			}
			throw new UndeclaredThrowableException(th);
		}
		return annotations;
	}
 	
	public static List<Annotation> parse(Reader pReader) {
		try {
			if (pReader instanceof BufferedReader) {
				return parse((BufferedReader) pReader);
			} else {
				return parse(new BufferedReader(pReader));
			}
		} catch (IOException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static List<Annotation> parse(BufferedReader pReader) throws IOException {
		final List<Annotation> annotations = new ArrayList<Annotation>();
		for (;;) {
			final int c = pReader.read();
			if (c == -1) {
				break;
			} else if (c == '@') {
				Annotation annotation = parseAnnotation(pReader);
				if (annotation != null) {
					annotations.add(annotation);
				}
			} else {
				//Ignore this character.
			}
		}
		return annotations;
	}

	private static Annotation parseAnnotation(BufferedReader pReader) throws IOException {
		// Read annotation name. Note, that the leading '@' has already been read.
		final StringBuilder nameSb = new StringBuilder();
		final int c0 = pReader.read();
		if (c0 == -1  ||  !Character.isJavaIdentifierStart(c0)) {
			return null;
		}
		nameSb.append((char) c0);
		for (;;) {
			final int c = pReader.read();
			if (c == -1) {
				return null;
			} else if (c == '(') {
				break;
			} else if (Character.isJavaIdentifierPart(c)) {
				nameSb.append((char) c);
 			} else {
 				return null;
 			}
		}

		// We have read the '('. Continue reading the attributes.
		final Map<String,String> attributes = new HashMap<String,String>();
		int cAttr0 = pReader.read();
		for (;;) {
			if (cAttr0 == -1) {
				return null;
			} else if (cAttr0 == ')') {
				return new Annotation(nameSb.toString(), attributes);
			} else if (Character.isJavaIdentifierStart(cAttr0)) {
				final String[] attr = parseAttribute(pReader, (char) cAttr0);
				if (attr == null) {
					return null;
				} else {
					if (attributes.put(attr[0], attr[1]) != null) {
						throw new IllegalArgumentException("Attribute " + attr[0] + " is present twice in annotation " + nameSb); 
					}
				}
				cAttr0 = pReader.read();
			} else if (cAttr0 == ',') {
				for (;;) {
					final int c = pReader.read();
					if (c == -1) {
						return null;
					} else if (!Character.isWhitespace(c)) {
						cAttr0 = c;
						break;
					}
				}
			} else if (Character.isWhitespace(cAttr0)) {
				// Skip whitespace
				cAttr0 = pReader.read();
			} else {
				return null;
			}
		}
	}

	private static String[] parseAttribute(Reader pReader, char pAttr0) throws IOException {
		final StringBuilder attrNameSb = new StringBuilder();
		attrNameSb.append(pAttr0);
		for (;;) {
			final int c = pReader.read();
			if (c == -1) {
				return null;
			} else if (c == '=') {
				final String value = parseAttributeValue(pReader);
				if (value == null) {
					return null;
				}
				return new String[]{attrNameSb.toString(), value};
			} else if (Character.isJavaIdentifierPart(c)) {
				attrNameSb.append((char) c);
			} else {
				return null;
			}
		}
	}

	private static String parseAttributeValue(Reader pReader) throws IOException {
		// Skip leading white space, if present.
		int c;
		for (;;) {
			c = pReader.read();
			if (c == -1) {
				return null;
			} else if (c == '\"') {
				final StringBuilder valueSb = new StringBuilder();
				for (;;) {
					final int valueC = pReader.read();
					if (valueC == -1) {
						return null;
					} else if (valueC == '\"') {
						return valueSb.toString();
					} else if (valueC == '\\') {
						final int cU = pReader.read();
						switch (cU) {
						case 'u':
						case 'U':
							break;
						case 'r':
						case 'R':
							valueSb.append('\r');
							break;
						case 'n':
						case 'N':
							valueSb.append('\n');
							break;
						case 't':
						case 'T':
							valueSb.append('\t');
							break;
						case 'f':
						case 'F':
							valueSb.append('\f');
							break;
						default:
							valueSb.append((char) cU);
							break;
						}
					} else {
						valueSb.append((char) valueC);
					}
				}
			} else if (Character.isWhitespace(c)) {
				// Skip this character.
			} else {
				return null;
			}
		}
	}
}
