package org.pcm.headless.core.simulator.simucom;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.java.Log;

@Log
public class SimuComGradleProcessor {
	private static final Pattern PACKAGE_PATTERN = Pattern.compile("package (.*);");

	private static final String GRADLE_BUILD_RESOURCE = "/simucom/build.gradle";
	private static final String GRADLE_SETTINGS_RESOURCE = "/simucom/settings.gradle";
	private static final String DEPENDENCIES_RESOURCE = "/simucom/dependencies.zip";
	private static final String MODELS_RESOURCE = "/simucom/models.zip";
	private static final String JAVA_AGENT_RESOURCES = "/simucom/java/";
	private static final String MODELS_PATH = "src" + File.separator + "main" + File.separator + "resources";
	private static final String SOURCES_PATH = "src" + File.separator + "main" + File.separator + "java";

	private static String javaHomePath = null;

	public static void setJavaHomePath(String homePath) {
		javaHomePath = homePath;
	}

	private File projectBasePath;

	public SimuComGradleProcessor(File projectBasePath) {
		this.projectBasePath = projectBasePath;
	}

	public File buildProject() {
		return GradleJarBuilder.buildGradleProject(projectBasePath,
				javaHomePath == null ? Optional.empty() : Optional.of(javaHomePath));
	}

	public void createMetadataFiles() {
		File destinationBuildGradle = new File(projectBasePath, "build.gradle");
		File destinationSettingsGradle = new File(projectBasePath, "settings.gradle");
		File destionationModels = new File(projectBasePath, MODELS_PATH);
		destionationModels.mkdirs();

		try {
			Files.copy(SimuComHeadlessSimulator.class.getResourceAsStream(GRADLE_BUILD_RESOURCE),
					destinationBuildGradle.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(SimuComHeadlessSimulator.class.getResourceAsStream(GRADLE_SETTINGS_RESOURCE),
					destinationSettingsGradle.toPath(), StandardCopyOption.REPLACE_EXISTING);

			// extract repository to outside
			extractRepository(SimuComHeadlessSimulator.class.getResourceAsStream(DEPENDENCIES_RESOURCE),
					projectBasePath);
			extractRepository(SimuComHeadlessSimulator.class.getResourceAsStream(MODELS_RESOURCE), destionationModels);

			// add agent files dynamically
			addAgentFilesDynamically();
		} catch (IOException e) {
			log.warning("Failed to setup SimuCom gradle project properly.");
		}
	}

	private void addAgentFilesDynamically() throws IOException {
		PathMatchingResourceResolver resourceResolver = new PathMatchingResourceResolver();
		resourceResolver.getResourceFiles(JAVA_AGENT_RESOURCES).forEach(ja -> {
			InputStream resourceInputStream = getClass().getResourceAsStream(JAVA_AGENT_RESOURCES + ja);
			String packageName = getPackageName(resourceInputStream);
			String[] pckgSplit = packageName.split("\\.");
			String currentString = SOURCES_PATH;
			for (String pckgPart : pckgSplit) {
				currentString += File.separator + pckgPart;
			}

			String[] fileNameSplit = ja.split("\\.");
			String fileNameConcat = "";
			for (int i = 0; i < fileNameSplit.length - 1; i++) {
				fileNameConcat += fileNameSplit[i] + ".";
			}
			fileNameConcat = fileNameConcat.substring(0, fileNameConcat.length() - 1);

			currentString += File.separator + fileNameConcat;

			File destinationFile = new File(projectBasePath, currentString);
			destinationFile.mkdirs();
			try {
				resourceInputStream = getClass().getResourceAsStream(JAVA_AGENT_RESOURCES + ja);
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

}
