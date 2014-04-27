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
package ch.mlutz.plugins.t4e.tapestry.editor.hyperlink;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryHtmlElement;

public class JwcidHyperlinkDetectorDelegate implements IHyperlinkDetectorDelegate {

	/**
	 * the log
	 */
	private static final IEclipseLog log=
			EclipseLogFactory.create(JwcidHyperlinkDetectorDelegate.class);

	/* (non-Javadoc)
	 * @see ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.IHyperlinkDetectorDelegate#detectHyperlinks(org.eclipse.jface.text.IDocument, java.lang.String, org.eclipse.jface.text.IRegion)
	 */
	@Override
	public List<SourceRangeHyperlink> detectHyperlinks(IDocument document,
			String content, IRegion region) {

		List<SourceRangeHyperlink> result= new ArrayList<SourceRangeHyperlink>();

		// go back until whitespace is found and
		int componentIdStart= -1;
		int charIndex= region.getOffset();
		while (charIndex >= 0
				&& !Character.isWhitespace(content.charAt(charIndex))) {
			if (content.charAt(charIndex) == '@') {
				componentIdStart= charIndex + 1;
			}
			charIndex--;
		}

		// charIndex might be -1, but that's ok
		String prefix= content.substring(charIndex + 1);

		final String expectedPrefix= "jwcid=\"";
		if (!prefix.startsWith(expectedPrefix)) {
			log.info("Expected prefix didn't match");
			return null;
		}

		if (componentIdStart == -1) {
			componentIdStart= charIndex + 1
					+ expectedPrefix.length();
		}

		// now go back until a " is found
		charIndex= region.getOffset();
		while (charIndex < content.length()
				&& !(content.charAt(charIndex) == '"')) {
			charIndex++;
		}

		int hyperlinkOffset= componentIdStart;

		int hyperlinkLength=charIndex
				- hyperlinkOffset;

		if (hyperlinkLength < 0) {
			// caret might rest on the first quote
			log.warn("hyperlinkLength: " + hyperlinkLength
				+ "; charIndex: " + charIndex
				+ "; hyperlinkOffset: " + hyperlinkOffset);
			return null;
		}

		String hyperlinkText= content.substring(componentIdStart,
			componentIdStart + hyperlinkLength);

		IRegion hyperlinkRegion= new Region(hyperlinkOffset, hyperlinkLength);

		TapestryIndex tapestryIndex= Activator.getDefault().getTapestryIndex();
		IFile documentFile= tapestryIndex.getDocumentToFileMapping(document);

		if (documentFile == null) {
			return null;
		}

		TapestryModule module= tapestryIndex.getModuleForResource(documentFile);

		if (module == null) {
			return null;
		}

		TapestryHtmlElement linkedComponent= null;
		for (TapestryHtmlElement component: module.getComponents()) {
			if (hyperlinkText.equals(component.getPath())) {
				linkedComponent= component;
			}
		}

		final TapestryHtmlElement finalLinkedComponent= linkedComponent;

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

		return result;
	}
}
