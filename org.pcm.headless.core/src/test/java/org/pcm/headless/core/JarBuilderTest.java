package org.pcm.headless.core;

import java.io.File;
import java.util.Optional;

import org.pcm.headless.core.simulator.simucom.GradleJarBuilder;

public class JarBuilderTest {

	public static void main(String[] args) {
		GradleJarBuilder.buildGradleProject(new File(
				"/Users/david/Desktop/PCM-Headless/git/org.pcm.headless.rest/SimulationData/2ae5f895-db8a-4cf4-9d05-1d59d6fba6f8/simucom"),
				Optional.of("/Library/Java/JavaVirtualMachines/jdk-12.0.2.jdk/Contents/Home"));
	}

}
