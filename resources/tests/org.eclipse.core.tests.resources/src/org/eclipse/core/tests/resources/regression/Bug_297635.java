/*******************************************************************************
 *  Copyright (c) 2010, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.content.ContentTypeTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Tests regression of bug 297635
 */
public class Bug_297635 extends ResourceTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		BundleWithSaveParticipant.install();
		saveFull();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			BundleWithSaveParticipant.uninstall();
		} finally {
			super.tearDown();
		}
	}

	public void testCleanSaveStateBySaveParticipantOnSnapshotSave() throws Exception {
		executeWithSaveManagerSpy(saveManagerSpy -> {
			try {
				saveSnapshot(saveManagerSpy);
			} catch (CoreException e) {
			}
			verify(saveManagerSpy).forgetSavedTree(BundleWithSaveParticipant.getBundleName());
		});
	}

	private void saveFull() throws CoreException {
		getWorkspace().save(true, getMonitor());
	}

	private void saveSnapshot(SaveManager saveManager) throws CoreException {
		saveManager.save(ISaveContext.SNAPSHOT, true, null, getMonitor());
	}

	private void executeWithSaveManagerSpy(Consumer<SaveManager> executeOnSpySaveManager) throws Exception {
		IWorkspace workspace = getWorkspace();
		String saveManagerFieldName = "saveManager";
		SaveManager originalSaveManager = (SaveManager) getField(workspace, saveManagerFieldName);
		SaveManager spySaveManager = spy(originalSaveManager);
		try {
			setField(workspace, saveManagerFieldName, spySaveManager);
			executeOnSpySaveManager.accept(spySaveManager);
		} finally {
			setField(workspace, saveManagerFieldName, originalSaveManager);
		}
	}

	private static Object getField(Object object, String fieldName) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(object);
	}

	private static void setField(Object object, String fieldName, Object value) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(object, value);
	}

	private static final class BundleWithSaveParticipant {
		private static String TEST_BUNDLE_LOCATION = "content/bundle01";

		private static Bundle bundle;

		private static ISaveParticipant saveParticipant = new ISaveParticipant() {
			@Override
			public void doneSaving(ISaveContext context) {
				// nothing to do
			}

			@Override
			public void prepareToSave(ISaveContext context) {
				context.needDelta();
				context.needSaveNumber();
			}

			@Override
			public void rollback(ISaveContext context) {
				// nothing to do
			}

			@Override
			public void saving(ISaveContext context) {
				// nothing to do
			}
		};

		public static String getBundleName() {
			if (bundle == null) {
				throw new IllegalStateException("Bundle has not been installed");
			}
			return bundle.getSymbolicName();
		}

		public static void uninstall() throws BundleException {
			if (bundle != null) {
				bundle.uninstall();
			}
		}

		public static void install() throws Exception {
			bundle = BundleTestingHelper.installBundle("", getContext(),
					ContentTypeTest.TEST_FILES_ROOT + TEST_BUNDLE_LOCATION);
			BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { bundle });
			bundle.start(Bundle.START_TRANSIENT);
			registerSaveParticipant(bundle);
		}

		private static BundleContext getContext() {
			return Platform.getBundle(PI_RESOURCES_TESTS).getBundleContext();
		}

		private static void registerSaveParticipant(Bundle saveParticipantsBundle) throws CoreException {
			getWorkspace().addSaveParticipant(saveParticipantsBundle.getSymbolicName(), saveParticipant);
		}

	}
}
