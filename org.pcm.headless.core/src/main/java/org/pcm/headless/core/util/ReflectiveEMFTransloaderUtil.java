package org.pcm.headless.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public class ReflectiveEMFTransloaderUtil {

	public static <T extends EObject> T transloadEMFObject(Object emf, T base, Class<T> clazz)
			throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Map<Object, EObject> convertedObjects = new HashMap<>();

		// we need the eobject from the other classloader
		Class<?> eObjectClass = Class.forName(EObject.class.getName(), true, emf.getClass().getClassLoader());
		Class<?> currentClass = emf.getClass();
		while (!currentClass.getName().equals(EObject.class.getName()) && currentClass.getSuperclass() != null) {
			currentClass = currentClass.getSuperclass();

			for (Method method : currentClass.getDeclaredMethods()) {
				if (method.getName().startsWith("get")) {
					method.setAccessible(true);
					Object res = method.invoke(emf);
					System.out.println(method.getName());
					if (res != null) {
						System.out.println(res.getClass().getName());
					}
				}
			}
		}

		return null;
	}

}
