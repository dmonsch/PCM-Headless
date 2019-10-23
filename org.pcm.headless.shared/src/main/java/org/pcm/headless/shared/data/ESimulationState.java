package org.pcm.headless.shared.data;

public enum ESimulationState {
	READY("ready"), QUEUED("queued"), RUNNING("running"), EXECUTED("executed"), FINISHED("finished");

	private String name;

	private ESimulationState(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ESimulationState fromString(String text) {
		for (ESimulationState b : ESimulationState.values()) {
			if (b.name.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}
