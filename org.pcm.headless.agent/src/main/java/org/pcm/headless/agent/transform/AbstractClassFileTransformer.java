package org.pcm.headless.agent.transform;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public abstract class AbstractClassFileTransformer implements ClassFileTransformer {
	private String name;

	public AbstractClassFileTransformer(String name) {
		this.name = name;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] byteCode = classfileBuffer;

		if (this.name.replaceAll("\\.", "/").equals(className)) {
			try {
				byteCode = transform(loader, name);
			} catch (NotFoundException | IOException | CannotCompileException e) {
				e.printStackTrace();
			}
		}

		return byteCode;
	}

	protected abstract byte[] transform(ClassLoader loader, String className)
			throws NotFoundException, IOException, CannotCompileException;

}
