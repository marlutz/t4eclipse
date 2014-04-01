/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - content assist processor
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tools.EclipseTools;

public class OgnlHyperlinkDetector extends AbstractHyperlinkDetector {

	private static final IEclipseLog log=
			EclipseLogFactory.create(OgnlHyperlinkDetector.class);

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region,
			boolean canHandleMultipleLinks) {

		IDocument document= viewer.getDocument();
		String contentBefore;
		ITypedRegion partition;
		try {
			partition= document.getPartition(region.getOffset());

			if (!partition.getType().equals(TapestryPartitionScanner.XML_TAG)) {
				return null;
			}

			contentBefore= document.get(partition.getOffset(),
					partition.getLength());
		} catch(BadLocationException e) {
			log.warn("Could not detect hyperlinks: ", e);
			return null;
		}

		List<IHyperlink> result= new ArrayList<IHyperlink>();

		// go back until whitespace is found and
		int charIndex= region.getOffset() - partition.getOffset();
		while (charIndex >= 0
				&& contentBefore.charAt(charIndex) != '"') {
			charIndex--;
		}

		// charIndex might be -1, but that's ok
		String prefix= contentBefore.substring(charIndex + 1);

		final String expectedPrefix= "ognl:";
		if (!prefix.startsWith(expectedPrefix)) {
			return null;
		}

		int ognlExpressionStart= charIndex + 1 + expectedPrefix.length();

		charIndex= region.getOffset() - partition.getOffset();
		while (charIndex < contentBefore.length()
				&& contentBefore.charAt(charIndex) != '"'
				&& contentBefore.charAt(charIndex) != '.') {
			charIndex++;
		}

		final int hyperLinkOffset= partition.getOffset() + ognlExpressionStart;

		final int hyperLinkLength= partition.getOffset() + charIndex
				- hyperLinkOffset;

		if (hyperLinkLength < 0) {
			// caret might rest on the first quote
			log.warn("OGNL hyperLinkLength: " + hyperLinkLength
				+ "; partition.getOffset(): " + partition.getOffset()
				+ "; charIndex: " + charIndex
				+ "; hyperLinkOffset: " + hyperLinkOffset);
			return null;
		}

		final String hyperlinkText= contentBefore.substring(ognlExpressionStart,
			ognlExpressionStart + hyperLinkLength);

		TapestryIndex tapestryIndex= Activator.getDefault().getTapestryIndex();
		IFile documentFile= tapestryIndex.getDocumentToFileMapping(document);

		if (documentFile == null) {
			return null;
		}

		ICompilationUnit javaCompilationUnit= tapestryIndex
			.getRelatedCompilationUnit(documentFile);

		if (javaCompilationUnit == null) {
			return null;
		}

		/*
		TapestryModule module= tapestryIndex.getModuleForResource(documentFile);

		TapestryHtmlElement linkedComponent= null;
		for (TapestryHtmlElement component: module.getComponents()) {
			if (hyperlinkText.equals(component.getPath())) {
				linkedComponent= component;
			}
		}

		final TapestryHtmlElement finalLinkedComponent= linkedComponent;
		*/

		IType[] types= new IType[0];
		try {
			types= javaCompilationUnit.getTypes();
		} catch (JavaModelException e) {
			log.warn("Could not get types of compilation unit "
				+ javaCompilationUnit.getElementName(), e);
		}

		IMethod methodMatch= null;
outer:
		for (IType type: types) {
			try {
				if (type.isClass() && Flags.isPublic(type.getFlags())) {
					IMethod[] methods = type.getMethods();
					for (IMethod method: methods) {
						String ognlMethodName= method.getElementName() + "()";
						if (ognlMethodName.equals(hyperlinkText)) {
							methodMatch= method;
							break outer;
						}

						ognlMethodName= ognlMethodName
								.replaceAll("^get(.+)\\(\\)$", "$1");
						// TODO: check this match!
						if (ognlMethodName.equalsIgnoreCase(hyperlinkText)) {
							methodMatch= method;
							break outer;
						}
					}
				}
			} catch (JavaModelException e) {
				log.warn("Could not get information for type " + type.getElementName(), e);
			}
		}

		IFile javaFile= null;
		try {
			javaFile= (IFile) javaCompilationUnit
			.getCorrespondingResource().getAdapter(IFile.class);
		} catch (JavaModelException e) {
			log.warn("Could not get corresponding resource for " + javaCompilationUnit.getElementName(), e);
		}
		final IFile finalJavaFile= javaFile;

		ISourceRange sourceRange= null;
		if (methodMatch != null) {
			try {
				sourceRange= methodMatch.getSourceRange();
			} catch (JavaModelException e) {
				log.warn("Could not get source range for method " + methodMatch.getElementName(), e);
			}
		}

		final ISourceRange finalSourceRange= sourceRange;

		if (finalJavaFile != null) {
			result.add(new IHyperlink() {

				@Override
				public void open() {
					IWorkbench wb= PlatformUI.getWorkbench();
					IWorkbenchWindow win= wb.getActiveWorkbenchWindow();
					IWorkbenchPage page= win.getActivePage();

					IEditorPart editorPart= EclipseTools.openFileInEditor(
						finalJavaFile, page);

					if (!(editorPart instanceof ITextEditor)) {
						log.info("editorPart !instanceof ITextEditor");
					}

					if (finalSourceRange == null) {
						log.info("finalSourceRange == null");
					}

					if (editorPart instanceof ITextEditor
							&& finalSourceRange != null) {

						log.info("Opening source range: " + finalSourceRange);

						((ITextEditor) editorPart).selectAndReveal(
							finalSourceRange.getOffset(), finalSourceRange.getLength());
					}
				}

				@Override
				public String getTypeLabel() {
					return null;
				}

				@Override
				public String getHyperlinkText() {
					return "Open method";
				}

				@Override
				public IRegion getHyperlinkRegion() {
					return new Region(hyperLinkOffset, hyperLinkLength);
				}
			});
		}
		/*
		log.info("detectHyperlinks called: " + hyperLinkOffset + " / "
				+ hyperLinkLength + ", size: " + result.size());
		*/

		if (result.size() > 0) {
			return result.toArray(new IHyperlink[result.size()]);
		} else {
			return null;
		}
	}

}