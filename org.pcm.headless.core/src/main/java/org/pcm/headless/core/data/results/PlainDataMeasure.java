package org.pcm.headless.core.data.results;

import lombok.Data;

@Data
public class PlainDataMeasure {

	private String unit;
	private AbstractMeasureValue value;

}
