package com.github.jochenw.icm.core.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.github.jochenw.icm.core.api.plugins.Attribute;
import com.github.jochenw.icm.core.api.plugins.IcmChange;

public class AsmClassInfoProvider {
	public interface ClassInfo {
		public String getQName();
		public String getSimpleName();
		public String getResourceName();
		public String getResourceType();
		public String getResourceDescription();
		public String getResourceVersion();
		public Map<String,String> getAttributes();
	}


	public ClassInfo getClassInfo(InputStream pIn) {
		final ClassNode classNode = new ClassNode(Opcodes.ASM6);
		try {
			final ClassReader cr = new ClassReader(pIn);
			cr.accept(classNode, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
			final Type type = Type.getObjectType(classNode.name);
			String resourceName = null;
			String resourceDescription = null;
			String resourceVersion = null;
			final Map<String,String> attributes = new HashMap<>();
			for (AnnotationNode an : classNode.visibleAnnotations) {
				final String anClass = getExternalName(an.desc);
				if (IcmChange.class.getName().equals(anClass)) {
					final List<Object> values = an.values;
					for (int i = 0;  i < values.size();  i += 2) {
						final String attrName = (String) values.get(i);
						final Object attrValue = values.get(i+1);
						switch (attrName) {
						  case "name":
							 resourceName = (String) attrValue;
							 break;
						  case "description":
							 resourceDescription = (String) attrValue;
							 break;
						  case "version":
							 resourceVersion = (String) attrValue;
							 break;
						  case "attributes":
							 @SuppressWarnings("unchecked")
							 final List<Object> attributeNodes = (List<Object>) attrValue;
							 for (Object o : attributeNodes) {
								 final AnnotationNode attrNode = (AnnotationNode) o;
								 final String attrNodeClass = getExternalName(attrNode.desc);
								 if (Attribute.class.getName().equals(attrNodeClass)) {
									 String atName = null;
									 String atValue = null;
									 final List<Object> attrValues = attrNode.values;
									 for (int j = 0;  j < attrValues.size();  j += 2) {
										 final String n = (String) attrValues.get(j);
										 switch (n) {
										   case "name":
											  atName = (String) attrValues.get(j+1);
											  break;
										   case "value":
											   atValue = (String) attrValues.get(j+1);
											   break;
										   default:
											   throw new IllegalStateException("Invalid annotation attribute: " + n);
										 }
									 }
									 if (atName != null  &&  atValue != null) {
										 attributes.put(atName, atValue);
									 } else {
										 throw new IllegalStateException("Expected annotation attribute missing.");
									 }
								 } else {
									 throw new IllegalStateException("Invalid attribute type: " + attrNodeClass);
								 }
							 }
							 break;
						   default:
							   throw new IllegalStateException("Invalid annotation attribute: " + attrName);

						}
					}
					break;
				}
			}
			final String resName = resourceName;
			final String resDescription = resourceDescription;
			final String resVersion = resourceVersion;
			return new ClassInfo() {
				@Override
				public String getQName() {
					return type.getClassName();
				}

				@Override
				public String getSimpleName() {
					final String name = getQName();
					final int dotOffset = name.lastIndexOf('.');
					final String nm;
					if (dotOffset == -1) {
						nm = name;
					} else {
						nm = name.substring(dotOffset+1);
					}
					final int dollarOffset = nm.lastIndexOf('$');
					if (dollarOffset == -1) {
						return nm;
					} else {
						return nm.substring(dollarOffset+1);
					}
				}

				@Override
				public Map<String, String> getAttributes() {
					return attributes;
				}

				@Override
				public String getResourceName() {
					return resName;
				}

				@Override
				public String getResourceType() {
					return "class:" + getQName();
				}

				@Override
				public String getResourceDescription() {
					return resDescription;
				}
				@Override
				public String getResourceVersion() {
					return resVersion;
				}
			};
		} catch (Throwable t) {
			return null;
		}
	}

	protected String getExternalName(String pInternalName) {
		final String s = Type.getObjectType(pInternalName).getClassName();
		if (s.startsWith("L")  &&  s.endsWith(";")) {
			return s.substring(1, s.length()-1);
		} else {
			return s;
		}
	}
}
