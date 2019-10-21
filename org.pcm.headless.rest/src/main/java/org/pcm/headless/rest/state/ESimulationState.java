package org.pcm.headless.rest.state;

public enum ESimulationState {
	READY("ready"), QUEUED("queued"), RUNNING("running"), EXECUTED("executed"), FINISHED("finished");

	private String name;

	private ESimulationState(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
