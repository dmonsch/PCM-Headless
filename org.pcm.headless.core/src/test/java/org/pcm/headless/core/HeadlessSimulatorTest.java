package org.pcm.headless.core;

import java.io.File;

import org.pcm.headless.shared.data.config.HeadlessModelConfig;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

public class HeadlessSimulatorTest {

	public static void main(String[] args) {
		HeadlessModelConfig conf1 = new HeadlessModelConfig();
		conf1.setAllocationFile(new File("examples/cocome/cocome.allocation"));
		conf1.setMonitorRepository(new File("examples/cocome/cocome.monitorrepository"));
		conf1.setRepositoryFiles(Lists.newArrayList(new File("examples/cocome/cocome.repository"),
				new File("examples/cocome/cocomeTypes.repository")));
		conf1.setResourceEnvironmentFile(new File("examples/cocome/cocome.resourceenvironment"));
		conf1.setSystemFile(new File("examples/cocome/cocome.system"));
		conf1.setUsageFile(new File("examples/cocome/cocome.usagemodel"));
		conf1.setAdditionals(Lists.newArrayList(new File("examples/cocome/cocome.measuringpoint")));

		HeadlessSimulationConfig conf2 = HeadlessSimulationConfig.builder().build();

		HeadlessPalladioSimulator sim = new HeadlessPalladioSimulator();
		InMemoryResultRepository repo = sim.triggerSimulation(conf1, conf2);

		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writeValueAsString(repo));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.exit(0);

	}

}
