package com.guujiang.jacob.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import com.guujiang.jacob.asm.MethodTransformer;

public class GeneratorClassTransformer implements ClassFileTransformer {
	private final static String CLASS_ANNOTATION_DESC = "Lcom/guujiang/jacob/annotation/Generator;";
	private final static String METHOD_ANNOTATION_DESC = "Lcom/guujiang/jacob/annotation/GeneratorMethod;";

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassNode clsNode = new ClassNode();

		cr.accept(clsNode, 0);

		if (!checkClass(clsNode)) {
			return classfileBuffer;
		}

		boolean transformed = false;

		try {
			for (MethodNode method : clsNode.methods) {
				if (checkMethod(method)) {
					new MethodTransformer(clsNode, method, new LoadClassProcessor(loader)).transform();
					transformed = true;
				}
			}
		} catch (AnalyzerException e) {
			return classfileBuffer;
		}

		if (transformed) {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			clsNode.accept(cw);
			return cw.toByteArray();
		}
		return classfileBuffer;
	}

	private boolean checkClass(ClassNode classNode) {
		if (classNode.visibleAnnotations == null) {
			return false;
		}
		for (AnnotationNode anno : classNode.visibleAnnotations) {
			if (CLASS_ANNOTATION_DESC.equals(anno.desc)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkMethod(MethodNode methodNode) {
		if (methodNode.visibleAnnotations == null) {
			return false;
		}
		for (AnnotationNode anno : methodNode.visibleAnnotations) {
			if (METHOD_ANNOTATION_DESC.equals(anno.desc)) {
				return true;
			}
		}
		return false;
	}

	class LoadClassProcessor implements IntermediateClassProcessor {
		private ClassLoader loader;

		public LoadClassProcessor(ClassLoader loader) {
			this.loader = loader;
		}

		@Override
		public void process(String className, byte[] bytes) {
			loadClass(bytes);
		}

		private void loadClass(byte[] bytes) {
			try {
				Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class,
						int.class);
				m.setAccessible(true);
				m.invoke(loader, null, bytes, 0, bytes.length);
			} catch (Exception e) {
				throw new RuntimeException("Failed to load class with reflect", e);
			}
		}
	}

}
