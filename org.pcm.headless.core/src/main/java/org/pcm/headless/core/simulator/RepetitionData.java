package org.pcm.headless.core.simulator;

import java.util.Map;

import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepetitionData {

	private PCMResourceSetPartition pcmPartition;

	private MDSDBlackboard blackboard;

	private Map<String, Object> configurationMap;

	private LocalMemoryRepository repository;

}
