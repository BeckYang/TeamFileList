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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.osgi.service.prefs.Preferences;

import com.beck.ep.team.TFMPlugin;
import com.beck.ep.team.internal.IAccessText;

public class FileListMenuMgr extends MenuManager {//implements IMenuListener
	private static final String CFG_ACTION_ID = "FileListMenuMgr_Separator";
	private Preferences pref;
	private IAccessText input;

	public FileListMenuMgr(String text, String id, IAccessText input, Preferences preferences) {
		super(text, id);
		this.pref = preferences;
		this.input = input;
		Separator sp = new Separator();
		sp.setId(CFG_ACTION_ID);
		//addMenuListener(this);
		add(sp);
		add(new XAction("&Save as...", 1));
		
		String defCfgName = pref.get("selectedList", "");
		try {
			String[] sa = pref.node("fileList").keys();
			for (int i = 0; i < sa.length; i++) {
				if (addItems(sa[i], defCfgName) != null) {;
					loadFileList(sa[i]);
				}
			}
		} catch (Exception e) {
			TFMPlugin.error("new FileListMenuMgr", e);
		}
	}
	
	private void loadFileList(String id) {
		pref.put("selectedList", id);
		String newTxt = pref.node("fileList").get(id, "");
		input.setText(newTxt);
	}
	
	private String addItems(String cfgName, String defCfgName) {
		String ret = null;
		XAction a = new XAction(cfgName);
		if (cfgName.equals(defCfgName)) {
			a.setChecked(true);
			ret = cfgName;
		}
		insertBefore(CFG_ACTION_ID, a);
		return ret;
	}
	
	private void saveAs() {
		InputDialog dialog = new InputDialog(input.getShell(), "Save file list", "Input file list name", pref.get("selectedList", ""), null);//new RegexValidator("^[0-9,]+$","Please input number and separator by char ','"));
		if (dialog.open() != InputDialog.OK) {
			return;
		}
		String key = dialog.getValue();
		if (key.length() == 0) {
			return;
		}
		IContributionItem[] items = getItems();
		boolean exists = false;
		for (int i = 0; i < items.length; i++) {
			if (CFG_ACTION_ID.equals(items[i].getId())) {
				i = items.length;//stop at this node...
			} else {
				ActionContributionItem aitem = (ActionContributionItem)items[i];
				boolean match = aitem.getAction().getId().equals(key);
				if (match) {
					exists = true;
				}
				aitem.getAction().setChecked(match);
			}
		}
		Preferences p = pref.node("fileList");
		p.put(key, input.getText());
		if (!exists) {
			addItems(key, key);
			pref.put("selectedList", key);
		}
	}
	
	private class XAction extends Action {
		private int type;
		private XAction(String cfgName) {
			super(cfgName, IAction.AS_RADIO_BUTTON);
			setId(cfgName);
			type = 0;
		}
		private XAction(String text, int type) {
			super(text);
			this.type = type;
		}
		public void run() {
			if (type==1) {
				saveAs();
				return;
			}
			if (!this.isChecked()) {
				return;//for IAction.AS_RADIO_BUTTON, two event are fired - 1st obj.isChecked()=false, 2nd obj.iChecked()=true
			} else {
				loadFileList(getId());
			}
		}
	}
}
