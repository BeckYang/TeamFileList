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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.beck.ep.team.IFilePacker;
import com.beck.ep.team.TFMPlugin;

public class ProjectFileList implements IRunnableWithProgress {
	IProject project;
	ArrayList<IFile> files;
	Map<File, String> jfiles;
	Pattern ptn = Pattern.compile("[\r\n]+");

	public ProjectFileList(IProject project) {
		this.project = project;
	}
	
	public String getProjectName() {
		return project.getName();
	}

	public boolean initFiles(String text, boolean throwErrorWhenNotFound) throws FileNotFoundException {
		files = new ArrayList<IFile>();
		jfiles = new HashMap<File, String>();
		String[] sa = ptn.split(text);
		for (int i = 0; i < sa.length; i++) {
			IFile f = null;
			if (sa[i].length() > 0) {
				f = project.getFile(sa[i]);
			}
			if (f != null) {
				boolean addJF = true;
				if (f.exists()) {
					files.add(f);
					addJF = false;
				}
				if (addJF) {
					File jf = f.getRawLocation().toFile();
					if (jf.exists()) {
						String parentDir = f.getProjectRelativePath().removeLastSegments(1).toString();
						jfiles.put(jf, parentDir);
					} else if (throwErrorWhenNotFound) {
						throw new FileNotFoundException(sa[i]);
					}
				}
			}
		}
		return files.size() > 0 || jfiles.size() > 0;
	}

	public void pack(Map<String, String> param, IFilePacker packer, IProgressMonitor monitor) throws Exception {
		packer.packToFile(param, project, files, jfiles, monitor);
	}
	
	private ZipUtil unzipFile;
	private IAccessText fAccessText;
	public boolean setUnzipFilePath(String zipAbsPath, String pwd, IAccessText accessText) throws Exception {
		unzipFile = ZipUtil.newUnzip(zipAbsPath);
		this.fAccessText = accessText;
		if (unzipFile.isEncrypted()) {
			if (pwd == null) {
				return false;
			} else {
				unzipFile.setPassword(pwd);
			}
		}
		return true;
	}
	
	public void run(IProgressMonitor monitor) {
		try {
			unzipFile.extract(monitor, project);
			fAccessText.showMessage(IAccessText.MSG_TYPE_INFO, "unzip " + unzipFile.getFile().getAbsolutePath() + " finished.");
		} catch (Exception e) {
			String msg = "unzip " + unzipFile.getFile().getAbsolutePath() + "error!";
			fAccessText.showMessage(IAccessText.MSG_TYPE_ERROR, msg);
			TFMPlugin.error(msg, e);
		}
		monitor.done();
	}

}
