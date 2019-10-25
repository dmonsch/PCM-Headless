package org.pcm.headless.shared.data.results;

import java.util.LinkedList;
import java.util.List;

import org.pcm.headless.shared.data.KeyValuePair;

import lombok.Data;

@Data
public class InMemoryResultRepository {

	private List<KeyValuePair<PlainMetricMeasuringPointBundle, PlainDataSeries>> values;

	public InMemoryResultRepository() {
		this.values = new LinkedList<>();
	}

	public void addDataSeries(PlainMetricMeasuringPointBundle bundle, PlainDataSeries series) {
		this.values.add(new KeyValuePair<>(bundle, series));
	}

}
