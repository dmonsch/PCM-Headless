package org.pcm.headless.core.transform;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

public class DefaultURITransformer implements IURITransformer {
	private Map<URI, URI> transformationRules;
	private int currentFileId;

	public DefaultURITransformer(Repository repo, System system, Allocation alloc, ResourceEnvironment env,
			UsageModel um) {
		transformationRules = new HashMap<>();

		transformationRules.put(repo.eResource().getURI(), URI.createFileURI("temp.repository"));
		transformationRules.put(system.eResource().getURI(), URI.createFileURI("temp.system"));
		transformationRules.put(alloc.eResource().getURI(), URI.createFileURI("temp.allocation"));
		transformationRules.put(env.eResource().getURI(), URI.createFileURI("temp.resourceenvironment"));
		transformationRules.put(um.eResource().getURI(), URI.createFileURI("temp.usagemodel"));

		currentFileId = 0;
	}

	@Override
	public URI transform(URI uri) {
		if (transformationRules.containsKey(uri)) {
			return transformationRules.get(uri);
		} else {
			return uri;
		}
	}

	@Override
	public void installRule(URI uri) {
		if (!transformationRules.containsKey(uri)) {
			String additionalPath = "additional/model" + String.valueOf(currentFileId++) + ".model";
			transformationRules.put(uri, URI.createFileURI(additionalPath));
		}
	}

}