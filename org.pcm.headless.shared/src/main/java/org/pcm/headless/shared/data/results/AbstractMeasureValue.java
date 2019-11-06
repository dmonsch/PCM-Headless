package org.pcm.headless.shared.data.results;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "t")
@JsonSubTypes({ @Type(value = DoubleMeasureValue.class, name = "d"),
		@Type(value = LongMeasureValue.class, name = "l") })
public abstract class AbstractMeasureValue {
}