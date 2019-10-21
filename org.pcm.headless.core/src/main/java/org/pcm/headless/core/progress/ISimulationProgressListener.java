package org.pcm.headless.core.progress;

public interface ISimulationProgressListener {

	public void processed();

	public void finished();

	public void finishedRepetition();

}
