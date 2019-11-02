package org.pcm.headless.core.simulator.simucom;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

public class GradleJarBuilder {

	public static File buildGradleProject(File basePath, Optional<String> javaHome) {
		GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(basePath);
		ProjectConnection connection = connector.connect();
		try {
			BuildLauncher build = connection.newBuild();

			// select tasks to run:
			build.forTasks("clean", "build", "jar");

			// in case you want the build to use java different than default:
			if (javaHome.isPresent()) {
				build.setJavaHome(new File(javaHome.get()));
			}

			// kick the build off:
			build.run();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}

		File outputFolder = new File(basePath, "build" + File.separator + "libs");
		return Stream.of(outputFolder.listFiles()).filter(f -> f.getName().endsWith(".jar")).findFirst()
				.orElseGet(null);
	}

}
