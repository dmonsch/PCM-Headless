package org.pcm.headless.agent.core;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import org.pcm.headless.agent.transform.impl.ExtensionHelperHook;
import org.pcm.headless.agent.transform.impl.PreferencesHelperTransformer;
import org.pcm.headless.agent.transform.impl.RecorderExtensionHelperTransformer;
import org.pcm.headless.agent.transform.impl.SchedulingFactoryTransformer;
import org.pcm.headless.agent.transform.impl.XTendTransformTransformer;

/**
 * Java agent entry point which is used to delegate the transformation of the
 * Eclipse plugin classes.
 * 
 * @author David Monschein
 *
 */
@SuppressWarnings("deprecation")
public class EclipsePluginTransformerAgent {
	private final Instrumentation instrumentation;

	private static Map<String, ClassFileTransformer> classTransformerMapping = new HashMap<>();

	static {
		classTransformerMapping.put("de.uka.ipd.sdq.simulation.preferences.SimulationPreferencesHelper",
				new PreferencesHelperTransformer());
		classTransformerMapping.put("org.palladiosimulator.commons.eclipseutils.ExtensionHelper",
				new ExtensionHelperHook());
		classTransformerMapping.put("de.uka.ipd.sdq.scheduler.factory.SchedulingFactory",
				new SchedulingFactoryTransformer());
		classTransformerMapping.put("org.palladiosimulator.recorderframework.utils.RecorderExtensionHelper",
				new RecorderExtensionHelperTransformer());
		classTransformerMapping.put("de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.XtendTransformPCMToCodeJob",
				new XTendTransformTransformer());
	}

	/**
	 * @param agentArgs
	 * @param instrumentation
	 */
	public static void premain(String agentArgs, Instrumentation instrumentation) {
		internal(agentArgs, instrumentation, false);
	}

	/**
	 * @param agentArgs
	 * @param instrumentation
	 */
	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		internal(agentArgs, instrumentation, true);
	}

	/**
	 * @param agentArgs
	 * @param instrumentation
	 * @param preStarted
	 */
	private static void internal(String agentArgs, Instrumentation instrumentation, boolean preStarted) {
		EclipsePluginTransformerAgent agent = new EclipsePluginTransformerAgent(instrumentation);
		agent.load();
	}

	/**
	 * @param agentArgs
	 * @param instrumentation
	 * @param preStarted
	 */
	public EclipsePluginTransformerAgent(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	private void load() {
		classTransformerMapping.entrySet().forEach(et -> {
			transformClass(et.getKey(), instrumentation, et.getValue());
		});
	}

	private void transformClass(String className, Instrumentation instrumentation, ClassFileTransformer transformer) {
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;
		// see if we can get the class using forName
		try {
			targetCls = Class.forName(className);
			targetClassLoader = targetCls.getClassLoader();
			transform(targetCls, targetClassLoader, instrumentation, transformer);
			return;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// otherwise iterate all loaded classes and find what we want
		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazz.getName().equals(className)) {
				targetCls = clazz;
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation, transformer);
				return;
			}
		}
		throw new RuntimeException("Failed to find class [" + className + "]");
	}

	private void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation,
			ClassFileTransformer transformer) {
		instrumentation.addTransformer(transformer, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
		}
	}

}
