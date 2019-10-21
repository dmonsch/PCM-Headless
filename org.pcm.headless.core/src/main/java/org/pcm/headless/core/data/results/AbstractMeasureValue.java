package org.pcm.headless.core.data.results;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = DoubleMeasureValue.class, name = "double"),
		@Type(value = LongMeasureValue.class, name = "long") })
public abstract class AbstractMeasureValue {

}