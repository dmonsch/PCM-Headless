package org.pcm.headless.api.client.transform;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.palladiosimulator.pcm.repository.PrimitiveDataType;
import org.pcm.headless.api.util.ModelUtil;

import de.uka.ipd.sdq.stoex.Expression;

public class TransitiveModelTransformerUtil {

	public TransitiveModelTransformerUtil() {
	}

	public List<EObject> copyObjects(List<EObject> obj) {
		return obj.stream().map(m -> EcoreUtil.copy(m)).collect(Collectors.toList());
	}

	public void relinkObjects(List<EObject> copies) {
		for (EObject copy : copies) {
			ModelUtil.getAllObjects(copy).forEach(o -> {
				linkingProcessObject(o, copy, new ModelLinkingResolver(copies));
			});
		}
	}

	public EObject getRootContainerOrNull(EObject obj) {
		EObject easyRoot = EcoreUtil.getRootContainer(obj);

		if (!(easyRoot instanceof Expression)
				&& (easyRoot.eResource() == null || easyRoot.eResource().getURI().scheme() == null
						|| !easyRoot.eResource().getURI().scheme().equals("pathmap"))
				&& !(easyRoot instanceof PrimitiveDataType)) {
			return easyRoot;
		}

		return null;
	}

	public boolean equalProxy(EObject o1, EObject o2) {
		EqualityHelper eq = new EqualityHelper();
		return eq.equals(o1, o2);
	}

	private void linkingProcessObject(EObject obj, EObject root, ModelLinkingResolver resolver) {
		obj.eClass().getEAllStructuralFeatures().forEach(feature -> {
			if (feature instanceof EReference) {
				Object result = obj.eGet(feature);
				if (result instanceof EObject) {
					EObject eResult = (EObject) result;
					EObject rootContainer = getRootContainerOrNull(eResult);

					if (rootContainer != null && !equalProxy(rootContainer, root)) {
						// => crossreference
						EObject correspondingObject = resolver.resolveCorrespondingObject(eResult, rootContainer);
						if (correspondingObject != null) {
							obj.eSet(feature, correspondingObject);
						}
					}
				}
			}
		});
	}

	// an improvement would be to remember the path and go the same path in the
	// copied model
	private class ModelLinkingResolver {
		private List<EObject> copies;

		private ModelLinkingResolver(List<EObject> copies) {
			this.copies = copies;
		}

		private EObject resolveCorrespondingObject(EObject obj, EObject container) {
			EObject copiedObject = copies.stream().filter(c -> {
				return equalProxy(container, c);
			}).findFirst().orElse(null);

			if (copiedObject != null) {
				// find child
				EObject copiedRef = ModelUtil.getObjectsExactType(copiedObject, obj.getClass()).stream().filter(r -> {
					return equalProxy(r, obj);
				}).findFirst().orElse(null);
				return copiedRef;
			}
			return null;
		}
	}

}
