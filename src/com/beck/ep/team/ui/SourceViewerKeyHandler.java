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

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

public class SourceViewerKeyHandler implements VerifyKeyListener {
	protected ITextOperationTarget fTarget;
	protected ITextViewer fViewer;
	
	public void verifyKey(VerifyEvent event) {
		if (!fViewer.isEditable()) {
			return;
		}
		if (event.stateMask == SWT.CTRL) {
			switch (event.keyCode) {
			case 'z':
				fTarget.doOperation(ITextOperationTarget.UNDO);
				break;
			case 'y':
				fTarget.doOperation(ITextOperationTarget.REDO);
				break;
			}
		}
	}
	
	public void install(ITextViewer viewer) {
		fTarget = viewer.getTextOperationTarget();
		fViewer = viewer;
		if (viewer instanceof ITextViewerExtension) {
			ITextViewerExtension e= (ITextViewerExtension)viewer;
			e.appendVerifyKeyListener(this);
		} else {
			viewer.getTextWidget().addVerifyKeyListener(this);
		}
	}
	
	public void uninstall(ITextViewer viewer) {
		if (viewer instanceof ITextViewerExtension) {
			ITextViewerExtension e= (ITextViewerExtension)viewer;
			e.removeVerifyKeyListener(this);
		} else {
			viewer.getTextWidget().removeVerifyKeyListener(this);
		}
	}
}
