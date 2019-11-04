package org.pcm.headless.core;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.core.simulator.IHeadlessSimulator;
import org.pcm.headless.core.simulator.ISimulationResults;
import org.pcm.headless.core.simulator.RepetitionData;
import org.pcm.headless.core.simulator.simucom.SimuComHeadlessSimulator;
import org.pcm.headless.core.simulator.simulizar.SimuLizarHeadlessSimulator;
import org.pcm.headless.core.util.PCMUtil;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Lists;

import lombok.extern.java.Log;

@Log
public class HeadlessPalladioSimulator {

	private static boolean PCM_INITIALIZED = false;

	public HeadlessPalladioSimulator() {
		if (!PCM_INITIALIZED) {
			log.info("Initializing packages and loading PCM default models.");
			PCMUtil.loadPCMModels();
			PCM_INITIALIZED = true;
		}
	}

	public void triggerSimulation(HeadlessModelConfig modelConfig, HeadlessSimulationConfig simulationConfig) {
		this.triggerSimulation(modelConfig, simulationConfig, null, null);
	}

	public void triggerSimulation(HeadlessModelConfig modelConfig, HeadlessSimulationConfig simulationConfig,
			ScheduledExecutorService service) {
		this.triggerSimulation(modelConfig, simulationConfig, null, service);
	}

	/**
	 * This method is used to trigger a simulation with a certain configuration.
	 * Please be aware that the output of this method needs to be treated carefully.
	 * All handles need to be closed manually and the repository needs to be removed
	 * from the central repository by hand. If you do not know how to handle this
	 * please consult
	 * {@link HeadlessPalladioSimulator#triggerSimulation(HeadlessModelConfig, HeadlessSimulationConfig)}
	 * or consider using it.
	 * 
	 * @param modelConfig      the configuration which holds the paths to the models
	 * @param simulationConfig the configuration which specifies the properties of
	 *                         the simulation
	 * @param listeners
	 * @return {@link LocalMemoryRepository} which contains the simulation data
	 */
	public void triggerSimulation(HeadlessModelConfig modelConfig, HeadlessSimulationConfig simulationConfig,
			List<ISimulationProgressListener> listeners, ScheduledExecutorService service) {
		// 1. create belonging simulator
		IHeadlessSimulator simulator = createSimulatorFromType(simulationConfig.getType());
		simulator.initialize(modelConfig, simulationConfig);

		// 2. once execution
		simulator.onceBefore();

		// 3. add own listener
		final List<ISimulationProgressListener> listenersCopy = listeners;

		ISimulationProgressListener internListener = new ISimulationProgressListener() {
			private volatile int reps = 0;
			private final int freps = simulationConfig.getRepetitions();

			@Override
			public synchronized void finishedRepetition() {
				reps++;
				if (reps == freps) {
					// 4. once after
					simulator.onceAfter();

					// 5 inform
					listenersCopy.forEach(l -> {
						l.finished(simulator.getResults());
					});
				}
			}

			@Override
			public void finished(ISimulationResults results) {
			}
		};

		if (listeners == null) {
			listeners = Lists.newArrayList(internListener);
		} else {
			listeners.add(internListener);
		}

		if (!simulationConfig.isParallelizeRepetitions()) {
			// no parallel execution
			for (int i = 0; i < simulationConfig.getRepetitions(); i++) {
				internSimulationProcess(simulator, listeners);
			}
		} else {
			// use scheduler
			for (int i = 0; i < simulationConfig.getRepetitions(); i++) {
				service.submit(() -> {
					try {
						internSimulationProcess(simulator, listenersCopy);
					} catch (Exception e) {
						// we do not want the thread to die
						listenersCopy.forEach(l -> l.finishedRepetition()); // otherwise we do not get the results
					}
				});
			}
		}
	}

	private void internSimulationProcess(IHeadlessSimulator simulator, List<ISimulationProgressListener> listeners) {
		// 3.1. prepare
		RepetitionData data = simulator.beforeRepetition();

		// 3.2. execute
		simulator.executeRepetition(data);

		// 3.3. inform
		if (listeners != null) {
			listeners.forEach(l -> l.finishedRepetition());
		}

		// 3.4. after rep
		simulator.afterRepetition(data);
	}

	private IHeadlessSimulator createSimulatorFromType(ESimulationType type) {
		switch (type) {
		case SIMUCOM:
			return new SimuComHeadlessSimulator();
		case SIMULIZAR:
			return new SimuLizarHeadlessSimulator();
		default:
			log.warning("Could not create the appropriate simulator.");
			return null;
		}
	}

}
