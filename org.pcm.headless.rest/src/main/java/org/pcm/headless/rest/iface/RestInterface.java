package org.pcm.headless.rest.iface;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.pcm.headless.core.HeadlessPalladioSimulator;
import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.core.simulator.ISimulationResults;
import org.pcm.headless.rest.data.SimulationStateSummary;
import org.pcm.headless.rest.data.StateSummary;
import org.pcm.headless.rest.state.PCMSimulationState;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.ESimulationState;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import lombok.extern.java.Log;

@Log
@RestController
@RequestMapping("/rest")
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
	private ScheduledExecutorService repetitionService;

	// queue
	private LinkedList<PCMSimulationState> readySimulations;
	private LinkedList<PCMSimulationState> queuedSimulations;
	private LinkedList<PCMSimulationState> runningSimulations;
	private LinkedList<PCMSimulationState> finishedSimulations;

	// properties
	@Value("${concurrentSimulations:3}")
	private int concurrentSimulations;

	@Value("${simulationMemory:10}")
	private int simulationMemory;

	@Value("${clearEnabled:true}")
	private boolean clearEnabled;

	@Value("${parallelRepetitionsEnabled:true}")
	private boolean parallelRepetitionsEnabled;

	@Value("${concurrentRepetitions:1}")
	private int concurrentRepetitions;

	@GetMapping("/clear")
	public synchronized String clear() {
		if (clearEnabled) {
			this.stateMapping.clear();
			this.resultMapping.clear();

			try {
				FileUtils.deleteDirectory(SIM_DATA_FOLDER);
			} catch (IOException e) {
				log.warning("Could not clean all files.");
			}

			this.readySimulations.clear();
			this.queuedSimulations.clear();
			this.runningSimulations.clear();
			this.finishedSimulations.clear();

			this.executorService.shutdownNow();
			this.executorService = Executors.newScheduledThreadPool(concurrentSimulations);
			this.repetitionService = Executors.newScheduledThreadPool(concurrentRepetitions);
		}

		return "{}";
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

	@GetMapping("/state")
	public String getStateSummary() {
		StateSummary r = new StateSummary();
		List<SimulationStateSummary> simR = stateMapping.entrySet().stream().map(st -> {
			SimulationStateSummary inner = new SimulationStateSummary();
			inner.setId(st.getValue().getId());
			inner.setSimulator(st.getValue().getSimConfig().getType().getName());
			inner.setFinishedRepetitions(st.getValue().getRepetitionProgress());
			inner.setName(st.getValue().getSimConfig().getExperimentName());
			inner.setRepetitions(st.getValue().getSimConfig().getRepetitions());
			inner.setState(st.getValue().getState().getName());
			inner.setSimulationTime(st.getValue().getSimConfig().getSimulationTime());
			inner.setMaximumMeasurementCount(st.getValue().getSimConfig().getMaximumMeasurementCount());
			return inner;
		}).collect(Collectors.toList());
		r.setSimulations(simR);

		try {
			return objectMapper.writeValueAsString(r);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "{}";
		}
	}

	@PostMapping("/{id}/set/config")
	public synchronized void setConfiguration(@PathVariable String id, @RequestParam String configJson) {
		if (stateMapping.containsKey(id)) {
			PCMSimulationState state = stateMapping.get(id);
			try {
				HeadlessSimulationConfig simConfig = objectMapper.readValue(configJson, HeadlessSimulationConfig.class);
				if (simConfig.getType() == ESimulationType.SIMUCOM) {
					simConfig.setParallelizeRepetitions(false);
					simConfig.setSimuComStoragePath(new File(state.getParentFolder(), "simucom").getAbsolutePath());
				}
				if (!parallelRepetitionsEnabled) {
					simConfig.setParallelizeRepetitions(false);
				}
				state.setSimConfig(simConfig);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@PostMapping("/{id}/set/{type}")
	public synchronized void setPart(@PathVariable String id, @PathVariable String type,
			@RequestParam("file") MultipartFile file) {
		if (stateMapping.containsKey(id)) {
			ESimulationPart part = ESimulationPart.fromString(type);
			PCMSimulationState state = stateMapping.get(id);

			if (part != null && file != null) {
				state.injectFile(file, part);
			}
		}
	}

	@GetMapping("/{id}/error")
	public String getErrorStack(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			PCMSimulationState state = stateMapping.get(id);
			if (state.getState() == ESimulationState.FAILED) {
				return state.getErrorStack().toString();
			} else {
				return "{}";
			}
		}
		return "{}";
	}

	@GetMapping("/{id}/results")
	public String getResults(@PathVariable String id) {
		if (resultMapping.containsKey(id)) {
			try {
				return objectMapper.writeValueAsString(resultMapping.get(id));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return "{}";
			}
		}
		return "{}";
	}

	@GetMapping("/{id}/state")
	public String getSimulationState(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			return stateMapping.get(id).getState().getName();
		}
		return "{}";
	}

	@GetMapping("/{id}/clear")
	public String clearSimulationState(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			boolean success = clearState(stateMapping.get(id));
			if (success) {
				return "{\"success\" : true}";
			}
		}
		return "{\"success\" : false}";
	}

	@GetMapping("/{id}/start")
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
		return "{}";
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.objectMapper = new ObjectMapper();
		this.executor = new HeadlessPalladioSimulator();

		this.stateMapping = new LinkedHashMap<>();
		this.resultMapping = new HashMap<>();

		this.queuedSimulations = new LinkedList<>();
		this.runningSimulations = new LinkedList<>();
		this.readySimulations = new LinkedList<>();
		this.finishedSimulations = new LinkedList<>();

		this.executorService = Executors.newScheduledThreadPool(concurrentSimulations);

		if (this.clearEnabled) {
			this.clear();
		}

		log.info("Initialized REST interface successfully.");
	}

	private void checkQueue() {
		if (this.queuedSimulations.size() > 0 && this.runningSimulations.size() < concurrentSimulations) {
			PCMSimulationState removeFromQueue = this.queuedSimulations.removeFirst();

			// create new listeners
			ISimulationProgressListener list = new ISimulationProgressListener() {

				@Override
				public void finishedRepetition() {
				}

				@Override
				public void finished(ISimulationResults results) {
					runningSimulations.remove(removeFromQueue);

					removeFromQueue.setState(ESimulationState.FINISHED);
					resultMapping.put(removeFromQueue.getId(), results.getConvertedRepository());
					results.close();
					removeFromQueue.setState(ESimulationState.EXECUTED);

					finishedSimulations.add(removeFromQueue);
					trimFinishedQueue();
					clearHTMLFiles();
				}
			};

			// start one
			this.executorService.submit(() -> {
				try {
					this.executor.triggerSimulation(removeFromQueue.getModelConfig(), removeFromQueue.getSimConfig(),
							Lists.newArrayList(removeFromQueue, list), repetitionService);
				} catch (Exception e) {
					e.printStackTrace();
					runningSimulations.remove(removeFromQueue);
					finishedSimulations.add(removeFromQueue);
					removeFromQueue.setState(ESimulationState.FAILED);
					removeFromQueue.setErrorStack(e);
					clearHTMLFiles();
				}
			});

			// add to running
			this.runningSimulations.add(removeFromQueue);

			removeFromQueue.setState(ESimulationState.RUNNING);
		}
	}

	private void trimFinishedQueue() {
		if (this.finishedSimulations.size() > simulationMemory) {
			this.clearState(this.finishedSimulations.removeFirst());
			this.trimFinishedQueue();
		}
	}

	private boolean clearState(PCMSimulationState pcmSimulationState) {
		if (pcmSimulationState.getState() == ESimulationState.READY
				|| pcmSimulationState.getState() == ESimulationState.QUEUED
				|| pcmSimulationState.getState() == ESimulationState.EXECUTED
				|| pcmSimulationState.getState() == ESimulationState.FAILED) {
			// delete data
			try {
				FileUtils.deleteDirectory(pcmSimulationState.getParentFolder());
			} catch (IOException e) {
				log.warning("Could not delete simulation data.");
			}

			// we can remove it
			this.stateMapping.remove(pcmSimulationState.getId());
			this.resultMapping.remove(pcmSimulationState.getId());

			this.queuedSimulations.remove(pcmSimulationState);
			this.runningSimulations.remove(pcmSimulationState);
			this.readySimulations.remove(pcmSimulationState);
			this.finishedSimulations.remove(pcmSimulationState);

			return true;
		}
		return false;
	}

	private void clearHTMLFiles() {
		Path currentRelativePath = Paths.get(".");

		Stream.of(currentRelativePath.toFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".html");
			}
		})).forEach(file -> {
			file.delete();
		});
	}

}
