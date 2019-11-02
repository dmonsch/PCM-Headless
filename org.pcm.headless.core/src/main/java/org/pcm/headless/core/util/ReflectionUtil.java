package org.pcm.headless.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

public class ReflectionUtil {

	@SuppressWarnings("unchecked")
	public static List<Object> asList(Object res) {
		if (List.class.isAssignableFrom(res.getClass())) {
			return (List<Object>) res;
		}
		return Lists.newArrayList();
	}

	public static Object safeStackedInvocation(Object target, Method... meths) {
		for (Method meth : meths) {
			target = safeInvoke(meth, target);
		}
		return target;
	}

	public static Object safeInvoke(Method meth, Object obj, Object... args) {
		try {
			return meth.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Object directlyInvokeMethod(Class<?> clazz, String name, Object obj, boolean direct, Object... args) {
		Method meth;
		if (direct) {
			try {
				meth = clazz.getDeclaredMethod(name);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			meth = Stream.of(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElse(null);
		}

		try {
			return meth.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Optional<Method> getMethodByName(Class<?> clazz, String name) {
		return Stream.of(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst();
	}

	public static Class<?> getClassFromClassloader(String className, ClassLoader loader) {
		try {
			return Class.forName(className, true, loader);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}
