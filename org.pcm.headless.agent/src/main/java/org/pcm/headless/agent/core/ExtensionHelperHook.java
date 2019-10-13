package org.pcm.headless.agent.core;

import java.io.IOException;

import org.pcm.headless.agent.transform.AbstractClassFileTransformer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class ExtensionHelperHook extends AbstractClassFileTransformer {
	private static final String EDP2_RAW_CONVERTER = "org.palladiosimulator.recorderframework.edp2.EDP2RawRecorder";

	public ExtensionHelperHook() {
		super("org.palladiosimulator.commons.eclipseutils.ExtensionHelper");
	}

	@Override
	protected byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get(className);

		CtClass string = cp.get("java.lang.String");

		CtMethod m2 = cc.getDeclaredMethod("getExecutableExtension",
				new CtClass[] { string, string, string, string, string });
		m2.insertBefore(generateIfExtensionPoint("org.palladiosimulator.recorderframework", "recorder",
				"recorderImplementation", "name", "Experiment Data Persistency &amp; Presentation (EDP2)")
				+ "return new " + EDP2_RAW_CONVERTER + "();" + "}");

		byte[] byteCode = cc.toBytecode();
		cc.detach();

		return byteCode;
	}

	private String generateIfExtensionPoint(String extensionPointID, String elementName, String attributeName,
			String filterAttributeName, String filterAttributeValue) {
		return "if (extensionPointID.equals(\"" + extensionPointID + "\") && elementName.equals(\"" + elementName
				+ "\") && " + "attributeName.equals(\"" + attributeName + "\") && filterAttributeName.equals(\""
				+ filterAttributeName + "\") &&" + "filterAttributeValue.equals(\"" + filterAttributeValue + "\")) {";
	}

}
