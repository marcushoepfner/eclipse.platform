package org.eclipse.ant.internal.ui;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import java.net.MalformedURLException;import java.net.URL;import java.util.*;import java.util.List;import org.apache.tools.ant.Project;import org.eclipse.jface.action.*;import org.eclipse.jface.preference.PreferenceConverter;import org.eclipse.jface.resource.ImageDescriptor;import org.eclipse.jface.text.*;import org.eclipse.jface.viewers.*;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.SashForm;import org.eclipse.swt.custom.StyleRange;import org.eclipse.swt.events.*;import org.eclipse.swt.graphics.Color;import org.eclipse.swt.graphics.Font;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.*;import org.eclipse.ui.*;import org.eclipse.ui.part.ViewPart;import org.eclipse.ui.texteditor.FindReplaceAction;import org.eclipse.ui.texteditor.ITextEditorActionConstants;public class AntConsole extends ViewPart {	// keeps track of al the instances of this class so that they can share the Colors and the Font	private static Vector instances = new Vector(10);	public final static String CONSOLE_ID = "org.eclipse.ant.ui.antconsole";	public final static String PROPERTY_PREFIX_FIND = "find_action.";	private final static int SASH_WIDTH = 3; // regular width for a sash		// strings for the memento	private final static String TREE_WIDTH_PROPERTY = "tree_width";	private final static String SHOW_ONLY_SELECTED_ITEM_PROPERTY = "wasShowOnlySelectedTreeItemsTurnedOn";	private final static String SHOW_TREE_PROPERTY = "hideOrShowTreeAction";		// UI objects	private SashForm sash;	private TreeViewer tree;	private TextViewer viewer;	private Action copyAction;	private Action selectAllAction;	private Action clearOutputAction;	private Action findAction;	private Action expandTreeItemAction;	private Action showTreeAction;	private Action showSelectedItemAction;	private IDocument document;	private Vector styleRangeVector;	private AntTreeLabelProvider labelprovider;	private AntTreeContentProvider contentProvider;		// Structure to store the textwidget index information	private OutputStructureElement root = null;		private OutputStructureElement currentElement = null;		// class variables that handle the colors and the font	static Color ERROR_COLOR;	static Color WARN_COLOR;	static Color INFO_COLOR;	static Color VERBOSE_COLOR;	static Color DEBUG_COLOR;	static Font ANT_FONT;	private static AntPropertyChangeListener changeListener = AntPropertyChangeListener.getInstance();	// lastWidth is used to store the width of the tree that the user set	private int lastTreeWidth = 30;	private boolean showOnlySelectedItems = false;	private boolean showTree = false;/** * Constructor for AntConsole */public AntConsole() {	super();	if (instances.size() == 0) {		// first time there is an instance of this class: intantiate the colors and register the listener		ERROR_COLOR = new Color(null, PreferenceConverter.getColor(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_ERROR_RGB));		WARN_COLOR = new Color(null, PreferenceConverter.getColor(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_WARNING_RGB));		INFO_COLOR = new Color(null, PreferenceConverter.getColor(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_INFO_RGB));		VERBOSE_COLOR = new Color(null, PreferenceConverter.getColor(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_VERBOSE_RGB));		DEBUG_COLOR = new Color(null, PreferenceConverter.getColor(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_DEBUG_RGB));		ANT_FONT = new Font(null, PreferenceConverter.getFontData(AntUIPlugin.getPlugin().getPreferenceStore(),IAntPreferenceConstants.CONSOLE_FONT));			AntUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(changeListener);	}		instances.add(this);	document = new Document();	styleRangeVector = new Vector(5);	labelprovider = new AntTreeLabelProvider(this);	contentProvider = new AntTreeContentProvider(this);	initializeOutputStructure();}/** * @see IViewPart */public void init(IViewSite site, IMemento memento) throws PartInitException {	super.init(site,memento);	if (memento != null) {		// retrieve the values of the previous session		lastTreeWidth = memento.getInteger(TREE_WIDTH_PROPERTY).intValue();		showOnlySelectedItems = memento.getInteger(SHOW_ONLY_SELECTED_ITEM_PROPERTY).intValue() != 0;		showTree = memento.getInteger(SHOW_TREE_PROPERTY).intValue() != 0;	}}protected void addContributions() {	// In order for the clipboard actions to be accessible via their shortcuts	// (e.g., Ctrl-C, Ctrl-V), we *must* set a global action handler for	// each action	IActionBars actionBars= getViewSite().getActionBars();	actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);	actionBars.setGlobalActionHandler(ITextEditorActionConstants.FIND, findAction);	actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);			MenuManager textViewerMgr = new MenuManager();	textViewerMgr.setRemoveAllWhenShown(true);	textViewerMgr.addMenuListener(new IMenuListener() {		public void menuAboutToShow(IMenuManager textViewerMgr) {			fillTextViewerContextMenu(textViewerMgr);		}	});	Menu textViewerMenu = textViewerMgr.createContextMenu(viewer.getControl());	viewer.getControl().setMenu(textViewerMenu);		MenuManager treeViewerMgr = new MenuManager();	treeViewerMgr.setRemoveAllWhenShown(true);	treeViewerMgr.addMenuListener(new IMenuListener() {		public void menuAboutToShow(IMenuManager treeViewerMgr) {			fillTreeViewerContextMenu(treeViewerMgr);		}	});	Menu treeViewerMenu = treeViewerMgr.createContextMenu(tree.getControl());	tree.getControl().setMenu(treeViewerMenu);		//add toolbar actions	IToolBarManager tbm= getViewSite().getActionBars().getToolBarManager();	tbm.add(showTreeAction);	tbm.add(showSelectedItemAction);	tbm.add(clearOutputAction);	getViewSite().getActionBars().updateActionBars();}private void createHideOrShowTreeAction() {	showTreeAction = new Action() {		public void run() {			showTree = isChecked();			if (showTree) {				// the tree is hidden, let's show it				sash.SASH_WIDTH = SASH_WIDTH;				sash.setWeights(new int[] {lastTreeWidth, 100-lastTreeWidth});				setToolTipText(Policy.bind("console.hideOutputStructureTree"));				// the "ShowOnlySelectedElement" functionality can be turned on				showSelectedItemAction.setEnabled(true);				showSelectedItemAction.setChecked(showOnlySelectedItems);				showSelectedItemAction.run();			}			else {				// let's hide the tree				sash.SASH_WIDTH = 0;				sash.setWeights(new int[] {0,100});				setToolTipText(Policy.bind("console.showOutputStructureTree"));				// show the whole document				showCompleteOutput();				// disable the show selected item action				showSelectedItemAction.setEnabled(false);			}		}	};		showTreeAction.setImageDescriptor(getImageDescriptor("icons/full/clcl16/hideOrShowTree.gif"));	showTreeAction.setChecked(showTree);	showTreeAction.setText(Policy.bind("console.showTree"));	String tooltip = showTree ? "console.hideOutputStructureTree" : "console.showOutputStructureTree";	showTreeAction.setToolTipText(Policy.bind(tooltip));}private boolean isTreeHidden() {	return sash.getWeights()[0] == 0;}public void append(String value) {	append(value, Project.MSG_INFO);}public void append(final String value, final int ouputLevel) {	getViewSite().getShell().getDisplay().syncExec(new Runnable() {		public void run() {			int start = document.get().length();			document.set(document.get() + value);			setOutputLevelColor(ouputLevel, start, value.length());			if (value.length() > 0 && viewer != null)				viewer.revealRange(document.get().length() - 1, 1);		}	});}private void setOutputLevelColor(int level, int start, int end) {	switch (level) {		case Project.MSG_ERR: 			addRangeStyle(start, end, ERROR_COLOR); 			break;		case Project.MSG_WARN: 			addRangeStyle(start, end, WARN_COLOR); 			break;		case Project.MSG_INFO: 			addRangeStyle(start, end, INFO_COLOR); 			break;		case Project.MSG_VERBOSE: 			addRangeStyle(start, end, VERBOSE_COLOR); 			break;		case Project.MSG_DEBUG: 			addRangeStyle(start, end, DEBUG_COLOR); 			break;		default: 			addRangeStyle(start, end, INFO_COLOR);	}}private void addRangeStyle(int start, int length, Color color) {	if (styleRangeVector.size() != 0) {		StyleRange lastStyle = (StyleRange) styleRangeVector.lastElement();		if (color.equals(lastStyle.foreground))			lastStyle.length += length;		else			styleRangeVector.add(new StyleRange(start, length, color, null));	} else		styleRangeVector.add(new StyleRange(start, length, color, null));	StyleRange[] styleArray = (StyleRange[]) styleRangeVector.toArray(new StyleRange[styleRangeVector.size()]);				if (viewer != null)		viewer.getTextWidget().setStyleRanges(styleArray);}protected void copySelectionToClipboard() {	viewer.doOperation(viewer.COPY);}/** * Creates the actions that will appear in this view's toolbar and popup menus. */protected void createActions() {	// Create the actions for the text viewer.	copyAction = new Action(Policy.bind("console.copy")) {		public void run() {			copySelectionToClipboard();		}	};	selectAllAction = new Action(Policy.bind("console.selectAll")) {		public void run() {			selectAllText();		}	};	clearOutputAction = new Action(Policy.bind("console.clearOutput")) {		public void run() {			clearOutput();		}	};	clearOutputAction.setImageDescriptor(getImageDescriptor("icons/full/clcl16/clear.gif"));	clearOutputAction.setToolTipText(Policy.bind("console.clearOutput"));		findAction = new FindReplaceAction(		AntUIPlugin.getResourceBundle(),		PROPERTY_PREFIX_FIND,		this);	findAction.setEnabled(true);		// Create the actions for the tree viewer.	createHideOrShowTreeAction();	expandTreeItemAction = new Action(Policy.bind("console.expandAll")) {		public void run() {			OutputStructureElement selectedElement = (OutputStructureElement) ((IStructuredSelection) tree.getSelection()).getFirstElement();			tree.expandToLevel(selectedElement, tree.ALL_LEVELS);		}	};		//create the toolbar actions	showSelectedItemAction = new Action() {		public void run() {			showOnlySelectedItems = isChecked();			if (showOnlySelectedItems) {				// we want to show only the selected tree items				showSelectedElementOnly();				// changes the labels				setToolTipText(Policy.bind("console.showCompleteOutput"));			}			else {				// we want to show the whole document now				showCompleteOutput();				// changes the labels				setToolTipText(Policy.bind("console.showSelectedElementOnly"));			}		}	};	showSelectedItemAction.setImageDescriptor(getImageDescriptor("icons/full/clcl16/showOnlySelectedText.gif"));	showSelectedItemAction.setChecked(showOnlySelectedItems);	showSelectedItemAction.setText(Policy.bind("console.showSelectedElementOnly"));			String tooltip = showOnlySelectedItems ? "console.showCompleteOutput" : "console.showSelectedElementOnly";	showSelectedItemAction.setToolTipText(Policy.bind(tooltip));}public void clearOutput() {	document.set("");	styleRangeVector.removeAllElements();	// the tree can be null if #createPartControl has not called yet, 	// i.e. if the console exists but has never been shown so far	if (tree != null) {		initializeOutputStructure();		refreshTree();	}}/* * Shows the output of the selected item only */protected void showSelectedElementOnly() {	IStructuredSelection selection = (IStructuredSelection) tree.getSelection();	if (selection.isEmpty())		viewer.setVisibleRegion(0,0);	else {		OutputStructureElement selectedElement = (OutputStructureElement) selection.getFirstElement();		// XXX NOTE: #setVisibleRegion doesn't keep the color information... See "1GHQC7Q: ITPUI:WIN2000 - TextViewer#setVisibleRegion doesn't take into account the colors"		viewer.setVisibleRegion(selectedElement.getStartIndex(), selectedElement.getLength());	}}/* * Shows the output of the whole docuent, and reveals the range of the selected item */protected void showCompleteOutput() {	// show all the document	viewer.setVisibleRegion(0, document.get().length());	// XXX should I have to do that? If this is not done, then the colors don't appear --> bug of #setVisibleRegion ? --> See "1GHQC7Q: ITPUI:WIN2000 - TextViewer#setVisibleRegion doesn't take into account the colors"	viewer.getTextWidget().setStyleRanges((StyleRange[]) styleRangeVector.toArray(new StyleRange[styleRangeVector.size()]));	// and then reveal the range of the selected item	revealRangeOfSelectedItem();}private void revealRangeOfSelectedItem() {	IStructuredSelection selection = (IStructuredSelection) tree.getSelection();	if (!selection.isEmpty()) {		// then show the reveal the range of the output accordingly		OutputStructureElement selectedElement = (OutputStructureElement) selection.getFirstElement();		viewer.revealRange(selectedElement.getStartIndex(), selectedElement.getLength());		viewer.setSelectedRange(selectedElement.getStartIndex(), selectedElement.getLength());	}	}public void initializeOutputStructure() {	// root is the first element of the structure: it is a fake so it doesn't need a real name	root = new OutputStructureElement("-- root --");	currentElement = new OutputStructureElement(Policy.bind("console.antScript"), root, 0);		if (tree != null)		initializeTreeInput();}public void initializeTreeInput() {	getSite().getShell().getDisplay().syncExec(new Runnable(){		public void run() {			if (tree != null)				tree.setInput(root);		}	});}public void refreshTree() {	// if the tree is null, it means that the view hasn't been shown yet, so we don't need to refresh it.	if (tree != null) {		getSite().getShell().getDisplay().syncExec(new Runnable(){			public void run() {					tree.refresh();					tree.expandAll();				}		});	}}public void updateFont() {	if (viewer != null)		viewer.getTextWidget().setFont(ANT_FONT);}protected void fillTextViewerContextMenu(IMenuManager manager) {	copyAction.setEnabled(viewer.canDoOperation(viewer.COPY));	selectAllAction.setEnabled(viewer.canDoOperation(viewer.SELECT_ALL));	manager.add(copyAction);	manager.add(findAction);	manager.add(selectAllAction);	manager.add(new Separator());	manager.add(showTreeAction);	manager.add(clearOutputAction);}protected void fillTreeViewerContextMenu(IMenuManager manager) {	manager.add(showSelectedItemAction);	manager.add(expandTreeItemAction);	manager.add(new Separator());	manager.add(showTreeAction);	manager.add(clearOutputAction);}public Object getAdapter(Class required) {	if (IFindReplaceTarget.class.equals(required))		return viewer.getFindReplaceTarget();	return super.getAdapter(required);}protected ImageDescriptor getImageDescriptor(String relativePath) {	try {		URL installURL = AntUIPlugin.getPlugin().getDescriptor().getInstallURL();		URL url = new URL(installURL,relativePath);		return ImageDescriptor.createFromURL(url);	} catch (MalformedURLException e) {		return null;	}}protected void selectAllText() {	viewer.doOperation(viewer.SELECT_ALL);}/** * @see WorkbenchPart#setFocus() */public void setFocus() {	sash.setFocus();}/** * @see WorkbenchPart#createPartControl(Composite) */public void createPartControl(Composite parent) {	sash = new SashForm(parent, SWT.HORIZONTAL);	GridLayout sashLayout = new GridLayout();	sashLayout.marginHeight = 0;	sashLayout.marginWidth = 0;	sash.setLayout(sashLayout);	sash.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));	tree = new TreeViewer(sash, SWT.V_SCROLL | SWT.H_SCROLL);	GridData treeData = new GridData(GridData.FILL_BOTH);	tree.getControl().setLayoutData(treeData);	tree.setContentProvider(contentProvider);	tree.setLabelProvider(labelprovider);	tree.setInput(root);	tree.expandAll();	addTreeViewerListeners();	viewer = new TextViewer(sash,SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);	GridData viewerData = new GridData(GridData.FILL_BOTH);	viewer.getControl().setLayoutData(viewerData);	viewer.setEditable(false);	viewer.setDocument(document);	viewer.getTextWidget().setFont(ANT_FONT);	viewer.getTextWidget().setStyleRanges((StyleRange[]) styleRangeVector.toArray(new StyleRange[styleRangeVector.size()]));	addTextViewerListeners();		// sets the ratio tree/textViewer for the sashForm	if (showTree)		sash.setWeights(new int[] {lastTreeWidth, 100-lastTreeWidth});	else		// the "hideOrShowTree" action wasn't checked: this means that the user didn't want to have the tree		sash.setWeights(new int[] {0, 100});	createActions();	addContributions();}protected void addTreeViewerListeners() {	tree.addSelectionChangedListener(new ISelectionChangedListener() {		public void selectionChanged(SelectionChangedEvent e) {			if (viewer != null)				if (showSelectedItemAction.isChecked())					showSelectedElementOnly();				else					revealRangeOfSelectedItem();		}	});	// to remember the place of the sash when we hide the tree	tree.getControl().addControlListener(new ControlAdapter() {		public void controlResized(ControlEvent e) {			if (tree.getControl().getSize().x != 0)				// we don't want the width to be stored when the tree is getting hidden 				// (because it equals zero and we want to have the previous value)				lastTreeWidth = new Float( (float) tree.getControl().getSize().x / sash.getSize().x * 100).intValue();		}	});}protected void addTextViewerListeners() {	viewer.getTextWidget().addMouseListener(new MouseAdapter() {		public void mouseDown(MouseEvent e) {			if (!showSelectedItemAction.isChecked())				selectTreeItem(viewer.getTextWidget().getCaretOffset());		}	});	viewer.getTextWidget().addKeyListener(new KeyAdapter(){ 		public void keyReleased(KeyEvent e) {			if (!showSelectedItemAction.isChecked())				selectTreeItem(viewer.getTextWidget().getCaretOffset()); 		}	});}protected void selectTreeItem(int caretPosition) {	// tree.getTree().getItems()[1] returns the root of the tree that contains the project	// it may not exist if there is no output (in this case, there is only one item: the "Ant Script" one)	if (tree.getTree().getItems().length != 1) {		TreeItem itemToSelect = null;		if (findItem(tree.getTree().getItems()[0], caretPosition) != null)			// the first item is the good one			itemToSelect = tree.getTree().getItems()[0];		else			// the first item is not the good one, let's check the second one and its children			itemToSelect = findItem(tree.getTree().getItems()[1], caretPosition);		tree.getTree().setSelection(new TreeItem[] {itemToSelect});	}}private TreeItem findItem(TreeItem item, int position) {	if (!( ((OutputStructureElement) item.getData()).getStartIndex() <= position 			&& ((OutputStructureElement) item.getData()).getEndIndex() > position))		return null;	for (int i=0; i<item.getItems().length; i++) {		TreeItem child = findItem (item.getItems()[i], position);		if (child != null)			return child;	}	return item;}/** * @see IViewPart */public void saveState(IMemento memento) {	memento.putInteger(TREE_WIDTH_PROPERTY, lastTreeWidth);	// it is not possible to put a boolean in a memento, so we use integers	memento.putInteger(SHOW_ONLY_SELECTED_ITEM_PROPERTY, showOnlySelectedItems ? 1 : 0);	memento.putInteger(SHOW_TREE_PROPERTY, showTree ? 1 : 0);}public void dispose() {	if (instances.size() == 1) {		// all the consoles are diposed: we can dispose the colors as well and remove the property listener		ERROR_COLOR.dispose();		WARN_COLOR.dispose();		INFO_COLOR.dispose();		VERBOSE_COLOR.dispose();		DEBUG_COLOR.dispose();		ANT_FONT.dispose();				AntUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(changeListener);	}	instances.remove(this);	super.dispose();}public static Vector getInstances() {	return instances;}public OutputStructureElement getCurrentOutputStructureElement() {	return currentElement;}public void setCurrentOutputStructureElement(OutputStructureElement output) {	this.currentElement = output;}}