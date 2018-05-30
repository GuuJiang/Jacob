package com.guujiang.jacob.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
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

import com.guujiang.jacob.agent.IntermediateClassProcessor;
import com.guujiang.jacob.annotation.GeneratorMethod;
import com.guujiang.jacob.annotation.OverRangePolicy;
import com.guujiang.jacob.asm.analyze.FullInterpreter;
import com.guujiang.jacob.asm.analyze.FullValue;

public class IteratorGenerator {
	private ClassNode outerClassNode;
	private MethodNode methodNode;

	private String outerClassSignature;

	private List<LabelNode> exits;

	private String className;
	private ClassNode classNode;

	private Type[] argumentTypes;
	private LocalVariableNode[] arguments;

	private List<FieldNode> fields = new LinkedList<>();

	private boolean isStatic;
	private OverRangePolicy overRange = OverRangePolicy.ReturnNull;

	public IteratorGenerator(ClassNode outClassNode, MethodNode methodNode) {
		this.outerClassNode = outClassNode;
		this.methodNode = methodNode;
		isStatic = (methodNode.access & ACC_STATIC) != 0;

		String annotationDesc = Type.getDescriptor(GeneratorMethod.class);
		for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
			if (annotationDesc.equals(annotationNode.desc)) {
				annotationNode.accept(new AnnotationVisitor(ASM6) {
					@Override
					public void visitEnum(String name, String desc, String value) {
						if ("overIterate".equals(name)) {
							overRange = OverRangePolicy.valueOf(value);
						}
					}
				});
			}
		}
	}

	public void generate(IntermediateClassProcessor processor) throws AnalyzerException {
		exits = new ArrayList<>();
		argumentTypes = Type.getArgumentTypes(methodNode.desc);
		arguments = new LocalVariableNode[argumentTypes.length];
		
		LabelNode start = (LabelNode) methodNode.instructions.get(0);
		int offset = isStatic ? 0 : 1;
		for (LocalVariableNode var : methodNode.localVariables) {
			int idx = var.index - offset;
			if (var.start == start && idx >= 0 && idx < arguments.length) {
				arguments[idx] = var;
			}
		}

		generateClass();
		generateConstructor();
		generateNextMethod();
		generateHasNextMethod();
		generateField();

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(cw);
		
		processor.process(className, cw.toByteArray());
	}

	private void generateClass() {
		className = outerClassNode.name + "$" + methodNode.name + "$Iterator";
		outerClassSignature = "L" + outerClassNode.name + ";";

		classNode = new ClassNode();
		classNode.version = outerClassNode.version;
		classNode.access = ACC_PRIVATE;
		if (isStatic) {
			classNode.access |= ACC_STATIC;
		}
		classNode.name = className;
		classNode.superName = "java/lang/Object";
		classNode.interfaces.add("java/util/Iterator");

		classNode.outerClass = outerClassNode.name;
		classNode.visitInnerClass(className, outerClassNode.name, methodNode.name + "$Iterator", classNode.access);
	}

	private void generateField() {
		if (!isStatic) {
			classNode.fields.add(new FieldNode(ACC_FINAL | ACC_SYNTHETIC, "this$0", outerClassSignature, null, null));
		}
		classNode.fields.add(new FieldNode(ACC_PRIVATE, "state$", "I", null, null));
		for (FieldNode field : fields) {
			classNode.fields.add(field);
		}
	}

	private void generateConstructor() {
		StringBuilder methodDesc = new StringBuilder();
		methodDesc.append('(');
		if (!isStatic) {
			methodDesc.append(outerClassSignature);
		}
		for (Type t : argumentTypes) {
			methodDesc.append(t.getDescriptor());
		}
		methodDesc.append(")V");
		MethodNode method = new MethodNode(ACC_PUBLIC, "<init>", methodDesc.toString(), null, null);
		InsnList insts = method.instructions;
		LabelNode start = new LabelNode();
		insts.add(start);
		if (!isStatic) {
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new VarInsnNode(ALOAD, 1));
			insts.add(new FieldInsnNode(PUTFIELD, className, "this$0", outerClassSignature));
		}
		insts.add(new VarInsnNode(ALOAD, 0));
		insts.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));

		int offset = isStatic ? 1 : 2;
		for (int i = 0; i < argumentTypes.length; ++i) {
			Type type = argumentTypes[i];
			FieldNode argField = new FieldNode(ACC_PRIVATE, "a" + i, type.getDescriptor(), null, null);
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new VarInsnNode(argumentTypes[i].getOpcode(ILOAD), i + offset));
			insts.add(new FieldInsnNode(PUTFIELD, className, argField.name, argField.desc));

			fields.add(argField);
		}
		insts.add(new InsnNode(RETURN));
		LabelNode end = new LabelNode();
		insts.add(end);

		offset = isStatic ? 0 : 1;
		method.localVariables.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, 0));
		for (int i = 0; i < argumentTypes.length; ++i) {
			LocalVariableNode var = arguments[i];
			method.localVariables
					.add(new LocalVariableNode(var.name, var.desc, var.signature, start, end, i + offset + 1));
		}

		classNode.methods.add(method);
	}

	private void generateHasNextMethod() throws AnalyzerException {
		MethodNode method = new MethodNode(ACC_PUBLIC, "hasNext", "()Z", null, null);

		if (overRange == OverRangePolicy.PreFetch) {
			portGeneratorMethod(method);
		} else {
			int exitCount = exits.size();
			if (overRange == OverRangePolicy.ReturnNull) {
				exitCount--;
			}
			InsnList insts = method.instructions;
			LabelNode start = new LabelNode();
			insts.add(start);
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new FieldInsnNode(GETFIELD, className, "state$", "I"));
			insts.add(constant(exitCount));
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
		}

		classNode.methods.add(method);
	}

	private void generateNextMethod() throws AnalyzerException {
		MethodNode method = new MethodNode(ACC_PUBLIC, "next", "()Ljava/lang/Object;", null, null);

		if (overRange == OverRangePolicy.PreFetch) {
			fields.add(new FieldNode(ACC_PRIVATE, "current$", Type.getDescriptor(Object.class), null, null));
			InsnList insts = method.instructions;
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new FieldInsnNode(GETFIELD, className, "current$", Type.getDescriptor(Object.class)));
			insts.add(new InsnNode(ARETURN));
		} else {
			portGeneratorMethod(method);
		}

		classNode.methods.add(method);
	}

	private void portGeneratorMethod(MethodNode method) throws AnalyzerException {
		InsnList insts = method.instructions;

		LabelNode start = new LabelNode();
		insts.add(start);

		for (int i = 0; i < argumentTypes.length; ++i) {
			Type type = argumentTypes[i];
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
				if (isStatic) {
					vins.var += 1;
					insts.add(vins);
					continue;
				}
				if (vins.getOpcode() == ALOAD && vins.var == 0) {
					insts.add(ins);
					insts.add(new FieldInsnNode(GETFIELD, className, "this$0", outerClassSignature));
					continue;
				}
			}

			if (ins instanceof IincInsnNode) {
				if (isStatic) {
					((IincInsnNode) ins).var += 1;
				}
				insts.add(ins);
				continue;
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

		if (overRange == OverRangePolicy.PreFetch) {
			insts.add(new InsnNode(POP));
			insts.add(new InsnNode(ICONST_0));
			insts.add(new InsnNode(IRETURN));
		} else {
			if (overRange == OverRangePolicy.ReturnNull) {
				yieldReturn(insts, null);
			}
			insts.add(new TypeInsnNode(NEW, "java/util/NoSuchElementException"));
			insts.add(new InsnNode(DUP));
			insts.add(new MethodInsnNode(INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "()V", false));
			insts.add(new InsnNode(ATHROW));
		}

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

		if (isStatic) {
			method.localVariables.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, 0));
		}
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

		int localStart = isStatic ? 0 : 1;
		int offset = isStatic ? 1 : 0;
		List<FieldNode> fieldNodes = new ArrayList<>();
		if (frame != null) {
			for (int i = localStart; i < frame.getLocals(); ++i) {
				FullValue local = frame.getLocal(i);
				if (local == FullValue.UNINITIALIZED_VALUE) {
					break;
				}
				FieldNode field = new FieldNode(ACC_PRIVATE, fieldPrefix + i, local.getType().getDescriptor(), null,
						null);
				insts.add(new VarInsnNode(ALOAD, 0));
				insts.add(new VarInsnNode(local.getType().getOpcode(ILOAD), i + offset));
				insts.add(new FieldInsnNode(PUTFIELD, className, field.name, field.desc));

				fieldNodes.add(field);
			}
		}
		if (overRange == OverRangePolicy.PreFetch) {
			insts.add(new VarInsnNode(ALOAD, 0));
			insts.add(new InsnNode(SWAP));
			insts.add(new FieldInsnNode(PUTFIELD, className, "current$", Type.getDescriptor(Object.class)));
			insts.add(new InsnNode(ICONST_1));
			insts.add(new InsnNode(IRETURN));
		} else {
			insts.add(new InsnNode(ARETURN));
		}
		insts.add(exit);
		if (frame != null) {
			for (int i = localStart; i < frame.getLocals(); ++i) {
				FullValue local = frame.getLocal(i);
				if (local == FullValue.UNINITIALIZED_VALUE) {
					break;
				}
				FieldNode field = fieldNodes.get(i - localStart);
				insts.add(new VarInsnNode(ALOAD, 0));
				insts.add(new FieldInsnNode(GETFIELD, className, field.name, field.desc));
				insts.add(new VarInsnNode(local.getType().getOpcode(ISTORE), i + offset));
			}
		}

		fields.addAll(fieldNodes);
	}
}
