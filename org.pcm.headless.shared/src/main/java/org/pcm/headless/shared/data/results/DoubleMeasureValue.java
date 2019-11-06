package org.pcm.headless.shared.data.results;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DoubleMeasureValue extends AbstractMeasureValue {

	private double v;

}
