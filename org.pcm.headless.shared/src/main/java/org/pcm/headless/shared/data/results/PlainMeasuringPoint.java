package org.pcm.headless.shared.data.results;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class PlainMeasuringPoint {

	private String stringRepresentation;
	private MeasuringPointType type;

	private List<String> sourceIds;

	public void setSourceId(String only) {
		this.setSourceIds(Lists.newArrayList(only));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlainMeasuringPoint other = (PlainMeasuringPoint) obj;
		if (sourceIds == null) {
			if (other.sourceIds != null)
				return false;
		} else if (!sourceIds.equals(other.sourceIds))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceIds == null) ? 0 : sourceIds.hashCode());
		return result;
	}

}
