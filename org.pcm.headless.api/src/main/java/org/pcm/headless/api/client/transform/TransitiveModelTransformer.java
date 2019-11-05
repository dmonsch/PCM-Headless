package org.pcm.headless.api.client.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.pcm.headless.api.util.ModelUtil;

import com.google.common.collect.Lists;

public class TransitiveModelTransformer {
	// base
	private List<EObject> models;

	// map resources to eobjects
	private Map<URI, EObject> resourceMapping;

	// transitive
	private List<Resource> transitiveClosure;

	// transitive resolved
	private Set<URI> transitiveResolved;

	public TransitiveModelTransformer(EObject... models) {
		this.models = Stream.of(models).collect(Collectors.toList());
		this.resourceMapping = new HashMap<>();
	}

	public EObject getModelByURI(URI uri) {
		return resourceMapping.get(uri);
	}

	public Set<URI> getTransitives() {
		return transitiveResolved;
	}

	public void transformURIs(IURITransformer transformer) {
		if (transitiveClosure == null) {
			this.buildTransitiveClosure();
		}

		// create rules before
		transitiveClosure.forEach(r -> {
			transformer.installRule(r.getURI());
		});

		resourceMapping.entrySet().forEach(e -> {
			// install rules for all crosses
			filterResources(
					allCrossReferences(e.getValue()).stream().map(ref -> ref.eResource()).collect(Collectors.toList()),
					new HashSet<>()).forEach(cr -> {
						transformer.installRule(cr.getURI());
					});

			// transform crosses
			allCrossReferences(e.getValue()).forEach(cr -> {
				cr.eResource().setURI(transformer.transform(cr.eResource().getURI()));
			});

			// itself
			e.getValue().eResource().setURI(transformer.transform(e.getKey()));
		});
	}

	public void buildTransitiveClosure() {
		Set<URI> resourcesClosure = new HashSet<>();
		List<Resource> resourcesClosed = new ArrayList<>();
		transitiveResolved = new HashSet<>();

		// add all ex models
		for (EObject m : models) {
			if (m != null) {
				resourcesClosed.add(m.eResource());
				resourceMapping.put(m.eResource().getURI(), m);
			}
		}

		// first filter
		resourcesClosed = filterResources(resourcesClosed, resourcesClosure);

		// build transitive closure
		for (EObject model : models) {
			closeTransitive(resourcesClosed, resourcesClosure, model);
		}

		// set closure
		this.transitiveClosure = resourcesClosed;
	}

	private void closeTransitive(List<Resource> container, Set<URI> uriContainer, EObject search) {
		List<EObject> crossReferences = allCrossReferences(search);
		List<Resource> toAdd = filterResources(
				crossReferences.stream().map(t -> t.eResource()).collect(Collectors.toList()), uriContainer);
		container.addAll(toAdd);

		if (toAdd.size() > 0) {
			transitiveResolved.addAll(toAdd.stream().map(t -> t.getURI()).collect(Collectors.toSet()));
			toAdd.forEach(cf -> {
				EObject modelRead = ModelUtil.readFromFile(cf.getURI().toFileString(), EObject.class);
				resourceMapping.put(cf.getURI(), modelRead);
				closeTransitive(container, uriContainer, modelRead);
			});
		}
	}

	private List<Resource> filterResources(List<Resource> res, Set<URI> already) {
		List<Resource> result = new ArrayList<>();
		for (Resource r : res) {
			if (!already.contains(r.getURI())) {
				already.add(r.getURI());
				result.add(r);
			}
		}
		return result;
	}

	private List<EObject> allCrossReferences(EObject obj) {
		if (obj == null) {
			return Lists.newArrayList();
		}
		Set<EObject> references = new HashSet<>();

		// direct ones
		obj.eCrossReferences().forEach(cr -> {
			references.add(cr);
		});

		// of all child contents
		obj.eAllContents().forEachRemaining(e -> {
			if (e.eCrossReferences().size() > 0) {
				e.eCrossReferences().forEach(cr -> {
					references.add(cr);
				});
			}
		});

		// remove own and nulls
		references.remove(null);
		references.remove(obj);

		// remove pathmaps
		return references.stream()
				.filter(ref -> ref.eResource() != null && ref.eResource().getURI().toString().startsWith("file:"))
				.collect(Collectors.toList());
	}

}