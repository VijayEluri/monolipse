/*
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
package monolipse.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;

import monolipse.ui.BooUI;
import monolipse.ui.editors.actions.ExpandCodeAction;
import monolipse.ui.editors.actions.ExpandMacrosAction;
import monolipse.ui.editors.actions.GotoDefinitionAction;
import monolipse.ui.editors.actions.ToggleCommentAction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;


public class BooEditor extends TextEditor {
	
	private final class BooOutlineInformationProvider implements
			IInformationProvider, IInformationProviderExtension {
		public String getInformation(ITextViewer textViewer, IRegion subject) {
			return null;
		}

		public IRegion getSubject(ITextViewer textViewer, int offset) {
			return new Region(offset, 1);
		}

		public Object getInformation2(ITextViewer textViewer, IRegion subject) {
			return outlinePage().outline();
		}
	}

	public static final String ID_EDITOR = "monolipse.ui.editors.BooEditor";

	private BooContentOutlinePage _outlinePage;
	
	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new BooSourceViewerConfiguration(getSharedColors(), this, getPreferenceStore()));
		setDocumentProvider(new BooDocumentProvider());
		setKeyBindingScopes(new String[] { "monolipse.ui.booEditorScope", "org.eclipse.ui.textEditorScope" });
	}

    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
        
        projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
		projectionSupport.install();
		
		//turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);
		
		annotationModel = viewer.getProjectionAnnotationModel();	
    }

    private ProjectionSupport projectionSupport;
    
	private Annotation[] oldAnnotations;
	private ProjectionAnnotationModel annotationModel;
	
	public void updateFoldingStructure(ArrayList<Position> positions)
	{
		Annotation[] annotations = new Annotation[positions.size()];
		
		//this will hold the new annotations along
		//with their corresponding positions
		HashMap<ProjectionAnnotation, Position> newAnnotations = new HashMap<ProjectionAnnotation, Position>();
		
		for(int i = 0;i < positions.size(); i++)
		{
			ProjectionAnnotation annotation = new ProjectionAnnotation();
			newAnnotations.put(annotation,positions.get(i));
			
			annotations[i] = annotation;
		}
		
		annotationModel.replaceAnnotations(oldAnnotations, newAnnotations);
		
		oldAnnotations = annotations;
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles)
    {
        ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

    	// ensure decoration support has been created and configured.
    	getSourceViewerDecorationSupport(viewer);
    	
    	return viewer;
    }

	protected void createActions() {
		super.createActions();

		ToggleCommentAction action = new ToggleCommentAction(BooUI.getResourceBundle(), "Toggle Comment", this);
		//action.setAccelerator(Action.convertAccelerator("M1+/"));
		setAction(ToggleCommentAction.ID, action);
		//setActionActivationCode(ToggleCommentAction.ID, '/', -1, SWT.MOD1);
		//markAsStateDependentAction(ToggleCommentAction.ID, true);
		action.configure(getSourceViewer(), getSourceViewerConfiguration());
		
		Action eca = new ExpandCodeAction(this);
		setAction(eca.getId(), eca);
		
		Action ema = new ExpandMacrosAction(this);
		setAction(ema.getId(), ema);
		
		Action gda = new GotoDefinitionAction(this);
		setAction(gda.getId(), gda);

		final InformationPresenter quickOutline = new InformationPresenter(quickOutlineCreator()) {
			public IInformationProvider getInformationProvider(String contentType) {
				return new BooOutlineInformationProvider();
			}
		};
		quickOutline.install(getSourceViewer());
		final Action outlineAction = new Action("QuickOutline") {
			public void run() {
				BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
					public void run() {
						quickOutline.showInformation();
					}
				});
				
			}
		};
		outlineAction.setImageDescriptor(BooUI.sharedImage(ISharedImages.IMG_DEF_VIEW));
		outlineAction.setId("monolipse.ui.editors.actions.BooQuickOutlineAction");
		outlineAction.setActionDefinitionId(outlineAction.getId());
		setAction(outlineAction.getId(), outlineAction);
	}

	private IInformationControlCreator quickOutlineCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
				return outlinePage().createQuickOutline(parent, shellStyle, treeStyle);
			}
		};
	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
//		menu.appendToGroup(ITextEditorActionConstants.MB_ADDITIONS, getAction(ToggleCommentAction.ID));
		menu.appendToGroup(ITextEditorActionConstants.MB_ADDITIONS, getAction(ExpandCodeAction.ID));
		menu.appendToGroup(ITextEditorActionConstants.MB_ADDITIONS, getAction(GotoDefinitionAction.ID));
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			return outlinePage();
		}
		return super.getAdapter(required);
	}

	public BooContentOutlinePage outlinePage() {
		if (_outlinePage == null) {
			_outlinePage = new BooContentOutlinePage(getDocumentProvider(), this);
			_outlinePage.setInput(getEditorInput());
		}
		return _outlinePage;
	}

	public BooDocument getDocument() {
		return (BooDocument) getDocumentProvider().getDocument(getEditorInput());
	}

	public Point getSelectedRange() {
		return getSourceViewer().getSelectedRange();
	}

	public Point getSelectedPosition() {
		Point range = getSelectedRange();
		
		try {
			int line = getSourceViewer().getDocument().getLineOfOffset(range.x);
			int column = calculateColumn(getSourceViewer(), range.x, line);
			return new Point(line, column);
		} catch (BadLocationException e) {
			BooUI.logException(e);
		}
		return null;
	}
	
	private int calculateColumn(ITextViewer textViewer, int offset, int line)
			throws BadLocationException {
		int startOffset = textViewer.getDocument().getLineOffset(line);
		int column = offset - startOffset;
		String text = textViewer.getDocument().get(startOffset, column);
		int textSize = 0;
		int tabSize = 4; // FIX: get real document tab size
		
		for(char ch: text.toCharArray()) {
			if (ch == '\t') {
				textSize += tabSize;
			}
			else textSize++;
		}
		return textSize;
	}
}
