package org.pcm.headless.shared.data.results;

public enum MeasuringPointType {

	ENTRY_LEVEL_CALL("entry"), ACTIVE_RESURCE("activer"), LINKING_RESURCE("linkr"), ASSEMBLY_OPERATION("ass_op"),
	EXTERNAL_CALL("ext_call");

	private String name;

	private MeasuringPointType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
