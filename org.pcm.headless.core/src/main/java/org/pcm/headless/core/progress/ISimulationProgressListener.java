package org.pcm.headless.core.progress;

import org.pcm.headless.core.simulator.ISimulationResults;

public interface ISimulationProgressListener {

	public void finished(ISimulationResults results);

	public void finishedRepetition();

}
