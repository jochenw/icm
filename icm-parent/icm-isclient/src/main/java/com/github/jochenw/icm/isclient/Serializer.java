package com.github.jochenw.icm.isclient;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;
import com.wm.util.Values;
import com.wm.util.coder.XMLCoder;

public class Serializer {
	public static void main(String[] pArgs) throws Exception {
		final IData data = IDataFactory.create();
		final IDataCursor crsr = data.getCursor();
		IDataUtil.put(crsr, "boolean", Boolean.valueOf(false));
		IDataUtil.put(crsr, "integer", Integer.valueOf(1));
		IDataUtil.put(crsr, "long", Long.valueOf(1));
		IDataUtil.put(crsr, "short", Short.valueOf((short) 1));
		IDataUtil.put(crsr, "byte", Byte.valueOf((byte) 1));
		IDataUtil.put(crsr, "double", Double.valueOf(1.0));
		IDataUtil.put(crsr, "float", Float.valueOf((float) 1.0));
		IDataUtil.put(crsr, "stringList", new String[] {"foo", "bar"});
		IDataUtil.put(crsr, "documentList", new IData[0]);
		IDataUtil.put(crsr, "objectList", new Object[] {"foo", "bar"});
		crsr.destroy();

		final XMLCoder xmlCoder = new XMLCoder(true);
		xmlCoder.encode(System.out, Values.use(data));
	}
}
