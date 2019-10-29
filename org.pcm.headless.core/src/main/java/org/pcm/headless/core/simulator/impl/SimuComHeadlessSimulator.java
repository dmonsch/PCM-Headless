package org.pcm.headless.core.simulator.impl;

import org.pcm.headless.core.proxy.ResourceContainerFactoryProxy;
import org.pcm.headless.core.simulator.AbstractHeadlessSimulator;
import org.pcm.headless.core.simulator.util.SimuComModelFactory;

import de.uka.ipd.sdq.simucomframework.ExperimentRunner;
import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;

public class SimuComHeadlessSimulator extends AbstractHeadlessSimulator {

	private SimuComModel model;

	@Override
	public void prepareRepetition() {
		// if parallel create new to avoid concurrent modifications
		if (simulationConfig.isParallelizeRepetitions()) {
			this.recreateData();
		}

		// simucom
		SimuComConfig simuComConfig = new SimuComConfig(configurationMap, false);
		model = SimuComModelFactory.createSimuComModel(simuComConfig);

		// initialize it
		synchronized (SimuComHeadlessSimulator.this) {
			model.initialiseResourceContainer(new ResourceContainerFactoryProxy(pcmPartition.getResourceEnvironment()));
		}
	}

	@Override
	public void executeRepetition() {
		ExperimentRunner.run(model);

		this.model.getProbeFrameworkContext().finish();
		this.model.getConfiguration().getRecorderConfigurationFactory().finalizeRecorderConfigurationFactory();
	}

}
