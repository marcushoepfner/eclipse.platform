/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.preferences;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.jface.dialogs.Dialog;

/**
 * Insert the type's description here.
 * @see PreferencePage
 */
public class MainPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {
	private static final String KEY_DESCRIPTION =
		"MainPreferencePage.description";
	private static final String PREFIX = UpdateUI.getPluginId();
	private static final String SYSTEM_VALUE = "system";
	private static final String KEY_CHECK_SIGNATURE =
		"MainPreferencePage.checkSignature";
	private static final String KEY_HISTORY_SIZE =
		"MainPreferencePage.historySize";
	private static final String KEY_UPDATE_VERSIONS =
		"MainPreferencePage.updateVersions";
	private static final String KEY_UPDATE_VERSIONS_EQUIVALENT =
		"MainPreferencePage.updateVersions.equivalent";
	private static final String KEY_UPDATE_VERSIONS_COMPATIBLE =
		"MainPreferencePage.updateVersions.compatible";

	private Label historySizeLabel;
	private Text historySizeText;
	private Button checkSignatureCheckbox;
	private Button equivalentButton;
	private Button compatibleButton;
	private Label httpProxyHostLabel;
	private Label httpProxyPortLabel;
	private Text httpProxyHostText;
	private Text httpProxyPortText;
	private Button enableHttpProxy;
	private static final String KEY_ENABLE_HTTP_PROXY =
		"MainPreferencePage.enableHttpProxy";
	private static final String KEY_HTTP_PROXY_SERVER =
		"MainPreferencePage.httpProxyHost";
	private static final String KEY_HTTP_PROXY_PORT =
		"MainPreferencePage.httpProxyPort";

	// these two values are for compatibility with old code
	public static final String EQUIVALENT_VALUE = "equivalent";
	public static final String COMPATIBLE_VALUE = "compatible";

	/**
	 * The constructor.
	 */
	public MainPreferencePage() {
		super();
		setPreferenceStore(UpdateUI.getDefault().getPreferenceStore());
		setDescription(UpdateUI.getString(KEY_DESCRIPTION));
	}

	/**
	 * Insert the method's description here.
	 * @see PreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
	
	/* (non-Javadoc)
	 * Method declared on PreferencePage.
	 */
	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(
			parent,
			"org.eclipse.update.ui.MainPreferencePage");

		Composite mainComposite =
			new Composite(parent, SWT.NULL);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		mainComposite.setLayout(layout);

		historySizeLabel = new Label(mainComposite, SWT.NONE);
		historySizeLabel.setText(UpdateUI.getString(KEY_HISTORY_SIZE));
		historySizeLabel.setFont(parent.getFont());
		historySizeText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		historySizeText.setLayoutData(gd);
		historySizeText.setFont(parent.getFont());

		checkSignatureCheckbox =
			new Button(mainComposite, SWT.CHECK | SWT.LEFT);
		checkSignatureCheckbox.setText(UpdateUI.getString(KEY_CHECK_SIGNATURE));
		checkSignatureCheckbox.setFont(parent.getFont());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkSignatureCheckbox.setLayoutData(gd);
		checkSignatureCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (checkSignatureCheckbox.getSelection() == false) {
					warnSignatureCheck(getShell());
				}
			}
		});
		
		createSpacer(mainComposite, 2);
		
		Group group = new Group(mainComposite, SWT.NONE);
		group.setText(UpdateUI.getString(KEY_UPDATE_VERSIONS));
		layout = new GridLayout();
		layout.numColumns = 1;
		group.setLayout(layout);
		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalAlignment = GridData.FILL;
		group.setLayoutData(gd);
		group.setFont(parent.getFont());

		equivalentButton = new Button(group, SWT.RADIO);
		equivalentButton.setText(
			UpdateUI.getString(KEY_UPDATE_VERSIONS_EQUIVALENT));
		equivalentButton.setFont(group.getFont());

		compatibleButton = new Button(group, SWT.RADIO);
		compatibleButton.setText(
			UpdateUI.getString(KEY_UPDATE_VERSIONS_COMPATIBLE));
		compatibleButton.setFont(group.getFont());

		createSpacer(mainComposite, 2);
		createHttpProxy(mainComposite, 2);

		return mainComposite;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
	}

	protected void createSpacer(Composite composite, int columnSpan) {
		Label label = new Label(composite, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = columnSpan;
		label.setLayoutData(gd);
	}

	protected void createHttpProxy(Composite composite, int columnSpan) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(UpdateUI.getString("MainPreferencePage.proxyGroup"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		GridData gd = new GridData();
		gd.horizontalSpan = columnSpan;
		gd.horizontalAlignment = GridData.FILL;
		group.setLayoutData(gd);
		group.setFont(composite.getFont());

		enableHttpProxy = new Button(group, SWT.CHECK);
		enableHttpProxy.setText(UpdateUI.getString(KEY_ENABLE_HTTP_PROXY));
		gd = new GridData();
		gd.horizontalSpan = 2;
		enableHttpProxy.setLayoutData(gd);

		httpProxyHostLabel = new Label(group, SWT.NONE);
		httpProxyHostLabel.setText(UpdateUI.getString(KEY_HTTP_PROXY_SERVER));

		httpProxyHostText = new Text(group, SWT.SINGLE | SWT.BORDER);
		httpProxyHostText.setFont(group.getFont());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		httpProxyHostText.setLayoutData(gd);

		httpProxyPortLabel = new Label(group, SWT.NONE);
		httpProxyPortLabel.setText(UpdateUI.getString(KEY_HTTP_PROXY_PORT));

		httpProxyPortText = new Text(group, SWT.SINGLE | SWT.BORDER);
		httpProxyPortText.setFont(group.getFont());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		httpProxyPortText.setLayoutData(gd);

		performDefaults();

		enableHttpProxy.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean enable = enableHttpProxy.getSelection();
				httpProxyPortLabel.setEnabled(enable);
				httpProxyHostLabel.setEnabled(enable);
				httpProxyPortText.setEnabled(enable);
				httpProxyHostText.setEnabled(enable);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

	}
	private int getHistorySize() {
		Preferences store = UpdateCore.getPlugin().getPluginPreferences();
		return store.getInt(UpdateCore.P_HISTORY_SIZE);
	}

	public static boolean getCheckDigitalSignature() {
		Preferences store = UpdateCore.getPlugin().getPluginPreferences();
		return store.getBoolean(UpdateCore.P_CHECK_SIGNATURE);
	}

	public static String getUpdateVersionsMode() {
		Preferences store = UpdateCore.getPlugin().getPluginPreferences();
		return store.getString(UpdateCore.P_UPDATE_VERSIONS);
	}

	public boolean performOk() {
		boolean result = super.performOk();
		if (result) {
			BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
				public void run() {
					try {
						SiteManager.getLocalSite().setMaximumHistoryCount(
							getHistorySize());
						SiteManager.setHttpProxyInfo(
							enableHttpProxy.getSelection(),
							httpProxyHostText.getText(),
							httpProxyPortText.getText());
					} catch (CoreException e) {
						UpdateUI.logException(e);
					}
				}
			});
		}
		UpdateUI.getDefault().savePluginPreferences();
		
		Preferences prefs = UpdateCore.getPlugin().getPluginPreferences();
		prefs.setValue(
			UpdateCore.P_CHECK_SIGNATURE,
			checkSignatureCheckbox.getSelection());
		prefs.setValue(UpdateCore.P_HISTORY_SIZE, historySizeText.getText());
		prefs.setValue(
			UpdateCore.P_UPDATE_VERSIONS,
			equivalentButton.getSelection()
				? EQUIVALENT_VALUE
				: COMPATIBLE_VALUE);
				
		UpdateCore.getPlugin().savePluginPreferences();
		
		return result;
	}
	public void performApply() {
		super.performApply();
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				SiteManager.setHttpProxyInfo(
					enableHttpProxy.getSelection(),
					httpProxyHostText.getText(),
					httpProxyPortText.getText());
			}
		});

		Preferences prefs = UpdateCore.getPlugin().getPluginPreferences();
		prefs.setValue(
			UpdateCore.P_CHECK_SIGNATURE,
			checkSignatureCheckbox.getSelection());
		prefs.setValue(UpdateCore.P_HISTORY_SIZE, historySizeText.getText());
		prefs.setValue(
			UpdateCore.P_UPDATE_VERSIONS,
			equivalentButton.getSelection()
				? EQUIVALENT_VALUE
				: COMPATIBLE_VALUE);
				
		UpdateCore.getPlugin().savePluginPreferences();
	}
	public void performDefaults() {
		super.performDefaults();

		enableHttpProxy.setSelection(SiteManager.isHttpProxyEnable());
		String serverValue = SiteManager.getHttpProxyServer();
		if (serverValue != null)
			httpProxyHostText.setText(serverValue);
		String portValue = SiteManager.getHttpProxyPort();
		if (portValue != null)
			httpProxyPortText.setText(portValue);

		httpProxyPortLabel.setEnabled(enableHttpProxy.getSelection());
		httpProxyHostLabel.setEnabled(enableHttpProxy.getSelection());
		httpProxyPortText.setEnabled(enableHttpProxy.getSelection());
		httpProxyHostText.setEnabled(enableHttpProxy.getSelection());

		Preferences prefs = UpdateCore.getPlugin().getPluginPreferences();
		checkSignatureCheckbox.setSelection(
			prefs.getBoolean(UpdateCore.P_CHECK_SIGNATURE));
		historySizeText.setText(prefs.getString(UpdateCore.P_HISTORY_SIZE));
		boolean isCompatible =
			UpdateCore.COMPATIBLE_VALUE.equals(
				prefs.getString(UpdateCore.P_UPDATE_VERSIONS));
		equivalentButton.setSelection(!isCompatible);
		compatibleButton.setSelection(isCompatible);
	}

//	public void propertyChange(PropertyChangeEvent event) {
//		super.propertyChange(event);
//		if (event.getSource().equals(checkSignatureEditor)) {
//			if (event.getNewValue().equals(Boolean.FALSE)) {
//				warnSignatureCheck(getShell());
//			}
//		}
//	}

	private void warnSignatureCheck(Shell shell) {
		MessageDialog.openWarning(
			shell,
			UpdateUI.getString("MainPreferencePage.digitalSignature.title"),
			UpdateUI.getString("MainPreferencePage.digitalSignature.message"));
	}
}