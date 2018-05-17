package com.guujiang.jacob.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import com.guujiang.jacob.asm.analyze.FullInterpreter;
import com.guujiang.jacob.asm.analyze.FullValue;

public class IteratorGenerator {
	private ClassNode outerClassNode;
	private MethodNode methodNode;

	private String outerClassSignature;

	private List<LabelNode> exits;

	private String className;
	private ClassNode classNode;

	private Type[] arguments;

	private Set<FieldNode> fields = new HashSet<>();

	public IteratorGenerator(ClassNode outClassNode, MethodNode methodNode) {
		this.outerClassNode = outClassNode;
		this.methodNode = methodNode;
	}

	public byte[] generate() throws AnalyzerException {

		exits = new ArrayList<>();
		arguments = Type.getArgumentTypes(methodNode.desc);

		generateClass();
		generateConstructor();
		generateNextMethod();
		generateField();
		generateHasNextMethod();

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(cw);

		return cw.toByteArray();
	}

	private void generateClass() {
		className = outerClassNode.name + "$" + methodNode.name + "$Iterator";
		outerClassSignature = "L" + outerClassNode.name + ";";

		classNode = new ClassNode();
		classNode.version = outerClassNode.version;
		classNode.access = ACC_PUBLIC;
		classNode.name = className;
		classNode.superName = "java/lang/Object";
		classNode.interfaces.add("java/util/Iterator");

		classNode.outerClass = outerClassNode.name;
		classNode.visitInnerClass(className, outerClassNode.name, methodNode.name + "$Iterator", ACC_PUBLIC);
	}

	private void generateField() {
		classNode.fields.add(new FieldNode(ACC_FINAL | ACC_SYNTHETIC, "this$0", outerClassSignature, null, null));
		classNode.fields.add(new FieldNode(ACC_PRIVATE, "state$", "I", null, null));
		for (FieldNode field : fields) {
			classNode.fields.add(field);
		}
	}

	private void generateConstructor() {
		StringBuilder methodDesc = new StringBuilder();
		methodDesc.append('(');
		methodDesc.append(outerClassSignature);
		for (Type t : arguments) {
			methodDesc.append(t.getDescriptor());
		}
		methodDesc.append(")V");
		MethodNode method = new MethodNode(ACC_PUBLIC, "<init>", methodDesc.toString(), null, null);
		InsnList insts = method.instructions;
		LabelNode start = new LabelNode();
		insts.add(start);
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new VarInsnNode(ALOAD, 1));
		insts.add(new FieldInsnNode(PUTFIELD, className, "this$0", outerClassSignature));
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
		for (int i = 0; i < arguments.length; ++i) {
			Type type = arguments[i];
			FieldNode argField = new FieldNode(ACC_PRIVATE, "a" + i, type.getDescriptor(), null, null);
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), i + 2));
			insts.add(new FieldInsnNode(PUTFIELD, className, argField.name, argField.desc));
			
			fields.add(argField);
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

	private void generateHasNextMethod() {
		MethodNode method = new MethodNode(ACC_PUBLIC, "hasNext", "()Z", null, null);
		InsnList insts = method.instructions;
		LabelNode start = new LabelNode();
		insts.add(start);
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new FieldInsnNode(GETFIELD, className, "state$", "I"));
		insts.add(constant(exits.size() - 1));
		LabelNode returnFalse = new LabelNode();
		insts.add(new JumpInsnNode(IF_ICMPGT, returnFalse));
		insts.add(new InsnNode(ICONST_1));
		insts.add(new InsnNode(IRETURN));
		insts.add(returnFalse);
		insts.add(new InsnNode(ICONST_0));
		insts.add(new InsnNode(IRETURN));
		LabelNode end = new LabelNode();
		insts.add(end);

		method.localVariables.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, 0));

		classNode.methods.add(method);
	}

	private void generateNextMethod() throws AnalyzerException {
		MethodNode method = new MethodNode(ACC_PUBLIC, "next", "()Ljava/lang/Object;", null, null);
		InsnList insts = method.instructions;

		LabelNode start = new LabelNode();
		insts.add(start);

		for (int i = 0; i < arguments.length; ++i) {
			Type type = arguments[i];
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new FieldInsnNode(GETFIELD, className, "a" + i, type.getDescriptor()));
			insts.add(new VarInsnNode(type.getOpcode(ISTORE), i + 1));
		}
		Iterator<AbstractInsnNode> iter = methodNode.instructions.iterator();

		Frame<FullValue>[] frames = new Analyzer<FullValue>(new FullInterpreter()).analyze(outerClassNode.name,
				methodNode);
		int index = -1;
		while (iter.hasNext()) {
			index++;

			AbstractInsnNode ins = iter.next();
			int opcode = ins.getOpcode();

			if (ins instanceof VarInsnNode) {
				VarInsnNode vins = (VarInsnNode) ins;
				if (vins.getOpcode() == ALOAD && vins.var == 0) {
					insts.add(ins);
					insts.add(new FieldInsnNode(GETFIELD, className, "this$0", outerClassSignature));
					continue;
				}
			}

			if (ins instanceof MethodInsnNode && "yield".equals(((MethodInsnNode) ins).name)) {
				yieldReturn(insts, frames[index]);
				continue;
			}

			if (opcode == ARETURN) {
				continue;
			}

			insts.add(ins);
		}

		yieldReturn(insts, null);
		insts.add(new TypeInsnNode(NEW, "java/util/NoSuchElementException"));
		insts.add(new InsnNode(DUP));
		insts.add(new MethodInsnNode(INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "()V", false));
		insts.add(new InsnNode(ATHROW));

		LabelNode end = new LabelNode();
		insts.add(end);

		// TODO use a bunch of if instead of a switch table since switch table
		// will blow up frame calculation
		InsnList transTable = new InsnList();
		for (int i = 0; i < exits.size(); ++i) {
			transTable.add(new VarInsnNode(ALOAD, 0));
			transTable.add(new FieldInsnNode(GETFIELD, className, "state$", "I"));
			transTable.add(constant(i + 1));
			transTable.add(new JumpInsnNode(IF_ICMPEQ, exits.get(i)));
		}

		insts.insert(start, transTable);

		for (LocalVariableNode var : methodNode.localVariables) {
			if ("this".equals(var.name)) {
				var.desc = "L" + className + ";";
				var.start = start;
				var.end = end;
			}
			method.localVariables.add(var);
		}

		classNode.methods.add(method);
	}

	private AbstractInsnNode constant(int val) {
		if (val <= 5) {
			return new InsnNode(ICONST_0 + val);
		} else {
			return new VarInsnNode(BIPUSH, val);
		}
	}

	private void yieldReturn(InsnList insts, Frame<FullValue> frame) {
		LabelNode exit = new LabelNode();
		exits.add(exit);
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(constant(exits.size()));
		insts.add(new FieldInsnNode(PUTFIELD, className, "state$", "I"));

		String fieldPrefix = "l" + exits.size();

		List<FieldNode> fieldNodes = new ArrayList<>();
		if (frame != null) {
			for (int i = 1; i < frame.getLocals(); ++i) {
				FullValue local = frame.getLocal(i);
				if (local == FullValue.UNINITIALIZED_VALUE) {
					break;
				}
				FieldNode field = new FieldNode(ACC_PRIVATE, fieldPrefix + i, local.getType().getDescriptor(), null,
						null);
				insts.add(new VarInsnNode(ALOAD, 0));
				insts.add(new VarInsnNode(local.getType().getOpcode(ILOAD), i));
				insts.add(new FieldInsnNode(PUTFIELD, className, field.name, field.desc));

				fieldNodes.add(field);
			}
		}
		insts.add(new InsnNode(ARETURN));
		insts.add(exit);
		if (frame != null) {
			for (int i = 1; i < frame.getLocals(); ++i) {
				FullValue local = frame.getLocal(i);
				if (local == FullValue.UNINITIALIZED_VALUE) {
					break;
				}
				FieldNode field = fieldNodes.get(i - 1);
				insts.add(new VarInsnNode(ALOAD, 0));
				insts.add(new FieldInsnNode(GETFIELD, className, field.name, field.desc));
				insts.add(new VarInsnNode(local.getType().getOpcode(ISTORE), i));
			}
		}

		fields.addAll(fieldNodes);
	}
}
