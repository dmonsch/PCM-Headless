package org.pcm.headless.api;

import java.io.File;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.CompositionFactory;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.system.SystemFactory;
import org.pcm.headless.api.client.transform.DefaultModelFileNameGenerator;
import org.pcm.headless.api.client.transform.TransitiveModelTransformer;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.PCMUtil;

import com.google.common.collect.Lists;

public class TransformerTest {

	public static void main(String[] args) {
		PCMUtil.loadPCMModels();

		File allocationFile = new File("examples/cocome/cocome.allocation");
		File repositoryFile = new File("examples/cocome/cocome.repository");
		File resourceEnvironmentFile = new File("examples/cocome/cocome.resourceenvironment");
		File systemFile = new File("examples/cocome/cocome.system");
		File usageFile = new File("examples/cocome/cocome.usagemodel");
		File monitorRepositoryFile = new File("examples/cocome/cocome_gen.monitorrepository");

		List<File> modelFiles = Lists.newArrayList(allocationFile, repositoryFile, resourceEnvironmentFile, systemFile,
				usageFile, monitorRepositoryFile);
		EObject[] objs = modelFiles.stream().map(f -> ModelUtil.readFromFile(f.getAbsolutePath(), EObject.class))
				.toArray(EObject[]::new);

		TransitiveModelTransformer trans2 = new TransitiveModelTransformer(objs);
		trans2.buildTransitiveClosure();
		trans2.buildModels(new DefaultModelFileNameGenerator());

	}

	private static void test(File repositoryFile) {

		Repository repo = ModelUtil.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
		Repository repo2 = ModelUtil.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
		System.out.println(new EcoreUtil.EqualityHelper().equals(repo, repo2));

		// build programmatically test
		Repository tRepo = RepositoryFactory.eINSTANCE.createRepository();
		BasicComponent comp = RepositoryFactory.eINSTANCE.createBasicComponent();
		comp.setEntityName("Test");
		tRepo.getComponents__Repository().add(comp);

		System.out.println(tRepo.eResource());

		org.palladiosimulator.pcm.system.System tSystem = SystemFactory.eINSTANCE.createSystem();
		AssemblyContext nAssembly = CompositionFactory.eINSTANCE.createAssemblyContext();
		nAssembly.setEncapsulatedComponent__AssemblyContext(comp);
		tSystem.getAssemblyContexts__ComposedStructure().add(nAssembly);

		AssemblyContext nAssembly2 = CompositionFactory.eINSTANCE.createAssemblyContext();
		nAssembly2.setEncapsulatedComponent__AssemblyContext(comp);
		tSystem.getAssemblyContexts__ComposedStructure().add(nAssembly2);

		org.palladiosimulator.pcm.system.System tSystem2 = SystemFactory.eINSTANCE.createSystem();
		AssemblyContext nAssembly3 = CompositionFactory.eINSTANCE.createAssemblyContext();
		nAssembly3.setEncapsulatedComponent__AssemblyContext(comp);
		tSystem2.getAssemblyContexts__ComposedStructure().add(nAssembly3);

		ModelUtil.saveToFile(tSystem, "model1.model");
		ModelUtil.saveToFile(tSystem2, "model2.model");
		// ModelUtil.saveToFile(tRepo, "model0.model");
		ModelUtil.saveToFile(tSystem, "model1.model");
		ModelUtil.saveToFile(tSystem2, "model2.model");
		ModelUtil.saveToFile(tRepo, "model0.model");

		EObject read = ModelUtil.readFromFile("model0.model", EObject.class);
		EObject read2 = ModelUtil.readFromFile("model0.model", EObject.class);

		System.out.println(nAssembly.eCrossReferences().get(0));
		System.out.println(nAssembly2.eCrossReferences().get(0));

		// TODO here
		EObject root = nAssembly3.eCrossReferences().get(0).eContainer();
		System.out.println(read.eResource().equals(root.eResource()));
		System.out.println(root.eResource().equals(tRepo.eResource()));

		System.out.println(read.eIsProxy());
		System.out.println(root.eIsProxy());

		ExtendedEquality helper = new ExtendedEquality();
		for (int i = 0; i < root.eClass().getFeatureCount(); i++) {
			EStructuralFeature feature = root.eClass().getEStructuralFeature(i);
			if (!feature.isDerived()) {
				System.out.println(feature.getName() + " -> " + String.valueOf(helper.eqFeature(read, read2, feature)));
			}
		}
		System.out.println(helper.equals(read2, read));
	}

	private static class ExtendedEquality extends EcoreUtil.EqualityHelper {
		public boolean eqFeature(EObject obj1, EObject obj2, EStructuralFeature feature) {
			return this.haveEqualFeature(obj1, obj2, feature);
		}
	}

}
