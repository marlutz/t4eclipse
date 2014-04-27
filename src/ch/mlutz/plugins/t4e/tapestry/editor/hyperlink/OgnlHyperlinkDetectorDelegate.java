/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - OgnlHyperlinkDetectorDelegate
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.editor.hyperlink;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

public class OgnlHyperlinkDetectorDelegate implements IHyperlinkDetectorDelegate {

	/**
	 * the log
	 */
	private static final IEclipseLog log=
			EclipseLogFactory.create(OgnlHyperlinkDetectorDelegate.class);

	/* (non-Javadoc)
	 * @see ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.IHyperlinkDetectorDelegate#detectHyperlinks(org.eclipse.jface.text.IDocument, java.lang.String, org.eclipse.jface.text.IRegion)
	 */
	@Override
	public List<SourceRangeHyperlink> detectHyperlinks(IDocument document,
			String content, IRegion region) {

		List<SourceRangeHyperlink> result= new ArrayList<SourceRangeHyperlink>();

		// go back until whitespace is found and
		int charIndex= region.getOffset();
		while (charIndex >= 0
				&& content.charAt(charIndex) != '"') {
			charIndex--;
		}

		// charIndex might be -1, but that's ok
		String prefix= content.substring(charIndex + 1);

		final String expectedPrefix= "ognl:";
		if (!prefix.startsWith(expectedPrefix)) {
			return null;
		}

		int ognlExpressionStart= charIndex + 1 + expectedPrefix.length();

		charIndex= region.getOffset();
		while (charIndex < content.length()
				&& content.charAt(charIndex) != '"'
				&& content.charAt(charIndex) != '.') {
			charIndex++;
		}

		final int hyperLinkOffset= ognlExpressionStart;

		final int hyperLinkLength= charIndex
				- hyperLinkOffset;

		if (hyperLinkLength < 0) {
			// caret might rest on the first quote
			log.warn("OGNL hyperLinkLength: " + hyperLinkLength
				+ "; charIndex: " + charIndex
				+ "; hyperLinkOffset: " + hyperLinkOffset);
			return null;
		}

		final String hyperlinkText= content.substring(ognlExpressionStart,
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
								.replaceAll("^(?:get|is)(.+)\\(\\)$", "$1");
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

		if (finalJavaFile != null) {
			SourceRangeHyperlink hyperlink= new SourceRangeHyperlink(
					new Region(hyperLinkOffset, hyperLinkLength),
					"Open method",
					javaFile,
					sourceRange,
					Constants.TAPESTRY_EDITOR_ID);
			result.add(hyperlink);
		}

		if (result.size() > 0) {
			return result;
		} else {
			return null;
		}
	}
}
