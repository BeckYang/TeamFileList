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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

import com.beck.ep.team.IFilePacker;

public class WarPatchCreator implements IFilePacker {
	public static String CLASS_OUTPUT_DIR = "WEB-INF/classes/";
	
	private IJavaProject javaProject;
	private IPath outputDir;
	
	public WarPatchCreator() {
		//throw error if org.eclipse.wst.common.** cannot be load...
		if (ComponentCore.class.getName() == null) {
			throw new RuntimeException("Required optional package: org.eclipse.wst.common.modulecore");
		}
	}
	
	public void packToFile(Map<String, String> param, IProject project, List<IFile> toPackFiles, 
			Map<File, String> toPackJavaFiles, IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Preparing...", 2);
		String fileAbsPath = param.get(IFilePacker.PARAM_FILE_ABS_PATH);
		String rootDir = param.get(IFilePacker.PARAM_ROOT_DIRNAME);
		if (rootDir != null) {
			if (rootDir.length() == 0) {
				rootDir = null;
			} else if (rootDir.endsWith("/")){
				rootDir = rootDir.substring(0, rootDir.length()-1);
			}
		}
		
		IPath[] srcs = null;
		if (initJDT(project)) {
			ArrayList<IPath> javaSrcs = new ArrayList<IPath>();
			IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
					javaSrcs.add(roots[i].getPath().removeFirstSegments(1));
				}
			}
			srcs = javaSrcs.toArray(new IPath[javaSrcs.size()]);
		}
		StringBuilder folder = new StringBuilder(128);
		if (rootDir != null) {
			folder.append(rootDir).append('/');
		}
		folder.append(CLASS_OUTPUT_DIR);
		int folderLen = folder.length();
		
		if (monitor.isCanceled()) {
			return;
		}
		monitor.beginTask("Create zip...", toPackFiles.size());
		ZipUtil zipFile = ZipUtil.newZipFile(fileAbsPath, true);
		try {
			for (Iterator iterator = toPackFiles.iterator(); !monitor.isCanceled() && iterator.hasNext();) {
				IFile file = (IFile) iterator.next();
				if (srcs != null) {
					IJavaElement ele = JavaCore.create(file);
					if (ele instanceof ICompilationUnit) {
						IPath packagePath = null;
						IPath relPath = file.getProjectRelativePath();
						for (int i = 0; i < srcs.length; i++) {
							if (srcs[i].isPrefixOf(relPath)) {
								packagePath = relPath.removeFirstSegments(srcs[i].segmentCount()).removeLastSegments(1);
							}
						}
						IType[] czs = ((ICompilationUnit)ele).getAllTypes();
						for (int i = 0; i < czs.length; i++) {
							String qname = czs[i].getTypeQualifiedName(); 
							IPath cp = packagePath.append(qname+".class");
							IFile fclazz = project.getFile(outputDir.append(cp));
							if (fclazz.isAccessible()) {
								folder.setLength(folderLen);
								zipFile.addStream(folder.append(cp.toString()).toString(), fclazz.getContents());
								int anonymousCnt = 1;
								IFile anonymousCZ = null;
								do {//inner class...
									cp = packagePath.append(qname+"$"+anonymousCnt+".class");
									anonymousCnt++;
									anonymousCZ = project.getFile(outputDir.append(cp));
									if (anonymousCZ.isAccessible()) {
										folder.setLength(folderLen);
										zipFile.addStream(folder.append(cp.toString()).toString(), anonymousCZ.getContents());
									} else {
										anonymousCZ = null;
									}
								} while (anonymousCZ != null);
							}
						}
						file = null;
					}
				}
				if (file != null){
					IVirtualResource[] vrs = ComponentCore.createResources(file);
					for (int i = 0; i < vrs.length; i++) {
						String path = vrs[i].getRuntimePath().toString();
						if (rootDir != null) {
							path = rootDir + path;
						} else {
							path = path.substring(1);
						}
						InputStream is = file.getContents();
						try {
							zipFile.addStream(path, is);
						} finally {
							try {
								is.close();
							} catch (Exception e) {
							}
						}
					}
				}
				monitor.worked(1);
			}
		} finally {
			zipFile.close();
		}
		monitor.done();
	}
	
	public boolean initJDT(IProject project) {
		try {
			boolean isJavaProject = project.hasNature("org.eclipse.jdt.core.javanature");
			if (isJavaProject) {
				javaProject = JavaCore.create(project);
				outputDir = javaProject.getOutputLocation().removeFirstSegments(1);//remove "project"...
			}
			return isJavaProject;
		} catch (Throwable e) {
		}
		return false;
	}

}
