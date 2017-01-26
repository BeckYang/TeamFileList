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

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


public class DefineExtractDialog extends StatusDialog {
	private String name;
	private String zipFile;
	private String extractDir;
	
	//UI 
	private Text edName;
	private Text edZipFile;
	private Text edExtractDir;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public DefineExtractDialog(Shell parent, int style) {
		super(parent);
		setShellStyle(getShellStyle() | style);
		name = "";
		setTitle("Define Unpack setting");
		validate();
		setHelpAvailable(false);
	}
	
	public void initDefault(String name, String zipFile, String extractDir) {
		this.name = name;
		this.zipFile = zipFile;
		this.extractDir = extractDir;
	}

	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		//line1
		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Setting name");
		edName = new Text(container, SWT.BORDER);
		edName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(container, SWT.NONE);
		
		//line2
		Label lblNewLabel_1 = new Label(container, SWT.NONE);
		lblNewLabel_1.setAlignment(SWT.RIGHT);
		lblNewLabel_1.setText("Zip file");
		edZipFile = new Text(container, SWT.BORDER);
		edZipFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		final Button btnSelectZip = new Button(container, SWT.NONE);
		btnSelectZip.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE));
		
		//line3
		Label lblNewLabel_2 = new Label(container, SWT.NONE);
		lblNewLabel_2.setText("Extract to");
		edExtractDir = new Text(container, SWT.BORDER);
		edExtractDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		final Button btnSelectFolder = new Button(container, SWT.NONE);
		btnSelectFolder.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
		
		if (name != null) {
			edName.setText(name);
		}
		if (zipFile != null) {
			edZipFile.setText(zipFile);
		}
		if (extractDir != null) {
			edExtractDir.setText(extractDir);
		}
		
		Listener handler = new Listener() {
			public void handleEvent(Event event) {
				Object w = event.widget;
				if (w == btnSelectFolder) {
					selectDir();
				} else if (w == btnSelectZip){
					selectZip();
				} else {
					validate();
				}
			}
		};
		btnSelectFolder.addListener(SWT.Selection, handler);
		btnSelectZip.addListener(SWT.Selection, handler);
		edName.addListener(SWT.Modify, handler);
		edExtractDir.addListener(SWT.Modify, handler);
		edZipFile.addListener(SWT.Modify, handler);
		return container;
	}
	
	private void validate() {
		if (edName != null) {
			name = edName.getText();
			extractDir = edExtractDir.getText();
			zipFile = edZipFile.getText();
		}
		
		String err = null;
		if (name.length() == 0) {
			err = "Action name is required";
		} else if (extractDir.length() == 0){
			err = "Extract to which folder is required ";
		} else if (zipFile.length() == 0) {
			err = "Zip file is required";
		}
		if (err == null) {
			updateStatus(Status.OK_STATUS);
		} else {
			updateStatus(new Status(Status.ERROR, getClass().getName(),1, err, null));
		}
	}
	
	private void selectZip() {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setFilterExtensions(new String[]{"*.zip"});
		String s = dialog.open();
		if (s != null) {
			edZipFile.setText(s);
			validate();
		}
	}
	
	private void selectDir() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		String s = dialog.open();
		if (s != null) {
			edExtractDir.setText(s);
			validate();
		}
	}
	
	public String getName() {
		return name;
	}
	public String getZipFile() {
		return zipFile;
	}
	public String getExtractDir() {
		return extractDir;
	}
	
	protected void okPressed() {
		super.okPressed();
	}
}
