package org.pcm.headless.rest.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.pcm.headless.core.progress.ISimulationProgressListener;
import org.pcm.headless.shared.data.ESimulationPart;
import org.pcm.headless.shared.data.ESimulationState;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.springframework.web.multipart.MultipartFile;

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

		this.modelConfig = new HeadlessModelConfig();
		this.simConfig = HeadlessSimulationConfig.builder().build();
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

	public void injectFile(MultipartFile file, ESimulationPart part) {
		File targetLocationFile = new File(parentFolder, file.getOriginalFilename());
		Path targetLocation = targetLocationFile.toPath();

		targetLocationFile.getParentFile().mkdirs();

		try {
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

		switch (part) {
		case ADDITIONAL:
			this.modelConfig.getAdditionals().add(targetLocationFile);
			break;
		case ALLOCATION:
			this.modelConfig.setAllocationFile(targetLocationFile);
			break;
		case MONITOR_REPOSITORY:
			this.modelConfig.setMonitorRepository(targetLocationFile);
			break;
		case REPOSITORY:
			this.modelConfig.getRepositoryFiles().add(targetLocationFile);
			break;
		case RESOURCE_ENVIRONMENT:
			this.modelConfig.setResourceEnvironmentFile(targetLocationFile);
			break;
		case SYSTEM:
			this.modelConfig.setSystemFile(targetLocationFile);
			break;
		case USAGE_MODEL:
			this.modelConfig.setUsageFile(targetLocationFile);
			break;
		default:
			break;

		}
	}

}
