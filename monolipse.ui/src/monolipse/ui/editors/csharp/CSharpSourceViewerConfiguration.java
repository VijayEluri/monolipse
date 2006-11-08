﻿/*
 * Boo Development Tools for the Eclipse IDE
 * Copyright (C) 2005 Rodrigo B. de Oliveira (rbo@acm.org)
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */
package monolipse.ui.editors.csharp;

import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import monolipse.ui.editors.BooColorConstants;
import monolipse.ui.editors.BooDoubleClickStrategy;
import monolipse.ui.editors.MarkerAnnotationHover;
import monolipse.ui.editors.MultiLineCommentScanner;
import monolipse.ui.editors.SingleQuotedStringScanner;
import monolipse.ui.editors.StringScanner;

public class CSharpSourceViewerConfiguration extends SourceViewerConfiguration {
	private BooDoubleClickStrategy _doubleClickStrategy;
	private CSharpScanner _scanner;
	private ISharedTextColors _colorManager;
	private MultiLineCommentScanner _multiLineCommentScanner;
	private StringScanner _dqsScanner;
	private SingleQuotedStringScanner _sqsScanner;

	public CSharpSourceViewerConfiguration(ISharedTextColors colors) {
		this._colorManager = colors;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return CSharpPartitionScanner.PARTITION_TYPES;
	}
	
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (_doubleClickStrategy == null) {
			_doubleClickStrategy = new BooDoubleClickStrategy();
		}
		return _doubleClickStrategy;
	}

	protected CSharpScanner getScanner() {
		if (_scanner == null) {
			_scanner = new CSharpScanner(_colorManager);
		}
		return _scanner;
	}
	
	protected MultiLineCommentScanner getMultiLineCommentScanner() {
		if (_multiLineCommentScanner == null) {
			_multiLineCommentScanner = new MultiLineCommentScanner(_colorManager);
		}
		return _multiLineCommentScanner;
	}
	
	protected StringScanner getDoubleQuotedStringScanner() {
		if (_dqsScanner == null) {
			_dqsScanner = new StringScanner(_colorManager.getColor(BooColorConstants.STRING));
		}
		return _dqsScanner;
	}
	
	protected SingleQuotedStringScanner getSingleQuotedStringScanner() {
		if (_sqsScanner == null) {
			_sqsScanner = new SingleQuotedStringScanner(_colorManager.getColor(BooColorConstants.STRING));
		}
		return _sqsScanner;
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new MarkerAnnotationHover();
	}
	
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		if (IDocument.DEFAULT_CONTENT_TYPE == contentType
			|| CSharpPartitionScanner.SINGLELINE_COMMENT_TYPE == contentType) {
			return new String[] { "//" };
		}
		return null;
	}
	
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
			}
		};
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		configureReconciler(reconciler, IDocument.DEFAULT_CONTENT_TYPE, getScanner());
		configureReconciler(reconciler, CSharpPartitionScanner.MULTILINE_COMMENT_TYPE, getMultiLineCommentScanner());		
		configureReconciler(reconciler, CSharpPartitionScanner.SINGLELINE_COMMENT_TYPE, getMultiLineCommentScanner());
		configureReconciler(reconciler, CSharpPartitionScanner.SINGLE_QUOTED_STRING, getSingleQuotedStringScanner());
		configureReconciler(reconciler, CSharpPartitionScanner.DOUBLE_QUOTED_STRING, getDoubleQuotedStringScanner());
		return reconciler;
	}
	private void configureReconciler(PresentationReconciler reconciler, String partitionType, ITokenScanner scanner) {
		DefaultDamagerRepairer dr;
		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, partitionType);
		reconciler.setRepairer(dr, partitionType);
	}

}