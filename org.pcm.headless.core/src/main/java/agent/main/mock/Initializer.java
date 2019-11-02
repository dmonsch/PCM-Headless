package agent.main.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.PcmPackage;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourcetype.ResourcetypePackage;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.simulation.AbstractSimulationConfig;
import de.uka.ipd.sdq.simulation.ISimulationControl;
import de.uka.ipd.sdq.simulation.IStatusObserver;

/**
 * This class is only used for the SimuCom simulations. It is like an agent and
 * simplifies the recursive calls. The idea is to inject this file into the JAR
 * which is build from the generated SimuCom resources.
 * 
 * @author David Monschein
 */
public class Initializer {

	public static void triggerSimulation(Map<String, Object> config) {
		SimuComConfig sconfig = new SimuComConfig(config, false);
		IStatusObserver mock = new StatusObserverMock();

		// it is weird that we need to do that via reflection but otherwise it takes our
		// mocked class
		// maybe i change this in the future, but its okay like that
		try {
			Class<?> simuComControlClass = Class.forName("main.SimuComControl");
			Class<?> simulationControlInterface = Class.forName(ISimulationControl.class.getName());

			Constructor<?> constructor = simuComControlClass.getConstructor();
			Object controlInstance = constructor.newInstance();

			Method prepareSimulationMethod = simulationControlInterface.getDeclaredMethod("prepareSimulation",
					AbstractSimulationConfig.class, IStatusObserver.class, boolean.class);
			Method startSimulationMethod = simulationControlInterface.getDeclaredMethod("startSimulation",
					AbstractSimulationConfig.class, IStatusObserver.class, boolean.class);

			prepareSimulationMethod.invoke(controlInstance, sconfig, mock, false);
			startSimulationMethod.invoke(controlInstance, sconfig, mock, false);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static IStatusObserver generateStatusObserverMock() {
		return new StatusObserverMock();
	}

	public static void initialize() {
		RepositoryPackage.eINSTANCE.eClass();
		PcmPackage.eINSTANCE.eClass();
		ResourcetypePackage.eINSTANCE.eClass();
		MonitorRepositoryPackage.eINSTANCE.eClass();
		MeasuringpointPackage.eINSTANCE.eClass();
		PcmmeasuringpointPackage.eINSTANCE.eClass();

		initPathmaps();

		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
	}

	private static void initPathmaps() {
		final String palladioResModel = "file:/Users/david/Desktop/SimulizarStandalone/workspace/SimulizarAutomizer/simucom/src/main/resources";
		final String metricSpecModel = "file:/Users/david/Desktop/SimulizarStandalone/workspace/SimulizarAutomizer/simucom/src/main/resources";

		String urlString = palladioResModel;
		final URI uri = URI.createURI(urlString);
		final URI target = uri.appendSegment("models").appendSegment("");
		URIConverter.URI_MAP.put(URI.createURI("pathmap://PCM_MODELS/"), target);

		urlString = metricSpecModel;
		final URI uri2 = URI.createURI(urlString);
		final URI target2 = uri2.appendSegment("models").appendSegment("");
		URIConverter.URI_MAP.put(URI.createURI("pathmap://METRIC_SPEC_MODELS/"), target2);

		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("resourcetype", new XMIResourceFactoryImpl());
		m.put("metricspec", new XMIResourceFactoryImpl());
	}

	private static class StatusObserverMock implements IStatusObserver {

		@Override
		public void updateStatus(int arg0, double arg1, long arg2) {
			System.out.println(arg0 + ";" + arg1 + ";" + arg2);
		}
	}

}
