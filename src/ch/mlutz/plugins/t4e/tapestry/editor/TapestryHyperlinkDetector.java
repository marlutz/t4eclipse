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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.SourceRangeHyperlink;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryHtmlElement;

public class TapestryHyperlinkDetector extends AbstractHyperlinkDetector {

	private static final IEclipseLog log=
			EclipseLogFactory.create(TapestryHyperlinkDetector.class);

	private OgnlHyperlinkDetector ognlHyperlinkDetector= new OgnlHyperlinkDetector();

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region,
			boolean canHandleMultipleLinks) {

		IDocument document= viewer.getDocument();
		String contentBefore;
		ITypedRegion partition;
		try {
			partition= document.getPartition(region.getOffset());

			if (!partition.getType().equals(TapestryPartitionScanner.XML_TAG)) {
				return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
			}

			contentBefore= document.get(partition.getOffset(),
					partition.getLength());
		} catch(BadLocationException e) {
			log.warn("Could not detect hyperlinks: ", e);
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}

		List<IHyperlink> result= new ArrayList<IHyperlink>();

		// go back until whitespace is found and
		int componentIdStart= -1;
		int charIndex= region.getOffset() - partition.getOffset();
		while (charIndex >= 0
				&& !Character.isWhitespace(contentBefore.charAt(charIndex))) {
			if (contentBefore.charAt(charIndex) == '@') {
				componentIdStart= charIndex + 1;
			}
			charIndex--;
		}

		// charIndex might be -1, but that's ok
		String prefix= contentBefore.substring(charIndex + 1);

		final String expectedPrefix= "jwcid=\"";
		if (!prefix.startsWith(expectedPrefix)) {
			log.info("Expected prefix didn't match");
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}

		if (componentIdStart == -1) {
			componentIdStart= charIndex + 1
					+ expectedPrefix.length();
		}

		charIndex= region.getOffset() - partition.getOffset();
		while (charIndex < contentBefore.length()
				&& !(contentBefore.charAt(charIndex) == '"')) {
			charIndex++;
		}

		int hyperlinkOffset= partition.getOffset() + componentIdStart;

		int hyperlinkLength= partition.getOffset() + charIndex
				- hyperlinkOffset;

		if (hyperlinkLength < 0) {
			// caret might rest on the first quote
			log.warn("hyperlinkLength: " + hyperlinkLength
				+ "; partition.getOffset(): " + partition.getOffset()
				+ "; charIndex: " + charIndex
				+ "; hyperlinkOffset: " + hyperlinkOffset);
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}

		String hyperlinkText= contentBefore.substring(componentIdStart,
			componentIdStart + hyperlinkLength);

		IRegion hyperlinkRegion= new Region(hyperlinkOffset, hyperlinkLength);

		TapestryIndex tapestryIndex= Activator.getDefault().getTapestryIndex();
		IFile documentFile= tapestryIndex.getDocumentToFileMapping(document);

		if (documentFile == null) {
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}

		TapestryModule module= tapestryIndex.getModuleForResource(documentFile);

		if (module == null) {
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}

		TapestryHtmlElement linkedComponent= null;
		for (TapestryHtmlElement component: module.getComponents()) {
			if (hyperlinkText.equals(component.getPath())) {
				linkedComponent= component;
			}
		}

		final TapestryHtmlElement finalLinkedComponent= linkedComponent;

		// now go back until a " is found


		/*
		if (Character.isWhitespace(s.charAt(i)) {
			containsWhitespace = true;
		}
		*/

		SourceRangeHyperlink hyperlink;

		if (finalLinkedComponent != null) {

			// Html hyperlink
			if (finalLinkedComponent.getHtmlFile() != null) {
				hyperlink= new SourceRangeHyperlink(
						hyperlinkRegion,
						"Open HTML template",
						finalLinkedComponent.getHtmlFile(), null,
						Constants.TAPESTRY_EDITOR_ID);
				result.add(hyperlink);
			}

			// Java hyperlink
			IFile javaFile= null;
			try {
				if (finalLinkedComponent.getJavaCompilationUnit() != null) {
					javaFile= (IFile) finalLinkedComponent
						.getJavaCompilationUnit().getCorrespondingResource()
						.getAdapter(IFile.class);
				}
			} catch (JavaModelException e) {
				log.warn("Getting corresponding resource for compilation unit "
					+ finalLinkedComponent.getJavaCompilationUnit().getElementName()
					+ " failed: ", e);
			}

			if (javaFile != null) {
				hyperlink= new SourceRangeHyperlink(
						hyperlinkRegion,
						"Open Java class",
						javaFile, null, null);
				result.add(hyperlink);
			}

			// Specification hyperlink
			if (finalLinkedComponent.getSpecification() != null) {
				// Html hyperlink
				if (finalLinkedComponent.getHtmlFile() != null) {
					hyperlink= new SourceRangeHyperlink(
							hyperlinkRegion,
							"Open specification",
							finalLinkedComponent.getSpecification(), null,
							null);
					result.add(hyperlink);
				}
			}
		}

		/*
		log.info("detectHyperlinks called: " + hyperLinkOffset + " / "
				+ hyperLinkLength + ", size: " + result.size());
		*/

		if (result.size() > 0) {
			IHyperlink[] ognlResult= ognlHyperlinkDetector.detectHyperlinks(
				viewer, region, canHandleMultipleLinks);
			if (ognlResult != null) {
				result.addAll(Arrays.asList(ognlResult));
			}
			return result.toArray(new IHyperlink[result.size()]);
		} else {
			return ognlHyperlinkDetector.detectHyperlinks(viewer, region, canHandleMultipleLinks);
		}
	}

}