package com.github.jochenw.icm.core.impl;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmInstallationTarget;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.FileLocker;
import com.github.jochenw.icm.core.util.FileLocker.StreamAccessor;


public class XmlFileInstallationTarget<V> implements IcmInstallationTarget<V> {
	public static final String NS = "http://namespaces.github.com/jochenw/icm/schemas/xfit/1.0.0";

	public static class InstalledResource {
		private final String name, type, version;

		public InstalledResource(String name, String type, String version) {
			super();
			this.name = name;
			this.type = type;
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getVersion() {
			return version;
		}
	}
	private final File file;
	@SuppressWarnings("rawtypes")
	@Inject IcmChangeNumberHandler versionProvider;

	public XmlFileInstallationTarget(File pFile) {
		file = pFile;
	}
	
	@Override
	public boolean isInstalled(Context pContext, IcmChangeInfo<V> pResource) {
		final Function<StreamAccessor,Boolean> action = new Function<StreamAccessor,Boolean>() {
			@Override
			public Boolean apply(StreamAccessor pAcc) {
				try (InputStream in = pAcc.getInputStream()) {
					final PushbackInputStream pis = new PushbackInputStream(in, 1);
					final int firstByte = pis.read();
					if (firstByte == -1) {
						// File is empty, so no installations recorded yet.
						pis.close();
					} else {
						pis.unread(firstByte);
						final List<InstalledResource> resources = getResources(pis);
						for (InstalledResource ir : resources) {
							@SuppressWarnings("unchecked")
							final IcmChangeNumberHandler<V> icnh = (IcmChangeNumberHandler<V>) versionProvider;
							if (ir.getName().equals(pResource.getTitle())  &&
								ir.getType().equals(pResource.getType())  &&
								ir.getVersion().equals(icnh.asString(pResource.getVersion()))) {
								return Boolean.TRUE;
							}
						}
					}
					return Boolean.FALSE;
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
			}
		};
		return callLocked(action).booleanValue();
	}

	protected List<InstalledResource> getResources(InputStream pis) {
		final XmlFileReadingHandler xfrh = new XmlFileReadingHandler();
		try {
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(false);
			spf.setNamespaceAware(true);
			final XMLReader xr = spf.newSAXParser().getXMLReader();
			xr.setContentHandler(xfrh);
			xr.parse(new InputSource(pis));
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		return xfrh.getResources();
	}

	@Override
	public void add(Context pContext, IcmChangeInfo<V> pResource) {
		final Function<StreamAccessor,Object> action = new Function<StreamAccessor,Object>() {
			@Override
			public Object apply(StreamAccessor pAcc) {
				try {
					final List<InstalledResource> resources = new ArrayList<>();
					try (InputStream in = pAcc.getInputStream()) {
						PushbackInputStream pis = new PushbackInputStream(in, 1);
						int firstByte = pis.read();
						if (firstByte != -1) {
							pis.unread(firstByte);
							resources.addAll(getResources(pis));
						}
					}
					
					@SuppressWarnings("unchecked")
					final IcmChangeNumberHandler<V> icnh = (IcmChangeNumberHandler<V>) versionProvider;
					resources.add(new InstalledResource(pResource.getTitle(), pResource.getType(), icnh.asString(pResource.getVersion())));
					try (OutputStream out = pAcc.getOutputStream()) {
						writeResources(resources, out);
					}
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
				return null;
			}
		};
		callLocked(action);
	}

	protected void writeResources(List<InstalledResource> pResources, OutputStream pOut) {
		final File dir = file.getParentFile();
		if (dir != null  &&  !dir.isDirectory()  &&  !dir.mkdirs()) {
			throw new IllegalStateException("Unable to create directory: " + dir.getAbsolutePath());
		}
		try {
			final SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
			final TransformerHandler th = stf.newTransformerHandler();
			final Transformer t = th.getTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.setOutputProperty(OutputKeys.STANDALONE, "yes");
			th.setResult(new StreamResult(pOut));
			th.startDocument();
			th.startElement(NS, "installedResources", "installedResources", new AttributesImpl());
			for (InstalledResource ir : pResources) {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "name", "name", "CDATA", ir.getName());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "type", "type", "CDATA", ir.getType());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "version", "version", "CDATA", ir.getVersion());
				th.startElement(NS, "resource", "resource", attrs);
				th.endElement(NS, "resource", "resource");
			}
			th.endElement(NS, "installedResources", "installedResources");
			th.endDocument();
			
			
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	protected <T> T callLocked(Function<StreamAccessor,T> pConsumer) {
		return FileLocker.callLocked(file, pConsumer);
	}

	@Override
	public int getNumberOfInstalledResources(Context pContext) {
		final Function<StreamAccessor,Integer> action = new Function<StreamAccessor,Integer>() {
			@Override
			public Integer apply(StreamAccessor pIn) {
				try (InputStream in = pIn.getInputStream()) {
					final PushbackInputStream pis = new PushbackInputStream(in, 1);
					final int firstByte = pis.read();
					if (firstByte == -1) {
						// File is empty, so no installations recorded yet.
						pis.close();
						return Integer.valueOf(0);
					} else {
						pis.unread(firstByte);
						final List<InstalledResource> resources = getResources(pis);
						return Integer.valueOf(resources.size());
					}
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
			}
		};
		return callLocked(action).intValue();
	}

	public class XmlFileReadingHandler implements ContentHandler {
		private List<InstalledResource> resources;
		private Locator locator;
		private int level;

		@Override
		public void setDocumentLocator(Locator pLocator) {
			locator = pLocator;
		}

		@Override
		public void startDocument() throws SAXException {
			resources = new ArrayList<>();
			level = 0;
		}

		protected SAXException error(String pMessage) {
			return new SAXParseException(pMessage, locator);
		}
		
		@Override
		public void endDocument() throws SAXException {
			if (level != 0) {
				throw error("Expected level=0, got " + level);
			}
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// Ignore this
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			// Ignore this
		}

		@Override
		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
			if (!NS.equals(pUri)) {
				throw error("Expected namespace=" + NS + ", got " + pUri);
			}
			if ("installedResources".equals(pLocalName)) {
				if (level != 0) {
					throw error("Unexpected element " + pQName + " at level " + level);
				}
			} else if ("resource".equals(pLocalName)) {
				if (level == 1) {
					final String name = pAttrs.getValue("name");
					final String type = pAttrs.getValue("type");
					final String version = pAttrs.getValue("version");
					if (name == null  ||  name.length() == 0) {
						throw error("Missing name attribute");
					}
					if (type == null  ||  type.length() == 0) {
						throw error("Missing type attribute");
					}
					if (version == null  ||  version.length() == 0) {
						throw error("Missing type attribute");
					}
					try {
						versionProvider.asVersion(version);
					} catch (Throwable t) {
						throw error("Invalid version attribute: " + version);
					}
					resources.add(new InstalledResource(name, type, version));
				} else {
					throw error("Unexpected element " + pQName + " at level " + level);
				}
			} else {
				throw error("Unexpected element " + pQName + " at level " + level);
			}
			++level;
		}

		@Override
		public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
			if (!NS.equals(pUri)) {
				throw error("Expected namespace=" + NS + ", got " + pUri);
			}
			--level;
			if ("installedResources".equals(pLocalName)) {
				if (level != 0) {
					throw error("Unexpected element " + pQName + " at level " + level);
				}
			} else if ("resource".equals(pLocalName)) {
				if (level != 1) {
					throw error("Unexpected element " + pQName + " at level " + level);
				}
			} else {
				throw error("Unexpected element " + pQName + " at level " + level);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			for (int i = 0;  i < length;  i++) {
				final char c = ch[i];
				if (!Character.isWhitespace(c)) {
					throw error("Unexpected non-whitespace character: " + new String(ch, start, length));
				}
			}
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
		}

		public List<InstalledResource> getResources() {
			return resources;
		}
	}
}
