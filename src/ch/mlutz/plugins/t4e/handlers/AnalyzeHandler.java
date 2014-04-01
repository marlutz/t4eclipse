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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

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
public class AnalyzeHandler extends AbstractHandler
{
	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			AnalyzeHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (event.getCommand().getId().equals(Command.ANALYZE)) {

			// get workbench window
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			// set selection service
			ISelectionService service = window.getSelectionService();
			// set structured selection
			IStructuredSelection structured = (IStructuredSelection) service.getSelection();

			//check if it is an IFile
			Object el= structured.getFirstElement();
			log.info(el.getClass().getName());

			ICompilationUnit compilationUnit= null;


			if (!(el instanceof ICompilationUnit) && el instanceof IFile) {
				IFile file = (IFile) el;

				compilationUnit= JavaCore.createCompilationUnitFrom((IFile) file);
				final IJavaElement javaElement= JavaCore.create((IFile) file);
				log.info(javaElement.toString());
				IPath path = file.getLocation();
				System.out.println(path.toPortableString());
			}

			if (el instanceof ICompilationUnit) {
				compilationUnit= (ICompilationUnit) el;

				log.info(compilationUnit.toString());
				try {
					log.info(compilationUnit.getTypes().toString());
				} catch (JavaModelException e) {
					log.warn("Getting types from compilation unit failed. ", e);
				}
			}

			/*
			//check if it is an ICompilationUnit
			if (structured.getFirstElement() instanceof ICompilationUnit) {
				ICompilationUnit cu = (ICompilationUnit) structured.getFirstElement();
				System.out.println(cu.getElementName());
			}
			*/
		}

		return null;
	}
}
