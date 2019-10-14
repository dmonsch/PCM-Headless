package org.pcm.headless.core.config;

import java.io.File;
import java.util.List;

import lombok.Data;

@Data
public class HeadlessModelConfig {

	private List<File> repositoryFiles;
	private File systemFile;
	private File resourceEnvironmentFile;
	private File allocationFile;
	private File usageFile;

	private File monitorRepository;

	private List<File> additionals;

}
