package org.pcm.headless.core.simulator;

import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

public interface IHeadlessSimulator {

	public void initialize(HeadlessModelConfig models, HeadlessSimulationConfig simulationConfig);

	public void onceBefore();

	public RepetitionData beforeRepetition();

	public void executeRepetition(RepetitionData data);

	public void afterRepetition(RepetitionData data);

	public void onceAfter();

	public ISimulationResults getResults();

}
