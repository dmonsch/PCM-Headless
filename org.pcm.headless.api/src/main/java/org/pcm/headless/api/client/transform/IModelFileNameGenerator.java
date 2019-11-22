package org.pcm.headless.api.client.transform;

import org.eclipse.emf.ecore.EObject;

public interface IModelFileNameGenerator {

	public String generateFileName(EObject obj);

}
