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

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.beck.ep.team.IFilePacker;
import com.beck.ep.team.TFMPlugin;

public class PackJob extends Job {
	ProjectFileList plist;
	IFilePacker packer;
	String actionDesc;
	Map<String, String> param;
	IAccessText fAT;
	
	public PackJob(ProjectFileList plist, IFilePacker packer, String actionDesc, Map<String, String> param, IAccessText fAT) {
		super("Pack "+actionDesc+" files of Project["+plist.getProjectName()+"]");
		this.plist = plist;
		this.packer = packer;
		this.actionDesc = actionDesc;
		this.param = param;
		this.fAT = fAT;
	}
	
	protected IStatus run(IProgressMonitor monitor) {
		try {
			plist.pack(param, packer, monitor);
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			} else {
				fAT.showMessage(IAccessText.MSG_TYPE_INFO, "Pack "+actionDesc+" files of Project["+plist.getProjectName()+"] finish.");
				return Status.OK_STATUS;
			}
		} catch (Exception e) {
			//TFMPlugin.error("pack error", e);
			return new Status(IStatus.ERROR, TFMPlugin.PLUGIN_ID, "pack error", e);
		}
	}
}
