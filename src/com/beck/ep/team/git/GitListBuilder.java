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

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.dialogs.CompareTargetSelectionDialog;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import com.beck.ep.team.IListBuilder;
import com.beck.ep.team.ui.CheckBoxAction;

public class GitListBuilder implements IListBuilder {
	protected String error;
	protected Preferences pref;
	
	public void addConfigMenu(IMenuManager menu) {
		MenuManager gitMgr = new MenuManager("&Git");
		gitMgr.add(GitModeMenuMgr.newMenuMgr(GitModeMenuMgr.PREF_TYPE_MODE, pref));
		gitMgr.add(GitModeMenuMgr.newMenuMgr(GitModeMenuMgr.PREF_TYPE_SOURCE, pref));
		gitMgr.add(GitModeMenuMgr.newMenuMgr(GitModeMenuMgr.PREF_TYPE_IGNORE, pref));
		gitMgr.add(new CheckBoxAction("&Include untrack files", pref, GitModeMenuMgr.PREF_TYPE_UNTRACK, true));
		menu.add(gitMgr);
	}
	
	public void setPreferences(Preferences preferences) {
		pref = preferences;
	}
	
	public String getError() {
		return error;
	}
	
	private int addTree(int mode, Shell shell, Repository rep, RevWalk rw, TreeWalk tw) throws Exception {
		int index = -1;
		switch (mode) {
		case GitModeMenuMgr.MODE_STAGING:
			tw.addTree(new DirCacheIterator(rep.readDirCache()));
			break;
		case GitModeMenuMgr.MODE_OTHERS://choose a GIT ref
			CompareTargetSelectionDialog dialog = new CompareTargetSelectionDialog(shell, rep, null);
			if (dialog.open() != CompareTargetSelectionDialog.OK) {
				return -2;
			}
			ObjectId obId = rep.getRef(dialog.getRefName()).getObjectId();//"master"
			tw.addTree(rw.parseCommit(obId).getTree());
			break;
		case GitModeMenuMgr.MODE_BRANCH_HEAD:
			Ref head = rep.getRef(Constants.HEAD);
			RevCommit rc = rw.parseCommit(head.getObjectId());
			tw.addTree(rc.getTree());
			break;
		default://compare to working area
			index = tw.addTree(new FileTreeIterator(rep));
			break;
		}
		return index;
	}

	public String[] createList(Shell shell, IProject project) throws Exception {
		error = null;
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null) {
			error = "Git Repository not found (project: "+project.getName()+")";
			return null;
		}
		Repository rep = mapping.getRepository();
		String pjPath = project.getLocation().toFile().getAbsolutePath();
		String repPath = rep.getDirectory().getParentFile().getAbsolutePath();
		int cutPrefixLen = 0;
		int pathDiffLen = pjPath.length() - repPath.length();
		if (pathDiffLen > 0) {
			cutPrefixLen = pathDiffLen;//for repositoy is not in "parent folder of project"
		}
		
		RevWalk rw = new RevWalk(rep);
		TreeWalk tw = new TreeWalk(rep);
		
		int ignoreOpt = pref.getInt(GitModeMenuMgr.PREF_TYPE_IGNORE, GitModeMenuMgr.IGNORE_EXCLUDE);
		if (ignoreOpt == GitModeMenuMgr.IGNORE_ONLY) {
			String[] sa = listIgnored(rep, rw, tw, cutPrefixLen);
			if (cutPrefixLen > 0) {
				for (int i = 0; i < sa.length; i++) {
					if (sa[i].length() > cutPrefixLen) {
						sa[i] = sa[i].substring(cutPrefixLen);
					}
				}
			}
			return sa;
		}
		
		int mode = pref.getInt(GitModeMenuMgr.PREF_TYPE_MODE, GitModeMenuMgr.MODE_BRANCH_HEAD);
		int sourceMode = pref.getInt(GitModeMenuMgr.PREF_TYPE_SOURCE, GitModeMenuMgr.MODE_WORKING_AREA);
		if (mode == sourceMode) {
			return new String[0];
		}
		int index = addTree(mode, shell, rep, rw, tw);
		if (index == -2) {
			return null;
		}
		int stageIndex = -1;
		boolean excludeUntrackOpt = false;
		if (!pref.getBoolean(GitModeMenuMgr.PREF_TYPE_UNTRACK, true) && sourceMode == GitModeMenuMgr.MODE_WORKING_AREA) {
			excludeUntrackOpt = true;
			if (mode == GitModeMenuMgr.MODE_STAGING) {
				stageIndex = 0;
			}
		}
		
		int idx2 = addTree(sourceMode, shell, rep, rw, tw);
		if (excludeUntrackOpt && stageIndex == -1) {
			addTree(GitModeMenuMgr.MODE_STAGING, shell, rep, rw, tw);
			stageIndex = 2;
		}
		// skip ignored resources
		if (ignoreOpt == GitModeMenuMgr.IGNORE_EXCLUDE && (index != -1 || idx2 != -1)) {
			int pos = (index != -1)?index:idx2;
			NotIgnoredFilter notIgnoredFilter = new NotIgnoredFilter(pos);//index = tw.addTree(?)
			tw.setFilter(notIgnoredFilter);
		}
		
		tw.setRecursive(true);
		ArrayList<String> sa = new ArrayList<String>();
		while (tw.next()) {
			if (!tw.idEqual(0, 1)) {
				if (excludeUntrackOpt) {
					if (tw.getTree(stageIndex, AbstractTreeIterator.class) == null) {
						WorkingTreeIterator t = tw.getTree(1, WorkingTreeIterator.class);
						if (t != null && !t.isEntryIgnored()) {//file is untrack...
							continue;
						}
					}
				}
				String relPath = tw.getPathString();
				if (cutPrefixLen > 0 && relPath.length() > cutPrefixLen) {
					relPath = relPath.substring(cutPrefixLen);
				}
				sa.add(relPath);
			}
		}
		
		return sa.toArray(new String[sa.size()]);
	}

	private String[] listIgnored(Repository rep, RevWalk rw, TreeWalk tw, int cutPrefixLen) throws Exception {
		addTree(GitModeMenuMgr.MODE_STAGING, null, rep, rw, tw);
		int workingTreeIndex = addTree(GitModeMenuMgr.MODE_WORKING_AREA, null, rep, rw, tw);
		IndexDiffFilter filter = new IndexDiffFilter(0, workingTreeIndex, true);
		tw.setFilter(filter);
		tw.setRecursive(true);
		while (tw.next()) {
			//just loop it...
		}
		Set<String> s = filter.getIgnoredPaths();
		return s.toArray(new String[s.size()]);
	}

}
