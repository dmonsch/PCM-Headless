package org.pcm.headless.core.simulator;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

public interface IHeadlessSimulator {

	public void initialize(HeadlessModelConfig models, HeadlessSimulationConfig simulationConfig);

	public void prepareRepetition();

	public void executeRepetition();

	public LocalMemoryRepository getResults();

}
