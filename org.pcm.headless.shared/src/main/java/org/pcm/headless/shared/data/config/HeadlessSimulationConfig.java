package org.pcm.headless.shared.data.config;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeadlessSimulationConfig {

	@Builder.Default
	private String experimentName = UUID.randomUUID().toString();

	@Builder.Default
	private long simulationTime = 150000;

	@Builder.Default
	private long maximumMeasurementCount = 10000;

	@Builder.Default
	private boolean useFixedSeed = false;

	@Builder.Default
	private boolean parallelizeRepetitions = false;

	@Builder.Default
	private int repetitions = 1;

}
