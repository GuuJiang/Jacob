package com.guujiang.jacob;

public class Stub {
	public static <T> void yield(T val) {
		throw new IllegalStateException("The generator should be instrumented with agent");
	}
}
