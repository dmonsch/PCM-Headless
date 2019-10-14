package org.pcm.headless.core.data.results;

public enum MeasurementType {

	CPU("cpu"), RESPONSE_TIME("response_time");

	private String name;

	private MeasurementType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
