package com.guujiang.jacob.agent;

import java.lang.instrument.Instrumentation;

public class Agent {
	public static void premain(String args, Instrumentation inst) {
		instrument(args, inst);
	}
	
	public static void agentmain(String args, Instrumentation inst) {
		instrument(args, inst);
	}
	
	static void instrument(String args, Instrumentation inst) {
		inst.addTransformer(new GeneratorClassTransformer());
	}
}
