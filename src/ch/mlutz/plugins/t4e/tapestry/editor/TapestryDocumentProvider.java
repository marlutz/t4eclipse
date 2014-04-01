package ch.mlutz.plugins.t4e.tapestry.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;

import ch.mlutz.plugins.t4e.Activator;

public class TapestryDocumentProvider extends FileDocumentProvider {


	protected IDocument createDocument(Object element) throws CoreException {

		/*

		IDocument document = super.createDocument(element);
		if (document != null)
		{
			IDocumentPartitioner partitioner = new FastPartitioner(
			 new XMLPartitionScanner(), new String[]
			{
					XMLPartitionScanner.XML_START_TAG,
					XMLPartitionScanner.XML_PI,
					XMLPartitionScanner.XML_DOCTYPE,
					XMLPartitionScanner.XML_END_TAG,
					XMLPartitionScanner.XML_TEXT,
					XMLPartitionScanner.XML_CDATA,
					XMLPartitionScanner.XML_COMMENT
			});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;

		/*/
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new TapestryPartitionScanner(),
					new String[] {
						TapestryPartitionScanner.XML_TAG,
						TapestryPartitionScanner.XML_COMMENT });
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);

			if (element instanceof FileEditorInput) {
				Activator.getDefault().getTapestryIndex()
					.addDocumentToFileMapping(document,
						((FileEditorInput) element).getFile());
			}
			/*
			removeDocumentToFileMapping(
				this.getSourceViewer().getDocument());
			*/
		}
		return document;
	}
}