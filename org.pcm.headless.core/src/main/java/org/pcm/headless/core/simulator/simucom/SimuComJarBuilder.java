package org.pcm.headless.core.simulator.simucom;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.common.collect.Lists;

public class SimuComJarBuilder {
	private static final Pattern PACKAGE_PATTERN = Pattern.compile("package (.*);");

	private static final String SOURCES_PATH = "src";
	private static final String COMPILE_OUTPUT_PATH = "output";
	private static final String JAVA_AGENT_RESOURCES = "/simucom/java/*";
	private static final String MODELS_RESOURCE = "/simucom/models.zip";
	private static final String SIMUCOM_DEPENDENCIES_RESOURCE = "/simucom/dependencies.zip";
	private static final String SIMUCON_DEPENDENCY_PATH = "simucomDependencies";

	private File projectBasePath;

	public SimuComJarBuilder(File rootPath) {
		this.projectBasePath = rootPath;
	}

	public File buildProject() {
		File sourceFolder = new File(projectBasePath, SOURCES_PATH);
		File outputFolder = new File(projectBasePath, COMPILE_OUTPUT_PATH);

		// 0. create folders
		outputFolder.mkdirs();
		sourceFolder.mkdirs();

		// 1. add agent files
		try {
			extractRepository(SimuComHeadlessSimulator.class.getResourceAsStream(MODELS_RESOURCE), outputFolder);
			addAgentFilesDynamically();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// 2. compile files
		List<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
		processPath(sourceFolder, javaFiles);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		List<String> optionList = new ArrayList<>();

		// 3. download dependencies for simucom
		File depsFolder = new File(SIMUCON_DEPENDENCY_PATH);
		if (!depsFolder.exists()) {
			depsFolder.mkdirs();
			try {
				File tempFile = File.createTempFile("simucomDeps", ".zip");

				extractRepository(SimuComHeadlessSimulator.class.getResourceAsStream(SIMUCOM_DEPENDENCIES_RESOURCE),
						depsFolder);

				tempFile.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		optionList.add("-classpath");
		StringBuilder classPathString = new StringBuilder();
		for (File depsFile : depsFolder.listFiles()) {
			classPathString.append(":");
			classPathString.append(depsFile.getAbsolutePath());
		}
		optionList.add(classPathString.substring(1));

		// 4. start compiling task
		try {
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Lists.newArrayList(outputFolder));

			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, optionList, null, javaFiles);
			boolean success = task.call();

			fileManager.close();

			if (success) {
				return outputFolder;
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void processPath(File rootPath, List<JavaFileObject> javaFiles) {
		Stream.of(rootPath.listFiles()).forEach(f -> {
			if (f.isDirectory()) {
				processPath(f, javaFiles);
			} else {
				if (f.getName().endsWith(".java")) {
					javaFiles.add(new SimpleJavaFileObjectImpl(f.toURI(), JavaFileObject.Kind.SOURCE));
				}
			}
		});
	}

	private void addAgentFilesDynamically() throws IOException {
		PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
		Stream.of(resourceResolver.getResources(JAVA_AGENT_RESOURCES)).forEach(ja -> {
			try {
				InputStream resourceInputStream = ja.getInputStream();
				String packageName = getPackageName(resourceInputStream);
				String[] pckgSplit = packageName.split("\\.");
				String currentString = SOURCES_PATH;
				for (String pckgPart : pckgSplit) {
					currentString += File.separator + pckgPart;
				}

				String[] fileNameSplit = ja.getFilename().split("\\.");
				String fileNameConcat = "";
				for (int i = 0; i < fileNameSplit.length - 1; i++) {
					fileNameConcat += fileNameSplit[i] + ".";
				}
				fileNameConcat = fileNameConcat.substring(0, fileNameConcat.length() - 1);

				currentString += File.separator + fileNameConcat;

				File destinationFile = new File(projectBasePath, currentString);
				destinationFile.mkdirs();
				resourceInputStream = ja.getInputStream();
				Files.copy(resourceInputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private String getPackageName(InputStream sourceFile) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceFile))) {
			String pckgLine = reader.readLine();
			Matcher pckgMatcher = PACKAGE_PATTERN.matcher(pckgLine);
			if (pckgMatcher.find()) {
				return pckgMatcher.group(1).trim();
			} else {
				return null;
			}
		} catch (IOException e) {
			return null;
		}
	}

	private void extractRepository(InputStream resource, File destDir) throws IOException {
		ZipInputStream zipIn = new ZipInputStream(resource);
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			File filePath = new File(destDir, entry.getName());
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				filePath.getParentFile().mkdirs();
				extractFile(zipIn, filePath);
			} else {
				// if the entry is a directory, make the directory
				filePath.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	private void extractFile(ZipInputStream zipIn, File filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[4096];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}

	private class SimpleJavaFileObjectImpl extends SimpleJavaFileObject {

		public SimpleJavaFileObjectImpl(URI uri, Kind kind) {
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
