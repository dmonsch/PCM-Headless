package org.pcm.headless.core;

import java.util.List;
import java.util.stream.IntStream;

import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.pcm.headless.core.data.InMemoryRepositoryReader;
import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.core.simulator.IHeadlessSimulator;
import org.pcm.headless.core.simulator.simucom.SimuComHeadlessSimulator;
import org.pcm.headless.core.simulator.simulizar.SimuLizarHeadlessSimulator;
import org.pcm.headless.core.util.PCMUtil;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

import lombok.extern.java.Log;

@Log
public class HeadlessPalladioSimulator {

	private static boolean PCM_INITIALIZED = false;

	private InMemoryRepositoryReader repositoryReader;

	public HeadlessPalladioSimulator() {
		if (!PCM_INITIALIZED) {
			log.info("Initializing packages and loading PCM default models.");
			PCMUtil.loadPCMModels();
			PCM_INITIALIZED = true;
		}
		repositoryReader = new InMemoryRepositoryReader();
	}

	public InMemoryResultRepository triggerSimulation(HeadlessModelConfig modelConfig,
			HeadlessSimulationConfig simulationConfig) {
		return this.triggerSimulation(modelConfig, simulationConfig, null);
	}

	public InMemoryResultRepository triggerSimulation(HeadlessModelConfig modelConfig,
			HeadlessSimulationConfig simulationConfig, List<ISimulationProgressListener> listeners) {
		LocalMemoryRepository results = triggerSimulationRaw(modelConfig, simulationConfig, listeners, true);

		// parse results
		InMemoryResultRepository outResults = repositoryReader.convertRepository(results);

		cleanUp(results);

		// tell all that we processed the results
		if (listeners != null) {
			listeners.forEach(l -> {
				l.processed();
			});
		}

		// return it
		return outResults;
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
	public LocalMemoryRepository triggerSimulationRaw(HeadlessModelConfig modelConfig,
			HeadlessSimulationConfig simulationConfig, List<ISimulationProgressListener> listeners, boolean wait) {
		// 1. create belonging simulator
		IHeadlessSimulator simulator = createSimulatorFromType(simulationConfig.getType());
		simulator.initialize(modelConfig, simulationConfig);

		// 2. execute multiple times
		IntStream str = IntStream.range(0, simulationConfig.getRepetitions());
		if (simulationConfig.isParallelizeRepetitions()) {
			str = str.parallel();
		}

		str.forEach(i -> {
			// 3.1. prepare
			simulator.prepareRepetition();

			// 3.2. execute
			simulator.executeRepetition();

			// 3.3. inform
			if (listeners != null) {
				listeners.forEach(l -> l.finishedRepetition());
			}
		});

		// 4 inform
		if (listeners != null) {
			listeners.forEach(l -> {
				l.finished();
				if (!wait) {
					l.processed(); // we completely finished
				}
			});
		}

		// 5. return
		return simulator.getResults();
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

	private void cleanUp(LocalMemoryRepository results) {
		try {
			results.close();
		} catch (DataNotAccessibleException e) {
			log.warning("The repository could not be closed. This may lead to a memory leak.");
		} finally {
			RepositoryManager.removeRepository(RepositoryManager.getCentralRepository(), results);
		}
	}

}
