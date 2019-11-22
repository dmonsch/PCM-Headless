package org.pcm.headless.api.client.transform;

import org.eclipse.emf.ecore.EObject;

public class DefaultModelFileNameGenerator implements IModelFileNameGenerator {

	private int modelId = 0;

	@Override
	public synchronized String generateFileName(EObject obj) {
		return "model" + String.valueOf(modelId++) + ".model";
	}

}
