/* === JSONSourceViewerConfiguration.java === */
package de.drremote.jsonplugin.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class JSONSourceViewerConfiguration extends SourceViewerConfiguration {

    private final ColorManager colorManager;

    public JSONSourceViewerConfiguration(ColorManager colorManager) {
        this.colorManager = colorManager;
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(
            org.eclipse.jface.text.source.ISourceViewer sourceViewer) {

        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer dr =
                new DefaultDamagerRepairer(new JSONScanner(colorManager));
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        return reconciler;
    }
}
