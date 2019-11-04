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
import org.pcm.headless.core.simulator.impl.BasicSimulationResultsImpl;
import org.pcm.headless.core.util.HeadlessSimulationConfigUtil;
import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Lists;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public abstract class AbstractHeadlessSimulator implements IHeadlessSimulator {
	protected HeadlessModelConfig modelConfig;
	protected HeadlessSimulationConfig simulationConfig;

	private BasicSimulationResultsImpl basicResults;

	@Override
	public void initialize(HeadlessModelConfig models, HeadlessSimulationConfig simulationConfig) {
		// 0. set them to the instance
		this.simulationConfig = simulationConfig;
		this.modelConfig = models;

		this.basicResults = new BasicSimulationResultsImpl();
	}

	@Override
	public ISimulationResults getResults() {
		return basicResults;
	}

	protected RepetitionData pullNewRepetitionData(LocalMemoryRepository repository) {
		// 1. create the resource partition from the config
		PCMResourceSetPartition pcmPartition = createResourceSetPartitionFromConfig(modelConfig);

		// 2. create blackboard
		MDSDBlackboard blackboard = createBlackboard(pcmPartition);

		// 3. create config map
		Map<String, Object> configurationMap = HeadlessSimulationConfigUtil.convertToConfigMap(simulationConfig);
		configurationMap.put(AbstractEDP2RecorderConfigurationFactory.REPOSITORY_ID, repository.getId());

		return new RepetitionData(pcmPartition, blackboard, configurationMap, repository);
	}

	protected LocalMemoryRepository pullNewInMemoryRepository() {
		LocalMemoryRepository nRepository = this.createInMemoryRepository(simulationConfig.getExperimentName());
		basicResults.addRepository(nRepository);
		return nRepository;
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

	private LocalMemoryRepository createInMemoryRepository(String domain) {
		LocalMemoryRepository repo = RepositoryFactory.eINSTANCE.createLocalMemoryRepository();
		repo.setId(UUID.randomUUID().toString());
		repo.setDomain(domain);
		RepositoryManager.addRepository(RepositoryManager.getCentralRepository(), repo);

		return repo;
	}

}
