package org.pcm.headless.core.data.results;

import lombok.Data;

@Data
public class PlainMetricMeasuringPointBundle {

	private PlainMetricDescription desc;
	private PlainMeasuringPoint point;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlainMetricMeasuringPointBundle other = (PlainMetricMeasuringPointBundle) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((point == null) ? 0 : point.hashCode());
		return result;
	}

}
