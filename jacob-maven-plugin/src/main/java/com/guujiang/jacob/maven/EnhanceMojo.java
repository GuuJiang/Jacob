package com.guujiang.jacob.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.guujiang.jacob.agent.IntermediateClassProcessor;
import com.guujiang.jacob.asm.MethodTransformer;

@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnhanceMojo extends AbstractMojo implements IntermediateClassProcessor {
	private final static String CLASS_ANNOTATION_DESC = "Lcom/guujiang/jacob/annotation/Generator;";
	private final static String METHOD_ANNOTATION_DESC = "Lcom/guujiang/jacob/annotation/GeneratorMethod;";

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	private File tempDir;

	public void execute() throws MojoExecutionException {
		Build build = project.getBuild();

		tempDir = new File(build.getDirectory(), "jacob-enhanced");

		try {
			tempDir.mkdirs();
			FileUtils.cleanDirectory(tempDir);
		} catch (IOException e) {
			throw new MojoExecutionException("unable to clean temp directory", e);
		}

		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
		try {
			Files.walk(Paths.get(build.getOutputDirectory()))
				.filter(Files::isRegularFile)
				.filter(f -> matcher.matches(f))
				.forEach(this::enhance);
		} catch (Exception e) {
			throw new MojoExecutionException("unable to enhance class", e);
		}
		
		try {
			FileUtils.copyDirectory(tempDir, new File(build.getOutputDirectory()));
		} catch (IOException e) {
			throw new MojoExecutionException("failed to write generated classes back", e);
		}
	}

	private void enhance(Path path) {
		try {
			ClassReader reader = new ClassReader(FileUtils.readFileToByteArray(path.toFile()));
			ClassNode clsNode = new ClassNode();
			reader.accept(clsNode, 0);

			if (!checkClass(clsNode)) {
				return;
			}

			boolean transformed = false;

			for (MethodNode method : clsNode.methods) {
				if (checkMethod(method)) {
					new MethodTransformer(clsNode, method, this).transform();
					transformed = true;
				}
			}

			if (transformed) {
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
				clsNode.accept(cw);
				
				storeGeneratedClass(clsNode.name, cw.toByteArray());
			}
		} catch (Exception e) {
			getLog().error("unable to process " + path, e);
		}
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
	
	private void storeGeneratedClass(String className, byte[] bytes) {
		File file = new File(tempDir, className + ".class");
		try {
			FileUtils.writeByteArrayToFile(file, bytes);
		} catch (IOException e) {
			getLog().error("failed to write generated class", e);
		}
	}

	@Override
	public void process(String className, byte[] bytes) {
		storeGeneratedClass(className, bytes);
	}
}
