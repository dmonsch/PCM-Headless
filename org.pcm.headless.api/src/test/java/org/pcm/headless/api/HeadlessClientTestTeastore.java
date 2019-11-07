package org.pcm.headless.api;

import java.io.File;

import org.pcm.headless.api.client.PCMHeadlessClient;
import org.pcm.headless.api.client.SimulationClient;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

public class HeadlessClientTestTeastore {

	public static void main(String[] args) {
		PCMHeadlessClient client = new PCMHeadlessClient("http://127.0.0.1:8080/");
		if (client.isReachable(3000)) {
			System.out.println("Backend erreichbar.");

			File allocationFile = new File("examples/teastore/teastore.allocation");
			File repositoryFile = new File("examples/teastore/teastore.repository");
			File monitorRepositoryFile = new File("examples/teastore/teastore.monitorrepository");
			File resourceEnvironmentFile = new File("examples/teastore/teastore.resourceenvironment");
			File systemFile = new File("examples/teastore/teastore.system");
			File usageFile = new File("examples/teastore/teastore.usagemodel");

			SimulationClient sim = client.prepareSimulation();
			sim.setAllocation(allocationFile);
			sim.setRepository(repositoryFile);
			sim.setSystem(systemFile);
			sim.setUsageModel(usageFile);
			sim.setResourceEnvironment(resourceEnvironmentFile);
			sim.setMonitorRepository(monitorRepositoryFile);

			sim.setSimulationConfig(HeadlessSimulationConfig.builder().type(ESimulationType.SIMUCOM)
					.experimentName("TeaStore Simulation").repetitions(10).build());

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
