package com.guujiang.jacob.asm.analyze;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public class FullValue implements Value {

	public static final FullValue UNINITIALIZED_VALUE = new FullValue(null);

	public static final FullValue INT_VALUE = new FullValue(Type.INT_TYPE);

	public static final FullValue FLOAT_VALUE = new FullValue(Type.FLOAT_TYPE);

	public static final FullValue LONG_VALUE = new FullValue(Type.LONG_TYPE);

	public static final FullValue DOUBLE_VALUE = new FullValue(Type.DOUBLE_TYPE);

	public static final FullValue RETURNADDRESS_VALUE = new FullValue(Type.VOID_TYPE);

	private final Type type;

	public FullValue(final Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public int getSize() {
		return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
	}

	public boolean isReference() {
		return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
	}

	@Override
	public boolean equals(final Object value) {
		if (value == this) {
			return true;
		} else if (value instanceof BasicValue) {
			if (type == null) {
				return ((FullValue) value).type == null;
			} else {
				return type.equals(((FullValue) value).type);
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return type == null ? 0 : type.hashCode();
	}

	@Override
	public String toString() {
		if (this == UNINITIALIZED_VALUE) {
			return ".";
		} else if (this == RETURNADDRESS_VALUE) {
			return "A";
		} else {
			return type.getDescriptor();
		}
	}
}
