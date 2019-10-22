package org.pcm.headless.shared.data.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class InMemoryResultRepository {

	private Map<PlainMetricMeasuringPointBundle, List<PlainDataSeries>> values;

	public InMemoryResultRepository() {
		this.values = new HashMap<>();
	}

	public void addDataSeries(PlainMetricMeasuringPointBundle bundle, PlainDataSeries series) {
		if (this.values.containsKey(bundle)) {
			this.values.get(bundle).add(series);
		} else {
			this.values.put(bundle, Lists.newArrayList(series));
		}
	}

}
