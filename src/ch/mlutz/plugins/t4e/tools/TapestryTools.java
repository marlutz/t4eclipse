/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry tools
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools;

import static ch.mlutz.plugins.t4e.tools.EclipseTools.extractFileBase;
import static ch.mlutz.plugins.t4e.tools.EclipseTools.extractFileExtension;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

public class TapestryTools {
	public static final String FILE_EXTENSION_HTML= ".html";
	public static final String FILE_EXTENSION_JWC= ".jwc";
	public static final String FILE_EXTENSION_PAGE= ".page";
	public static final String FILE_EXTENSION_APPLICATION= ".application";

	public static boolean isHtmlFileChecked(IFile file) throws CoreException {
		return FILE_EXTENSION_HTML.equalsIgnoreCase(extractFileExtension(file.getName())) ||
				(file.exists() && file.getContentDescription() != null
				&& file.getContentDescription().getContentType().equals(
						Platform.getContentTypeManager().getContentType("org.eclipse.wst.html.core.htmlsource")
				));
	}

	public static boolean isComponentSpecification(IFile file) {
		return FILE_EXTENSION_JWC.equalsIgnoreCase(extractFileExtension(file.getName()));
	}

	public static boolean isPageSpecification(IFile file) {
		return FILE_EXTENSION_PAGE.equalsIgnoreCase(extractFileExtension(file.getName()));
	}

	/**
	 * Returns true if the file given is a Tapestry application specification
	 * file (i.e. has extension .application).
	 *
	 * @param file the file to be checked
	 * @return true if file is a Tapestry application specification; false
	 * 		otherwise
	 */
	public static boolean isAppSpecification(IFile file) {
		return FILE_EXTENSION_APPLICATION.equalsIgnoreCase(extractFileExtension(file.getName()));
	}

	/**
	 * Returns true if the file given is a Java file (i.e. has content type
	 * org.eclipse.jdt.core.javaSource).
	 *
	 * @param file the file to be checked
	 * @return true if the file is a Java file; false otherwise
	 * @throws CoreException
	 */
	public static boolean isJavaFile(IFile file) throws CoreException {
		return (file.getContentDescription() != null
				&& file.getContentDescription().getContentType().equals(
						Platform.getContentTypeManager().getContentType("org.eclipse.jdt.core.javaSource")
				));
	}

	/**
	 * Tries to retrieve a component specification file (.jwc) for the html
	 * file given. Returns null if none such file exists.
	 *
	 * @param htmlFile the file for which a component specification is searched
	 * 			for
	 * @return the handle to the specification file if it exists, null otherwise
	 */
	public static IFile findComponentSpecificationforHtmlFile(IFile htmlFile) {
		return findSameBaseFileByExtension(htmlFile, FILE_EXTENSION_JWC);
	}

	public static IFile findPageSpecificationforHtmlFile(IFile htmlFile) {
		return findSameBaseFileByExtension(htmlFile, FILE_EXTENSION_PAGE);
	}

	public static IFile findHtmlForSpecificationFile(IFile specificationFile) {
		return findSameBaseFileByExtension(specificationFile, FILE_EXTENSION_HTML);
	}

	private static IFile findSameBaseFileByExtension(IFile baseFile, String extension) {
		String targetFileName= extractFileBase(baseFile.getName()) + extension;
		IContainer container= baseFile.getParent();
		IFile targetFile;
		if (container instanceof IFolder) {
			targetFile= ((IFolder) container).getFile(targetFileName);
			return targetFile.exists() ? targetFile : null;
		} else if (container instanceof IProject) {
			targetFile= ((IProject) container).getFile(targetFileName);
			return targetFile.exists() ? targetFile : null;
		} else {
			return null;
		}
	}
}
