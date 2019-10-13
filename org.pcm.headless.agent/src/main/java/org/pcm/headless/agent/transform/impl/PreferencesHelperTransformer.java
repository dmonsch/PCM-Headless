package org.pcm.headless.agent.transform.impl;

import java.io.IOException;

import org.pcm.headless.agent.transform.AbstractClassFileTransformer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PreferencesHelperTransformer extends AbstractClassFileTransformer {
	private static final String DEFAULT_SIMULATION_ENGINE = "de.uka.ipd.sdq.simucomframework.desmoj.engine1";
	private static final String DEFAULT_SIMULATION_ENGINE_FACTORY = "de.uka.ipd.sdq.simulation.abstractsimengine.desmoj.DesmoJSimEngineFactory";

	public PreferencesHelperTransformer() {
		super("de.uka.ipd.sdq.simulation.preferences.SimulationPreferencesHelper");
	}

	@Override
	protected byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get(className);

		CtMethod m = cc.getDeclaredMethod("getDefaultEngineId");
		m.setBody("{" + "return \"" + DEFAULT_SIMULATION_ENGINE + "\";" + "}");

		CtMethod m2 = cc.getDeclaredMethod("getPreferredSimulationEngine");
		m2.setBody("{" + "return new " + DEFAULT_SIMULATION_ENGINE_FACTORY + "();" + "}");

		byte[] res = cc.toBytecode();
		cc.detach();

		return res;
	}

}
