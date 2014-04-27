/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - SourceRangeHyperlink
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor.hyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tools.EclipseTools;

/**
 * IHyperlink implementation that opens a file in an editor and possibly
 * reveals a source range on click.
 *
 * @author Marcel Lutz
 */
public class SourceRangeHyperlink implements IHyperlink {

	private static final IEclipseLog log=
		EclipseLogFactory.create(SourceRangeHyperlink.class);

	private IRegion hyperlinkRegion;

	private String hyperlinkText;

	private IFile targetFile;

	private ISourceRange targetSourceRange;

	private String editorId;

	/**
	 * an additional offset to shift the hyperlinkRegion by
	 */
	private int partitionOffset= 0;

	/**
	 * Constructor
	 *
	 * @param hyperlinkRegion the hyperlink's region
	 * @param hyperlinkText the hyperlink's label
	 * @param targetFile the file to open on hyperlink click
	 * @param targetSourceRange the range to highlight after opening the file;
	 * 		null for no text highlighting
	 * @param editorId the id of the editor to open the file in
	 */
	public SourceRangeHyperlink(IRegion hyperlinkRegion, String hyperlinkText,
			IFile targetFile, ISourceRange targetSourceRange, String editorId) {
		super();
		this.hyperlinkRegion=	hyperlinkRegion;
		this.hyperlinkText=		hyperlinkText;
		this.targetFile=		targetFile;
		this.targetSourceRange=	targetSourceRange;
		this.editorId= 			editorId;
	}

	@Override
	public void open() {
		IWorkbench wb= PlatformUI.getWorkbench();
		IWorkbenchWindow win= wb.getActiveWorkbenchWindow();
		IWorkbenchPage page= win.getActivePage();

		IEditorPart editorPart;
		if (editorId != null) {
			editorPart= EclipseTools.openFileInEditor(
					targetFile, page, editorId);
		} else {
			editorPart= EclipseTools.openFileInEditor(
					targetFile, page);
		}

		if (targetSourceRange != null) {
			if (editorPart instanceof ITextEditor) {
				((ITextEditor) editorPart).selectAndReveal(
						targetSourceRange.getOffset(),
						targetSourceRange.getLength());
			} else {
				log.info("editorPart !instanceof ITextEditor");
			}
		}
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return hyperlinkText;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		if (partitionOffset == 0) {
			return hyperlinkRegion;
		} else {
			return new Region(hyperlinkRegion.getOffset() + partitionOffset,
					hyperlinkRegion.getLength());
		}
	}

	// getters and setters

	public int getPartitionOffset() {
		return partitionOffset;
	}

	public void setPartitionOffset(int partitionOffset) {
		this.partitionOffset= partitionOffset;
	}
}
