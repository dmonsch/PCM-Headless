package org.pcm.headless.agent.transform.impl;

import java.io.IOException;

import org.pcm.headless.agent.transform.AbstractClassFileTransformer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class XTendTransformTransformer extends AbstractClassFileTransformer {

	public XTendTransformTransformer() {
		super("de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.XtendTransformPCMToCodeJob");
	}

	@Override
	protected byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get(className);

		CtMethod m = cc.getDeclaredMethod("getBasePath");

		m.setBody("{"
				+ "java.io.File dataFolder = new java.io.File(new java.io.File((java.lang.String) this.configuration.getCodeGenerationRequiredBundles().get(this.configuration.getCodeGenerationRequiredBundles().size() - 1)), \"src\" + java.io.File.separator + \"main\" + java.io.File.separator + \"java\");"
				+ "dataFolder.mkdirs();" + "return dataFolder.getAbsolutePath();" + "}");

		byte[] byteCode = cc.toBytecode();
		cc.detach();
		return byteCode;
	}

}
