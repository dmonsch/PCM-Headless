package org.pcm.headless.shared.data.results;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LongMeasureValue extends AbstractMeasureValue {

	private long v;

}
