package org.pcm.headless.core.simulator.simulizar;

import java.util.HashMap;
import java.util.Map;

import org.palladiosimulator.simulizar.access.ModelAccess;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;
import org.palladiosimulator.simulizar.runtimestate.SimuLizarRuntimeState;
import org.palladiosimulator.simulizar.runtimestate.SimulationCancelationDelegate;
import org.pcm.headless.core.proxy.ResourceContainerFactoryProxy;
import org.pcm.headless.core.simulator.AbstractHeadlessSimulator;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class SimuLizarHeadlessSimulator extends AbstractHeadlessSimulator {

	private SimuLizarRuntimeState runtimeState;

	@Override
	public void prepareRepetition() {
		// if parallel create new to avoid concurrent modifications
		if (simulationConfig.isParallelizeRepetitions()) {
			this.recreateData();
		}

		// create runtime state
		runtimeState = buildSimulizarConfiguration(this.blackboard, configurationMap);

		// initialize it
		synchronized (SimuLizarHeadlessSimulator.this) {
			runtimeState.getModel().initialiseResourceContainer(
					new ResourceContainerFactoryProxy(pcmPartition.getResourceEnvironment()));
		}
	}

	@Override
	public void executeRepetition() {
		runtimeState.runSimulation();
		runtimeState.cleanUp();
	}

	private synchronized SimuLizarRuntimeState buildSimulizarConfiguration(MDSDBlackboard blackboard,
			Map<String, Object> configMap) {
		SimuLizarWorkflowConfiguration config = new SimuLizarWorkflowConfiguration(new HashMap<>());

		SimuComConfig simuconfig = new SimuComConfig(configMap, false);
		config.setSimuComConfiguration(simuconfig);

		return new SimuLizarRuntimeState(config, new ModelAccess(blackboard),
				new SimulationCancelationDelegate(() -> false));
	}

}
