package org.pcm.headless.core;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.core.simulator.ISimulationResults;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Lists;

public class HeadlessSimulatorTest {

	public static void main(String[] args) {
		HeadlessModelConfig conf1 = new HeadlessModelConfig();
		conf1.setAllocationFile(new File("examples/cocome/cocome.allocation"));
		conf1.setMonitorRepository(new File("examples/cocome/cocome.monitorrepository"));
		conf1.setRepositoryFiles(Lists.newArrayList(new File("examples/cocome/cocome.repository"),
				new File("examples/cocome/cocomeTypes.repository")));
		conf1.setResourceEnvironmentFile(new File("examples/cocome/cocome.resourceenvironment"));
		conf1.setSystemFile(new File("examples/cocome/cocome.system"));
		conf1.setUsageFile(new File("examples/cocome/cocome.usagemodel"));
		conf1.setAdditionals(Lists.newArrayList(new File("examples/cocome/cocome.measuringpoint")));

		HeadlessSimulationConfig conf2 = HeadlessSimulationConfig.builder().repetitions(10)
				.parallelizeRepetitions(false).type(ESimulationType.SIMUCOM).build();
		conf2.setSimuComStoragePath("simucom");

		HeadlessPalladioSimulator sim = new HeadlessPalladioSimulator();
		ScheduledExecutorService execServ = Executors.newScheduledThreadPool(4);
		sim.triggerSimulation(conf1, conf2, Lists.newArrayList(new ISimulationProgressListener() {
			@Override
			public void finishedRepetition() {
				System.out.println("TEST");
			}

			@Override
			public void finished(ISimulationResults results) {
				System.out.println(results.getConvertedRepository().getValues().size());
			}
		}), execServ);

		execServ.shutdown();
		try {
			execServ.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.exit(0);

	}

}
