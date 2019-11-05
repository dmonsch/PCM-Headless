package org.pcm.headless.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.entity.ContentType;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.headless.api.client.data.InMemoryModelConfig;
import org.pcm.headless.api.client.transform.DefaultURITransformer;
import org.pcm.headless.api.client.transform.TransitiveModelTransformer;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.MonitorRepositoryTransformer;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.ESimulationState;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SimulationClient {
	// STATICS
	private static final long POLL_DELAY = 250;
	private static final long MAX_TIMEOUT_RESULTS = 60000;

	// URLS
	private static final String SET_URL = "/rest/{id}/set/";
	private static final String SET_CONFIG_URL = "/rest/{id}/set/config";
	private static final String CLEAR_URL = "/rest/{id}/clear";
	private static final String GET_STATE_URL = "/rest/{id}/state";
	private static final String RESULTS_URL = "/rest//{id}/results";

	private static final String START_URL = "/rest/{id}/start";

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private String baseUrl;
	private String id;

	private InMemoryModelConfig models;

	private boolean synced = false;

	public SimulationClient(String baseUrl, String id) {
		this.baseUrl = baseUrl;
		this.id = id;

		this.models = new InMemoryModelConfig();
	}

	public InMemoryResultRepository getResults() {
		try {
			String resultBody = Unirest.get(this.baseUrl + integrateId(RESULTS_URL)).asString().getBody();
			return JSON_MAPPER.readValue(resultBody, InMemoryResultRepository.class);
		} catch (UnirestException | JsonProcessingException e) {
			return null;
		}
	}

	public void sync() {
		// REPOSITORY
		this.models.getRepositorys().forEach(r -> {
			uploadModel(r, ESimulationPart.REPOSITORY);
		});

		// ALLOCATION
		uploadModel(models.getAllocation(), ESimulationPart.ALLOCATION);

		// SYSTEM
		uploadModel(models.getSystem(), ESimulationPart.SYSTEM);

		// USAGE
		uploadModel(models.getUsage(), ESimulationPart.USAGE_MODEL);

		// RESOURCE ENV
		uploadModel(models.getResourceEnvironment(), ESimulationPart.RESOURCE_ENVIRONMENT);

		// MONITOR REPO
		if (models.getMonitorRepository() != null) {
			MonitorRepositoryTransformer.makePersistable(models.getMonitorRepository());
			uploadModel(models.getMonitorRepository(), ESimulationPart.MONITOR_REPOSITORY);
		}

		// ADDITIONALS
		this.models.getAdditionals().forEach(r -> {
			uploadModel(r, ESimulationPart.ADDITIONAL);
		});

		synced = true;
	}

	public boolean executeSimulation(ISimulationResultListener resultListener) {
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

		if (synced) {
			try {
				Unirest.get(this.baseUrl + integrateId(START_URL)).asString().getBody();
				executorService.submit(new ResultListenerTask(resultListener, executorService));
				return true;
			} catch (UnirestException e) {
				return false;
			}
		}
		return false;
	}

	public void createTransitiveClosure() {
		// collect all models from the config
		EObject[] loadedModels = models.getAllModels().stream().toArray(EObject[]::new);

		// create transformer
		TransitiveModelTransformer transformer = new TransitiveModelTransformer(loadedModels);

		// create uri transformer
		DefaultURITransformer uriTransformer = new DefaultURITransformer();

		// transform everything
		transformer.transformURIs(uriTransformer);

		// add additionals
		for (URI additional : transformer.getTransitives()) {
			EObject model = transformer.getModelByURI(additional);
			if (model instanceof Repository) {
				// add a repository
				setModel(model, ESimulationPart.REPOSITORY);
			} else {
				// it is something else
				setModel(model, ESimulationPart.ADDITIONAL);
			}
		}
	}

	public boolean clear() {
		try {
			Unirest.get(this.baseUrl + integrateId(CLEAR_URL)).asString().getBody();
		} catch (UnirestException e) {
			return false;
		}
		return true;
	}

	public ESimulationState getState() {
		try {
			return ESimulationState
					.fromString(Unirest.get(this.baseUrl + integrateId(GET_STATE_URL)).asString().getBody());
		} catch (UnirestException e) {
			return null;
		}
	}

	public void setSimulationConfig(HeadlessSimulationConfig config) {
		try {
			Unirest.post(this.baseUrl + integrateId(SET_CONFIG_URL))
					.field("configJson", JSON_MAPPER.writeValueAsString(config)).asString().getBody();
		} catch (JsonProcessingException | UnirestException e) {
			e.printStackTrace();
		}
	}

	private <T extends EObject> void setModel(T obj, ESimulationPart part) {
		switch (part) {
		case ADDITIONAL:
			models.getAdditionals().add(obj);
			break;
		case ALLOCATION:
			models.setAllocation(obj);
			break;
		case MONITOR_REPOSITORY:
			models.setMonitorRepository(obj);
			break;
		case REPOSITORY:
			models.getRepositorys().add(obj);
			break;
		case RESOURCE_ENVIRONMENT:
			models.setResourceEnvironment(obj);
			break;
		case SYSTEM:
			models.setSystem(obj);
			break;
		case USAGE_MODEL:
			models.setUsage(obj);
			break;
		default:
			break;
		}
	}

	private void uploadModel(EObject obj, ESimulationPart part) {
		String orgFileName = obj.eResource().getURI().lastSegment();

		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			ModelUtil.saveToFile(obj, tempFile.getAbsolutePath());

			Unirest.post(this.baseUrl + integrateId(SET_URL) + part.toString())
					.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, orgFileName)
					.asString().getBody();
			tempFile.delete();
		} catch (IOException | UnirestException e) {
			e.printStackTrace();
		}
	}

	private String integrateId(String url) {
		return url.replaceAll("\\{id\\}", this.id);
	}

	// METHODS FOR SETTINGS MODELS
	public SimulationClient setRepository(Repository repo) {
		synced = false;
		setModel(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setRepository(File repo) {
		return this.setRepository(ModelUtil.readFromFile(repo.getAbsolutePath(), Repository.class));
	}

	public SimulationClient setSystem(System repo) {
		synced = false;
		setModel(repo, ESimulationPart.SYSTEM);
		return this;
	}

	public SimulationClient setSystem(File repo) {
		return this.setSystem(ModelUtil.readFromFile(repo.getAbsolutePath(), System.class));
	}

	public SimulationClient setUsageModel(UsageModel repo) {
		synced = false;
		setModel(repo, ESimulationPart.USAGE_MODEL);
		return this;
	}

	public SimulationClient setUsageModel(File repo) {
		return this.setUsageModel(ModelUtil.readFromFile(repo.getAbsolutePath(), UsageModel.class));
	}

	public SimulationClient setAllocation(Allocation repo) {
		synced = false;
		setModel(repo, ESimulationPart.ALLOCATION);
		return this;
	}

	public SimulationClient setAllocation(File repo) {
		return this.setAllocation(ModelUtil.readFromFile(repo.getAbsolutePath(), Allocation.class));
	}

	public SimulationClient setResourceEnvironment(ResourceEnvironment repo) {
		synced = false;
		setModel(repo, ESimulationPart.RESOURCE_ENVIRONMENT);
		return this;
	}

	public SimulationClient setResourceEnvironment(File repo) {
		return this.setResourceEnvironment(ModelUtil.readFromFile(repo.getAbsolutePath(), ResourceEnvironment.class));
	}

	public SimulationClient setMonitorRepository(MonitorRepository repo) {
		synced = false;
		setModel(repo, ESimulationPart.MONITOR_REPOSITORY);
		return this;
	}

	public SimulationClient setMonitorRepository(File repo) {
		return this.setMonitorRepository(ModelUtil.readFromFile(repo.getAbsolutePath(), MonitorRepository.class));
	}

	private class ResultListenerTask implements Runnable {
		private ISimulationResultListener listener;
		private int trys;
		private ScheduledExecutorService executorService;

		public ResultListenerTask(ISimulationResultListener listener, ScheduledExecutorService executorService) {
			this.listener = listener;
			this.trys = 0;
			this.executorService = executorService;
		}

		@Override
		public void run() {
			ESimulationState currentState = SimulationClient.this.getState();

			if (currentState != ESimulationState.EXECUTED && this.trys * POLL_DELAY < MAX_TIMEOUT_RESULTS) {
				this.trys++;
				executorService.schedule(this, POLL_DELAY, TimeUnit.MILLISECONDS);
			} else if (this.trys * POLL_DELAY >= MAX_TIMEOUT_RESULTS) {
				executorService.shutdown();
				listener.onResult(null); // => timeout
			} else if (currentState == ESimulationState.EXECUTED) {
				executorService.shutdown();
				listener.onResult(getResults());
			}
		}
	}
}
