/* === JSONEditor.java === */
package de.drremote.jsonplugin.editor;

import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
// WICHTIG: diesen Import NICHT mehr verwenden:
// import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class JSONEditor extends TextEditor {

    private static final String ACTION_FORMAT_JSON = "de.drremote.jsonplugin.actions.formatJson";
    private static final String ACTION_MINIFY_JSON = "de.drremote.jsonplugin.actions.minifyJson";

    private JSONOutlinePage outlinePage;
    private final ColorManager colorManager = new ColorManager();

    public JSONEditor() {
        // new SourceViewerConfiguration with syntax highlighting
        setSourceViewerConfiguration(new JSONSourceViewerConfiguration(colorManager));
        setDocumentProvider(new JSONDocumentProvider());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            if (outlinePage == null) {
                outlinePage = new JSONOutlinePage(this);
                ISelectionProvider selectionProvider = getSelectionProvider();
                if (selectionProvider != null) {
                    selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {

                        @Override
                        public void selectionChanged(SelectionChangedEvent event) {
                            if (outlinePage != null) {
                                outlinePage.updateSelectionFromEditor(event.getSelection());
                            }
                        }
                    });
                }
                outlinePage.setInput(getDocument());
            }
            return outlinePage;
        }
        return super.getAdapter(required);
    }

    public IDocument getDocument() {
        IEditorInput input = getEditorInput();
        return getDocumentProvider().getDocument(input);
    }

    @Override
    protected void doSetInput(IEditorInput input) throws org.eclipse.core.runtime.CoreException {
        super.doSetInput(input);
        if (outlinePage != null) {
            outlinePage.setInput(getDocument());
        }
    }

    @Override
    public void doSave(org.eclipse.core.runtime.IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);

        // validate JSON and set error markers
        validateJson();

        if (outlinePage != null) {
            outlinePage.refresh();
        }
    }

    private void validateJson() {
        if (getEditorInput() instanceof IFileEditorInput fileInput) {
            IFile file = fileInput.getFile();
            JSONValidationUtil.validateJson(file, getDocument());
        }
    }

    @Override
    protected void createActions() {
        super.createActions();

        // 1) Pretty-print
        Action formatJsonAction = new Action("Format JSON") {
            @Override
            public void run() {
                try {
                    JSONFormatUtil.formatDocument(getDocument());

                    if (outlinePage != null) {
                        outlinePage.refresh();
                    }

                    validateJson();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Ctrl+Shift+F Command verknüpfen – direkte Command-ID
        formatJsonAction.setActionDefinitionId("de.drremote.jsonplugin.commands.formatJson");
        
        
        // Action registrieren
        setAction("Format", formatJsonAction);
        setAction(ACTION_FORMAT_JSON, formatJsonAction);
        markAsStateDependentAction(ACTION_FORMAT_JSON, false);

        // 2) Minify
        Action minifyJsonAction = new Action("Minify JSON") {
            @Override
            public void run() {
                try {
                    JSONFormatUtil.minifyDocument(getDocument());

                    if (outlinePage != null) {
                        outlinePage.refresh();
                    }

                    validateJson();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        setAction(ACTION_MINIFY_JSON, minifyJsonAction);
        markAsStateDependentAction(ACTION_MINIFY_JSON, false);
    }

    @Override
    protected void editorContextMenuAboutToShow(IMenuManager menu) {
        super.editorContextMenuAboutToShow(menu);

        // add both actions to the Edit menu group
        if (getAction(ACTION_FORMAT_JSON) != null) {
            menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ACTION_FORMAT_JSON));
        }
        if (getAction(ACTION_MINIFY_JSON) != null) {
            menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ACTION_MINIFY_JSON));
        }
    }

    @Override
    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }
}
