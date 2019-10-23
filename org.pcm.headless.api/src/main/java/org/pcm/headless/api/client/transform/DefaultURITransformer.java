package org.pcm.headless.api.client.transform;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;

public class DefaultURITransformer implements IURITransformer {
	private Map<URI, URI> transformationRules;
	private int currentFileId;

	public DefaultURITransformer() {
		transformationRules = new HashMap<>();

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
			String additionalPath = "model" + String.valueOf(currentFileId++) + ".model";
			transformationRules.put(uri, URI.createFileURI(additionalPath));
		}
	}

}