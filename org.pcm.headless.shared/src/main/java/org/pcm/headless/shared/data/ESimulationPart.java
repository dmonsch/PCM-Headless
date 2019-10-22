package org.pcm.headless.shared.data;

public enum ESimulationPart {
	REPOSITORY("repository"), SYSTEM("system"), USAGE_MODEL("usagemodel"), ALLOCATION("allocation"),
	RESOURCE_ENVIRONMENT("resourceenv"), MONITOR_REPOSITORY("monitor"), ADDITIONAL("addit");

	private String string;

	private ESimulationPart(String data) {
		this.string = data;
	}

	@Override
	public String toString() {
		return this.string;
	}

	public static ESimulationPart fromString(String text) {
		for (ESimulationPart b : ESimulationPart.values()) {
			if (b.string.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}