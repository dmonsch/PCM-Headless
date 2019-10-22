package org.pcm.headless.shared.data.results;

import lombok.Data;

@Data
public class PlainDataMeasure {

	private String unit;
	private AbstractMeasureValue value;

}
