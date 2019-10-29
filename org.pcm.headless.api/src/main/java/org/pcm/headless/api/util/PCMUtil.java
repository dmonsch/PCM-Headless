package org.pcm.headless.api.util;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.monitorrepository.impl.MonitorRepositoryPackageImpl;
import org.palladiosimulator.pcm.PcmPackage;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourcetype.ResourcetypePackage;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;

public class PCMUtil {
	private static final String METRIC_DESC_PATHMAP = "pathmap://METRIC_SPEC_MODELS/commonMetrics.metricspec";

	/**
	 * Visits all common PCM package classes to load them.
	 */
	public static void loadPCMModels() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

		RepositoryPackage.eINSTANCE.eClass();
		PcmPackage.eINSTANCE.eClass();
		ResourcetypePackage.eINSTANCE.eClass();

		MonitorRepositoryPackage.eINSTANCE.eClass();
		MonitorRepositoryPackageImpl.init();
		((MonitorRepositoryPackageImpl) MonitorRepositoryPackage.eINSTANCE).createPackageContents();
		((MonitorRepositoryPackageImpl) MonitorRepositoryPackage.eINSTANCE).initializePackageContents();

		MeasuringpointPackage.eINSTANCE.eClass();
		PcmmeasuringpointPackage.eINSTANCE.eClass();

		initPathmaps();
	}

	public static Optional<MetricDescription> getMetricByID(String id) {
		return ModelUtil.readFromURI(METRIC_DESC_PATHMAP, MetricDescriptionRepository.class).getMetricDescriptions()
				.stream().filter(d -> d.getId().equals(id)).findFirst();
	}

	private static void initPathmaps() {
		final String palladioResModel = "models/Palladio.resourcetype";
		final String metricSpecModel = "models/commonMetrics.metricspec";
		final URL url = PCMUtil.class.getClassLoader().getResource(palladioResModel);
		final URL url2 = PCMUtil.class.getClassLoader().getResource(metricSpecModel);
		if (url == null || url2 == null) {
			throw new RuntimeException("Error getting common definitions");
		}

		String urlString = url.toString();
		urlString = urlString.substring(0, urlString.length() - palladioResModel.length() - 1);
		final URI uri = URI.createURI(urlString);
		final URI target = uri.appendSegment("models").appendSegment("");
		URIConverter.URI_MAP.put(URI.createURI("pathmap://PCM_MODELS/"), target);

		urlString = url2.toString();
		urlString = urlString.substring(0, urlString.length() - metricSpecModel.length() - 1);
		final URI uri2 = URI.createURI(urlString);
		final URI target2 = uri2.appendSegment("models").appendSegment("");
		URIConverter.URI_MAP.put(URI.createURI("pathmap://METRIC_SPEC_MODELS/"), target2);

		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("resourcetype", new XMIResourceFactoryImpl());
		m.put("metricspec", new XMIResourceFactoryImpl());
	}

}
