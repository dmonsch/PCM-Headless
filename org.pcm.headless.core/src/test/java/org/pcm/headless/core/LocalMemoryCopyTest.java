package org.pcm.headless.core;

import java.lang.reflect.InvocationTargetException;

import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.palladiosimulator.edp2.models.Repository.RepositoryFactory;
import org.pcm.headless.core.util.ReflectiveEMFTransloaderUtil;

public class LocalMemoryCopyTest {

	public static void main(String[] args)
			throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		LocalMemoryRepository repo = RepositoryFactory.eINSTANCE.createLocalMemoryRepository();
		LocalMemoryRepository copy = RepositoryFactory.eINSTANCE.createLocalMemoryRepository();

		ReflectiveEMFTransloaderUtil.transloadEMFObject(repo, copy, LocalMemoryRepository.class);
	}

}
