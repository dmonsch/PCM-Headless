package org.pcm.headless.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.edp2.models.Repository.RepositoryFactory;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.recorderframework.edp2.config.AbstractEDP2RecorderConfigurationFactory;
import org.palladiosimulator.simulizar.access.ModelAccess;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;
import org.palladiosimulator.simulizar.runtimestate.SimuLizarRuntimeState;
import org.palladiosimulator.simulizar.runtimestate.SimulationCancelationDelegate;
import org.pcm.headless.core.config.HeadlessModelConfig;
import org.pcm.headless.core.config.HeadlessSimulationConfig;
import org.pcm.headless.core.proxy.ResourceContainerFactoryProxy;
import org.pcm.headless.core.util.ModelUtil;
import org.pcm.headless.core.util.PCMUtil;

import com.google.common.collect.Lists;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import lombok.extern.java.Log;

@Log
public class HeadlessPalladioSimulator {

	private static boolean PCM_INITIALIZED = false;

	public HeadlessPalladioSimulator() {
		if (!PCM_INITIALIZED) {
			log.info("Initializing packages and loading PCM default models.");
			PCMUtil.loadPCMModels();
			PCM_INITIALIZED = true;
		}
	}

	public void triggerSimulation(HeadlessModelConfig modelConfig, HeadlessSimulationConfig simulationConfig) {
		LocalMemoryRepository results = triggerSimulationRaw(modelConfig, simulationConfig);

		MonitorRepository monitorRepo = ModelUtil.readFromFile(modelConfig.getMonitorRepository().getAbsolutePath(),
				MonitorRepository.class);
		if (monitorRepo != null) {
			// TODO create mapping
		}

		cleanUp(results);
	}

	/**
	 * This method is used to trigger a simulation with a certain configuration.
	 * Please be aware that the output of this method needs to be treated carefully.
	 * All handles need to be closed manually and the repository needs to be removed
	 * from the central repository by hand. If you do not know how to handle this
	 * please consult
	 * {@link HeadlessPalladioSimulator#triggerSimulation(HeadlessModelConfig, HeadlessSimulationConfig)}
	 * or consider using it.
	 * 
	 * @param modelConfig      the configuration which holds the paths to the models
	 * @param simulationConfig the configuration which specifies the properties of
	 *                         the simulation
	 * @return {@link LocalMemoryRepository} which contains the simulation data
	 */
	public LocalMemoryRepository triggerSimulationRaw(HeadlessModelConfig modelConfig,
			HeadlessSimulationConfig simulationConfig) {
		// 1. create the resource partition from the config
		PCMResourceSetPartition pcmPartition = createResourceSetPartitionFromConfig(modelConfig);

		// 2. create blackboard
		MDSDBlackboard blackboard = createBlackboard(pcmPartition);

		// 3. create config map
		Map<String, Object> configurationMap = simulationConfig.convertToConfigMap();

		// 4. create in memory repo
		LocalMemoryRepository repository = createInMemoryRepository("HeadlessDomain");
		configurationMap.put(AbstractEDP2RecorderConfigurationFactory.REPOSITORY_ID, repository.getId());

		// 5. execute multiple times
		IntStream str = IntStream.range(0, simulationConfig.getRepetitions());
		if (simulationConfig.isParallelizeRepetitions()) {
			str = str.parallel();
		}
		str.forEach(i -> {
			// 5.1 create simulizar configuration (atm i dont know if this is necessary
			// within every repetition)
			SimuLizarRuntimeState runtime = buildSimulizarConfiguration(blackboard, configurationMap);

			// 5.2 initialize resource environment
			runtime.getModel().initialiseResourceContainer(
					new ResourceContainerFactoryProxy(pcmPartition.getResourceEnvironment()));

			// 5.3 execute
			runtime.runSimulation();
			runtime.cleanUp();
		});

		// 6. return
		return repository;
	}

	private void cleanUp(LocalMemoryRepository results) {
		try {
			results.close();
		} catch (DataNotAccessibleException e) {
			log.warning("The repository could not be closed. This may lead to a memory leak.");
		} finally {
			RepositoryManager.removeRepository(RepositoryManager.getCentralRepository(), results);
		}
	}

	private SimuLizarRuntimeState buildSimulizarConfiguration(MDSDBlackboard blackboard,
			Map<String, Object> configMap) {
		SimuLizarWorkflowConfiguration config = new SimuLizarWorkflowConfiguration(new HashMap<>());

		SimuComConfig simuconfig = new SimuComConfig(configMap, false);
		config.setSimuComConfiguration(simuconfig);

		return new SimuLizarRuntimeState(config, new ModelAccess(blackboard),
				new SimulationCancelationDelegate(() -> false));
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
