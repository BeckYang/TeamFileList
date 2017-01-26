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
package com.beck.ep.team.ui;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.osgi.service.prefs.Preferences;

import com.beck.ep.team.TFMPlugin;
import com.beck.ep.team.internal.IAccessText;
import com.beck.ep.team.internal.ZipUtil;

public class CustomUnpackMgr extends MenuManager {
	private static final String CFG_ACTION_ID = "CustomMenuMgr_Separator";
	private Preferences pref;
	private IAccessText fAccessText;
	
	public CustomUnpackMgr(String text, String id, IAccessText accessText, Preferences preferences) {
		super(text, id);
		this.pref = preferences;
		this.fAccessText = accessText;
		Separator sp = new Separator();
		sp.setId(CFG_ACTION_ID);
		add(sp);
		add(new XAction("&Define new...", 1));
		add(new XAction("&Modify/Save as...", 2));
		add(new XAction("&Remove...", 3));
		
		try {
			String[] sa = pref.childrenNames();
			for (int i = 0; i < sa.length; i++) {
				Preferences p = pref.node(sa[i]);
				addItems(sa[i], p.get("zipFile", null), p.get("extractDir", null));
			}
		} catch (Exception e) {
			TFMPlugin.error("init error", e);
		}
	}
	
	private void defineNew() {
		try {
			showEditDialog(null);
		} catch (Exception e) {
			TFMPlugin.error("showEditDialog error", e);
		}
	}
	private void showEditDialog(Preferences currPref) throws Exception {
		DefineExtractDialog dialog = new DefineExtractDialog(fAccessText.getShell(), SWT.RESIZE);
		if (currPref != null) {
			dialog.initDefault(currPref.name(), currPref.get("zipFile", null), currPref.get("extractDir", null));
		}
		if (dialog.open() == DefineExtractDialog.OK) {
			String key = dialog.getName();
			boolean exists  = pref.nodeExists(key);
			Preferences p = pref.node(key);
			p.put("zipFile", dialog.getZipFile());
			p.put("extractDir", dialog.getExtractDir());
			if (!exists) {
				addItems(key, dialog.getZipFile(), dialog.getExtractDir());
			}
		}
	}
	private void modifyPre() {
		try {
			Preferences p = selectPreferences("Select setting that you want to modify");
			if (p != null) {
				showEditDialog(p);
			}
		} catch (Exception e) {
			TFMPlugin.error("modifyPre error", e);
		}
	}
	private void deletePre() {
		try {
			Preferences p = selectPreferences("Select setting that you want to remove");
			if (p != null) {
				remove(p.name());
				p.removeNode();
			}
		} catch (Exception e) {
			TFMPlugin.error("deletePre error", e);
		}
	}
	private Preferences selectPreferences(String title) throws Exception {
		String[] sa = pref.childrenNames();
		if (sa.length == 0){
			return null;
		}
		Preferences[] pa = new Preferences[sa.length];
		for (int i = 0; i < pa.length; i++) {
			pa[i] = pref.node(sa[i]);
		}
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(fAccessText.getShell(), new PLP());
		dialog.setTitle(title);
		dialog.setElements(pa);
		dialog.setMessage("Type to filter by name:");
		dialog.setMultipleSelection(false);
		if (dialog.open() == ElementListSelectionDialog.OK) {
			return (Preferences)dialog.getResult()[0];
		}
		return null;
	}
	private static class PLP extends LabelProvider {
		public String getText(Object element) {
			Preferences p = (Preferences)element;
			return p.name()+" - extract ["+p.get("zipFile", "")+"] to ["+p.get("extractDir", "")+"]";
		}
	}
	
	private void extractZipTo(String zipFile, String extractDir) {
		try {
			ZipUtil.extractAll(zipFile ,extractDir);
			fAccessText.showMessage(IAccessText.MSG_TYPE_INFO, "extract ["+zipFile+"] to ["+extractDir+"] finished.");
		} catch (Exception e) {
			String msg = "extract ["+zipFile+"] to ["+extractDir+"] error: "+e.getMessage();
			fAccessText.showMessage(IAccessText.MSG_TYPE_ERROR, msg);
			TFMPlugin.error(msg, e);
		}
	}
	
	private void addItems(String name, String zipFile, String extractDir) {
		XAction a = new XAction(name, 0);
		insertBefore(CFG_ACTION_ID, a);
	}
	
	private class ExtractJob extends Job {
		private String zipFile;
		private String extractDir;
		public ExtractJob(String zipFile, String extractDir) {
			super("extract ["+zipFile+"] to ["+extractDir+"]");
			this.zipFile = zipFile;
			this.extractDir = extractDir;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			fAccessText.showMessage(IAccessText.MSG_TYPE_ERROR, "Waiting workspace action complete...");
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) {
					}
				}, new NullProgressMonitor());
			} catch (Exception e) {
				TFMPlugin.info("extractZipTo --> waiting error", e);
			}
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			extractZipTo(zipFile, extractDir);
			return Status.OK_STATUS;
		}
	}

	private class XAction extends Action {
		private int type;
		private XAction(String name, int type) {
			super(name);
			this.type = type;
			if (type == 0) {
				setId(name);
			}
		}
		public void run() {
			switch (type) {
			case 1: defineNew(); return;
			case 2: modifyPre(); return;
			case 3: deletePre(); return;
			}
			Preferences p = pref.node(getId());
			//extractZipTo(zipFile, extractDir);
			new ExtractJob(p.get("zipFile", null), p.get("extractDir", null)).schedule();
		}
	}

}
