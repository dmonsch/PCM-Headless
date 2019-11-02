package org.pcm.headless.core.data;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.pcm.headless.shared.data.results.MeasuringPointType;

import com.google.common.collect.Lists;

import lombok.extern.java.Log;

@Log
public class RepositoryReaderUtil {
	private static final Pattern ASSEMBLY_CTX_PATTERN = Pattern.compile("AssemblyCtx: (.*),");
	private static final Pattern CALL_ID_PATTERN = Pattern.compile("CallID: (.*)>");
	private static final Pattern ENTRY_LEVEL_SYSTEM_PATTERN = Pattern.compile("EntryLevelSystemCall id: (.*) ");

	public static Pair<List<String>, MeasuringPointType> extractMeasuringPointDataFromString(String data) {
		if (data.startsWith("Call ")) {
			Matcher assemblyMatcher = ASSEMBLY_CTX_PATTERN.matcher(data);
			Matcher callIdMatcher = CALL_ID_PATTERN.matcher(data);

			if (assemblyMatcher.find() && callIdMatcher.find()) {
				return Pair.of(Lists.newArrayList(assemblyMatcher.group(1), callIdMatcher.group(1)),
						MeasuringPointType.ASSEMBLY_OPERATION);
			} else {
				return Pair.of(Lists.newArrayList(), MeasuringPointType.ASSEMBLY_OPERATION);
			}
		} else if (data.startsWith("Call_")) {
			Matcher entryLevelSystemMatcher = ENTRY_LEVEL_SYSTEM_PATTERN.matcher(data);
			if (entryLevelSystemMatcher.find()) {
				return Pair.of(Lists.newArrayList(entryLevelSystemMatcher.group(1)),
						MeasuringPointType.ENTRY_LEVEL_CALL);
			} else {
				return Pair.of(Lists.newArrayList(), MeasuringPointType.ENTRY_LEVEL_CALL);
			}
		} else if (data.startsWith("Usage Scenario:")) {
			return Pair.of(Lists.newArrayList(), MeasuringPointType.USAGE_SCENARIO);
		}

		log.warning("Could not parse measuring point described by '" + data + "'.");
		return Pair.of(Lists.newArrayList(), MeasuringPointType.UNKNOWN);
	}

}
