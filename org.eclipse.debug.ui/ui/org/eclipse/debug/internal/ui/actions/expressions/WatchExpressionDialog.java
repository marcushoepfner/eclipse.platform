/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;


import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.StatusInfo;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.actions.TextViewerAction;

/**
 * Dialog for edit watch expression.
 */
public class WatchExpressionDialog extends StatusDialog {

	/**
	 * The detail formatter to edit.
	 */
	private IWatchExpression fWatchExpression;
	
	// widgets
	private SourceViewer fSnippetViewer;
	private Button fCheckBox;

	public WatchExpressionDialog(Shell parent, IWatchExpression watchExpression, boolean editDialog) {
		super(parent);
		fWatchExpression= watchExpression;
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		String helpContextId = null;
		if (editDialog) {
			setTitle(ActionMessages.WatchExpressionDialog_0); 
			helpContextId = IDebugHelpContextIds.EDIT_WATCH_EXPRESSION_DIALOG;
		} else {
			setTitle(ActionMessages.WatchExpressionDialog_1); 
			helpContextId = IDebugHelpContextIds.ADD_WATCH_EXPRESSION_DIALOG;
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, helpContextId);
	}

	/**
	 * Create the dialog area.
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		// snippet label
		Label label = new Label(container, SWT.NONE);
		label.setText(ActionMessages.WatchExpressionDialog_2); 
		gd= new GridData(GridData.BEGINNING);
		label.setLayoutData(gd);
		label.setFont(font);
		
		fSnippetViewer = new SourceViewer(container, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.LEFT_TO_RIGHT);
		fSnippetViewer.setInput(this);
		
		IDocument document = new Document();
		fSnippetViewer.configure(new SourceViewerConfiguration());
		fSnippetViewer.setEditable(true);
		fSnippetViewer.setDocument(document);
		document.addDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
			public void documentChanged(DocumentEvent event) {
				checkValues();
			}
		});

		fSnippetViewer.getTextWidget().setFont(JFaceResources.getTextFont());

		Control control= fSnippetViewer.getControl();
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(10);
		gd.widthHint= convertWidthInCharsToPixels(80);
		control.setLayoutData(gd);
		fSnippetViewer.getDocument().set(fWatchExpression.getExpressionText());

		// actions
		final TextViewerAction cutAction = new TextViewerAction(fSnippetViewer, ITextOperationTarget.CUT);
		cutAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		cutAction.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		cutAction.setText(ActionMessages.WatchExpressionDialogMenu_0);
		final TextViewerAction copyAction = new TextViewerAction(fSnippetViewer, ITextOperationTarget.COPY);
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		copyAction.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		copyAction.setText(ActionMessages.WatchExpressionDialogMenu_1);
		final TextViewerAction pasteAction = new TextViewerAction(fSnippetViewer, ITextOperationTarget.PASTE);
		pasteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		pasteAction.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		pasteAction.setText(ActionMessages.WatchExpressionDialogMenu_2);

		// context menu
		MenuManager menuManager = new MenuManager();
		menuManager.add(cutAction);
		menuManager.add(copyAction);
		menuManager.add(pasteAction);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				cutAction.update();
				copyAction.update();
				pasteAction.update();
			}
		});
		Menu menu = menuManager.createContextMenu(fSnippetViewer.getTextWidget());
		fSnippetViewer.getTextWidget().setMenu(menu);

		// enable checkbox
		fCheckBox= new Button(container, SWT.CHECK | SWT.LEFT);
		fCheckBox.setText(ActionMessages.WatchExpressionDialog_3); 
		fCheckBox.setSelection(fWatchExpression.isEnabled());
		fCheckBox.setFont(font);

		applyDialogFont(container);
		fSnippetViewer.getControl().setFocus();
		return container;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		fWatchExpression.setEnabled(fCheckBox.getSelection());
		fWatchExpression.setExpressionText(fSnippetViewer.getDocument().get());
		super.okPressed();
	}
	
	/**
	 * Check the field values and display a message in the status if needed.
	 */
	private void checkValues() {
		StatusInfo status= new StatusInfo();
		if (fSnippetViewer.getDocument().get().trim().length() == 0) {
			status.setError(ActionMessages.WatchExpressionDialog_4); 
		}
		updateStatus(status);
	}

}
