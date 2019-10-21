package org.pcm.headless.rest.iface;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.io.FileUtils;
import org.pcm.headless.core.HeadlessPalladioSimulator;
import org.pcm.headless.core.data.results.InMemoryResultRepository;
import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.rest.state.PCMSimulationState;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@RestController
public class RestInterface implements InitializingBean {
	// files & folders
	private static final File SIM_DATA_FOLDER = new File("SimulationData/");

	// models
	private Map<String, PCMSimulationState> stateMapping;
	private Map<String, InMemoryResultRepository> resultMapping;

	// executing
	private HeadlessPalladioSimulator executor;
	private ObjectMapper objectMapper;
	private ScheduledExecutorService executorService;

	// queue
	private LinkedList<PCMSimulationState> readySimulations;
	private LinkedList<PCMSimulationState> queuedSimulations;
	private LinkedList<PCMSimulationState> runningSimulations;
	private LinkedList<PCMSimulationState> finishedSimulations;

	// properties
	@Value("${concurrentSimulations:0}")
	private int concurrentSimulations;

	@Value("${simulationMemory:10}")
	private int simulationMemory;

	@GetMapping("/clear")
	public synchronized String clear() {
		this.stateMapping.clear();
		try {
			FileUtils.deleteDirectory(SIM_DATA_FOLDER);
			return "true";
		} catch (IOException e) {
			return "false";
		}
	}

	@GetMapping("/prepare")
	public String prepareSimulation() {
		String generatedId = UUID.randomUUID().toString();
		File parentFolder = new File(SIM_DATA_FOLDER, generatedId);
		PCMSimulationState state = new PCMSimulationState(generatedId, parentFolder);
		stateMapping.put(generatedId, state);
		readySimulations.add(state);
		return generatedId;
	}

	@GetMapping("/state/{id}")
	public String getSimulationState(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			return stateMapping.get(id).getState().getName();
		}
		return "null";
	}

	@GetMapping("/start/{id}/")
	public synchronized String startSimulation(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			PCMSimulationState state = stateMapping.get(id);
			state.queued();
			this.queuedSimulations.add(state);

			checkQueue();
		}
		return "{}";
	}

	@GetMapping("/ping")
	public String reachable() {
		return "true";
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.objectMapper = new ObjectMapper();
		this.executor = new HeadlessPalladioSimulator();

		this.stateMapping = new HashMap<>();
		this.resultMapping = new HashMap<>();

		this.queuedSimulations = new LinkedList<>();
		this.runningSimulations = new LinkedList<>();
		this.readySimulations = new LinkedList<>();

		this.executorService = Executors.newScheduledThreadPool(concurrentSimulations);
	}

	private void checkQueue() {
		if (this.queuedSimulations.size() > 0 && this.runningSimulations.size() < concurrentSimulations) {
			PCMSimulationState removeFromQueue = this.queuedSimulations.removeFirst();

			// create new listeners
			ISimulationProgressListener list = new ISimulationProgressListener() {
				@Override
				public void processed() {
					finishedSimulations.add(removeFromQueue);
					trimFinishedQueue();
				}

				@Override
				public void finishedRepetition() {
				}

				@Override
				public void finished() {
					runningSimulations.remove(removeFromQueue);
				}
			};

			// start one
			this.executorService.submit(() -> {
				InMemoryResultRepository resultRepository = this.executor.triggerSimulation(
						removeFromQueue.getModelConfig(), removeFromQueue.getSimConfig(),
						Lists.newArrayList(removeFromQueue, list));
				resultMapping.put(removeFromQueue.getId(), resultRepository);
			});
			// add to running
			this.runningSimulations.add(removeFromQueue);
		}
	}

	private void trimFinishedQueue() {
		// TODO
	}

}
