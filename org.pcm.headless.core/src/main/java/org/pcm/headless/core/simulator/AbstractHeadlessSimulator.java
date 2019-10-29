package org.pcm.headless.core.simulator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.edp2.models.Repository.RepositoryFactory;
import org.palladiosimulator.recorderframework.edp2.config.AbstractEDP2RecorderConfigurationFactory;
import org.pcm.headless.core.util.HeadlessSimulationConfigUtil;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Lists;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public abstract class AbstractHeadlessSimulator implements IHeadlessSimulator {

	protected PCMResourceSetPartition pcmPartition;
	protected MDSDBlackboard blackboard;
	protected HeadlessModelConfig modelConfig;
	protected HeadlessSimulationConfig simulationConfig;
	protected Map<String, Object> configurationMap;

	private LocalMemoryRepository repository;
	// otherwise we dont use the initially loaded items
	private boolean firstCall = true;

	@Override
	public LocalMemoryRepository getResults() {
		return repository;
	}

	@Override
	public void initialize(HeadlessModelConfig models, HeadlessSimulationConfig simulationConfig) {
		// 0. set them to the instance
		this.simulationConfig = simulationConfig;
		this.modelConfig = models;

		// 1. create the resource partition from the config
		pcmPartition = createResourceSetPartitionFromConfig(models);

		// 2. create blackboard
		blackboard = createBlackboard(pcmPartition);

		// 3. create config map
		configurationMap = HeadlessSimulationConfigUtil.convertToConfigMap(simulationConfig);

		// 4. create in memory repo
		repository = createInMemoryRepository("HeadlessDomain");
		configurationMap.put(AbstractEDP2RecorderConfigurationFactory.REPOSITORY_ID, repository.getId());
	}

	protected void recreateData() {
		if (!firstCall) {
			// 1. create the resource partition from the config
			pcmPartition = createResourceSetPartitionFromConfig(modelConfig);

			// 2. create blackboard
			blackboard = createBlackboard(pcmPartition);

			// 3. create config map
			configurationMap = HeadlessSimulationConfigUtil.convertToConfigMap(simulationConfig);
		} else {
			firstCall = false;
		}
	}

	private LocalMemoryRepository createInMemoryRepository(String domain) {
		LocalMemoryRepository repo = RepositoryFactory.eINSTANCE.createLocalMemoryRepository();
		repo.setId(UUID.randomUUID().toString());
		repo.setDomain(domain);
		RepositoryManager.addRepository(RepositoryManager.getCentralRepository(), repo);

		return repo;
	}

	private MDSDBlackboard createBlackboard(PCMResourceSetPartition partition) {
		MDSDBlackboard blackboard = new MDSDBlackboard();
		blackboard.addPartition("org.palladiosimulator.pcmmodels.partition", partition);
		return blackboard;
	}

	private PCMResourceSetPartition createResourceSetPartitionFromConfig(HeadlessModelConfig modelConfig) {
		PCMResourceSetPartition partition = new PCMResourceSetPartition();

		// collect all files
		List<File> collectedFiles = Lists.newArrayList(modelConfig.getAllocationFile(),
				modelConfig.getResourceEnvironmentFile(), modelConfig.getSystemFile(), modelConfig.getUsageFile(),
				modelConfig.getMonitorRepository());
		collectedFiles.addAll(modelConfig.getRepositoryFiles());
		collectedFiles.addAll(modelConfig.getAdditionals());

		// load them into the resource set
		collectedFiles.forEach(file -> partition.loadModel(URI.createFileURI(file.getAbsolutePath())));

		// resolve proxies
		partition.resolveAllProxies();

		return partition;
	}

}
