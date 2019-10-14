package org.pcm.headless.core.config;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import de.uka.ipd.sdq.simulation.AbstractSimulationConfig;
import lombok.Data;

@Data
public class HeadlessSimulationConfig {
	private static final String EDP2_RECORDER = "Experiment Data Persistency &amp; Presentation (EDP2)";

	private String experimentName = UUID.randomUUID().toString();
	private long simulationTime = 150000;
	private long maximumMeasurementCount = 10000;
	private boolean useFixedSeed = false;
	private boolean parallelizeRepetitions = false;

	private int repetitions;

	public Map<String, Object> convertToConfigMap() {

		Map<String, Object> configMap = Maps.newHashMap();
		configMap.put(AbstractSimulationConfig.VERBOSE_LOGGING, false);
		configMap.put(AbstractSimulationConfig.VARIATION_ID, AbstractSimulationConfig.DEFAULT_VARIATION_NAME);
		configMap.put(AbstractSimulationConfig.SIMULATOR_ID, AbstractSimulationConfig.DEFAULT_SIMULATOR_ID);
		configMap.put(AbstractSimulationConfig.EXPERIMENT_RUN, this.experimentName);
		configMap.put(AbstractSimulationConfig.SIMULATION_TIME, this.simulationTime);
		configMap.put(AbstractSimulationConfig.MAXIMUM_MEASUREMENT_COUNT, this.maximumMeasurementCount);
		configMap.put(AbstractSimulationConfig.PERSISTENCE_RECORDER_NAME, EDP2_RECORDER);
		configMap.put(AbstractSimulationConfig.USE_FIXED_SEED, this.useFixedSeed);

		return configMap;

	}

}
