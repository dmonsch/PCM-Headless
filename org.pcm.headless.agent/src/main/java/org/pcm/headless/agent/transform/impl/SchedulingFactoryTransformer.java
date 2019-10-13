package org.pcm.headless.agent.transform.impl;

import java.io.IOException;

import org.pcm.headless.agent.transform.AbstractClassFileTransformer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * This transformer has only debug purposes. So it could be removed.
 * 
 * @deprecated
 * @author David Monschein
 *
 */
public class SchedulingFactoryTransformer extends AbstractClassFileTransformer {
	public SchedulingFactoryTransformer() {
		super("de.uka.ipd.sdq.scheduler.factory.SchedulingFactory");
	}

	@Override
	protected byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get(className);

		CtMethod m = cc.getDeclaredMethod("getSchedulerExtensionFactory");
		m.insertBefore("System.out.println(extensionId);");

		byte[] byteCode = cc.toBytecode();
		cc.detach();

		return byteCode;
	}

}
