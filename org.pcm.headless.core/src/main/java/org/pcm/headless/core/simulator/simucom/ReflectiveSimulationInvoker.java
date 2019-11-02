package org.pcm.headless.core.simulator.simucom;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.UUID;

import org.palladiosimulator.edp2.dao.Edp2Dao;
import org.palladiosimulator.recorderframework.edp2.config.AbstractEDP2RecorderConfigurationFactory;
import org.pcm.headless.core.util.ReflectionUtil;

public class ReflectiveSimulationInvoker {
	private static final String AGENT_MAIN_CLASS = "agent.main.Initializer";

	private File jar;

	private Object localMemoryRepository;
	private URLClassLoader classloader;

	public ReflectiveSimulationInvoker(File simuComJar) {
		this.jar = simuComJar;
	}

	public void invokeSimulation(Map<String, Object> configMap) {
		classloader = prepareClassloader(this.jar);

		// generate id
		String repositoryId = UUID.randomUUID().toString();

		// get classes
		Class<?> initializerClass = ReflectionUtil.getClassFromClassloader(AGENT_MAIN_CLASS, classloader);

		// init agent
		ReflectionUtil.directlyInvokeMethod(initializerClass, "initialize", null, true);
		localMemoryRepository = ReflectionUtil.directlyInvokeMethod(initializerClass, "createRepository", null, false,
				repositoryId, "MyDomain");

		// set repository id
		configMap.put(AbstractEDP2RecorderConfigurationFactory.REPOSITORY_ID, repositoryId);

		// start simulation
		ReflectionUtil.directlyInvokeMethod(initializerClass, "triggerSimulation", null, false, localMemoryRepository,
				configMap);
	}

	public Object getResultRepository() {
		return localMemoryRepository;
	}

	public void close() {
		Class<?> initializerClass = ReflectionUtil.getClassFromClassloader(AGENT_MAIN_CLASS, classloader);
		Class<?> edp2DaoClass = ReflectionUtil.getClassFromClassloader(Edp2Dao.class.getName(), classloader);

		ReflectionUtil.directlyInvokeMethod(edp2DaoClass, "close", localMemoryRepository, true);
		ReflectionUtil.directlyInvokeMethod(initializerClass, "removeRepository", null, false, localMemoryRepository);

		// close whole classloader
		try {
			classloader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private URLClassLoader prepareClassloader(File jar) {
		try {
			return new URLClassLoader(new URL[] { jar.toURI().toURL() },
					ReflectiveSimulationInvoker.class.getClass().getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

}
