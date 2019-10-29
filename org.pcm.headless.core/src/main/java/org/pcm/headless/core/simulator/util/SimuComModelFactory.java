package org.pcm.headless.core.simulator.util;

import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorFactory;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.simucomframework.calculator.RecorderAttachingCalculatorFactoryDecorator;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.simucomstatus.SimuComStatus;
import de.uka.ipd.sdq.simucomframework.simucomstatus.SimucomstatusFactory;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimEngineFactory;
import de.uka.ipd.sdq.simulation.preferences.SimulationPreferencesHelper;

public class SimuComModelFactory {

	public static SimuComModel createSimuComModel(final SimuComConfig configuration) {
		// Status model to store the runtime state of the simulator
		final SimuComStatus simuComStatus = createSimuComStatus();

		// Factory used to create the simulation engine used in the simulation,
		// e.g., SSJ engine or Desmo-J engine
		final ISimEngineFactory simEngineFactory = getSimEngineFactory();

		// ProbeFramework context used to take the measurements of the simulation
		final ProbeFrameworkContext probeFrameworkContext = new ProbeFrameworkContext(
				new RecorderAttachingCalculatorFactoryDecorator(new DefaultCalculatorFactory(), configuration));

		final SimuComModel simuComModel = new SimuComModel(configuration, simuComStatus, simEngineFactory, false,
				probeFrameworkContext);

		simuComModel.getSimulationStatus().setCurrentSimulationTime(0);

		return simuComModel;
	}

	private static ISimEngineFactory getSimEngineFactory() {
		// load factory for the preferred simulation engine
		final ISimEngineFactory factory = SimulationPreferencesHelper.getPreferredSimulationEngine();
		if (factory == null) {
			throw new RuntimeException("There is no simulation engine available. Install at least one engine.");
		}
		return factory;
	}

	/**
	 * Gets the SimuCom status, creates one if none exists.
	 *
	 * @return the SimuCom status.
	 */
	private static SimuComStatus createSimuComStatus() {
		final SimuComStatus simuComStatus = SimucomstatusFactory.eINSTANCE.createSimuComStatus();

		simuComStatus.setProcessStatus(SimucomstatusFactory.eINSTANCE.createSimulatedProcesses());
		simuComStatus.setResourceStatus(SimucomstatusFactory.eINSTANCE.createSimulatedResources());

		return simuComStatus;
	}
}