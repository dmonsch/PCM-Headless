package org.pcm.headless.core.simulator.simucom;

import java.io.File;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.metricspec.MetricSpecPackage;
import org.pcm.headless.core.simulator.AbstractHeadlessSimulator;
import org.pcm.headless.core.simulator.RepetitionData;

import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuComWorkflowConfiguration;
import de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.DetermineFailureTypesJob;
import de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.XtendTransformPCMToCodeJob;
import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import lombok.extern.java.Log;

@Log
public class SimuComHeadlessSimulator extends AbstractHeadlessSimulator {
	private SimuComWorkflowConfiguration workflowConfiguration;

	private RepetitionData repData;
	private LocalMemoryRepository repository;

	private File resultingJar;

	@Override
	public RepetitionData beforeRepetition() {
		return repData;
	}

	@Override
	public void afterRepetition(RepetitionData data) {
		// nothing at all
	}

	@Override
	public void onceBefore() {
		// 0. warn for using parallelism
		if (simulationConfig.isParallelizeRepetitions()) {
			log.warning("Parallelizing simulation repetitions are not supported by the SimuCom simulator!");
		}

		// 0.1. init classes
		MetricSpecPackage.eINSTANCE.eClass();

		// 1. build config
		repository = super.pullNewInMemoryRepository();
		repData = super.pullNewRepetitionData(repository);

		SimuComConfig simuComConfig = new SimuComConfig(repData.getConfigurationMap(), false);
		workflowConfiguration = new SimuComWorkflowConfiguration(repData.getConfigurationMap());
		workflowConfiguration.setSimuComConfiguration(simuComConfig);

		// 2. transform models
		buildCodeUsingXtend(repData.getBlackboard());

		// 3. prepare & build gradle project
		File projectBasePath = new File(simulationConfig.getSimuComStoragePath());
		SimuComGradleProcessor gradleProcessor = new SimuComGradleProcessor(projectBasePath);
		gradleProcessor.createMetadataFiles();
		resultingJar = gradleProcessor.buildProject();
	}

	@Override
	public void onceAfter() {
	}

	@Override
	public void executeRepetition(RepetitionData data) {
		// reflective trigger the simulation
		ReflectiveSimulationInvoker invoker = new ReflectiveSimulationInvoker(resultingJar);
		invoker.invokeSimulation(data.getConfigurationMap());
		invoker.close();
	}

	private void buildCodeUsingXtend(MDSDBlackboard blackboard) {
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
			log.warning("Failed to generate code via SimuCom.");
		}
	}
}
