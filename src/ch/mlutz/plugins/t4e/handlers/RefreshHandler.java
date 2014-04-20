/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Refresh handler
 ******************************************************************************/
package ch.mlutz.plugins.t4e.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Command;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * This handler handles refreshing projects.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RefreshHandler extends AbstractHandler
{
	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			RefreshHandler.class);

	/**
	 * This execute handles refresh and refresh all commands
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// get workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		// set selection service
		ISelectionService service = window.getSelectionService();
		// set structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();

		TapestryIndexer tapestryIndexer= Activator.getDefault()
				.getTapestryIndexer();
		if (event.getCommand().getId().equals(Command.REFRESH)) {
			log.info("Refresh one project.");

			IProject project= null;
			if (structured.getFirstElement() instanceof IProject) {
				project= (IProject) structured.getFirstElement();
			} else if (structured.getFirstElement() instanceof IJavaProject) {
				project= ((IJavaProject) structured.getFirstElement()).getProject();
			}

			tapestryIndexer.removeProjectFromIndex(project);
			tapestryIndexer.addProjectToIndex(project);
		} else if (event.getCommand().getId().equals(
				Command.CLEAR_TAPESTRY_INDEX)) {
			log.info("Cleared Tapestry index.");
			Activator.getDefault().getTapestryIndex().clear();
		}

		/*
		// get workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		// set selection service
		ISelectionService service = window.getSelectionService();
		// set structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();

		//check if it is an IFile
		if (structured.getFirstElement() instanceof IFile) {
			// get the selected file
			IFile file = (IFile) structured.getFirstElement();
			// get the path
			IPath path = file.getLocation();
			System.out.println(path.toPortableString());
		}

		//check if it is an ICompilationUnit
		if (structured.getFirstElement() instanceof ICompilationUnit) {
			ICompilationUnit cu = (ICompilationUnit) structured.getFirstElement();
			System.out.println(cu.getElementName());
		}
		*/

		return null;
	}
}
