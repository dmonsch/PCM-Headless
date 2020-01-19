package org.pcm.headless.api;

import java.io.File;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.headless.api.client.PCMHeadlessClient;
import org.pcm.headless.api.client.SimulationClient;
import org.pcm.headless.api.client.measure.MonitorRepositoryBuilderUtil;
import org.pcm.headless.api.client.transform.TransitiveModelTransformerUtil;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Lists;

public class HeadlessClientTestCoCoMEExtended {

	public static void main(String[] args) {
		PCMHeadlessClient client = new PCMHeadlessClient("http://127.0.0.1:8080/");
		if (client.isReachable(3000)) {
			java.lang.System.out.println("Backend erreichbar.");

			File allocationFile = new File("examples/cocome/cocome.allocation");
			File monitorRepositoryFile = new File("examples/cocome/cocome_gen.monitorrepository");
			File repositoryFile = new File("examples/cocome/cocome.repository");
			File resourceEnvironmentFile = new File("examples/cocome/cocome.resourceenvironment");
			File systemFile = new File("examples/cocome/cocome.system");
			File usageFile = new File("examples/cocome/cocome.usagemodel");

			MonitorRepositoryBuilderUtil monitorBuilder = new MonitorRepositoryBuilderUtil(repositoryFile, systemFile,
					usageFile);
			monitorBuilder.monitorExternalCalls().monitorUsageScenarios();
			monitorBuilder.saveToFile(monitorRepositoryFile, new File("examples/cocome/cocome_gen.measuringpoint"));

			org.palladiosimulator.pcm.system.System system = ModelUtil.readFromFile(systemFile.getAbsolutePath(),
					org.palladiosimulator.pcm.system.System.class);

			SimulationClient sim = client.prepareSimulation();

			Allocation alloc = ModelUtil.readFromFile(allocationFile.getAbsolutePath(), Allocation.class);
			Repository repo = ModelUtil.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
			org.palladiosimulator.pcm.system.System sys = ModelUtil.readFromFile(systemFile.getAbsolutePath(),
					org.palladiosimulator.pcm.system.System.class);
			UsageModel usage = ModelUtil.readFromFile(usageFile.getAbsolutePath(), UsageModel.class);
			ResourceEnvironment env = ModelUtil.readFromFile(resourceEnvironmentFile.getAbsolutePath(),
					ResourceEnvironment.class);

			// copy it
			List<EObject> old = Lists.newArrayList(alloc, repo, env, usage, sys);
			TransitiveModelTransformerUtil util = new TransitiveModelTransformerUtil();
			List<EObject> copies = util.copyObjects(old);
			util.relinkObjects(copies);

			Allocation allocationCopy = copies.stream().filter(f -> f instanceof Allocation).map(Allocation.class::cast)
					.findFirst().get();
			Repository repositoryCopy = copies.stream().filter(f -> f instanceof Repository).map(Repository.class::cast)
					.findFirst().get();
			UsageModel usageCopy = copies.stream().filter(f -> f instanceof UsageModel).map(UsageModel.class::cast)
					.findFirst().get();
			ResourceEnvironment resourceEnvironmentCopy = copies.stream().filter(f -> f instanceof ResourceEnvironment)
					.map(ResourceEnvironment.class::cast).findFirst().get();
			System systemCopy = copies.stream().filter(f -> f instanceof System).map(System.class::cast).findFirst()
					.get();

			sim.setAllocation(allocationCopy);
			sim.setRepository(repositoryCopy);
			sim.setSystem(systemCopy);
			sim.setUsageModel(usageCopy);
			sim.setResourceEnvironment(resourceEnvironmentCopy);
			sim.setMonitorRepository(monitorBuilder.getModel());

			sim.setSimulationConfig(HeadlessSimulationConfig.builder().type(ESimulationType.SIMUCOM).repetitions(1)
					.simulationTime(500000).maximumMeasurementCount(30000).experimentName("CoCoME Simulation").build());

			sim.createTransitiveClosure();
			sim.sync();

			long simStart = java.lang.System.currentTimeMillis();
			boolean success = sim.executeSimulation(res -> {
				java.lang.System.out.println(res.getValues().size());
				java.lang.System.out
						.println("Simulation needed " + (java.lang.System.currentTimeMillis() - simStart) + "ms");
			});

			ModelUtil.saveToFile(repositoryCopy, repositoryFile);
			ModelUtil.saveToFile(resourceEnvironmentCopy, resourceEnvironmentFile);
			ModelUtil.saveToFile(systemCopy, systemFile);
			ModelUtil.saveToFile(allocationCopy, allocationFile);
			ModelUtil.saveToFile(usageCopy, usageFile);
		}
	}

}