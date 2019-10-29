package org.pcm.headless.core.data;

import java.util.List;
import java.util.stream.Collectors;

import javax.measure.unit.UnitFormat;

import org.palladiosimulator.edp2.dao.MeasurementsDaoRegistry;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.DoubleBinaryMeasurements;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentRun;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.ExperimentData.LongBinaryMeasurements;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.EntryLevelSystemCallMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.LinkingResourceMeasuringPoint;
import org.pcm.headless.shared.data.results.AbstractMeasureValue;
import org.pcm.headless.shared.data.results.DoubleMeasureValue;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;
import org.pcm.headless.shared.data.results.LongMeasureValue;
import org.pcm.headless.shared.data.results.MeasuringPointType;
import org.pcm.headless.shared.data.results.PlainDataMeasure;
import org.pcm.headless.shared.data.results.PlainDataSeries;
import org.pcm.headless.shared.data.results.PlainMeasuringPoint;
import org.pcm.headless.shared.data.results.PlainMetricDescription;
import org.pcm.headless.shared.data.results.PlainMetricMeasuringPointBundle;

import com.google.common.collect.Lists;

import lombok.extern.java.Log;

@Log
public class InMemoryRepositoryReader {

	public InMemoryResultRepository converRepository(LocalMemoryRepository org) {
		InMemoryResultRepository repo = new InMemoryResultRepository();

		for (ExperimentGroup group : org.getExperimentGroups()) {
			for (ExperimentSetting setting : group.getExperimentSettings()) {
				for (ExperimentRun run : setting.getExperimentRuns()) {
					processExperimentRun(run, repo, org);
				}
			}
		}

		return repo;
	}

	private void processExperimentRun(ExperimentRun run, InMemoryResultRepository result, LocalMemoryRepository org) {
		for (Measurement msrm : run.getMeasurement()) {
			MeasuringPoint belongingPoint = msrm.getMeasuringType().getMeasuringPoint();
			MetricDescription metricDesc = msrm.getMeasuringType().getMetric();

			PlainMeasuringPoint convertedPoint = convertMeasuringPoint(belongingPoint);
			PlainMetricDescription convertedDesc = convertMetricDescription(metricDesc);

			PlainMetricMeasuringPointBundle bundle = new PlainMetricMeasuringPointBundle();
			bundle.setDesc(convertedDesc);
			bundle.setPoint(convertedPoint);

			for (MeasurementRange range : msrm.getMeasurementRanges()) {
				for (DataSeries series : range.getRawMeasurements().getDataSeries()) {
					PlainDataSeries plainSeries = convertDataSeries(series, org);
					result.addDataSeries(bundle, plainSeries);
				}
			}
		}

	}

	private PlainDataSeries convertDataSeries(DataSeries series, LocalMemoryRepository org) {
		MeasurementsDaoRegistry reg = org.getMeasurementsDaoFactory().getDaoRegistry();
		if (!reg.getRegisteredUuids().contains(series.getValuesUuid())) {
			return null;
		}

		PlainDataSeries output = new PlainDataSeries();

		List<PlainDataMeasure> measures = reg.getMeasurementsDao(series.getValuesUuid()).getMeasurements().stream()
				.map(m -> {
					String unit = UnitFormat.getInstance().format(m.getUnit());

					PlainDataMeasure measure = new PlainDataMeasure();
					measure.setUnit(unit);
					measure.setValue(convertMeasureValue(m.getValue(), series.getClass()));

					return measure;
				}).collect(Collectors.toList());
		output.setMeasures(measures);

		return output;
	}

	private AbstractMeasureValue convertMeasureValue(Object value, Class<? extends DataSeries> class1) {
		if (DoubleBinaryMeasurements.class.isAssignableFrom(class1)) {
			DoubleMeasureValue val = new DoubleMeasureValue();
			val.setValue((double) value);
			return val;
		} else if (LongBinaryMeasurements.class.isAssignableFrom(class1)) {
			LongMeasureValue val = new LongMeasureValue();
			val.setValue((long) value);
			return val;
		} else {
			log.warning("Could not parse results of type " + class1.getName() + "!");
		}
		return null;
	}

	private PlainMetricDescription convertMetricDescription(MetricDescription metricDesc) {
		PlainMetricDescription conv = new PlainMetricDescription();

		conv.setId(metricDesc.getId());
		conv.setTextual(metricDesc.getTextualDescription());

		return conv;
	}

	private PlainMeasuringPoint convertMeasuringPoint(MeasuringPoint belongingPoint) {
		PlainMeasuringPoint conv = new PlainMeasuringPoint();
		conv.setStringRepresentation(belongingPoint.getStringRepresentation());

		java.lang.System.out.println(belongingPoint.getClass().getName());

		if (belongingPoint instanceof EntryLevelSystemCallMeasuringPoint) {
			conv.setType(MeasuringPointType.ENTRY_LEVEL_CALL);
			conv.setSourceId(((EntryLevelSystemCallMeasuringPoint) belongingPoint).getEntryLevelSystemCall().getId());
		} else if (belongingPoint instanceof ActiveResourceMeasuringPoint) {
			conv.setType(MeasuringPointType.ACTIVE_RESURCE);
			conv.setSourceId(((ActiveResourceMeasuringPoint) belongingPoint).getActiveResource().getId());
		} else if (belongingPoint instanceof LinkingResourceMeasuringPoint) {
			conv.setType(MeasuringPointType.LINKING_RESURCE);
			conv.setSourceId(((LinkingResourceMeasuringPoint) belongingPoint).getLinkingResource().getId());
		} else if (belongingPoint instanceof AssemblyOperationMeasuringPoint) {
			conv.setType(MeasuringPointType.ASSEMBLY_OPERATION);
			conv.setSourceIds(
					Lists.newArrayList(((AssemblyOperationMeasuringPoint) belongingPoint).getAssembly().getId(),
							((AssemblyOperationMeasuringPoint) belongingPoint).getOperationSignature().getId(),
							((AssemblyOperationMeasuringPoint) belongingPoint).getRole().getId()));
		} else if (belongingPoint instanceof ExternalCallActionMeasuringPoint) {
			conv.setType(MeasuringPointType.EXTERNAL_CALL);
			conv.setSourceId(((ExternalCallActionMeasuringPoint) belongingPoint).getExternalCall().getId());
		} else {
			log.warning(
					"Could not resolve type of measuring point (class = " + belongingPoint.getClass().getName() + ").");
		}

		return conv;
	}

}
