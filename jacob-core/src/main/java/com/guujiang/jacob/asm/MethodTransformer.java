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

public class MethodTransformer {

	private ClassLoader loader;
	private ClassNode outerClassNode;
	private MethodNode methodNode;

	public MethodTransformer(ClassLoader loader, ClassNode outerClassNode, MethodNode methodNode) {
		this.loader = loader;
		this.outerClassNode = outerClassNode;
		this.methodNode = methodNode;
	}

	public void transform() {
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
		insts.add(new VarInsnNode(ALOAD, 0));
		for (int i = 0; i < arguments.length; ++i) {
			insts.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), i + 1));
		}
		StringBuilder desc = new StringBuilder();
		desc.append("(L");
		desc.append(outerClassNode.name);
		desc.append(';');
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
		methodNode.localVariables
				.add(new LocalVariableNode("this", "L" + outerClassNode.name + ";", null, start, end, 0));
		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = args.get(i + 1);
			methodNode.localVariables.add(new LocalVariableNode(var.name, var.desc, var.signature, start, end, i + 2));
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
