package org.pcm.headless.core.util;

import java.util.Map;

import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import com.google.common.collect.Maps;

import de.uka.ipd.sdq.simulation.AbstractSimulationConfig;

public class HeadlessSimulationConfigUtil {

	private static final String EDP2_RECORDER = "Experiment Data Persistency &amp; Presentation (EDP2)";

	public static Map<String, Object> convertToConfigMap(HeadlessSimulationConfig config) {

		Map<String, Object> configMap = Maps.newHashMap();
		configMap.put(AbstractSimulationConfig.VERBOSE_LOGGING, false);
		configMap.put(AbstractSimulationConfig.VARIATION_ID, AbstractSimulationConfig.DEFAULT_VARIATION_NAME);
		configMap.put(AbstractSimulationConfig.SIMULATOR_ID, AbstractSimulationConfig.DEFAULT_SIMULATOR_ID);
		configMap.put(AbstractSimulationConfig.EXPERIMENT_RUN, config.getExperimentName());
		configMap.put(AbstractSimulationConfig.SIMULATION_TIME, String.valueOf(config.getSimulationTime()));
		configMap.put(AbstractSimulationConfig.MAXIMUM_MEASUREMENT_COUNT,
				String.valueOf(config.getMaximumMeasurementCount()));
		configMap.put(AbstractSimulationConfig.PERSISTENCE_RECORDER_NAME, EDP2_RECORDER);
		configMap.put(AbstractSimulationConfig.USE_FIXED_SEED, config.isUseFixedSeed());

		return configMap;

	}

}
