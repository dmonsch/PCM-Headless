package org.pcm.headless.core.data;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasuringType;
import org.palladiosimulator.edp2.models.ExperimentData.Run;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.StringMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceReference;
import org.palladiosimulator.pcmmeasuringpoint.EntryLevelSystemCallMeasuringPoint;
import org.pcm.headless.core.util.ReflectionUtil;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;
import org.pcm.headless.shared.data.results.MeasuringPointType;
import org.pcm.headless.shared.data.results.PlainMeasuringPoint;
import org.pcm.headless.shared.data.results.PlainMetricDescription;
import org.pcm.headless.shared.data.results.PlainMetricMeasuringPointBundle;

import com.google.common.collect.Lists;

import de.uka.ipd.sdq.identifier.Identifier;
import lombok.extern.java.Log;

@Log
public class ReflectiveInMemoryRepositoryReader {
	public InMemoryResultRepository convertRepository(Object org) {
		InMemoryResultRepository repo = new InMemoryResultRepository();
		ReflectiveReferences refs = new ReflectiveReferences(org.getClass().getClassLoader());

		for (Object group : ReflectionUtil.asList(ReflectionUtil.safeInvoke(refs.getExperimentGroupsMethod, org))) {
			for (Object setting : ReflectionUtil
					.asList(ReflectionUtil.safeInvoke(refs.getExperimentSettingsMethod, group))) {
				for (Object run : ReflectionUtil
						.asList(ReflectionUtil.safeInvoke(refs.getExperimentRunsMethod, setting))) {
					processExperimentRun(run, repo, org, refs);
				}
			}
		}

		return repo;
	}

	private void processExperimentRun(Object run, InMemoryResultRepository repo, Object org,
			ReflectiveReferences refs) {
		for (Object msrm : ReflectionUtil.asList(ReflectionUtil.safeInvoke(refs.getMeasurementMethod, run))) {
			Object belongingType = ReflectionUtil.safeInvoke(refs.getMeasuringTypeMethod, msrm);
			Object belongingPoint = ReflectionUtil.safeInvoke(refs.getMeasuringPointMethod, belongingType);
			Object belongingMetric = ReflectionUtil.safeInvoke(refs.getMetricMethod, belongingType);

			PlainMeasuringPoint convertedPoint = convertMeasuringPoint(belongingPoint, refs);
			PlainMetricDescription convertedDesc = convertMetricDescription(belongingMetric, refs);

			PlainMetricMeasuringPointBundle bundle = new PlainMetricMeasuringPointBundle();
			bundle.setDesc(convertedDesc);
			bundle.setPoint(convertedPoint);

			// TODO
		}
	}

	private PlainMetricDescription convertMetricDescription(Object belongingMetric,
			org.pcm.headless.core.data.ReflectiveInMemoryRepositoryReader.ReflectiveReferences refs) {
		// TODO Auto-generated method stub
		return null;
	}

	// maybe this should be separated into multiple methods
	private PlainMeasuringPoint convertMeasuringPoint(Object belongingPoint, ReflectiveReferences refs) {
		PlainMeasuringPoint conv = new PlainMeasuringPoint();

		conv.setStringRepresentation(
				(String) ReflectionUtil.safeInvoke(refs.getStringRepresentationMethod, belongingPoint));

		if (refs.entryLevelSystemCallMeasuringPointClass.isAssignableFrom(belongingPoint.getClass())) {
			conv.setType(MeasuringPointType.ENTRY_LEVEL_CALL);
			conv.setSourceId((String) ReflectionUtil.safeStackedInvocation(belongingPoint,
					refs.getEntryLevelSystemCallMethod, refs.getIdMethod));
		} else if (refs.activeResourceMeasuringPointClass.isAssignableFrom(belongingPoint.getClass())) {
			conv.setType(MeasuringPointType.ACTIVE_RESURCE);
			conv.setSourceId((String) ReflectionUtil.safeStackedInvocation(belongingPoint, refs.getActiveResourceMethod,
					refs.getIdMethod));
		} else if (refs.stringMeasuringPointClass.isAssignableFrom(belongingPoint.getClass())) {
			String measuringPointString = (String) ReflectionUtil.safeInvoke(refs.getMeasuringPointStringMethod,
					belongingPoint);
			Pair<List<String>, MeasuringPointType> extractedData = RepositoryReaderUtil
					.extractMeasuringPointDataFromString(measuringPointString);
			conv.setSourceIds(extractedData.getLeft());
			conv.setType(extractedData.getRight());
		} else {
			log.warning(
					"Could not resolve type of measuring point (class = " + belongingPoint.getClass().getName() + ").");
			conv.setType(MeasuringPointType.UNKNOWN);
			conv.setSourceIds(Lists.newArrayList());
		}

		return conv;
	}

	// we also could resolve all this references dynamically at runtime
	// but this would be much slower (but cleaner code)
	// -> maybe i plan to realize this in the future
	private static class ReflectiveReferences {
		// classes
		private Class<?> localMemoryRepositoryClass;
		private Class<?> repositoryClass;
		private Class<?> experimentGroupClass;
		private Class<?> experimentSettingClass;
		private Class<?> runClass;

		private Class<?> measurementClass;
		private Class<?> measuringTypeClass;

		private Class<?> measuringPointClass;

		private Class<?> identifierClass;

		private Class<?> entryLevelSystemCallMeasuringPointClass;
		private Class<?> stringMeasuringPointClass;
		private Class<?> activeResourceMeasuringPointClass;

		private Class<?> activeResourceReferenceClass;

		// methods
		private Method getExperimentGroupsMethod;
		private Method getExperimentSettingsMethod;
		private Method getExperimentRunsMethod;
		private Method getMeasurementMethod;

		private Method getMeasuringTypeMethod;
		private Method getMeasuringPointMethod;
		private Method getMetricMethod;
		private Method getStringRepresentationMethod;

		private Method getEntryLevelSystemCallMethod;
		private Method getMeasuringPointStringMethod;
		private Method getActiveResourceMethod;

		private Method getIdMethod;

		private ReflectiveReferences(ClassLoader loader) {
			// classes
			localMemoryRepositoryClass = ReflectionUtil.getClassFromClassloader(LocalMemoryRepository.class.getName(),
					loader);
			repositoryClass = ReflectionUtil.getClassFromClassloader(Repository.class.getName(), loader);
			experimentGroupClass = ReflectionUtil.getClassFromClassloader(ExperimentGroup.class.getName(), loader);
			experimentSettingClass = ReflectionUtil.getClassFromClassloader(ExperimentSetting.class.getName(), loader);
			runClass = ReflectionUtil.getClassFromClassloader(Run.class.getName(), loader);
			measurementClass = ReflectionUtil.getClassFromClassloader(Measurement.class.getName(), loader);
			measuringTypeClass = ReflectionUtil.getClassFromClassloader(MeasuringType.class.getName(), loader);
			measuringPointClass = ReflectionUtil.getClassFromClassloader(MeasuringPoint.class.getName(), loader);
			entryLevelSystemCallMeasuringPointClass = ReflectionUtil
					.getClassFromClassloader(EntryLevelSystemCallMeasuringPoint.class.getName(), loader);
			identifierClass = ReflectionUtil.getClassFromClassloader(Identifier.class.getName(), loader);
			stringMeasuringPointClass = ReflectionUtil.getClassFromClassloader(StringMeasuringPoint.class.getName(),
					loader);
			activeResourceMeasuringPointClass = ReflectionUtil
					.getClassFromClassloader(ActiveResourceMeasuringPoint.class.getName(), loader);
			activeResourceReferenceClass = ReflectionUtil
					.getClassFromClassloader(ActiveResourceReference.class.getName(), loader);

			// methods
			getExperimentGroupsMethod = ReflectionUtil.getMethodByName(repositoryClass, "getExperimentGroups")
					.orElse(null);
			getExperimentSettingsMethod = ReflectionUtil.getMethodByName(experimentGroupClass, "getExperimentSettings")
					.orElse(null);
			getExperimentRunsMethod = ReflectionUtil.getMethodByName(experimentSettingClass, "getExperimentRuns")
					.orElse(null);
			getMeasurementMethod = ReflectionUtil.getMethodByName(runClass, "getMeasurement").orElse(null);
			getMeasuringTypeMethod = ReflectionUtil.getMethodByName(measurementClass, "getMeasuringType").orElse(null);
			getMeasuringPointMethod = ReflectionUtil.getMethodByName(measuringTypeClass, "getMeasuringPoint")
					.orElse(null);
			getMetricMethod = ReflectionUtil.getMethodByName(measuringTypeClass, "getMetric").orElse(null);
			getStringRepresentationMethod = ReflectionUtil
					.getMethodByName(measuringPointClass, "getStringRepresentation").orElse(null);

			getIdMethod = ReflectionUtil.getMethodByName(identifierClass, "getId").orElse(null);

			getEntryLevelSystemCallMethod = ReflectionUtil
					.getMethodByName(entryLevelSystemCallMeasuringPointClass, "getEntryLevelSystemCall").orElse(null);
			getMeasuringPointStringMethod = ReflectionUtil
					.getMethodByName(stringMeasuringPointClass, "getMeasuringPoint").orElse(null);
			getActiveResourceMethod = ReflectionUtil.getMethodByName(activeResourceReferenceClass, "getActiveResource")
					.orElse(null);

		}
	}

}
