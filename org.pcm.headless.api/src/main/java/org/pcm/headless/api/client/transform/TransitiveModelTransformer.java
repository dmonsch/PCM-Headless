package org.pcm.headless.api.client.transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.MonitorRepositoryTransformer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.java.Log;

@Log
public class TransitiveModelTransformer {
	// base
	private List<EObject> models;

	// closure
	private List<EObject> transitiveClosure;

	// util
	private TransitiveModelTransformerUtil transformerUtil;

	public TransitiveModelTransformer(EObject... models) {
		this.models = Stream.of(models).collect(Collectors.toList());
		this.transformerUtil = new TransitiveModelTransformerUtil();
	}

	public List<EObject> buildModels(IModelFileNameGenerator fileNameGenerator) {
		if (transitiveClosure == null || transitiveClosure.size() == 0) {
			this.buildTransitiveClosure();
		}

		// exit if empty
		if (transitiveClosure.size() == 0) {
			return Lists.newArrayList();
		}

		// base path
		File modelBasePath;
		try {
			modelBasePath = Files.createTempDirectory("temporaryModels").toFile();
		} catch (IOException e) {
			return Lists.newArrayList();
		}

		// create copies
		List<EObject> transitiveClosureCopy = transformerUtil.copyObjects(transitiveClosure);

		// relink the copies
		transformerUtil.relinkObjects(transitiveClosureCopy);

		// generate file names
		Map<EObject, File> resultingFileMap = createFileMappingAndInitializeResources(transitiveClosureCopy,
				modelBasePath, fileNameGenerator);

		// transform the links
		Map<EObject, File> cacheFileMapping = new HashMap<>();
		transitiveClosureCopy.forEach(m -> {
			File resultingFile = resolveAndCache(m, resultingFileMap, cacheFileMapping);

			// transform crosses
			allCrossReferences(m).forEach(cr -> {
				EObject rootContainer = transformerUtil.getRootContainerOrNull(cr);
				if (rootContainer != null) {
					File resultingFileCrossReference = resolveAndCache(rootContainer, resultingFileMap,
							cacheFileMapping);

					cr.eResource().setURI(URI.createFileURI(resultingFileCrossReference.getName()));
				}
			});

			// transform itself
			m.eResource().setURI(URI.createFileURI(resultingFile.getName()));
		});

		// remove temp folder
		modelBasePath.delete();

		return transitiveClosureCopy;
	}

	public void buildTransitiveClosure() {
		transitiveClosure = Lists.newArrayList();

		// add all ex models
		for (EObject m : models) {
			if (m != null) {
				transitiveClosure.add(m);
			}
		}

		// build transitive closure
		for (EObject model : models) {
			closeTransitive(transitiveClosure, model);
		}

		List<EObject> transitiveClosureIdent = Lists.newArrayList();
		for (EObject m : transitiveClosure) {
			boolean contained = transitiveClosureIdent.stream().anyMatch(e -> {
				return m == e || m.equals(e) || transformerUtil.equalProxy(m, e);
			});
			if (!contained) {
				transitiveClosureIdent.add(m);
			}
		}

		// rewrite transitive closure
		transitiveClosure = transitiveClosureIdent;

	}

	private Map<EObject, File> createFileMappingAndInitializeResources(List<EObject> transitiveClosure, File basePath,
			IModelFileNameGenerator fileNameGenerator) {
		Map<EObject, File> resultingFileMap = new HashMap<>();
		for (EObject obj : transitiveClosure) {
			// clone and put
			resultingFileMap.put(obj, new File(basePath, fileNameGenerator.generateFileName(obj)));
		}

		// save them so all have a eresource
		transitiveClosure.forEach(m -> {
			File resultingFile = resultingFileMap.get(m);
			if (m instanceof MonitorRepository) {
				MonitorRepositoryTransformer.makePersistable(m);
			}
			ModelUtil.saveToFile(m, resultingFile);
		});

		// normally we could build a graph and determine a sequence for the models to
		// save, so that all have a resource
		// but we do it "bruteforce" like
		// we save until all have reference; but at maximum n times
		// where n is the number of models
		// otherwise it is not deterministic
		boolean corrected = false;
		for (int i = 0; i < transitiveClosure.size(); i++) {
			boolean any = false;
			for (EObject m : transitiveClosure) {
				File resultingFile = resultingFileMap.get(m);
				boolean success = ModelUtil.saveToFile(m, resultingFile);
				if (!success) {
					any = true;
				}
			}

			if (!any) {
				corrected = true;
				break;
			}
		}

		if (!corrected) {
			log.warning("Could not save all models successfully.");
		}

		return resultingFileMap;
	}

	private File resolveAndCache(EObject obj, Map<EObject, File> orgMap, Map<EObject, File> cache) {
		if (cache.containsKey(obj)) {
			return cache.get(obj);
		} else {
			File result = orgMap.entrySet().stream().filter(r -> {
				return transformerUtil.equalProxy(r.getKey(), obj);
			}).map(r -> r.getValue()).findFirst().orElse(null);

			cache.put(obj, result);

			return result;
		}
	}

	private void closeTransitive(List<EObject> container, EObject search) {
		Set<EObject> crossReferences = allCrossReferences(search);

		List<EObject> toAdd = Lists.newArrayList();
		for (EObject crossRef : crossReferences) {
			if (!container.contains(crossRef)) {
				container.add(crossRef);
				toAdd.add(crossRef);
			}
		}

		if (toAdd.size() > 0) {
			toAdd.forEach(modelRead -> {
				closeTransitive(container, modelRead);
			});
		}
	}

	private Set<EObject> allCrossReferences(EObject obj) {
		if (obj == null) {
			return Sets.newHashSet();
		}
		Set<EObject> references = new HashSet<>();

		// direct ones
		obj.eCrossReferences().forEach(cr -> {
			references.add(transformerUtil.getRootContainerOrNull(cr));
		});

		// of all child contents
		obj.eAllContents().forEachRemaining(e -> {
			if (e.eCrossReferences().size() > 0) {
				e.eCrossReferences().forEach(cr -> {
					references.add(transformerUtil.getRootContainerOrNull(cr));
				});
			}
		});

		// remove own and nulls
		references.remove(null);
		references.remove(obj);

		// remove pathmaps
		return references;
	}

}