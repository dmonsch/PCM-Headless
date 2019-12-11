package org.pcm.headless.api.client;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.headless.api.client.data.InMemoryModelConfig;
import org.pcm.headless.api.client.transform.DefaultModelFileNameGenerator;
import org.pcm.headless.api.client.transform.TransitiveModelTransformer;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.MonitorRepositoryTransformer;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.ESimulationState;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SimulationClient {
	// STATICS
	private static final long DEFAULT_TIMEOUT = 60000;
	private static final long DEFAULT_POLL_DELAY = 250;

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

	private OkHttpClient client;

	private boolean synced = false;

	public SimulationClient(String baseUrl, String id, OkHttpClient client) {
		this.baseUrl = baseUrl;
		this.id = id;
		this.client = client;

		this.models = new InMemoryModelConfig();
	}

	public InMemoryResultRepository getResults() {
		Request request = new Request.Builder().url(this.baseUrl + integrateId(RESULTS_URL)).build();
		try (Response response = client.newCall(request).execute()) {
			return JSON_MAPPER.readValue(response.body().string(), InMemoryResultRepository.class);
		} catch (IOException e) {
			return null;
		}
	}

	public void clearModels() {
		this.models.clear();
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
		return this.executeSimulation(resultListener, DEFAULT_TIMEOUT, DEFAULT_POLL_DELAY);
	}

	public boolean executeSimulation(ISimulationResultListener resultListener, long timeout) {
		return this.executeSimulation(resultListener, timeout, DEFAULT_POLL_DELAY);
	}

	public boolean executeSimulation(ISimulationResultListener resultListener, long timeout, long delay) {
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

		if (synced) {
			Request request = new Request.Builder().url(this.baseUrl + integrateId(START_URL)).build();
			try (Response response = client.newCall(request).execute()) {
				executorService.submit(new ResultListenerTask(resultListener, executorService, timeout, delay));
			} catch (IOException e) {
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
		DefaultModelFileNameGenerator fileNameGenerator = new DefaultModelFileNameGenerator();

		// transform everything
		List<EObject> resultingModels = transformer.buildModels(fileNameGenerator);

		// clear existing models
		clearModels();

		// write modified models
		for (EObject obj : resultingModels) {
			setModelGeneric(obj);
		}
	}

	public boolean clear() {
		Request request = new Request.Builder().url(this.baseUrl + integrateId(CLEAR_URL)).build();
		try (Response response = client.newCall(request).execute()) {
			response.body().string();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public ESimulationState getState() {
		Request request = new Request.Builder().url(this.baseUrl + integrateId(GET_STATE_URL)).build();
		try (Response response = client.newCall(request).execute()) {
			return ESimulationState.fromString(response.body().string());
		} catch (IOException e) {
			return null;
		}
	}

	public void setSimulationConfig(HeadlessSimulationConfig config) {
		RequestBody formBody;
		try {
			formBody = new FormBody.Builder().add("configJson", JSON_MAPPER.writeValueAsString(config)).build();
		} catch (JsonProcessingException e1) {
			return;
		}

		Request request = new Request.Builder().url(this.baseUrl + integrateId(SET_CONFIG_URL)).post(formBody).build();
		try (Response response = client.newCall(request).execute()) {
			response.body().string();
		} catch (IOException e) {
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

			EObject copy = EcoreUtil.copy(obj);
			if (copy instanceof MonitorRepository) {
				MonitorRepositoryTransformer.makePersistable(copy);
			}
			ModelUtil.saveToFile(copy, tempFile.getAbsolutePath());

			RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file",
					orgFileName, RequestBody.create(tempFile, MediaType.parse("application/octet-stream"))).build();
			Request request = new Request.Builder().url(this.baseUrl + integrateId(SET_URL) + part.toString())
					.post(requestBody).build();
			client.newCall(request).execute();

			tempFile.delete();
		} catch (IOException e) {
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

	public SimulationClient setAdditional(File model) {
		return this.setAdditional(ModelUtil.readFromFile(model.getAbsolutePath(), EObject.class));
	}

	public SimulationClient setAdditional(EObject obj) {
		synced = false;
		setModel(obj, ESimulationPart.ADDITIONAL);
		return this;
	}

	public SimulationClient setModelGeneric(File modelFile) {
		return this.setModelGeneric(ModelUtil.readFromFile(modelFile.getAbsolutePath(), EObject.class));
	}

	public SimulationClient setModelGeneric(EObject readFromFile) {
		if (readFromFile instanceof Repository) {
			return setRepository((Repository) readFromFile);
		} else if (readFromFile instanceof System) {
			return setSystem((System) readFromFile);
		} else if (readFromFile instanceof UsageModel) {
			return setUsageModel((UsageModel) readFromFile);
		} else if (readFromFile instanceof ResourceEnvironment) {
			return setResourceEnvironment((ResourceEnvironment) readFromFile);
		} else if (readFromFile instanceof Allocation) {
			return setAllocation((Allocation) readFromFile);
		} else if (readFromFile instanceof MonitorRepository) {
			return setMonitorRepository((MonitorRepository) readFromFile);
		} else {
			return setAdditional(readFromFile);
		}
	}

	private class ResultListenerTask implements Runnable {
		private ISimulationResultListener listener;
		private int trys;
		private ScheduledExecutorService executorService;
		private long timeout;
		private long delay;

		public ResultListenerTask(ISimulationResultListener listener, ScheduledExecutorService executorService,
				long timeout, long delay) {
			this.listener = listener;
			this.trys = 0;
			this.executorService = executorService;
			this.timeout = timeout;
			this.delay = delay;
		}

		@Override
		public void run() {
			ESimulationState currentState = SimulationClient.this.getState();

			if (currentState != ESimulationState.EXECUTED && this.trys * delay < timeout
					&& currentState != ESimulationState.FAILED) {
				this.trys++;
				executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
			} else if (this.trys * delay >= timeout || currentState == ESimulationState.FAILED) {
				executorService.shutdown();
				listener.onResult(null); // => timeout
			} else if (currentState == ESimulationState.EXECUTED) {
				executorService.shutdown();
				listener.onResult(getResults());
			}
		}
	}
}
