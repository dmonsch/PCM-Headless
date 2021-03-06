package org.pcm.headless.api.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.google.common.collect.Lists;

public class ModelUtil {

	public static URI createFileURI(String string) {
		return URI.createFileURI(new File(string).getAbsolutePath());
	}

	public static <T> T readFromURI(String uri, Class<T> clazz) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		URI filePathUri = org.eclipse.emf.common.util.URI.createURI(uri);

		Resource resource = resourceSet.getResource(filePathUri, true);
		return clazz.cast(resource.getContents().get(0));
	}

	/**
	 * Reads a Model from file with a given class
	 * 
	 * @param path  file path
	 * @param clazz model type class
	 * @return parsed model
	 */
	public static <T> T readFromFile(String path, Class<T> clazz) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		URI filePathUri = org.eclipse.emf.common.util.URI.createFileURI(path);

		Resource resource = resourceSet.getResource(filePathUri, true);
		return clazz.cast(resource.getContents().get(0));
	}

	public static <T extends EObject> boolean saveToFile(T model, File path) {
		if (path != null) {
			path.getParentFile().mkdirs();
			return saveToFile(model, path.getAbsolutePath());
		}
		return false;
	}

	/**
	 * Saves a model to file
	 * 
	 * @param model model to save
	 * @param path  path for the file
	 */
	public static <T extends EObject> boolean saveToFile(T model, String path) {
		URI writeModelURI = URI.createFileURI(path);

		final Resource.Factory.Registry resourceRegistry = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> map = resourceRegistry.getExtensionToFactoryMap();
		map.put("*", new XMIResourceFactoryImpl());

		final ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.setResourceFactoryRegistry(resourceRegistry);

		final Resource resource = resourceSet.createResource(writeModelURI);
		resource.getContents().add(model);
		try {
			resource.save(null);
		} catch (final IOException e) {
			return false;
		}
		return true;
	}

	public static boolean validateModelPath(String path, Class<? extends EObject> type) {
		if (path == null || path.isEmpty())
			return true;
		File file = new File(path);
		if (file.exists()) {
			try {
				EObject res = ModelUtil.readFromFile(path, type);
				if (res != null && type.isInstance(res)) {
					return true;
				}
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <T extends EObject> List<T> getObjects(final EObject pcmModel, final Class<T> type) {
		List<T> results = new ArrayList<>();
		TreeIterator<EObject> it = pcmModel.eAllContents();
		while (it.hasNext()) {
			EObject eo = it.next();
			if (type.isInstance(eo)) {
				results.add((T) eo);
			}
		}
		return results;
	}

	public static List<EObject> getAllObjects(final EObject parent) {
		List<EObject> results = Lists.newArrayList(parent);
		TreeIterator<EObject> it = parent.eAllContents();
		while (it.hasNext()) {
			EObject eo = it.next();
			results.add(eo);
		}
		return results;
	}

	public static Collection<EObject> getObjectsExactType(EObject parent, Class<? extends EObject> class1) {
		List<EObject> results = new ArrayList<>();
		TreeIterator<EObject> it = parent.eAllContents();
		while (it.hasNext()) {
			EObject eo = it.next();
			if (eo.getClass().equals(class1)) {
				results.add(eo);
			}
		}

		if (parent.getClass().equals(class1)) {
			results.add(parent);
		}

		return results;
	}

}