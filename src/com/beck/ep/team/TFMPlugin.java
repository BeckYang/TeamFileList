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
package com.beck.ep.team;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import org.eclipse.swt.widgets.Shell;

/**
 * The activator class controls the plug-in life cycle
 */
public class TFMPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.beck.ep.team"; //$NON-NLS-1$
	
	public static final String PACKER_ZIP = "zip"; 
	public static final String PACKER_WAR_PATCH = "warPatch"; 

	// The shared instance
	private static TFMPlugin plugin;
	
	/**
	 * The constructor
	 */
	public TFMPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TFMPlugin getDefault() {
		return plugin;
	}
	
	private IEclipsePreferences fEPreference = null;
	public IEclipsePreferences getPreferences() {
		if (fEPreference == null) {
			fEPreference = new InstanceScope().getNode(getBundle().getSymbolicName());
		}
		return fEPreference;
	}
	
	public IFilePacker newFilePacker(String packerType) {
		IFilePacker p = null;
		Class cz = null;
		if (PACKER_ZIP.equals(packerType)) {
			cz = com.beck.ep.team.internal.ZipCreator.class;
		} else if (PACKER_WAR_PATCH.equals(packerType)){
			cz = com.beck.ep.team.internal.WarPatchCreator.class;
		}
		try {
			p = (IFilePacker)(cz.newInstance());
		} catch (Throwable e) {
			info("newFilePacker error", e);
		}
		return p;
	}

	public static void showMessage(Shell shell, String message) {
		MessageDialog.openInformation( shell, "Team File List", message);
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	public static void error(String message, Throwable exception) {
		getDefault()._log(IStatus.ERROR, message, exception);
	}
	
	public static void info(String message, Throwable exception) {
		getDefault()._log(IStatus.INFO, message, exception);
	}
	
	public void _log(int severity, String message, Throwable exception) {
		message = (message != null) ? message : "null"; //$NON-NLS-1$
		Status statusObj = new Status(severity, PLUGIN_ID, severity, message, exception);
		Platform.getLog(getBundle()).log(statusObj);
	}
}
