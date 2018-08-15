package com.github.jochenw.icm.core.api.plugins;

import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.github.jochenw.icm.core.util.Exceptions;

public class IcmPluginsParser {
	public static class Handler implements ContentHandler {
		private final Consumer<String> classNameConsumer;
		private Locator locator;
		int level;

		public Handler(Consumer<String> pClassNameConsumer) {
			classNameConsumer = pClassNameConsumer;
		}
		@Override
		public void setDocumentLocator(Locator pLocator) {
			locator = pLocator;
		}

		@Override
		public void startDocument() throws SAXException {
			level = 0;
		}

		@Override
		public void endDocument() throws SAXException {
			// Nothing to do
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// Nothing to do
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			// Nothing to do
		}

		protected SAXException error(String pMessage) {
			return new SAXParseException(pMessage, locator);
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes pAttrs) throws SAXException {
			if (!XmlPluginRepository.NS_1_0_0.equals(uri)) {
				throw error("Expected namespace " + XmlPluginRepository.NS_1_0_0 + ", got " + uri);
			}
			switch (localName) {
			  case "icm-plugins":
				if (level != 0) {
					throw error("Element icm-plugins unexpected at level " + level);
				}
				break;
			  case "icm-plugin":
				  if (level != 1) {
					  throw error("Element icm-plugin unexpected at level " + level);
				  }
				  final String className = pAttrs.getValue(XMLConstants.NULL_NS_URI, "class");
				  if (className == null  ||  className.length() == 0) {
					  throw error("Missing, or empty attribute: icm-plugin/@class");
				  }
				  classNameConsumer.accept(className);
				  break;
			  default:
				  throw error("Unexpected element: " + qName);
			}
			++level;
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			--level;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			for (int i = 0;  i < length;  i++) {
				if (!Character.isWhitespace(ch[start+i])) {
					throw error("Unexpected non-whitespace characters: " + new String(ch, start, length));
				}
			}
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			// Do nothing
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			throw error("Unexpected PI: target=" + target + ", data=" + data);
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
			throw error("Unexpected skipped entity: " + name);
		}
	}

	public void parse(InputSource pSource, Consumer<String> classNameConsumer) {
		try {
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(false);
			spf.setNamespaceAware(true);
			final XMLReader xr = spf.newSAXParser().getXMLReader();
			final ContentHandler ch = new Handler(classNameConsumer);
			xr.setContentHandler(ch);
			xr.parse(pSource);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

}
