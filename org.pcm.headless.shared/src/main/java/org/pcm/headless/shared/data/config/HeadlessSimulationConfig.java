package org.pcm.headless.shared.data.config;

import java.util.UUID;

import org.pcm.headless.shared.data.ESimulationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

	@Builder.Default
	private ESimulationType type = ESimulationType.SIMULIZAR;

}
