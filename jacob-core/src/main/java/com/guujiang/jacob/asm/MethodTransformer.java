package com.guujiang.jacob.asm;

import static org.objectweb.asm.Opcodes.*;

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

import com.guujiang.jacob.agent.IntermediateClassProcessor;

public class MethodTransformer {

	private ClassNode outerClassNode;
	private MethodNode methodNode;
	private IntermediateClassProcessor processor;

	private boolean isStatic;

	public MethodTransformer(ClassNode outerClassNode, MethodNode methodNode, IntermediateClassProcessor processor) {
		this.outerClassNode = outerClassNode;
		this.methodNode = methodNode;
		this.processor = processor;
		isStatic = (methodNode.access & ACC_STATIC) != 0;
	}

	public void transform() throws AnalyzerException {
		new IteratorGenerator(outerClassNode, methodNode).generate(processor);
		new IterableGenerator(outerClassNode, methodNode).generate(processor);

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
			methodNode.localVariables
					.add(new LocalVariableNode(var.name, var.desc, var.signature, start, end, i + offset));
		}
	}

}
