package org.pcm.headless.core.simulator.simucom;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.pcm.headless.core.data.ReflectiveInMemoryRepositoryReader;
import org.pcm.headless.core.simulator.AbstractHeadlessSimulator;

import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuComWorkflowConfiguration;
import de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.DetermineFailureTypesJob;
import de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.XtendTransformPCMToCodeJob;
import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;

public class SimuComHeadlessSimulator extends AbstractHeadlessSimulator {
	private static final String GRADLE_BUILD_RESOURCE = "/simucom/build.gradle";
	private static final String GRADLE_SETTINGS_RESOURCE = "/simucom/settings.gradle";

	private static final String DEPENDENCIES_RESOURCE = "/simucom/dependencies.zip";
	private static final String MODELS_RESOURCE = "/simucom/models.zip";

	private static final String MODELS_PATH = "src" + File.separator + "main" + File.separator + "resources";

	private static final String INITIALIZER_PACKAGE_PATH = "agent" + File.separator + "main" + File.separator
			+ "Initializer.java";
	private static final String INITIALIZER_CODE_RESOURCE = "/simucom/Initializer.txt";

	private SimuComWorkflowConfiguration workflowConfiguration;

	@Override
	public void prepareRepetition() {
		// if parallel create new to avoid concurrent modifications
		if (simulationConfig.isParallelizeRepetitions()) {
			this.recreateData();
		}

		// simucom
		SimuComConfig simuComConfig = new SimuComConfig(configurationMap, false);
		workflowConfiguration = new SimuComWorkflowConfiguration(configurationMap);
		workflowConfiguration.setSimuComConfiguration(simuComConfig);
	}

	@Override
	public void executeRepetition() {
		// TODO only once this all
		// jobs
		DetermineFailureTypesJob job1 = new DetermineFailureTypesJob(workflowConfiguration);
		job1.setBlackboard(blackboard);

		// set path
		workflowConfiguration.getCodeGenerationRequiredBundles().add(simulationConfig.getSimuComStoragePath());

		XtendTransformPCMToCodeJob job2 = new XtendTransformPCMToCodeJob(workflowConfiguration);
		job2.setBlackboard(blackboard);

		IProgressMonitorMock mock = new IProgressMonitorMock();

		try {
			job1.execute(mock);
			job2.execute(mock);
		} catch (JobFailedException | UserCanceledException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to generate code via SimuCom.");
		}

		// copy build.gradle & settings.gradle to outside
		File projectBasePath = new File(simulationConfig.getSimuComStoragePath());
		File destinationBuildGradle = new File(projectBasePath, "build.gradle");
		File destinationSettingsGradle = new File(projectBasePath, "settings.gradle");
		File destinationInitializer = new File(projectBasePath,
				"src" + File.separator + "main" + File.separator + "java" + File.separator + INITIALIZER_PACKAGE_PATH);
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

			// add agent
			destinationInitializer.getParentFile().mkdirs();
			Files.copy(SimuComHeadlessSimulator.class.getResourceAsStream(INITIALIZER_CODE_RESOURCE),
					destinationInitializer.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// build gradle project
		File resultingJar = GradleJarBuilder.buildGradleProject(projectBasePath, Optional.empty());

		// reflective trigger the simulation
		ReflectiveSimulationInvoker invoker = new ReflectiveSimulationInvoker(resultingJar);
		invoker.invokeSimulation(configurationMap);

		// convert
		ReflectiveInMemoryRepositoryReader reader = new ReflectiveInMemoryRepositoryReader();
		reader.convertRepository(invoker.getResultRepository());

		invoker.close();
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

	private class IProgressMonitorMock implements IProgressMonitor {

		@Override
		public void beginTask(String arg0, int arg1) {
		}

		@Override
		public void done() {
		}

		@Override
		public void internalWorked(double arg0) {
		}

		@Override
		public boolean isCanceled() {
			return false;
		}

		@Override
		public void setCanceled(boolean arg0) {
		}

		@Override
		public void setTaskName(String arg0) {
		}

		@Override
		public void subTask(String arg0) {
		}

		@Override
		public void worked(int arg0) {
		}

	}
}
