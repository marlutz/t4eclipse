package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.ui.editors.text.TextEditor;

import ch.mlutz.plugins.t4e.Activator;

public class TapestryEditor extends TextEditor {

	private ColorManager colorManager;

	public TapestryEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new TapestryConfiguration(colorManager));
		setDocumentProvider(new TapestryDocumentProvider());
	}
	public void dispose() {
		Activator.getDefault().getTapestryIndex().removeDocumentToFileMapping(
			this.getSourceViewer().getDocument());
		colorManager.dispose();
		super.dispose();
	}

}
