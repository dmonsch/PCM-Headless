package org.pcm.headless.api;

import java.io.File;

import org.pcm.headless.api.client.PCMHeadlessClient;
import org.pcm.headless.api.client.SimulationClient;
import org.pcm.headless.api.client.measure.MonitorRepositoryBuilderUtil;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

public class HeadlessClientTestCoCoME {

	public static void main(String[] args) {
		PCMHeadlessClient client = new PCMHeadlessClient("http://127.0.0.1:8080/");
		if (client.isReachable(3000)) {
			System.out.println("Backend erreichbar.");

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

			SimulationClient sim = client.prepareSimulation();
			sim.setAllocation(allocationFile);
			sim.setRepository(repositoryFile);
			sim.setSystem(systemFile);
			sim.setUsageModel(usageFile);
			sim.setResourceEnvironment(resourceEnvironmentFile);
			sim.setMonitorRepository(monitorRepositoryFile);

			sim.setSimulationConfig(HeadlessSimulationConfig.builder().type(ESimulationType.SIMUCOM).repetitions(1)
					.simulationTime(500000).maximumMeasurementCount(30000).experimentName("CoCoME Simulation").build());

			sim.createTransitiveClosure();
			sim.sync();

			long simStart = System.currentTimeMillis();
			boolean success = sim.executeSimulation(res -> {
				System.out.println(res.getValues().size());
				System.out.println("Simulation needed " + (System.currentTimeMillis() - simStart) + "ms");
			});
		}
	}

}