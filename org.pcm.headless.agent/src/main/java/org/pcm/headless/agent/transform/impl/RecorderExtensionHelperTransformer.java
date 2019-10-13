package org.pcm.headless.agent.transform.impl;

import java.io.IOException;

import org.pcm.headless.agent.transform.AbstractClassFileTransformer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class RecorderExtensionHelperTransformer extends AbstractClassFileTransformer {
	private static final String RECORDER_CLASS_NAME = "org.palladiosimulator.recorderframework.edp2.config.EDP2RecorderConfigurationFactory";

	public RecorderExtensionHelperTransformer() {
		super("org.palladiosimulator.recorderframework.utils.RecorderExtensionHelper");
	}

	@Override
	protected byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get(className);

		CtMethod m = cc.getDeclaredMethod("getRecorderConfigurationFactoryForName");
		m.setBody("{" + "return new " + RECORDER_CLASS_NAME + "();" + "}");

		byte[] byteCode = cc.toBytecode();
		cc.detach();

		return byteCode;
	}

}
