package com.guujiang.jacob.asm;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class IterableGenerator {
	private ClassNode outerClassNode;
	private MethodNode methodNode;

	private String outerClassSignature;

	private String className;
	private ClassNode classNode;

	private String iteratorClassName;

	private Type[] arguments;

	public IterableGenerator(ClassNode outClassNode, MethodNode methodNode) {
		this.outerClassNode = outClassNode;
		this.methodNode = methodNode;
	}

	public byte[] generate() {

		arguments = Type.getArgumentTypes(methodNode.desc);

		generateClass();
		generateFields();
		generateConstructor();
		generateIteratorMethod();

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(cw);
		return cw.toByteArray();
	}

	private void generateClass() {
		className = outerClassNode.name + "$" + methodNode.name + "$Iterable";
		iteratorClassName = outerClassNode.name + "$" + methodNode.name + "$Iterator";
		outerClassSignature = "L" + outerClassNode.name + ";";

		classNode = new ClassNode();
		classNode.version = outerClassNode.version;
		classNode.access = ACC_PUBLIC;
		classNode.name = className;
		classNode.superName = "java/lang/Object";
		classNode.interfaces.add("java/lang/Iterable");

		classNode.outerClass = outerClassNode.name;
		classNode.visitInnerClass(className, outerClassNode.name, methodNode.name + "$Iterable", ACC_PUBLIC);
	}

	private void generateFields() {
		classNode.fields.add(new FieldNode(ACC_FINAL | ACC_SYNTHETIC, "this$0", outerClassSignature, null, null));

		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = methodNode.localVariables.get(i + 1);
			classNode.fields.add(new FieldNode(ACC_PRIVATE, var.name, var.desc, var.signature, null));
		}
	}

	private void generateConstructor() {
		StringBuilder desc = new StringBuilder();
		desc.append('(');
		desc.append(outerClassSignature);
		for (Type t : arguments) {
			desc.append(t.getDescriptor());
		}
		desc.append(")V");
		MethodNode method = new MethodNode(ACC_PUBLIC, "<init>", desc.toString(), null, null);
		InsnList insts = method.instructions;
		LabelNode start = new LabelNode();
		insts.add(start);
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new VarInsnNode(ALOAD, 1));
		insts.add(new FieldInsnNode(PUTFIELD, className, "this$0", outerClassSignature));
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = methodNode.localVariables.get(i + 1);
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), i + 2));
			insts.add(new FieldInsnNode(PUTFIELD, className, var.name, var.desc));
		}
		insts.add(new InsnNode(RETURN));
		LabelNode end = new LabelNode();
		insts.add(end);

		method.localVariables.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, 0));
		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = methodNode.localVariables.get(i + 1);
			method.localVariables.add(new LocalVariableNode(var.name, var.desc, var.signature, start, end, i + 2));
		}

		classNode.methods.add(method);
	}

	private void generateIteratorMethod() {
		MethodNode method = new MethodNode(ACC_PUBLIC, "iterator", "()Ljava/util/Iterator;", null, null);
		InsnList insts = method.instructions;
		LabelNode start = new LabelNode();
		insts.add(start);

		insts.add(new TypeInsnNode(NEW, iteratorClassName));
		insts.add(new InsnNode(DUP));
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new FieldInsnNode(GETFIELD, className, "this$0", outerClassSignature));
		for (int i = 0; i < arguments.length; ++i) {
			LocalVariableNode var = methodNode.localVariables.get(i + 1);
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new FieldInsnNode(GETFIELD, className, var.name, var.desc));
		}

		StringBuilder desc = new StringBuilder();
		desc.append("(");
		desc.append(outerClassSignature);
		for (int i = 0; i < arguments.length; ++i) {
			desc.append(arguments[i].getDescriptor());
		}
		desc.append(")V");
		insts.add(new MethodInsnNode(INVOKESPECIAL, iteratorClassName, "<init>", desc.toString(), false));
		insts.add(new InsnNode(ARETURN));

		LabelNode end = new LabelNode();
		insts.add(end);

		method.localVariables.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, 0));

		classNode.methods.add(method);
	}
}
