package org.pcm.headless.shared.data.results;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.pcm.headless.shared.data.KeyValuePair;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class InMemoryResultRepository {

	private List<KeyValuePair<PlainMetricMeasuringPointBundle, List<PlainDataSeries>>> values;

	public InMemoryResultRepository() {
		this.values = new LinkedList<>();
	}

	public void addDataSeries(PlainMetricMeasuringPointBundle bundle, PlainDataSeries series) {
		this.addMultipleDataSeries(bundle, Lists.newArrayList(series));
	}

	public void addMultipleDataSeries(PlainMetricMeasuringPointBundle bundle, List<PlainDataSeries> series) {
		Optional<KeyValuePair<PlainMetricMeasuringPointBundle, List<PlainDataSeries>>> exPoint = this.values.stream()
				.filter(val -> {
					return val.getKey().equals(bundle);
				}).findFirst();
		if (exPoint.isPresent()) {
			exPoint.get().getValue().addAll(series);
		} else {
			this.values.add(new KeyValuePair<>(bundle, series));
		}
	}

	public void merge(InMemoryResultRepository other) {
		other.values.forEach(v -> {
			addMultipleDataSeries(v.getKey(), v.getValue());
		});
	}

}
