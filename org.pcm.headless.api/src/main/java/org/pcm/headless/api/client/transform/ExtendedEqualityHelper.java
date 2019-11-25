package org.pcm.headless.api.client.transform;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

/**
 * This is an extension/ modification of the {@link EqualityHelper} of the EMF
 * framework. In this implementation we mark objects as visited and do not use a
 * map. In cases of loops in the references of model objects, this
 * implementation provides better results in terms of content equality.
 * 
 * @author David Monschein
 *
 */
public class ExtendedEqualityHelper {
	public boolean equals(EObject eObject1, EObject eObject2, Set<EObject> visitedObjects) {
		if (eObject1 == null) {
			return eObject2 == null;
		}

		if (eObject2 == null) {
			return false;
		}

		if (visitedObjects.contains(eObject1) || visitedObjects.contains(eObject2)) {
			return true;
		}

		if (eObject1 == eObject2) {
			visitedObjects.add(eObject1);
			visitedObjects.add(eObject2);
			return true;
		}

		if (eObject1.eIsProxy()) {
			if (((InternalEObject) eObject1).eProxyURI().equals(((InternalEObject) eObject2).eProxyURI())) {
				visitedObjects.add(eObject1);
				visitedObjects.add(eObject2);
				return true;
			} else {
				visitedObjects.add(eObject1);
				visitedObjects.add(eObject2);
				return false;
			}
		} else if (eObject2.eIsProxy()) {
			visitedObjects.add(eObject1);
			visitedObjects.add(eObject2);
			return false;
		}

		EClass eClass = eObject1.eClass();
		if (eClass != eObject2.eClass()) {
			visitedObjects.add(eObject1);
			visitedObjects.add(eObject2);
			return false;
		}

		// mark as visited
		visitedObjects.add(eObject1);
		visitedObjects.add(eObject2);

		for (int i = 0, size = eClass.getFeatureCount(); i < size; ++i) {
			EStructuralFeature feature = eClass.getEStructuralFeature(i);
			if (!feature.isDerived()) {
				if (!haveEqualFeature(eObject1, eObject2, feature, visitedObjects)) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean equals(List<EObject> list1, List<EObject> list2, Set<EObject> feats) {
		int size = list1.size();
		if (size != list2.size()) {
			return false;
		}

		for (int i = 0; i < size; i++) {
			EObject eObject1 = list1.get(i);
			EObject eObject2 = list2.get(i);
			if (!equals(eObject1, eObject2, feats)) {
				return false;
			}
		}

		return true;
	}

	protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature,
			Set<EObject> visitedObjects) {
		// If the set states are the same, and the values of the feature are the
		// structurally equal, they are equal.
		//
		final boolean isSet1 = eObject1.eIsSet(feature);
		final boolean isSet2 = eObject2.eIsSet(feature);
		if (isSet1 && isSet2) {
			return feature instanceof EReference
					? haveEqualReference(eObject1, eObject2, (EReference) feature, visitedObjects)
					: haveEqualAttribute(eObject1, eObject2, (EAttribute) feature, visitedObjects);
		} else {
			return isSet1 == isSet2;
		}
	}

	@SuppressWarnings("unchecked")
	protected boolean haveEqualReference(EObject eObject1, EObject eObject2, EReference reference,
			Set<EObject> visitedObjects) {
		Object value1 = eObject1.eGet(reference);
		Object value2 = eObject2.eGet(reference);

		return reference.isMany() ? equals((List<EObject>) value1, (List<EObject>) value2, visitedObjects)
				: equals((EObject) value1, (EObject) value2, visitedObjects);
	}

	protected boolean haveEqualAttribute(EObject eObject1, EObject eObject2, EAttribute attribute,
			Set<EObject> visitedFeats) {
		Object value1 = eObject1.eGet(attribute);
		Object value2 = eObject2.eGet(attribute);

		// If the first value is null, the second value must be null.
		//
		if (value1 == null) {
			return value2 == null;
		}

		// Since the first value isn't null, if the second one is, they aren't equal.
		//
		if (value2 == null) {
			return false;
		}

		// If this is a feature map...
		//
		if (FeatureMapUtil.isFeatureMap(attribute)) {
			// The feature maps must be equal.
			//
			FeatureMap featureMap1 = (FeatureMap) value1;
			FeatureMap featureMap2 = (FeatureMap) value2;
			return equalFeatureMaps(featureMap1, featureMap2, visitedFeats);
		} else {
			// The values must be Java equal.
			//
			return equalValues(value1, value2);
		}
	}

	protected boolean equalValues(Object value1, Object value2) {
		return value1.equals(value2);
	}

	protected boolean equalFeatureMaps(FeatureMap featureMap1, FeatureMap featureMap2, Set<EObject> visitedObjects) {
		// If they don't have the same size, the feature maps aren't equal.
		//
		int size = featureMap1.size();
		if (size != featureMap2.size()) {
			return false;
		}

		// Compare entries in order.
		//
		for (int i = 0; i < size; i++) {
			// If entries don't have the same feature, the feature maps aren't equal.
			//
			EStructuralFeature feature = featureMap1.getEStructuralFeature(i);
			if (feature != featureMap2.getEStructuralFeature(i)) {
				return false;
			}

			Object value1 = featureMap1.getValue(i);
			Object value2 = featureMap2.getValue(i);

			if (!equalFeatureMapValues(value1, value2, feature, visitedObjects)) {
				return false;
			}
		}

		// There is no reason they aren't equals.
		//
		return true;
	}

	protected boolean equalFeatureMapValues(Object value1, Object value2, EStructuralFeature feature,
			Set<EObject> visitedObjects) {
		if (feature instanceof EReference) {
			// If the referenced EObjects aren't equal, the feature maps aren't equal.
			//
			return equals((EObject) value1, (EObject) value2, visitedObjects);
		} else {
			// If the values aren't Java equal, the feature maps aren't equal.
			//
			return value1 == null ? value2 == null : equalValues(value1, value2);
		}
	}

}
