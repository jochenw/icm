package com.github.jochenw.rcm.isclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;

public class DefaultValuesReader implements ValuesReader {
	public static class TypedMap<K,V> extends HashMap<K,V> {
		private static final long serialVersionUID = 5238918262532765317L;
		private final String javaClass;
		public TypedMap(String pJavaClass) {
			javaClass = pJavaClass;
		}
		public String getJavaClass() {
			return javaClass;
		}
	}
	public static class TypedList<E> extends ArrayList<E> {
		private static final long serialVersionUID = -8741674316724758380L;
		private final String type;
		private final int depth;
		public TypedList(String pType, int pDepth) {
			type = pType;
			depth = pDepth;
		}
		public String getType() {
			return type;
		}
		public int getDepth() {
			return depth;
		}
	}
	private static class TypedStringBuilder {
		private final StringBuilder sb = new StringBuilder();
		private final String type;
		TypedStringBuilder(String pType) {
			type = pType;
		}
		String getType() {
			return type;
		}
		StringBuilder getStringBuilder() {
			return sb;
		}
	}
	private static class NamedObject {
		private final String name;
		private final Object value;
		NamedObject(String pName, Object pValue) {
			name = pName;
			value = pValue;
		}
		public String getName() { return name; }
		public Object getValue() { return value; }
	}
	private static class Handler implements ContentHandler {
		private final List<Object> stack = new ArrayList<Object>();
		private Object current;
		private Locator locator;
		private Object result;

		public Object getResult() {
			return result;
		}

		public void setDocumentLocator(Locator pLocator) {
			locator = pLocator;
		}

		public void startDocument() throws SAXException {
			stack.clear();
			current = null;
		}

		protected SAXException error(String pMessage) throws SAXException {
			return new SAXParseException(pMessage, new LocatorImpl(locator));
		}
		
		public void endDocument() throws SAXException {
			if (!stack.isEmpty()) {
				throw error("Unexpected end of document");
			}
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// Ignore this
		}

		public void endPrefixMapping(String prefix) throws SAXException {
			// Ignore this
		}

		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAtts) throws SAXException {
			final String name = pAtts.getValue("name");
			final Object value;
			if (isElement(pUri, "Values", pLocalName)) {
				final String version = pAtts.getValue("version");
				if (!"2.0".equals(version)) {
					throw error("Unexpected version number for Values: " + version);
				}
				if (stack.isEmpty()) {
					value = new HashMap<String,Object>();
				} else {
					throw error("Values element not expected at level " + stack.size());
				}
			} else if (isElement(pUri, "value", pLocalName)  ||  isElement(pUri, "Boolean", pLocalName)) {
				value = new StringBuilder();
			} else if (isElement(pUri, "number", pLocalName)) {
				final String type = pAtts.getValue("type");
				if (!"Integer".equals(type)  &&  !"Long".equals(type)  &&  !"Short".equals(type)  &&  !"Byte".equals(type)) {
					throw error("Invalid number type: " + pAtts.getValue("type"));
				}
				value = new TypedStringBuilder(type);
			} else if (isElement(pUri, "double", pLocalName)) {
				value = new TypedStringBuilder("Double");
			} else if (isElement(pUri, "float", pLocalName)) {
				value = new TypedStringBuilder("Float");
			} else if (isElement(pUri, "array", pLocalName)) {
				final String type = pAtts.getValue("type");
				final String depthStr = pAtts.getValue("depth");
				final int depth;
				if (depthStr == null) {
					depth = 0;
				} else {
					try {
						depth = Integer.parseInt(depthStr);
					} catch (NumberFormatException nfe) {
						throw error("Invalid value for record/@depth: " + depthStr);
					}
				}
				value = new TypedList<Object>(type, depth);
			} else if (isElement(pUri, "record", pLocalName)) {
				final String javaclass = pAtts.getValue("javaclass");
				value = new TypedMap<Object,Object>(javaclass);
			} else {
				throw error("Unecpected element found: " + pQName);
			}
			if (name != null  &&  name.length() > 0) {
				push(new NamedObject(name, value));
			} else {
				push(value);
			}
		}

		protected void push(Object pObject) {
			if (current != null) {
				stack.add(current);
			}
			current = pObject;
		}

		protected Object pop() {
			final Object o = current;
			if (stack.isEmpty()) {
				current = null;
			} else {
				current = stack.remove(stack.size()-1);
			}
			return o;
		}
		
		protected boolean isElement(String pUri, String pExpected, String pActual) {
			if (pUri != null  &&  pUri.length() > 0) {
				return false; // Expected default namespace
			}
			return pExpected.equals(pActual);
		}
		
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (stack.isEmpty()) {
				result = current;
			}
			final Object o = pop();
			Object envelope = current;
			if (envelope == null) {
				return;
			}
			if (envelope instanceof NamedObject) {
				envelope = ((NamedObject) envelope).getValue();
			}
			if (envelope instanceof Map) {
				@SuppressWarnings("unchecked")
				final Map<String,Object> map = (Map<String,Object>) envelope;
				if (!(o instanceof NamedObject)) {
					throw error("Expected name for map entry not found.");
				}
				final NamedObject no = (NamedObject) o;
				Object v = asValue(no.getValue());
				if ("Boolean".equals(localName)  &&  v instanceof String) {
					v = Boolean.valueOf((String) v);
				}
				map.put(no.getName(), asValue(v));
			} else if (envelope instanceof List) {
				@SuppressWarnings("unchecked")
				final List<Object> list = (List<Object>) envelope;
				list.add(asValue(o));
			} else {
				throw error("Invalid type of envelope: " + envelope.getClass().getName());
			}
		}

		private Object asValue(Object pObject) throws SAXException {
			Object o = pObject;
			if (o instanceof NamedObject) {
				o = ((NamedObject) o).getValue();
			}
			if (o instanceof StringBuilder) {
				o = o.toString();
			} else if (o instanceof TypedStringBuilder) {
				final TypedStringBuilder tsb = (TypedStringBuilder) o;
				final String type = tsb.getType();
				final String value = tsb.getStringBuilder().toString();
				if ("Integer".equals(type)) {
					try {
						return Integer.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid integer value: " + value);
					}
				} else if ("Short".equals(type)) {
					try {
						return Short.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid short value: " + value);
					}
				} else if ("Long".equals(type)) {
					try {
						return Long.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid long value: " + value);
					}
				} else if ("Byte".equals(type)) {
					try {
						return Byte.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid byte value: " + value);
					}
				} else if ("Double".equals(type)) {
					try {
						return Double.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid long value: " + value);
					}
				} else if ("Float".equals(type)) {
					try {
						return Float.valueOf(value);
					} catch (NumberFormatException nfe) {
						throw error("Invalid byte value: " + value);
					}
				} 
			}
			return o;
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (!append(ch, start, length)) {
				boolean isWhitespace = true;
				for (int i = 0;  i < length;  i++) {
					if (!Character.isWhitespace(ch[i+start])) {
						isWhitespace = false;
						break;
					}
				}
				if (!isWhitespace) {
					throw error("Unexpected non-whitespace characters: " + new String(ch, start, length));
				}
			}
		}

		private boolean append(char[] ch, int start, int length) {
			Object o = current;
			if (o instanceof NamedObject) {
				o = ((NamedObject) o).getValue();
			}
			if (o instanceof StringBuilder) {
				((StringBuilder) o).append(ch, start, length);
				return true;
			} else if (o instanceof TypedStringBuilder) {
				((TypedStringBuilder) o).getStringBuilder().append(ch, start, length);
				return true;
			} else {
				return false;
			}
		}
		
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			if (!append(ch, start, length)) {
				// Ignore this
			}
		}

		public void processingInstruction(String pTarget, String pData) throws SAXException {
			throw error("Unexpected PI: " + pTarget + ", " + pData);
		}

		public void skippedEntity(String pName) throws SAXException {
			throw error("Unexpected skipped entity: " + pName);
		}
	}

	public <O> O read(Reader pReader) {
		final InputSource isource = new InputSource(pReader);
		return read(isource);
	}

	public <O> O read(InputStream pStream) {
		final InputSource isource = new InputSource(pStream);
		return read(isource);
	}

	public <O> O read(File pFile) {
		InputStream istream = null;
		Throwable th = null;
		O result = null;
		try {
			istream = new FileInputStream(pFile);
			result = read(istream);
			istream.close();
			istream = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (istream != null) {
				try {
					istream.close();
				} catch (Throwable t) {
					if (th == null) { th = t; }
				}
			}
		}
		if (th != null) {
			if (th instanceof RuntimeException) { throw (RuntimeException) th; }
			if (th instanceof Error) { throw (Error) th; }
			throw new UndeclaredThrowableException(th);
		}
		return result;
	}

	public <O> O read(URL pUrl) {
		InputStream istream = null;
		Throwable th = null;
		O result = null;
		try {
			istream = pUrl.openStream();
			result = read(istream);
			istream.close();
			istream = null;
		} catch (Throwable t) {
			th = t;
		} finally {
			if (istream != null) {
				try {
					istream.close();
				} catch (Throwable t) {
					if (th == null) { th = t; }
				}
			}
		}
		if (th != null) {
			if (th instanceof RuntimeException) { throw (RuntimeException) th; }
			if (th instanceof Error) { throw (Error) th; }
			throw new UndeclaredThrowableException(th);
		}
		return result;
	}

	public <O> O read(InputSource pSource) {
		try {
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			final XMLReader xr = spf.newSAXParser().getXMLReader();
			final Handler h = new Handler();
			xr.setContentHandler(h);
			xr.parse(pSource);
			@SuppressWarnings("unchecked")
			final O o = (O) h.getResult();
			return o;
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (IOException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}
}
