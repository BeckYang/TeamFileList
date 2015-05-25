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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
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
		add(new XAction("&Define new...", null, null));
		
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
		DefineExtractDialog dialog = new DefineExtractDialog(fAccessText.getShell(), SWT.NONE);
		if (dialog.open() == DefineExtractDialog.OK) {
			String key = dialog.getName();
			try {
				boolean exists  = pref.nodeExists(key);
				Preferences p = pref.node(key);
				p.put("zipFile", dialog.getZipFile());
				p.put("extractDir", dialog.getExtractDir());
				if (!exists) {
					addItems(key, dialog.getZipFile(), dialog.getExtractDir());
				}
			} catch (Exception e) {
				TFMPlugin.error("defineNew error", e);
			}
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
		XAction a = new XAction(name, zipFile, extractDir);
		insertBefore(CFG_ACTION_ID, a);
	}

	private class XAction extends Action {
		private String zipFile;
		private String extractDir;
		private XAction(String name, String zipFile, String extractDir) {
			super(name);
			setId(name);
			this.zipFile = zipFile;
			this.extractDir = extractDir;
		}
		public void run() {
			if (zipFile == null || extractDir == null) {
				defineNew();
				return;
			}
			extractZipTo(zipFile, extractDir);
		}
	}

}
