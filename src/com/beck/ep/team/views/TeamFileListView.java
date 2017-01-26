/*
 New BSD License http://www.opensource.org/licenses/bsd-license.php
 Copyright (c) 2015, Beck Yang
 All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright notice, this 
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this 
   list of conditions and the following disclaimer in the documentation and/or 
   other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.beck.ep.team.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.osgi.service.prefs.Preferences;

import com.beck.ep.team.IFilePacker;
import com.beck.ep.team.IListBuilder;
import com.beck.ep.team.TFMPlugin;
import com.beck.ep.team.internal.IAccessText;
import com.beck.ep.team.internal.PackJob;
import com.beck.ep.team.internal.ProjectFileList;
import com.beck.ep.team.internal.TeamListBuilder;
import com.beck.ep.team.ui.CustomUnpackMgr;
import com.beck.ep.team.ui.FileListMenuMgr;
import com.beck.ep.team.ui.SourceViewerKeyHandler;


public class TeamFileListView extends ViewPart {
	//public static final String ID = "com.beck.ep.team.views.TeamFileListView";

	private String rootDir;
	private IAccessText fAT;
	public boolean deleteIfExist = true;
	
	//UI
	private Action createListAction;
	private Action zipAction;
	private Action unzipAction;
	private Action warPatchAction;
	private Action otherCfgAction;
	private Action sortAction;
	private Action ckBlockAction;
	//private Action testAction;
	private Action ckCheckFileAction;
	private Action ckSaveWhenExitAction;
	FileListMenuMgr loadListMgr;
	IMenuManager customUnpackMgr;
	private SourceViewer edFileList;
	private IDocument fDoc;
	private Shell fShell;
	private Label fMsg;
	private Color redColor;
	private Color msgColor;
	
	public TeamFileListView() {
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		edFileList.getTextWidget().setFocus();
	}
	
	private Shell _getShell() {
		return fShell;//edFileList.getShell();
	}
	
	private String _getText() {
		IDocument doc = fDoc;
		return doc.get();
	}
	
	private void _setText(String s) {
		IDocument doc = fDoc;
		doc.set(s);
	}
	
	private void setTextFromBG(String s) {
		if (Display.getCurrent() != null) {
			_setText(s);
			return;
		}
		BGMsg bgmsg = new BGMsg();
		bgmsg.mode = 1;
		bgmsg.message = s;
		fShell.getDisplay().asyncExec(bgmsg);
	}
	
	private void showMessageFromBG(int msgType, String s) {
		if (Display.getCurrent() != null) {
			if (msgType == IAccessText.MSG_TYPE_ERROR) {
				showError(s);
			} else {
				showMessage(s);
			}
			return;
		}
		BGMsg bgmsg = new BGMsg();
		bgmsg.mode = msgType;
		bgmsg.message = s;
		fShell.getDisplay().asyncExec(bgmsg);
	}
	
	private class DropListender implements DropTargetListener {
		Transfer transfer = null;
		private DropListender(Transfer tx) {
			transfer = tx;
		}
		public void dragEnter(DropTargetEvent event) {
			for (int i = 0; i < event.dataTypes.length; i++) {
				if (transfer.isSupportedType(event.dataTypes[i])){
					event.currentDataType = event.dataTypes[i];
					i = event.dataTypes.length;
				}
			}
			event.detail = DND.DROP_COPY;//change operation to copy - keep original file...  
		}

		public void dropAccept(DropTargetEvent event) {
			//do nothing
		}
		public void dragLeave(DropTargetEvent event) {
			//do nothing
		}

		public void dragOperationChanged(DropTargetEvent event) {
			if (event.detail == DND.DROP_DEFAULT) {
				if ((event.operations & DND.DROP_COPY) != 0) {
					event.detail = DND.DROP_COPY;
				} else {
					event.detail = DND.DROP_NONE;
				}
			}
		}

		public void dragOver(DropTargetEvent event) {
			event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
		}

		public void drop(DropTargetEvent event) {
			if (!transfer.isSupportedType(event.currentDataType)){
				return;
			}
			IProject currProj = getProject();
			if (currProj == null){
				return;
			}
			IResource[] resa = (IResource[])event.data;
			StringBuilder sb = new StringBuilder(256);
			sb.append(_getText());
			int len = sb.length();
			if (len > 0 && sb.charAt(len-1) == '\n') {
				sb.setLength(len-1);
			}
			for (int i = 0; i < resa.length; i++) {
				IResource res = resa[i];
				if (res.getProject().equals(currProj)) {
					sb.append("\n").append(res.getProjectRelativePath());
				}
			}
			_setText(sb.toString());
		}
	}
	
	private class BGMsg implements Runnable {
		int mode;
		String message;
		public void run() {
			switch(mode) {
			case 1:
				_setText(message);
				break;
			case IAccessText.MSG_TYPE_INFO:
				showMessage(message);
				break;
			case IAccessText.MSG_TYPE_ERROR:
				showError(message);
				break;
			}
		}
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		fShell = parent.getShell();
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, true);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		fMsg = new Label(container, SWT.NONE);
		fMsg.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
		msgColor = fMsg.getDisplay().getSystemColor(SWT.COLOR_BLACK);
		redColor = fMsg.getDisplay().getSystemColor(SWT.COLOR_RED);
		
		fDoc = new Document();
		edFileList = new SourceViewer(container, null, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		edFileList.setDocument(fDoc);
		edFileList.configure(new SourceViewerConfiguration());//for undoManager & other... 
		edFileList.getTextWidget().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		edFileList.getTextWidget().setFont(JFaceResources.getTextFont());
		DropTarget target = new DropTarget(edFileList.getTextWidget(),DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
		target.setTransfer(new Transfer[]{ResourceTransfer.getInstance()});
		target.addDropListener(new DropListender(target.getTransfer()[0]));
		new SourceViewerKeyHandler().install(edFileList);
		
		fAT = new IAccessText() {
			public void setText(String newTxt) {
				setTextFromBG(newTxt);
			}
			public String getText() {
				return _getText();
			}
			public void showMessage(int msgType, String s) {
				showMessageFromBG(msgType, s);
			}
			public Shell getShell() {
				return _getShell();
			}
		};
		
		boolean newBlockSelection = true;
		try {
			edFileList.getTextWidget().getBlockSelection();
		} catch (Throwable e) {
			newBlockSelection = false;
		}
		makeActions(newBlockSelection);
		
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(loadListMgr);
		manager.add(customUnpackMgr);
		manager.add(otherCfgAction);
		manager.add(ckCheckFileAction);
		manager.add(ckSaveWhenExitAction);
		
		IListBuilder lb = TeamListBuilder.newListBuilder(TeamListBuilder.REPOSITORY_ID_GIT);
		if (lb != null) {
			lb.addConfigMenu(manager);
		}
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(createListAction);
		if (ckBlockAction != null) {
			manager.add(ckBlockAction);
		}
		//manager.add(testAction);//TODO for debug
		manager.add(sortAction);
		manager.add(new Separator());
		manager.add(unzipAction);
		manager.add(zipAction);
		if (warPatchAction != null) {
			manager.add(warPatchAction);
		}
		
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private IProject prevProject = null;
	private IProject getProject() {
		IProject project = null;
		try {
			IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			int viewCount = 3;
			String[] viewId = new String[viewCount*2];
			IWorkbenchPage page = win.getActivePage();
			IViewReference[] viewRef = page.getViewReferences();
			//find best fit view
			for (int i = 0; i < viewRef.length; i++) {
				String id = viewRef[i].getId();
				int pos = -1;
				if ("org.eclipse.jdt.ui.PackageExplorer".equals(id)) {
					pos = 0;
				} else if ("org.eclipse.ui.navigator.ProjectExplorer".equals(id)) {
					pos = 1;
				} else if ("org.eclipse.ui.views.ResourceNavigator".equals(id)) {
					pos = 2;
				}
				if (pos != -1) {
					IWorkbenchPart part = viewRef[i].getPart(false);
					if (page.isPartVisible(part)) {
						viewId[pos] = id;
					} else {
						viewId[pos+viewCount] = id;
					}
				}
			}
			
			for (int i = 0; i < viewId.length && project == null; i++) {
				if (viewId[i] != null) {
				ISelection selection = win.getSelectionService().getSelection(viewId[i]);
				if (selection instanceof IStructuredSelection) {
					Object o = ((IStructuredSelection)selection).getFirstElement();
					IResource res = null;
					if (o instanceof IResource) {
						res = (IResource)o;
					} else if (o instanceof IAdaptable) {
						res = (IResource)((IAdaptable)o).getAdapter(IResource.class);
					}
					if (res != null) {
						project = res.getProject();
					}
				}
				}
			}
			if (project == null) {
				IEditorInput input = win.getActivePage().getActiveEditor().getEditorInput();
				if (input instanceof IFileEditorInput) {
					project = ((IFileEditorInput)input).getFile().getProject();
				}
			}
		} catch (Exception e) {
		}
		if (project == null) {
			project = prevProject;
		} else {
			prevProject = project;
			if (!project.isOpen()) {
				return null;
			}
		}
		return project;
	}
	
	private void createList() {
		IProject project = getProject();
		if (project == null) {
			showError("Please select valid project");
			return;
		}
		try {
			TeamListBuilder tb = new TeamListBuilder();
			String[] sa = tb.createList(_getShell(), project);
			if (sa != null) {
				setFileList(sa);
				showMessage("List files for project["+project.getName()+"]");
			} else {
				String error = tb.getError();
				if (error != null) {
					showError(error);
				}
			}
		} catch (Throwable e) {
			TFMPlugin.error("createList", e);
			showError("error: "+e.getMessage());
		}
	}
	
	private void setFileList(String[] sa) {
		Arrays.sort(sa);
		StringBuilder sb = new StringBuilder(sa.length * 48);
		for (int i = 0; i < sa.length; i++) {
			sb.append(sa[i]).append('\n');
		}
		_setText(sb.toString());
	}
	
	private void pack(IFilePacker packer, String actionDesc, String suffix) {
		IProject project = getProject();
		if (project == null) {
			showError("Please select valid project");
			return;
		}
		ProjectFileList plist = new ProjectFileList(project);
		try {
			if (!plist.initFiles(_getText(), ckCheckFileAction.isChecked())) {
				showError("All files cannot be found under project["+project.getName()+"]");
				return;
			}
		} catch (FileNotFoundException e) {
			showError("["+e.getMessage()+"] not found under project["+project.getName()+"]");
			return;
		}
		FileDialog dialog = new FileDialog(_getShell(), SWT.SAVE);
		dialog.setText("Compress files of project["+project.getName()+"] to zip file");
		dialog.setFilterExtensions(new String[]{"*"+suffix});
		String absPath = dialog.open();
		if (absPath != null) {
			if (!absPath.toLowerCase().endsWith(suffix)) {
				absPath = absPath + suffix;
			}
			if (deleteIfExist) {
				File f = new File(absPath);
				if (f.exists()) {
					f.delete();
				}
			}
			Map<String, String> param = new HashMap<String, String>();
			param.put(IFilePacker.PARAM_FILE_ABS_PATH, absPath);
			param.put(IFilePacker.PARAM_ROOT_DIRNAME, rootDir);
			try {
				//PlatformUI.getWorkbench().getProgressService().run(true, true, 
						new PackJob(plist, packer, actionDesc, param, fAT).schedule();
			} catch (Throwable e) {
			}
		}
	}
	
	
	
	private void unzip() {
		IProject project = getProject();
		if (project == null) {
			showError("Please select valid project");
			return;
		}
		ProjectFileList plist = new ProjectFileList(getProject());
		FileDialog dialog = new FileDialog(_getShell(), SWT.OPEN);
		dialog.setText("Unzip selected file to project["+project.getName()+"]");
		String zipAbsPath = dialog.open();
		if (zipAbsPath == null) {
			return;
		}
		try {
			if (!plist.setUnzipFilePath(zipAbsPath, null, fAT)) {
				InputDialog input = new InputDialog(_getShell(), "Input zip password", zipAbsPath + "required password to extract...", "", null);
				if (input.open() != InputDialog.OK) {
					return;
				}
				String pwd = input.getValue();
				if (!plist.setUnzipFilePath(zipAbsPath, pwd, fAT)) {
					return;
				}
			}
			PlatformUI.getWorkbench().getProgressService().run(true, true, plist);
		} catch (Exception e) {
		}
	}
	
	private class XAction extends Action {
		int type;
		private XAction(int type) {
			this.type = type;
		}
		private XAction(int type, String text, int style) {
			super(text, style);
			this.type = type;
		}
		
		public void run() {
			switch (type) {
			case 1: createList(); break;
			case 2: pack(TFMPlugin.getDefault().newFilePacker(TFMPlugin.PACKER_ZIP), "source", ".zip"); break;
			case 3: unzip(); break;
			case 4: blockSelection(); break;
			case 5: pack(TFMPlugin.getDefault().newFilePacker(TFMPlugin.PACKER_WAR_PATCH), "war patch",".zip"); break;
			case 6: otherCfg(); break;
			case 7: sortList(); break;
			case 900: saveCfg(); break;
			case 999: test(); break;
			}
		}
	}
	
	public void dispose() {
		if (TFMPlugin.getDefault().getPreferences().node("option").getBoolean("saveWhenExit", false)) {
			loadListMgr.saveList(fDoc.get());
		}
		super.dispose();
	}
	
	private void saveCfg() {
		Preferences option = TFMPlugin.getDefault().getPreferences().node("option");
		option.putBoolean("fileNotExist", ckCheckFileAction.isChecked());
		option.putBoolean("saveWhenExit", ckSaveWhenExitAction.isChecked());
	}
	
	private void makeActions(boolean newBlockSelection) {
		loadListMgr = new FileListMenuMgr("&Saved File list", "loadFileList", fAT, TFMPlugin.getDefault().getPreferences().node("fmenu"));
		customUnpackMgr = new CustomUnpackMgr("&Unpack defined setting", "runCustomAction", fAT, TFMPlugin.getDefault().getPreferences().node("customUnpack"));
		
		createListAction = new XAction(1);
		createListAction.setText("Create file list");
		createListAction.setToolTipText("List changed files of selected project");
		createListAction.setImageDescriptor(TFMPlugin.getImageDescriptor("/icons/cmp_tree.ico"));
		
		zipAction = new XAction(2);
		zipAction.setText("Zip files");
		zipAction.setToolTipText("Copy workspace files in the list to a zip file");
		zipAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor("IMG_ETOOL_EXPORT_WIZ"));
		
		unzipAction = new XAction(3);
		unzipAction.setText("Unzip files");
		unzipAction.setToolTipText("Unzip from external file to workspace project");
		unzipAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor("IMG_ETOOL_IMPORT_WIZ"));
		
		otherCfgAction = new XAction(6);
		otherCfgAction.setText("set root folder for new zip...");
		Preferences option = TFMPlugin.getDefault().getPreferences().node("option");
		ckCheckFileAction = new XAction(900, "show error when file not exists", IAction.AS_CHECK_BOX);
		ckCheckFileAction.setChecked(option.getBoolean("fileNotExist", true));
		ckSaveWhenExitAction = new XAction(900, "save file list when exit", IAction.AS_CHECK_BOX);
		ckSaveWhenExitAction.setChecked(option.getBoolean("saveWhenExit", false));
		
		if (TFMPlugin.getDefault().newFilePacker(TFMPlugin.PACKER_WAR_PATCH) != null) {
			warPatchAction = new XAction(5);
			warPatchAction.setText("Create WAR patch");
			warPatchAction.setToolTipText("Create WAR patch base on file list to a zip file");
			warPatchAction.setImageDescriptor(TFMPlugin.getImageDescriptor("/icons/war_patch.ico"));
		}
		
		if (newBlockSelection) {
			ckBlockAction = new XAction(4, "Taggle Block Selection Mode", Action.AS_CHECK_BOX);
			try {
				ckBlockAction.setImageDescriptor(
						ImageDescriptor.createFromURL(new java.net.URL("platform:/plugin/org.eclipse.ui.workbench.texteditor/icons/full/etool16/block_selection_mode.gif")));
			} catch (Exception e) {
			}
		}
		sortAction = new XAction(7);
		sortAction.setText("Sort file list");
		try {
			sortAction.setImageDescriptor(
					ImageDescriptor.createFromURL(new java.net.URL("platform:/plugin/org.eclipse.jdt.ui/icons/full/dlcl16/alphab_sort_co.gif")));
		} catch (Exception e) {
		}
		
		/*testAction = new XAction(999);
		testAction.setText("test...");
		testAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT));*/
	}
	private void sortList() {
		_setText(ProjectFileList.sort(_getText()));
	}
	
	private void showError(String message) {
		fMsg.setForeground(redColor);
		fMsg.setText(message);
	}
	
	private void showMessage(String message) {
		fMsg.setForeground(msgColor);
		fMsg.setText(message);
	}
	
	private void otherCfg() {
		//IEclipsePreferences pref = TFMPlugin.getDefault().getPreferences();
		//pref.get(IFilePacker.PARAM_ROOT_DIRNAME, "");
		InputDialog dialog = new InputDialog(_getShell(), "Set root folder for new zip", "Specify root folder for creating zip file", rootDir, null);
		if (dialog.open() == InputDialog.OK) {
			String newVal = dialog.getValue();
			if (newVal.length() == 0) {
				newVal = null;
			}
			rootDir = newVal;
			//pref.put(IFilePacker.PARAM_ROOT_DIRNAME, newVal);
		}
	}
	
	private void blockSelection() {
		StyledText w = edFileList.getTextWidget();
		w.setBlockSelection(!w.getBlockSelection());
	}

	private void test() {
		/*
		IProject project = getProject();
		if (project == null) {
			showMessage("Please select valid project");
			return;
		}*/
	}
}
