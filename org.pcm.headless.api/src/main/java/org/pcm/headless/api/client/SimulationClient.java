package org.pcm.headless.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.entity.ContentType;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SimulationClient {
	// URLS
	private static final String SET_URL = "/{id}/set/";
	private static final String CLEAR_URL = "/{id}/clear";

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private String baseUrl;
	private String id;

	private HeadlessModelConfig models;

	public SimulationClient(String baseUrl, String id) {
		this.baseUrl = baseUrl;
		this.id = id;

		this.models = new HeadlessModelConfig();
	}

	public boolean clear() {
		try {
			Unirest.get(this.baseUrl + integrateId(CLEAR_URL)).asString().getBody();
		} catch (UnirestException e) {
			return false;
		}
		return true;
	}

	private <T extends EObject> boolean setModel(T obj, ESimulationPart part) {
		String orgFileName = obj.eResource().getURI().lastSegment();

		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			ModelUtil.saveToFile(obj, tempFile.getAbsolutePath());

			applyModelFile(tempFile, part);

			tempFile.delete();

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

	private void uploadModelFile(File tempFile, ESimulationPart part, String orgFileName)
			throws FileNotFoundException, UnirestException {
		Unirest.post(this.baseUrl + integrateId(SET_URL) + part.toString())
				.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, orgFileName)
				.asString().getBody();
	}

	private String integrateId(String url) {
		return url.replaceAll("\\{id\\}", this.id);
	}

	// METHODS FOR SETTINGS MODELS
	public SimulationClient setRepository(Repository repo) {
		setModel(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setRepository(File repo) {
		applyModelFile(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setSystem(System repo) {
		setModel(repo, ESimulationPart.SYSTEM);
		return this;
	}

	public SimulationClient setSystem(File repo) {
		applyModelFile(repo, ESimulationPart.SYSTEM);
		return this;
	}

	public SimulationClient setUsageModel(UsageModel repo) {
		setModel(repo, ESimulationPart.USAGE_MODEL);
		return this;
	}

	public SimulationClient setUsageModel(File repo) {
		applyModelFile(repo, ESimulationPart.USAGE_MODEL);
		return this;
	}

	public SimulationClient setAllocation(Allocation repo) {
		setModel(repo, ESimulationPart.ALLOCATION);
		return this;
	}

	public SimulationClient setAllocation(File repo) {
		applyModelFile(repo, ESimulationPart.ALLOCATION);
		return this;
	}

	public SimulationClient setResourceEnvironment(ResourceEnvironment repo) {
		setModel(repo, ESimulationPart.REPOSITORY);
		return this;
	}

	public SimulationClient setResourceEnvironment(File repo) {
		applyModelFile(repo, ESimulationPart.RESOURCE_ENVIRONMENT);
		return this;
	}

	public SimulationClient setMonitorRepository(MonitorRepository repo) {
		setModel(repo, ESimulationPart.MONITOR_REPOSITORY);
		return this;
	}

	public SimulationClient setMonitorRepository(File repo) {
		applyModelFile(repo, ESimulationPart.MONITOR_REPOSITORY);
		return this;
	}
}
