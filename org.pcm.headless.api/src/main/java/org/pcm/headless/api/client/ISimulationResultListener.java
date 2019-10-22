package org.pcm.headless.api.client;

import org.pcm.headless.shared.data.results.InMemoryResultRepository;

@FunctionalInterface
public interface ISimulationResultListener {
	public void onResult(InMemoryResultRepository results);
}
