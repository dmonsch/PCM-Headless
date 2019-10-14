package org.pcm.headless.core.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class ModelUtil {

	public static URI createFileURI(String string) {
		return URI.createFileURI(new File(string).getAbsolutePath());
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

	public static <T extends EObject> void saveToFile(T model, File path) {
		if (path != null) {
			path.getParentFile().mkdirs();
			saveToFile(model, path.getAbsolutePath());
		}
	}

	/**
	 * Saves a model to file
	 * 
	 * @param model model to save
	 * @param path  path for the file
	 */
	public static <T extends EObject> void saveToFile(T model, String path) {
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
			e.printStackTrace();
		}
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

}