package org.pcm.headless.rest.data;

import lombok.Data;

@Data
public class SimulationStateSummary {

	private String name;
	private int repetitions;
	private int finishedRepetitions;

	private String state;

}
