package org.pcm.headless.rest.state;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.pcm.headless.core.config.HeadlessModelConfig;
import org.pcm.headless.core.config.HeadlessSimulationConfig;
import org.pcm.headless.core.progress.ISimulationProgressListener;

import lombok.Data;

@Data
public class PCMSimulationState implements ISimulationProgressListener {

	private ESimulationState state;
	private int repetitionProgress;

	private HeadlessModelConfig modelConfig;
	private HeadlessSimulationConfig simConfig;

	private String id;
	private File parentFolder;

	public PCMSimulationState(String id, File parentFolder) {
		this.id = id;
		this.parentFolder = parentFolder;
		if (!this.parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		this.state = ESimulationState.READY;
		this.repetitionProgress = 0;
	}

	public PCMSimulationState(String id) {
		this(id, new File(id));
	}

	public boolean clear() {
		try {
			FileUtils.deleteDirectory(parentFolder);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public void reset() {
		this.state = ESimulationState.READY;
		this.repetitionProgress = 0;
	}

	public void queued() {
		this.state = ESimulationState.QUEUED;
	}

	@Override
	public void finished() {
		this.state = ESimulationState.EXECUTED;
	}

	@Override
	public synchronized void finishedRepetition() {
		this.state = ESimulationState.RUNNING;
		this.repetitionProgress++;
	}

	@Override
	public void processed() {
		this.state = ESimulationState.FINISHED;
	}

}
