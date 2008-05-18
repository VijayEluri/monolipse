package monolipse.ui.editors;

import java.util.HashMap;
import java.util.Map;

import monolipse.core.compiler.OutlineNode;
import monolipse.ui.BooUI;
import monolipse.ui.IBooUIConstants;

import org.eclipse.jdt.internal.ui.text.AbstractInformationControl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


public class BooContentOutlinePage extends ContentOutlinePage {
	
	public class BooOutlineInformationControl extends AbstractInformationControl {

		public BooOutlineInformationControl(Shell parent, int shellStyle, int treeStyle) {
			super(parent, shellStyle, treeStyle);
			
		}

		protected TreeViewer createTreeViewer(Composite parent, int style) {
			TreeViewer tree = new TreeViewer(parent, style);
			tree.setAutoExpandLevel(4);
			tree.setContentProvider(new OutlineContentProvider());
			tree.setLabelProvider(new OutlineLabelProvider());
//			tree.addSelectionChangedListener(new ISelectionChangedListener() {
//				public void selectionChanged(SelectionChangedEvent event) {
//					Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
//					if (null == selected) return;
//					goToNode(((OutlineNode)selected));
//				}
//			});
			return tree;
		}
		
		protected Object getSelectedElement() {
			OutlineNode node = (OutlineNode)super.getSelectedElement();
			if (null != node) {
				goToNode(node);
			}
			return node;
		}
		
		protected String getId() {
			return getClass().getName();
		}

		public void setInput(Object information) {
			getTreeViewer().setInput(information);
		}
		
		protected void selectFirstMatch() {
			Tree tree= getTreeViewer().getTree();
			Object element= findElement(tree.getItems());
			if (element != null)
				getTreeViewer().setSelection(new StructuredSelection(element), true);
			else
				getTreeViewer().setSelection(StructuredSelection.EMPTY);
		}

		private Object findElement(TreeItem[] items) {
			ILabelProvider labelProvider= (ILabelProvider)getTreeViewer().getLabelProvider();
			for (int i= 0; i < items.length; i++) {
				Object element= items[i].getData();
				if (element != null) {
					String label= labelProvider.getText(element);
					if (fStringMatcher.match(label)) {
						return element;
					}
				}

				element = findElement(items[i].getItems());
				if (element != null) return element;
			}
			return null;
		}
	}

	
	public static class OutlineLabelProvider extends LabelProvider {
		
		private final Map _imageMap = new HashMap();
		
		public OutlineLabelProvider() {
			setUpImageMap();
		}
		
		public String getText(Object element) {
			return ((OutlineNode)element).name();
		}
		
		public Image getImage(Object element) {
			return (Image) _imageMap.get(((OutlineNode)element).type());
		}
		
		void setUpImageMap() {
			mapImage(OutlineNode.CLASS, IBooUIConstants.CLASS);
			mapImage(OutlineNode.METHOD, IBooUIConstants.METHOD);
			mapImage(OutlineNode.CONSTRUCTOR, IBooUIConstants.METHOD);
			mapImage(OutlineNode.FIELD, IBooUIConstants.FIELD);
			mapImage(OutlineNode.PROPERTY, IBooUIConstants.PROPERTY);
			mapImage(OutlineNode.EVENT, IBooUIConstants.EVENT);
			mapImage(OutlineNode.INTERFACE, IBooUIConstants.INTERFACE);
			mapImage(OutlineNode.CALLABLE, IBooUIConstants.CALLABLE);
			mapImage(OutlineNode.STRUCT, IBooUIConstants.STRUCT);
			mapImage(OutlineNode.ENUM, IBooUIConstants.ENUM);
		}
		
		void mapImage(String entityType, String key) {
			_imageMap.put(entityType, BooUI.getImage(key));
		}
	}

	public static class OutlineContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			return ((OutlineNode)parentElement).children();
		}

		public Object getParent(Object element) {
			return ((OutlineNode)element).parent();
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}		
	}

	private IDocumentProvider _documentProvider;
	private IEditorInput _editorInput;
	private BooEditor _editor;

	public BooContentOutlinePage(IDocumentProvider documentProvider, BooEditor editor) {
		_documentProvider = documentProvider;
		_editor = editor;
	}

	public void setInput(IEditorInput editorInput) {
		_editorInput = editorInput;
	}
	
	private void gotoLine(int line) {
		try {
			BooDocument document = getDocument();
			IRegion info = document.getLineInformation(line);
			_editor.selectAndReveal(info.getOffset(), info.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		setUpOutline();
		
		setUpTreeViewer();
		
		toolBarManager().add(createSortAction());
		
	}

	private void setUpTreeViewer() {
		final TreeViewer tree = getTreeViewer();
		setUpTreeViewer(tree);
	}

	void setUpTreeViewer(final TreeViewer tree) {
		tree.setAutoExpandLevel(4);
		tree.setContentProvider(new OutlineContentProvider());
		tree.setLabelProvider(new OutlineLabelProvider());
		tree.setInput(outline());
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (null == selected) return;
				int line = ((OutlineNode)selected).line()-1;
				gotoLine(line);
			}
		});
	}

	public OutlineNode outline() {
		return getDocument().getOutline();
	}

	private IToolBarManager toolBarManager() {
		return getSite().getActionBars().getToolBarManager();
	}

	private Action createSortAction() {
		Action sortAction = new Action("Sort", Action.AS_CHECK_BOX) {
			public void run() {
				getTreeViewer().setComparator(isChecked() ? new ViewerComparator() : null);
			}
		};
		sortAction.setToolTipText("sorts by name");
		sortAction.setImageDescriptor(BooUI.sharedImage(ISharedImages.IMG_DEF_VIEW));
		return sortAction;
	}

	void setUpOutline() {
		final BooDocument document = getDocument();
		document.addOutlineListener(new BooDocument.OutlineListener() {
			public void outlineChanged(OutlineNode node) {
				final TreeViewer tree = getTreeViewer();
				tree.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						tree.setInput(document.getOutline());
					};
				});
			}
		});
	}

	private BooDocument getDocument() {
		return (BooDocument) _documentProvider.getDocument(_editorInput);
	}

	public IInformationControl createQuickOutline(Shell parent, int shellStyle,
			int treeStyle) {
		return new BooOutlineInformationControl(parent, shellStyle, treeStyle);
	}

	private void goToNode(final OutlineNode node) {
		int line = node.line()-1;
		gotoLine(line);
	}
}
