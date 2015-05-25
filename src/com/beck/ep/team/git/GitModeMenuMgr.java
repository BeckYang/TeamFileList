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
package com.beck.ep.team.git;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.osgi.service.prefs.Preferences;

import com.beck.ep.team.internal.TeamListBuilder;

public class GitModeMenuMgr extends MenuManager {
	public static final String PREF_TYPE_MODE = "mode";
	public static final String PREF_TYPE_SOURCE = "source";
	public static final String PREF_TYPE_IGNORE = "ignore";
	public static final String PREF_TYPE_UNTRACK = "untrack";
	
	public static final int MODE_WORKING_AREA = 0;
	public static final int MODE_STAGING = 1;
	public static final int MODE_OTHERS = 2;
	public static final int MODE_BRANCH_HEAD = 3;
	
	public static final int IGNORE_EXCLUDE = 100;
	public static final int IGNORE_INCLUDE = 101;
	public static final int IGNORE_ONLY = 102;
	
	private String prefKey;
	private Preferences pref;
	
	static GitModeMenuMgr newMenuMgr(String prefType, Preferences pref) {
		String prefKey = PREF_TYPE_MODE;
		String text = "Compare &Target";
		int[] modes = null;
		//new String[]{"Working area","Staging area","Other branch/tag/reference"};
		if (PREF_TYPE_SOURCE.equals(prefType)) {
			prefKey = PREF_TYPE_SOURCE;
			text = "Compare &Source";
			modes = new int[]{MODE_WORKING_AREA, MODE_STAGING, MODE_BRANCH_HEAD};
		} else if (PREF_TYPE_IGNORE.equals(prefType)){
			prefKey = PREF_TYPE_IGNORE;
			text = "Compare &Mode";
			modes = new int[]{IGNORE_EXCLUDE, IGNORE_INCLUDE, IGNORE_ONLY};
		} else {
			modes = new int[]{MODE_BRANCH_HEAD, MODE_STAGING, MODE_OTHERS};
		}
		String id = TeamListBuilder.REPOSITORY_ID_GIT + prefKey;
		return new GitModeMenuMgr(text, id, pref, prefKey, modes);
	}
	
	GitModeMenuMgr(String text, String id, Preferences pref, String prefKey, int[] modes) {
		super(text, id);
		this.pref = pref;
		this.prefKey = prefKey;
		int def = pref.getInt(prefKey, modes[0]);
		for (int i = 0; i < modes.length; i++) {
			int mode = modes[i];
			String label = "Current branch head";
			switch (mode) {
				case MODE_OTHERS: 		label = "&Other branch/tag/reference"; break;
				case MODE_WORKING_AREA: label = "&Working area"; break;
				case MODE_STAGING: 		label = "&Staging area"; break;
				case IGNORE_EXCLUDE: 	label = "&Exclude ignored file"; break;
				case IGNORE_INCLUDE: 	label = "&Include ignored file"; break;
				case IGNORE_ONLY: 		label = "&Only ignored files"; break;
			}
			XAction action = new XAction(label, mode);
			add(action);
			if (mode == def) {
				action.setChecked(true);
			}
		}
	}
	
	private class XAction extends Action {
		private int type;
		private XAction(String text, int type) {
			super(text, IAction.AS_RADIO_BUTTON);
			this.type = type;
		}
		public void run() {
			if (!this.isChecked()) {
				return;//for IAction.AS_RADIO_BUTTON, two event are fired - 1st obj.isChecked()=false, 2nd obj.iChecked()=true
			} else {
				pref.putInt(prefKey, type);
			}
		}
	}
	
}
