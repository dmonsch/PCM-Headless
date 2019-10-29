package org.pcm.headless.shared.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ESimulationType {
	SIMULIZAR("SimuLizar"), SIMUCOM("SimuCom");

	private String name;

	private ESimulationType(String name) {
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	@JsonCreator
	public static ESimulationType fromString(String text) {
		for (ESimulationType b : ESimulationType.values()) {
			if (b.name.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}
