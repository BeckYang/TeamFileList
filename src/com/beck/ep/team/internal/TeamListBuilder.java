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
package com.beck.ep.team.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;

import com.beck.ep.team.IListBuilder;
import com.beck.ep.team.TFMPlugin;

public class TeamListBuilder {
	public static String REPOSITORY_ID_GIT = "org.eclipse.egit.core.GitProvider";
	
	protected String error;
	public String getError() {
		return error;
	}
	
	public static IListBuilder newListBuilder(String respId) {
		Class cz = null;
		if (REPOSITORY_ID_GIT.equals(respId)) {
			cz = com.beck.ep.team.git.GitListBuilder.class;
		}
		if (cz != null) {
			try {
				IListBuilder lb = (IListBuilder)(cz.newInstance());
				lb.setPreferences(TFMPlugin.getDefault().getPreferences().node(respId));
				return lb;
			} catch (Throwable e) {
			}
		}
		return null;
	}

	public String[] createList(Shell shell, IProject project) throws Exception {
		error = null;
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider == null) {
			error = "No Repository provider exist. Project: "+project.getName();
			return null;
		}
		IListBuilder lb = newListBuilder(provider.getID());
		if (lb != null) {
			String[] sa = lb.createList(shell, project);
			error = lb.getError();
			return sa;
		}
		
		Subscriber subscriber = provider.getSubscriber();
		
		SyncInfoSet set = new SyncInfoSet();
		//set.add(SyncInfo.CONFLICTING);
		
		Filter filter = new Filter();
		ProgressMonitorDialog monitor = new ProgressMonitorDialog(shell);
		monitor.setCancelable(true);
		monitor.open();
		IResource[] members = project.members();
		subscriber.collectOutOfSync(members, IResource.DEPTH_INFINITE, set, monitor.getProgressMonitor());
		monitor.close();
		
		SyncInfo[] infos = set.getNodes(filter);
		String[] sa = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			sa[i] = infos[i].getLocal().getProjectRelativePath().toString();
		}
		return sa;
	}
	
	public class Filter extends FastSyncInfoFilter {
		public int kind = SyncInfo.OUTGOING;
		public int resType = IResource.FILE;
		
		public boolean select(SyncInfo info) {
			if (info.getLocal().getType() != resType) {
				return false;
			}
			int ik = info.getKind();
			int stat = (ik & kind);
			return stat > 0;
		}
	}
}
