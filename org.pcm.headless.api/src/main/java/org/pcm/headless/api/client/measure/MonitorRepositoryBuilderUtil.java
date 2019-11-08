package org.pcm.headless.api.client.measure;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointFactory;
import org.palladiosimulator.monitorrepository.FeedThrough;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcmmeasuringpoint.UsageScenarioMeasuringPoint;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.MonitorRepositoryTransformer;
import org.pcm.headless.api.util.PCMUtil;

import com.google.common.collect.Lists;

public class MonitorRepositoryBuilderUtil {
	private static final String BASE_RESPONSE_TIME_METRIC_ID = "_6rYmYs7nEeOX_4BzImuHbA";

	private List<Repository> repositories;
	private System system;
	private UsageModel usage;

	private MonitorRepository mRepo;
	private MeasuringPointRepository pRepo;

	public MonitorRepositoryBuilderUtil(File repo, File system, File usage) {
		this(Lists.newArrayList(repo), system, usage);
	}

	public MonitorRepositoryBuilderUtil(List<File> repo, File system, File usage) {
		this(repo.stream().map(r -> ModelUtil.readFromFile(r.getAbsolutePath(), Repository.class))
				.collect(Collectors.toList()), ModelUtil.readFromFile(system.getAbsolutePath(), System.class),
				ModelUtil.readFromFile(usage.getAbsolutePath(), UsageModel.class));
	}

	public MonitorRepositoryBuilderUtil(List<Repository> repo, System system, UsageModel usage) {
		this.repositories = repo;
		this.system = system;
		this.usage = usage;

		this.mRepo = MonitorRepositoryFactory.eINSTANCE.createMonitorRepository();
		this.pRepo = MeasuringpointFactory.eINSTANCE.createMeasuringPointRepository();
	}

	public void saveToFile(File monitorRepositoryFile, File measuringPointRepositoryFile) {
		MonitorRepositoryTransformer.makePersistable(mRepo);

		ModelUtil.saveToFile(pRepo, measuringPointRepositoryFile);
		ModelUtil.saveToFile(mRepo, monitorRepositoryFile);
	}

	public MonitorRepositoryBuilderUtil monitorUsageScenarios() {
		ModelUtil.getObjects(usage, UsageScenario.class).forEach(scenario -> {
			UsageScenarioMeasuringPoint nPoint = PcmmeasuringpointFactory.eINSTANCE.createUsageScenarioMeasuringPoint();
			nPoint.setUsageScenario(scenario);
			pRepo.getMeasuringPoints().add(nPoint);

			createMonitorForMeasuringPoint(nPoint);
		});

		return this;
	}

	public MonitorRepositoryBuilderUtil monitorExternalCalls() {
		repositories.forEach(repository -> {
			ModelUtil.getObjects(repository, ExternalCallAction.class).forEach(action -> {
				ExternalCallActionMeasuringPoint nPoint = PcmmeasuringpointFactory.eINSTANCE
						.createExternalCallActionMeasuringPoint();
				nPoint.setExternalCall(action);
				pRepo.getMeasuringPoints().add(nPoint);

				createMonitorForMeasuringPoint(nPoint);
			});
		});
		return this;
	}

	/**
	 * WARNING: This currently does not work with the SimuLizar simulator, because
	 * there is no support for it. Consider using
	 * {@link MonitorRepositoryBuilderUtil#monitorExternalCalls()}
	 * 
	 * @return current instance of the builder
	 */
	public MonitorRepositoryBuilderUtil monitorServices() {
		repositories.forEach(repository -> {
			ModelUtil.getObjects(repository, OperationProvidedRole.class).forEach(provRole -> {
				List<AssemblyContext> matchingContexts = ModelUtil.getObjects(system, AssemblyContext.class).stream()
						.filter(ctx -> {
							return ctx.getEncapsulatedComponent__AssemblyContext().getId()
									.equals(provRole.getProvidingEntity_ProvidedRole().getId());
						}).collect(Collectors.toList());

				provRole.getProvidedInterface__OperationProvidedRole().getSignatures__OperationInterface()
						.forEach(sig -> {
							matchingContexts.forEach(ctx -> {
								// create point
								AssemblyOperationMeasuringPoint nPoint = PcmmeasuringpointFactory.eINSTANCE
										.createAssemblyOperationMeasuringPoint();
								nPoint.setAssembly(ctx);
								nPoint.setOperationSignature(sig);
								nPoint.setRole(provRole);
								pRepo.getMeasuringPoints().add(nPoint);

								// create monitor
								createMonitorForMeasuringPoint(nPoint);
							});
						});
			});
		});

		return this;
	}

	private void createMonitorForMeasuringPoint(MeasuringPoint point) {
		// create monitor
		Monitor nMonitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();
		MeasurementSpecification spec = MonitorRepositoryFactory.eINSTANCE.createMeasurementSpecification();
		spec.setMetricDescription(PCMUtil.getMetricByID(BASE_RESPONSE_TIME_METRIC_ID).get());
		nMonitor.setMeasuringPoint(point);
		nMonitor.getMeasurementSpecifications().add(spec);
		mRepo.getMonitors().add(nMonitor);

		// create feed through
		FeedThrough ft = MonitorRepositoryFactory.eINSTANCE.createFeedThrough();
		spec.setProcessingType(ft);
	}

}
