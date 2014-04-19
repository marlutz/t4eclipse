/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Eclipse tools
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import ch.mlutz.plugins.t4e.constants.Constants;

public class EclipseTools {
	/**
	 * Extracts the extension of a file name.
	 *
	 * @param filename the name of the file as a String
	 * @return the extension of a file (e.g. ".html") that starts with a dot
	 * 			and contains no further dots; alternatively, the empty string if
	 * 			the file has no extension
	 */
	public static String extractFileExtension(String filename) {
		Pattern pattern= Pattern.compile("(\\.[^\\./]+)$");
		Matcher matcher= pattern.matcher(filename);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "";
		}
	}

	/**
	 * Extracts the base of a file name (i.e. without extension)
	 *
	 * @param filename the name of the file as a String
	 * @return the base of a file name (e.g. Object.java ==> Object) that does
	 *          not end with a dot
	 */
	public static String extractFileBase(String filename) {
		Pattern pattern= Pattern.compile("^(.*)\\.[^\\./]+$");
		Matcher matcher= pattern.matcher(filename);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return filename;
		}
	}

	/**
	 * @param project
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public static IPackageFragmentRoot[] getPackageFragmentRoots(IProject project)
		throws CoreException, JavaModelException
	{
		IPackageFragmentRoot[] packageFragmentRoots= null;
		IJavaProject javaProject= null;
		if (project.hasNature(JavaCore.NATURE_ID)) {
			// Cast the IProject to IJavaProject.
			javaProject= JavaCore.create(project);

			// Get the array of IPackageFragmentRoot using getAllPackageFragmentRoots()
			packageFragmentRoots= javaProject.getAllPackageFragmentRoots();

			// Get the one(s) which have getKind() == IPackageFragmentRoot.K_SOURCE
			for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
				if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
					System.out.println("Source Folder: " + packageFragmentRoot.getPath());
				}
			}
		}

		return packageFragmentRoots;
	}

	/**
	 * Opens a new editor with the file to open in it.
	 *
	 * @param fileToOpen the file to be opened in the editor
	 * @param pageForNewEditor the page to open the editor with the file in
	 * @return
	 */
	public static IEditorPart openFileInEditor(IFile fileToOpen,
			IWorkbenchPage pageForNewEditor) {
		IEditorDescriptor desc = PlatformUI.getWorkbench().
			getEditorRegistry().getDefaultEditor(fileToOpen.getName());

		// use tapestry editor as default
		String editorId= Constants.TAPESTRY_EDITOR_ID;
		if (desc != null) {
			editorId= desc.getId();
		}

		IEditorPart result= null;
		if (pageForNewEditor != null) {
			try {
				result= pageForNewEditor.openEditor(new FileEditorInput(fileToOpen), editorId);
			} catch (PartInitException e) {
				// TODO generate warning
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Opens a new editor with the file to open in it.
	 *
	 * @param fileToOpen the file to be opened in the editor
	 * @param pageForNewEditor the page to open the editor with the file in
	 * @param editorId
	 * @return
	 * @throws PartInitException
	 */
	public static IEditorPart openFileInEditorChecked(IFile fileToOpen,
			IWorkbenchPage pageForNewEditor, String editorId)
					throws PartInitException {

		IEditorPart result= null;
		if (pageForNewEditor != null) {
			result= pageForNewEditor.openEditor(new FileEditorInput(fileToOpen),
					editorId);
		}
		return result;
	}

	/**
	 * Opens a new editor with the file to open in it.
	 *
	 * @param fileToOpen the file to be opened in the editor
	 * @param pageForNewEditor the page to open the editor with the file in
	 * @param editorId
	 * @return
	 */
	public static IEditorPart openFileInEditor(IFile fileToOpen,
			IWorkbenchPage pageForNewEditor, String editorId) {

		IEditorPart result= null;
		if (pageForNewEditor != null) {
			try {
				result= pageForNewEditor.openEditor(new FileEditorInput(fileToOpen), editorId);
			} catch (PartInitException e) {
				// TODO generate warning
				e.printStackTrace();
			}
		}
		return result;
	}

	public static void logMessage(String message) {
		DateFormat df= new SimpleDateFormat("HH:mm:ss.SSS");
		System.out.println(df.format(new Date()) + " " + message);
	}
}
