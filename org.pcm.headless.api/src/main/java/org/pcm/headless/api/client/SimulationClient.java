package org.pcm.headless.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.headless.api.client.transform.DefaultURITransformer;
import org.pcm.headless.api.client.transform.TransitiveModelTransformer;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.ESimulationState;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SimulationClient {
	// URLS
	private static final String SET_URL = "/rest/{id}/set/";
	private static final String SET_CONFIG_URL = "/rest/{id}/set/config";
	private static final String CLEAR_URL = "/rest/{id}/clear";
	private static final String GET_STATE_URL = "/rest/{id}/state";

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private String baseUrl;
	private String id;

	private HeadlessModelConfig models;

	private Map<String, String> orgFileNameMapping;

	private boolean synced = false;

	public SimulationClient(String baseUrl, String id) {
		this.baseUrl = baseUrl;
		this.id = id;

		this.models = new HeadlessModelConfig();
		this.orgFileNameMapping = new HashMap<>();
	}

	public void sync() {
		// REPOSITORY
		this.models.getRepositoryFiles().forEach(r -> {
			uploadModelFile(r, ESimulationPart.REPOSITORY, getOriginalFilename(r));
		});

		// ALLOCATION
		uploadModelFile(models.getAllocationFile(), ESimulationPart.ALLOCATION,
				getOriginalFilename(models.getAllocationFile()));

		// SYSTEM
		uploadModelFile(models.getSystemFile(), ESimulationPart.SYSTEM, getOriginalFilename(models.getSystemFile()));

		// USAGE
		uploadModelFile(models.getUsageFile(), ESimulationPart.USAGE_MODEL, getOriginalFilename(models.getUsageFile()));

		// RESOURCE ENV
		uploadModelFile(models.getResourceEnvironmentFile(), ESimulationPart.RESOURCE_ENVIRONMENT,
				getOriginalFilename(models.getResourceEnvironmentFile()));

		// ADDITIONALS
		this.models.getAdditionals().forEach(r -> {
			uploadModelFile(r, ESimulationPart.ADDITIONAL, getOriginalFilename(r));
		});

		synced = true;
	}

	public boolean executeSimulation(ISimulationResultListener resultListener) {
		if (synced) {
			// TODO

			return true;
		}
		return false;
	}

	public void createTransitiveClosure() {
		// collect all models from the config
		EObject[] loadedModels = models.getAllFiles().stream()
				.map(f -> ModelUtil.readFromFile(f.getAbsolutePath(), EObject.class)).toArray(EObject[]::new);

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

	public boolean clearLocal() {
		return models.getAllFiles().stream().allMatch(f -> f.delete());
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

	private <T extends EObject> boolean setModel(T obj, ESimulationPart part) {
		String orgFileName = obj.eResource().getURI().lastSegment();

		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			ModelUtil.saveToFile(obj, tempFile.getAbsolutePath());

			applyModelFile(tempFile, part);

			orgFileNameMapping.put(tempFile.getCanonicalPath(), orgFileName);

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private void applyModelFile(File tempFile, ESimulationPart part) {
		switch (part) {
		case ADDITIONAL:
			models.getAdditionals().add(tempFile);
			break;
		case ALLOCATION:
			models.setAllocationFile(tempFile);
			break;
		case MONITOR_REPOSITORY:
			models.setMonitorRepository(tempFile);
			break;
		case REPOSITORY:
			models.getRepositoryFiles().add(tempFile);
			break;
		case RESOURCE_ENVIRONMENT:
			models.setResourceEnvironmentFile(tempFile);
			break;
		case SYSTEM:
			models.setSystemFile(tempFile);
			break;
		case USAGE_MODEL:
			models.setUsageFile(tempFile);
			break;
		default:
			break;
		}
	}

	private String getOriginalFilename(File file) {
		try {
			return orgFileNameMapping.get(file.getCanonicalPath());
		} catch (IOException e) {
			return null;
		}
	}

	private void uploadModelFile(File tempFile, ESimulationPart part, String orgFileName) {
		try {
			Unirest.post(this.baseUrl + integrateId(SET_URL) + part.toString())
					.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, orgFileName)
					.asString().getBody();
		} catch (FileNotFoundException | UnirestException e) {
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
		synced = false;
		applyModelFile(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setSystem(System repo) {
		synced = false;
		setModel(repo, ESimulationPart.SYSTEM);
		return this;
	}

	public SimulationClient setSystem(File repo) {
		synced = false;
		applyModelFile(repo, ESimulationPart.SYSTEM);
		return this;
	}

	public SimulationClient setUsageModel(UsageModel repo) {
		synced = false;
		setModel(repo, ESimulationPart.USAGE_MODEL);
		return this;
	}

	public SimulationClient setUsageModel(File repo) {
		synced = false;
		applyModelFile(repo, ESimulationPart.USAGE_MODEL);
		return this;
	}

	public SimulationClient setAllocation(Allocation repo) {
		synced = false;
		setModel(repo, ESimulationPart.ALLOCATION);
		return this;
	}

	public SimulationClient setAllocation(File repo) {
		synced = false;
		applyModelFile(repo, ESimulationPart.ALLOCATION);
		return this;
	}

	public SimulationClient setResourceEnvironment(ResourceEnvironment repo) {
		synced = false;
		setModel(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setResourceEnvironment(File repo) {
		synced = false;
		applyModelFile(repo, ESimulationPart.RESOURCE_ENVIRONMENT);
		return this;
	}

	public SimulationClient setMonitorRepository(MonitorRepository repo) {
		synced = false;
		setModel(repo, ESimulationPart.MONITOR_REPOSITORY);
		return this;
	}

	public SimulationClient setMonitorRepository(File repo) {
		synced = false;
		applyModelFile(repo, ESimulationPart.MONITOR_REPOSITORY);
		return this;
	}
}
