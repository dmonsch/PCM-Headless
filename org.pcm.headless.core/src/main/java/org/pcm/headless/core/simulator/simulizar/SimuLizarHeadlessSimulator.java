package org.pcm.headless.core.simulator.simulizar;

import java.util.HashMap;
import java.util.Map;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.simulizar.access.ModelAccess;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;
import org.palladiosimulator.simulizar.runtimestate.SimuLizarRuntimeState;
import org.palladiosimulator.simulizar.runtimestate.SimulationCancelationDelegate;
import org.pcm.headless.core.proxy.ResourceContainerFactoryProxy;
import org.pcm.headless.core.simulator.AbstractHeadlessSimulator;
import org.pcm.headless.core.simulator.RepetitionData;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class SimuLizarHeadlessSimulator extends AbstractHeadlessSimulator {
	private LocalMemoryRepository onceRepository;
	private RepetitionData onceData;

	@Override
	public void onceBefore() {
		if (!simulationConfig.isParallelizeRepetitions()) {
			onceRepository = super.pullNewInMemoryRepository();
			onceData = super.pullNewRepetitionData(onceRepository);
		}
	}

	@Override
	public RepetitionData beforeRepetition() {
		if (!simulationConfig.isParallelizeRepetitions()) {
			return onceData;
		} else {
			LocalMemoryRepository temporaryRepo = super.pullNewInMemoryRepository();
			return super.pullNewRepetitionData(temporaryRepo);
		}
	}

	@Override
	public void executeRepetition(RepetitionData data) {
		// create runtime state
		SimuLizarRuntimeState runtimeState = buildSimulizarConfiguration(data.getBlackboard(),
				data.getConfigurationMap());

		// initialize it
		runtimeState.getModel().initialiseResourceContainer(
				new ResourceContainerFactoryProxy(data.getPcmPartition().getResourceEnvironment()));

		runtimeState.runSimulation();
		runtimeState.cleanUp();
	}

	@Override
	public void afterRepetition(RepetitionData data) {
	}

	@Override
	public void onceAfter() {
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
