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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import com.beck.ep.team.IFilePacker;

public class ZipCreator implements IFilePacker {

	public void packToFile(Map<String, String> param, IProject project, List<IFile> toPackFiles, 
			Map<File, String> toPackJavaFiles, IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Preparing...", 2);
		String fileAbsPath = param.get(IFilePacker.PARAM_FILE_ABS_PATH);
		String rootDir = param.get(IFilePacker.PARAM_ROOT_DIRNAME);
		if (rootDir != null) {
			if (rootDir.length() == 0) {
				rootDir = null;
			} else if (!rootDir.endsWith("/")){
				rootDir = rootDir+"/";
			}
		}
		ZipUtil zipFile = ZipUtil.newZipFile(fileAbsPath, false);
		try {
			zipFile.setDefaultFolderPath(project.getRawLocation().toString());
			
			ArrayList<File> fs = new ArrayList<File>(toPackFiles.size()+10);
			for (Iterator iterator = toPackFiles.iterator(); iterator.hasNext();) {
				IFile file = (IFile) iterator.next();
				fs.add(file.getRawLocation().toFile());
			}
			for (Iterator iterator = toPackJavaFiles.keySet().iterator(); iterator.hasNext();) {
				File f = (File)iterator.next();
				if (!f.isDirectory()) {
					fs.add(f);
					iterator.remove();
				}
			}
			//folder must be added after file -> exception may throw if "add file" after "add folder"  
			if (monitor.isCanceled()) {
				return;
			}
			monitor.beginTask("Create zip...", fs.size()+toPackJavaFiles.size());
			if (fs.size() > 0) {
				zipFile.addFiles(fs, rootDir);
			}
			if (monitor.isCanceled()) {
				return;
			}
			monitor.worked(fs.size());
			for (Iterator iterator = toPackJavaFiles.entrySet().iterator(); !monitor.isCanceled() && iterator.hasNext();) {
				Map.Entry e = (Map.Entry)iterator.next();
				File f = (File)e.getKey();
				String root = (String)e.getValue();
				if (rootDir != null) {
					root = rootDir + root;
				}
				zipFile.addFolder(f, root);
				monitor.worked(1);
			}
		} finally {
			zipFile.close();
		}
		monitor.done();
	}
	
}
