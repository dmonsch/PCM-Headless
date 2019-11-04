package org.pcm.headless.core.simulator;

import java.util.List;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

public interface ISimulationResults {

	public List<LocalMemoryRepository> getRaw();

	public InMemoryResultRepository getConvertedRepository();

	public void close();

}
