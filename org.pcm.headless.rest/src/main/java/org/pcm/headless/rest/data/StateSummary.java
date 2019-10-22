package org.pcm.headless.rest.data;

import java.util.List;

import lombok.Data;

@Data
public class StateSummary {

	private List<SimulationStateSummary> simulations;

}
