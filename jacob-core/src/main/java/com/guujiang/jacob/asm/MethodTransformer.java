package com.guujiang.jacob.asm;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class MethodTransformer {

	private ClassLoader loader;
	private ClassNode outerClassNode;
	private MethodNode methodNode;

	private boolean isStatic;

	public MethodTransformer(ClassLoader loader, ClassNode outerClassNode, MethodNode methodNode) {
		this.loader = loader;
		this.outerClassNode = outerClassNode;
		this.methodNode = methodNode;
		isStatic = (methodNode.access & ACC_STATIC) != 0;
	}

	public void transform() throws AnalyzerException {
		loadClass(new IteratorGenerator(outerClassNode, methodNode).generate());
		loadClass(new IterableGenerator(outerClassNode, methodNode).generate());

		String iterableClassName = outerClassNode.name + "$" + methodNode.name + "$Iterable";

		Type[] arguments = Type.getArgumentTypes(methodNode.desc);

		InsnList insts = methodNode.instructions;
		insts.clear();

		LabelNode start = new LabelNode();
		insts.add(start);

		insts.add(new TypeInsnNode(NEW, iterableClassName));
		insts.add(new InsnNode(DUP));
		
		int offset = 0;
		if (!isStatic) {
			insts.add(new VarInsnNode(ALOAD, 0));
			offset = 1;
		}
		for (int i = 0; i < arguments.length; ++i) {
			insts.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), i + offset));
		}
		StringBuilder desc = new StringBuilder();
		desc.append("(");
		if (!isStatic) {
			desc.append('L');
			desc.append(outerClassNode.name);
			desc.append(';');
		}
		for (Type t : arguments) {
			desc.append(t.getDescriptor());
		}
		desc.append(")V");
		insts.add(new MethodInsnNode(INVOKESPECIAL, iterableClassName, "<init>", desc.toString(), false));
		insts.add(new InsnNode(ARETURN));

		LabelNode end = new LabelNode();
		insts.add(end);

		List<LocalVariableNode> args = new ArrayList<>(methodNode.localVariables);
		methodNode.localVariables.clear();
		if (!isStatic) {
			methodNode.localVariables
					.add(new LocalVariableNode("this", "L" + outerClassNode.name + ";", null, start, end, 0));
		}
		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = args.get(i + offset);
			methodNode.localVariables.add(new LocalVariableNode(var.name, var.desc, var.signature, start, end, i + offset));
		}
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
