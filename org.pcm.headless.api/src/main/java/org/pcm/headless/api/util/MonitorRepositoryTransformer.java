package org.pcm.headless.api.util;

import java.lang.reflect.Field;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.DynamicValueHolder;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.InternalEObject;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.impl.MeasurementSpecificationImpl;

public class MonitorRepositoryTransformer {

	public static void makePersistable(EObject repo) {
		ModelUtil.getObjects(repo, MeasurementSpecification.class).forEach(spec -> {
			try {
				Field nameField = MeasurementSpecificationImpl.class.getDeclaredField("NAME__ESETTING_DELEGATE");
				nameField.setAccessible(true);

				nameField.set(spec, new MonitorRepositoryTransformer.FakeDelegate());

				nameField.setAccessible(false);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		});
	}

	private static class FakeDelegate implements EStructuralFeature.Internal.SettingDelegate {

		@Override
		public Object dynamicGet(InternalEObject arg0, DynamicValueHolder arg1, int arg2, boolean arg3, boolean arg4) {
			return null;
		}

		@Override
		public NotificationChain dynamicInverseAdd(InternalEObject arg0, DynamicValueHolder arg1, int arg2,
				InternalEObject arg3, NotificationChain arg4) {
			return null;
		}

		@Override
		public NotificationChain dynamicInverseRemove(InternalEObject arg0, DynamicValueHolder arg1, int arg2,
				InternalEObject arg3, NotificationChain arg4) {
			return null;
		}

		@Override
		public boolean dynamicIsSet(InternalEObject arg0, DynamicValueHolder arg1, int arg2) {
			return false;
		}

		@Override
		public void dynamicSet(InternalEObject arg0, DynamicValueHolder arg1, int arg2, Object arg3) {
		}

		@Override
		public Setting dynamicSetting(InternalEObject arg0, DynamicValueHolder arg1, int arg2) {
			return null;
		}

		@Override
		public void dynamicUnset(InternalEObject arg0, DynamicValueHolder arg1, int arg2) {
		}

	}

}
