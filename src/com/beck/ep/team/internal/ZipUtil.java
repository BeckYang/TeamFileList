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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class ZipUtil {
	private File file;
	private ZipArchiveOutputStream zout;
	private ZipFile zipFile;
	private String folderPath;
	
	private ZipUtil(String fileAbsPath) throws Exception {
		file = new File(fileAbsPath);
		zipFile = new ZipFile(fileAbsPath);
	}
	private ZipUtil(String fileAbsPath, boolean isStreamMode) throws Exception {
		file = new File(fileAbsPath);
		zout = new ZipArchiveOutputStream(file);
	}
	
	/** create instance of Zip */
	public static ZipUtil newZipFile(String fileAbsPath, boolean isStreamMode) throws Exception {
		return new ZipUtil(fileAbsPath, isStreamMode);
	}
	
	public void addStream(String fileNameInZip, InputStream in) throws Exception {
		addStream(fileNameInZip, in, 0);
	}
	public void addStream(String fileNameInZip, InputStream in, long fileAttr) throws Exception {
		try {
			ZipArchiveEntry e = new ZipArchiveEntry(fileNameInZip);
			e.setExternalAttributes(fileAttr);
			zout.putArchiveEntry(e);
			IOUtils.copy(in, zout);
			zout.closeArchiveEntry();
		} finally {
			try {
				in.close();
			} catch (Throwable e) {
			}
		}
	}

	public void setDefaultFolderPath(String defaultFolderPath) {
		if (defaultFolderPath != null) {
			if (File.separatorChar != '/') {
				defaultFolderPath = defaultFolderPath.replace(File.separatorChar, '/');
			}
		}
		folderPath = defaultFolderPath;
	}

	public void addFiles(ArrayList<File> fs, String rootFolderInZip) throws Exception {
		int cutPath = -1;
		int folderLen = -1;
		boolean notUnix = File.separatorChar != '/';
		if (folderPath == null) {
			if (notUnix) {
				String path = fs.get(0).getAbsolutePath();
				int pos = path.indexOf(':');
				if (pos != -1) {
					cutPath = pos+1;//remove windows driver letter
				}
			}
		} else if (folderPath.length() > 0) {
			folderLen = folderPath.length() + 1;
		}
		for (Iterator iterator = fs.iterator(); iterator.hasNext();) {
			File f = (File) iterator.next();
			String path = f.getAbsolutePath();
			if (notUnix) {
				path = path.replace(File.separatorChar, '/');
			}
			if (folderLen > 0 && path.startsWith(folderPath)) {
				path = path.substring(folderLen, path.length());
			} else if (cutPath > 0) {
				path = path.substring(cutPath, path.length());
			}
			if (rootFolderInZip != null) {
				path = rootFolderInZip + path;
			}
			FileInputStream fin = new FileInputStream(f);
			addStream(path, fin, getExternalAttribute(f));
		}
	}
	
	public static int ATTR_DIR = 0x10;
	public static int ATTR_HIDDEN = 0x02;
	public static int ATTR_READ_ONLY = 0x01;
	//public static int ATTR_ARCHIVE = 0x20;
	
	public static long getExternalAttribute(File f) {
		long attr = 0;
		//following implement is for Win32 only...
		if (f.isHidden()) {
			attr = attr | ATTR_HIDDEN;
		}
		if (!f.canWrite()) {
			attr = attr | ATTR_READ_ONLY;
		}
		//use Java7 java.nio.file.?
		return attr;
	}
	
	/**
	 * 
	 * @param folder
	 * @param parentFolderInZip - 
	 * @throws Exception
	 */
	public void addFolder(File folder, String parentFolderInZip) throws Exception {
		StringBuilder sb = new StringBuilder(parentFolderInZip.length()+64).append(parentFolderInZip);
		if (!parentFolderInZip.endsWith("/")) {
			sb.append('/');
		}
		sb.append(folder.getName()).append('/');
		int baseLen = sb.length();
		long attr = getExternalAttribute(folder);
		if (attr != 0) {
			attr = attr | ATTR_DIR;
			ZipArchiveEntry ze = new ZipArchiveEntry(sb.toString());
			ze.setExternalAttributes(attr);
			zout.putArchiveEntry(ze);
			zout.closeArchiveEntry();
		}
		
		File[] fa = folder.listFiles();
		for (int i = 0; i < fa.length; i++) {
			File f = fa[i];
			if (f.isDirectory()) {
				addFolder(f, sb.toString());
			} else {
				FileInputStream fin = new FileInputStream(f);
				addStream(sb.append(f.getName()).toString(), fin, getExternalAttribute(f));
				sb.setLength(baseLen);
			}
		}
	}

	/* for unzip...*/
	//zip4j.core.ZipFile.extractAll(destPath);
	public static void extractAll(String zipFile, String extractDir) throws Exception {
		ZipFile unzipFile = new ZipFile(zipFile);
		try {
			File root = new File(extractDir);
			Enumeration<ZipArchiveEntry> fileHeaderList = unzipFile.getEntries();
			while(fileHeaderList.hasMoreElements()) {
				ZipArchiveEntry fileHeader = fileHeaderList.nextElement();
				if (fileHeader.isDirectory()) {
					//...
				} else if (!fileHeader.isUnixSymlink()) {
					File f = new File(root, fileHeader.getName());
					File dir = f.getParentFile();
					if (!dir.exists()) {
						dir.mkdirs();
					}
					FileOutputStream fout = new FileOutputStream(f);
					try {
						IOUtils.copy(unzipFile.getInputStream(fileHeader), fout);
					} finally {
						try {
							fout.close();
						} catch (Throwable e) {
						}
					}
					f.setLastModified(fileHeader.getLastModifiedDate().getTime());
				}
			}
		} finally {
			ZipFile.closeQuietly(unzipFile);
		}
	}
	
	/** create instance for unZip */
	public static ZipUtil newUnzip(String zipAbsPath) throws Exception {
		ZipUtil zipUtil = new ZipUtil(zipAbsPath);
		return zipUtil;
	}

	public boolean isEncrypted() throws Exception {
		return false;//not support
	}

	public void setPassword(String pwd) throws Exception {
		//not support
	}
	
	public File getFile() {
		return file;
	}
	public void extract(IProgressMonitor monitor, IProject project) throws Exception {
		HashSet<IContainer> newdir = new HashSet<IContainer>();
		ArrayList<ZipArchiveEntry> fileHeaderList = new ArrayList<ZipArchiveEntry>();
		for (Enumeration<ZipArchiveEntry> ez = zipFile.getEntries(); ez.hasMoreElements();) {
			fileHeaderList.add(ez.nextElement());
		}
		monitor.beginTask("unzip file", fileHeaderList.size());
		Iterator<ZipArchiveEntry> iter = fileHeaderList.iterator();
		while(!monitor.isCanceled() && iter.hasNext()) {
			ZipArchiveEntry fileHeader = iter.next();
			if (fileHeader.isDirectory()) {
				//need to handle?
			} else if (!fileHeader.isUnixSymlink()){
				IFile file = project.getFile(fileHeader.getName());
				boolean refreshFile = file.exists();
				
				InputStream is = zipFile.getInputStream(fileHeader);
				try {
					if (refreshFile) {
						file.setContents(is, true, false, monitor);
						file.refreshLocal(IFile.DEPTH_ZERO, monitor);
						newdir.add(file.getParent());
					} else {
						if (!file.getParent().exists()) {
							IPath path = file.getProjectRelativePath().removeLastSegments(1);
							int len = path.segmentCount() - 1;
							for (int i = len; i >= 0; i--) {
								IFolder fd = project.getFolder(path.removeLastSegments(i));
								if (!fd.exists()) {
									fd.create(true, true, monitor);
								}
							}
						}
						file.create(is, true, monitor);
					}
				} finally {
					//is.close();//file.setContents(...) already do it
				}
			}
			monitor.worked(1);
		}
		//refresh dir...
		for (Iterator iterator = newdir.iterator(); iterator.hasNext();) {
			IContainer container = (IContainer) iterator.next();
			container.refreshLocal(IFile.DEPTH_ONE, monitor);
		}
	}
	public void close() {
		ZipFile.closeQuietly(zipFile);
		if (zout != null) {
			try {
				zout.close();
			} catch (Throwable e) {
			}
		}
	}
	
}
