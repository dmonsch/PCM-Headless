package org.pcm.headless.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.google.common.collect.Lists;

public class CompilationTest {

	public static void main(String[] args) throws IOException {
		String path = "/Users/david/Desktop/PCM-Headless/git/org.pcm.headless.rest/SimulationData/7c4c0ebb-c5f4-478c-8bb8-53f8fbf75d36/simucom/src/main/java/";
		String outputPath = "/Users/david/Desktop/PCM-Headless/git/org.pcm.headless.rest/SimulationData/7c4c0ebb-c5f4-478c-8bb8-53f8fbf75d36/simucom/output/";
		File rootPath = new File(path);

		List<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		processPath(rootPath, javaFiles);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		// -- H E R E --//
		// Specify where to put the genereted .class files
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Lists.newArrayList(new File(outputPath)));

		JavaCompiler.CompilationTask task = ToolProvider.getSystemJavaCompiler().getTask(null, fileManager, diagnostics,
				null, null, javaFiles);
		boolean success = task.call();

		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			System.out.println(diagnostic.getCode());
			System.out.println(diagnostic.getKind());
			System.out.println(diagnostic.getPosition());
			System.out.println(diagnostic.getStartPosition());
			System.out.println(diagnostic.getEndPosition());
			System.out.println(diagnostic.getSource());
			System.out.println(diagnostic.getMessage(null));

		}
		System.out.println("Success: " + success);

	}

	private static void processPath(File rootPath, List<JavaFileObject> javaFiles) {
		Stream.of(rootPath.listFiles()).forEach(f -> {
			if (f.isDirectory()) {
				processPath(f, javaFiles);
			} else {
				if (f.getName().endsWith(".java")) {
					javaFiles.add(new EasyMock(f.toURI(), JavaFileObject.Kind.SOURCE));
				}
			}
		});
	}

	private static class EasyMock extends SimpleJavaFileObject {

		public EasyMock(URI uri, Kind kind) {
			super(uri, kind);
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			try {
				String ret = Files.readString(new File(this.uri).toPath(), StandardCharsets.UTF_8);
				return ret;
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
		}

	}

}
