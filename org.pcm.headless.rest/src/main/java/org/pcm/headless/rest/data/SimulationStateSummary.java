package org.pcm.headless.rest.data;

import lombok.Data;

@Data
public class SimulationStateSummary {

	private String id;
	private String name;
	private String simulator;
	private int repetitions;
	private int finishedRepetitions;
	private long simulationTime;
	private long maximumMeasurementCount;

	private String state;

}
